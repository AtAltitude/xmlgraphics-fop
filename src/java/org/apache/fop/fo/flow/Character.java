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

// Java
import java.util.List;

// XML
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

// FOP
import org.apache.fop.fo.CharIterator;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.OneCharIterator;
import org.apache.fop.layoutmgr.CharacterLayoutManager;

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

    /**
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
     * XSL Content Model: empty
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws SAXParseException {
            invalidChildError(loc, nsURI, localName);
    }

    /**
     * @see org.apache.fop.fo.FObj#charIterator
     */
    public CharIterator charIterator() {
        return new OneCharIterator(characterValue);
        // But what it the character is ignored due to white space handling?
    }

    /**
     * @see org.apache.fop.fo.FObj#addLayoutManager(List)
     */
    public void addLayoutManager(List list) { 	 
        String str = getPropString(PR_CHARACTER);
        if (str.length() == 1) {
            CharacterLayoutManager lm = new CharacterLayoutManager(this);
            list.add(lm);
        }
    }

    /**
     * @see org.apache.fop.fo.FObj#getName()
     */
    public String getName() {
        return "fo:character";
    }
    
    /**
     * @see org.apache.fop.fo.FObj#getNameId()
     */
    public int getNameId() {
        return FO_CHARACTER;
    }
}
