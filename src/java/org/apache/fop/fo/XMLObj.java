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

package org.apache.fop.fo;

// Java
import java.awt.geom.Point2D;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import javax.xml.parsers.DocumentBuilderFactory;

// FOP
import org.apache.fop.apps.FOPException;

/**
 * Abstract class modelling generic, non-XSL-FO XML objects. Such objects are
 * stored in a DOM.
 */
public abstract class XMLObj extends FONode {

    // temp reference for attributes
    private Attributes attr = null;

    /** DOM element representing this node */
    protected Element element;

    /** DOM document containing this node */
    protected Document doc;

    /**
     *
     * @param parent the parent formatting object
     */
    public XMLObj(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FONode#processNode
     */
    public void processNode(String elementName, Locator locator, Attributes attlist) throws FOPException {
        setLocation(locator);
        name = elementName;
        attr = attlist;
    }

    /**
     * @return DOM document representing this foreign XML
     */
    public Document getDOMDocument() {
        return doc;
    }

    public Point2D getDimension(Point2D view) {
         return null;
    }

    /**
     * @return string containing the namespace for this node
     */
    public abstract String getNameSpace();

    /**
     * @return string containing the namespace for this document (which is the
     * same namespace as for this node ??)
     */
    public String getDocumentNamespace() {
        return getNameSpace();
    }

    private static HashMap ns = new HashMap();
    static {
        ns.put("xlink", "http://www.w3.org/1999/xlink");
    }

    /**
     * Add an element to the DOM document
     * @param doc DOM document to which to add an element
     * @param parent the parent element of the element that is being added
     */
    public void addElement(Document doc, Element parent) {
        this.doc = doc;
        element = doc.createElementNS(getNameSpace(), name);

        for (int count = 0; count < attr.getLength(); count++) {
            String rf = attr.getValue(count);
            String qname = attr.getQName(count);
            int idx = qname.indexOf(":");
            if (idx == -1) {
                element.setAttribute(qname, rf);
            } else {
                String pref = qname.substring(0, idx);
                String tail = qname.substring(idx + 1);
                if (pref.equals("xmlns")) {
                    ns.put(tail, rf);
                } else {
                    element.setAttributeNS((String)ns.get(pref), tail, rf);
                }
            }
        }
        attr = null;
        parent.appendChild(element);
    }

    /**
     * Add the top-level element to the DOM document
     * @param doc DOM document
     * @param svgRoot non-XSL-FO element to be added as the root of this document
     */
    public void buildTopLevel(Document doc, Element svgRoot) {
        // build up the info for the top level element
        for (int count = 0; count < attr.getLength(); count++) {
            String rf = attr.getValue(count);
            String qname = attr.getQName(count);
            int idx = qname.indexOf(":");
            if (idx == -1) {
                element.setAttribute(qname, rf);
            } else {
                String pref = qname.substring(0, idx);
                String tail = qname.substring(idx + 1);
                if (pref.equals("xmlns")) {
                    ns.put(tail, rf);
                } else {
                    element.setAttributeNS((String)ns.get(pref), tail, rf);
                }
            }
        }
    }

    /**
     * Create an empty DOM document
     * @return DOM document
     */
    public Document createBasicDocument() {
        doc = null;

        element = null;
        try {
            DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            fact.setNamespaceAware(true);
            doc = fact.newDocumentBuilder().newDocument();
            Element el = doc.createElement(name);
            doc.appendChild(el);

            element = doc.getDocumentElement();
            buildTopLevel(doc, element);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }

    /**
     * @param child FONode child that should be added to this node
     */
    protected void addChild(FONode child) {
        if (child instanceof XMLObj) {
            ((XMLObj)child).addElement(doc, element);
        } else {
            // in theory someone might want to embed some defined
            // xml (eg. fo) inside the foreign xml
            // they could use a different namespace
            getLogger().debug("Invalid element: " + child.getName() + " inside foreign xml markup");
        }
    }

    /**
     * Add parsed characters to this object
     * @param data array of characters contaning the text to add
     * @param start starting array element to add
     * @param length number of characters from the array to add
     * @param locator location in fo source file.
     */
    protected void addCharacters(char data[], int start, int length,
                                 Locator locator) {
        String str = new String(data, start, length - start);
        org.w3c.dom.Text text = doc.createTextNode(str);
        element.appendChild(text);
    }

    /**
     * This is a hook for an FOTreeVisitor subclass to be able to access
     * this object.
     * @param fotv the FOTreeVisitor subclass that can access this object.
     * @see org.apache.fop.fo.FOTreeVisitor
     */
    public void acceptVisitor(FOTreeVisitor fotv) {
        fotv.serveXMLObj(this);
    }

}

