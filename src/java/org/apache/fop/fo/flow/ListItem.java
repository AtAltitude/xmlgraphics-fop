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

// Java
import java.util.List;

// XML
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

// FOP
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.layoutmgr.list.ListItemLayoutManager;

/**
 * Class modelling the fo:list-item object. See Sec. 6.8.3 of the XSL-FO
 * Standard.
 */
public class ListItem extends FObj {

    private ListItemLabel label = null;
    private ListItemBody body = null;

    private int align;
    private int alignLast;
    private int breakBefore;
    private int breakAfter;
    private int lineHeight;
    private int startIndent;
    private int endIndent;
    private int spaceBefore;
    private int spaceAfter;

    /**
     * @param parent FONode that is the parent of this object
     */
    public ListItem(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FObj#addProperties
     */
    protected void addProperties(Attributes attlist) throws SAXParseException {
        super.addProperties(attlist);
        getFOInputHandler().startListItem(this);
    }

    private void setup() {
        setupID();
        this.align = this.propertyList.get(PR_TEXT_ALIGN).getEnum();
        this.alignLast = this.propertyList.get(PR_TEXT_ALIGN_LAST).getEnum();
        this.lineHeight =
            this.propertyList.get(PR_LINE_HEIGHT).getLength().getValue();
        this.spaceBefore =
            this.propertyList.get(PR_SPACE_BEFORE | CP_OPTIMUM).getLength().getValue();
        this.spaceAfter =
            this.propertyList.get(PR_SPACE_AFTER | CP_OPTIMUM).getLength().getValue();
    }

    /**
     * @see org.apache.fop.fo.FONode#addChildNode(FONode)
     */
    public void addChildNode(FONode child) {
        if ("fo:list-item-label".equals(child.getName())) {
            label = (ListItemLabel)child;
        } else if ("fo:list-item-body".equals(child.getName())) {
            body = (ListItemBody)child;
        } else if ("fo:marker".equals(child.getName())) {
            // marker
        } else {
            // error
        }
    }

    /**
     * @return false (ListItem cannot generate inline areas)
     */
    public boolean generatesInlineAreas() {
        return false;
    }

    /**
     * @return true (ListItem can contain Markers)
     */
    protected boolean containsMarkers() {
        return true;
    }

    /**
     * @see org.apache.fop.fo.FObj#addLayoutManager(List)
     * @todo remove checks for non-nulls after validateChildNode() added
     */
    public void addLayoutManager(List list) { 	 
        if (label != null && body != null) {
            ListItemLayoutManager blm = new ListItemLayoutManager(this);
            list.add(blm);
        } else {
            getLogger().error("list-item requires list-item-label and list-item-body");
        }
    }

    public ListItemLabel getLabel() {
        return label;
    }

    public ListItemBody getBody() {
        return body;
    }

    protected void endOfNode() throws SAXParseException {
        super.endOfNode();
        getFOInputHandler().endListItem(this);
    }

    public String getName() {
        return "fo:list-item";
    }
}

