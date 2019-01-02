/*
 * @copyright Copyright (c) 2019 Animati Sistemas de Informática Ltda. (http://www.animati.com.br)
 */
package br.com.animati.texture.codec;

import br.com.animati.texture.codec.loader.GeometryLoaderMath;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.vecmath.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.image.util.Unit;

/**
 *
 * @author Gabriela Bauermann (gabriela@animati.com.br)
 * @version 2019, 02 Jan.
 */
public class TextureGeometry {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextureGeometry.class);

    private static final String STRING_NULL = "Null";
    private static final NumberFormat DF3 = NumberFormat.getNumberInstance(Locale.US);
    private static final int Z_SPACING_PRECISION = 3; // 3 decimals

    static {
        DF3.setMaximumFractionDigits(Z_SPACING_PRECISION);
    }


    private double[] firstAcqPixSpacing;
    private boolean isUnknown = false;
    private boolean isVariable = false;
    private Map<String, Integer> zSpacings;
    private double[] originalSeriesOrientationPatient;
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

    /**
     * Stores an occurrence of the given slice-spacing om a {@code Map<String, Integer>}.
     *
     * Uses a String conversion of 3-decimals to limit the tolerance to 0.001, like the MPR of weasis.dicom.view2d.
     *
     * @param space Spacing value to add.
     */
    public void addZSpacingOccurence(final Double space) {
        if (zSpacings == null) {
            zSpacings = new HashMap<>();
        }
        String sp = asString(space);
        Integer number = zSpacings.get(sp);
        if (number == null) {
            zSpacings.put(sp, 1);
            LOGGER.info("Found new z-spacing value: " + sp);
        } else {
            zSpacings.put(sp, number + 1);
        }
    }

    private String asString(final Double value) {
        if (value != null) {
            return DF3.format(value);
        }
        return STRING_NULL;
    }

    private Double asDouble(final String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    /**
     * @return True if the original series has known and regular slice-spacing.
     */
    public boolean isSliceSpacingRegular() {
        if (zSpacings != null && zSpacings.size() == 1) {
            String[] toArray = zSpacings.keySet().toArray(new String[1]);
            Double value = asDouble(toArray[0]);
            return value != null;
        }
        return false;
    }

    /**
     * @return True if it has any occurrence of negative slice spacing.
     */
    public boolean hasNegativeSliceSpacing() {
        if (zSpacings != null && !zSpacings.isEmpty()) {
            Iterator<String> iterator = zSpacings.keySet().iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                Double val = asDouble(next);
                if (val != null && val < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Consults the slice-spacing map to get the most common one. If there are two or more spacing with the same higher
     * occurrence value, the first one found is returned. If no value if found, returns zero.
     *
     * @return The most common slice spacing. Can be negative!
     */
    public double getMostCommonSpacing() {
        if (zSpacings == null || zSpacings.isEmpty()) {
            return 0;
        }
        if (zSpacings.size() == 1) {
            String[] toArray = zSpacings.keySet().toArray(new String[1]);
            Double val = asDouble(toArray[0]);
            if (val == null) {
                return 0;
            }
            return asDouble(toArray[0]);
        }
        String[] toArray = zSpacings.keySet().toArray(new String[zSpacings.size()]);
        String maxKey = toArray[0];
        for (int i = 1; i < toArray.length; i++) {
            if (zSpacings.get(maxKey) < zSpacings.get(toArray[i])) {
                maxKey = toArray[i];
            }
        }
        Double val = asDouble(maxKey);
        if (val == null) {
            return 0;
        }
        return asDouble(maxKey);
    }

    /**
     * Calculates DimentionMultiplier vector, based on actual values of pixel-spaces and distance between frames.
     *
     * @return Calculated dimension multiplier vector.
     */
    public Vector3d getDimensionMultiplier() {
        double[] pixSp = firstAcqPixSpacing;
        if (pixSp == null) {
            pixSp = new double[]{1, 1};
        }
        double zSp = getMostCommonSpacing();
        return GeometryLoaderMath.getNormalizedVector(pixSp[0], pixSp[1], Math.abs(zSp));
    }


    /**
     * Valid if has 6 double s. Set to a double[] of one element to make not-valid.
     *
     * @param imOri OrientationPatient to set
     */
    public void setOrientationPatient(final double[] imOri) {
        if (imOri == null) {
            originalSeriesOrientationPatient = null;
        } else {
            originalSeriesOrientationPatient = imOri.clone();
        }
    }

    /**
     * Valid if has 6 double s. Set to a double[] of one element to make not-valid.
     *
     * @return original SeriesOrientationPatient
     */
    public double[] getOriginalSeriesOrientationPatient() {
        if (originalSeriesOrientationPatient == null) {
            return null;
        }
        return originalSeriesOrientationPatient.clone();
    }



    public Unit getPixelSpacingUnit() {
        if (firstAcqPixSpacing != null) {
            return Unit.MILLIMETER;
        }
        return Unit.PIXEL;
    }

}
