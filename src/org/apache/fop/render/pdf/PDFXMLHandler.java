/*
 * $Id$
 * Copyright (C) 2001-2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.render.pdf;

import org.apache.fop.render.XMLHandler;
import org.apache.fop.render.RendererContext;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFPage;
import org.apache.fop.pdf.PDFState;
import org.apache.fop.pdf.PDFStream;
import org.apache.fop.pdf.PDFNumber;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.svg.PDFTextElementBridge;
import org.apache.fop.svg.PDFAElementBridge;
import org.apache.fop.svg.PDFGraphics2D;
import org.apache.fop.svg.SVGUserAgent;
import org.apache.fop.layout.FontInfo;

import org.w3c.dom.Document;

import java.io.OutputStream;

import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.ViewBox;

import org.apache.batik.gvt.GraphicsNode;

import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGSVGElement;

import java.awt.geom.AffineTransform;

/**
 * PDF XML handler.
 * This handler handles XML for foreign objects when rendering to PDF.
 * It renders SVG to the PDF document using the PDFGraphics2D.
 * The properties from the PDF renderer are subject to change.
 */
public class PDFXMLHandler implements XMLHandler {
    /**
     * The PDF document that is being drawn into.
     */
    public static final String PDF_DOCUMENT = "pdfDoc";

    /**
     * The output stream that the document is being sent to.
     */
    public static final String OUTPUT_STREAM = "outputStream";

    /**
     * The current pdf state.
     */
    public static final String PDF_STATE = "pdfState";

    /**
     * The current PDF page for page renference and as a resource context.
     */
    public static final String PDF_PAGE = "pdfPage";

    /**
     * The current PDF page for page renference and as a resource context.
     */
    public static final String PDF_CONTEXT = "pdfContext";

    /**
     * The current PDF stream to draw directly to.
     */
    public static final String PDF_STREAM = "pdfStream";

    /**
     * The width of the current pdf page.
     */
    public static final String PDF_WIDTH = "width";

    /**
     * The height of the current pdf page.
     */
    public static final String PDF_HEIGHT = "height";

    /**
     * The current font information for the pdf renderer.
     */
    public static final String PDF_FONT_INFO = "fontInfo";

    /**
     * The current pdf font name.
     */
    public static final String PDF_FONT_NAME = "fontName";

    /**
     * The current pdf font size.
     */
    public static final String PDF_FONT_SIZE = "fontSize";

    /**
     * The x position that this is being drawn at.
     */
    public static final String PDF_XPOS = "xpos";

    /**
     * The y position that this is being drawn at.
     */
    public static final String PDF_YPOS = "ypos";

    /**
     * Create a new PDF XML handler for use by the PDF renderer.
     */
    public PDFXMLHandler() {
    }

    /**
     * Handle the XML.
     * This checks the type of XML and handles appropraitely.
     *
     * @param context the renderer context
     * @param doc the XML document to render
     * @param ns the namespace of the XML document
     * @throws Exception any sort of exception could be thrown and shuld be handled
     */
    public void handleXML(RendererContext context, Document doc,
                          String ns) throws Exception {
        PDFInfo pdfi = getPDFInfo(context);

        String svg = "http://www.w3.org/2000/svg";
        if (svg.equals(ns)) {
            SVGHandler svghandler = new SVGHandler();
            svghandler.renderSVGDocument(context, doc, pdfi);
        } else {
        }
    }

    /**
     * Get the pdf information from the render context.
     *
     * @param context the renderer context
     * @return the pdf information retrieved from the context
     */
    public static PDFInfo getPDFInfo(RendererContext context) {
        PDFInfo pdfi = new PDFInfo();
        pdfi.pdfDoc = (PDFDocument)context.getProperty(PDF_DOCUMENT);
        pdfi.outputStream = (OutputStream)context.getProperty(OUTPUT_STREAM);
        pdfi.pdfState = (PDFState)context.getProperty(PDF_STATE);
        pdfi.pdfPage = (PDFPage)context.getProperty(PDF_PAGE);
        pdfi.pdfContext = (PDFResourceContext)context.getProperty(PDF_CONTEXT);
        pdfi.currentStream = (PDFStream)context.getProperty(PDF_STREAM);
        pdfi.width = ((Integer)context.getProperty(PDF_WIDTH)).intValue();
        pdfi.height = ((Integer)context.getProperty(PDF_HEIGHT)).intValue();
        pdfi.fi = (FontInfo)context.getProperty(PDF_FONT_INFO);
        pdfi.currentFontName = (String)context.getProperty(PDF_FONT_NAME);
        pdfi.currentFontSize = ((Integer)context.getProperty(PDF_FONT_SIZE)).intValue();
        pdfi.currentXPosition = ((Integer)context.getProperty(PDF_XPOS)).intValue();
        pdfi.currentYPosition = ((Integer)context.getProperty(PDF_YPOS)).intValue();
        return pdfi;
    }

    /**
     * PDF information structure for drawing the XML document.
     */
    public static class PDFInfo {
        /** see PDF_DOCUMENT */
        public PDFDocument pdfDoc;
        /** see OUTPUT_STREAM */
        public OutputStream outputStream;
        /** see PDF_STATE */
        public PDFState pdfState;
        /** see PDF_PAGE */
        public PDFPage pdfPage;
        /** see PDF_CONTEXT */
        public PDFResourceContext pdfContext;
        /** see PDF_STREAM */
        public PDFStream currentStream;
        /** see PDF_WIDTH */
        public int width;
        /** see PDF_HEIGHT */
        public int height;
        /** see PDF_FONT_INFO */
        public FontInfo fi;
        /** see PDF_FONT_NAME */
        public String currentFontName;
        /** see PDF_FONT_SIZE */
        public int currentFontSize;
        /** see PDF_XPOS */
        public int currentXPosition;
        /** see PDF_YPOS */
        public int currentYPosition;
    }

    /**
     * This method is placed in an inner class so that we don't get class
     * loading errors if batik is not present.
     */
    protected class SVGHandler {
        /**
         * Render the svg document.
         * @param context the renderer context
         * @param doc the svg document
         * @param pdfInfo the pdf information of the current context
         */
        protected void renderSVGDocument(RendererContext context, Document doc, PDFInfo pdfInfo) {
            int xOffset = pdfInfo.currentXPosition;
            int yOffset = pdfInfo.currentYPosition;

            SVGUserAgent ua
                 = new SVGUserAgent(context.getUserAgent(), new AffineTransform());

            GVTBuilder builder = new GVTBuilder();
            BridgeContext ctx = new BridgeContext(ua);
            PDFTextElementBridge tBridge = new PDFTextElementBridge(pdfInfo.fi);
            ctx.putBridge(tBridge);

            PDFAElementBridge aBridge = new PDFAElementBridge();
            // to get the correct transform we need to use the PDFState
            AffineTransform transform = pdfInfo.pdfState.getTransform();
            transform.translate(xOffset / 1000f, yOffset / 1000f);
            aBridge.setCurrentTransform(transform);
            ctx.putBridge(aBridge);

            GraphicsNode root;
            try {
                root = builder.build(ctx, doc);
            } catch (Exception e) {
                context.getUserAgent().getLogger().error("svg graphic could not be built: "
                                       + e.getMessage(), e);
                return;
            }
            // get the 'width' and 'height' attributes of the SVG document
            float w = (float)ctx.getDocumentSize().getWidth() * 1000f;
            float h = (float)ctx.getDocumentSize().getHeight() * 1000f;

            float sx = pdfInfo.width / (float)w;
            float sy = pdfInfo.height / (float)h;

            ctx = null;
            builder = null;

            /*
             * Clip to the svg area.
             * Note: To have the svg overlay (under) a text area then use
             * an fo:block-container
             */
            pdfInfo.currentStream.add("q\n");
            // transform so that the coordinates (0,0) is from the top left
            // and positive is down and to the right. (0,0) is where the
            // viewBox puts it.
            pdfInfo.currentStream.add(sx + " 0 0 " + sy + " " + xOffset / 1000f + " "
                              + yOffset / 1000f + " cm\n");

            SVGSVGElement svg = ((SVGDocument)doc).getRootElement();
            AffineTransform at = ViewBox.getPreserveAspectRatioTransform(svg, w / 1000f, h / 1000f);
            if (!at.isIdentity()) {
                double[] vals = new double[6];
                at.getMatrix(vals);
                pdfInfo.currentStream.add(PDFNumber.doubleOut(vals[0], 5) + " "
                                + PDFNumber.doubleOut(vals[1], 5) + " "
                                + PDFNumber.doubleOut(vals[2], 5) + " "
                                + PDFNumber.doubleOut(vals[3], 5) + " "
                                + PDFNumber.doubleOut(vals[4]) + " "
                                + PDFNumber.doubleOut(vals[5]) + " cm\n");
            }

            if (pdfInfo.pdfContext == null) {
                pdfInfo.pdfContext = pdfInfo.pdfPage;
            }
            PDFGraphics2D graphics = new PDFGraphics2D(true, pdfInfo.fi, pdfInfo.pdfDoc,
                                     pdfInfo.pdfContext, pdfInfo.pdfPage.referencePDF(),
                                     pdfInfo.currentFontName,
                                     pdfInfo.currentFontSize);
            graphics.setGraphicContext(new org.apache.batik.ext.awt.g2d.GraphicContext());
            pdfInfo.pdfState.push();
            transform = new AffineTransform();
            // scale to viewbox
            transform.translate(xOffset / 1000f, yOffset / 1000f);
            pdfInfo.pdfState.setTransform(transform);
            graphics.setPDFState(pdfInfo.pdfState);
            graphics.setOutputStream(pdfInfo.outputStream);
            try {
                root.paint(graphics);
                pdfInfo.currentStream.add(graphics.getString());
            } catch (Exception e) {
                context.getUserAgent().getLogger().error("svg graphic could not be rendered: "
                                       + e.getMessage(), e);
            }

            pdfInfo.currentStream.add("Q\n");
            pdfInfo.pdfState.pop();
        }
    }
}

