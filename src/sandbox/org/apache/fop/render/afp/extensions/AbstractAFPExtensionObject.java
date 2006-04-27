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

package org.apache.fop.render.afp.extensions;

// FOP
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Base class for the AFP-specific extension elements.
 */
public abstract class AbstractAFPExtensionObject extends FONode {

    private AFPPageSetup setupCode = null;

    private String _name = null;
    
    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     * @param parent the parent formatting object
     * @param name the name of the afp element
     */
    public AbstractAFPExtensionObject(FONode parent, String name) {
        super(parent);
        _name = name;
        setupCode = new AFPPageSetup(name);
    }

    /**
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
     * here, blocks XSL FO's from having non-FO parents.
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName)
                throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** @see org.apache.fop.fo.FONode */
    protected void addCharacters(char[] data, int start, int length,
                                 PropertyList pList, Locator locator) {
    }

    /** @see org.apache.fop.fo.FONode#getNamespaceURI() */
    public String getNamespaceURI() {
        return AFPElementMapping.NAMESPACE;
    }

    /**@see org.apache.fop.fo.FONode#getNormalNamespacePrefix() */
    public String getNormalNamespacePrefix() {
        return AFPElementMapping.NAMESPACE_PREFIX;
    }

    /** @see org.apache.fop.fo.FONode#processNode */
    public void processNode(String elementName, Locator locator,
                            Attributes attlist, PropertyList propertyList)
                                throws FOPException {
        String name = attlist.getValue("name");
        if (name != null && name.length() > 0) {
            setupCode.setName(name);
        } else {
            throw new FOPException(elementName + " must have a name attribute.");
        }
        if (AFPElementMapping.INCLUDE_PAGE_SEGMENT.equals(elementName)) {
            name = attlist.getValue("src");
            if (name != null && name.length() > 0) {
                setupCode.setValue(name);
            } else {
                throw new FOPException(elementName + " must have a src attribute.");
            }
        }
        if (AFPElementMapping.TAG_LOGICAL_ELEMENT.equals(elementName)) {
            name = attlist.getValue("value");
            if (name != null && name.length() > 0) {
                setupCode.setValue(name);
            } else {
                throw new FOPException(elementName + " must have a value attribute.");
            }
        }
    }

    /** @see org.apache.fop.fo.FONode#endOfNode() */
    protected void endOfNode() throws FOPException {
        super.endOfNode();
    }

    /** @see org.apache.fop.fo.FONode#getExtensionAttachment() */
    public ExtensionAttachment getExtensionAttachment() {
        return this.setupCode;
    }

    /** @see org.apache.fop.fo.FONode#getLocalName() */
    public String getLocalName() {
        return _name;
    }

}

