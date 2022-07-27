/*
 * Copyright (c) 2022 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.weasis.core.api.explorer.ObservableEvent;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.editor.SeriesViewerFactory;
import org.weasis.core.ui.editor.ViewerPluginBuilder;
import org.weasis.core.util.LangUtil;
import org.weasis.dicom.explorer.HangingProtocols.OpeningViewer;

public class PluginOpeningStrategy {

  private OpeningViewer openingMode;
  private boolean openPlugin;

  private boolean resetVeto;

  private final Map<MediaSeriesGroup, Boolean> openPatientMap = new HashMap<>();

  public PluginOpeningStrategy(OpeningViewer openingMode) {
    this.openPlugin = true;
    setOpeningMode(openingMode);
  }

  public OpeningViewer getOpeningMode() {
    return openingMode;
  }

  public void setOpeningMode(OpeningViewer openingMode) {
    this.openingMode = Objects.requireNonNullElse(openingMode, OpeningViewer.ALL_PATIENTS);
  }

  public boolean isOpenPlugin() {
    return openPlugin;
  }

  public void setOpenPlugin(boolean openPlugin) {
    this.openPlugin = openPlugin;
  }

  public boolean hasToBeOpened(MediaSeriesGroup patient) {
    return LangUtil.getNULLtoTrue(openPatientMap.get(Objects.requireNonNull(patient)));
  }

  public boolean isResetVeto() {
    return resetVeto;
  }

  public void setResetVeto(boolean resetVeto) {
    this.resetVeto = resetVeto;
  }

  public void reset() {
    if (!resetVeto) {
      this.openPlugin = true;
      openPatientMap.clear();
    }
  }

  public void put(MediaSeriesGroup patient, boolean b) {
    openPatientMap.put(patient, b);
  }

  public void prepareImport() {
    if (OpeningViewer.ONE_PATIENT_CLEAN.equals(openingMode)
        || OpeningViewer.ALL_PATIENTS_CLEAN.equals(openingMode)) {
      UIManager.closeSeriesViewer(new ArrayList<>(UIManager.VIEWER_PLUGINS));
    }
  }

  public void openViewerPlugin(
      MediaSeriesGroup patient, DicomModel dicomModel, Series<?> dicomSeries) {
    Objects.requireNonNull(dicomModel);
    Objects.requireNonNull(dicomSeries);

    boolean selectPatient = true;
    if (!OpeningViewer.NONE.equals(openingMode) && isOpenPlugin() && hasToBeOpened(patient)) {
      SeriesViewerFactory plugin = UIManager.getViewerFactory(dicomSeries.getMimeType());
      if (plugin != null && !(plugin instanceof MimeSystemAppFactory)) {
        if (OpeningViewer.ONE_PATIENT.equals(openingMode)
            || OpeningViewer.ONE_PATIENT_CLEAN.equals(openingMode)) {
          setOpenPlugin(false);
        } else {
          put(patient, false);
        }
        selectPatient = false;
        ViewerPluginBuilder.openSequenceInPlugin(plugin, dicomSeries, dicomModel, true, true);
      }
    }
    if (selectPatient) {
      // Send event to select the related patient in Dicom Explorer.
      dicomModel.firePropertyChange(
          new ObservableEvent(ObservableEvent.BasicAction.SELECT, dicomModel, null, dicomSeries));
    }
  }
}
