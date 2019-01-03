/*
 * @copyright Copyright (c) 2019 Animati Sistemas de Informática Ltda. (http://www.animati.com.br)
 */
package br.com.animati.texture.codec.loader;

import javax.vecmath.Vector3d;
import org.dcm4che3.data.Tag;
import org.weasis.core.api.media.data.TagReadable;
import org.weasis.dicom.codec.TagD;
import org.weasis.dicom.codec.geometry.ImageOrientation;

/**
 *
 * @author Gabriela Bauermann (gabriela@animati.com.br)
 * @version 2019, 02 Jan.
 */
public class GeometryLoaderMath {

    private GeometryLoaderMath() {
    }

    /**
     * Checks if two image orientation information represent the same orientation plane.
     *
     * @param v1 Orientation information 1.
     * @param v2 Orientation information 2.
     * @return true if they have the same orientation plane.
     */
    public static boolean isSameOrientation(double[] v1, double[] v2) {
        if (v1 != null && v1.length == 6 && v2 != null && v2.length == 6) {
            ImageOrientation.Label label1 = ImageOrientation.makeImageOrientationLabelFromImageOrientationPatient(v1[0], v1[1], v1[2],
                v1[3], v1[4], v1[5]);
            ImageOrientation.Label label2 = ImageOrientation.makeImageOrientationLabelFromImageOrientationPatient(v2[0], v2[1], v2[2],
                v2[3], v2[4], v2[5]);

            if (label1 != null && !label1.equals(ImageOrientation.Label.OBLIQUE)) {
                return label1.equals(label2);
            }
            // If oblique search if the plan has approximately the same orientation
            double[] postion1 = ImageOrientation.computeNormalVectorOfPlan(v1);
            double[] postion2 = ImageOrientation.computeNormalVectorOfPlan(v2);
            if (postion1 != null && postion2 != null) {
                double prod = postion1[0] * postion2[0] + postion1[1] * postion2[1] + postion1[2] * postion2[2];
                // A little tolerance
                if (prod > 0.95) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get pixel-spacing from any of the known possible tags: PixelSpacing, ImagerPixelSpacing or
     * NominalScannedPixelSpacing.
     *
     * @param elmt The image element.
     * @return Pixel Spacing array, or null.
     */
    public static double[] getPixelSpacingByTag(final TagReadable elmt) {
        double[] pixSp = TagD.getTagValue(elmt, Tag.PixelSpacing, double[].class);
        if (pixSp == null || pixSp.length != 2) {
            pixSp = TagD.getTagValue(elmt, Tag.ImagerPixelSpacing, double[].class);
        }
        if (pixSp == null || pixSp.length != 2) {
            pixSp = TagD.getTagValue(elmt, Tag.NominalScannedPixelSpacing, double[].class);
        }
        if (pixSp != null && pixSp.length == 2) {
            return pixSp;
        }
        return null;
    }


    /**
     * Obtains the right values to a valid dimendionMultiplier.
     *
     * - Change X and Y places to convert DICOM coordinates to texturedicom coordinates.
     * - Divide all by the smaller horizontal value (x or y), so best-fit works.
     *
     * @param xSp x-spacing.
     * @param ySp y-spacing.
     * @param zSp z-spacing.
     * @return A valid parameter for dimendionMultiplier.
     */
    public static Vector3d getNormalizedVector(final double xSp, final double ySp, final double zSp) {
        if (xSp <= 0 || ySp <= 0 || zSp <= 0) {
            return new Vector3d(1, 1, 1);
        }

        double min = ySp;

        // X and Y are in order diferent of DICOM order, at texturedicom.
        // By convention, we use ySp as the "main dimension", for scale.
        return new Vector3d(ySp / min, xSp / min, zSp / min);
    }


}
