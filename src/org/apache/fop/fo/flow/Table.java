/*
 * $Id$
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
import java.util.List;

// FOP
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.datatypes.LengthRange;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.properties.TableLayout;
import org.apache.fop.fo.properties.TableOmitFooterAtBreak;
import org.apache.fop.fo.properties.TableOmitHeaderAtBreak;
import org.apache.fop.layout.AccessibilityProps;
import org.apache.fop.layout.AuralProps;
import org.apache.fop.layout.BackgroundProps;
import org.apache.fop.layout.BorderAndPadding;
import org.apache.fop.layout.MarginProps;
import org.apache.fop.layout.RelativePositionProps;
import org.apache.fop.layoutmgr.table.TableLayoutManager;

public class Table extends FObj {
    private static final int MINCOLWIDTH = 10000; // 10pt

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
    private LengthRange ipd;
    private int height;

    private boolean bAutoLayout = false;
    private int contentWidth = 0; // Sum of column widths
    /** Optimum inline-progression-dimension */
    private int optIPD;
    /** Minimum inline-progression-dimension */
    private int minIPD;
    /** Maximum inline-progression-dimension */
    private int maxIPD;

    public Table(FONode parent) {
        super(parent);
    }

    protected void addChild(FONode child) {
        if (child.getName().equals("fo:table-column")) {
            if (columns == null) {
                columns = new ArrayList();
            }
            columns.add(((TableColumn)child).getLayoutManager());
        } else if (child.getName().equals("fo:table-footer")) {
            tableFooter = (TableBody)child;
        } else if (child.getName().equals("fo:table-header")) {
            tableHeader = (TableBody)child;
        } else {
            // add bodies
            super.addChild(child);
        }
    }

    /**
     * Return a LayoutManager responsible for laying out this FObj's content.
     * Must override in subclasses if their content can be laid out.
     */
    public void addLayoutManager(List list) {
        TableLayoutManager tlm = new TableLayoutManager();
        tlm.setUserAgent(getUserAgent());
        tlm.setFObj(this);
        tlm.setColumns(columns);
        if (tableHeader != null) {
            tlm.setTableHeader(tableHeader.getLayoutManager());
        }
        if (tableFooter != null) {
            tlm.setTableFooter(tableFooter.getLayoutManager());
        }
        list.add(tlm);
    }

    public void setup() {
        // Common Accessibility Properties
        AccessibilityProps mAccProps = propMgr.getAccessibilityProps();

        // Common Aural Properties
        AuralProps mAurProps = propMgr.getAuralProps();

        // Common Border, Padding, and Background Properties
        BorderAndPadding bap = propMgr.getBorderAndPadding();
        BackgroundProps bProps = propMgr.getBackgroundProps();

        // Common Margin Properties-Block
        MarginProps mProps = propMgr.getMarginProps();

        // Common Relative Position Properties
        RelativePositionProps mRelProps =
                propMgr.getRelativePositionProps();

        // this.properties.get("block-progression-dimension");
        // this.properties.get("border-after-precendence");
        // this.properties.get("border-before-precedence");
        // this.properties.get("border-collapse");
        // this.properties.get("border-end-precendence");
        // this.properties.get("border-separation");
        // this.properties.get("border-start-precendence");
        // this.properties.get("break-after");
        // this.properties.get("break-before");
        setupID();
        // this.properties.get("inline-progression-dimension");
        // this.properties.get("height");
        // this.properties.get("keep-together");
        // this.properties.get("keep-with-next");
        // this.properties.get("keep-with-previous");
        // this.properties.get("table-layout");
        // this.properties.get("table-omit-footer-at-break");
        // this.properties.get("table-omit-header-at-break");
        // this.properties.get("width");
        // this.properties.get("writing-mode");

        this.breakBefore = this.properties.get("break-before").getEnum();
        this.breakAfter = this.properties.get("break-after").getEnum();
        this.spaceBefore = this.properties.get(
                             "space-before.optimum").getLength().getValue();
        this.spaceAfter = this.properties.get(
                            "space-after.optimum").getLength().getValue();
        this.backgroundColor =
          this.properties.get("background-color").getColorType();
        this.ipd = this.properties.get(
                     "inline-progression-dimension").getLengthRange();
        this.height = this.properties.get("height").getLength().getValue();
        this.bAutoLayout = (this.properties.get(
                "table-layout").getEnum() == TableLayout.AUTO);

        this.omitHeaderAtBreak = this.properties.get(
                "table-omit-header-at-break").getEnum() 
                                            == TableOmitHeaderAtBreak.TRUE;
        this.omitFooterAtBreak = this.properties.get(
                "table-omit-footer-at-break").getEnum() 
                                            == TableOmitFooterAtBreak.TRUE;

    }

    public boolean generatesInlineAreas() {
        return false;
    }

    protected boolean containsMarkers() {
        return true;
    }

}

