/*
 * $Id: MultiByteFont.java,v 1.2 2003/03/06 17:43:05 jeremias Exp $
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
package org.apache.fop.fonts;

//Java
import java.util.Map;

//FOP
import org.apache.fop.pdf.PDFWArray;

/**
 * Generic MultiByte (CID) font
 */
public class MultiByteFont extends CIDFont {

    private static int uniqueCounter = 1;


    private String ttcName = null;
    private String encoding = "Identity-H";

    private String embedResourceName = null;

    private int defaultWidth = 0;
    private CIDFontType cidType = CIDFontType.CIDTYPE2;

    private String namePrefix = null;    // Quasi unique prefix
    //private PDFWArray warray = new PDFWArray();
    private int width[] = null;

    private BFEntry[] bfentries = null;

    /**
     * usedGlyphs contains orginal, new glyph index
     */
    private Map usedGlyphs = new java.util.HashMap();

    /**
     * usedGlyphsIndex contains new glyph, original index
     */
    private Map usedGlyphsIndex = new java.util.HashMap();
    private int usedGlyphsCount = 0;


    /**
     * Default constructor
     */
    public MultiByteFont() {
        // Make sure that the 3 first glyphs are included
        usedGlyphs.put(new Integer(0), new Integer(0));
        usedGlyphsIndex.put(new Integer(0), new Integer(0));
        usedGlyphsCount++;
        usedGlyphs.put(new Integer(1), new Integer(1));
        usedGlyphsIndex.put(new Integer(1), new Integer(1));
        usedGlyphsCount++;
        usedGlyphs.put(new Integer(2), new Integer(2));
        usedGlyphsIndex.put(new Integer(2), new Integer(2));
        usedGlyphsCount++;

        // Create a quasiunique prefix for fontname
        int cnt = 0;
        synchronized (this.getClass()) {
            cnt = uniqueCounter++;
        }
        int ctm = (int)(System.currentTimeMillis() & 0xffff);
        namePrefix = new String(cnt + "E" + Integer.toHexString(ctm));

        setFontType(FontType.TYPE0);
    }

    /**
     * @see org.apache.fop.fonts.CIDFont#getDefaultWidth()
     */
    public int getDefaultWidth() {
        return defaultWidth;
    }

    /**
     * @see org.apache.fop.fonts.CIDFont#getRegistry()
     */
    public String getRegistry() {
        return "Adobe";
    }

    /**
     * @see org.apache.fop.fonts.CIDFont#getOrdering()
     */
    public String getOrdering() {
        return "UCS";
    }

    /**
     * @see org.apache.fop.fonts.CIDFont#getSupplement()
     */
    public int getSupplement() {
        return 0;
    }

    /**
     * @see org.apache.fop.fonts.CIDFont#getCIDType()
     */
    public CIDFontType getCIDType() {
        return cidType;
    }

    /**
     * Sets the CIDType.
     * @param cidType The cidType to set
     */
    public void setCIDType(CIDFontType cidType) {
        this.cidType = cidType;
    }

    /**
     * @see org.apache.fop.fonts.CIDFont#getCidBaseFont()
     */
    public String getCidBaseFont() {
        if (isEmbeddable()) {
            return namePrefix + super.getFontName();
        } else {
            return super.getFontName();
        }
    }

    /**
     * @see org.apache.fop.fonts.FontDescriptor#isEmbeddable()
     */
    public boolean isEmbeddable() {
        if (getEmbedFileName() == null
            && embedResourceName == null) {
            return false;
        } else {
            return true;
            }
    }

    /**
     * @see org.apache.fop.fonts.Typeface#getEncoding()
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @see org.apache.fop.fonts.FontMetrics#getFontName()
     */
    public String getFontName() {
        if (isEmbeddable()) {
            return namePrefix + super.getFontName();
        } else {
            return super.getFontName();
        }
    }

    /**
     * @see org.apache.fop.fonts.FontMetrics#getWidth(int, int)
     */
    public int getWidth(int i, int size) {
        if (isEmbeddable()) {
            Integer idx = (Integer)usedGlyphsIndex.get(new Integer(i));
            return size * width[idx.intValue()];
        } else {
            return size * width[i];
        }
    }

    /**
     * @see org.apache.fop.fonts.FontMetrics#getWidths()
     */
    public int[] getWidths() {
        int[] arr = new int[width.length];
        System.arraycopy(width, 0, arr, 0, width.length - 1);
        /*
        for (int i = 0; i < arr.length; i++)
            arr[i] *= size;
        */
        return arr;
    }

    /**
     * @see org.apache.fop.fonts.CIDFont#getSubsetWidths()
     */
    public PDFWArray getSubsetWidths() {
        // Create widths for reencoded chars
        PDFWArray warray = new PDFWArray();
        int[] tmpWidth = new int[usedGlyphsCount];

        for (int i = 0; i < usedGlyphsCount; i++) {
            Integer nw = (Integer)usedGlyphsIndex.get(new Integer(i));
            int nwx = (nw == null) ? 0 : nw.intValue();
            tmpWidth[i] = width[nwx];
        }
        warray.addEntry(0, tmpWidth);
        return warray;
    }

    /**
     * Remaps a codepoint based.
     * @param i codepoint to remap
     * @return new codepoint
     */
/* unused
    public Integer reMap(Integer i) {
        if (isEmbeddable()) {
            Integer ret = (Integer)usedGlyphsIndex.get(i);
            if (ret == null) {
                ret = i;
            }
            return ret;
        } else {
            return i;
        }

    }
*/

    /**
     * @see org.apache.fop.fonts.Font#mapChar(char)
     */
    public char mapChar(char c) {
        int idx = (int)c;
        int retIdx = 0;

        for (int i = 0; (i < bfentries.length) && retIdx == 0; i++) {
            if (bfentries[i].getUnicodeStart() <= idx
                    && bfentries[i].getUnicodeEnd() >= idx) {
                retIdx = bfentries[i].getGlyphStartIndex() + idx
                         - bfentries[i].getUnicodeStart();
            }
        }

        if (isEmbeddable()) {
            // Reencode to a new subset font or get
            // the reencoded value
            Integer newIdx = (Integer)usedGlyphs.get(new Integer(retIdx));
            if (newIdx == null) {
                usedGlyphs.put(new Integer(retIdx),
                               new Integer(usedGlyphsCount));
                usedGlyphsIndex.put(new Integer(usedGlyphsCount),
                                    new Integer(retIdx));
                retIdx = usedGlyphsCount;
                // System.out.println(c+"("+(int)c+") = "+retIdx);
                usedGlyphsCount++;
            } else {
                retIdx = newIdx.intValue();
            }
        }

        return (char)retIdx;
    }

    /**
     * Sets the bfentries.
     * @param bfentries The bfentries to set
     */
    public void setBFEntries(BFEntry[] bfentries) {
        this.bfentries = bfentries;
    }

    /**
     * Sets the defaultWidth.
     * @param defaultWidth The defaultWidth to set
     */
    public void setDefaultWidth(int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    /**
     * Returns the TrueType Collection Name.
     * @return the TrueType Collection Name
     */
    public String getTTCName() {
        return ttcName;
    }

    /**
     * Sets the the TrueType Collection Name.
     * @param ttcName the TrueType Collection Name
     */
    public void setTTCName(String ttcName) {
        this.ttcName = ttcName;
    }

    /**
     * Adds a new CID width entry to the font.
     * @param cidWidthIndex index
     * @param wds array of widths
     */
    /*
    public void addCIDWidthEntry(int cidWidthIndex, int[] wds) {
        this.warray.addEntry(cidWidthIndex, wds);
    }*/


    /**
     * Sets the width array.
     * @param wds array of widths.
     */
    public void setWidthArray(int[] wds) {
        this.width = wds;
    }

    /**
     * Returns a Map of used Glyphs.
     * @return Map Map of used Glyphs
     */
    public Map getUsedGlyphs() {
        return usedGlyphs;
    }

}

