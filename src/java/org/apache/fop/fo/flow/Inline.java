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

// XML
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

// FOP
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.CharIterator;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObjMixed;
import org.apache.fop.fo.InlineCharIterator;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAural;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonFont;
import org.apache.fop.fo.properties.CommonMarginInline;
import org.apache.fop.fo.properties.CommonRelativePosition;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.LengthRangeProperty;

/**
 * Class modelling the fo:inline formatting object.
 */
public class Inline extends FObjMixed {
    // The value of properties relevant for fo:inline.
    private CommonAccessibility commonAccessibility;
    private CommonAural commonAural;
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private CommonFont commonFont;
    private CommonMarginInline commonMarginInline;
    private CommonRelativePosition commonRelativePosition;
    // private ToBeImplementedProperty alignmentAdjust;
    // private ToBeImplementedProperty alignmentBaseline;
    private Length baselineShift;
    private LengthRangeProperty blockProgressionDimension;
    private ColorType color;
    // private ToBeImplementedProperty dominantBaseline;
    private Length height;
    private String id;
    private LengthRangeProperty inlineProgressionDimension;
    private KeepProperty keepTogether;
    private KeepProperty keepWithNext;
    private KeepProperty keepWithPrevious;
    private Length lineHeight;
    private int textDecoration;
    // private ToBeImplementedProperty visibility;
    private Length width;
    private int wrapOption;
    // End of property values

    // used for FO validation
    private boolean blockOrInlineItemFound = false;
    private boolean canHaveBlockLevelChildren = true;

    /**
     * @param parent FONode that is the parent of this object
     */
    public Inline(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FObj#bind(PropertyList)
     */
    public void bind(PropertyList pList) throws SAXParseException {
        commonAccessibility = pList.getAccessibilityProps();
        commonAural = pList.getAuralProps();
        commonBorderPaddingBackground = pList.getBorderPaddingBackgroundProps();
        commonFont = pList.getFontProps();
        commonMarginInline = pList.getMarginInlineProps();
        commonRelativePosition = pList.getRelativePositionProps();
        // alignmentAdjust = pList.get(PR_ALIGNMENT_ADJUST);
        // alignmentBaseline = pList.get(PR_ALIGNMENT_BASELINE);
        baselineShift = pList.get(PR_BASELINE_SHIFT).getLength();
        blockProgressionDimension = pList.get(PR_BLOCK_PROGRESSION_DIMENSION).getLengthRange();
        color = pList.get(PR_COLOR).getColorType();
        // dominantBaseline = pList.get(PR_DOMINANT_BASELINE);
        height = pList.get(PR_HEIGHT).getLength();
        id = pList.get(PR_ID).getString();
        inlineProgressionDimension = pList.get(PR_INLINE_PROGRESSION_DIMENSION).getLengthRange();
        keepTogether = pList.get(PR_KEEP_TOGETHER).getKeep();
        keepWithNext = pList.get(PR_KEEP_WITH_NEXT).getKeep();
        keepWithPrevious = pList.get(PR_KEEP_WITH_PREVIOUS).getKeep();
        lineHeight = pList.get(PR_LINE_HEIGHT).getLength();
        textDecoration = pList.get(PR_TEXT_DECORATION).getEnum();
        // visibility = pList.get(PR_VISIBILITY);
        width = pList.get(PR_WIDTH).getLength();
        wrapOption = pList.get(PR_WRAP_OPTION).getEnum();
    }

    /**
     * @see org.apache.fop.fo.FONode#startOfNode
     */
    protected void startOfNode() throws SAXParseException {
       /* Check to see if this node can have block-level children.
        * See validateChildNode() below.
        */
       int lvlLeader = findAncestor(FO_LEADER);
       int lvlFootnote = findAncestor(FO_FOOTNOTE);
       int lvlInCntr = findAncestor(FO_INLINE_CONTAINER);

       if (lvlLeader > 0) {
           if (lvlInCntr < 0 ||
               (lvlInCntr > 0 && lvlInCntr > lvlLeader)) {
               canHaveBlockLevelChildren = false;
           }
       } else if (lvlFootnote > 0) {
           if (lvlInCntr < 0 || lvlInCntr > lvlFootnote) {
               canHaveBlockLevelChildren = false;
           }
       }

        checkId(id);
        getFOEventHandler().startInline(this);
    }

    /**
     * @see org.apache.fop.fo.FONode#endOfNode
     */
    protected void endOfNode() throws SAXParseException {
        getFOEventHandler().endInline(this);
    }

    /**
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
     * XSL Content Model: marker* (#PCDATA|%inline;|%block;)*
     * Additionally: " An fo:inline that is a descendant of an fo:leader
     *  or fo:footnote may not have block-level children, unless it has a
     *  nearer ancestor that is an fo:inline-container." (paraphrased)
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws SAXParseException {
        if (nsURI == FO_URI && localName.equals("marker")) {
            if (blockOrInlineItemFound) {
               nodesOutOfOrderError(loc, "fo:marker", 
                    "(#PCDATA|%inline;|%block;)");
            }
        } else if (!isBlockOrInlineItem(nsURI, localName)) {
            invalidChildError(loc, nsURI, localName);
        } else if (!canHaveBlockLevelChildren && isBlockItem(nsURI, localName)) {
            String ruleViolated = 
                " An fo:inline that is a descendant of an fo:leader" +
                " or fo:footnote may not have block-level children," +
                " unless it has a nearer ancestor that is an" +
                " fo:inline-container.";
            invalidChildError(loc, nsURI, localName, ruleViolated);
        } else {
            blockOrInlineItemFound = true;
        }
    }

    /**
     * Return the Common Margin Properties-Inline.
     */
    public CommonMarginInline getCommonMarginInline() {
        return commonMarginInline;
    }

    /**
     * Return the Common Border, Padding, and Background Properties.
     */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return commonBorderPaddingBackground;
    }

    /**
     * Return the Common Font Properties.
     */
    public CommonFont getCommonFont() {
        return commonFont;
    }

    /**
     * Return the "color" property.
     */
    public ColorType getColor() {
        return color;
    }

    /**
     * Return the "id" property.
     */
    public String getId() {
        return id;
    }

    /**
     * Return the "text-decoration" property.
     */
    public int getTextDecoration() {
        return textDecoration; 
    }

    /**
     * @see org.apache.fop.fo.FObjMixed#charIterator
     */
    public CharIterator charIterator() {
        return new InlineCharIterator(this, propMgr.getBorderAndPadding());
    }

    /**
     * @see org.apache.fop.fo.FObj#getName()
     */
    public String getName() {
        return "fo:inline";
    }
    
    /**
     * @see org.apache.fop.fo.FObj#getNameId()
     */
    public int getNameId() {
        return FO_INLINE;
    }
}
