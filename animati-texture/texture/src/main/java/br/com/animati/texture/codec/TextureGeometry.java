/*
 * @copyright Copyright (c) 2019 Animati Sistemas de Informática Ltda. (http://www.animati.com.br)
 */
package br.com.animati.texture.codec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.weasis.core.api.image.util.Unit;

/**
 *
 * @author Gabriela Bauermann (gabriela@animati.com.br)
 * @version 2019, 02 Jan.
 */
public class TextureGeometry {

    private double[] firstAcqPixSpacing;
    private boolean isUnknown = false;
    private boolean isVariable = false;
    private final Map<Integer, double[]> pixelSpacingMap = new HashMap<>();


    /**
     * Feed acquisition pixel values for this volume geometric calculations.
     *
     * @param place Place in texture.
     * @param pixSpacing New value of pixel spacing.
     * @return A message useful for logging.
     */
    public String submitAcquisitionPixelSpacing(int place, final double[] pixSpacing) {
        pixelSpacingMap.put(place, pixSpacing);
        return submitAcquisitionPixelSpacing(pixSpacing);
    }

    /**
     * Feed acquisition pixel values for this volume geometric calculations.
     *
     * @param pixSpacing New value of pixel spacing.
     * @return A message useful for logging.
     */
    private String submitAcquisitionPixelSpacing(final double[] pixSpacing) {
        if (isVariablePixelSpacing() || isUnknownPixelSpacing()) {
            return null; // Do nothing, we know is invalid.
        } else {
            if (pixSpacing == null || pixSpacing.length != 2) {
                setUnknownPixelSpacing(true);
                return "Found unknown pixel spacing";
            } else {
                double[] acqPixSpacing = getAcquisitionPixelSpacing();
                if (acqPixSpacing == null) { // Its first to submit
                    setAcquisitionPixelSpacing(pixSpacing);
                    return "First pixel spacing (" + pixSpacing[0] + ", " + pixSpacing[1] + ")";
                } else {
                    // Compare with actual, only to see if is different:
                    if (!Arrays.equals(acqPixSpacing, pixSpacing)) {
                        setVariablePixelSpacing(true);
                        return "Found variable pixel spacing";
                    }
                }
            }
        }
        return null;
    }


    protected void setAcquisitionPixelSpacing(final double[] pixSpacing) {
        if (pixSpacing == null) {
            firstAcqPixSpacing = null;
        } else {
            firstAcqPixSpacing = pixSpacing.clone();
        }
    }

    /**
     * Returns the first pixel spacing value of acquisition plane for the volume, or null. This value may not be
     * true for the entire value: always check uniqueness using isVariablePixelSpacing() method.
     *
     * @return First pixel spacing value of acquisition plane for the volume, or null.
     */
    public double[] getAcquisitionPixelSpacing() {
        if (firstAcqPixSpacing == null) {
            return null;
        }
        return firstAcqPixSpacing.clone();
    }

    /**
     * Acquisition Pixel Spacing from element on given place.
     *
     * @param place Given place on texture.
     * @return Pixel spacing value, or null.
     */
    public double[] getAcquisitionPixelSpacing(int place) {
        return pixelSpacingMap.get(place);
    }

    public boolean isVariablePixelSpacing() {
        return isVariable;
    }

    public boolean isUnknownPixelSpacing() {
        return isUnknown;
    }

    public void setUnknownPixelSpacing(boolean unknown) {
        isUnknown = unknown;
    }

    public void setVariablePixelSpacing(boolean variable) {
        isVariable = variable;
    }


    public Unit getPixelSpacingUnit() {
        if (firstAcqPixSpacing != null) {
            return Unit.MILLIMETER;
        }
        return Unit.PIXEL;
    }

}
