/*
 * Copyright 1999-2004 The Apache Software Foundation.
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

package org.apache.fop.fo.pagination;

// XML
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

// FOP
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FOElementMapping;
import org.apache.fop.fo.FONode;
import org.apache.fop.layoutmgr.AddLMVisitor;

/**
 * Class modelling the fo:static-content object. See Sec. 6.4.19 of the XSL-FO
 * Standard.
 */
public class StaticContent extends Flow {

    /**
     * @param parent FONode that is the parent of this object
     */
    public StaticContent(FONode parent) {
        super(parent);
    }

    private void setup() {
    }

    /**
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
     * XSL Content Model: (%block;)+
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws SAXParseException {
        if (!isBlockItem(nsURI, localName)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /**
     * Make sure content model satisfied, if so then tell the
     * StructureRenderer that we are at the end of the flow.
     * @see org.apache.fop.fo.FONode#end
     */
    protected void endOfNode() throws SAXParseException {
        if (childNodes == null) {
            missingChildElementError("(%block;)+");
        }
        getFOInputHandler().endFlow(this);
    }

    /**
     * flowname checking is more stringient for static content currently
     * @param name the flow-name to set
     * @throws FOPException for a missing flow name
     */
    protected void setFlowName(String name) throws FOPException {
        if (name == null || name.equals("")) {
            throw new FOPException("A 'flow-name' is required for "
                                   + getName() + ".");
        } else {
            super.setFlowName(name);
        }

    }

    /**
     * This is a hook for the AddLMVisitor class to be able to access
     * this object.
     * @param aLMV the AddLMVisitor object that can access this object.
     */
    public void acceptVisitor(AddLMVisitor aLMV) {
        aLMV.serveStaticContent(this);
    }

    public String getName() {
        return "fo:static-content";
    }
}
