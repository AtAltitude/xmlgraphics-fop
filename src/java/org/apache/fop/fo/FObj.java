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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.flow.Marker;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.properties.PropertyMaker;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Base class for representation of formatting objects and their processing.
 */
public class FObj extends FONode implements Constants {
    public static PropertyMaker[] propertyListTable = null;
    
    /** Formatting properties for this fo element. */
    protected PropertyList propertyList;

    /** Property manager for providing refined properties/traits. */
    protected PropertyManager propMgr;

    /** Id of this fo element of null if no id. */
    protected String id = null;

    /** The children of this node. */
    public ArrayList children = null;

    /** Markers added to this element. */
    protected Map markers = null;

    /** Dynamic layout dimension. Used to resolve relative lengths. */
    protected Map layoutDimension = null;

    /**
     * Create a new formatting object.
     * All formatting object classes extend this class.
     *
     * @param parent the parent node
     */
    public FObj(FONode parent) {
        super(parent);

        if (propertyListTable == null) {
            propertyListTable = new PropertyMaker[Constants.PROPERTY_COUNT+1];
            PropertyMaker[] list = FOPropertyMapping.getGenericMappings();
            for (int i = 1; i < list.length; i++) {
                if (list[i] != null)
                    propertyListTable[i] = list[i]; 
            }    
        }
    }

    /**
     * @see org.apache.fop.fo.FONode#processNode
     */
    public void processNode(String elementName, Locator locator, 
                            Attributes attlist) throws FOPException {
        setLocation(locator);
        addProperties(attlist);
    }

    /**
     * Set the name of this element.
     * The prepends "fo:" to the name to indicate it is in the fo namespace.
     * @param str the xml element name
     */
    public void setName(String str) {
        name = "fo:" + str;
    }

    /**
     * Set properties for this FO based on node attributes
     * @param attlist Collection of attributes passed to us from the parser.
     */
    protected void addProperties(Attributes attlist) throws FOPException {
        FObj parentFO = findNearestAncestorFObj();
        PropertyList parentPL = null;

        if (parentFO != null) {
            parentPL = parentFO.getPropertiesForNamespace(FOElementMapping.URI);
        }

        propertyList = new PropertyList(this, parentPL, FOElementMapping.URI);
        propertyList.addAttributesToList(attlist);
        propMgr = new PropertyManager(propertyList);
        setWritingMode();
    }

    /**
     * Return the PropertyManager object for this FO.  PropertyManager
     * tends to hold the traits for this FO, and is primarily used in layout.
     * @return the property manager for this FO
     */
    public PropertyManager getPropertyManager() {
        return propMgr;
    }

    /**
     * Return the property list object for this FO.  PropertyList tends
     * to hold the base, pre-trait properties for this FO, either explicitly
     * declared in the input XML or from inherited values.
     */
    public PropertyList getPropertyList() {
        return propertyList;
    }

    /**
     * Helper method to quickly obtain the value of a property
     * for this FO, without querying for the propertyList first.
     * @param name - the name of the desired property to obtain
     * @return the property
     */
    public Property getProperty(int propId) {
        return propertyList.get(propId);
    }

   /**
     * Find the nearest parent, grandparent, etc. FONode that is also an FObj
     * @return FObj the nearest ancestor FONode that is an FObj
     */
    public FObj findNearestAncestorFObj() {
      FONode par = parent;
      while (par != null && !(par instanceof FObj)) {
          par = par.parent;
      }
      return (FObj) par;
    }

    /**
     * Find nearest ancestor which generates Reference Areas.
     *
     * @param includeSelf Set to true to consider the current FObj as an
     * "ancestor". Set to false to only return a true ancestor.
     * @param returnRoot Supposing a condition where no appropriate ancestor
     * FObj is found, setting returnRoot to true will return the FObj with no
     * parent (presumably the root FO). Otherwise, null will be returned.
     * Note that this will override a false setting for includeSelf, and return
     * the current node if it is the root FO. Setting returnRoot to true should
     * always return a valid FObj.
     * @return FObj of the nearest ancestor that generates Reference Areas
     * and fits the parameters.
     */
    private FObj findNearestAncestorGeneratingRAs(boolean includeSelf,
                                                  boolean returnRoot) {
        FObj p = this;
        if (includeSelf && p.generatesReferenceAreas()) {
            return p;
        }
        FObj parent = p.findNearestAncestorFObj();
        if (parent == null && returnRoot) {
            return p;
        }
        do {
            p = parent;
            parent = p.findNearestAncestorFObj();
        } while (parent != null && !p.generatesReferenceAreas());
        if (p.generatesReferenceAreas()) {
            return p;
        }
        // if we got here, it is because parent is null
        if (returnRoot) {
            return p;
        } else {
            return null;
        }
    }

    /**
     * For a given namespace, determine whether the properties of this object
     * match that namespace.
     * @param nameSpaceURI the namespace URI to be tested against
     * @return this.propertyList, if the namespaces match; otherwise, null
     */
    public PropertyList getPropertiesForNamespace(String nameSpaceURI) {
        if (this.propertyList == null) {
            return null;
        }
        if (!nameSpaceURI.equals(this.propertyList.getNameSpace())) {
            return null;
        }
        return this.propertyList;
    }

    /* This section is the implemenation of the property context. */

    /**
     * Assign the size of a layout dimension to the key. 
     * @param key the Layout dimension, from PercentBase.
     * @param dimension The layout length.
     */
    public void setLayoutDimension(Integer key, int dimension) {
        if (layoutDimension == null){
            layoutDimension = new HashMap();
        }
        layoutDimension.put(key, new Integer(dimension));
    }
    
    /**
     * Assign the size of a layout dimension to the key. 
     * @param key the Layout dimension, from PercentBase.
     * @param dimension The layout length.
     */
    public void setLayoutDimension(Integer key, float dimension) {
        if (layoutDimension == null){
            layoutDimension = new HashMap();
        }
        layoutDimension.put(key, new Float(dimension));
    }
    
    /**
     * Return the size associated with the key.
     * @param key The layout dimension key.
     * @return the length.
     */
    public Number getLayoutDimension(Integer key) {
        if (layoutDimension != null) {
            Number result = (Number) layoutDimension.get(key);
            if (result != null) {
                return result;
            }
        }
        if (parent != null) {
            return ((FObj) parent).getLayoutDimension(key);
        }
        return new Integer(0);
    }

    /**
     * Add the child to this object.
     *
     * @param child the child node to add
     */
    protected void addChild(FONode child) {
        if (containsMarkers() && child.isMarker()) {
            addMarker((Marker)child);
        } else {
            if (children == null) {
                children = new ArrayList();
            }
            children.add(child);
        }
    }

    /**
     * Setup the id for this formatting object.
     * Most formatting objects can have an id that can be referenced.
     * This methods checks that the id isn't already used by another
     * fo and sets the id attribute of this object.
     */
    public void setupID() {
        Property prop = this.propertyList.get(PR_ID);
        if (prop != null) {
            String str = prop.getString();
            if (str != null && !str.equals("")) {
                Set idrefs = getFOInputHandler().getIDReferences();
                if (!idrefs.contains(str)) {
                    id = str;
                    idrefs.add(id);
                } else {
                    getLogger().warn("duplicate id:" + str + " ignored");
                }
            }
        }
    }

    /**
     * Get the id string for this formatting object.
     * This will be unique for the fo document.
     *
     * @return the id string or null if not set
     */
    public String getID() {
        return id;
    }

    /**
     * Check if this formatting object generates reference areas.
     *
     * @return true if generates reference areas
     */
    public boolean generatesReferenceAreas() {
        return false;
    }

    /**
     * Check if this formatting object generates inline areas.
     *
     * @return true if generates inline areas
     */
    public boolean generatesInlineAreas() {
        return true;
    }

    /**
     * Check if this formatting object may contain markers.
     *
     * @return true if this can contian markers
     */
    protected boolean containsMarkers() {
        return false;
    }

    /**
     * Set writing mode for this FO.
     * Use that from the nearest ancestor, including self, which generates
     * reference areas, or from root FO if no ancestor found.
     */
    protected void setWritingMode() {
        FObj p = findNearestAncestorGeneratingRAs(true, true);
        this.propertyList.setWritingMode(
          p.getProperty(PR_WRITING_MODE).getEnum());
    }

    /**
     * Return an iterator over all the children of this FObj.
     * @return A ListIterator.
     */
    public ListIterator getChildren() {
        if (children != null) {
            return children.listIterator();
        }
        return null;
    }

    /**
     * Return an iterator over the object's children starting
     * at the pased node.
     * @param childNode First node in the iterator
     * @return A ListIterator or null if childNode isn't a child of
     * this FObj.
     */
    public ListIterator getChildren(FONode childNode) {
        if (children != null) {
            int i = children.indexOf(childNode);
            if (i >= 0) {
                return children.listIterator(i);
            }
        }
        return null;
    }

   /**
     * Add the marker to this formatting object.
     * If this object can contain markers it checks that the marker
     * has a unique class-name for this object and that it is
     * the first child.
     * @param marker Marker to add.
     */
    public void addMarker(Marker marker) {
        String mcname = marker.getMarkerClassName();
        if (children != null) {
            // check for empty children
            for (Iterator iter = children.iterator(); iter.hasNext();) {
                FONode node = (FONode)iter.next();
                if (node instanceof FOText) {
                    FOText text = (FOText)node;
                    if (text.willCreateArea()) {
                        getLogger().error("fo:marker must be an initial child: " + mcname);
                        return;
                    } else {
                        iter.remove();
                    }
                } else {
                    getLogger().error("fo:marker must be an initial child: " + mcname);
                    return;
                }
            }
        }
        if (markers == null) {
            markers = new HashMap();
        }
        if (!markers.containsKey(mcname)) {
            markers.put(mcname, marker);
        } else {
            getLogger().error("fo:marker 'marker-class-name' "
                    + "must be unique for same parent: " + mcname);
        }
    }

    /**
     * @return true if there are any Markers attached to this object
     */
    public boolean hasMarkers() {
        return markers != null && !markers.isEmpty();
    }

    /**
     * @return th collection of Markers attached to this object
     */
    public Map getMarkers() {
        return markers;
    }

    /**
     * This is a hook for an FOTreeVisitor subclass to be able to access
     * this object.
     * @param fotv the FOTreeVisitor subclass that can access this object.
     * @see org.apache.fop.fo.FOTreeVisitor
     */
    public void acceptVisitor(FOTreeVisitor fotv) {
        fotv.serveFObj(this);
    }
    

    /*
     * Return a string representation of the fo element.
     * Deactivated in order to see precise ID of each fo element created
     *    (helpful for debugging)
     */
/*    public String toString() {
        return getName() + " at line " + line + ":" + column;
    }
*/    

    public String getName() {
        return null;
    }

    /**
     * Convenience method for validity checking.  Checks if the
     * incoming node is a member of the "%block;" parameter entity
     * as defined in Sect. 6.2 of the XSL 1.0 & 1.1 Recommendations
     * @param nsURI namespace URI of incoming invalid node
     * @param lName local name (i.e., no prefix) of incoming node 
     * @return true if a member, false if not
     */
    protected static boolean isBlockItem(String nsURI, String lName) {
        return (nsURI == FOElementMapping.URI && 
            (lName.equals("block") 
            || lName.equals("table") 
            || lName.equals("table-and-caption") 
            || lName.equals("block-container")
            || lName.equals("list-block") 
            || lName.equals("float")))
            || isNeutralItem(nsURI, lName);
    }

    /**
     * Convenience method for validity checking.  Checks if the
     * incoming node is a member of the neutral item list
     * as defined in Sect. 6.2 of the XSL 1.0 & 1.1 Recommendations
     * @param nsURI namespace URI of incoming invalid node
     * @param lName local name (i.e., no prefix) of incoming node 
     * @return true if a member, false if not
     */
    protected static boolean isNeutralItem(String nsURI, String lName) {
        return (nsURI == FOElementMapping.URI && 
            (lName.equals("multi-switch") 
            || lName.equals("multi-properties")
            || lName.equals("wrapper") 
            || lName.equals("retrieve-marker")));
    }
}

