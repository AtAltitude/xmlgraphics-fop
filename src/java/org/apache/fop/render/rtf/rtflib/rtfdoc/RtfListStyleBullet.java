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
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */
package org.apache.fop.render.rtf.rtflib.rtfdoc;

//Java
import java.io.IOException;

//FOP
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfElement;

public class RtfListStyleBullet extends RtfListStyle {
    
    /**
     * Gets call before a RtfListItem has to be written.
     * 
     * @see org.apache.fop.render.rtf.rtflib.rtfdoc.RtfListStyle#writeListPrefix(RtfListItem)
     */
    public void writeListPrefix(RtfListItem item) throws IOException {
        // bulleted list
        item.writeControlWord("pnlvlblt");
        item.writeControlWord("ilvl0");
        item.writeOneAttribute(RtfListTable.LIST_NUMBER, new Integer(item.getNumber()));
        item.writeOneAttribute("pnindent",
                item.getParentList().attrib.getValue(RtfListTable.LIST_INDENT));
        item.writeControlWord("pnf1");
        item.writeGroupMark(true);
        item.writeControlWord("pndec");
        item.writeOneAttribute(RtfListTable.LIST_FONT_TYPE, "2");
        item.writeControlWord("pntxtb");
        item.writeControlWord("'b7");
        item.writeGroupMark(false);
    }

    /**
     * Gets call before a paragraph, which is contained by a RtfListItem has to be written.
     * 
     * @see org.apache.fop.render.rtf.rtflib.rtfdoc.RtfListStyle#writeParagraphPrefix(RtfElement)
     */
    public void writeParagraphPrefix(RtfElement element) throws IOException {
        element.writeGroupMark(true);
        element.writeControlWord("pntext");
        element.writeGroupMark(false);
    }

    /**
     * Gets call when the list table has to be written.
     * 
     * @see org.apache.fop.render.rtf.rtflib.rtfdoc.RtfListStyle#writeLevelGroup(RtfElement)
     */
    public void writeLevelGroup(RtfElement element) throws IOException {
        element.attrib.set(RtfListTable.LIST_NUMBER_TYPE, 23);
        element.writeGroupMark(true);
        element.writeOneAttributeNS(RtfListTable.LIST_TEXT_FORM, "\\'01\\'b7");
        element.writeGroupMark(false);
            
        element.writeGroupMark(true);
        element.writeOneAttributeNS(RtfListTable.LIST_NUM_POSITION, null);
        element.writeGroupMark(false);
            
        element.attrib.set(RtfListTable.LIST_FONT_TYPE, 2);
    }
       
}
