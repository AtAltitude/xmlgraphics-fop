/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.pagination;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.apps.FOPException;
import org.apache.fop.area.RegionReference;

import java.awt.Rectangle;
import org.xml.sax.Attributes;

public class RegionAfter extends RegionBA {


    public RegionAfter(FONode parent) {
        super(parent);
    }


    protected Rectangle getViewportRectangle (Rectangle pageRefRect) {
	// Depends on extent and precedence
	Rectangle vpRect =
	    new Rectangle((int)pageRefRect.getX(),
			  (int)pageRefRect.getY() - (int)pageRefRect.getHeight() + 
			  getExtent(),
			  (int)pageRefRect.getWidth(), getExtent());
	if (getPrecedence() == false) {
	    adjustIPD(vpRect);
	}
	return vpRect;
    }


    protected String getDefaultRegionName() {
        return "xsl-region-after";
    }

    public int getRegionAreaClass() {
        return RegionReference.AFTER;
    }

    public String getRegionClass() {
        return Region.AFTER;
    }


}
