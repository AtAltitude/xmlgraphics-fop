/*>$File$ -- $Id$ -- 

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
import org.apache.xml.fop.fo.properties.*;
import org.apache.xml.fop.layout.*;
import org.apache.xml.fop.apps.FOPException;

// Java
import java.util.Vector;

public class TableRow extends FObj {
	
    public static class Maker extends FObj.Maker {
	public FObj make(FObj parent, PropertyList propertyList)
	    throws FOPException {
	    return new TableRow(parent, propertyList);
	}
    }

    public static FObj.Maker maker() {
	return new TableRow.Maker();
    }

    FontState fs;
    int startIndent;
    int endIndent;
    int spaceBefore;
    int spaceAfter;

    int widthOfCellsSoFar = 0;
    int largestCellHeight = 0;

    Vector columns;

    BlockArea blockArea;

    public TableRow(FObj parent, PropertyList propertyList) {
	super(parent, propertyList);
	this.name = "fo:table-row";
    }

    public void setColumns(Vector columns) {
	this.columns = columns;
    }

    public int layout(Area area) throws FOPException {
	if (this.marker == BREAK_AFTER) {
	    return OK;
	}

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
	    this.startIndent =
		this.properties.get("start-indent").getLength().mvalue(); 
	    this.endIndent =
		this.properties.get("end-indent").getLength().mvalue(); 
	    this.spaceBefore =
		this.properties.get("space-before.optimum").getLength().mvalue();  
	    this.spaceAfter =
		this.properties.get("space-after.optimum").getLength().mvalue(); 
	    if (area instanceof BlockArea) {
		area.end();
	    }

	    //if (this.isInListBody) {
	    //startIndent += bodyIndent + distanceBetweenStarts;
	    //}

	    this.marker = 0;

	}

	if ((spaceBefore != 0) && (this.marker ==0)) {
	    area.addDisplaySpace(spaceBefore);
	}

	this.blockArea =
	    new BlockArea(fs, area.getAllocationWidth(), 
			  area.spaceLeft(), startIndent, endIndent, 0,
			  0, 0, 0);
	blockArea.setPage(area.getPage());
	blockArea.start();

	int numChildren = this.children.size();
	if (numChildren != columns.size()) {
	    System.err.println("WARNING: Number of children under table-row not equal to number of table-columns");
	    return OK;
	}

	for (int i = this.marker; i < numChildren; i++) {
	    TableCell cell = (TableCell) children.elementAt(i);

	    //if (this.isInListBody) {
	    //fo.setIsInListBody();
	    //fo.setDistanceBetweenStarts(this.distanceBetweenStarts);
	    //fo.setBodyIndent(this.bodyIndent);
	    //}

	    cell.setStartOffset(widthOfCellsSoFar);
	    int width = ((TableColumn) columns.elementAt(i)).getColumnWidth();

	    cell.setWidth(width);
	    widthOfCellsSoFar += width;

	    int status;
	    if ((status = cell.layout(blockArea)) != OK) {
		this.marker = i;
		if ((i != 0) && (status == AREA_FULL_NONE)) {
		    status = AREA_FULL_SOME;
		}
		//blockArea.end();
		area.addChild(blockArea);
		area.increaseHeight(blockArea.getHeight());
		return status;
	    }

	    int h = cell.getHeight();
	    blockArea.addDisplaySpace(-h);
	    if (h > largestCellHeight) {
		largestCellHeight = h;
	    }

	}

	blockArea.end();
	area.addChild(blockArea);
	area.addDisplaySpace(largestCellHeight);
	area.increaseHeight(largestCellHeight);

	if (spaceAfter != 0) {
	    area.addDisplaySpace(spaceAfter);
	}

	if (area instanceof BlockArea) {
	    area.start();
	}

	return OK;
    }

    public int getAreaHeight() {
	return blockArea.getHeight();
    }
}
