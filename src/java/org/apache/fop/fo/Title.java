/*
 * $Id: Title.java,v 1.15 2003/03/05 21:48:01 jeremias Exp $
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
package org.apache.fop.fo;

// FOP
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.datatypes.Length;
import org.apache.fop.layout.AccessibilityProps;
import org.apache.fop.layout.AuralProps;
import org.apache.fop.layout.BackgroundProps;
import org.apache.fop.layout.BorderAndPadding;
import org.apache.fop.layout.FontState;
import org.apache.fop.layout.MarginInlineProps;
import org.apache.fop.layoutmgr.ContentLayoutManager;
import org.apache.fop.layoutmgr.InlineStackingLayoutManager;
import org.apache.fop.layoutmgr.LMiter;

/**
 * Class modelling the fo:title object. See Sec. 6.4.20 in the XSL-FO Standard.
 */
public class Title extends FObjMixed {

    /**
     * @param parent FONode that is the parent of this object
     */
    public Title(FONode parent) {
        super(parent);
    }

    /**
     * TODO: shouldn't this code be in Layout??
     * @return the Title area
     */
    public org.apache.fop.area.Title getTitleArea() {
        org.apache.fop.area.Title title =
                 new org.apache.fop.area.Title();
        // use special layout manager to add the inline areas
        // to the Title.
        InlineStackingLayoutManager lm;
        lm = new InlineStackingLayoutManager();
        lm.setUserAgent(getUserAgent());
        lm.setFObj(this);
        lm.setLMiter(new LMiter(children.listIterator()));
        lm.init();

        // get breaks then add areas to title

        ContentLayoutManager clm = new ContentLayoutManager(title);
        clm.setUserAgent(getUserAgent());
        lm.setParent(clm);

        clm.fillArea(lm);

        return title;
    }

    private void setup() {

        // Common Accessibility Properties
        AccessibilityProps mAccProps = propMgr.getAccessibilityProps();

        // Common Aural Properties
        AuralProps mAurProps = propMgr.getAuralProps();

        // Common Border, Padding, and Background Properties
        BorderAndPadding bap = propMgr.getBorderAndPadding();
        BackgroundProps bProps = propMgr.getBackgroundProps();

        // Common Font Properties
        FontState fontState = propMgr.getFontState(structHandler.getFontInfo());

        // Common Margin Properties-Inline
        MarginInlineProps mProps = propMgr.getMarginInlineProps();

        Property prop;
        prop = this.properties.get("baseline-shift");
        if (prop instanceof LengthProperty) {
            Length bShift = prop.getLength();
        } else if (prop instanceof EnumProperty) {
            int bShift = prop.getEnum();
        }
        ColorType col = this.properties.get("color").getColorType();
        Length lHeight = this.properties.get("line-height").getLength();
        int lShiftAdj = this.properties.get(
                          "line-height-shift-adjustment").getEnum();
        int vis = this.properties.get("visibility").getEnum();
        Length zIndex = this.properties.get("z-index").getLength();

    }
}

