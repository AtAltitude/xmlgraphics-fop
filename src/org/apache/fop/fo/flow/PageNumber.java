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
package org.apache.xml.fop.fo.flow;

// FOP
import org.apache.xml.fop.fo.*;
import org.apache.xml.fop.datatypes.*;
import org.apache.xml.fop.fo.properties.*;
import org.apache.xml.fop.layout.*;
import org.apache.xml.fop.apps.FOPException;

// Java
import java.util.Enumeration;

public class PageNumber extends FObj {

    public static class Maker extends FObj.Maker {
	public FObj make(FObj parent, PropertyList propertyList)
	    throws FOPException {
	    return new PageNumber(parent, propertyList);
	}
    }

    public static FObj.Maker maker() {
	return new PageNumber.Maker();
    }

    FontState fs;
    float red;
    float green;
    float blue;
    int wrapOption;
    int whiteSpaceTreatment;

    public PageNumber(FObj parent, PropertyList propertyList) {
	super(parent, propertyList);
	this.name = "fo:page-number";
    }

    public int layout(Area area) throws FOPException {
	if (!(area instanceof BlockArea)) {
	    System.err.println("WARNING: page-number outside block area");
	    return OK;
	}
	if (this.marker == START) {
	    String fontFamily = this.properties.get("font-family").getString();
	    String fontStyle = this.properties.get("font-style").getString();
	    String fontWeight = this.properties.get("font-weight").getString();
	    int fontSize = this.properties.get("font-size").getLength().mvalue();
		
	    this.fs = new FontState(area.getFontInfo(), fontFamily,
				     fontStyle, fontWeight, fontSize);

	    ColorType c = this.properties.get("color").getColorType();
	    this.red = c.red();
	    this.green = c.green();
	    this.blue = c.blue();

	    this.wrapOption = this.properties.get("wrap-option").getEnum();
	    this.whiteSpaceTreatment = this.properties.get("white-space-treatment").getEnum();
	    
	    this.marker = 0;
	}
	String p = Integer.toString(area.getPage().getNumber());
	this.marker = ((BlockArea) area).addText(fs, red, green, blue, wrapOption, whiteSpaceTreatment, p.toCharArray(), 0, p.length());
	return OK;
    }
}
