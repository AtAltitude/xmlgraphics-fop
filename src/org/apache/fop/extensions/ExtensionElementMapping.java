/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.extensions;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FOTreeBuilder;

import java.util.HashMap;

public class ExtensionElementMapping implements ElementMapping {

    public static final String URI = "http://xml.apache.org/fop/extensions";

    private static HashMap foObjs = null;

    public synchronized void addToBuilder(FOTreeBuilder builder) {
        if(foObjs == null) {
            foObjs = new HashMap();    
            foObjs.put("outline", new O());
            foObjs.put("label", new L());
        }
        builder.addMapping(URI, foObjs);
    }

    class O extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new Outline(parent);
        }
    }

    class L extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new Label(parent);
        }
    }
}
