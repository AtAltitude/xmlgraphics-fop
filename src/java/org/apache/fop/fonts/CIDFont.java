/*
 * $Id: CIDFont.java,v 1.3 2003/03/11 04:06:43 vmote Exp $
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

/**
 * Abstract base class for CID fonts.
 */
public abstract class CIDFont extends CustomFont {

    /**
     * usedGlyphs contains orginal, new glyph index
     */
    public Map usedGlyphs = new java.util.HashMap();

    /**
     * usedGlyphsIndex contains new glyph, original index
     */
    public Map usedGlyphsIndex = new java.util.HashMap();
    public int usedGlyphsCount = 0;

    //private PDFWArray warray = new PDFWArray();
    public int width[] = null;

    // ---- Required ----
    /**
     * Returns the name of the base font.
     * @return the name of the base font
     */
    public abstract String getCidBaseFont();

    /**
     * Returns the type of the CID font.
     * @return the type of the CID font
     */
    public abstract CIDFontType getCIDType();

    /**
     * Returns the name of the issuer of the font.
     * @return a String identifying an issuer of character collections -
     * for example, Adobe
     */
    public abstract String getRegistry();

    /**
     * Returns a font name for use within a registry.
     * @return a String that uniquely names a character collection issued by
     * a specific registry - for example, Japan1.
     */
    public abstract String getOrdering();

    /**
     * Returns the supplement number of the character collection.
     * @return the supplement number
     */
    public abstract int getSupplement();


    // ---- Optional ----
    /**
     * Returns the default width for this font.
     * @return the default width
     */
    public int getDefaultWidth() {
        return 0;
    }

    /**
     * @see org.apache.fop.fonts.Typeface#isMultiByte()
     */
    public boolean isMultiByte() {
        return true;
    }

}