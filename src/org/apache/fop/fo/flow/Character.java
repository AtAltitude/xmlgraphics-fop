/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

    Copyright (C) 1999 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "FOP" and  "Apache Software Foundation"  must not be used to
    endorse  or promote  products derived  from this  software without  prior
    written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 James Tauber <jtauber@jtauber.com>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

 */

package org.apache.fop.fo.flow;

//fop
import org.apache.fop.fo.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.layout.BlockArea;
import org.apache.fop.layout.Area;
import org.apache.fop.layout.inline.InlineArea;
import org.apache.fop.fo.FObj;
import org.apache.fop.layout.FontState;
import org.apache.fop.apps.FOPException;
import org.apache.fop.messaging.MessageHandler;


/**
 *  this class represents the flow object 'fo:character'. Its use is defined by
 *  the spec: "The fo:character flow object represents a character that is mapped to
 *  a glyph for presentation. It is an atomic unit to the formatter.
 *  When the result tree is interpreted as a tree of formatting objects,
 *  a character in the result tree is treated as if it were an empty
 *  element of type fo:character with a character attribute
 *  equal to the Unicode representation of the character.
 *  The semantics of an "auto" value for character properties, which is
 *  typically their initial value,  are based on the Unicode codepoint.
 *  Overrides may be specified in an implementation-specific manner." (6.6.3)
 *
 */
public class Character  extends FObj {
    public final static int OK = 0;
    public final static int DOESNOT_FIT = 1;


    public Character(FObj parent, PropertyList propertyList) {
        super(parent, propertyList);
        this.name = "fo:character";
    }

    public static FObj.Maker maker() {
        return new Character.Maker();
    }


    public static class Maker extends FObj.Maker {
        public FObj make(FObj parent,
                         PropertyList propertyList) throws FOPException {
            return new Character(parent, propertyList);
        }
    }


    public Status layout(Area area) throws FOPException {
        BlockArea blockArea;
        blockArea = (BlockArea) area;
        boolean textDecoration;

        //retrieving font property information for fo:leader
        String fontFamily = this.properties.get("font-family").getString();
        String fontStyle = this.properties.get("font-style").getString();
        String fontWeight = this.properties.get("font-weight").getString();
        int fontSize =
          this.properties.get("font-size").getLength().mvalue();
        //wrapping it up into Fontstate
        FontState fontstate = new FontState(area.getFontInfo(), fontFamily,
                                            fontStyle, fontWeight, fontSize);
        //color properties
        ColorType c = this.properties.get("color").getColorType();
        float red = c.red();
        float green = c.green();
        float blue = c.blue();

        int whiteSpaceCollapse = this.properties.get(
                                    "white-space-collapse").getEnum();
        int wrapOption = this.parent.properties.get("wrap-option").getEnum();

        int tmp = this.properties.get("text-decoration").getEnum();
        if (tmp == org.apache.fop.fo.properties.TextDecoration.UNDERLINE) {
          textDecoration = true;
        } else {
          textDecoration = false;
        }

        //Character specific properties
        char characterValue = this.properties.get("character").getCharacter();


        // initialize id
        String id = this.properties.get("id").getString();
        blockArea.getIDReferences().initializeID(id, blockArea);
        blockArea.addCharacter(fontstate,red,green,blue,wrapOption,this.getLinkSet(),
                               whiteSpaceCollapse,characterValue,textDecoration);
        return new Status(Status.OK);


    }

}
