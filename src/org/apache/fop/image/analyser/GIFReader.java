/*
 * $Id$
 * Copyright (C) 2001-2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */
package org.apache.fop.image.analyser;

// Java
import java.io.BufferedInputStream;
import java.io.IOException;

// FOP
import org.apache.fop.image.FopImage;
import org.apache.fop.fo.FOUserAgent;

/**
 * ImageReader object for GIF image type.
 *
 * @author    Pankaj Narula
 * @version   $Id$
 */
public class GIFReader implements ImageReader {

    private static final int GIF_SIG_LENGTH = 10;

    /** @see org.apache.fop.image.analyser.ImageReader */
    public FopImage.ImageInfo verifySignature(String uri, BufferedInputStream bis,
                FOUserAgent ua) throws IOException {
        byte[] header = getDefaultHeader(bis);
        boolean supported = ((header[0] == 'G')
                && (header[1] == 'I')
                && (header[2] == 'F')
                && (header[3] == '8')
                && (header[4] == '7' || header[4] == '9')
                && (header[5] == 'a'));
        if (supported) {
            FopImage.ImageInfo info = getDimension(header);
            info.mimeType = getMimeType();
            return info;
        } else {
            return null;
        }
    }

    /**
     * Returns the MIME type supported by this implementation.
     *
     * @return   The MIME type
     */
    public String getMimeType() {
        return "image/gif";
    }

    private FopImage.ImageInfo getDimension(byte[] header) {
        FopImage.ImageInfo info = new FopImage.ImageInfo();
        // little endian notation
        int byte1 = header[6] & 0xff;
        int byte2 = header[7] & 0xff;
        info.width = ((byte2 << 8) | byte1) & 0xffff;

        byte1 = header[8] & 0xff;
        byte2 = header[9] & 0xff;
        info.height = ((byte2 << 8) | byte1) & 0xffff;
        return info;
    }

    private byte[] getDefaultHeader(BufferedInputStream imageStream)
                throws IOException {
        byte[] header = new byte[GIF_SIG_LENGTH];
        try {
            imageStream.mark(GIF_SIG_LENGTH + 1);
            imageStream.read(header);
            imageStream.reset();
        } catch (IOException ex) {
            try {
                imageStream.reset();
            } catch (IOException exbis) {
                // throw the original exception, not this one
            }
            throw ex;
        }
        return header;
    }

}