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
import org.apache.fop.datatypes.*;
import org.apache.fop.layout.Area;
import org.apache.fop.layout.BlockArea;
import org.apache.fop.layout.FontState;
import org.apache.fop.apps.FOPException;

// Java
import java.util.Enumeration;

public class ListBlock extends FObj {

    public static class Maker extends FObj.Maker {
	public FObj make(FObj parent, PropertyList propertyList)
	    throws FOPException {
	    return new ListBlock(parent, propertyList);
	}
    }

    public static FObj.Maker maker() {
	return new ListBlock.Maker();
    }

    FontState fs;
    int align;
    int alignLast;
    int breakBefore;
    int breakAfter;
    int lineHeight;
    int startIndent;
    int endIndent;
    int spaceBefore;
    int spaceAfter;
    int provisionalDistanceBetweenStarts;
    int provisionalLabelSeparation;
    int spaceBetweenListRows = 0;
    ColorType backgroundColor;

    public ListBlock(FObj parent, PropertyList propertyList) {
	super(parent, propertyList);
	this.name = "fo:list-block";
    }

    public Status layout(Area area) throws FOPException {
	if (this.marker == START) {
	    String fontFamily =
		this.properties.get("font-family").getString(); 
	    String fontStyle =
		this.properties.get("font-style").getString(); 
	    String fontWeight =
		this.properties.get("font-weight").getString(); 
	    int fontSize =
		this.properties.get("font-size").getLength().mvalue(); 
	    
	    this.fs = new FontState(area.getFontInfo(), fontFamily,
				    fontStyle, fontWeight, fontSize);
	    
	    this.align = this.properties.get("text-align").getEnum(); 
	    this.alignLast =
		this.properties.get("text-align-last").getEnum(); 
	    this.lineHeight =
		this.properties.get("line-height").getLength().mvalue(); 
	    this.startIndent =
		this.properties.get("start-indent").getLength().mvalue(); 
	    this.endIndent =
		this.properties.get("end-indent").getLength().mvalue();
	    this.spaceBefore =
		this.properties.get("space-before.optimum").getLength().mvalue();
	    this.spaceAfter =
		this.properties.get("space-after.optimum").getLength().mvalue();
	    this.provisionalDistanceBetweenStarts =
		this.properties.get("provisional-distance-between-starts").getLength().mvalue();
	    this.provisionalLabelSeparation =
		this.properties.get("provisional-label-separation").getLength().mvalue(); 
	    this.spaceBetweenListRows = 0; // not used at present
	    this.backgroundColor =
		this.properties.get("background-color").getColorType();
	    
	    this.marker = 0;

	    if (area instanceof BlockArea) {
		area.end();
	    }

	    if (spaceBefore != 0) {
		area.addDisplaySpace(spaceBefore);
	    }

	    if (this.isInListBody) {
		startIndent += bodyIndent + distanceBetweenStarts;
		bodyIndent = startIndent;
	    }

	    if (this.isInTableCell) {
		startIndent += forcedStartOffset;
		endIndent += area.getAllocationWidth() - forcedWidth -
		    forcedStartOffset;
	    }

             // initialize id                       
            String id = this.properties.get("id").getString();            
            area.getIDReferences().initializeID(id,area); 
	}        

	BlockArea blockArea =
	    new BlockArea(fs, area.getAllocationWidth(),
			  area.spaceLeft(), startIndent, endIndent, 0,
			  align, alignLast, lineHeight);
	blockArea.setPage(area.getPage());
	blockArea.setBackgroundColor(backgroundColor);
	blockArea.start();
        
        blockArea.setAbsoluteHeight(area.getAbsoluteHeight());
        blockArea.setIDReferences(area.getIDReferences());

	int numChildren = this.children.size();
	for (int i = this.marker; i < numChildren; i++) {
	    if (!(children.elementAt(i) instanceof ListItem)) {
		System.err.println("WARNING: This version of FOP requires list-items inside list-blocks");
		return new Status(Status.OK);
	    }
	    ListItem listItem = (ListItem) children.elementAt(i);
	    listItem.setDistanceBetweenStarts(this.provisionalDistanceBetweenStarts);
	    listItem.setLabelSeparation(this.provisionalLabelSeparation);
	    listItem.setBodyIndent(this.bodyIndent);
	    Status status;
	    if ((status = listItem.layout(blockArea)).isIncomplete()) {
		this.marker = i;
		blockArea.end();
		area.addChild(blockArea);
		area.increaseHeight(blockArea.getHeight());
                area.setAbsoluteHeight(blockArea.getAbsoluteHeight()); 
		return status;
	    }
	}

	blockArea.end();
	area.addChild(blockArea);
	area.increaseHeight(blockArea.getHeight());
        area.setAbsoluteHeight(blockArea.getAbsoluteHeight());

	if (spaceAfter != 0) {
	    area.addDisplaySpace(spaceAfter);
	}

	if (area instanceof BlockArea) {
	    area.start();
	}
	
	return new Status(Status.OK);
    }
}
