/*
 * $Id: Character.java,v 1.22 2003/03/06 11:36:30 jeremias Exp $
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
import java.util.List;

// FOP
import org.apache.fop.fo.CharIterator;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.OneCharIterator;
import org.apache.fop.fo.FOTreeVisitor;
import org.apache.fop.fo.properties.CommonAural;
import org.apache.fop.fo.properties.CommonBorderAndPadding;
import org.apache.fop.fo.properties.CommonBackground;
import org.apache.fop.fo.properties.CommonHyphenation;
import org.apache.fop.fo.properties.CommonMarginInline;
import org.apache.fop.fo.properties.CommonRelativePosition;
import org.apache.fop.apps.FOPException;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.layoutmgr.LeafNodeLayoutManager;

/**
 * This class represents the flow object 'fo:character'. Its use is defined by
 * the spec: "The fo:character flow object represents a character that is mapped to
 * a glyph for presentation. It is an atomic unit to the formatter.
 * When the result tree is interpreted as a tree of formatting objects,
 * a character in the result tree is treated as if it were an empty
 * element of type fo:character with a character attribute
 * equal to the Unicode representation of the character.
 * The semantics of an "auto" value for character properties, which is
 * typically their initial value,  are based on the Unicode codepoint.
 * Overrides may be specified in an implementation-specific manner." (6.6.3)
 *
 */
public class Character extends FObj {

    /** constant indicating that the character is OK */
    public static final int OK = 0;
    /** constant indicating that the character does not fit */
    public static final int DOESNOT_FIT = 1;

    private char characterValue;

    /**
     * @param parent FONode that is the parent of this object
     */
    public Character(FONode parent) {
        super(parent);
    }

    public InlineArea getInlineArea() {
        String str = this.properties.get("character").getString();
        if (str.length() == 1) {
            org.apache.fop.area.inline.Character ch =
              new org.apache.fop.area.inline.Character(
                str.charAt(0));
            return ch;
        }
        return null;
    }

    private void setup() throws FOPException {

        // Common Aural Properties
        CommonAural mAurProps = propMgr.getAuralProps();

        // Common Border, Padding, and Background Properties
        CommonBorderAndPadding bap = propMgr.getBorderAndPadding();
        CommonBackground bProps = propMgr.getBackgroundProps();

        // Common Font Properties
        //this.fontState = propMgr.getFontState(area.getFontInfo());

        // Common Hyphenation Properties
        CommonHyphenation mHyphProps = propMgr.getHyphenationProps();

        // Common Margin Properties-Inline
        CommonMarginInline mProps = propMgr.getMarginInlineProps();

        // Common Relative Position Properties
        CommonRelativePosition mRelProps =
          propMgr.getRelativePositionProps();

        // this.properties.get("alignment-adjust");
        // this.properties.get("treat-as-word-space");
        // this.properties.get("alignment-baseline");
        // this.properties.get("baseline-shift");
        // this.properties.get("character");
        // this.properties.get("color");
        // this.properties.get("dominant-baseline");
        // this.properties.get("text-depth");
        // this.properties.get("text-altitude");
        // this.properties.get("glyph-orientation-horizontal");
        // this.properties.get("glyph-orientation-vertical");
        setupID();
        // this.properties.get("keep-with-next");
        // this.properties.get("keep-with-previous");
        // this.properties.get("letter-spacing");
        // this.properties.get("line-height");
        // this.properties.get("line-height-shift-adjustment");
        // this.properties.get("score-spaces");
        // this.properties.get("suppress-at-line-break");
        // this.properties.get("text-decoration");
        // this.properties.get("text-shadow");
        // this.properties.get("text-transform");
        // this.properties.get("word-spacing");
    }

    /**
     * @see org.apache.fop.fo.FObj#charIterator
     */
    public CharIterator charIterator() {
        return new OneCharIterator(characterValue);
        // But what it the character is ignored due to white space handling?
    }

    public void acceptVisitor(FOTreeVisitor fotv) {
        fotv.serveVisitor(this);
    }

}
