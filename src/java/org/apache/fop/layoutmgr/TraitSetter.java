/*
 * Copyright 1999-2005 The Apache Software Foundation.
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

/* $Id: TraitSetter.java,v 1.6 2004/02/27 17:49:25 jeremias Exp $ */

package org.apache.fop.layoutmgr;

import org.apache.fop.traits.BorderProps;
import org.apache.fop.area.Area;
import org.apache.fop.area.Trait;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;

/**
 * This is a helper class used for setting common traits on areas.
 */
public class TraitSetter {

    /**
     * Sets border and padding traits on areas.
     * @param area area to set the traits on
     * @param bpProps border and padding properties
     * @param bNotFirst True if the area is not the first area
     * @param bNotLast True if the area is not the last area
     */
    public static void setBorderPaddingTraits(Area area,
            CommonBorderPaddingBackground bpProps, boolean bNotFirst, boolean bNotLast) {
        int iBP;
        iBP = bpProps.getPadding(CommonBorderPaddingBackground.START, bNotFirst);
        if (iBP > 0) {
            //area.addTrait(new Trait(Trait.PADDING_START, new Integer(iBP)));
            area.addTrait(Trait.PADDING_START, new Integer(iBP));
        }
        iBP = bpProps.getPadding(CommonBorderPaddingBackground.END, bNotLast);
        if (iBP > 0) {
            //area.addTrait(new Trait(Trait.PADDING_END, new Integer(iBP)));
            area.addTrait(Trait.PADDING_END, new Integer(iBP));
        }
        iBP = bpProps.getPadding(CommonBorderPaddingBackground.BEFORE, false);
        if (iBP > 0) {
            // area.addTrait(new Trait(Trait.PADDING_BEFORE, new Integer(iBP)));
            area.addTrait(Trait.PADDING_BEFORE, new Integer(iBP));
        }
        iBP = bpProps.getPadding(CommonBorderPaddingBackground.AFTER, false);
        if (iBP > 0) {
            //area.addTrait(new Trait(Trait.PADDING_AFTER, new Integer(iBP)));
            area.addTrait(Trait.PADDING_AFTER, new Integer(iBP));
        }

        addBorderTrait(area, bpProps, bNotFirst,
                       CommonBorderPaddingBackground.START, Trait.BORDER_START);

        addBorderTrait(area, bpProps, bNotLast, CommonBorderPaddingBackground.END,
                       Trait.BORDER_END);

        addBorderTrait(area, bpProps, false, CommonBorderPaddingBackground.BEFORE,
                       Trait.BORDER_BEFORE);

        addBorderTrait(area, bpProps, false, CommonBorderPaddingBackground.AFTER,
                       Trait.BORDER_AFTER);
    }

    /**
     * Sets border traits on an area.
     * @param area area to set the traits on
     * @param bpProps border and padding properties
     */
    private static void addBorderTrait(Area area,
                                       CommonBorderPaddingBackground bpProps,
                                       boolean bDiscard, int iSide,
                                       Object oTrait) {
        int iBP = bpProps.getBorderWidth(iSide, bDiscard);
        if (iBP > 0) {
            //     area.addTrait(new Trait(oTrait,
            //     new BorderProps(bpProps.getBorderStyle(iSide),
            //     iBP,
            //     bpProps.getBorderColor(iSide))));
            area.addTrait(oTrait,
                          new BorderProps(bpProps.getBorderStyle(iSide),
                                          iBP, bpProps.getBorderColor(iSide)));
        }
    }

    /**
     * Add borders to an area.
     * Layout managers that create areas with borders can use this to
     * add the borders to the area.
     * @param area the area to set the traits on.
     * @param bordProps border properties
     */
    public static void addBorders(Area area, CommonBorderPaddingBackground bordProps) {
        BorderProps bps = getBorderProps(bordProps, CommonBorderPaddingBackground.BEFORE);
        if (bps.width != 0) {
            area.addTrait(Trait.BORDER_BEFORE, bps);
        }
        bps = getBorderProps(bordProps, CommonBorderPaddingBackground.AFTER);
        if (bps.width != 0) {
            area.addTrait(Trait.BORDER_AFTER, bps);
        }
        bps = getBorderProps(bordProps, CommonBorderPaddingBackground.START);
        if (bps.width != 0) {
            area.addTrait(Trait.BORDER_START, bps);
        }
        bps = getBorderProps(bordProps, CommonBorderPaddingBackground.END);
        if (bps.width != 0) {
            area.addTrait(Trait.BORDER_END, bps);
        }

        int padding = bordProps.getPadding(CommonBorderPaddingBackground.START, false);
        if (padding != 0) {
            area.addTrait(Trait.PADDING_START, new java.lang.Integer(padding));
        }

        padding = bordProps.getPadding(CommonBorderPaddingBackground.END, false);
        if (padding != 0) {
            area.addTrait(Trait.PADDING_END, new java.lang.Integer(padding));
        }

        padding = bordProps.getPadding(CommonBorderPaddingBackground.BEFORE, false);
        if (padding != 0) {
            area.addTrait(Trait.PADDING_BEFORE, new java.lang.Integer(padding));
        }

        padding = bordProps.getPadding(CommonBorderPaddingBackground.AFTER, false);
        if (padding != 0) {
            area.addTrait(Trait.PADDING_AFTER, new java.lang.Integer(padding));
        }
    }

    private static BorderProps getBorderProps(CommonBorderPaddingBackground bordProps, int side) {
        BorderProps bps;
        bps = new BorderProps(bordProps.getBorderStyle(side),
                              bordProps.getBorderWidth(side, false),
                              bordProps.getBorderColor(side));
        return bps;
    }

    /**
     * Add background to an area.
     * Layout managers that create areas with a background can use this to
     * add the background to the area.
     * @param area the area to set the traits on
     * @param backProps the background properties
     */
    public static void addBackground(Area area, CommonBorderPaddingBackground backProps) {
        Trait.Background back = new Trait.Background();
        back.setColor(backProps.backgroundColor);

        if (backProps.backgroundImage != null) {
            back.setURL(backProps.backgroundImage);
            back.setRepeat(backProps.backgroundRepeat);
            if (backProps.backgroundPositionHorizontal != null) {
                back.setHoriz(backProps.backgroundPositionHorizontal.getValue());
            }
            if (backProps.backgroundPositionVertical != null) {
                back.setVertical(backProps.backgroundPositionVertical.getValue());
            }
        }

        if (back.getColor() != null || back.getURL() != null) {
            area.addTrait(Trait.BACKGROUND, back);
        }
    }

    /**
     * Add space to a block area.
     * Layout managers that create block areas can use this to add space
     * outside of the border rectangle to the area.
     * @param area the area to set the traits on.
     * @param bpProps the border, padding and background properties
     * @param marginProps the margin properties.
     */
    public static void addMargins(Area area,
                                  CommonBorderPaddingBackground bpProps,
                                  CommonMarginBlock marginProps) {
        int spaceStart = marginProps.startIndent.getValue()
                            - marginProps.inheritedStartIndent.getValue()
                            - bpProps.getBorderStartWidth(false)
                            - bpProps.getPaddingStart(false);
        if (spaceStart != 0) {
            area.addTrait(Trait.SPACE_START, new Integer(spaceStart));
        }

        int spaceEnd = marginProps.endIndent.getValue()
                            - marginProps.inheritedEndIndent.getValue()
                            - bpProps.getBorderEndWidth(false)
                            - bpProps.getPaddingEnd(false);
        if (spaceEnd != 0) {
            area.addTrait(Trait.SPACE_END, new Integer(spaceEnd));
        }
    }

    /**
     * Sets the traits for breaks on an area.
     * @param area the area to set the traits on.
     * @param breakBefore the value for break-before
     * @param breakAfter the value for break-after
     */
    public static void addBreaks(Area area,  int breakBefore, int breakAfter) {
        area.addTrait(Trait.BREAK_AFTER, new Integer(breakAfter));
        area.addTrait(Trait.BREAK_BEFORE, new Integer(breakBefore));
    }
}
