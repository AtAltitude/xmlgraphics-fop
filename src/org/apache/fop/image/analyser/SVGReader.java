/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.image.analyser;

// Java
import java.io.BufferedInputStream;
import java.io.IOException;

import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGSVGElement;

// FOP
import org.apache.fop.image.SVGImage;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;

import org.apache.batik.dom.svg.*;
import org.w3c.dom.*;
import org.w3c.dom.svg.*;
import org.w3c.dom.svg.SVGLength;
import org.apache.batik.bridge.*;
import org.apache.batik.swing.svg.*;
import org.apache.batik.swing.gvt.*;
import org.apache.batik.gvt.*;
import org.apache.batik.gvt.renderer.*;
import org.apache.batik.gvt.filter.*;
import org.apache.batik.gvt.event.*;

import org.w3c.dom.DOMImplementation;
import org.apache.batik.dom.svg.SVGDOMImplementation;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.AffineTransform;
import java.awt.Point;
import java.awt.geom.Dimension2D;
import java.awt.Dimension;

import org.apache.fop.image.FopImage;
import org.apache.fop.fo.FOUserAgent;
import org.apache.fop.svg.SVGUserAgent;

/**
 * ImageReader object for SVG document image type.
 */
public class SVGReader implements ImageReader {
    public static final String SVG_MIME_TYPE = "image/svg+xml";
    boolean batik = true;

    public SVGReader() {
    }

    public FopImage.ImageInfo verifySignature(String uri, BufferedInputStream fis,
                                   FOUserAgent ua) throws IOException {
        return loadImage(uri, fis, ua);
    }

    public String getMimeType() {
        return SVG_MIME_TYPE;
    }

    /**
     * This means the external svg document will be loaded twice.
     * Possibly need a slightly different design for the image stuff.
     */
    protected FopImage.ImageInfo loadImage(String uri, BufferedInputStream fis,
                                   FOUserAgent ua) {
        if(batik) {
            try {
                Loader loader = new Loader();
                return loader.getImage(uri, fis, ua);
            } catch (NoClassDefFoundError e) {
                batik = false;
                //ua.getLogger().error("Batik not in class path", e);
                return null;
            }
        }
        return null;
    }

    /**
     * This method is put in another class so that the classloader
     * does not attempt to load batik related classes when constructing
     * the SVGReader class.
     */
    class Loader {
        private FopImage.ImageInfo getImage(String uri, BufferedInputStream fis,
                                   FOUserAgent ua) {
            // parse document and get the size attributes of the svg element

            try {
                FopImage.ImageInfo info = new FopImage.ImageInfo();
                info.mimeType = getMimeType();

                int length = fis.available();
                fis.mark(length);
                SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(
                                                  SVGImage.getParserName());
                SVGDocument doc = factory.createDocument(uri, fis);
                info.data = doc;

                Element e = ((SVGDocument) doc).getRootElement();
                String s;
                SVGUserAgent userAg =
                  new SVGUserAgent(new AffineTransform());
                userAg.setLogger(ua.getLogger());
                BridgeContext ctx = new BridgeContext(userAg);
                UnitProcessor.Context uctx =
                  UnitProcessor.createContext(ctx, e);

                // 'width' attribute - default is 100%
                s = e.getAttributeNS(null,
                                     SVGOMDocument.SVG_WIDTH_ATTRIBUTE);
                if (s.length() == 0) {
                    s = SVGOMDocument.SVG_SVG_WIDTH_DEFAULT_VALUE;
                }
                info.width = (int) UnitProcessor.svgHorizontalLengthToUserSpace (
                          s, SVGOMDocument.SVG_WIDTH_ATTRIBUTE, uctx);

                // 'height' attribute - default is 100%
                s = e.getAttributeNS(null,
                                     SVGOMDocument.SVG_HEIGHT_ATTRIBUTE);
                if (s.length() == 0) {
                    s = SVGOMDocument.SVG_SVG_HEIGHT_DEFAULT_VALUE;
                }
                info.height = (int) UnitProcessor.svgVerticalLengthToUserSpace (
                           s, SVGOMDocument.SVG_HEIGHT_ATTRIBUTE, uctx);

                return info;
            } catch (NoClassDefFoundError ncdfe) {
                batik = false;
                //ua.getLogger().error("Batik not in class path", ncdfe);
                return null;
            }
            catch (Exception e) {
                // If the svg is invalid then it throws an IOException
                // so there is no way of knowing if it is an svg document

                // ua.getLogger().error("Could not load external SVG: " +
                //                       e.getMessage(), e);
                // assuming any exception means this document is not svg
                // or could not be loaded for some reason
                try {
                    fis.reset();
                } catch (IOException ioe) { }
                return null;
            }
        }
    }

}

