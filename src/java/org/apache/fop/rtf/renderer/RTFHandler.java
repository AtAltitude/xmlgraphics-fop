/*
 * $Id: RTFHandler.java,v 1.4 2003/03/07 09:47:56 jeremias Exp $
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
package org.apache.fop.rtf.renderer;

// Java
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;

// XML
import org.xml.sax.SAXException;

// FOP
import org.apache.fop.layout.FontInfo;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.StructureHandler;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.properties.Constants;
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.flow.Flow;
import org.apache.fop.fo.flow.ExternalGraphic;
import org.apache.fop.fo.flow.InstreamForeignObject;
import org.apache.fop.fo.flow.Leader;
import org.apache.fop.fo.flow.ListBlock;
import org.apache.fop.fo.flow.ListItem;
import org.apache.fop.fo.flow.Table;
import org.apache.fop.fo.flow.TableBody;
import org.apache.fop.fo.flow.TableCell;
import org.apache.fop.fo.flow.TableRow;

// JFOR
import org.apache.fop.rtf.rtflib.rtfdoc.RtfAttributes;
import org.apache.fop.rtf.rtflib.rtfdoc.RtfFile;
import org.apache.fop.rtf.rtflib.rtfdoc.RtfSection;
import org.apache.fop.rtf.rtflib.rtfdoc.RtfText;
import org.apache.fop.rtf.rtflib.rtfdoc.RtfParagraph;
import org.apache.fop.rtf.rtflib.rtfdoc.RtfDocumentArea;

/**
 * RTF Handler: generates RTF output using the structure events from
 * the FO Tree sent to this structure handler.
 *
 * @author bdelacretaz@apache.org
 */
public class RTFHandler extends StructureHandler {

    private FontInfo fontInfo = new FontInfo();
    private RtfFile rtfFile;
    private final OutputStream os;
    private RtfSection sect;
    private RtfDocumentArea docArea;
    private RtfParagraph para;
    private boolean warned = false;

    private static final String ALPHA_WARNING = "WARNING: RTF renderer is "
        + "veryveryalpha at this time, see class org.apache.fop.rtf.renderer.RTFHandler";

    /**
     * Creates a new RTF structure handler.
     * @param os OutputStream to write to
     */
    public RTFHandler(OutputStream os) {
        this.os = os;
        // use pdf fonts for now, this is only for resolving names
        org.apache.fop.render.pdf.FontSetup.setup(fontInfo, null);
        System.err.println(ALPHA_WARNING);
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#getFontInfo()
     */
    public FontInfo getFontInfo() {
        return this.fontInfo;
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        // FIXME sections should be created
        try {
            rtfFile = new RtfFile(new OutputStreamWriter(os));
            docArea = rtfFile.startDocumentArea();
        } catch (IOException ioe) {
            // FIXME could we throw Exception in all StructureHandler events?
            throw new SAXException("IOException: " + ioe);
        }
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endDocument()
     */
    public void endDocument() throws SAXException {
        try {
            rtfFile.flush();
        } catch (IOException ioe) {
            // FIXME could we throw Exception in all StructureHandler events?
            throw new SAXException("IOException: " + ioe);
        }
    }

    /**
     * @see org.apache.fop.fo.StructureHandler
     */
    public void startPageSequence(PageSequence pageSeq)  {
        try {
            sect = docArea.newSection();
            if (!warned) {
                sect.newParagraph().newText(ALPHA_WARNING);
                warned = true;
            }
        } catch (IOException ioe) {
            // FIXME could we throw Exception in all StructureHandler events?
            throw new Error("IOException: " + ioe);
        }
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endPageSequence(PageSequence)
     */
    public void endPageSequence(PageSequence pageSeq) throws FOPException {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startFlow(Flow)
     */
    public void startFlow(Flow fl) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endFlow(Flow)
     */
    public void endFlow(Flow fl) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startBlock(Block)
     */
    public void startBlock(Block bl) {
        try {
            RtfAttributes rtfAttr = new RtfAttributes();
            rtfAttr.set(mapBlockTextAlign(bl));
            para = sect.newParagraph(rtfAttr);
        } catch (IOException ioe) {
            // FIXME could we throw Exception in all StructureHandler events?
            throw new Error("IOException: " + ioe);
        }
    }


    /**
     * @see org.apache.fop.fo.StructureHandler#endBlock(Block)
     */
    public void endBlock(Block bl) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startTable(Table)
     */
    public void startTable(Table tbl) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endTable(Table)
     */
    public void endTable(Table tbl) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startHeader(TableBody)
     */
    public void startHeader(TableBody th) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endHeader(TableBody)
     */
    public void endHeader(TableBody th) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startFooter(TableBody)
     */
    public void startFooter(TableBody tf) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endFooter(TableBody)
     */
    public void endFooter(TableBody tf) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startBody(TableBody)
     */
    public void startBody(TableBody tb) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endBody(TableBody)
     */
    public void endBody(TableBody tb) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startRow(TableRow)
     */
    public void startRow(TableRow tr) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endRow(TableRow)
     */
    public void endRow(TableRow tr) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startCell(TableCell)
     */
    public void startCell(TableCell tc) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endCell(TableCell)
     */
    public void endCell(TableCell tc) {
    }

    // Lists
    /**
     * @see org.apache.fop.fo.StructureHandler#startList(ListBlock)
     */
    public void startList(ListBlock lb) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endList(ListBlock)
     */
    public void endList(ListBlock lb) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startListItem(ListItem)
     */
    public void startListItem(ListItem li) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endListItem(ListItem)
     */
    public void endListItem(ListItem li) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startListLabel()
     */
    public void startListLabel() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endListLabel()
     */
    public void endListLabel() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startListBody()
     */
    public void startListBody() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endListBody()
     */
    public void endListBody() {
    }

    // Static Regions
    /**
     * @see org.apache.fop.fo.StructureHandler#startStatic()
     */
    public void startStatic() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endStatic()
     */
    public void endStatic() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startMarkup()
     */
    public void startMarkup() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endMarkup()
     */
    public void endMarkup() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#startLink()
     */
    public void startLink() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#endLink()
     */
    public void endLink() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#image(ExternalGraphic)
     */
    public void image(ExternalGraphic eg) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#pageRef()
     */
    public void pageRef() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#foreignObject(InstreamForeignObject)
     */
    public void foreignObject(InstreamForeignObject ifo) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#footnote()
     */
    public void footnote() {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#leader(Leader)
     */
    public void leader(Leader l) {
    }

    /**
     * @see org.apache.fop.fo.StructureHandler#characters(char[], int, int)
     */
    public void characters(char data[], int start, int length) {
        try {
            para.newText(new String(data, start, length));
         } catch (IOException ioe) {
            // FIXME could we throw Exception in all StructureHandler events?
            throw new Error("IOException: " + ioe);
        }
    }

    private static String mapBlockTextAlign(Block bl) {
        int fopValue = bl.properties.get("text-align").getEnum();
        String rtfValue = null;
        switch (fopValue) {
            case Constants.CENTER: {
                rtfValue = RtfText.ALIGN_CENTER;
                break;
            }
            case Constants.END: {
                rtfValue = RtfText.ALIGN_RIGHT;
                break;
            }
            case Constants.JUSTIFY: {
                rtfValue = RtfText.ALIGN_JUSTIFIED;
                break;
            }
            default: {
                rtfValue = RtfText.ALIGN_LEFT;
                break;
            }
        }
        return rtfValue;
    }

}
