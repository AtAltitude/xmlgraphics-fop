/*
 * $Id: PDFGoTo.java,v 1.10 2003/03/07 08:25:46 jeremias Exp $
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
package org.apache.fop.pdf;

/**
 * class representing a /GoTo object.
 * This can either have a Goto to a page reference and location
 * or to a specified PDF reference string.
 */
public class PDFGoTo extends PDFAction {

    /**
     * the pageReference
     */
    private String pageReference;
    private String destination = null;
    private float xPosition = 0;
    private float yPosition = 0;

    /**
     * create a /GoTo object.
     *
     * @param pageReference the pageReference represented by this object
     */
    public PDFGoTo(String pageReference) {
        /* generic creation of object */
        super();

        this.pageReference = pageReference;
    }

    /**
     * Sets page reference after object has been created
     *
     * @param pageReference the new page reference to use
     */
    public void setPageReference(String pageReference) {
        this.pageReference = pageReference;
    }

    /**
     * Sets the Y position to jump to
     *
     * @param yPosition y position
     */
    public void setYPosition(float yPosition) {
        this.yPosition = yPosition;
    }

    /**
     * Set the destination string for this Goto.
     *
     * @param dest the PDF destination string
     */
    public void setDestination(String dest) {
        destination = dest;
    }

    /**
     * Sets the x Position to jump to
     *
     * @param xPosition x position
     */
    public void setXPosition(int xPosition) {
        this.xPosition = (xPosition / 1000f);
    }

    /**
     * Get the PDF reference for the GoTo action.
     *
     * @return the PDF reference for the action
     */
    public String getAction() {
        return referencePDF();
    }

    /**
     * @see org.apache.fop.pdf.PDFObject#toPDFString()
     */
    public String toPDFString() {
        String dest;
        if (destination == null) {
            dest = "/D [" + this.pageReference + " /XYZ " + xPosition
                          + " " + yPosition + " null]\n";
        } else {
            dest = "/D [" + this.pageReference + " " + destination + "]\n";
        }
        return getObjectID() 
                    + "<< /Type /Action\n/S /GoTo\n" + dest
                    + ">>\nendobj\n";
    }

    /*
     * example
     * 29 0 obj
     * <<
     * /S /GoTo
     * /D [23 0 R /FitH 600]
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

        if (obj == null || !(obj instanceof PDFGoTo)) {
            return false;
        }

        PDFGoTo gt = (PDFGoTo)obj;

        if (gt.pageReference == null) {
            if (pageReference != null) {
                return false;
            }
        } else {
            if (!gt.pageReference.equals(pageReference)) {
                return false;
            }
        }

        if (destination == null) {
            if (!(gt.destination == null && gt.xPosition == xPosition
                && gt.yPosition == yPosition)) {
                return false;
            }
        } else {
            if (!destination.equals(gt.destination)) {
                return false;
            }
        }

        return true;
    }
}

