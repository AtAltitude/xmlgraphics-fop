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
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// XML
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

// FOP
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.util.CharUtilities;
import org.apache.fop.fo.extensions.ExtensionElementMapping;
import org.apache.fop.fo.extensions.svg.SVGElementMapping;

/**
 * base class for nodes in the XML tree
 */
public abstract class FONode {

    protected static String FO_URI = FOElementMapping.URI;

    /** Parent FO node */
    protected FONode parent;

    /**  Marks location of this object from the input FO
     *   Call locator.getSystemId(), getLineNumber(),
     *   getColumnNumber() for file, line, column
     *   information
     */
    public Locator locator;

    /** Logger for fo-tree related messages **/
    private static Log log = LogFactory.getLog(FONode.class);

    /**
     * Main constructor.
     * @param parent parent of this node
     */
    protected FONode(FONode parent) {
        this.parent = parent;
    }

    /**
     * Set the location information for this element
     * @param locator the org.xml.sax.Locator object
     */
    public void setLocator(Locator locator) {
        if (locator != null) {
            this.locator = locator;
        }
    }

    /**
     * Recursively goes up the FOTree hierarchy until the fo:root is found,
     * which returns the parent FOEventHandler.
     * @return the FOEventHandler object that is the parent of the FO Tree
     */
    public FOEventHandler getFOEventHandler() {
        return parent.getFOEventHandler();
    }

    /**
     * Returns the user agent for the node.
     * @return FOUserAgent
     */
    public FOUserAgent getUserAgent() {
        return getFOEventHandler().getUserAgent();
    }

    /**
     * Returns the logger for the node.
     * @return the logger
     */
    public Log getLogger() {
        return log;
    }

    /**
     * Initialize the node with its name, location information, and attributes
     * The attributes must be used immediately as the sax attributes
     * will be altered for the next element.
     * @param elementName element name (e.g., "fo:block")
     * @param locator Locator object (ignored by default)
     * @param attlist Collection of attributes passed to us from the parser.
     * @throws SAXParseException for errors or inconsistencies in the attributes
    */
    public void processNode(String elementName, Locator locator, Attributes attlist) throws SAXParseException {
        System.out.println("name = " + elementName);
    }

    /**
     * Create a property list for this node. Return null if the node does not
     * need a property list.
     * @param parent the closest parent propertylist. 
     * @param foEventHandler The FOEventHandler where the PropertyListMaker 
     *              instance can be found.
     * @return A new property list.
     */
    protected PropertyList createPropertyList(PropertyList parent, FOEventHandler foEventHandler) throws SAXParseException {
        return null;
    }

    /**
     * Checks to make sure, during SAX processing of input document, that the
     * incoming node is valid for the this (parent) node (e.g., checking to
     * see that fo:table is not an immediate child of fo:root)
     * called within FObj constructor
     * @param namespaceURI namespace of incoming node
     * @param localName (e.g. "table" for "fo:table")
     * @throws SAXParseException if incoming node not valid for parent
     */
    protected void validateChildNode(Locator loc, String namespaceURI, String localName) 
        throws SAXParseException {}

    /**
     * Adds characters (does nothing here)
     * @param data text
     * @param start start position
     * @param length length of the text
     * @param locator location in fo source file. 
     */
    protected void addCharacters(char data[], int start, int length,
                                 Locator locator) throws SAXParseException {
        // ignore
    }

    /**
    *
    */
    protected void startOfNode() throws SAXParseException {
        // do nothing by default
   }

    /**
     *
     */
    protected void endOfNode() throws SAXParseException {
        // do nothing by default
    }

    /**
     * @param child child node to be added to the childNodes of this node
     */
    protected void addChildNode(FONode child) throws SAXParseException {
    }

    /**
     * @return the parent node of this node
     */
    public FONode getParent() {
        return this.parent;
    }

    /**
     * Return an iterator over all the child nodes of this FObj.
     * @return A ListIterator.
     */
    public ListIterator getChildNodes() {
        return null;
    }

    /**
     * Return an iterator over the object's child nodes starting
     * at the pased node.
     * @param childNode First node in the iterator
     * @return A ListIterator or null if child node isn't a child of
     * this FObj.
     */
    public ListIterator getChildNodes(FONode childNode) {
        return null;
    }

    /**
     * @return an iterator for the characters in this node
     */
    public CharIterator charIterator() {
        return new OneCharIterator(CharUtilities.CODE_EOT);
    }

    /**
     * Helper function to standardize the names of all namespace URI - local
     * name pairs in text messages.
     * For readability, using fo:, fox:, svg:, for those namespaces even
     * though that prefix may not have been chosen in the document.
     * @param namespaceURI URI of node found 
     *         (e.g., "http://www.w3.org/1999/XSL/Format")
     * @param localName local name of node, (e.g., "root" for "fo:root")
     * @return the prefix:localname, if fo/fox/svg, or a longer representation
     * with the unabbreviated URI otherwise.
     */
    public static String getNodeString(String namespaceURI, String localName) {
        if (namespaceURI.equals(FOElementMapping.URI)) {
            return "fo:" + localName;
        } else if (namespaceURI.equals(ExtensionElementMapping.URI)) {
            return "fox:" + localName;
        } else if (namespaceURI.equals(SVGElementMapping.URI)) {
            return "svg:" + localName;
        } else
            return "(Namespace URI: \"" + namespaceURI + "\", " +
                "Local Name: \"" + localName + "\")";
    }

    /**
     * Helper function to standardize "too many" error exceptions
     * (e.g., two fo:declarations within fo:root)
     * @param loc org.xml.sax.Locator object of the error (*not* parent node)
     * @param offendingNode incoming node that would cause a duplication.
     */
    protected void attributeError(String problem) 
        throws SAXParseException {
        throw new SAXParseException (errorText(locator) + getName() + ", " + 
            problem, locator);
    }

    /**
     * Helper function to standardize "too many" error exceptions
     * (e.g., two fo:declarations within fo:root)
     * @param loc org.xml.sax.Locator object of the error (*not* parent node)
     * @param nsURI namespace URI of incoming invalid node
     * @param lName local name (i.e., no prefix) of incoming node 
     */
    protected void tooManyNodesError(Locator loc, String nsURI, String lName) 
        throws SAXParseException {
        throw new SAXParseException (errorText(loc) + getName() + ", only one " 
            + getNodeString(nsURI, lName) + " may be declared.", loc);
    }

    /**
     * Helper function to standardize "too many" error exceptions
     * (e.g., two fo:declarations within fo:root)
     * This overrloaded method helps make the caller code better self-documenting
     * @param loc org.xml.sax.Locator object of the error (*not* parent node)
     * @param offendingNode incoming node that would cause a duplication.
     */
    protected void tooManyNodesError(Locator loc, String offendingNode) 
        throws SAXParseException {
        throw new SAXParseException (errorText(loc) + getName() + ", only one " 
            + offendingNode + " may be declared.", loc);
    }

    /**
     * Helper function to standardize "out of order" exceptions
     * (e.g., fo:layout-master-set appearing after fo:page-sequence)
     * @param loc org.xml.sax.Locator object of the error (*not* parent node)
     * @param tooLateNode string name of node that should be earlier in document
     * @param tooEarlyNode string name of node that should be later in document
     */
    protected void nodesOutOfOrderError(Locator loc, String tooLateNode, 
        String tooEarlyNode) throws SAXParseException {
        throw new SAXParseException (errorText(loc) + "For " + getName() + ", " + tooLateNode 
            + " must be declared before " + tooEarlyNode + ".", loc);
    }
    
    /**
     * Helper function to return "invalid child" exceptions
     * (e.g., fo:block appearing immediately under fo:root)
     * @param loc org.xml.sax.Locator object of the error (*not* parent node)
     * @param nsURI namespace URI of incoming invalid node
     * @param lName local name (i.e., no prefix) of incoming node 
     */
    protected void invalidChildError(Locator loc, String nsURI, String lName) 
        throws SAXParseException {
        invalidChildError(loc, nsURI, lName, null);
    }
    
    /**
     * Helper function to return "invalid child" exceptions with more
     * complex validation rules (i.e., needing more explanation of the problem)
     * @param loc org.xml.sax.Locator object of the error (*not* parent node)
     * @param nsURI namespace URI of incoming invalid node
     * @param lName local name (i.e., no prefix) of incoming node
     * @param ruleViolated text explanation of problem
     */
    protected void invalidChildError(Locator loc, String nsURI, String lName,
        String ruleViolated)
        throws SAXParseException {
        throw new SAXParseException (errorText(loc) + getNodeString(nsURI, lName) + 
            " is not a valid child element of " + getName() 
            + ((ruleViolated != null) ? ": " + ruleViolated : "."), loc);
    }

    /**
     * Helper function to return missing child element errors
     * (e.g., fo:layout-master-set not having any page-master child element)
     * @param contentModel The XSL Content Model for the fo: object.
     * or a similar description indicating child elements needed.
     */
    protected void missingChildElementError(String contentModel)
        throws SAXParseException {
        throw new SAXParseException(errorText(locator) + getName() + 
            " is missing child elements. \nRequired Content Model: " 
            + contentModel, locator);
    }

    /**
     * Helper function to return missing child element errors
     * (e.g., fo:layout-master-set not having any page-master child element)
     * @param contentModel The XSL Content Model for the fo: object.
     * or a similar description indicating child elements needed.
     */
    protected void missingPropertyError(String propertyName)
        throws SAXParseException {
        throw new SAXParseException(errorText(locator) + getName() +
            " is missing required \"" + propertyName + "\" property.", locator);
    }

    /**
     * Helper function to return "Error (line#/column#)" string for
     * above exception messages
     * @param loc org.xml.sax.Locator object
     * @return String opening error text
     */
    protected static String errorText(Locator loc) {
        if (loc == null) {
            return "Error(Unknown location): ";
        } else {
            return "Error(" + loc.getLineNumber() + "/" + loc.getColumnNumber() + "): ";
        }
    }

    /**
     * Return a LayoutManager responsible for laying out this FObj's content.
     * Must override in subclasses if their content can be laid out.
     * @param list the list to which the layout manager(s) should be added
     */
    public void addLayoutManager(List list) {
    }

    /**
     * Returns the name of the node
     * @return the name of this node
     */
    public String getName() {
        return null;
    }

    /**
     * Returns the Constants class integer value of this node
     * @return the integer enumeration of this FO (e.g., FO_ROOT)
     *      if a formatting object, FO_UNKNOWN_NODE otherwise
     */
    public int getNameId() {
        return Constants.FO_UNKNOWN_NODE;
    }
}

