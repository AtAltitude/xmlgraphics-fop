/*
 * $Id$
 * Copyright (C) 2001-2003 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.pdf;

/**
 * class representing a /FileSpec object.
 *
 */
public class PDFFileSpec extends PDFObject {

    /**
     * the filename
     */
    protected String filename;

    /**
     * create a /FileSpec object.
     *
     * @param number the object's number
     * @param filename the filename represented by this object
     */
    public PDFFileSpec(int number, String filename) {

        /* generic creation of object */
        super(number);

        this.filename = filename;
    }

    /**
     * represent the object in PDF
     *
     * @return the PDF string
     */
    public byte[] toPDF() {
        String p = new String(this.number + " " + this.generation
                              + " obj\n<<\n/Type /FileSpec\n" + "/F ("
                              + this.filename + ")\n" + ">>\nendobj\n");
        return p.getBytes();
    }

    /*
     * example
     * 29 0 obj
     * <<
     * /Type /FileSpec
     * /F (table1.pdf)
     * >>
     * endobj
     */

    /**
     * Check if this equals another object.
     *
     * @param obj the object to compare
     * @return true if this equals other object
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFFileSpec)) {
            return false;
        }

        PDFFileSpec spec = (PDFFileSpec)obj;

        if (!spec.filename.equals(filename)) {
            return false;
        }

        return true;
    }
}

