/* $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources."
 */

package org.apache.fop.svg;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.Area;
import org.apache.fop.layout.FontState;
import org.apache.fop.apps.FOPException;
import org.apache.fop.layout.inline.*;

import org.apache.batik.dom.svg.*;
import org.w3c.dom.*;
import org.w3c.dom.svg.*;
import org.w3c.dom.svg.SVGLength;

import org.w3c.dom.DOMImplementation;
import org.apache.batik.dom.svg.SVGDOMImplementation;

import java.io.File;

/**
 * class representing svg:svg pseudo flow object.
 */
public class SVGElement extends Svg {

    /**
     * inner class for making SVG objects.
     */
    public static class Maker extends FObj.Maker {

        /**
         * make an SVG object.
         *
         * @param parent the parent formatting object
         * @param propertyList the explicit properties of this object
         *
         * @return the SVG object
         */
        public FObj make(FObj parent,
                         PropertyList propertyList) throws FOPException {
            return new SVGElement(parent, propertyList);
        }
    }

    /**
     * returns the maker for this object.
     *
     * @return the maker for SVG objects
     */
    public static FObj.Maker maker() {
        return new SVGElement.Maker();
    }

    FontState fs;

    /**
     * constructs an SVG object (called by Maker).
     *
     * @param parent the parent formatting object
     * @param propertyList the explicit properties of this object
     */
    public SVGElement(FObj parent, PropertyList propertyList) {
        super(parent, propertyList);
    }

    /**
     * layout this formatting object.
     *
     * @param area the area to layout the object into
     *
     * @return the status of the layout
     */
    public Status layout(Area area) throws FOPException {

        if (!(area instanceof ForeignObjectArea)) {
            // this is an error
            throw new FOPException("SVG not in fo:instream-foreign-object");
        }

        /*if (this.marker == BREAK_AFTER) {
            return new Status(Status.OK);
        }*/

        if (this.marker == START) {
            /* retrieve properties */
            String id = this.properties.get("id").getString();
            String fontFamily =
              this.properties.get("font-family").getString();
            String fontStyle =
              this.properties.get("font-style").getString();
            String fontWeight =
              this.properties.get("font-weight").getString();
            String fontSz = this.properties.get("font-size").getString();
            int fontSize = area.getFontState().getFontSize();
            try {
                fontSize = Integer.parseInt(fontSz);
            } catch (Exception e) {
            }

            // FIX-ME: should get the font-variant property
            this.fs = new FontState(area.getFontInfo(), fontFamily,
                                    fontStyle, fontWeight, fontSize, FontVariant.NORMAL);

            //						this.width = this.properties.get("width").getString();
            //						this.height = this.properties.get("height").getString();

            this.marker = 0;
        }

        /* create an SVG area */
        /* if width and height are zero, may want to get the bounds of the content. */

        DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Document doc = impl.createDocument(svgNS, "svg", null);

        Element svgRoot = doc.getDocumentElement();

        try {
            ((SVGOMDocument)doc).setURLObject(new File(".").toURL());
        } catch (Exception e) {
        }

        DefaultSVGContext dc = new DefaultSVGContext() {
                                   public float getPixelToMM() {
                                       return 0.264583333333333333333f;
                                       // 72 dpi
                                   }
                                   public float getViewportWidth() {
                                       return 100;
                                   }
                                   public float getViewportHeight() {
                                       return 100;
                                   }
                               };
        ((SVGOMDocument)doc).setSVGContext(dc);

        for (int count = 0; count < props.length; count++) {
            if (this.properties.get(props[count]) != null) {
                String rf = this.properties.get(props[count]).getString();
                if (rf != null)
                    svgRoot.setAttributeNS(null, props[count], rf);
            }
        }
        //doc.appendChild(topLevel);
        int numChildren = this.children.size();
        for (int i = 0; i < numChildren; i++) {
            Object child = children.elementAt(i);
            if (child instanceof SVGObj) {
                ((SVGObj) child).addGraphic(doc, svgRoot);
            } else if (child instanceof String) {
                org.w3c.dom.Text text = doc.createTextNode((String) child);
                svgRoot.appendChild(text);
            }
        }

        float width = ((SVGSVGElement) svgRoot).getWidth().getBaseVal().
                      getValue();
        float height = ((SVGSVGElement) svgRoot).getHeight().getBaseVal().
                       getValue();
        SVGArea svg = new SVGArea(fs, width, height);
        svg.setSVGDocument(doc);
        svg.start();

        /* finish off the SVG area */
        svg.end();

        /* add the SVG area to the containing area */
        ForeignObjectArea foa = (ForeignObjectArea) area;
        foa.setObject(svg);
        foa.setIntrinsicWidth(svg.getWidth());
        foa.setIntrinsicHeight(svg.getHeight());

        /* return status */
        return new Status(Status.OK);
    }
}
