/*******************************************************************************
 * Copyright (c) 2010 Nicolas Roduit.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 ******************************************************************************/
package org.weasis.dicom.explorer.wado;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.weasis.core.api.explorer.ObservableEvent;
import org.weasis.core.api.explorer.model.DataExplorerModel;
import org.weasis.core.api.gui.util.GuiExecutor;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.media.data.Thumbnail;
import org.weasis.core.api.service.BundleTools;
import org.weasis.dicom.codec.DicomSeries;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.explorer.ExplorerTask;
import org.weasis.dicom.explorer.Messages;
import org.weasis.dicom.explorer.pref.download.SeriesDownloadPrefUtils;

public class LoadRemoteDicomManifest extends ExplorerTask {

    public static final String CONCURRENT_SERIES = "download.concurrent.series"; //$NON-NLS-1$
    public static final BlockingQueue<Runnable> loadingQueue = new PriorityBlockingQueue<Runnable>(10,
        new PriorityTaskComparator());
    public static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        BundleTools.SYSTEM_PREFERENCES.getIntProperty(CONCURRENT_SERIES, 3),
        BundleTools.SYSTEM_PREFERENCES.getIntProperty(CONCURRENT_SERIES, 3), 0L, TimeUnit.MILLISECONDS, loadingQueue);
    public static final ArrayList<LoadSeries> currentTasks = new ArrayList<LoadSeries>();
    private final String[] xmlFiles;
    private final DicomModel dicomModel;

    private static class PriorityTaskComparator implements Comparator<Runnable>, Serializable {

        private static final long serialVersionUID = 513213203958362767L;

        @Override
        public int compare(final Runnable r1, final Runnable r2) {
            LoadSeries o1 = (LoadSeries) r1;
            LoadSeries o2 = (LoadSeries) r2;
            DownloadPriority val1 = o1.getPriority();
            DownloadPriority val2 = o2.getPriority();

            int rep = val1.getPriority().compareTo(val2.getPriority());
            if (rep != 0) {
                return rep;
            }
            rep = DicomModel.PATIENT_COMPARATOR.compare(val1.getPatient(), val2.getPatient());
            if (rep != 0) {
                return rep;
            }

            rep = DicomModel.STUDY_COMPARATOR.compare(val1.getStudy(), val2.getStudy());
            if (rep != 0) {
                return rep;
            }
            return DicomModel.SERIES_COMPARATOR.compare(val1.getSeries(), val2.getSeries());
        }
    }

    public LoadRemoteDicomManifest(String[] xmlFiles, DataExplorerModel explorerModel) {
        super(Messages.getString("DicomExplorer.loading"), true); //$NON-NLS-1$
        if (xmlFiles == null || !(explorerModel instanceof DicomModel)) {
            throw new IllegalArgumentException("invalid parameters"); //$NON-NLS-1$
        }
        this.xmlFiles = xmlFiles;
        this.dicomModel = (DicomModel) explorerModel;
    }

    public LoadRemoteDicomManifest(File[] xmlFiles, DataExplorerModel explorerModel) {
        super(Messages.getString("DicomExplorer.loading"), true); //$NON-NLS-1$
        if (xmlFiles == null || !(explorerModel instanceof DicomModel)) {
            throw new IllegalArgumentException("invalid parameters"); //$NON-NLS-1$
        }
        String[] xmlRef = new String[xmlFiles.length];
        for (int i = 0; i < xmlFiles.length; i++) {
            if (xmlFiles[i] != null) {
                xmlRef[i] = xmlFiles[i].getAbsolutePath();
            }
        }
        this.xmlFiles = xmlRef;
        this.dicomModel = (DicomModel) explorerModel;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.LoadingStart, dicomModel, null,
            this));
        for (int i = 0; i < xmlFiles.length; i++) {
            if (xmlFiles[i] != null) {
                URI uri = null;
                try {
                    if (!xmlFiles[i].startsWith("http")) { //$NON-NLS-1$
                        try {
                            File file = new File(xmlFiles[i]);
                            if (file.canRead()) {
                                uri = file.toURI();
                            }
                        } catch (Exception e) {
                        }
                    }
                    if (uri == null) {
                        uri = new URL(xmlFiles[i]).toURI();
                    }
                    ArrayList<LoadSeries> wadoTasks = DownloadManager.buildDicomSeriesFromXml(uri, dicomModel);

                    if (wadoTasks != null) {
                        boolean downloadImmediately = SeriesDownloadPrefUtils.downloadImmediately();
                        for (final LoadSeries loadSeries : wadoTasks) {
                            addLoadSeries(loadSeries, dicomModel);
                            if (downloadImmediately) {
                                loadingQueue.offer(loadSeries);
                            } else {
                                GuiExecutor.instance().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadSeries.getProgressBar().setValue(0);
                                        loadSeries.stop();
                                    }
                                });
                            }
                        }
                        // Sort tasks from the download priority order
                        Collections.sort(currentTasks, Collections.reverseOrder(new PriorityTaskComparator()));
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

        executor.prestartAllCoreThreads();
        return true;
    }

    @Override
    protected void done() {
        dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.LoadingStop, dicomModel, null,
            this));
    }

    public static synchronized void addLoadSeries(LoadSeries series, DicomModel dicomModel) {
        if (series != null) {
            if (dicomModel != null) {
                dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.LoadingStart, dicomModel,
                    null, series));
            }
            if (!currentTasks.contains(series)) {
                currentTasks.add(series);
            }
        }
    }

    public static synchronized void removeLoadSeries(LoadSeries series, DicomModel dicomModel) {
        if (series != null) {
            currentTasks.remove(series);
            if (dicomModel != null) {
                dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.LoadingStop, dicomModel,
                    null, series));
            }
            if (currentTasks.size() == 0) {
                // When all loadseries are ended, reset to default the number of simultaneous download (series)
                LoadRemoteDicomManifest.executor.setCorePoolSize(BundleTools.SYSTEM_PREFERENCES.getIntProperty(
                    CONCURRENT_SERIES, 3));
            }
        }
    }

    public static void stopDownloading(DicomSeries series, DicomModel dicomModel) {
        if (series != null) {
            synchronized (LoadRemoteDicomManifest.currentTasks) {
                for (final LoadSeries loading : LoadRemoteDicomManifest.currentTasks) {
                    if (loading.getDicomSeries() == series) {
                        removeLoadSeries(loading, dicomModel);
                        LoadRemoteDicomManifest.loadingQueue.remove(loading);
                        if (StateValue.STARTED.equals(loading.getState())) {
                            loading.cancel(true);
                        }
                        // Ensure to stop downloading
                        series.setSeriesLoader(null);
                        break;
                    }
                }
            }
        }
    }

    public static void resume() {
        handleAllSeries(new LoadSeriesHandler() {
            @Override
            public void handle(LoadSeries loadSeries) {
                loadSeries.resume();
            }
        });
    }

    public static void stop() {
        handleAllSeries(new LoadSeriesHandler() {
            @Override
            public void handle(LoadSeries loadSeries) {
                loadSeries.stop();
            }
        });
    }

    private static void handleAllSeries(LoadSeriesHandler handler) {
        for (LoadSeries loadSeries : new ArrayList<LoadSeries>(currentTasks)) {
            handler.handle(loadSeries);
            Thumbnail thumbnail = (Thumbnail) loadSeries.getDicomSeries().getTagValue(TagW.Thumbnail);
            if (thumbnail != null) {
                thumbnail.repaint();
            }
        }
    }

    private static interface LoadSeriesHandler {
        void handle(LoadSeries loadSeries);
    }

}
