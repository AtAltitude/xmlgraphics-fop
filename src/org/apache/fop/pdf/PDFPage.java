/*-- $Id$ -- 

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================
 
    Copyright (C) 1999 The Apache Software Foundation. All rights reserved.
 
 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:
 
 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.
 
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
 
 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.
 
 4. The names "Fop" and  "Apache Software Foundation"  must not be used to
    endorse  or promote  products derived  from this  software without  prior
    written permission. For written permission, please contact
    apache@apache.org.
 
 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.
 
 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
 Software Foundation, please see <http://www.apache.org/>.
 
 */
package org.apache.fop.pdf;



// Java

import java.io.PrintWriter;

import java.util.Vector;



/**

 * class representing a /Page object.

 *

 * There is one of these for every page in a PDF document. The object

 * specifies the dimensions of the page and references a /Resources

 * object, a contents stream and the page's parent in the page

 * hierarchy.

 */

public class PDFPage extends PDFObject {



    /** the page's parent, a /Pages object */

    protected PDFPages parent;



    /** the page's /Resource object */

    protected PDFResources resources;



    /** the contents stream */

    protected PDFStream contents;



    /** the width of the page in points */

    protected int pagewidth;



    /** the height of the page in points */

    protected int pageheight;



    /**

     * create a /Page object

     *

     * @param number the object's number

     * @param resources the /Resources object

     * @param contents the content stream

     * @param pagewidth the page's width in points

     * @param pageheight the page's height in points

     */

    public PDFPage(int number, PDFResources resources,

		   PDFStream contents, int pagewidth,

		   int pageheight) {



	/* generic creation of object */

	super(number);



	/* set fields using parameters */

	this.resources = resources;

	this.contents = contents;

	this.pagewidth = pagewidth;

	this.pageheight = pageheight;

    }



    /**

     * set this page's parent

     *

     * @param parent the /Pages object that is this page's parent

     */

    public void setParent(PDFPages parent) {

	this.parent = parent;

    }



    /**

     * represent this object as PDF

     *

     * @return the PDF string

     */

    public String toPDF() {

	String p = this.number + " " + this.generation

	    + " obj\n<< /Type /Page\n/Parent "

	    + this.parent.referencePDF() + "\n/MediaBox [ 0 0 "

	    + this.pagewidth + " " + this.pageheight + " ]\n/Resources "

	    + this.resources.referencePDF() + "\n/Contents "

	    + this.contents.referencePDF() + " >>\nendobj\n";

	return p;

    }

}

