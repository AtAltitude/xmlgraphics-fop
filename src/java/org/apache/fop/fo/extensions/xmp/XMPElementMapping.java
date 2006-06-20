/*
 * Copyright 2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fo.extensions.xmp;

import java.util.HashMap;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.ElementMapping;
import org.apache.xmlgraphics.xmp.XMPConstants;

import org.w3c.dom.DOMImplementation;

/**
 * Setup the element mapping for XMP metadata.
 */
public class XMPElementMapping extends ElementMapping {
    
    /** Main constructor. */
    public XMPElementMapping() {
        namespaceURI = XMPConstants.XMP_NAMESPACE;
    }

    /** @see org.apache.fop.fo.ElementMapping#getDOMImplementation() */
    public DOMImplementation getDOMImplementation() {
        return getDefaultDOMImplementation();
    }

    /** @see org.apache.fop.fo.ElementMapping#initialize() */
    protected void initialize() {
        if (foObjs == null) {
            foObjs = new HashMap();
            foObjs.put("xmpmeta", new XMPMetaElementMaker());
        }
    }

    static class XMPMetaElementMaker extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new XMPMetaElement(parent);
        }
    }

}
