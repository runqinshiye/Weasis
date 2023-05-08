/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.editor;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.swing.JMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.ui.docking.DockableTool;
import org.weasis.core.ui.util.Toolbar;

public abstract class MimeSystemAppViewer implements SeriesViewer<MediaElement> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MimeSystemAppViewer.class);

  private static final String ERROR_MSG =
      "Cannot open {} with the default system application"; // NON-NLS

  @Override
  public void close() {}

  @Override
  public List<MediaSeries<MediaElement>> getOpenSeries() {
    return Collections.emptyList();
  }

  public static void startAssociatedProgramFromLinux(File file) {
    if (file != null && file.canRead()) {
      try {
        String[] cmd = new String[] {"xdg-open", file.getCanonicalPath()}; // NON-NLS
        Runtime.getRuntime().exec(cmd);
      } catch (IOException e) {
        LOGGER.error(ERROR_MSG, file, e);
      }
    }
  }

  public static void startAssociatedProgramFromWinCMD(File file) {
    if (file != null && file.canRead()) {
      try {
        String[] cmd = new String[] {"cmd", "/c", file.getCanonicalPath()}; // NON-NLS
        Runtime.getRuntime().exec(cmd); // NON-NLS
      } catch (IOException e) {
        LOGGER.error(ERROR_MSG, file, e);
      }
    }
  }

  public static void startAssociatedProgramFromDesktop(final Desktop desktop, File file) {
    if (file != null && file.canRead()) {
      try {
        desktop.open(file);
      } catch (IOException e) {
        LOGGER.error(ERROR_MSG, file, e);
      }
    }
  }

  @Override
  public void removeSeries(MediaSeries<MediaElement> sequence) {}

  @Override
  public JMenu fillSelectedPluginMenu(JMenu menu) {
    return null;
  }

  @Override
  public List<Toolbar> getToolBar() {
    return null;
  }

  @Override
  public List<DockableTool> getToolPanel() {
    return null;
  }

  @Override
  public void setSelected(boolean selected) {}

  @Override
  public MediaSeriesGroup getGroupID() {
    return null;
  }
}
