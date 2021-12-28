/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.gui.util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultFormatterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.Messages;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.util.StringUtil;

public class GuiUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(GuiUtils.class);

  private GuiUtils() {
    super();
  }

  public static void setPreferredWidth(Component component, int width, int minWidth) {
    Dimension dim = component.getPreferredSize();
    dim.width = width;
    component.setPreferredSize(dim);
    dim = component.getMinimumSize();
    dim.width = minWidth;
    component.setMinimumSize(dim);
  }

  public static void setPreferredWidth(Component component, int width) {
    setPreferredWidth(component, width, 50);
  }

  public static void setPreferredHeight(Component component, int height) {
    Dimension dim = component.getPreferredSize();
    dim.height = height;
    component.setPreferredSize(dim);
    dim = component.getMinimumSize();
    dim.height = 50;
    component.setMinimumSize(dim);
  }

  public static void showCenterScreen(Window window) {
    try {
      Rectangle bound =
          GraphicsEnvironment.getLocalGraphicsEnvironment()
              .getDefaultScreenDevice()
              .getDefaultConfiguration()
              .getBounds();
      window.setLocation(
          bound.x + (bound.width - window.getWidth()) / 2,
          bound.y + (bound.height - window.getHeight()) / 2);
    } catch (Exception e) {
      LOGGER.error("Cannot center the window to the screen", e);
    }
    window.setVisible(true);
  }

  public static void showCenterScreen(Window window, Component parent) {
    if (parent == null) {
      showCenterScreen(window);
    } else {
      Dimension sSize = parent.getSize();
      Dimension wSize = window.getSize();
      Point p = parent.getLocationOnScreen();
      window.setLocation(
          p.x + ((sSize.width - wSize.width) / 2), p.y + ((sSize.height - wSize.height) / 2));
      window.setVisible(true);
    }
  }


  public static void addChangeListener(JSlider slider, ChangeListener listener) {
    ChangeListener[] listeners = slider.getChangeListeners();
    for (ChangeListener changeListener : listeners) {
      if (listener == changeListener) {
        return;
      }
    }
    slider.addChangeListener(listener);
  }

  private static void addCheckActionToJFormattedTextField(final JFormattedTextField textField) {
    textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check"); // NON-NLS
    textField
        .getActionMap()
        .put(
            "check", // NON-NLS
            new AbstractAction() {

              @Override
              public void actionPerformed(ActionEvent e) {
                try {
                  textField.commitEdit(); // so use it.
                  textField.postActionEvent(); // stop editing //for DefaultCellEditor
                } catch (java.text.ParseException pe) {
                  LOGGER.error(
                      "Exception when commit value in {}", textField.getClass().getName(), pe);
                }
                textField.setValue(textField.getValue());
              }
            });
  }

  public static void addCheckAction(final JFormattedTextField textField) {
    textField.setHorizontalAlignment(SwingConstants.RIGHT);
    addCheckActionToJFormattedTextField(textField);
  }

  public static void setNumberModel(JSpinner spin, int val, int min, int max, int delta) {
    spin.setModel(new SpinnerNumberModel(val < min ? min : Math.min(val, max), min, max, delta));
    final JFormattedTextField ftf = ((JSpinner.DefaultEditor) spin.getEditor()).getTextField();
    addCheckActionToJFormattedTextField(ftf);
  }

  public static void formatCheckAction(JSpinner spin) {
    final JFormattedTextField ftf = ((JSpinner.DefaultEditor) spin.getEditor()).getTextField();
    addCheckActionToJFormattedTextField(ftf);
  }

  public static Number getFormattedValue(JFormattedTextField textField) {
    AbstractFormatterFactory formatter = textField.getFormatterFactory();
    if (formatter instanceof DefaultFormatterFactory
        && textField
            .getFormatter()
            .equals(((DefaultFormatterFactory) formatter).getEditFormatter())) {
      try {
        // to be sure that the value is commit (by default it is when the JFormattedTextField losing
        // the focus)
        textField.commitEdit();
      } catch (ParseException pe) {
        LOGGER.error("Exception when commit value in {}", textField.getClass().getName(), pe);
      }
    }
    Number val = null;
    try {
      val = (Number) textField.getValue();
    } catch (Exception e) {
      LOGGER.error("Cannot get number form textField", e);
    }
    return val;
  }

  // A convenience method for creating menu items
  public static JMenuItem menuItem(
      String label, ActionListener listener, String command, int mnemonic, int acceleratorKey) {
    JMenuItem item = new JMenuItem(label);
    item.addActionListener(listener);
    item.setActionCommand(command);
    if (mnemonic != 0) {
      item.setMnemonic((char) mnemonic);
    }
    if (acceleratorKey != 0) {
      item.setAccelerator(KeyStroke.getKeyStroke(acceleratorKey, java.awt.Event.CTRL_MASK));
    }
    return item;
  }

  public static Dimension getSmallIconButtonSize() {
    return new Dimension(22, 22);
  }

  public static Dimension getBigIconButtonSize() {
    return new Dimension(34, 34);
  }

  public static Dimension getBigIconToogleButtonSize() {
    return new Dimension(30, 30);
  }

  public static JButton createHelpButton(final String topic, boolean small) {
    JButton jButtonHelp = new JButton();
    jButtonHelp.putClientProperty("JButton.buttonType", "help");
    jButtonHelp.addActionListener(
        e -> {
          try {
            GuiUtils.openInDefaultBrowser(
                jButtonHelp,
                new URL(BundleTools.SYSTEM_PREFERENCES.getProperty("weasis.help.online") + topic));
          } catch (MalformedURLException e1) {
            LOGGER.error("Cannot open online help", e1);
          }
        });

    return jButtonHelp;
  }

  public static int getMaxLength(Rectangle bounds) {
    if (bounds.width < bounds.height) {
      return bounds.height;
    }
    return bounds.width;
  }

  public static void addTooltipToComboList(final JComboBox<?> combo) {
    Object comp = combo.getUI().getAccessibleChild(combo, 0);
    if (comp instanceof BasicComboPopup) {
      final BasicComboPopup popup = (BasicComboPopup) comp;
      popup
          .getList()
          .getSelectionModel()
          .addListSelectionListener(
              e -> {
                if (!e.getValueIsAdjusting()) {
                  ListSelectionModel model = (ListSelectionModel) e.getSource();
                  int first = model.getMinSelectionIndex();
                  if (first >= 0) {
                    Object item = combo.getItemAt(first);
                    ((JComponent) combo.getRenderer()).setToolTipText(item.toString());
                  }
                }
              });
    }
  }

  public static void openInDefaultBrowser(Component parent, URL url) {
    if (url != null) {
      if (AppProperties.OPERATING_SYSTEM.startsWith("linux")) { // NON-NLS
        try {
          String cmd = String.format("xdg-open %s", url); // NON-NLS
          Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
          LOGGER.error("Cannot open URL to the system browser", e);
        }
      } else if (Desktop.isDesktopSupported()) {
        final Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
          try {
            desktop.browse(url.toURI());
          } catch (IOException | URISyntaxException e) {
            LOGGER.error("Cannot open URL to the desktop browser", e);
          }
        }
      } else {
        JOptionPane.showMessageDialog(
            parent,
            Messages.getString("JMVUtils.browser") + StringUtil.COLON_AND_SPACE + url,
            Messages.getString("JMVUtils.error"),
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  public static HyperlinkListener buildHyperlinkListener() {
    return e -> {
      JTextPane pane = (JTextPane) e.getSource();
      if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
        pane.setToolTipText(e.getDescription());
      } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
        pane.setToolTipText(null);
      } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        Component parent = e.getSource() instanceof Component ? (Component) e.getSource() : null;
        openInDefaultBrowser(parent, e.getURL());
      }
    };
  }

  public static void addItemToMenu(JMenu menu, JMenuItem item) {
    if (menu != null && item != null) {
      menu.add(item);
    }
  }

  public static void addItemToMenu(JPopupMenu menu, Component item) {
    if (menu != null && item != null) {
      menu.add(item);
    }
  }
}
