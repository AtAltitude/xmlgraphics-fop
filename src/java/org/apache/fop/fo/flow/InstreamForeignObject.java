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

package org.apache.fop.fo.flow;

// XML
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

// FOP
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOElementMapping;
import org.apache.fop.layoutmgr.AddLMVisitor;
import org.apache.fop.fo.FObj;

/**
 * The instream-foreign-object flow formatting object.
 * This is an atomic inline object that contains
 * xml data.
 */
public class InstreamForeignObject extends FObj {

    boolean hasNonXSLNamespaceElement = false;

    /**
     * constructs an instream-foreign-object object (called by Maker).
     *
     * @param parent the parent formatting object
     */
    public InstreamForeignObject(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
     * XSL Content Model: one (1) non-XSL namespace child
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws SAXParseException {
        if (nsURI == FOElementMapping.URI) {
            invalidChildError(loc, nsURI, localName);
        } else if (hasNonXSLNamespaceElement) {
            tooManyNodesError(loc, "child element");
        } else {
            hasNonXSLNamespaceElement = true;
        }
    }

    /**
     * Make sure content model satisfied, if so then tell the
     * StructureRenderer that we are at the end of the flow.
     * @see org.apache.fop.fo.FONode#end
     */
    protected void endOfNode() throws SAXParseException {
        if (!hasNonXSLNamespaceElement) {
            missingChildElementError("one (1) non-XSL namespace child");
        }
    }

    public int computeXOffset (int ipd, int cwidth) {
        int xoffset = 0;
        int ta = propertyList.get(PR_TEXT_ALIGN).getEnum();
        switch (ta) {
            case TextAlign.CENTER:
                xoffset = (ipd - cwidth) / 2;
                break;
            case TextAlign.END:
                xoffset = ipd - cwidth;
                break;
            case TextAlign.START:
                break;
            case TextAlign.JUSTIFY:
            default:
                break;
        }
        return xoffset;
    }

    public int computeYOffset(int bpd, int cheight) {
        int yoffset = 0;
        int da = propertyList.get(PR_DISPLAY_ALIGN).getEnum();
        switch (da) {
            case DisplayAlign.BEFORE:
                break;
            case DisplayAlign.AFTER:
                yoffset = bpd - cheight;
                break;
            case DisplayAlign.CENTER:
                yoffset = (bpd - cheight) / 2;
                break;
            case DisplayAlign.AUTO:
            default:
                break;
        }
        return yoffset;
    }

    /**
     * This flow object generates inline areas.
     * @see org.apache.fop.fo.FObj#generatesInlineAreas()
     * @return true
     */
    public boolean generatesInlineAreas() {
        return true;
    }

    /**
     * This is a hook for the AddLMVisitor class to be able to access
     * this object.
     * @param aLMV the AddLMVisitor object that can access this object.
     */
    public void acceptVisitor(AddLMVisitor aLMV) {
        aLMV.serveInstreamForeignObject(this);
    }

    public String getName() {
        return "fo:instream-foreign-object";
    }
}
