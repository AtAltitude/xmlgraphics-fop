/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.datatypes.*;
import org.apache.fop.layout.*;
import org.apache.fop.fo.flow.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.apps.FOPException;

import org.apache.fop.layoutmgr.LMiter;
import org.apache.fop.layoutmgr.InlineStackingLayoutManager;
import org.apache.fop.layoutmgr.ContentLayoutManager;

/**
 */
public class Title extends FObjMixed {

    public Title(FONode parent) {
        super(parent);
    }

    public org.apache.fop.area.Title getTitleArea() {
        org.apache.fop.area.Title title =
                 new org.apache.fop.area.Title();
        // use special layout manager to add the inline areas
        // to the Title.
        InlineStackingLayoutManager lm;
        lm = new InlineStackingLayoutManager(this,
                     new LMiter(children.listIterator()));
        lm.init();

        // get breaks then add areas to title

        ContentLayoutManager clm = new ContentLayoutManager(title);
        lm.setParentLM(clm);

        clm.fillArea(lm);

        return title;
    }

    public void setup() {

        // Common Accessibility Properties
        AccessibilityProps mAccProps = propMgr.getAccessibilityProps();

        // Common Aural Properties
        AuralProps mAurProps = propMgr.getAuralProps();

        // Common Border, Padding, and Background Properties
        BorderAndPadding bap = propMgr.getBorderAndPadding();
        BackgroundProps bProps = propMgr.getBackgroundProps();

        // Common Font Properties
        FontState fontState = propMgr.getFontState(structHandler.getFontInfo());

        // Common Margin Properties-Inline
        MarginInlineProps mProps = propMgr.getMarginInlineProps();

        Property prop;
        prop = this.properties.get("baseline-shift");
        if (prop instanceof LengthProperty) {
            Length bShift = prop.getLength();
        } else if (prop instanceof EnumProperty) {
            int bShift = prop.getEnum();
        }
        ColorType col = this.properties.get("color").getColorType();
        Length lHeight = this.properties.get("line-height").getLength();
        int lShiftAdj = this.properties.get(
                          "line-height-shift-adjustment").getEnum();
        int vis = this.properties.get("visibility").getEnum();
        Length zIndex = this.properties.get("z-index").getLength();

    }
}

