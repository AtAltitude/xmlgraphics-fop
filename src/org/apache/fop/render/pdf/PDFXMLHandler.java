/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.render.pdf;

import org.apache.fop.fo.FOUserAgent;
import org.apache.fop.render.XMLHandler;
import org.apache.fop.render.RendererContext;
import org.apache.fop.pdf.*;
import org.apache.fop.svg.*;
import org.apache.fop.svg.SVGUserAgent;
import org.apache.fop.layout.FontInfo;

import org.apache.batik.dom.util.DOMUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.batik.bridge.*;
import org.apache.batik.swing.svg.*;
import org.apache.batik.swing.gvt.*;
import org.apache.batik.gvt.*;
import org.apache.batik.gvt.renderer.*;
import org.apache.batik.gvt.filter.*;
import org.apache.batik.gvt.event.*;

import org.w3c.dom.*;
import org.w3c.dom.svg.*;
import org.w3c.dom.css.*;
import org.w3c.dom.svg.SVGLength;

import java.awt.geom.AffineTransform;

/**
 */
public class PDFXMLHandler implements XMLHandler {
public static final String PDF_DOCUMENT = "pdfDoc";
public static final String OUTPUT_STREAM = "outputStream";
public static final String PDF_STATE = "pdfState";
public static final String PDF_PAGE = "pdfPage";
public static final String PDF_STREAM = "pdfStream";
public static final String PDF_WIDTH = "width";
public static final String PDF_HEIGHT = "height";
public static final String PDF_FONT_INFO = "fontInfo";
public static final String PDF_FONT_NAME = "fontName";
public static final String PDF_FONT_SIZE = "fontSize";
public static final String PDF_XPOS = "xpos";
public static final String PDF_YPOS = "ypos";

    public PDFXMLHandler() {
    }

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

    public static PDFInfo getPDFInfo(RendererContext context) {
        PDFInfo pdfi = new PDFInfo();
        pdfi.pdfDoc = (PDFDocument)context.getProperty(PDF_DOCUMENT);
        pdfi.outputStream = (OutputStream)context.getProperty(OUTPUT_STREAM);
        pdfi.pdfState = (PDFState)context.getProperty(PDF_STATE);
        pdfi.pdfPage = (PDFPage)context.getProperty(PDF_PAGE);
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

    public static class PDFInfo {
        PDFDocument pdfDoc;
        OutputStream outputStream;
        PDFState pdfState;
        PDFPage pdfPage;
        public PDFStream currentStream;
        int width;
        int height;
        FontInfo fi;
        String currentFontName;
        int currentFontSize;
        int currentXPosition;
        int currentYPosition;
    }

    /**
     * This method is placed in an inner class so that we don't get class
     * loading errors if batik is not present.
     */
    protected class SVGHandler {
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
            if(!at.isIdentity()) {
                double[] vals = new double[6];
                at.getMatrix(vals);
                pdfInfo.currentStream.add(PDFNumber.doubleOut(vals[0]) + " "
                                + PDFNumber.doubleOut(vals[1]) + " "
                                + PDFNumber.doubleOut(vals[2]) + " "
                                + PDFNumber.doubleOut(vals[3]) + " "
                                + PDFNumber.doubleOut(vals[4]) + " "
                                + PDFNumber.doubleOut(vals[5]) + " cm\n");
            }

            PDFGraphics2D graphics = new PDFGraphics2D(true, pdfInfo.fi, pdfInfo.pdfDoc,
                                     pdfInfo.pdfPage, pdfInfo.pdfPage.referencePDF(), pdfInfo.currentFontName,
                                     pdfInfo.currentFontSize,
                                     pdfInfo.currentXPosition,
                                     pdfInfo.currentYPosition);
            graphics.setGraphicContext(new org.apache.batik.ext.awt.g2d.GraphicContext());
            pdfInfo.pdfState.push();
            transform = new AffineTransform();
            // TODO scale to viewbox
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

