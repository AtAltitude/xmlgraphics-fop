/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.*;
import org.apache.fop.layout.FontState;
import org.apache.fop.apps.FOPException;
import org.apache.fop.layoutmgr.list.Item;

// Java
import java.util.Enumeration;

public class ListItemLabel extends FObj {

    public ListItemLabel(FONode parent) {
        super(parent);
    }

    public Item getItemLayoutManager() {
        Item itemLabel = new Item();
        itemLabel.setUserAgent(getUserAgent());
        itemLabel.setFObj(this);
        return itemLabel;
    }

    public void setup() {

        // Common Accessibility Properties
        AccessibilityProps mAccProps = propMgr.getAccessibilityProps();

        setupID();
        // this.properties.get("keep-together");

        /*
         * For calculating the lineage - The fo:list-item-label formatting object
         * does not generate any areas. The fo:list-item-label formatting object
         * returns the sequence of areas created by concatenating the sequences
         * of areas returned by each of the children of the fo:list-item-label.
         */

    }

    protected boolean containsMarkers() {
        return true;
    }

}

