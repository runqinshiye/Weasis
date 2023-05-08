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

import java.awt.Point;
import org.weasis.core.api.gui.util.DecFormatter;
import org.weasis.core.api.image.util.Unit;
import org.weasis.core.ui.Messages;

/** User: boraldo Date: 28.01.14 Time: 15:02 */
public class PixelInfo {
  private double[] values;
  private String[] channelNames;
  private String pixelValueUnit;
  private Point position;
  private Unit pixelSpacingUnit;
  // Only display square pixel, so one value is enough.
  private Double pixelSize;

  public double[] getValues() {
    return values;
  }

  public void setValues(double[] values) {
    this.values = values;
  }

  public String[] getChannelNames() {
    return channelNames;
  }

  public void setChannelNames(String[] channelNames) {
    this.channelNames = channelNames;
  }

  public String getPixelValueUnit() {
    return pixelValueUnit;
  }

  public void setPixelValueUnit(String pixelValueUnit) {
    this.pixelValueUnit = pixelValueUnit;
  }

  public String getPixelValueText() {
    if (values != null) {
      if (values.length == 1) {
        StringBuilder text = new StringBuilder();
        text.append(DecFormatter.allNumber(values[0]));
        if (pixelValueUnit != null) {
          text.append(" ");
          text.append(pixelValueUnit);
        }
        return text.toString();
      } else if (values.length > 1) {
        // TODO add preference for Pixel value type (RGB, IHS...) and pixel position (pix, real)
        // float[] ihs = IHSColorSpace.getInstance().fromRGB(new float[]
        // {
        // c[0] / 255f, c[1] / 255f, c[2] / 255f
        // });
        // c[0] = (int) (ihs[0] * 255f);
        // c[1] = (int) ((ihs[1] / (Math.PI * 2)) * 255f);
        // c[2] = (int) (ihs[2] * 255f);
        //
        // message.append(" (I = " + c[0] + ", H = " + c[1] + ", S = " + c[2] + ")");

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
          text.append(" ");
          text.append(
              (channelNames == null || i >= channelNames.length)
                  ? Messages.getString("PixelInfo.unknown")
                  : channelNames[i].substring(0, 1));
          text.append("=");
          text.append(DecFormatter.twoDecimal(values[i]));
        }
        return text.toString();
      }
    }
    return Messages.getString("PixelInfo.no_val");
  }

  public Unit getPixelSpacingUnit() {
    return pixelSpacingUnit;
  }

  public void setPixelSpacingUnit(Unit pixelSpacingUnit) {
    this.pixelSpacingUnit = pixelSpacingUnit;
  }

  public Double getPixelSize() {
    return pixelSize;
  }

  public void setPixelSize(Double pixelSize) {
    this.pixelSize = pixelSize;
  }

  public Point getPosition() {
    return position;
  }

  public void setPosition(Point position) {
    this.position = position;
  }

  public String getPixelPositionText() {
    if (position == null) {
      return Messages.getString("DefaultView2d.out");
    }
    return "(" + position.x + "," + position.y + ")";
  }

  public String getRealPositionText() {
    if (position == null || pixelSpacingUnit == null || pixelSize == null) {
      return getPixelPositionText();
    }

    StringBuilder text = new StringBuilder("(");
    text.append(DecFormatter.twoDecimal(pixelSize * position.x));
    text.append(pixelSpacingUnit.getAbbreviation());
    text.append(",");
    text.append(DecFormatter.twoDecimal(pixelSize * position.y));
    text.append(pixelSpacingUnit.getAbbreviation());
    text.append(")");
    return text.toString();
  }
}
