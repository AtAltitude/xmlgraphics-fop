/*
 * $Id: Table.java,v 1.50 2003/03/05 20:38:21 jeremias Exp $
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 *
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package org.apache.fop.fo.flow;

// Java
import java.util.ArrayList;

// XML
import org.xml.sax.Attributes;

// FOP
import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.fo.LengthRangeProperty;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.FOTreeVisitor;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAural;
import org.apache.fop.fo.properties.CommonBackground;
import org.apache.fop.fo.properties.CommonBorderAndPadding;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.CommonRelativePosition;

/**
 * Class modelling the fo:table object. See Sec. 6.7.3 of the XSL-FO Standard.
 */
public class Table extends FObj {
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
     * Overrides FObj.
     * @param child FONode child object to be added
     */
    protected void addChild(FONode child) {
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
            super.addChild(child);
        }
    }

    private void setup() {
        // Common Accessibility Properties
        CommonAccessibility mAccProps = propMgr.getAccessibilityProps();

        // Common Aural Properties
        CommonAural mAurProps = propMgr.getAuralProps();

        // Common Border, Padding, and Background Properties
        CommonBorderAndPadding bap = propMgr.getBorderAndPadding();
        CommonBackground bProps = propMgr.getBackgroundProps();

        // Common Margin Properties-Block
        CommonMarginBlock mProps = propMgr.getMarginProps();

        // Common Relative Position Properties
        CommonRelativePosition mRelProps =
                propMgr.getRelativePositionProps();

        // this.propertyList.get("block-progression-dimension");
        // this.propertyList.get("border-after-precendence");
        // this.propertyList.get("border-before-precedence");
        // this.propertyList.get("border-collapse");
        // this.propertyList.get("border-end-precendence");
        // this.propertyList.get("border-separation");
        // this.propertyList.get("border-start-precendence");
        // this.propertyList.get("break-after");
        // this.propertyList.get("break-before");
        setupID();
        // this.propertyList.get("inline-progression-dimension");
        // this.propertyList.get("height");
        // this.propertyList.get("keep-together");
        // this.propertyList.get("keep-with-next");
        // this.propertyList.get("keep-with-previous");
        // this.propertyList.get("table-layout");
        // this.propertyList.get("table-omit-footer-at-break");
        // this.propertyList.get("table-omit-header-at-break");
        // this.propertyList.get("width");
        // this.propertyList.get("writing-mode");

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

    }

    /**
     * @return false (Table does not generate inline areas)
     */
    public boolean generatesInlineAreas() {
        return false;
    }

    /**
     * @return true (Table contains Markers)
     */
    protected boolean containsMarkers() {
        return true;
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
     * This is a hook for an FOTreeVisitor subclass to be able to access
     * this object.
     * @param fotv the FOTreeVisitor subclass that can access this object.
     * @see org.apache.fop.fo.FOTreeVisitor
     */
    public void acceptVisitor(FOTreeVisitor fotv) {
        fotv.serveTable(this);
    }

    /**
     * @see org.apache.fop.fo.FObj#handleAttrs
     */
    public void handleAttrs(Attributes attlist) throws FOPException {
        super.handleAttrs(attlist);

        setupID();

        getFOTreeControl().getFOInputHandler().startTable(this);
    }

    protected void end() {
        getFOTreeControl().getFOInputHandler().endTable(this);
    }

}
