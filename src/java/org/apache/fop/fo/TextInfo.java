/*
 * $Id: TextInfo.java,v 1.6 2003/03/05 21:48:02 jeremias Exp $
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
import org.apache.fop.fonts.Font;
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.traits.SpaceVal;

/**
 * Collection of properties used in
 */
public class TextInfo {
    /** object containing the font information */
    public Font fs;
    /** fo:color property */
    public ColorType color;
    /** fo:wrap-option property */
    public int wrapOption;
    /** fo:wrap-option property: true if wrapOption = WRAP */
    public boolean bWrap ;
    /** fo:white-space-collapse property*/
    public int whiteSpaceCollapse;
    /** fo:vertical-align property */
    public int verticalAlign;
    /** fo:line-height property */
    public int lineHeight;
    /** fo:text-transform property */
    public int textTransform = Constants.TextTransform.NONE;

    // Props used for calculating inline-progression-dimension
    /** fo:word-spacing property */
    public SpaceVal wordSpacing;
    /** fo:letter-spacing property */
    public SpaceVal letterSpacing;

    /** can this text be hyphenated? */
    public boolean bCanHyphenate = true;

    /** fo:text-decoration property: is text underlined? */
    public boolean underlined = false;
    /** fo:text-decoration property: is text overlined? */
    public boolean overlined = false;
    /** fo:text-decoration property: is text overstriked? */
    public boolean lineThrough = false;

}
