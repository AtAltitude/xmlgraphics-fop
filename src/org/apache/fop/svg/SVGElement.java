/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.svg;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.Area;
import org.apache.fop.layout.FontState;
import org.apache.fop.apps.FOPException;
import org.apache.fop.layout.inline.*;
import org.apache.fop.configuration.Configuration;

import org.apache.batik.dom.svg.*;
import org.apache.batik.dom.util.XMLSupport;
import org.w3c.dom.*;
import org.w3c.dom.svg.*;
import org.w3c.dom.svg.SVGLength;
import org.xml.sax.Attributes;
import org.apache.batik.bridge.*;
import org.apache.batik.swing.svg.*;
import org.apache.batik.swing.gvt.*;
import org.apache.batik.gvt.*;
import org.apache.batik.gvt.renderer.*;
import org.apache.batik.gvt.filter.*;
import org.apache.batik.gvt.event.*;
import org.apache.batik.bridge.UnitProcessor;
import org.apache.batik.css.value.ImmutableFloat;
import org.apache.batik.css.CSSOMReadOnlyValue;
import org.apache.batik.util.SVGConstants;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.css.CSSPrimitiveValue;

import org.apache.batik.dom.svg.SVGDOMImplementation;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * class representing svg:svg pseudo flow object.
 */
public class SVGElement extends SVGObj {

    FontState fs;

    /**
     * constructs an SVG object (called by Maker).
     *
     * @param parent the parent formatting object
     * @param propertyList the explicit properties of this object
     */
    public SVGElement(FONode parent) {
        super(parent);
    }

    public void handleAttrs(Attributes attlist) throws FOPException {
        super.handleAttrs(attlist);
        init();
    }

    /**
     * layout this formatting object.
     *
     * @param area the area to layout the object into
     *
     * @return the status of the layout
     */
    public Status layout(final Area area) throws FOPException {

        if (!(area instanceof ForeignObjectArea)) {
            // this is an error
            throw new FOPException("SVG not in fo:instream-foreign-object");
        }

            this.fs = area.getFontState();

        final Element svgRoot = element;
        /* create an SVG area */
        /* if width and height are zero, get the bounds of the content. */
        DefaultSVGContext dc = new DefaultSVGContext() {
            public float getPixelToMM() {
                // 72 dpi
                return 0.35277777777777777778f;
            }

            public float getViewportWidth(Element e) throws IllegalStateException {
                if(e == svgRoot) {
                    ForeignObjectArea foa = (ForeignObjectArea)area;
                    if(!foa.isContentWidthAuto()) {
                        return foa.getContentWidth();
                    }
                }
                return super.getViewportWidth(e);
            }

            public float getViewportHeight(Element e) throws IllegalStateException {
                if(e == svgRoot) {
                    ForeignObjectArea foa = (ForeignObjectArea)area;
                    if(!foa.isContentHeightAuto()) {
                        return foa.getContentHeight();
                    }
                }
                return super.getViewportHeight(e);
            }

            public List getDefaultFontFamilyValue() {
                return FONT_FAMILY;
            }
        };
        ((SVGOMDocument)doc).setSVGContext(dc);

        try {
            String baseDir = Configuration.getStringValue("baseDir");
            if(baseDir != null) {
                ((SVGOMDocument)doc).setURLObject(new URL(baseDir));
            }
        } catch (Exception e) {
            log.error("Could not set base URL for svg", e);
        }

        Element e = ((SVGDocument)doc).getRootElement();

        //if(!e.hasAttributeNS(XMLSupport.XMLNS_NAMESPACE_URI, "xmlns")) {
            e.setAttributeNS(XMLSupport.XMLNS_NAMESPACE_URI, "xmlns", SVGDOMImplementation.SVG_NAMESPACE_URI);
        //}

        Point2D p2d = getSize(this.fs, svgRoot);

        SVGArea svg = new SVGArea(fs, (float)p2d.getX(),
                                  (float)p2d.getY());
        svg.setSVGDocument(doc);
        svg.start();

        /* finish off the SVG area */
        svg.end();

        /* add the SVG area to the containing area */
        ForeignObjectArea foa = (ForeignObjectArea)area;
        foa.setObject(svg);
        foa.setIntrinsicWidth(svg.getWidth());
        foa.setIntrinsicHeight(svg.getHeight());

        /* return status */
        return new Status(Status.OK);
    }

    private void init() {
        DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        doc = impl.createDocument(svgNS, "svg", null);

        element = doc.getDocumentElement();

        buildTopLevel(doc, element);
    }

    public final static List FONT_FAMILY;
    static {
        FONT_FAMILY = new ArrayList();
        FONT_FAMILY.add("Helvetica");
        FONT_FAMILY.add("Times");
        FONT_FAMILY.add("Courier");
        FONT_FAMILY.add("sans-serif");
        FONT_FAMILY.add("serif");
    }

    public static Point2D getSize(FontState fs, Element svgRoot) {
        String str;
        UnitProcessor.Context ctx;
        ctx = new PDFUnitContext(fs, svgRoot);
        str = svgRoot.getAttributeNS(null, SVGConstants.SVG_WIDTH_ATTRIBUTE);
        if (str.length() == 0) str = "100%";
        float width = UnitProcessor.svgHorizontalLengthToUserSpace
            (str, SVGConstants.SVG_WIDTH_ATTRIBUTE, ctx); 

        str = svgRoot.getAttributeNS(null, SVGConstants.SVG_HEIGHT_ATTRIBUTE);
        if (str.length() == 0) str = "100%";
        float height = UnitProcessor.svgVerticalLengthToUserSpace
            (str, SVGConstants.SVG_HEIGHT_ATTRIBUTE, ctx);
        return new Point2D.Float(width, height);
    }
    /**
     * This class is the default context for a particular
     * element. Informations not available on the element are get from
     * the bridge context (such as the viewport or the pixel to
     * millimeter factor.
     */
    public static class PDFUnitContext implements UnitProcessor.Context {

        /** The element. */
        protected Element e;
        protected FontState fs;
        public PDFUnitContext(FontState fs, Element e) { 
            this.e  = e;
            this.fs = fs;
        }

        /**
         * Returns the element.
         */
        public Element getElement() {
            return e;
        }

        /**
      * Returns the context of the parent element of this context.
         * Since this is always for the root SVG element there never
         * should be one...
         */
        public UnitProcessor.Context getParentElementContext() {
            return null;
        }

        /**
         * Returns the pixel to mm factor.
         */
        public float getPixelToMM() {
                return 0.264583333333333333333f;
                // 72 dpi
        }

        /**
         * Returns the font-size medium value in pt.
         */
        public float getMediumFontSize() {
            return 9f;
        }

        /**
         * Returns the font-size value.
         */
        public CSSPrimitiveValue getFontSize() {
            return new CSSOMReadOnlyValue
                (new ImmutableFloat(CSSPrimitiveValue.CSS_PT,
                                    fs.getFontSize()));
        }

        /**
         * Returns the x-height value.
         */
        public float getXHeight() {
            return 0.5f;
        }

        /**
         * Returns the viewport width used to compute units.
         */
        public float getViewportWidth() {
            return 100;
        }

        /**
         * Returns the viewport height used to compute units.
         */
        public float getViewportHeight() {
            return 100;
        }
    }
}
