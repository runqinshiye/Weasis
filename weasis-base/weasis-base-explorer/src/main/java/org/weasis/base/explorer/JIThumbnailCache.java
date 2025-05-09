/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.base.explorer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.base.explorer.list.ThumbnailList;
import org.weasis.core.api.gui.util.GuiExecutor;
import org.weasis.core.api.image.cv.ImageCVIO;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.util.ThreadUtil;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageConversion;
import org.weasis.opencv.op.ImageProcessor;

public final class JIThumbnailCache {
  private static final Logger LOGGER = LoggerFactory.getLogger(JIThumbnailCache.class);

  private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
  // Set only one concurrent thread. The time-consuming part is in loading image thread (see
  // ImageElement)
  private final ExecutorService qExecutor =
      new ThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.MILLISECONDS,
          queue,
          ThreadUtil.getThreadFactory("Thumbnail Cache")); // NON-NLS

  private final Map<URI, ThumbnailIcon> cachedThumbnails =
      Collections.synchronizedMap(
          new LinkedHashMap<>(80) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry eldest) {
              return size() > 100;
            }
          });

  public synchronized void invalidate() {
    this.cachedThumbnails.clear();
  }

  public void removeInQueue(ImageElement imgElement) {
    Runnable r = null;
    for (Runnable runnable : queue) {
      if (Objects.equals(imgElement, ((ThumbnailRunnable) runnable).getDiskObject())) {
        r = runnable;
      }
    }
    if (r != null && !queue.remove(r)) {
      LOGGER.error("Cannot remove thumbnail from the queue");
    }
  }

  public ThumbnailIcon getThumbnailFor(
      final ImageElement diskObject,
      final ThumbnailList<? extends MediaElement> aThumbnailList,
      final int index) {
    try {
      final ThumbnailIcon jiIcon = this.cachedThumbnails.get(diskObject.getMediaURI());
      if (jiIcon != null) {
        return jiIcon;
      }

    } catch (final Exception e) {
      LOGGER.error("", e);
    }
    if (!diskObject.isLoading()) {
      loadThumbnail(diskObject, aThumbnailList, index);
    }
    return null;
  }

  private void loadThumbnail(
      final ImageElement diskObject,
      final ThumbnailList<? extends MediaElement> thumbnailList,
      final int index) {
    if ((index > thumbnailList.getLastVisibleIndex())
        || (index < thumbnailList.getFirstVisibleIndex())) {
      return;
    }
    for (Runnable runnable : queue) {
      if (diskObject.equals(((ThumbnailRunnable) runnable).getDiskObject())) {
        return;
      }
    }
    cleanPending();
    ThumbnailRunnable runnable = new ThumbnailRunnable(diskObject, thumbnailList, index);
    qExecutor.execute(runnable);
  }

  private void cleanPending() {
    for (Runnable runnable : queue) {
      ThumbnailRunnable r = (ThumbnailRunnable) runnable;
      int index = r.getIndex();
      if ((index > r.getThumbnailList().getLastVisibleIndex())
          || (index < r.getThumbnailList().getFirstVisibleIndex())) {
        removeInQueue(r.getDiskObject());
      }
    }
  }

  class ThumbnailRunnable implements Runnable {
    final ImageElement diskObject;
    final ThumbnailList<? extends MediaElement> thumbnailList;
    final int index;

    public ThumbnailRunnable(
        ImageElement diskObject, ThumbnailList<? extends MediaElement> thumbnailList, int index) {
      this.diskObject = diskObject;
      this.thumbnailList = thumbnailList;
      this.index = index;
    }

    public ImageElement getDiskObject() {
      return diskObject;
    }

    public ThumbnailList<? extends MediaElement> getThumbnailList() {
      return thumbnailList;
    }

    public int getIndex() {
      return index;
    }

    @Override
    public void run() {
      PlanarImage img = null;

      // Get the final that contain the thumbnail when the uncompressed mode is activated
      File file = diskObject.getFile();
      if (file != null && file.getName().endsWith(".wcv")) {
        File thumbFile = new File(ImageCVIO.changeExtension(file.getPath(), ".jpg"));
        if (thumbFile.canRead()) {
          img = ImageProcessor.readImage(thumbFile, null);
        }
      }

      if (img == null) {
        img = diskObject.getRenderedImage(diskObject.getImage(null));
      }

      if (img == null) {
        return;
      }

      final BufferedImage tIcon =
          ImageConversion.toBufferedImage(
              (PlanarImage) ImageProcessor.buildThumbnail(img, ThumbnailRenderer.ICON_DIM, true));

      GuiExecutor.execute(
          () -> {
            if (tIcon != null) {
              cachedThumbnails.put(diskObject.getMediaURI(), new ThumbnailIcon(tIcon));
            }
            thumbnailList.getThumbnailListModel().notifyAsUpdated(index);
          });
    }
  }
}
