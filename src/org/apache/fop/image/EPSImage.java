/*
 * $Id$
 * Copyright (C) 2001-2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.image;


/**
 * EPS image handler.
 * This handles the Encapulated PostScript images.
 * It gets the dimensions and original data from the analyser.
 *
 * @see AbstractFopImage
 * @see FopImage
 */
public class EPSImage extends AbstractFopImage {
    private String docName;
    private int[] bbox;

    private EPSData epsData = null;

    /**
     * Create an EPS image with the image information.
     *
     * @param imgInfo the information containing the data and bounding box
     */
    public EPSImage(FopImage.ImageInfo imgInfo) {
        super(imgInfo);
        init("");
        if (imgInfo.data instanceof EPSData) {
            epsData = (EPSData) imgInfo.data;
            bbox = new int[4];
            bbox[0] = (int) epsData.bbox[0];
            bbox[1] = (int) epsData.bbox[1];
            bbox[2] = (int) epsData.bbox[2];
            bbox[3] = (int) epsData.bbox[3];

            loaded = loaded | ORIGINAL_DATA;
        }
    }

    /**
     * Initialize docName and bounding box.
     * @param name the document name
     */
    private void init(String name) {
        bbox = new int[4];
        bbox[0] = 0;
        bbox[1] = 0;
        bbox[2] = 0;
        bbox[3] = 0;

        docName = name;
    }

    /**
     * Return the name of the eps
     * @return the name of the eps
     */
    public String getDocName() {
        return docName;
    }

    /**
     * Return the bounding box
     * @return an int array containing the bounding box
     */
    public int[] getBBox() {
        return bbox;
    }

    /**
     * Get the eps image.
     *
     * @return the original eps image data
     */
    public byte[] getEPSImage() {
        if (epsData.epsFile == null) {
            //log.error("ERROR LOADING EXTERNAL EPS");
        }
        return epsData.epsFile;
    }

    /**
     * Data for EPS image.
     */
    public static class EPSData {
        public long[] bbox;
        public boolean isAscii; // True if plain ascii eps file

        // offsets if not ascii
        public long psStart = 0;
        public long psLength = 0;
        public long wmfStart = 0;
        public long wmfLength = 0;
        public long tiffStart = 0;
        public long tiffLength = 0;

        /** raw eps file */
        public byte[] rawEps;
        /** eps part */
        public byte[] epsFile;
        public byte[] preview = null;
    }

}
