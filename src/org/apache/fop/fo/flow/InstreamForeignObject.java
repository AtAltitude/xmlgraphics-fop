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

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.*;
import org.apache.fop.layout.BlockArea;
import org.apache.fop.layout.FontState;
import org.apache.fop.apps.FOPException;

// Java
import java.util.Enumeration;

public class InstreamForeignObject extends FObj {
	/**
	 * inner class for making SVG objects.
	 */
	public static class Maker extends FObj.Maker {

	/**
	 * make an SVG object.
	 *
	 * @param parent the parent formatting object
	 * @param propertyList the explicit properties of this object
	 *
	 * @return the SVG object
	 */
	public FObj make(FObj parent, PropertyList propertyList)
		throws FOPException {
		return new InstreamForeignObject(parent, propertyList);
	}
	}

	/**
	 * returns the maker for this object.
	 *
	 * @return the maker for SVG objects
	 */
	public static FObj.Maker maker() {
	return new InstreamForeignObject.Maker();
	}

	FontState fs;
	int breakBefore;
	int breakAfter;
	int width;
	int height;
	int contwidth;
	int contheight;
	boolean wauto;
	boolean hauto;
	boolean cwauto;
	boolean chauto;
	int spaceBefore;
	int spaceAfter;

    Area areaCurrent;

	/**
	 * constructs an instream-foreign-object object (called by Maker).
	 *
	 * @param parent the parent formatting object
	 * @param propertyList the explicit properties of this object
	 */
	public InstreamForeignObject(FObj parent, PropertyList propertyList) {
	super(parent, propertyList);
	this.name = "fo:instream-foreign-object";
	}

	/**
	 * layout this formatting object.
	 *
	 * @param area the area to layout the object into
	 *
	 * @return the status of the layout
	 */
	public Status layout(Area area) throws FOPException {
	
	if (this.marker == BREAK_AFTER) {
		return new Status(Status.OK);
	}

	if (this.marker == START) {
		/* retrieve properties */
		String id = this.properties.get("id").getString();
		String fontFamily = this.properties.get("font-family").getString();
		String fontStyle = this.properties.get("font-style").getString();
		String fontWeight = this.properties.get("font-weight").getString();
		int fontSize = this.properties.get("font-size").getLength().mvalue();
		
		this.fs = new FontState(area.getFontInfo(), fontFamily,
					 fontStyle, fontWeight, fontSize);
		
		this.breakBefore = this.properties.get("break-before").getEnum();
		this.breakAfter = this.properties.get("break-after").getEnum();
		this.width = this.properties.get("width").getLength().mvalue();
		this.height = this.properties.get("height").getLength().mvalue();
		this.contwidth = this.properties.get("content-width").getLength().mvalue();
		this.contheight = this.properties.get("content-height").getLength().mvalue();
		this.wauto = this.properties.get("width").getLength().isAuto();
		this.hauto = this.properties.get("height").getLength().isAuto();
		this.cwauto = this.properties.get("content-width").getLength().isAuto();
		this.chauto = this.properties.get("content-height").getLength().isAuto();
		
		this.spaceBefore =
		this.properties.get("space-before.optimum").getLength().mvalue();
		this.spaceAfter =
		this.properties.get("space-after.optimum").getLength().mvalue();

	    area.getIDReferences().createID(id);                        
		/* if is embedded in a block area */
		if (area instanceof BlockArea) {
		/* temporarily end the block area */
		area.end();
		}
		
		this.marker = 0;

		// this doesn't work for auto sizes
		if (breakBefore == BreakBefore.PAGE || ((this.height + area.getHeight()) > area.getMaxHeight())) {
		return new Status(Status.FORCE_PAGE_BREAK);
		}

		if (breakBefore == BreakBefore.ODD_PAGE) {
		return new Status(Status.FORCE_PAGE_BREAK_ODD);
		}

		if (breakBefore == BreakBefore.EVEN_PAGE) {
		return new Status(Status.FORCE_PAGE_BREAK_EVEN);
		}
	}
	   
	/* if there is a space-before */
	if (spaceBefore != 0) {
		/* add a display space */
		area.addDisplaySpace(spaceBefore);
	}

	this.areaCurrent =
	    new ForeignObjectArea(fs, area.getAllocationWidth(), 
                          area.spaceLeft(), 0);
	areaCurrent.setPage(area.getPage());
	/* layout foreign object */
//	svg.start();

	/* add the SVG area to the containing area */
	area.addChild(areaCurrent);

	/* iterate over the child formatting objects and lay them out
	   into the SVG area */
	int numChildren = this.children.size();
	if(numChildren > 1) {
	    // error
	}
	if(this.children.size() > 0) {
	FONode fo = (FONode) children.elementAt(0);
	Status status;
	// currently FONode must be an SVG
	if ((status = fo.layout(this.areaCurrent)).isIncomplete()) {
	return status;
	}

	/* finish off the SVG area */
//	svg.end();

	/* increase the height of the containing area accordingly */
	area.increaseHeight(areaCurrent.getHeight());
	}

	/* if there is a space-after */
	if (spaceAfter != 0) {
		/* add a display space */
		area.addDisplaySpace(spaceAfter);
	}

	/* if is embedded in a block area */
	if (area instanceof BlockArea) {
		/* re-start the block area */
		area.start();
	}
	
	if (breakAfter == BreakAfter.PAGE) {
		this.marker = BREAK_AFTER;
		return new Status(Status.FORCE_PAGE_BREAK);
	}

	if (breakAfter == BreakAfter.ODD_PAGE) {
		this.marker = BREAK_AFTER;
		return new Status(Status.FORCE_PAGE_BREAK_ODD);
	}
	
	if (breakAfter == BreakAfter.EVEN_PAGE) {
		this.marker = BREAK_AFTER;
		return new Status(Status.FORCE_PAGE_BREAK_EVEN);
	}

	/* return status */
	return new Status(Status.OK);
	}
}
