/*
 * @copyright Copyright (c) 2019 Animati Sistemas de Informática Ltda. (http://www.animati.com.br)
 */
package br.com.animati.texture.codec;

import java.util.Arrays;
import java.util.Objects;
import org.weasis.core.api.image.util.Unit;

/**
 *
 * @author Gabriela Bauermann (gabriela@animati.com.br)
 * @version 2019, 02 Jan.
 */
public class PixelSize {

    private final double[] pixelSpacing;
    private final Unit pixelSpacingUnit;

    /**
     * Creates an Empty pixel size (size 1; unit PIXEL).
     */
    public PixelSize() {
        this(null, null);
    }

    /**
     * Creates a Pixel size with the given parameters.
     *
     * @param spacing Pixel spacing array.
     * @param unit Pixel scaling unit.
     */
    public PixelSize(final double[] spacing, final Unit unit) {
        if (spacing == null) {
            pixelSpacing = new double[] {1.0};
        } else {
            pixelSpacing = spacing.clone();
        }
        if (unit == null) {
            pixelSpacingUnit = Unit.PIXEL;
        } else {
            pixelSpacingUnit = unit;
        }
    }

    public double[] getPixelSpacing() {
        return pixelSpacing.clone();
    }

    public Unit getPixelSpacingUnit() {
        return pixelSpacingUnit;
    }

    /**
     * @return Squared pixel size, as double.
     */
    public double getPixelSize() {
        if (pixelSpacing.length == 1) {
            return pixelSpacing[0];
        }
        // Scale is the Y, which is X on texture-dicom geometry model.
        return pixelSpacing[1];
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PixelSize) {
            PixelSize other = (PixelSize) obj;
            Unit uns = other.getPixelSpacingUnit();
            if (!uns.equals(getPixelSpacingUnit())) {
                return false;
            }
            if (Arrays.equals(getPixelSpacing(), other.getPixelSpacing())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Arrays.hashCode(this.pixelSpacing);
        hash = 23 * hash + Objects.hashCode(this.pixelSpacingUnit);
        return hash;
    }

    @Override
    public String toString() {
        return "PixelSize:" + Arrays.toString(pixelSpacing) + "/Unit:" + pixelSpacingUnit;
    }


}
