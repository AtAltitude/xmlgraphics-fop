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
package org.apache.fop.pdf;

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * class representing an /Info object
 */
public class PDFInfo extends PDFObject {

    /**
     * the application producing the PDF
     */
    private String producer;

    private String title = null;
    private String author = null;
    private String subject = null;
    private String keywords = null;

    // the name of the application that created the
    // original document before converting to PDF
    private String creator;

    /**
     * create an Info object
     *
     * @param number the object's number
     */
    public PDFInfo(int number) {
        super(number);
    }

    /**
     * set the producer string
     *
     * @param producer the producer string
     */
    public void setProducer(String producer) {
        this.producer = producer;
    }

    /**
     * set the creator string
     *
     * @param creator the document creator
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * set the title string
     *
     * @param t the document title
     */
    public void setTitle(String t) {
        this.title = t;
    }

    /**
     * set the author string
     *
     * @param a the document author
     */
    public void setAuthor(String a) {
        this.author = a;
    }

    /**
     * set the subject string
     *
     * @param s the document subject
     */
    public void setSubject(String s) {
        this.subject = s;
    }

    /**
     * set the keywords string
     *
     * @param k the keywords for this document
     */
    public void setKeywords(String k) {
        this.keywords = k;
    }

    /**
     * produce the PDF representation of the object
     *
     * @return the PDF
     */
    public byte[] toPDF() {
        String p = this.number + " " + this.generation
                   + " obj\n<< /Type /Info\n";
        if (title != null) {
            p += "/Title (" + this.title + ")\n";
        }
        if (author != null) {
            p += "/Author (" + this.author + ")\n";
        }
        if (subject != null) {
            p += "/Subject (" + this.subject + ")\n";
        }
        if (keywords != null) {
            p += "/Keywords (" + this.keywords + ")\n";
        }

        if (creator != null) {
            p += "/Creator (" + this.creator + ")\n";
        }

        p += "/Producer (" + this.producer + ")\n";

        // creation date in form (D:YYYYMMDDHHmmSSOHH'mm')
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
        String str = sdf.format(date) + "+00'00'";
        p += "/CreationDate (D:" + str + ")";
        p += " >>\nendobj\n";
        return p.getBytes();
    }
}

