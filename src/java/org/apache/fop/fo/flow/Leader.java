/*
 * $Id: Leader.java,v 1.35 2003/03/06 11:36:30 jeremias Exp $
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

import org.apache.fop.datatypes.ColorType;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.PercentLength;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOTreeVisitor;
import org.apache.fop.fo.FObjMixed;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAural;
import org.apache.fop.fo.properties.CommonBackground;
import org.apache.fop.fo.properties.CommonBorderAndPadding;
import org.apache.fop.fo.properties.CommonMarginInline;
import org.apache.fop.fo.properties.CommonRelativePosition;
import org.apache.fop.fo.properties.FOPropertyMapping;
import org.apache.fop.fo.properties.LeaderPattern;
import org.apache.fop.fonts.Font;

/**
 * Class modelling fo:leader object. See Sec. 6.6.9 of the XSL-FO Standard.
 * The main property of fo:leader is leader-pattern.
 * The following patterns are treated: rule, space, dots and use-content.
 */
public class Leader extends FObjMixed {

    private int ruleStyle;
    private int ruleThickness;
    private int leaderPattern;
    private int patternWidth;
    /** FontState for this object */
    protected Font fontState;

    /**
     * @param parent FONode that is the parent of this object
     */
    public Leader(FONode parent) {
        super(parent);
    }

    public void setup() {

        // Common Accessibility Properties
        CommonAccessibility mAccProps = propMgr.getAccessibilityProps();

        // Common Aural Properties
        CommonAural mAurProps = propMgr.getAuralProps();

        // Common Border, Padding, and Background Properties
        CommonBorderAndPadding bap = propMgr.getBorderAndPadding();
        CommonBackground bProps = propMgr.getBackgroundProps();

        // Common Font Properties
        this.fontState = propMgr.getFontState(getFOTreeControl());

        // Common Margin Properties-Inline
        CommonMarginInline mProps = propMgr.getMarginInlineProps();

        // Common Relative Position Properties
        CommonRelativePosition mRelProps = propMgr.getRelativePositionProps();

        // this.propertyList.get("alignment-adjust");
        // this.propertyList.get("alignment-baseline");
        // this.propertyList.get("baseline-shift");
        // this.propertyList.get("color");
        // this.propertyList.get("dominant-baseline");
        // this.propertyList.get("text-depth");
        // this.propertyList.get("text-altitude");
        setupID();
        // this.propertyList.get("leader-alignment");
        // this.propertyList.get("leader-length");
        // this.propertyList.get("leader-pattern");
        // this.propertyList.get("leader-pattern-width");
        // this.propertyList.get("rule-style");
        // this.propertyList.get("rule-thickness");
        // this.propertyList.get("letter-spacing");
        // this.propertyList.get("line-height");
        // this.propertyList.get("line-height-shift-adjustment");
        // this.propertyList.get("text-shadow");
        // this.propertyList.get("visibility");
        // this.propertyList.get("word-spacing");
        // this.propertyList.get("z-index");

        // color properties
        ColorType c = this.propertyList.get(PR_COLOR).getColorType();
        float red = c.getRed();
        float green = c.getGreen();
        float blue = c.getBlue();

        // fo:leader specific properties
        // determines the pattern of leader; allowed values: space, rule,dots, use-content
        leaderPattern = this.propertyList.get(PR_LEADER_PATTERN).getEnum();
        switch(leaderPattern) {
            case LeaderPattern.SPACE:
                // use Space
            break;
            case LeaderPattern.RULE:
                // the following properties only apply
                // for leader-pattern = "rule"
                ruleThickness =
                         propertyList.get(PR_RULE_THICKNESS).getLength().getValue();
                ruleStyle = propertyList.get(PR_RULE_STYLE).getEnum();
            break;
            case LeaderPattern.DOTS:
            break;
            case LeaderPattern.USECONTENT:
                // use inline layout manager to create inline areas
                // add the inline parent multiple times until leader full
            break;
        }

        // if leaderPatternWidth = 0 = default = use-font-metric
        patternWidth =
            this.propertyList.get(PR_LEADER_PATTERN_WIDTH).getLength().getValue();

    }

    public int getLength(String prop, int dim) {
        int length;
        int propId = FOPropertyMapping.getPropertyId(prop);
        Length maxlength = propertyList.get(propId).getLength();
        if (maxlength instanceof PercentLength) {
            length = (int)(((PercentLength)maxlength).value()
                                      * dim);
        } else {
            length = maxlength.getValue();
        }
        return length;
    }

    public int getRuleStyle() {
        return ruleStyle;
    }

    public int getRuleThickness() {
        return ruleThickness;
    }

    public int getLeaderPattern() {
        return leaderPattern;
    }

    public Font getFontState() {
        return fontState;
    }

    public int getPatternWidth() {
        return patternWidth;
    }

    /**
     * This is a hook for an FOTreeVisitor subclass to be able to access
     * this object.
     * @param fotv the FOTreeVisitor subclass that can access this object.
     * @see org.apache.fop.fo.FOTreeVisitor
     */
    public void acceptVisitor(FOTreeVisitor fotv) {
        fotv.serveLeader(this);
    }

}
