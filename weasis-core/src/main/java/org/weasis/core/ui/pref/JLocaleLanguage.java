/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.pref;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.JComboBox;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.api.util.LocalUtil;

public class JLocaleLanguage extends JComboBox<JLocale> implements ItemListener, Refreshable {

  private final ArrayList<Locale> languages = new ArrayList<>();

  public JLocaleLanguage() {
    super();
    initLocales();
    sortLocales();
    addItemListener(this);
  }

  private void initLocales() {
    String langs = System.getProperty("weasis.languages", null);
    if (langs != null) {
      String[] items = langs.split(",");
      for (String s : items) {
        String item = s.trim();
        int index = item.indexOf(' ');
        Locale l = LocalUtil.textToLocale(index > 0 ? item.substring(0, index) : item);
        if (l != null) {
          languages.add(l);
        }
      }
    }
    if (languages.isEmpty()) {
      languages.add(Locale.ENGLISH);
    }
  }

  private void sortLocales() {
    Locale defaultLocale = Locale.getDefault();
    // Allow sorting correctly string in each language
    final Collator collator = Collator.getInstance(defaultLocale);
    languages.sort((l1, l2) -> collator.compare(l1.getDisplayName(), l2.getDisplayName()));

    JLocale dloc = null;
    for (Locale locale : languages) {
      JLocale val = new JLocale(locale);
      if (locale.equals(defaultLocale)) {
        dloc = val;
      }
      addItem(val);
    }
    if (dloc != null) {
      this.setSelectedItem(dloc);
    }
  }

  public void selectLocale(String locale) {
    Locale sLoc = LocalUtil.textToLocale(locale);
    Object item = getSelectedItem();
    if (item instanceof JLocale jLocale && sLoc.equals(jLocale.getLocale())) {
      return;
    }

    int defaultIndex = -1;
    for (int i = 0; i < getItemCount(); i++) {
      Locale l = getItemAt(i).getLocale();
      if (l.equals(sLoc)) {
        defaultIndex = i;
        break;
      }
      if (l.equals(Locale.ENGLISH)) {
        defaultIndex = i;
      }
    }
    setSelectedIndex(defaultIndex);
  }

  @Override
  public void itemStateChanged(ItemEvent iEvt) {
    if (iEvt.getStateChange() == ItemEvent.SELECTED) {
      Object item = getSelectedItem();
      if (item instanceof JLocale jLocale) {
        removeItemListener(this);
        Locale locale = jLocale.getLocale();
        Locale.setDefault(locale);
        BundleTools.SYSTEM_PREFERENCES.setProperty(
            "locale.lang.code", LocalUtil.localeToText(locale));
        removeAllItems();
        sortLocales();
        addItemListener(this);
        firePropertyChange("locale", null, locale);
        valueHasChanged();
      }
    }
  }
}
