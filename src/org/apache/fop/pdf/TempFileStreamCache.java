/*
 * $Id$
 * Copyright (C) 2002-2003 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.pdf;

import org.apache.fop.util.StreamUtilities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;

/**
 * StreamCache implementation that uses temporary files rather than heap.
 */
public class TempFileStreamCache extends StreamCache {

    /**
     * The current output stream.
     */
    private BufferedOutputStream output;

    /**
     * The temp file.
     */
    private File tempFile;

    /**
     * Creates a new TempFileStreamCache.
     *
     * @throws IOException if there is an IO error
     */
    public TempFileStreamCache() throws IOException {
        tempFile = File.createTempFile("org.apache.fop.pdf.StreamCache-",
                                       ".temp");
        tempFile.deleteOnExit();
    }

    /**
     * Get the current OutputStream. Do not store it - it may change
     * from call to call.
     *
     * @throws IOException if there is an IO error
     * @return the output stream for this cache
     */
    public OutputStream getOutputStream() throws IOException {
        if (output == null) {
            output = new BufferedOutputStream(
                       new FileOutputStream(tempFile));
        }
        return output;
    }

    /**
     * Filter the cache with the supplied PDFFilter.
     *
     * @param filter the filter to apply
     * @throws IOException if there is an IO error
     */
    public void applyFilter(PDFFilter filter) throws IOException {
        if (output == null) {
            return;
        }

        output.close();
        output = null;

        // need a place to put results
        File newTempFile =
          File.createTempFile("org.apache.fop.pdf.StreamCache-",
                              ".temp");
        newTempFile.deleteOnExit();

        // filter may not be buffered
        BufferedInputStream input =
          new BufferedInputStream(new FileInputStream(tempFile));
        BufferedOutputStream output = new BufferedOutputStream(
                                        new FileOutputStream(newTempFile));
        filter.encode(input, output, (int) tempFile.length());
        input.close();
        output.close();
        tempFile.delete();
        tempFile = newTempFile;
    }

    /**
     * Outputs the cached bytes to the given stream.
     *
     * @param stream the output stream to write to
     * @throws IOException if there is an IO error
     */
    public void outputStreamData(OutputStream stream) throws IOException {
        if (output == null) {
            return;
        }

        output.close();
        output = null;

        // don't need a buffer because streamCopy is buffered
        FileInputStream input = new FileInputStream(tempFile);
        StreamUtilities.streamCopy(input, output);
        input.close();
    }

    /**
     * Returns the current size of the stream.
     *
     * @throws IOException if there is an IO error
     * @return the size of the cache
     */
    public int getSize() throws IOException {
        if (output != null) {
            output.flush();
        }
        return (int) tempFile.length();
    }

    /**
     * Closes the cache and frees resources.
     *
     * @throws IOException if there is an IO error
     */
    public void close() throws IOException {
        if (output != null) {
            output.close();
            output = null;
        }
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    /**
     * Clears and resets the cache.
     *
     * @throws IOException if there is an IO error
     */
    public void reset() throws IOException {
        if (output != null) {
            output.close();
            output = null;
        }
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }
}
