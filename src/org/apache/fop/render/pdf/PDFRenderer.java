/*
 * $Id$
 * Copyright (C) 2001-2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.render.pdf;

// FOP
import org.apache.fop.render.PrintRenderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.fo.FOUserAgent;
import org.apache.fop.image.FopImage;
import org.apache.fop.image.XMLImage;
import org.apache.fop.image.ImageFactory;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Version;
import org.apache.fop.fo.properties.RuleStyle;
import org.apache.fop.pdf.PDFStream;
import org.apache.fop.pdf.PDFDocument; 
import org.apache.fop.pdf.PDFInfo;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.pdf.PDFXObject;      
import org.apache.fop.pdf.PDFPage;        
import org.apache.fop.pdf.PDFState;
import org.apache.fop.pdf.PDFLink;
import org.apache.fop.pdf.PDFOutline;
import org.apache.fop.pdf.PDFAnnotList;
import org.apache.fop.pdf.PDFColor;
import org.apache.fop.extensions.BookmarkData;

import org.apache.fop.area.Trait;
import org.apache.fop.area.TreeExt;
import org.apache.fop.area.CTM; 
import org.apache.fop.area.Title;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Page;
import org.apache.fop.area.RegionReference;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.inline.Character;
import org.apache.fop.area.inline.Word;
import org.apache.fop.area.inline.Viewport;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.layout.FontState;
import org.apache.fop.layout.FontMetric;
import org.apache.fop.traits.BorderProps;

import org.w3c.dom.Document;

// Java
import java.io.IOException;
import java.io.OutputStream;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.List;

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
    protected HashMap pages = null;

    /**
     * Page references are stored using the PageViewport as the key
     * when a reference is made the PageViewport is used
     * for pdf this means we need the pdf page reference
     */
    protected HashMap pageReferences = new HashMap();

    private String producer;

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
    protected PDFAnnotList currentAnnotList;

    /**
     * the current page to add annotations to
     */
    protected PDFPage currentPage;

    // drawing state
    protected PDFState currentState = null;

    protected PDFColor currentColor;
    protected String currentFontName = "";
    protected int currentFontSize = 0;
    protected int pageHeight;

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
     * create the PDF renderer
     */
    public PDFRenderer() {
    }

    /**
     * set the PDF document's producer
     *
     * @param producer string indicating application producing PDF
     */
    public void setProducer(String prod) {
        producer = prod;
    }

    public void setUserAgent(FOUserAgent agent) {
        super.setUserAgent(agent);
        PDFXMLHandler xmlHandler = new PDFXMLHandler();
        //userAgent.setDefaultXMLHandler(MIME_TYPE, xmlHandler);
        String svg = "http://www.w3.org/2000/svg";
        userAgent.addXMLHandler(MIME_TYPE, svg, xmlHandler);
    }

    public void startRenderer(OutputStream stream) throws IOException {
        ostream = stream;
        this.pdfDoc = new PDFDocument(Version.getVersion());
        this.pdfDoc.setProducer(producer);
        pdfDoc.outputHeader(stream);
    }

    public void stopRenderer() throws IOException {
        FontSetup.addToResources(pdfDoc, pdfDoc.getResources(), fontInfo);
        pdfDoc.outputTrailer(ostream);

        this.pdfDoc = null;
        ostream = null;
    }

    public boolean supportsOutOfOrder() {
        return true;
    }

    public void renderExtension(TreeExt ext) {
        // render bookmark extension
        if (ext instanceof BookmarkData) {
            renderRootExtensions((BookmarkData)ext);
        }
    }

    protected void renderRootExtensions(BookmarkData bookmarks) {
        for (int i = 0; i < bookmarks.getCount(); i++) {
            BookmarkData ext = bookmarks.getSubData(i);
            renderOutline(ext, null);
        }
    }

    private void renderOutline(BookmarkData outline, PDFOutline parentOutline) {
        PDFOutline outlineRoot = pdfDoc.getOutlineRoot();
        PDFOutline pdfOutline = null;
        String intDest = (String)pageReferences.get(outline.getPage());
        if (parentOutline == null) {
            pdfOutline = pdfDoc.makeOutline(outlineRoot,
                                    outline.getLabel(), intDest);
        } else {
            PDFOutline pdfParentOutline = parentOutline;
            pdfOutline = pdfDoc.makeOutline(pdfParentOutline,
                                    outline.getLabel(), intDest);
        }

        for (int i = 0; i < outline.getCount(); i++) {
            renderOutline(outline.getSubData(i), pdfOutline);
        }
    }

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
     */
    public void preparePage(PageViewport page) {
        this.pdfResources = this.pdfDoc.getResources();

        Rectangle2D bounds = page.getViewArea();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        currentPage = this.pdfDoc.makePage(this.pdfResources,
                                           (int) Math.round(w / 1000), (int) Math.round(h / 1000));
        if (pages == null) {
            pages = new HashMap();
        }
        pages.put(page, currentPage);
        pageReferences.put(page, currentPage.referencePDF());
    }

    /**
     * This method creates a pdf stream for the current page
     * uses it as the contents of a new page. The page is written
     * immediately to the output stream.
     */
    public void renderPage(PageViewport page) throws IOException,
    FOPException {
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
            currentPage = this.pdfDoc.makePage(this.pdfResources,
                              (int) Math.round(w / 1000), (int) Math.round(h / 1000));
            pageReferences.put(page, currentPage.referencePDF());
        }
        currentStream =
          this.pdfDoc.makeStream(PDFStream.CONTENT_FILTER, false);

        currentState = new PDFState();
        currentState.setTransform(new AffineTransform(1, 0, 0, -1, 0,
                                   (int) Math.round(pageHeight / 1000)));
        // Transform origin at top left to origin at bottom left
        currentStream.add("1 0 0 -1 0 "
                           + (int) Math.round(pageHeight / 1000) + " cm\n");
        //currentStream.add("BT\n");
        currentFontName = "";

        Page p = page.getPage();
        renderPageAreas(p);

        //currentStream.add("ET\n");

        this.pdfDoc.addStream(currentStream);
        currentPage.setContents(currentStream);
        this.pdfDoc.addPage(currentPage);
        this.pdfDoc.output(ostream);
    }


    protected void startVParea(CTM ctm) {
        // Set the given CTM in the graphics state
        currentState.push();
        currentState.setTransform(
          new AffineTransform(CTMHelper.toPDFArray(ctm)));

        currentStream.add("q\n");
        // multiply with current CTM
        currentStream.add(CTMHelper.toPDFString(ctm) + " cm\n");
        // Set clip?
        currentStream.add("BT\n");
    }

    protected void endVParea() {
        currentStream.add("ET\n");
        currentStream.add("Q\n");
        currentState.pop();
    }

    protected void renderRegion(RegionReference region) {
        // Draw a rectangle so we can see it!
        // x=0,y=0,w=ipd,h=bpd
        currentFontName = "";
        super.renderRegion(region);
    }

    protected void handleBlockTraits(Block block) {
        // draw border and background
        BorderProps bps = (BorderProps)block.getTrait(Trait.BORDER_BEFORE);
        if(bps != null) {
            float startx = ((float) currentBlockIPPosition) / 1000f;
            float starty = (currentBPPosition / 1000f);
            float endx = (currentBlockIPPosition + block.getWidth()) / 1000f;

            currentStream.add("ET\n");
            currentStream.add("q\n");

            currentStream.add(bps.width / 1000f + " w\n");

            currentStream.add(startx + " " + starty + " m\n");
            currentStream.add(endx + " " + starty + " l\n");
            currentStream.add("S\n");

            currentStream.add("Q\n");
            currentStream.add("BT\n");
        }
        bps = (BorderProps)block.getTrait(Trait.BORDER_START);
        if(bps != null) {
        }
        bps = (BorderProps)block.getTrait(Trait.BORDER_AFTER);
        if(bps != null) {
        }
        bps = (BorderProps)block.getTrait(Trait.BORDER_END);
        if(bps != null) {
        }
    }

    protected void renderBlockViewport(BlockViewport bv, List children) {
        // clip and position viewport if necessary

        // save positions
        int saveIP = currentIPPosition;
        int saveBP = currentBPPosition;
        String saveFontName = currentFontName;

        CTM ctm = bv.getCTM();

        closeText();

        if (bv.getPositioning() == Block.ABSOLUTE) {

            currentIPPosition = 0;
            currentBPPosition = 0;

            currentStream.add("ET\n");

            if (bv.getClip()) {
                currentStream.add("q\n");
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
            renderBlocks(children);
            endVParea();

            if (bv.getClip()) {
                currentStream.add("Q\n");
            }
            currentStream.add("BT\n");

            // clip if necessary

            currentIPPosition = saveIP;
            currentBPPosition = saveBP;
        } else {

            if (ctm != null) {
                currentIPPosition = 0;
                currentBPPosition = 0;

                currentStream.add("ET\n");

                double[] vals = ctm.toArray();
                boolean aclock = vals[2] == 1.0;
                if (vals[2] == 1.0) {
                    ctm = ctm.translate(-saveBP - bv.getHeight(), -saveIP);
                } else if (vals[0] == -1.0) {
                    ctm = ctm.translate(-saveIP - bv.getWidth(), -saveBP - bv.getHeight());
                } else {
                    ctm = ctm.translate(saveBP, saveIP - bv.getWidth());
                }
            }

            if (bv.getClip()) {
                currentStream.add("q\n"); 
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
            renderBlocks(children);
            if (ctm != null) {
                endVParea();
            }
        
            if (bv.getClip()) {
                currentStream.add("Q\n");
            }
            if (ctm != null) {
                currentStream.add("BT\n");
            }

            // clip if necessary

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
        Object tr = ip.getTrait(Trait.INTERNAL_LINK);
        boolean internal = false;
        String dest = null;
        if (tr == null) {
            dest = (String)ip.getTrait(Trait.EXTERNAL_LINK);
        } else {
            PageViewport pv = (PageViewport)tr;
            dest = (String)pageReferences.get(pv);
            internal = true;
        }
        if (dest != null) {
            float start = currentBlockIPPosition;
            float top = (ip.getOffset() + currentBPPosition) / 1000f;
            float height = ip.getHeight() / 1000f;
            super.renderInlineParent(ip);
            float width = (currentBlockIPPosition - start) / 1000f;
            start = start / 1000f;
            // add link to pdf document
            Rectangle2D rect = new Rectangle2D.Float(start, top, width, height);
            // transform rect to absolute coords
            AffineTransform transform = currentState.getTransform();
            rect = transform.createTransformedShape(rect).getBounds();

            int type = internal ? PDFLink.INTERNAL : PDFLink.EXTERNAL;
            PDFLink pdflink = pdfDoc.makeLink(rect, dest, type);
            currentPage.addAnnotation(pdflink);
        } else {
            super.renderInlineParent(ip);
        }
    }

    public void renderCharacter(Character ch) {

        super.renderCharacter(ch);
    }

    public void renderWord(Word word) {
        StringBuffer pdf = new StringBuffer();

        String name = (String) word.getTrait(Trait.FONT_NAME);
        int size = ((Integer) word.getTrait(Trait.FONT_SIZE)).intValue();

        // This assumes that *all* CIDFonts use a /ToUnicode mapping
        Font f = (Font) fontInfo.getFonts().get(name);
        boolean useMultiByte = f.isMultiByte();

        // String startText = useMultiByte ? "<FEFF" : "(";
        String startText = useMultiByte ? "<" : "(";
        String endText = useMultiByte ? "> " : ") ";

        updateFont(name, size, pdf);
        updateColor(true, pdf);

        int rx = currentBlockIPPosition;
        // int bl = pageHeight - currentBPPosition;
        int bl = currentBPPosition + word.getOffset();

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
        prevWordWidth = word.getWidth();
        prevWordX = rx;

        String s = word.getWord();

        FontMetric metrics = fontInfo.getMetricsFor(name);
        FontState fs = new FontState(name, metrics, size);
        escapeText(s, fs, useMultiByte, pdf);
        pdf.append(endText);

        currentStream.add(pdf.toString());

        super.renderWord(word);
    }

    public void escapeText(String s, FontState fs,
                           boolean useMultiByte, StringBuffer pdf) {
        String startText = useMultiByte ? "<" : "(";
        String endText = useMultiByte ? "> " : ") ";

        boolean kerningAvailable = false;
        HashMap kerning = null;
        kerning = fs.getKerning();
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
                pdf.append(getUnicodeString(ch));
            }

            if (kerningAvailable && (i + 1) < l) {
                addKerning(pdf, (new Integer((int) ch)),
                           (new Integer((int) fs.mapChar(s.charAt(i + 1)))
                           ), kerning, startText, endText);
            }
        }
    }

    /**
     * Convert a char to a multibyte hex representation
     */
    private String getUnicodeString(char c) {
        StringBuffer buf = new StringBuffer(4);

        byte[] uniBytes = null;
        try {
            char[] a = {c};
            uniBytes = new String(a).getBytes("UnicodeBigUnmarked");
        } catch (java.io.UnsupportedEncodingException e) {
            // This should never fail
        }

        for (int i = 0; i < uniBytes.length; i++) {
            int b = (uniBytes[i] < 0) ? (int)(256 + uniBytes[i])
                        : (int) uniBytes[i];

            String hexString = Integer.toHexString(b);
            if (hexString.length() == 1) {
                buf = buf.append("0" + hexString);
            } else {
                buf = buf.append(hexString);
            }
        }
        return buf.toString();
    }

    private void addKerning(StringBuffer buf, Integer ch1, Integer ch2,
                            HashMap kerning, String startText, String endText) {
        HashMap kernPair = (HashMap) kerning.get(ch1);

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
    private void closeText() {
        if (textOpen) {
            currentStream.add("] TJ\n");
            textOpen = false;
            prevWordX = 0;
            prevWordY = 0;
        }
    }

    private void updateColor(boolean fill, StringBuffer pdf) {
        /*PDFColor areaColor = null;
         if (this.currentFill instanceof PDFColor) {
             areaColor = (PDFColor)this.currentFill;
         }

         if (areaColor == null || areaColor.red() != (double)area.getRed()
                 || areaColor.green() != (double)area.getGreen()
                 || areaColor.blue() != (double)area.getBlue()) {

             areaColor = new PDFColor((double)area.getRed(),
                                      (double)area.getGreen(),
                                      (double)area.getBlue());

             closeText();
             this.currentFill = areaColor;
             pdf.append(this.currentFill.getColorSpaceOut(true));
         }*/
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

    public void renderImage(Image image, Rectangle2D pos) {
        String url = image.getURL();

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
        if (!fopimage.load(FopImage.DIMENSIONS, userAgent)) {
            return;
        }
        String mime = fopimage.getMimeType();
        if ("text/xml".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA, userAgent)) {
                return;
            }
            Document doc = ((XMLImage) fopimage).getDocument();
            String ns = ((XMLImage) fopimage).getNameSpace();

            renderDocument(doc, ns, pos);
        } else if ("image/svg+xml".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA, userAgent)) {
                return;
            }
            Document doc = ((XMLImage) fopimage).getDocument();
            String ns = ((XMLImage) fopimage).getNameSpace();

            renderDocument(doc, ns, pos);
        } else if ("image/eps".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA, userAgent)) {
                return;
            }
            FopPDFImage pdfimage = new FopPDFImage(fopimage, url);
            int xobj = pdfDoc.addImage(null, pdfimage).getXNumber();
            fact.releaseImage(url, userAgent);
        } else if ("image/jpeg".equals(mime)) {
            if (!fopimage.load(FopImage.ORIGINAL_DATA, userAgent)) {
                return;
            }
            FopPDFImage pdfimage = new FopPDFImage(fopimage, url);
            int xobj = pdfDoc.addImage(null, pdfimage).getXNumber();
            fact.releaseImage(url, userAgent);

            int w = (int) pos.getWidth() / 1000;
            int h = (int) pos.getHeight() / 1000;
            placeImage((int) pos.getX() / 1000,
                       (int) pos.getY() / 1000, w, h, xobj);
        } else {
            if (!fopimage.load(FopImage.BITMAP, userAgent)) {
                return;
            }
            FopPDFImage pdfimage = new FopPDFImage(fopimage, url);
            int xobj = pdfDoc.addImage(null, pdfimage).getXNumber();
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

    protected void placeImage(int x, int y, int w, int h, int xobj) {
        currentStream.add("q\n" + ((float) w) + " 0 0 "
                          + ((float) -h) + " "
                          + (((float) currentBlockIPPosition) / 1000f + x) + " "
                          + (((float)(currentBPPosition + 1000 * h)) / 1000f
                          + y) + " cm\n" + "/Im" + xobj + " Do\nQ\n");

    }

    public void renderForeignObject(ForeignObject fo, Rectangle2D pos) {
        Document doc = fo.getDocument();
        String ns = fo.getNameSpace();
        renderDocument(doc, ns, pos);
    }

    public void renderDocument(Document doc, String ns, Rectangle2D pos) {
        RendererContext context;
        context = new RendererContext(MIME_TYPE);
        context.setUserAgent(userAgent);

        context.setProperty(PDFXMLHandler.PDF_DOCUMENT, pdfDoc);
        context.setProperty(PDFXMLHandler.OUTPUT_STREAM, ostream);
        context.setProperty(PDFXMLHandler.PDF_STATE, currentState);
        context.setProperty(PDFXMLHandler.PDF_PAGE, currentPage);
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
        userAgent.renderXML(context, doc, ns);

    }

    /**
     * Render an inline viewport.
     * This renders an inline viewport by clipping if necessary.
     * @param viewport the viewport to handle
     */
    public void renderViewport(Viewport viewport) {
        closeText();
        currentStream.add("ET\n");
        if (viewport.getClip()) {
            currentStream.add("q\n");

            float x = currentBlockIPPosition / 1000f;
            float y = (currentBPPosition + viewport.getOffset()) / 1000f;
            float width = viewport.getWidth() / 1000f;
            float height = viewport.getHeight() / 1000f;
            clip(x, y, width, height);
        }
        super.renderViewport(viewport);

        if (viewport.getClip()) {
            currentStream.add("Q\n");
        }
        currentStream.add("BT\n");
    }

    /**
     * Render leader area.
     * This renders a leader area which is an area with a rule.
     * @param area the leader area to render
     */
    public void renderLeader(Leader area) {
        closeText();
        currentStream.add("ET\n");
        currentStream.add("q\n");
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

            currentStream.add(startx + " " + starty + " m\n");
            currentStream.add(endx + " " + starty + " l\n");
            currentStream.add("S\n");
        } else {
            if (style == RuleStyle.DOUBLE) {
                float third = area.getRuleThickness() / 3000f;
                currentStream.add(third + " w\n");
                currentStream.add(startx + " " + starty + " m\n");
                currentStream.add(endx + " " + starty + " l\n");
                currentStream.add("S\n");

                currentStream.add(startx + " " + (starty + 2 * third) + " m\n");
                currentStream.add(endx + " " + (starty + 2 * third) + " l\n");
                currentStream.add("S\n");
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

        currentStream.add("Q\n");
        currentStream.add("BT\n");
        super.renderLeader(area);
    }
}

