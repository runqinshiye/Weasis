/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.internal;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.felix.prefs.BackingStore;
import org.apache.felix.service.command.CommandProcessor;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.explorer.model.AbstractFileModel;
import org.weasis.core.api.gui.util.AppProperties;
import org.weasis.core.api.gui.util.GuiExecutor;
import org.weasis.core.api.media.data.Codec;
import org.weasis.core.api.service.AuditLog;
import org.weasis.core.api.service.BundlePreferences;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.api.util.ResourceUtil;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.editor.FileModel;
import org.weasis.core.ui.editor.SeriesViewerFactory;
import org.weasis.core.ui.editor.ViewerPluginBuilder;
import org.weasis.core.ui.editor.image.dockable.MeasureTool;
import org.weasis.core.ui.model.layer.AbstractInfoLayer;
import org.weasis.core.util.FileUtil;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}") // NON-NLS
public class Activator implements BundleActivator, ServiceListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    bundleContext.registerService(
        BackingStore.class.getName(), new StreamBackingStoreImpl(bundleContext), null);

    for (ServiceReference<Codec> service : bundleContext.getServiceReferences(Codec.class, null)) {
      registerCodecPlugins(bundleContext.getService(service));
    }

    bundleContext.addServiceListener(this, BundleTools.createServiceFilter(Codec.class));

    initLoggerAndAudit(bundleContext);
    File file = ResourceUtil.getResource("presets.xml");
    if (file.canRead()) {
      System.setProperty("dicom.presets.path", file.getPath());
    }

    registerCommands(bundleContext);
    File dataFolder = AppProperties.getBundleDataFolder(bundleContext);
    FileUtil.readProperties(
        new File(dataFolder, "persistence.properties"), BundleTools.LOCAL_UI_PERSISTENCE);
    Preferences prefs = BundlePreferences.getDefaultPreferences(bundleContext);
    AbstractInfoLayer.applyPreferences(prefs);
    MeasureTool.viewSetting.initMonitors();
    MeasureTool.viewSetting.applyPreferences(prefs);

    // Must be instantiated in EDT
    GuiExecutor.instance()
        .execute(
            () -> {
              try {
                for (ServiceReference<SeriesViewerFactory> service :
                    bundleContext.getServiceReferences(SeriesViewerFactory.class, null)) {
                  registerSeriesViewerFactory(bundleContext.getService(service));
                }
              } catch (InvalidSyntaxException e) {
                LOGGER.error("", e);
              }
            });

    bundleContext.addServiceListener(
        this, BundleTools.createServiceFilter(Codec.class, SeriesViewerFactory.class));
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    BundleTools.saveSystemPreferences();

    // Save preferences
    Preferences prefs = BundlePreferences.getDefaultPreferences(bundleContext);
    AbstractInfoLayer.savePreferences(prefs);
    MeasureTool.viewSetting.savePreferences(prefs);
    File dataFolder = AppProperties.getBundleDataFolder(bundleContext);
    if (dataFolder != null) {
      File file = new File(dataFolder, "persistence.properties");
      FileUtil.prepareToWriteFile(file);
      FileUtil.storeProperties(file, BundleTools.LOCAL_UI_PERSISTENCE, null);
    }
  }

  @Override
  public synchronized void serviceChanged(ServiceEvent event) {

    ServiceReference<?> sRef = event.getServiceReference();
    BundleContext context = AppProperties.getBundleContext(sRef);
    Object service = null;
    try {
      service = context.getService(sRef);
    } catch (RuntimeException e) {
      LOGGER.error("Cannot get service of {}", sRef.getBundle(), e);
    }
    if (service == null) {
      return;
    }

    if (service instanceof Codec codec) {
      // TODO manage when several identical MimeType, register the default one
      if (event.getType() == ServiceEvent.REGISTERED) {
        registerCodecPlugins(codec);
      } else if (event.getType() == ServiceEvent.UNREGISTERING) {
        if (BundleTools.CODEC_PLUGINS.contains(codec)) {
          LOGGER.info("Unregister Image Codec Plug-in: {}", codec.getCodecName());
          BundleTools.CODEC_PLUGINS.remove(codec);
        }
        // Unget service object and null references.
        context.ungetService(sRef);
      }
    } else if (service instanceof SeriesViewerFactory viewerFactory) {
      // Must be instantiated in EDT
      GuiExecutor.instance()
          .execute(
              () -> {
                if (event.getType() == ServiceEvent.REGISTERED) {
                  registerSeriesViewerFactory(viewerFactory);
                } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                  if (UIManager.SERIES_VIEWER_FACTORIES.contains(viewerFactory)) {
                    LOGGER.info(
                        "Unregister series viewer plug-in: {}", viewerFactory.getDescription());
                    UIManager.SERIES_VIEWER_FACTORIES.remove(viewerFactory);
                  }
                  context.ungetService(sRef);
                }
              });
    }
  }

  private static void registerCodecPlugins(Codec codec) {
    if (codec != null && !BundleTools.CODEC_PLUGINS.contains(codec)) {
      BundleTools.CODEC_PLUGINS.add(codec);
      LOGGER.info("Register Image Codec Plug-in: {}", codec.getCodecName());
    }
  }

  private static void registerSeriesViewerFactory(SeriesViewerFactory factory) {
    if (factory != null && !UIManager.SERIES_VIEWER_FACTORIES.contains(factory)) {
      UIManager.SERIES_VIEWER_FACTORIES.add(factory);
      LOGGER.info("Register series viewer plug-in: {}", factory.getDescription());
    }
  }

  private static void registerCommands(BundleContext context) {
    Dictionary<String, Object> dict = new Hashtable<>();
    dict.put(CommandProcessor.COMMAND_SCOPE, "image");
    dict.put(CommandProcessor.COMMAND_FUNCTION, AbstractFileModel.functions.toArray(new String[0]));
    context.registerService(FileModel.class.getName(), ViewerPluginBuilder.DefaultDataModel, dict);
  }

  private static void initLoggerAndAudit(BundleContext bundleContext) throws IOException {
    // Audit log for giving statistics about usage of Weasis
    String loggerKey = "audit.log";
    String[] loggerVal = new String[] {"org.weasis.core.api.service.AuditLog"};
    // Activate audit log by adding an entry "audit.log=true" in Weasis.
    if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(loggerKey, false)) {
      AuditLog.createOrUpdateLogger(
          bundleContext,
          loggerKey,
          loggerVal,
          "DEBUG",
          AppProperties.WEASIS_PATH
              + File.separator
              + "log"
              + File.separator
              + "audit-" // NON-NLS
              + AppProperties.WEASIS_USER
              + ".log",
          "{0,date,dd.MM.yyyy HH:mm:ss.SSS} *{4}* {5}", // NON-NLS
          null,
          null,
          "0");
      AuditLog.LOGGER.info("Start audit log session");
    } else {
      ServiceReference<ConfigurationAdmin> configurationAdminReference =
          bundleContext.getServiceReference(ConfigurationAdmin.class);
      if (configurationAdminReference != null) {
        ConfigurationAdmin confAdmin = bundleContext.getService(configurationAdminReference);
        if (confAdmin != null) {
          Configuration logConfiguration =
              AuditLog.getLogConfiguration(confAdmin, loggerKey, loggerVal[0]);
          if (logConfiguration == null) {
            logConfiguration =
                confAdmin.createFactoryConfiguration(
                    "org.apache.sling.commons.log.LogManager.factory.config", null);
            Dictionary<String, Object> loggingProperties = new Hashtable<>();
            loggingProperties.put("org.apache.sling.commons.log.level", "ERROR"); // NON-NLS
            loggingProperties.put("org.apache.sling.commons.log.names", loggerVal);
            // add this property to give us something unique to re-find this configuration
            loggingProperties.put(loggerKey, loggerVal[0]);
            logConfiguration.update(loggingProperties);
          } else {
            Dictionary loggingProperties = logConfiguration.getProperties();
            loggingProperties.remove(AuditLog.LOG_FILE);
            logConfiguration.update(loggingProperties);
          }
        }
      }
    }
  }
}
