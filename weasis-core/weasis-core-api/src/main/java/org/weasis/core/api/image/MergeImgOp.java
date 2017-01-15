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
package org.weasis.core.api.image;

import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.image.cv.ImageProcessor;

public class MergeImgOp extends AbstractOp {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergeImgOp.class);

    public static final String OP_NAME = "merge.img"; //$NON-NLS-1$

    /**
     * The second image for merging operation (Required parameter). Note: calling clearIOCache will remove the parameter
     * value.
     *
     * java.awt.image.RenderedImage value.
     */
    public static final String INPUT_IMG2 = "op.input.img.2"; //$NON-NLS-1$

    /**
     * Opacity of the top image (Optional parameter).
     *
     * Integer value. Default value is 255 (highest value => no transparency).
     */
    public static final String P_OPACITY = "opacity"; //$NON-NLS-1$

    public MergeImgOp() {
        setName(OP_NAME);
    }

    public MergeImgOp(MergeImgOp op) {
        super(op);
    }

    @Override
    public MergeImgOp copy() {
        return new MergeImgOp(this);
    }

    @Override
    public void process() throws Exception {
        Mat source = (Mat) params.get(Param.INPUT_IMG);
        Mat source2 = (Mat) params.get(INPUT_IMG2);
        Mat result = source;

        if (source2 != null) {
            Integer transparency = (Integer) params.get(P_OPACITY);
            result = ImageProcessor.combineTwoImages(source, source2, transparency == null ? 255 : transparency);
        }
        params.put(Param.OUTPUT_IMG, result);
    }

}
