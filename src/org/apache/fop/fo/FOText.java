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
 
 4. The names "FOP" and  "Apache Software Foundation"  must not be used to
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

package org.apache.fop.fo;

// FOP
import org.apache.fop.layout.Area;
import org.apache.fop.layout.BlockArea;
import org.apache.fop.layout.FontState;
import org.apache.fop.datatypes.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.apps.FOPException;

/**
 * a text node in the formatting object tree
 */
public class FOText extends FONode {

    protected char[] ca;
    protected int start;
    protected int length;

    FontState fs;
    float red;
    float green;
    float blue;
    int wrapOption;
    int whiteSpaceTreatment;

    protected FOText(char[] chars, int s, int e, FObj parent) {
	super(parent);
	this.start = 0;
	this.ca = new char[e - s];
	for (int i = s; i < e; i++)
	    this.ca[i - s] = chars[i];
	this.length = e - s;
    }

    public Status layout(Area area) throws FOPException {
	if (!(area instanceof BlockArea)) {
	    System.err.println("WARNING: text outside block area" + new String(ca, start, length));
	    return new Status(Status.OK);
	}
	if (this.marker == START) {
	    String fontFamily =
		this.parent.properties.get("font-family").getString(); 
	    String fontStyle =
		this.parent.properties.get("font-style").getString(); 
	    String fontWeight =
		this.parent.properties.get("font-weight").getString(); 
	    int fontSize =
		this.parent.properties.get("font-size").getLength().mvalue(); 
	    
	    this.fs = new FontState(area.getFontInfo(), fontFamily, fontStyle,
				    fontWeight, fontSize); 
	    
	    ColorType c =
		this.parent.properties.get("color").getColorType();
	    this.red = c.red();
	    this.green = c.green();
	    this.blue = c.blue();
	    
	    this.wrapOption =
		this.parent.properties.get("wrap-option").getEnum(); 
	    this.whiteSpaceTreatment =
		this.parent.properties.get("white-space-treatment").getEnum();

	    this.marker = this.start;
	}
	int orig_start = this.marker;
	this.marker = ((BlockArea) area).addText(fs, red, green, blue,
						 wrapOption,
						 whiteSpaceTreatment,
						 ca, this.marker, length);
	if (this.marker == -1) {
	    return new Status(Status.OK);
	} else if (this.marker != orig_start) {
	    return new Status(Status.AREA_FULL_SOME);
	} else {
	    return new Status(Status.AREA_FULL_NONE);
	}
    }
}
