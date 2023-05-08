/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.model.graphic.imp.angle;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import org.weasis.core.api.service.WProperties;
import org.weasis.core.ui.model.graphic.Graphic;
import org.weasis.core.ui.test.testers.GraphicTester;

public class CobbAngleToolGraphicTest extends GraphicTester<CobbAngleToolGraphic> {
  private static final String XML_0 = "/graphic/cobbAngle/cobbAngle.graphic.0.xml"; // NON-NLS
  private static final String XML_1 = "/graphic/cobbAngle/cobbAngle.graphic.1.xml"; // NON-NLS

  public static final String BASIC_TPL =
      "<cobbAngle fill=\"%s\" showLabel=\"%s\" thickness=\"%s\" uuid=\"%s\">" // NON-NLS
          + "<paint rgb=\"%s\"/>" // NON-NLS
          + "<pts/>" // NON-NLS
          + "</cobbAngle>"; // NON-NLS

  public static final CobbAngleToolGraphic COMPLETE_OBJECT = new CobbAngleToolGraphic();

  static {
    COMPLETE_OBJECT.setUuid(GRAPHIC_UUID_1);

    List<Point2D> pts =
        Arrays.asList(
            new Point2D.Double(1770.5, 1435.0),
            new Point2D.Double(1765.5, 1445.0),
            new Point2D.Double(1611.5, 1590.0),
            new Point2D.Double(1575.5, 1589.0),
            new Point2D.Double(1637.1255967238944, 1500.8183122461598));
    COMPLETE_OBJECT.setPts(pts);
  }

  @Override
  public String getTemplate() {
    return BASIC_TPL;
  }

  @Override
  public Object[] getParameters() {
    return new Object[] {
      Graphic.DEFAULT_FILLED,
      Graphic.DEFAULT_LABEL_VISIBLE,
      Graphic.DEFAULT_LINE_THICKNESS,
      getGraphicUuid(),
      WProperties.color2Hexadecimal(Graphic.DEFAULT_COLOR, true)
    };
  }

  @Override
  public String getXmlFilePathCase0() {
    return XML_0;
  }

  @Override
  public String getXmlFilePathCase1() {
    return XML_1;
  }

  @Override
  public CobbAngleToolGraphic getExpectedDeserializeCompleteGraphic() {
    return COMPLETE_OBJECT;
  }
}
