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
import java.util.ArrayList;

// XML
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

// FOP
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.layoutmgr.AddLMVisitor;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAural;
import org.apache.fop.fo.properties.CommonBackground;
import org.apache.fop.fo.properties.CommonBorderAndPadding;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.CommonRelativePosition;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.apache.fop.fo.LMVisited;

/**
 * Class modelling the fo:table object. See Sec. 6.7.3 of the XSL-FO Standard.
 */
public class Table extends FObj implements LMVisited {
    private static final int MINCOLWIDTH = 10000; // 10pt

    /** collection of columns in this table */
    protected ArrayList columns = null;
    private TableBody tableHeader = null;
    private TableBody tableFooter = null;
    private boolean omitHeaderAtBreak = false;
    private boolean omitFooterAtBreak = false;

    private int breakBefore;
    private int breakAfter;
    private int spaceBefore;
    private int spaceAfter;
    private ColorType backgroundColor;
    private LengthRangeProperty ipd;
    private int height;

    private boolean bAutoLayout = false;
    private int contentWidth = 0; // Sum of column widths
    /** Optimum inline-progression-dimension */
    private int optIPD;
    /** Minimum inline-progression-dimension */
    private int minIPD;
    /** Maximum inline-progression-dimension */
    private int maxIPD;

    /**
     * @param parent FONode that is the parent of this object
     */
    public Table(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FObj#addProperties
     */
    protected void addProperties(Attributes attlist) throws SAXParseException {
        super.addProperties(attlist);
        this.breakBefore = this.propertyList.get(PR_BREAK_BEFORE).getEnum();
        this.breakAfter = this.propertyList.get(PR_BREAK_AFTER).getEnum();
        this.spaceBefore = this.propertyList.get(
                             PR_SPACE_BEFORE | CP_OPTIMUM).getLength().getValue();
        this.spaceAfter = this.propertyList.get(
                            PR_SPACE_AFTER | CP_OPTIMUM).getLength().getValue();
        this.backgroundColor =
          this.propertyList.get(PR_BACKGROUND_COLOR).getColorType();
        this.ipd = this.propertyList.get(
                     PR_INLINE_PROGRESSION_DIMENSION).getLengthRange();
        this.height = this.propertyList.get(PR_HEIGHT).getLength().getValue();
        this.bAutoLayout = (this.propertyList.get(
                PR_TABLE_LAYOUT).getEnum() == TableLayout.AUTO);

        this.omitHeaderAtBreak = this.propertyList.get(
                PR_TABLE_OMIT_HEADER_AT_BREAK).getEnum()
                                            == TableOmitHeaderAtBreak.TRUE;
        this.omitFooterAtBreak = this.propertyList.get(
                PR_TABLE_OMIT_FOOTER_AT_BREAK).getEnum()
                                            == TableOmitFooterAtBreak.TRUE;
        getFOInputHandler().startTable(this);
    }

    /**
     * @see org.apache.fop.fo.FONode#addChildNode(FONode)
     */
    protected void addChildNode(FONode child) {
        if (child.getName().equals("fo:table-column")) {
            if (columns == null) {
                columns = new ArrayList();
            }
            columns.add(((TableColumn)child));
        } else if (child.getName().equals("fo:table-footer")) {
            tableFooter = (TableBody)child;
        } else if (child.getName().equals("fo:table-header")) {
            tableHeader = (TableBody)child;
        } else {
            // add bodies
            super.addChildNode(child);
        }
    }

    /**
     * @return false (Table does not generate inline areas)
     */
    public boolean generatesInlineAreas() {
        return false;
    }

    public ArrayList getColumns() {
        return columns;
    }

    public TableBody getTableHeader() {
        return tableHeader;
    }

    public TableBody getTableFooter() {
        return tableFooter;
    }

    /**
     * This is a hook for the AddLMVisitor class to be able to access
     * this object.
     * @param aLMV the AddLMVisitor object that can access this object.
     */
    public void acceptVisitor(AddLMVisitor aLMV) {
        aLMV.serveTable(this);
    }

    protected void endOfNode() throws SAXParseException {
        getFOInputHandler().endTable(this);
    }

    public String getName() {
        return "fo:table";
    }

    /**
     * @see org.apache.fop.fo.FObj#getNameId()
     */
    public int getNameId() {
        return FO_TABLE;
    }
}
