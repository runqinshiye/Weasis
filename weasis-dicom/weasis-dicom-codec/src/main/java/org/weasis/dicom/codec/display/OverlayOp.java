/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.codec.display;

import java.util.HashMap;
import java.util.Optional;
import org.dcm4che3.data.Tag;
import org.dcm4che3.img.DicomImageReadParam;
import org.dcm4che3.img.DicomMetaData;
import org.dcm4che3.img.data.OverlayData;
import org.dcm4che3.img.data.PrDicomObject;
import org.dcm4che3.img.stream.ImageDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.image.AbstractOp;
import org.weasis.core.api.image.ImageOpEvent;
import org.weasis.core.api.image.ImageOpEvent.OpEvent;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.PRSpecialElement;
import org.weasis.opencv.data.PlanarImage;

public class OverlayOp extends AbstractOp {
  private static final Logger LOGGER = LoggerFactory.getLogger(OverlayOp.class);

  public static final String OP_NAME = ActionW.IMAGE_OVERLAY.getTitle();

  public static final String P_SHOW = "overlay"; // NON-NLS
  public static final String P_IMAGE_ELEMENT = "img.element";

  public OverlayOp() {
    setName(OP_NAME);
  }

  public OverlayOp(OverlayOp op) {
    super(op);
  }

  @Override
  public OverlayOp copy() {
    return new OverlayOp(this);
  }

  @Override
  public void handleImageOpEvent(ImageOpEvent event) {
    OpEvent type = event.getEventType();
    if (OpEvent.ImageChange.equals(type) || OpEvent.ResetDisplay.equals(type)) {
      setParam(WindowAndPresetsOp.P_PR_ELEMENT, null);
      setParam(P_IMAGE_ELEMENT, event.getImage());
    } else if (OpEvent.ApplyPR.equals(type)) {
      HashMap<String, Object> p = event.getParams();
      if (p != null) {
        PRSpecialElement pr =
            Optional.ofNullable(p.get(ActionW.PR_STATE.cmd()))
                .filter(PRSpecialElement.class::isInstance)
                .map(PRSpecialElement.class::cast)
                .orElse(null);
        setParam(WindowAndPresetsOp.P_PR_ELEMENT, pr == null ? null : pr.getPrDicomObject());
        setParam(P_IMAGE_ELEMENT, event.getImage());
      }
    }
  }

  @Override
  public void process() throws Exception {
    PlanarImage source = (PlanarImage) params.get(Param.INPUT_IMG);
    PlanarImage result = source;
    Boolean overlay = (Boolean) params.get(P_SHOW);

    if (overlay != null && overlay) {
      ImageElement image = (ImageElement) params.get(P_IMAGE_ELEMENT);
      if (image != null) {
        if (image.getMediaReader() instanceof DicomMediaIO) {
          DicomMediaIO reader = (DicomMediaIO) image.getMediaReader();
          DicomMetaData md = reader.getDicomMetaData();
          if (md != null) {
            ImageDescriptor desc = md.getImageDescriptor();
            if (image.getKey() instanceof Integer) {
              int frame = (Integer) image.getKey();
              DicomImageReadParam p = new DicomImageReadParam();
              p.setPresentationState((PrDicomObject) params.get(WindowAndPresetsOp.P_PR_ELEMENT));
              PlanarImage original = source;
              if (!desc.getEmbeddedOverlay().isEmpty()) {
                original = reader.getImageFragment(image, (Integer) image.getKey(), false);
              }
              result = OverlayData.getOverlayImage(original, source, desc, p, frame);
            }
          }
        }
      }
    }
    params.put(Param.OUTPUT_IMG, result);
  }
}
