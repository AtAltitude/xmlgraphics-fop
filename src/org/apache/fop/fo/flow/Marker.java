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
import org.apache.fop.datatypes.*;
import org.apache.fop.apps.FOPException;

import org.xml.sax.Attributes;

public class Marker extends FObjMixed {

    private String markerClassName;

    public Marker(FONode parent) {
        super(parent);
    }

    public void handleAttrs(Attributes attlist) throws FOPException {
        super.handleAttrs(attlist);
        // do check to see that 'this' is under fo:flow

        this.markerClassName =
            this.properties.get("marker-class-name").getString();

        // check to ensure that no other marker with same parent
        // has this 'marker-class-name' is in addMarker() method
        try {
            ((FObj)parent).addMarker(this);
        } catch (FOPException fopex) {
            getLogger().error("marker cannot be added to '" + parent
                                 + "'");
        }
    }

    public String getMarkerClassName() {
        return markerClassName;
    }


}
