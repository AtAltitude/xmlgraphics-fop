/*
 * $Id: PDFRenderer.java,v 1.137 2003/03/05 20:38:27 jeremias Exp $
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
package org.apache.fop.render.pdf;

// Java
import java.io.IOException;
import java.io.OutputStream;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.Map;
import java.util.List;

// XML
import org.w3c.dom.Document;

// Avalon
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

// FOP
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Version;
import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.CTM;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.Page;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Title;
import org.apache.fop.area.Trait;
import org.apache.fop.area.TreeExt;
import org.apache.fop.area.extensions.BookmarkData;
import org.apache.fop.area.inline.Character;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.Viewport;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.datatypes.ColorType;
import org.apache.fop.fonts.Typeface;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontSetup;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.image.FopImage;
import org.apache.fop.image.ImageFactory;
import org.apache.fop.image.XMLImage;
import org.apache.fop.pdf.PDFAnnotList;
import org.apache.fop.pdf.PDFColor;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFEncryptionManager;
import org.apache.fop.pdf.PDFFilterList;
import org.apache.fop.pdf.PDFInfo;
import org.apache.fop.pdf.PDFLink;
import org.apache.fop.pdf.PDFOutline;
import org.apache.fop.pdf.PDFPage;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.pdf.PDFState;
import org.apache.fop.pdf.PDFStream;
import org.apache.fop.pdf.PDFText;
import org.apache.fop.pdf.PDFXObject;
import org.apache.fop.render.PrintRenderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.traits.BorderProps;


/*
todo:

word rendering and optimistion
pdf state optimisation
line and border
background pattern
writing mode
text decoration

*/

/**
 * Renderer that renders areas to PDF
 *
 */
public class PDFRenderer extends PrintRenderer {
    /**
     * The mime type for pdf
     */
    public static final String MIME_TYPE = "application/pdf";

    /**
     * the PDF Document being created
     */
    protected PDFDocument pdfDoc;

    /**
     * Map of pages using the PageViewport as the key
     * this is used for prepared pages that cannot be immediately
     * rendered
     */
    protected Map pages = null;

    /**
     * Page references are stored using the PageViewport as the key
     * when a reference is made the PageViewport is used
     * for pdf this means we need the pdf page reference
     */
    protected Map pageReferences = new java.util.HashMap();
    /** Page viewport references */
    protected Map pvReferences = new java.util.HashMap();

    /**
     * The output stream to write the document to
     */
    protected OutputStream ostream;

    /**
     * the /Resources object of the PDF document being created
     */
    protected PDFResources pdfResources;

    /**
     * the current stream to add PDF commands to
     */
    protected PDFStream currentStream;

    /**
     * the current annotation list to add annotations to
     */
    protected PDFResourceContext currentContext = null;

    /**
     * the current page to add annotations to
     */
    protected PDFPage currentPage;

    /** drawing state */
    protected PDFState currentState = null;

    /** Name of currently selected font */
    protected String currentFontName = "";
    /** Size of currently selected font */
    protected int currentFontSize = 0;
    /** page height */
    protected int pageHeight;

    /** Registry of PDF filters */
    protected Map filterMap;

    /**
     * true if a TJ command is left to be written
     */
    protected boolean textOpen = false;

    /**
     * the previous Y coordinate of the last word written.
     * Used to decide if we can draw the next word on the same line.
     */
    protected int prevWordY = 0;

    /**
     * the previous X coordinate of the last word written.
     * used to calculate how much space between two words
     */
    protected int prevWordX = 0;

    /**
     * The width of the previous word. Used to calculate space between
     */
    protected int prevWordWidth = 0;

    /**
     * reusable word area string buffer to reduce memory usage
     */
    private StringBuffer wordAreaPDF = new StringBuffer();

    /**
     * Offset for rendering text, taking into account borders and padding for
     * both region and block.
     */
    protected int bpMarginOffset = 0;

    /**
     * Offset for rendering text, taking into account borders and padding for 
     * both the region and block.
     */
    protected int ipMarginOffset = 0;

    /**
     * create the PDF renderer
     */
    public PDFRenderer() {
    }

    /**
     * Configure the PDF renderer.
     * Get the configuration to be used for pdf stream filters,
     * fonts etc.
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration cfg) throws ConfigurationException {
        //PDF filters
        this.filterMap = PDFFilterList.buildFilterMapFromConfiguration(cfg);

        //Font configuration
        List cfgFonts = FontSetup.buildFontListFromConfiguration(cfg);
        if (this.fontList == null) {
            this.fontList = cfgFonts;
        } else {
            this.fontList.addAll(cfgFonts);
        }
    }

    /**
     * @see org.apache.fop.render.Renderer#setUserAgent(FOUserAgent)
     */
    public void setUserAgent(FOUserAgent agent) {
        super.setUserAgent(agent);
        PDFXMLHandler xmlHandler = new PDFXMLHandler();
        //userAgent.setDefaultXMLHandler(MIME_TYPE, xmlHandler);
        String svg = "http://www.w3.org/2000/svg";
        addXMLHandler(userAgent, MIME_TYPE, svg, xmlHandler);
    }

    /**
     * @see org.apache.fop.render.Renderer#startRenderer(OutputStream)
     */
    public void startRenderer(OutputStream stream) throws IOException {
        ostream = stream;
        producer = "FOP " + Version.getVersion();
        this.pdfDoc = new PDFDocument(producer);
        setupLogger(this.pdfDoc);
        this.pdfDoc.setCreator(creator);
        this.pdfDoc.setCreationDate(creationDate);
        this.pdfDoc.setFilterMap(filterMap);
        this.pdfDoc.outputHeader(stream);

        //Setup encryption if necessary
        PDFEncryptionManager.setupPDFEncryption(userAgent, this.pdfDoc, getLogger());
    }

    /**
     * @see org.apache.fop.render.Renderer#stopRenderer()
     */
    public void stopRenderer() throws IOException {
        pdfDoc.getResources().addFonts(pdfDoc, 
            (org.apache.fop.apps.Document) fontInfo);
        pdfDoc.outputTrailer(ostream);

        this.pdfDoc = null;
        ostream = null;

        pages = null;

        pageReferences.clear();
        pvReferences.clear();
        pdfResources = null;
        currentStream = null;
        currentContext = null;
        currentPage = null;
        currentState = null;
        currentFontName = "";
        wordAreaPDF = new StringBuffer();
    }

    /**
     * @see org.apache.fop.render.Renderer#supportsOutOfOrder()
     */
    public boolean supportsOutOfOrder() {
        return true;
    }

    /**
     * @see org.apache.fop.render.Renderer#renderExtension(TreeExt)
     */
    public void renderExtension(TreeExt ext) {
        // render bookmark extension
        if (ext instanceof BookmarkData) {
            renderRootExtensions((BookmarkData)ext);
        }
    }

    /**
     * Renders the root extension elements
     * @param bookmarks the bookmarks to render
     */
    protected void renderRootExtensions(BookmarkData bookmarks) {
        for (int i = 0; i < bookmarks.getCount(); i++) {
            BookmarkData ext = bookmarks.getSubData(i);
            renderOutline(ext, null);
        }
    }

    private void renderOutline(BookmarkData outline, PDFOutline parentOutline) {
        PDFOutline outlineRoot = pdfDoc.getOutlineRoot();
        PDFOutline pdfOutline = null;
        PageViewport pv = outline.getPage();
        if (pv != null) {
            Rectangle2D bounds = pv.getViewArea();
            double h = bounds.getHeight();
            float yoffset = (float)h / 1000f;
            String intDest = (String)pageReferences.get(pv.getKey());
            if (parentOutline == null) {
                pdfOutline = pdfDoc.getFactory().makeOutline(outlineRoot,
                                        outline.getLabel(), intDest, yoffset);
            } else {
                PDFOutline pdfParentOutline = parentOutline;
                pdfOutline = pdfDoc.getFactory().makeOutline(pdfParentOutline,
                                        outline.getLabel(), intDest, yoffset);
            }
        }

        for (int i = 0; i < outline.getCount(); i++) {
            renderOutline(outline.getSubData(i), pdfOutline);
        }
    }

    /** Saves the graphics state of the rendering engine. */
    protected void saveGraphicsState() {
        currentStream.add("q\n");
    }

    /** Restores the last graphics state of the rendering engine. */
    protected void restoreGraphicsState() {
        currentStream.add("Q\n");
    }

    /** Indicates the beginning of a text object. */
    protected void beginTextObject() {
        currentStream.add("BT\n");
    }

    /** Indicates the end of a text object. */
    protected void endTextObject() {
        currentStream.add("ET\n");
    }

    /**
     * Start the next page sequence.
     * For the pdf renderer there is no concept of page sequences
     * but it uses the first available page sequence title to set
     * as the title of the pdf document.
     *
     * @param seqTitle the title of the page sequence
     */
    public void startPageSequence(Title seqTitle) {
        if (seqTitle != null) {
            String str = convertTitleToString(seqTitle);
            PDFInfo info = this.pdfDoc.getInfo();
            info.setTitle(str);
        }
    }

    /**
     * The pdf page is prepared by making the page.
     * The page is made in the pdf document without any contents
     * and then stored to add the contents later.
     * The page objects is stored using the area tree PageViewport
     * as a key.
     *
     * @param page the page to prepare
     */
    public void preparePage(PageViewport page) {
        this.pdfResources = this.pdfDoc.getResources();

        Rectangle2D bounds = page.getViewArea();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        currentPage = this.pdfDoc.getFactory().makePage(
            this.pdfResources,
            (int) Math.round(w / 1000), (int) Math.round(h / 1000));
        if (pages == null) {
            pages = new java.util.HashMap();
        }
        pages.put(page, currentPage);
        pageReferences.put(page.getKey(), currentPage.referencePDF());
        pvReferences.put(page.getKey(), page);
    }

    /**
     * This method creates a pdf stream for the current page
     * uses it as the contents of a new page. The page is written
     * immediately to the output stream.
     * @see org.apache.fop.render.Renderer#renderPage(PageViewport)
     */
    public void renderPage(PageViewport page)
                throws IOException, FOPException {
        if (pages != null
                && (currentPage = (PDFPage) pages.get(page)) != null) {
            pages.remove(page);
            Rectangle2D bounds = page.getViewArea();
            double h = bounds.getHeight();
            pageHeight = (int) h;
        } else {
            this.pdfResources = this.pdfDoc.getResources();
            Rectangle2D bounds = page.getViewArea();
            double w = bounds.getWidth();
            double h = bounds.getHeight();
            pageHeight = (int) h;
            currentPage = this.pdfDoc.getFactory().makePage(
                this.pdfResources,
                (int) Math.round(w / 1000), (int) Math.round(h / 1000));
            pageReferences.put(page.getKey(), currentPage.referencePDF());
            pvReferences.put(page.getKey(), page);
        }
        currentStream = this.pdfDoc.getFactory()
            .makeStream(PDFFilterList.CONTENT_FILTER, false);

        currentState = new PDFState();
        currentState.setTransform(new AffineTransform(1, 0, 0, -1, 0,
                                   (int) Math.round(pageHeight / 1000)));
        // Transform origin at top left to origin at bottom left
        currentStream.add("1 0 0 -1 0 "
                           + (int) Math.round(pageHeight / 1000) + " cm\n");
        currentFontName = "";

        Page p = page.getPage();
        renderPageAreas(p);

        this.pdfDoc.registerObject(currentStream);
        currentPage.setContents(currentStream);
        PDFAnnotList annots = currentPage.getAnnotations();
        if (annots != null) {
            this.pdfDoc.addObject(annots);
        }
        this.pdfDoc.addObject(currentPage);
        this.pdfDoc.output(ostream);
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#startVParea(CTM)
     */
    protected void startVParea(CTM ctm) {
        // Set the given CTM in the graphics state
        currentState.push();
        currentState.setTransform(
          new AffineTransform(CTMHelper.toPDFArray(ctm)));

        saveGraphicsState();
        // multiply with current CTM
        currentStream.add(CTMHelper.toPDFString(ctm) + " cm\n");
        // Set clip?
        beginTextObject();
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#endVParea()
     */
    protected void endVParea() {
        endTextObject();
        restoreGraphicsState();
        currentState.pop();
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderBlocks(Block, List)
     */
    protected void renderBlocks(Block block, List blocks) {
        int saveIPMargin = ipMarginOffset;
        int saveBPMargin = bpMarginOffset;
        if (block != null) {
            Integer spaceStart = (Integer) block.getTrait(Trait.SPACE_START); 
            if (spaceStart != null) {
                ipMarginOffset += spaceStart.intValue();
            }

            Integer paddingStart = (Integer) block.getTrait(Trait.PADDING_START);
            if (paddingStart != null) {
                ipMarginOffset += paddingStart.intValue();
            }
            Integer paddingBefore = (Integer) block.getTrait(Trait.PADDING_BEFORE);
            if (paddingBefore != null) {
                bpMarginOffset += paddingBefore.intValue();
            }

            BorderProps borderStartWidth = (BorderProps) block.getTrait(Trait.BORDER_START);
            if (borderStartWidth != null) {
                ipMarginOffset += borderStartWidth.width;
            }
            BorderProps borderBeforeWidth = (BorderProps) block.getTrait(Trait.BORDER_BEFORE);
            if (borderBeforeWidth != null) {
                bpMarginOffset += borderBeforeWidth.width;
            }
        }
        super.renderBlocks(block, blocks);
        ipMarginOffset = saveIPMargin;
        bpMarginOffset = saveBPMargin;
    }

    /**
     * Handle the traits for a region
     * This is used to draw the traits for the given page region.
     * (See Sect. 6.4.1.2 of XSL-FO spec.)
     * @param region the RegionViewport whose region is to be drawn
     */
    protected void handleRegionTraits(RegionViewport region) {
        currentFontName = "";
        float startx = 0;
        float starty = 0;
        Rectangle2D viewArea = region.getViewArea();
        float width = (float)(viewArea.getWidth() / 1000f);
        float height = (float)(viewArea.getHeight() / 1000f);

        if (region.getRegion().getRegionClass()
                == org.apache.fop.fo.pagination.Region.BODY_CODE) {
            bpMarginOffset = region.getBorderAndPaddingWidthBefore();
            ipMarginOffset = region.getBorderAndPaddingWidthStart();
        }

        drawBackAndBorders(region, startx, starty, width, height);
    }

    /**
     * Handle block traits.
     * The block could be any sort of block with any positioning
     * so this should render the traits such as border and background
     * in its position.
     *
     * @param block the block to render the traits
     */
    protected void handleBlockTraits(Block block) {
        /*  ipMarginOffset for a particular block = region border + 
         *  region padding + parent block padding + current block padding
         */

        float startx = (currentIPPosition + ipMarginOffset) / 1000f;
        float starty = (currentBPPosition + bpMarginOffset) / 1000f;
        float width = block.getWidth() / 1000f;

        Integer spaceStart = (Integer) block.getTrait(Trait.SPACE_START); 
        if (spaceStart != null) {
            startx += spaceStart.floatValue() / 1000;
            width -= spaceStart.floatValue() / 1000;
        }
        Integer spaceEnd = (Integer) block.getTrait(Trait.SPACE_END); 
        if (spaceEnd != null) {
            width -= spaceEnd.floatValue() / 1000;
        }

        drawBackAndBorders(block, startx, starty,
            width, block.getHeight() / 1000f);
    }

    /**
     * Draw the background and borders.
     * This draws the background and border traits for an area given
     * the position.
     *
     * @param block the area to get the traits from
     * @param startx the start x position
     * @param starty the start y position
     * @param width the width of the area
     * @param height the height of the area
     */
    protected void drawBackAndBorders(Area block,
                    float startx, float starty,
                    float width, float height) {
        // draw background then border

        boolean started = false;
        Trait.Background back;
        back = (Trait.Background)block.getTrait(Trait.BACKGROUND);
        if (back != null) {
            started = true;
            closeText();
            endTextObject();
            //saveGraphicsState();

            if (back.getColor() != null) {
                updateColor(back.getColor(), true, null);
                currentStream.add(startx + " " + starty + " "
                                  + width + " " + height + " re\n");
                currentStream.add("f\n");
            }
            if (back.getURL() != null) {
                ImageFactory fact = ImageFactory.getInstance();
                FopImage fopimage = fact.getImage(back.getURL(), userAgent);
                if (fopimage != null && fopimage.load(FopImage.DIMENSIONS, userAgent.getLogger())) {
                    if (back.getRepeat() == BackgroundRepeat.REPEAT) {
                        // create a pattern for the image
                    } else {
                        // place once
                        Rectangle2D pos;
                        pos = new Rectangle2D.Float((startx + back.getHoriz()) * 1000,
                                                    (starty + back.getVertical()) * 1000,
                                                    fopimage.getWidth() * 1000,
                                                    fopimage.getHeight() * 1000);
                        putImage(back.getURL(), pos);
                    }
                }
            }
        }

        BorderProps bps = (BorderProps)block.getTrait(Trait.BORDER_BEFORE);
        if (bps != null) {
            float endx = startx + width;

            if (!started) {
                started = true;
                closeText();
                endTextObject();
                //saveGraphicsState();
            }

            float bwidth = bps.width / 1000f;
            updateColor(bps.color, true, null);
            currentStream.add(startx + " " + starty + " "
                              + width + " " + bwidth + " re\n");
            currentStream.add("f\n");
        }
        bps = (BorderProps)block.getTrait(Trait.BORDER_AFTER);
        if (bps != null) {
            if (!started) {
                started = true;
                closeText();
                endTextObject();
                //saveGraphicsState();
            }

            float bwidth = bps.width / 1000f;
            updateColor(bps.color, true, null);
            currentStream.add(startx + " " + (starty + height - bwidth) + " "
                              + width + " " + bwidth + " re\n");
            currentStream.add("f\n");
        }
        bps = (BorderProps)block.getTrait(Trait.BORDER_START);
        if (bps != null) {
            if (!started) {
                started = true;
                closeText();
                endTextObject();
                //saveGraphicsState();
            }

            float bwidth = bps.width / 1000f;
            updateColor(bps.color, true, null);
            currentStream.add(startx + " " + starty + " "
                              + bwidth + " " + height + " re\n");
            currentStream.add("f\n");
        }
        bps = (BorderProps)block.getTrait(Trait.BORDER_END);
        if (bps != null) {
            if (!started) {
                started = true;
                closeText();
                endTextObject();
                //saveGraphicsState();
            }

            float bwidth = bps.width / 1000f;
            updateColor(bps.color, true, null);
            currentStream.add((startx + width - bwidth) + " " + starty + " "
                              + bwidth + " " + height + " re\n");
            currentStream.add("f\n");
        }
        if (started) {
            //restoreGraphicsState();
            beginTextObject();
            // font last set out of scope in text section
            currentFontName = "";
        }
    }

    /**
     * Draw a line.
     *
     * @param startx the start x position
     * @param starty the start y position
     * @param endx the x end position
     * @param endy the y end position
     */
    private void drawLine(float startx, float starty, float endx, float endy) {
        currentStream.add(startx + " " + starty + " m\n");
        currentStream.add(endx + " " + endy + " l\n");
        currentStream.add("S\n");
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderBlockViewport(BlockViewport, List)
     */
    protected void renderBlockViewport(BlockViewport bv, List children) {
        // clip and position viewport if necessary

        // save positions
        int saveIP = currentIPPosition;
        int saveBP = currentBPPosition;
        String saveFontName = currentFontName;

        CTM ctm = bv.getCTM();

        if (bv.getPositioning() == Block.ABSOLUTE) {

            currentIPPosition = 0;
            currentBPPosition = 0;

            closeText();
            endTextObject();

            if (bv.getClip()) {
                saveGraphicsState();
                float x = (float)(bv.getXOffset() + containingIPPosition) / 1000f;
                float y = (float)(bv.getYOffset() + containingBPPosition) / 1000f;
                float width = (float)bv.getWidth() / 1000f;
                float height = (float)bv.getHeight() / 1000f;
                clip(x, y, width, height);
            }

            CTM tempctm = new CTM(containingIPPosition, containingBPPosition);
            ctm = tempctm.multiply(ctm);

            startVParea(ctm);
            handleBlockTraits(bv);
            renderBlocks(bv, children);
            endVParea();

            if (bv.getClip()) {
                restoreGraphicsState();
            }
            beginTextObject();

            // clip if necessary

            currentIPPosition = saveIP;
            currentBPPosition = saveBP;
        } else {

            if (ctm != null) {
                currentIPPosition = 0;
                currentBPPosition = 0;

                closeText();
                endTextObject();

                double[] vals = ctm.toArray();
                //boolean aclock = vals[2] == 1.0;
                if (vals[2] == 1.0) {
                    ctm = ctm.translate(-saveBP - bv.getHeight(), -saveIP);
                } else if (vals[0] == -1.0) {
                    ctm = ctm.translate(-saveIP - bv.getWidth(), -saveBP - bv.getHeight());
                } else {
                    ctm = ctm.translate(saveBP, saveIP - bv.getWidth());
                }
            }

            // clip if necessary
            if (bv.getClip()) {
                if (ctm == null) {
                    closeText();
                    endTextObject();
                }
                saveGraphicsState();
                float x = (float)bv.getXOffset() / 1000f;
                float y = (float)bv.getYOffset() / 1000f;
                float width = (float)bv.getWidth() / 1000f;
                float height = (float)bv.getHeight() / 1000f;
                clip(x, y, width, height);
            }

            if (ctm != null) {
                startVParea(ctm);
            }
            handleBlockTraits(bv);
            renderBlocks(bv, children);
            if (ctm != null) {
                endVParea();
            }

            if (bv.getClip()) {
                restoreGraphicsState();
                if (ctm == null) {
                    beginTextObject();
                }
            }
            if (ctm != null) {
                beginTextObject();
            }

            currentIPPosition = saveIP;
            currentBPPosition = saveBP;
            currentBPPosition += (int)(bv.getHeight());
        }
        currentFontName = saveFontName;
    }

    /**
     * Clip an area.
     * write a clipping operation given coordinates in the current
     * transform.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the area
     * @param height the height of the area
     */
    protected void clip(float x, float y, float width, float height) {
        currentStream.add(x + " " + y + " m\n");
        currentStream.add((x + width) + " " + y + " l\n");
        currentStream.add((x + width) + " " + (y + height) + " l\n");
        currentStream.add(x + " " + (y + height) + " l\n");
        currentStream.add("h\n");
        currentStream.add("W\n");
        currentStream.add("n\n");
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderLineArea(LineArea)
     */
    protected void renderLineArea(LineArea line) {
        super.renderLineArea(line);
        closeText();
    }

    /**
     * Render inline parent area.
     * For pdf this handles the inline parent area traits such as
     * links, border, background.
     * @param ip the inline parent area
     */
    public void renderInlineParent(InlineParent ip) {
        float start = (currentBlockIPPosition + ipMarginOffset) / 1000f;
        float top = (ip.getOffset() + currentBPPosition + bpMarginOffset) / 1000f;
        float width = ip.getWidth() / 1000f;
        float height = ip.getHeight() / 1000f;
        drawBackAndBorders(ip, start, top, width, height);

        // render contents
        super.renderInlineParent(ip);

        // place the link over the top
        Object tr = ip.getTrait(Trait.INTERNAL_LINK);
        boolean internal = false;
        String dest = null;
        float yoffset = 0;
        if (tr == null) {
            dest = (String)ip.getTrait(Trait.EXTERNAL_LINK);
        } else {
            String pvKey = (String)tr;
            dest = (String)pageReferences.get(pvKey);
            if (dest != null) {
                PageViewport pv = (PageViewport)pvReferences.get(pvKey);
                Rectangle2D bounds = pv.getViewArea();
                double h = bounds.getHeight();
                yoffset = (float)h / 1000f;
                internal = true;
            }
        }
        if (dest != null) {
            // add link to pdf document
            Rectangle2D rect = new Rectangle2D.Float(start, top, width, height);
            // transform rect to absolute coords
            AffineTransform transform = currentState.getTransform();
            rect = transform.createTransformedShape(rect).getBounds();

            int type = internal ? PDFLink.INTERNAL : PDFLink.EXTERNAL;
            PDFLink pdflink = pdfDoc.getFactory().makeLink(
                        rect, dest, type, yoffset);
            currentPage.addAnnotation(pdflink);
        }
    }

    /**
     * @see org.apache.fop.render.Renderer#renderCharacter(Character)
     */
    public void renderCharacter(Character ch) {
        super.renderCharacter(ch);
    }

    /**
     * @see org.apache.fop.render.Renderer#renderText(TextArea)
     */
    public void renderText(TextArea text) {
        StringBuffer pdf = new StringBuffer();

        String name = (String) text.getTrait(Trait.FONT_NAME);
        int size = ((Integer) text.getTrait(Trait.FONT_SIZE)).intValue();

        // This assumes that *all* CIDFonts use a /ToUnicode mapping
        Typeface f = (Typeface) fontInfo.getFonts().get(name);
        boolean useMultiByte = f.isMultiByte();

        // String startText = useMultiByte ? "<FEFF" : "(";
        String startText = useMultiByte ? "<" : "(";
        String endText = useMultiByte ? "> " : ") ";

        updateFont(name, size, pdf);
        ColorType ct = (ColorType) text.getTrait(Trait.COLOR);
        if (ct != null) {
            updateColor(ct, true, pdf);
        }

        // word.getOffset() = only height of text itself
        // currentBlockIPPosition: 0 for beginning of line; nonzero
        //  where previous line area failed to take up entire allocated space
        int rx = currentBlockIPPosition + ipMarginOffset;
        int bl = currentBPPosition + bpMarginOffset + text.getOffset();
/*
        System.out.println("\nBlockIP Position: " + currentBlockIPPosition +
            "; currentBPPosition: " + currentBPPosition +
            "; offset: " + text.getOffset() +
            "; Text = " + text.getTextArea());
*/
        // Set letterSpacing
        //float ls = fs.getLetterSpacing() / this.currentFontSize;
        //pdf.append(ls).append(" Tc\n");

        if (!textOpen || bl != prevWordY) {
            closeText();

            pdf.append("1 0 0 -1 " + (rx / 1000f) + " "
                       + (bl / 1000f) + " Tm [" + startText);
            prevWordY = bl;
            textOpen = true;
        } else {
            // express the space between words in thousandths of an em
            int space = prevWordX - rx + prevWordWidth;
            float emDiff = (float) space / (float) currentFontSize * 1000f;
            // this prevents a problem in Acrobat Reader and other viewers
            // where large numbers cause text to disappear or default to
            // a limit
            if (emDiff < -33000) {
                closeText();

                pdf.append("1 0 0 1 " + (rx / 1000f) + " "
                           + (bl / 1000f) + " Tm [" + startText);
                textOpen = true;
            } else {
                pdf.append(Float.toString(emDiff));
                pdf.append(" ");
                pdf.append(startText);
            }
        }
        prevWordWidth = text.getWidth();
        prevWordX = rx;

        String s = text.getTextArea();

        FontMetrics metrics = fontInfo.getMetricsFor(name);
        Font fs = new Font(name, metrics, size);
        escapeText(s, fs, useMultiByte, pdf);
        pdf.append(endText);

        currentStream.add(pdf.toString());

        super.renderText(text);
    }

    /**
     * Escapes text according to PDF rules.
     * @param s Text to escape
     * @param fs Font state
     * @param useMultiByte Indicates the use of multi byte convention
     * @param pdf target buffer for the escaped text
     */
    public void escapeText(String s, Font fs,
                           boolean useMultiByte, StringBuffer pdf) {
        String startText = useMultiByte ? "<" : "(";
        String endText = useMultiByte ? "> " : ") ";

        boolean kerningAvailable = false;
        Map kerning = fs.getKerning();
        if (kerning != null && !kerning.isEmpty()) {
            kerningAvailable = true;
        }

        int l = s.length();

        for (int i = 0; i < l; i++) {
            char ch = fs.mapChar(s.charAt(i));

            if (!useMultiByte) {
                if (ch > 127) {
                    pdf.append("\\");
                    pdf.append(Integer.toOctalString((int) ch));
                } else {
                    switch (ch) {
                        case '(':
                        case ')':
                        case '\\':
                            pdf.append("\\");
                            break;
                    }
                    pdf.append(ch);
                }
            } else {
                pdf.append(PDFText.toUnicodeHex(ch));
            }

            if (kerningAvailable && (i + 1) < l) {
                addKerning(pdf, (new Integer((int) ch)),
                           (new Integer((int) fs.mapChar(s.charAt(i + 1)))
                           ), kerning, startText, endText);
            }
        }
    }

    private void addKerning(StringBuffer buf, Integer ch1, Integer ch2,
                            Map kerning, String startText, String endText) {
        Map kernPair = (Map) kerning.get(ch1);

        if (kernPair != null) {
            Integer width = (Integer) kernPair.get(ch2);
            if (width != null) {
                buf.append(endText).append(-width.intValue());
                buf.append(' ').append(startText);
            }
        }
    }

    /**
     * Checks to see if we have some text rendering commands open
     * still and writes out the TJ command to the stream if we do
     */
    protected void closeText() {
        if (textOpen) {
            currentStream.add("] TJ\n");
            textOpen = false;
            prevWordX = 0;
            prevWordY = 0;
        }
    }

    private void updateColor(ColorType col, boolean fill, StringBuffer pdf) {
        Color newCol = new Color(col.getRed(), col.getGreen(), col.getBlue());
        boolean update = false;
        if (fill) {
            update = currentState.setBackColor(newCol);
        } else {
            update = currentState.setColor(newCol);
        }

        if (update) {
            PDFColor color = new PDFColor((double)col.getRed(),
                                     (double)col.getGreen(),
                                     (double)col.getBlue());

            closeText();

            if (pdf != null) {
                pdf.append(color.getColorSpaceOut(fill));
            } else {
                currentStream.add(color.getColorSpaceOut(fill));
            }
        }
    }

    private void updateFont(String name, int size, StringBuffer pdf) {
        if ((!name.equals(this.currentFontName))
                || (size != this.currentFontSize)) {
            closeText();

            this.currentFontName = name;
            this.currentFontSize = size;
            pdf = pdf.append("/" + name + " " + ((float) size / 1000f)
                              + " Tf\n");
        }
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderImage(Image, Rectangle2D)
     */
    public void renderImage(Image image, Rectangle2D pos) {
        String url = image.getURL();
        putImage(url, pos);
    }

    /**
     * Adds a PDF XObject (a bitmap) to the PDF that will later be referenced.
     * @param url URL of the bitmap
     * @param pos Position of the bitmap
     */
    protected void putImage(String url, Rectangle2D pos) {
        PDFXObject xobject = pdfDoc.getImage(url);
        if (xobject != null) {
            int w = (int) pos.getWidth() / 1000;
            int h = (int) pos.getHeight() / 1000;
            placeImage((int) pos.getX() / 1000,
                       (int) pos.getY() / 1000, w, h, xobject.getXNumber());
            return;
        }

        ImageFactory fact = ImageFactory.getInstance();
        FopImage fopimage = fact.getImage(url, userAgent);
        if (fopimage == null) {
            return;
        }
        if (!fopimage.load(FopImage.DIMENSIONS, userAgent.getLogger())) {
            return;
        }
        String mime = fopimage.getMimeType();
        if ("text/xml".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA, userAgent.getLogger())) {
                return;
            }
            Document doc = ((XMLImage) fopimage).getDocument();
            String ns = ((XMLImage) fopimage).getNameSpace();

            renderDocument(doc, ns, pos);
        } else if ("image/svg+xml".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA, userAgent.getLogger())) {
                return;
            }
            Document doc = ((XMLImage) fopimage).getDocument();
            String ns = ((XMLImage) fopimage).getNameSpace();

            renderDocument(doc, ns, pos);
        } else if ("image/eps".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA, userAgent.getLogger())) {
                return;
            }
            FopPDFImage pdfimage = new FopPDFImage(fopimage, url);
            int xobj = pdfDoc.addImage(currentContext, pdfimage).getXNumber();
            fact.releaseImage(url, userAgent);
        } else if ("image/jpeg".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA, userAgent.getLogger())) {
                return;
            }
            FopPDFImage pdfimage = new FopPDFImage(fopimage, url);
            int xobj = pdfDoc.addImage(currentContext, pdfimage).getXNumber();
            fact.releaseImage(url, userAgent);

            int w = (int) pos.getWidth() / 1000;
            int h = (int) pos.getHeight() / 1000;
            placeImage((int) pos.getX() / 1000,
                       (int) pos.getY() / 1000, w, h, xobj);
        } else {
            if (!fopimage.load(FopImage.BITMAP, userAgent.getLogger())) {
                return;
            }
            FopPDFImage pdfimage = new FopPDFImage(fopimage, url);
            int xobj = pdfDoc.addImage(currentContext, pdfimage).getXNumber();
            fact.releaseImage(url, userAgent);

            int w = (int) pos.getWidth() / 1000;
            int h = (int) pos.getHeight() / 1000;
            placeImage((int) pos.getX() / 1000,
                       (int) pos.getY() / 1000, w, h, xobj);
        }

        // output new data
        try {
            this.pdfDoc.output(ostream);
        } catch (IOException ioe) {
            // ioexception will be caught later
        }
    }

    /**
     * Places a previously registered image at a certain place on the page.
     * @param x X coordinate
     * @param y Y coordinate
     * @param w width for image
     * @param h height for image
     * @param xobj object number of the referenced image
     */
    protected void placeImage(int x, int y, int w, int h, int xobj) {
        saveGraphicsState();
        currentStream.add(((float) w) + " 0 0 "
                          + ((float) -h) + " "
                          + (((float) currentBlockIPPosition + ipMarginOffset) / 1000f + x) + " "
                          + (((float)(currentBPPosition + bpMarginOffset + 1000 * h)) / 1000f
                          + y) + " cm\n" + "/Im" + xobj + " Do\n");
        restoreGraphicsState();
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderForeignObject(ForeignObject, Rectangle2D)
     */
    public void renderForeignObject(ForeignObject fo, Rectangle2D pos) {
        Document doc = fo.getDocument();
        String ns = fo.getNameSpace();
        renderDocument(doc, ns, pos);
    }

    /**
     * Renders an XML document (SVG for example).
     * @param doc DOM document representing the XML document
     * @param ns Namespace for the document
     * @param pos Position on the page
     */
    public void renderDocument(Document doc, String ns, Rectangle2D pos) {
        RendererContext context;
        context = new RendererContext(MIME_TYPE);
        context.setUserAgent(userAgent);

        context.setProperty(PDFXMLHandler.PDF_DOCUMENT, pdfDoc);
        context.setProperty(PDFXMLHandler.OUTPUT_STREAM, ostream);
        context.setProperty(PDFXMLHandler.PDF_STATE, currentState);
        context.setProperty(PDFXMLHandler.PDF_PAGE, currentPage);
        context.setProperty(PDFXMLHandler.PDF_CONTEXT,
                    currentContext == null ? currentPage : currentContext);
        context.setProperty(PDFXMLHandler.PDF_CONTEXT, currentContext);
        context.setProperty(PDFXMLHandler.PDF_STREAM, currentStream);
        context.setProperty(PDFXMLHandler.PDF_XPOS,
                            new Integer(currentBlockIPPosition + (int) pos.getX()));
        context.setProperty(PDFXMLHandler.PDF_YPOS,
                            new Integer(currentBPPosition + (int) pos.getY()));
        context.setProperty(PDFXMLHandler.PDF_FONT_INFO, fontInfo);
        context.setProperty(PDFXMLHandler.PDF_FONT_NAME, currentFontName);
        context.setProperty(PDFXMLHandler.PDF_FONT_SIZE,
                            new Integer(currentFontSize));
        context.setProperty(PDFXMLHandler.PDF_WIDTH,
                            new Integer((int) pos.getWidth()));
        context.setProperty(PDFXMLHandler.PDF_HEIGHT,
                            new Integer((int) pos.getHeight()));
        renderXML(userAgent, context, doc, ns);

    }

    /**
     * Render an inline viewport.
     * This renders an inline viewport by clipping if necessary.
     * @param viewport the viewport to handle
     */
    public void renderViewport(Viewport viewport) {
        closeText();

        float x = currentBlockIPPosition / 1000f;
        float y = (currentBPPosition + viewport.getOffset()) / 1000f;
        float width = viewport.getWidth() / 1000f;
        float height = viewport.getHeight() / 1000f;
        drawBackAndBorders(viewport, x, y, width, height);

        endTextObject();

        if (viewport.getClip()) {
            saveGraphicsState();

            clip(x, y, width, height);
        }
        super.renderViewport(viewport);

        if (viewport.getClip()) {
            restoreGraphicsState();
        }
        beginTextObject();
    }

    /**
     * Render leader area.
     * This renders a leader area which is an area with a rule.
     * @param area the leader area to render
     */
    public void renderLeader(Leader area) {
        closeText();
        endTextObject();
        saveGraphicsState();
        int style = area.getRuleStyle();
        boolean alt = false;
        switch(style) {
            case RuleStyle.SOLID:
                currentStream.add("[] 0 d\n");
            break;
            case RuleStyle.DOTTED:
                currentStream.add("[2] 0 d\n");
            break;
            case RuleStyle.DASHED:
                currentStream.add("[6 4] 0 d\n");
            break;
            case RuleStyle.DOUBLE:
            case RuleStyle.GROOVE:
            case RuleStyle.RIDGE:
                alt = true;
            break;
        }
        float startx = ((float) currentBlockIPPosition) / 1000f;
        float starty = ((currentBPPosition + area.getOffset()) / 1000f);
        float endx = (currentBlockIPPosition + area.getWidth()) / 1000f;
        if (!alt) {
            currentStream.add(area.getRuleThickness() / 1000f + " w\n");
            drawLine(startx, starty, endx, starty);
        } else {
            if (style == RuleStyle.DOUBLE) {
                float third = area.getRuleThickness() / 3000f;
                currentStream.add(third + " w\n");
                drawLine(startx, starty, endx, starty);

                drawLine(startx, (starty + 2 * third), endx, (starty + 2 * third));
            } else {
                float half = area.getRuleThickness() / 2000f;

                currentStream.add("1 g\n");
                currentStream.add(startx + " " + starty + " m\n");
                currentStream.add(endx + " " + starty + " l\n");
                currentStream.add(endx + " " + (starty + 2 * half) + " l\n");
                currentStream.add(startx + " " + (starty + 2 * half) + " l\n");
                currentStream.add("h\n");
                currentStream.add("f\n");
                if (style == RuleStyle.GROOVE) {
                    currentStream.add("0 g\n");
                    currentStream.add(startx + " " + starty + " m\n");
                    currentStream.add(endx + " " + starty + " l\n");
                    currentStream.add(endx + " " + (starty + half) + " l\n");
                    currentStream.add((startx + half) + " " + (starty + half) + " l\n");
                    currentStream.add(startx + " " + (starty + 2 * half) + " l\n");
                } else {
                    currentStream.add("0 g\n");
                    currentStream.add(endx + " " + starty + " m\n");
                    currentStream.add(endx + " " + (starty + 2 * half) + " l\n");
                    currentStream.add(startx + " " + (starty + 2 * half) + " l\n");
                    currentStream.add(startx + " " + (starty + half) + " l\n");
                    currentStream.add((endx - half) + " " + (starty + half) + " l\n");
                }
                currentStream.add("h\n");
                currentStream.add("f\n");
            }

        }

        restoreGraphicsState();
        beginTextObject();
        super.renderLeader(area);
    }
}

