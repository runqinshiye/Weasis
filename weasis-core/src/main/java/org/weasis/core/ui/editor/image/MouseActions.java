/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.editor.image;

import java.awt.event.InputEvent;
import org.osgi.service.prefs.Preferences;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.service.AuditLog;
import org.weasis.core.api.service.BundlePreferences;
import org.weasis.core.api.service.BundleTools;

public class MouseActions {

  public static final int SCROLL_MASK = 1 << 1;

  public static final String PREFERENCE_NODE = "mouse.action";
  public static final String P_MOUSE_LEFT = "mouse_left"; // NON-NLS
  public static final String P_MOUSE_MIDDLE = "mouse_middle"; // NON-NLS
  public static final String P_MOUSE_RIGHT = "mouse_right"; // NON-NLS
  public static final String P_MOUSE_WHEEL = "mouse_wheel"; // NON-NLS

  public static final String T_LEFT = "left"; // NON-NLS
  public static final String T_MIDDLE = "middle"; // NON-NLS
  public static final String T_RIGHT = "right"; // NON-NLS
  public static final String T_WHEEL = "wheel"; // NON-NLS

  private String left;
  private String middle;
  private String right;
  private String wheel;
  private int activeButtons;

  public MouseActions(Preferences prefs) {
    this.activeButtons =
        BundleTools.SYSTEM_PREFERENCES.getIntProperty(
            "weasis.toolbar.mouse.buttons",
            InputEvent.BUTTON1_DOWN_MASK
                | InputEvent.BUTTON2_DOWN_MASK
                | InputEvent.BUTTON3_DOWN_MASK
                | SCROLL_MASK);
    this.left =
        BundleTools.SYSTEM_PREFERENCES.getProperty(
            "weasis.toolbar.mouse.left", ActionW.WINLEVEL.cmd());
    this.middle =
        BundleTools.SYSTEM_PREFERENCES.getProperty(
            "weasis.toolbar.mouse.middle", ActionW.PAN.cmd());
    this.right =
        BundleTools.SYSTEM_PREFERENCES.getProperty(
            "weasis.toolbar.mouse.right", ActionW.CONTEXTMENU.cmd());
    this.wheel =
        BundleTools.SYSTEM_PREFERENCES.getProperty(
            "weasis.toolbar.mouse.wheel", ActionW.SCROLL_SERIES.cmd());

    applyPreferences(prefs);

    int numberOfButtons = java.awt.MouseInfo.getNumberOfButtons();
    if (numberOfButtons < 3) {
      // Invalidate middle button click when doesn't exist (OK with genuine Mac mouses)
      this.activeButtons &= ~(1 << 11);
    }
  }

  public String getWheel() {
    return wheel;
  }

  public void setWheel(String wheel) {
    this.wheel = wheel;
  }

  @Override
  public String toString() {
    return left + "/" + right + "/" + wheel;
  }

  public String getLeft() {
    return left;
  }

  public void setLeft(String left) {
    this.left = left;
  }

  public String getRight() {
    return right;
  }

  public void setRight(String right) {
    this.right = right;
  }

  public String getMiddle() {
    return middle;
  }

  public void setMiddle(String middle) {
    this.middle = middle;
  }

  public String getAction(String type) {
    if (MouseActions.T_LEFT.equals(type)) {
      return left;
    } else if (MouseActions.T_MIDDLE.equals(type)) {
      return middle;
    } else if (MouseActions.T_RIGHT.equals(type)) {
      return right;
    } else if (MouseActions.T_WHEEL.equals(type)) {
      return wheel;
    }
    return null;
  }

  public int getActiveButtons() {
    return activeButtons;
  }

  public void setActiveButtons(int activeButtons) {
    this.activeButtons = activeButtons;
  }

  public void setAction(String type, String action) {
    if (MouseActions.T_LEFT.equals(type)) {
      setLeft(action);
    } else if (MouseActions.T_MIDDLE.equals(type)) {
      setMiddle(action);
    } else if (MouseActions.T_RIGHT.equals(type)) {
      setRight(action);
    } else if (MouseActions.T_WHEEL.equals(type)) {
      setWheel(action);
    }
    AuditLog.LOGGER.info("mouse:{} action:{}", type, action);
  }

  public void applyPreferences(Preferences prefs) {
    if (prefs != null) {
      Preferences p = prefs.node(MouseActions.PREFERENCE_NODE);
      left = p.get(P_MOUSE_LEFT, left);
      middle = p.get(P_MOUSE_MIDDLE, middle);
      right = p.get(P_MOUSE_RIGHT, right);
      wheel = p.get(P_MOUSE_WHEEL, wheel);
    }
  }

  public void savePreferences(Preferences prefs) {
    if (prefs != null) {
      Preferences p = prefs.node(MouseActions.PREFERENCE_NODE);
      BundlePreferences.putStringPreferences(p, P_MOUSE_LEFT, left);
      BundlePreferences.putStringPreferences(p, P_MOUSE_MIDDLE, middle);
      BundlePreferences.putStringPreferences(p, P_MOUSE_RIGHT, right);
      BundlePreferences.putStringPreferences(p, P_MOUSE_WHEEL, wheel);
    }
  }
}
