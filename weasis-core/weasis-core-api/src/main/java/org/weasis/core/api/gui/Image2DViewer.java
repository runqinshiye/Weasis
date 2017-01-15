/*******************************************************************************
 * Copyright (c) 2016 Weasis Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 *******************************************************************************/
package org.weasis.core.api.gui;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import org.opencv.core.Mat;
import org.weasis.core.api.gui.model.ViewModel;
import org.weasis.core.api.image.util.ImageLayer;
import org.weasis.core.api.image.util.MeasurableLayer;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.api.media.data.MediaSeries;

public interface Image2DViewer<E extends ImageElement> {

    MediaSeries<E> getSeries();

    int getFrameIndex();

    void drawLayers(Graphics2D g2d, AffineTransform transform, AffineTransform inverseTransform);

    ViewModel getViewModel();

    ImageLayer<E> getImageLayer();

    MeasurableLayer getMeasurableLayer();

    AffineTransform getAffineTransform();

    E getImage();

    Mat getSourceImage();

    Object getActionValue(String action);

}
