/*
 * @copyright Copyright (c) 2018 Animati Sistemas de Informática Ltda. (http://www.animati.com.br)
 */

package br.com.animati.texture.codec.loader;

import java.awt.image.DataBufferByte;

/**
 * Math for Bytes (conversions needed to send to texture-dicom lib).
 *
 * @author Gabriela Bauermann (gabriela@animati.com.br)
 * @version 2018, Sep 28.
 */
public class DataBytesMath {

    private DataBytesMath() {
    }

    /**
     * To use in color images: changes the chanel order (from BGR usen in OpenCv to RBG for texturedicom).
     *
     * @param dataBuffer Original dataBuffer from Opencv color image.
     * @return New byte array.
     */
    public static byte[] bufferBytetoBytesArray(final DataBufferByte dataBuffer) {
        byte[] bytesOut = dataBuffer.getData();

        // Conversion from BGR to RGB - needed for OpenCV versio only
        for (int i = 0; i < bytesOut.length; i += 3) {
            byte oldR = bytesOut[i];
            bytesOut[i] = bytesOut[i + 2];
            bytesOut[i + 2] = oldR;
        }
        //////

        return bytesOut;
    }

    /**
     * Converts a short array (of more than 8-bits) to a byes array.
     * The results length is "2 x" the original array length.
     *
     * @param data Original data from image data-buffer.
     * @return New byte array.
     */
    public static byte[] shortArrayToByteArray(final short[] data) {
        byte[] bytesOut = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            bytesOut[i * 2] = (byte) (data[i] & 0xFF);
            bytesOut[i * 2 + 1] = (byte) ((data[i] >>> 8) & 0xFF);
        }
        return bytesOut;
    }

    /**
     * Converts a short array (of 8-bits) to a byes array.
     *
     * @param data Original data from image data-buffer.
     * @return New byte array.
     */
    public static byte[] short8ArrayToByteArray(final short[] data) {
        byte[] bytesOut = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            bytesOut[i] = (byte) data[i];
        }
        return bytesOut;
    }

}
