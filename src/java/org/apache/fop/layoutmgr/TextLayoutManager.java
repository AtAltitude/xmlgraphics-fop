/*
 * $Id: TextLayoutManager.java,v 1.21 2003/03/05 20:38:26 jeremias Exp $
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
package org.apache.fop.layoutmgr;

import java.util.ArrayList;

import org.apache.fop.fo.TextInfo;
import org.apache.fop.traits.SpaceVal;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.Word;
import org.apache.fop.area.inline.Space;
import org.apache.fop.util.CharUtilities;

/**
 * LayoutManager for text (a sequence of characters) which generates one
 * or more inline areas.
 */
public class TextLayoutManager extends AbstractLayoutManager {

    /**
     * Store information about each potential word area.
     * Index of character which ends the area, IPD of area, including
     * any word-space and letter-space.
     * Number of word-spaces?
     */
    private class AreaInfo {
        private short iStartIndex;
        private short iBreakIndex;
        private short iWScount;
        private MinOptMax ipdArea;
        public AreaInfo(short iSIndex, short iBIndex, short iWS,
                 MinOptMax ipd) {
            iStartIndex = iSIndex;
            iBreakIndex = iBIndex;
            iWScount = iWS;
            ipdArea = ipd;
        }
    }


    // Hold all possible breaks for the text in this LM's FO.
    private ArrayList vecAreaInfo;

    /** Non-space characters on which we can end a line. */
    private static final String BREAK_CHARS = "-/" ;

    private char[] chars;
    private TextInfo textInfo;

    private static final char NEWLINE = '\n';
    private static final char SPACE = '\u0020'; // Normal space
    private static final char NBSPACE = '\u00A0'; // Non-breaking space
    private static final char LINEBREAK = '\u2028';
    private static final char ZERO_WIDTH_SPACE = '\u200B';
    // byte order mark
    private static final char ZERO_WIDTH_NOBREAK_SPACE = '\uFEFF';

    /** Start index of first character in this parent Area */
    private short iAreaStart = 0;
    /** Start index of next "word" */
    private short iNextStart = 0;
    /** Size since last makeArea call, except for last break */
    private MinOptMax ipdTotal;
    /** Size including last break possibility returned */
    // private MinOptMax nextIPD = new MinOptMax(0);
    /** size of a space character (U+0020) glyph in current font */
    private int spaceCharIPD;
    /** size of the hyphen character glyph in current font */
    private int hyphIPD;
    /** 1/2 of word-spacing value */
    private SpaceVal halfWS;
    /** Number of space characters after previous possible break position. */
    private int iNbSpacesPending;

    /**
     * Create a Text layout manager.
     *
     * @param chars the characters
     * @param textInfo the text information for doing layout
     */
    public TextLayoutManager(char[] chars, TextInfo textInfo) {
        this.chars = chars;
        this.textInfo = textInfo;
        this.vecAreaInfo = new java.util.ArrayList();

        // With CID fonts, space isn't neccesary currentFontState.width(32)
        spaceCharIPD = CharUtilities.getCharWidth(' ', textInfo.fs);
        // Use hyphenationChar property
        hyphIPD = CharUtilities.getCharWidth('-', textInfo.fs);
        // Make half-space: <space> on either side of a word-space)
        SpaceVal ws = textInfo.wordSpacing;
        halfWS = new SpaceVal(MinOptMax.multiply(ws.getSpace(), 0.5),
                ws.isConditional(), ws.isForcing(), ws.getPrecedence());
    }

    /**
     * Text always generates inline areas.
     *
     * @return true
     */
    public boolean generatesInlineAreas() {
        return true;
    }

    /**
     * Get the word characters between two positions.
     * This is used when doing hyphenation or other word manipulations.
     *
     * @param sbChars the string buffer to put the chars into
     * @param bp1 the start position
     * @param bp2 the end position
     */
    public void getWordChars(StringBuffer sbChars, Position bp1,
                             Position bp2) {
        LeafPosition endPos = (LeafPosition) bp2;
        AreaInfo ai =
          (AreaInfo) vecAreaInfo.get(endPos.getLeafPos());
        // Skip all leading spaces for hyphenation
        int i;
        for (i = ai.iStartIndex; 
                i < ai.iBreakIndex && CharUtilities.isAnySpace(chars[i]) == true;
                i++) {
            //nop
        }
        sbChars.append(new String(chars, i, ai.iBreakIndex - i));
    }

    /**
     * Return value indicating whether the next area to be generated could
     * start a new line. This should only be called in the "START" condition
     * if a previous inline BP couldn't end the line.
     * Return true if the first character is a potential linebreak character.
     *
     * @param context the layout context for determining a break
     * @return true if can break before this text
     */
    public boolean canBreakBefore(LayoutContext context) {
        char c = chars[iNextStart];
        return ((c == NEWLINE) 
                || (textInfo.bWrap && (CharUtilities.isSpace(c) 
                || BREAK_CHARS.indexOf(c) >= 0)));
    }

    /**
     * Reset position for returning next BreakPossibility.
     *
     * @param prevPos the position to reset to
     */
    public void resetPosition(Position prevPos) {
        if (prevPos != null) {
            // ASSERT (prevPos.getLM() == this)
            if (prevPos.getLM() != this) {
                getLogger().error(
                  "TextLayoutManager.resetPosition: " + "LM mismatch!!!");
            }
            LeafPosition tbp = (LeafPosition) prevPos;
            AreaInfo ai =
              (AreaInfo) vecAreaInfo.get(tbp.getLeafPos());
            if (ai.iBreakIndex != iNextStart) {
                iNextStart = ai.iBreakIndex;
                vecAreaInfo.ensureCapacity(tbp.getLeafPos() + 1);
                // TODO: reset or recalculate total IPD = sum of all word IPD
                // up to the break position
                ipdTotal = ai.ipdArea;
                setFinished(false);
            }
        } else {
            // Reset to beginning!
            vecAreaInfo.clear();
            iNextStart = 0;
            setFinished(false);
        }
    }

    // TODO: see if we can use normal getNextBreakPoss for this with
    // extra hyphenation information in LayoutContext
    private boolean getHyphenIPD(HyphContext hc, MinOptMax hyphIPD) {
        // Skip leading word-space before calculating count?
        boolean bCanHyphenate = true;
        int iStopIndex = iNextStart + hc.getNextHyphPoint();

        if (chars.length < iStopIndex || textInfo.bCanHyphenate == false) {
            iStopIndex = chars.length;
            bCanHyphenate = false;
        }
        hc.updateOffset(iStopIndex - iNextStart);

        for (; iNextStart < iStopIndex; iNextStart++) {
            char c = chars[iNextStart];
            hyphIPD.opt += CharUtilities.getCharWidth(c, textInfo.fs);
            // letter-space?
        }
        // Need to include hyphen size too, but don't count it in the
        // stored running total, since it would be double counted
        // with later hyphenation points
        return bCanHyphenate;
    }

    /**
     * Return the next break possibility that fits the constraints.
     * @param context An object specifying the flags and input information
     * concerning the context of the BreakPoss.
     * @return BreakPoss An object containing information about the next
     * legal break position or the end of the text run if no break
     * was found.
     * <p>Assumptions: white-space-treatment and
     * linefeed-treatment processing
     * are already done, so there are no TAB or RETURN characters remaining.
     * white-space-collapse handling is also done
     * (but perhaps this shouldn't be true!)
     * There may be LINEFEED characters if they weren't converted
     * into spaces. A LINEFEED always forces a break.
     */
    public BreakPoss getNextBreakPoss(LayoutContext context) {
        /* On first call in a new Line, the START_AREA
         * flag in LC is set.
         */

        int iFlags = 0;

        if (context.startsNewArea()) {
            /* This could be first call on this LM, or the first call
             * in a new (possible) LineArea.
             */
            ipdTotal = new MinOptMax(0);
            iFlags |= BreakPoss.ISFIRST;
        }


        /* HANDLE SUPPRESSED LEADING SPACES
         * See W3C XSL Rec. 7.16.3.
         * Suppress characters whose "suppress-at-line-break" property = "suppress"
         * This can only be set on an explicit fo:character object. The default
         * behavior is that U+0020 is suppressed; all other character codes are
         * retained.
         */
        if (context.suppressLeadingSpace()) {
            for (; iNextStart < chars.length
                    && chars[iNextStart] == SPACE; iNextStart++) {
            }
            // If now at end, nothing to compose here!
            if (iNextStart >= chars.length) {
                setFinished(true);
                return null; // Or an "empty" BreakPoss?
            }
        }


        /* Start of this "word", plus any non-suppressed leading space.
         * Collapse any remaining word-space with leading space from
         * ancestor FOs.
         * Add up other leading space which is counted in the word IPD.
         */

        SpaceSpecifier pendingSpace = new SpaceSpecifier(false);
        short iThisStart = iNextStart; // Index of first character counted
        MinOptMax spaceIPD = new MinOptMax(0); // Extra IPD from word-spacing
        // Sum of glyph IPD of all characters in a word, inc. leading space
        int wordIPD = 0;
        short iWScount = 0; // Count of word spaces
        boolean bSawNonSuppressible = false;

        for (; iNextStart < chars.length; iNextStart++) {
            char c = chars[iNextStart];
            if (CharUtilities.isAnySpace(c) == false) {
                break;
            }
            if (c == SPACE || c == NBSPACE) {
                ++iWScount;
                // Counted as word-space
                if (iNextStart == iThisStart
                        && (iFlags & BreakPoss.ISFIRST) != 0) {
                    // If possible, treat as normal inter-word space
                    if (context.getLeadingSpace().hasSpaces()) {
                        context.getLeadingSpace().addSpace(halfWS);
                    } else {
                        // Doesn't combine with any other leading spaces
                        // from ancestors
                        spaceIPD.add(halfWS.getSpace());
                    }
                } else {
                    pendingSpace.addSpace(halfWS);
                    spaceIPD.add(pendingSpace.resolve(false));
                }
                wordIPD += spaceCharIPD; // Space glyph IPD
                pendingSpace.clear();
                pendingSpace.addSpace(halfWS);
                if (c == NBSPACE) {
                    bSawNonSuppressible = true;
                }
            } else {
                // If we have letter-space, so we apply this to fixed-
                // width spaces (which are not word-space) also?
                bSawNonSuppressible = true;
                spaceIPD.add(pendingSpace.resolve(false));
                pendingSpace.clear();
                wordIPD += CharUtilities.getCharWidth(c, textInfo.fs);
            }
        }

        if (iNextStart < chars.length) {
            spaceIPD.add(pendingSpace.resolve(false));
        } else {
            // This FO ended with spaces. Return the BP
            if (!bSawNonSuppressible) {
                iFlags |= BreakPoss.ALL_ARE_SUPPRESS_AT_LB;
            }
            return makeBreakPoss(iThisStart, spaceIPD, wordIPD,
                                 context.getLeadingSpace(), pendingSpace, iFlags,
                                 iWScount);
        }

        if (context.tryHyphenate()) {
            // Get the size of the next syallable
            MinOptMax hyphIPD = new MinOptMax(0);
            if (getHyphenIPD(context.getHyphContext(), hyphIPD)) {
                iFlags |= (BreakPoss.CAN_BREAK_AFTER | BreakPoss.HYPHENATED);
            }
            wordIPD += hyphIPD.opt;
        } else {
            // Look for a legal line-break: breakable white-space and certain
            // characters such as '-' which can serve as word breaks.
            // Don't look for hyphenation points here though
            for (; iNextStart < chars.length; iNextStart++) {
                char c = chars[iNextStart];
                if ((c == NEWLINE) || // Include any breakable white-space as break char
                        //  even if fixed width
                        (textInfo.bWrap && (CharUtilities.isSpace(c) 
                                            || BREAK_CHARS.indexOf(c) >= 0))) {
                    iFlags |= BreakPoss.CAN_BREAK_AFTER;
                    if (c != SPACE) {
                        iNextStart++;
                        if (c != NEWLINE) {
                            wordIPD += CharUtilities.getCharWidth(c,
                                                                  textInfo.fs);
                        } else {
                            iFlags |= BreakPoss.FORCE;
                        }
                    }
                    // If all remaining characters would be suppressed at
                    // line-end, set a flag for parent LM.
                    int iLastChar;
                    for (iLastChar = iNextStart;
                            iLastChar < chars.length 
                            && chars[iLastChar] == SPACE; iLastChar++) {
                        //nop
                    }
                    if (iLastChar == chars.length) {
                        iFlags |= BreakPoss.REST_ARE_SUPPRESS_AT_LB;
                    }
                    return makeBreakPoss(iThisStart, spaceIPD, wordIPD,
                                         context.getLeadingSpace(), null, iFlags,
                                         iWScount);
                }
                wordIPD += CharUtilities.getCharWidth(c, textInfo.fs);
                // Note, if a normal non-breaking space, is it stretchable???
                // If so, keep a count of these embedded spaces.
            }
        }
        return makeBreakPoss(iThisStart, spaceIPD, wordIPD,
                             context.getLeadingSpace(), null, iFlags, iWScount);
    }

    private BreakPoss makeBreakPoss(short iWordStart,
                                    MinOptMax spaceIPD, int wordDim,
                                    SpaceSpecifier leadingSpace, SpaceSpecifier trailingSpace,
                                    int flags, short iWScount) {
        MinOptMax ipd = new MinOptMax(wordDim);
        ipd.add(spaceIPD);
        if (ipdTotal != null) {
            ipd.add(ipdTotal); // sum of all words so far in line
        }
        // Note: break position now stores total size to here

        // Position is the index of the info for this word in the vector
        vecAreaInfo.add(
          new AreaInfo(iWordStart, iNextStart, iWScount, ipd));
        BreakPoss bp = new BreakPoss(
                         new LeafPosition(this, vecAreaInfo.size() - 1));
        ipdTotal = ipd;
        if ((flags & BreakPoss.HYPHENATED) != 0) {
            // Add the hyphen size, but don't change total IPD!
            bp.setStackingSize(
              MinOptMax.add(ipd, new MinOptMax(hyphIPD)));
        } else {
            bp.setStackingSize(ipd);
        }
        // TODO: make this correct (see Keiron's vertical alignment code)
        bp.setNonStackingSize(new MinOptMax(textInfo.lineHeight));

        /* Set max ascender and descender (offset from baseline),
         * used for calculating the bpd of the line area containing
         * this text.
         */
        //bp.setDescender(textInfo.fs.getDescender());
        //bp.setAscender(textInfo.fs.getAscender());
        if (iNextStart == chars.length) {
            flags |= BreakPoss.ISLAST;
            setFinished(true);
        }
        bp.setFlag(flags);
        if (trailingSpace != null) {
            bp.setTrailingSpace(trailingSpace);
        } else {
            bp.setTrailingSpace(new SpaceSpecifier(false));
        }
        if (leadingSpace != null) {
            bp.setLeadingSpace(leadingSpace);
        } else {
            bp.setLeadingSpace(new SpaceSpecifier(false));
        }
        return bp;
    }


    /**
     * Generate and add areas to parent area.
     * This can either generate an area for each "word" and each space, or
     * an area containing all text with a parameter controlling the size of
     * the word space. The latter is most efficient for PDF generation.
     * Set size of each area.
     * @param posIter Iterator over Position information returned
     * by this LayoutManager.
     * @param context LayoutContext for adjustments
     */
    public void addAreas(PositionIterator posIter, LayoutContext context) {
        // Add word areas
        AreaInfo ai = null ;
        int iStart = -1;
        int iWScount = 0;

        /* On first area created, add any leading space.
         * Calculate word-space stretch value.
         */
        while (posIter.hasNext()) {
            LeafPosition tbpNext = (LeafPosition) posIter.next();
            ai = (AreaInfo) vecAreaInfo.get(tbpNext.getLeafPos());
            if (iStart == -1) {
                iStart = ai.iStartIndex;
            }
            iWScount += ai.iWScount;
        }
        if (ai == null) {
            return;
        }
        // Calculate total adjustment
        int iAdjust = 0;
        double dSpaceAdjust = context.getSpaceAdjust();
        if (dSpaceAdjust > 0.0) {
            // Stretch by factor
            //     System.err.println("Potential stretch = " +
            //        (ai.ipdArea.max - ai.ipdArea.opt));
            iAdjust = (int)((double)(ai.ipdArea.max
                                     - ai.ipdArea.opt) * dSpaceAdjust);
        } else if (dSpaceAdjust < 0.0) {
            // Shrink by factor
            //     System.err.println("Potential shrink = " +
            //        (ai.ipdArea.opt - ai.ipdArea.min));
            iAdjust = (int)((double)(ai.ipdArea.opt
                                     - ai.ipdArea.min) * dSpaceAdjust);
        }
        // System.err.println("Text adjustment factor = " + dSpaceAdjust +
        //    " total=" + iAdjust);

        // Make an area containing all characters between start and end.
        InlineArea word = null;
        int adjust = 0;
        // ingnore newline character
        if (chars[ai.iBreakIndex - 1] == NEWLINE) {
            adjust = 1;
        }
        String str = new String(chars, iStart, ai.iBreakIndex - iStart - adjust);
        if (" ".equals(str)) {
            word = new Space();
            word.setWidth(ai.ipdArea.opt + iAdjust);
        } else  {
            Word w = createWord(
                      str,
                      ai.ipdArea.opt + iAdjust, context.getBaseline());
            if (iWScount > 0) {
                //getLogger().error("Adjustment per word-space= " +
                //                   iAdjust / iWScount);
                w.setWSadjust(iAdjust / iWScount);
            }
            word = w;
        }
        if ((chars[iStart] == SPACE || chars[iStart] == NBSPACE) 
                && context.getLeadingSpace().hasSpaces()) {
            context.getLeadingSpace().addSpace(halfWS);
        }
        // Set LAST flag if done making characters
        int iLastChar;
        for (iLastChar = ai.iBreakIndex;
                iLastChar < chars.length && chars[iLastChar] == SPACE;
                iLastChar++) {
            //nop
        }
        context.setFlags(LayoutContext.LAST_AREA,
                         iLastChar == chars.length);

        // Can we have any trailing space? Yes, if last char was a space!
        context.setTrailingSpace(new SpaceSpecifier(false));
        if (chars[ai.iBreakIndex - 1] == SPACE 
                || chars[ai.iBreakIndex - 1] == NBSPACE) {
            context.getTrailingSpace().addSpace(halfWS);
        }
        if (word != null) {
            parentLM.addChild(word);
        }
    }

    /**
     * Create an inline word area.
     * This creates a Word and sets up the various attributes.
     *
     * @param str the string for the word
     * @param width the width that the word uses
     * @param base the baseline position
     * @return the new word area
     */
    protected Word createWord(String str, int width, int base) {
        Word curWordArea = new Word();
        curWordArea.setWidth(width);
        curWordArea.setHeight(textInfo.fs.getAscender()
                              - textInfo.fs.getDescender());
        curWordArea.setOffset(textInfo.fs.getAscender());
        curWordArea.setOffset(base);

        curWordArea.setWord(str);
        curWordArea.addTrait(Trait.FONT_NAME, textInfo.fs.getFontName());
        curWordArea.addTrait(Trait.FONT_SIZE,
                             new Integer(textInfo.fs.getFontSize()));
        curWordArea.addTrait(Trait.COLOR, this.textInfo.color);
        return curWordArea;
    }

}

