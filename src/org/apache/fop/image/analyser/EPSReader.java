/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */ 
package org.apache.fop.image.analyser;

// Java
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// FOP
import org.apache.fop.image.FopImage;
import org.apache.fop.image.EPSImage;
import org.apache.fop.fo.FOUserAgent;

/**
 * ImageReader object for EPS document image type.
 *
 * @version   $Id$
 */
public class EPSReader implements ImageReader {

    private static final byte[] EPS_HEADER_ASCII = "%!PS".getBytes();
    private static final byte[] BOUNDINGBOX = "%%BoundingBox: ".getBytes();

    /** @see org.apache.fop.image.analyser.ImageReader */
    public FopImage.ImageInfo verifySignature(String uri, InputStream bis,
                FOUserAgent ua) throws IOException {

        boolean isEPS = false;

        bis.mark(32);
        byte[] header = new byte[30];
        bis.read(header, 0, 30);
        bis.reset();

        EPSImage.EPSData data = new EPSImage.EPSData();

        // Check if binary header
        if (getLong(header, 0) == 0xC6D3D0C5) {
            data.isAscii = false;
            isEPS = true;

            data.psStart = getLong(header, 4);
            data.psLength = getLong(header, 8);
            data.wmfStart = getLong(header, 12);
            data.wmfLength = getLong(header, 16);
            data.tiffStart = getLong(header, 20);
            data.tiffLength = getLong(header, 24);

        } else {
            // Check if plain ascii
            byte[] epsh = "%!PS".getBytes();
            if (EPS_HEADER_ASCII[0] == header[0]
                    && EPS_HEADER_ASCII[1] == header[1]
                    && EPS_HEADER_ASCII[2] == header[2]
                    && EPS_HEADER_ASCII[3] == header[3]) {
                data.isAscii = true;
                isEPS = true;
            }
        }

        if (isEPS) {
            FopImage.ImageInfo info = new FopImage.ImageInfo();
            info.mimeType = getMimeType();
            info.data = data;
            readEPSImage(bis, data);
            data.bbox = readBBox(data);

            if (data.bbox != null) {
                info.width = (int) (data.bbox[2] - data.bbox[0]);
                info.height = (int) (data.bbox[3] - data.bbox[1]);

                // image data read
                bis.close();
                info.inputStream = null;

                return info;
            } else {
                // Ain't eps if no BoundingBox
                isEPS = false;
            }
        }

        return null;
    }

    /**
     * Returns the MIME type supported by this implementation.
     *
     * @return   The MIME type
     */
    public String getMimeType() {
        return "image/eps";
    }

    private long getLong(byte[] buf, int idx) {
        int b1 = buf[idx] & 0xff;
        int b2 = buf[idx + 1] & 0xff;
        int b3 = buf[idx + 2] & 0xff;
        int b4 = buf[idx + 3] & 0xff;

        return (long) ((b4 << 24) | (b3 << 16) | (b2 << 8) | b1);
    }

    /**
     * Read the eps file and extract eps part.
     *
     * @param bis              The InputStream
     * @param data             EPSData object to write the results to
     * @exception IOException  If an I/O error occurs
     */
    private void readEPSImage(InputStream bis, EPSImage.EPSData data)
                throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] file;
        byte[] readBuf = new byte[20480];
        int bytesRead;
        int index = 0;
        boolean cont = true;

        try {
            while ((bytesRead = bis.read(readBuf)) != -1) {
                baos.write(readBuf, 0, bytesRead);
            }
        } catch (java.io.IOException ex) {
            throw new IOException("Error while loading EPS image: "
                    + ex.getMessage());
        }

        file = baos.toByteArray();

        if (data.isAscii) {
            data.rawEps = null;
            data.epsFile = new byte[file.length];
            System.arraycopy(file, 0, data.epsFile, 0, data.epsFile.length);
        } else {
            data.rawEps = new byte[file.length];
            data.epsFile = new byte[(int) data.psLength];
            System.arraycopy(file, 0, data.rawEps, 0, data.rawEps.length);
            System.arraycopy(data.rawEps, (int) data.psStart, data.epsFile, 0,
                    (int) data.psLength);
        }
    }

    /**
     * Get embedded TIFF preview or null.
     *
     * @param data  The EPS payload
     * @return      The embedded preview
     */
    public byte[] getPreview(EPSImage.EPSData data) {
        if (data.preview == null) {
            if (data.tiffLength > 0) {
                data.preview = new byte[(int) data.tiffLength];
                System.arraycopy(data.rawEps, (int) data.tiffStart, data.preview, 0,
                        (int) data.tiffLength);
            }
        }
        return data.preview;
    }

    /**
     * Extract bounding box from eps part.
     *
     * @param data  The EPS payload
     * @return      An Array of four coordinates making up the bounding box
     */
    private long[] readBBox(EPSImage.EPSData data) {
        long[] mbbox = null;
        int idx = 0;
        boolean found = false;

        while (!found && (data.epsFile.length > (idx + BOUNDINGBOX.length))) {
            boolean sfound = true;
            int i = idx;
            for (i = idx; sfound && (i - idx) < BOUNDINGBOX.length; i++) {
                if (BOUNDINGBOX[i - idx] != data.epsFile[i]) {
                    sfound = false;
                }
            }
            if (sfound) {
                found = true;
                idx = i;
            } else {
                idx++;
            }
        }

        if (!found) {
            return mbbox;
        }

        mbbox = new long[4];
        idx += readLongString(data, mbbox, 0, idx);
        idx += readLongString(data, mbbox, 1, idx);
        idx += readLongString(data, mbbox, 2, idx);
        idx += readLongString(data, mbbox, 3, idx);

        return mbbox;
    }

    private int readLongString(EPSImage.EPSData data, long[] mbbox, int i, int idx) {
        while (idx < data.epsFile.length && (data.epsFile[idx] == 32)) {
            idx++;
        }

        int nidx = idx;

        // check also for ANSI46(".") to identify floating point values
        while (nidx < data.epsFile.length
                && ((data.epsFile[nidx] >= 48 && data.epsFile[nidx] <= 57)
                || (data.epsFile[nidx] == 45)
                || (data.epsFile[nidx] == 46))) {
            nidx++;
        }

        byte[] num = new byte[nidx - idx];
        System.arraycopy(data.epsFile, idx, num, 0, nidx - idx);
        String ns = new String(num);

        //if( ns.indexOf(".") != -1 ) {
        // do something like logging a warning
        //}

        // then parse the double and round off to the next math. Integer
        mbbox[i] = (long) Math.ceil(Double.parseDouble(ns));

        return (1 + nidx - idx);
    }

}

