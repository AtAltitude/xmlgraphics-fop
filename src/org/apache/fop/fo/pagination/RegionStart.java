/*
 * $Id$
 * Copyright (C) 2001-2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources."
 */

package org.apache.fop.fo.pagination;

// Java
import java.awt.Rectangle;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.datatypes.FODimension;
import org.apache.fop.area.RegionReference;

public class RegionStart extends RegionSE {


    public RegionStart(FONode parent) {
        super(parent);
    }


    protected Rectangle getViewportRectangle (FODimension reldims) {
        // Depends on extent and precedence
        // This is the rectangle relative to the page-reference area in
        // writing-mode relative coordinates
        Rectangle vpRect =
            new Rectangle(0, 0, getExtent(), reldims.bpd);
        adjustIPD(vpRect);
        return vpRect;
    }

    protected String getDefaultRegionName() {
        return "xsl-region-start";
    }

    public String getRegionClass() {
        return Region.START;
    }

    public int getRegionAreaClass() {
        return RegionReference.START;
    }

}

