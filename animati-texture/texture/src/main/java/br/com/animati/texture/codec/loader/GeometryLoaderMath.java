/*
 * @copyright Copyright (c) 2019 Animati Sistemas de Informática Ltda. (http://www.animati.com.br)
 */
package br.com.animati.texture.codec.loader;

import javax.vecmath.Vector3d;

/**
 *
 * @author Gabriela Bauermann (gabriela@animati.com.br)
 * @version 2019, 02 Jan.
 */
public class GeometryLoaderMath {

    private GeometryLoaderMath() {
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
