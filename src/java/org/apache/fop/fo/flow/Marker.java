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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


import org.xml.sax.Locator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.FObjMixed;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.PropertyListMaker;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.Property;

/**
 * Marker formatting object.
 */
public class Marker extends FObjMixed {
    // The value of properties relevant for fo:marker.
    private String markerClassName;
    // End of property values

    private MarkerPropertyList propertyList;
    private PropertyListMaker savePropertyListMaker;
    private HashMap children = new HashMap();

    /**
     * Create a marker fo.
     * @param parent the parent fo node
     */
    public Marker(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FObj#bind(PropertyList)
     */
    public void bind(PropertyList pList) throws FOPException {
        markerClassName = pList.get(PR_MARKER_CLASS_NAME).getString();
    }
    
    /**
     * Rebind the marker and all the children using the specified 
     * parentPropertyList which comes from the fo:retrieve-marker element.
     * @param parentPropertyList The property list from fo:retrieve-marker.
     */
    public void rebind(PropertyList parentPropertyList) throws FOPException {
        // Set a new parent property list and bind all the children again.
        propertyList.setParentPropertyList(parentPropertyList);
        for (Iterator i = children.keySet().iterator(); i.hasNext(); ) {
            FONode child = (FONode) i.next();
            PropertyList childList = (PropertyList) children.get(child);
            if (child instanceof FObj) {
                ((FObj) child).bind(childList);
            } else if (child instanceof FOText) {
                ((FOText) child).bind(childList);
            }
        }
    }

    protected PropertyList createPropertyList(PropertyList parent, FOEventHandler foEventHandler) throws FOPException {
        propertyList = new MarkerPropertyList(this, parent);
        return propertyList;
    }

    protected void startOfNode() {
        FOEventHandler foEventHandler = getFOEventHandler(); 
        // Push a new property list maker which will make MarkerPropertyLists.
        savePropertyListMaker = foEventHandler.getPropertyListMaker();
        foEventHandler.setPropertyListMaker(new PropertyListMaker() {
            public PropertyList make(FObj fobj, PropertyList parentPropertyList) {
                PropertyList pList = new MarkerPropertyList(fobj, parentPropertyList);
                children.put(fobj, pList);
                return pList;
            }
        });
    }

    protected void addChildNode(FONode child) throws FOPException {
        if (!children.containsKey(child)) {
            children.put(child, propertyList);
        }
        super.addChildNode(child);
    }

    protected void endOfNode() {
        // Pop the MarkerPropertyList maker.
        getFOEventHandler().setPropertyListMaker(savePropertyListMaker);
        savePropertyListMaker = null;
    }

    /**
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
     * XSL Content Model: (#PCDATA|%inline;|%block;)*
     * Additionally: "An fo:marker may contain any formatting objects that 
     * are permitted as a replacement of any fo:retrieve-marker that retrieves
     * the fo:marker's children."
     * @todo implement "additional" constraint, possibly within fo:retrieve-marker
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws ValidationException {
        if (!isBlockOrInlineItem(nsURI, localName)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /**
     * @see org.apache.fop.fo.FONode#addLayoutManager(List)
     * @todo remove null check when vCN() & endOfNode() implemented
     */
    public void addLayoutManager(List list) {
        ListIterator baseIter = getChildNodes();
        if (baseIter == null) {
            return;
        }
        while (baseIter.hasNext()) {
            FONode child = (FONode) baseIter.next();
            child.addLayoutManager(list);
        }
    }

    /**
     * Return the "marker-class-name" property.
     */
    public String getMarkerClassName() {
        return markerClassName;
    }

    /**
     * @see org.apache.fop.fo.FObj#getName()
     */
    public String getName() {
        return "fo:marker";
    }
    
    /**
     * @see org.apache.fop.fo.FObj#getNameId()
     */
    public int getNameId() {
        return FO_MARKER;
    }

    /**
     * An implementation of PropertyList which only stores the explicit
     * assigned properties. It is memory efficient but slow. 
     */
    public class MarkerPropertyList extends PropertyList {
        HashMap explicit = new HashMap();
        public MarkerPropertyList(FObj fobj, PropertyList parentPropertyList) {
            super(fobj, parentPropertyList);
        }
        
        /**
         * Set the parent property list. Used to assign a new parent 
         * before re-binding all the child elements.   
         */
        public void setParentPropertyList(PropertyList parentPropertyList) {
            this.parentPropertyList = parentPropertyList;
        }

        public void putExplicit(int propId, Property value) {
            explicit.put(new Integer(propId), value);
        }

        public Property getExplicit(int propId) {
            return (Property) explicit.get(new Integer(propId));
        }
    }

}
