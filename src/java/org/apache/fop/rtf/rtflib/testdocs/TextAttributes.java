/*
 * $Id$
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
 * The RTF library of the FOP project consists of voluntary contributions made by
 * many individuals on behalf of the Apache Software Foundation and was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and contributors of
 * the jfor project (www.jfor.org), who agreed to donate jfor to the FOP project.
 * For more information on the Apache Software Foundation, please
 * see <http://www.apache.org/>.
 */
package org.apache.fop.rtf.rtflib.testdocs;

import java.util.Date;
import java.io.*;
import org.apache.fop.rtf.rtflib.rtfdoc.*;

/**  Generates a simple RTF test document for the jfor rtflib package.
 *  @author Bertrand Delacretaz bdelacretaz@codeconsult.ch
 */

class TextAttributes extends TestDocument
{
    /** generate the body of the test document */
    protected void generateDocument(RtfDocumentArea rda,RtfSection sect)
    throws IOException {
        final RtfParagraph para = sect.newParagraph();
        para.newText("This is normal\n");
        para.newText("This is bold\n",new RtfAttributes().set(RtfText.ATTR_BOLD));
        para.newText("This is italic\n",new RtfAttributes().set(RtfText.ATTR_ITALIC));
        para.newText("This is underline\n",new RtfAttributes().set(RtfText.ATTR_UNDERLINE));
        
        // RTF font sizes are in half-points
        para.newText("This is size 48\n",new RtfAttributes().set(RtfText.ATTR_FONT_SIZE,96));
        
        para.newText(
            "This is bold and italic\n",
            new RtfAttributes().set(RtfText.ATTR_BOLD).set(RtfText.ATTR_ITALIC)
          );
        
        final RtfAttributes attr = new RtfAttributes();
        attr.set(RtfText.ATTR_BOLD).set(RtfText.ATTR_ITALIC);
        attr.set(RtfText.ATTR_UNDERLINE);
        attr.set(RtfText.ATTR_FONT_SIZE,72);
        para.newText("This is bold, italic, underline and size 36\n",attr);
        
        para.newText("This is back to normal\n");
    }
}