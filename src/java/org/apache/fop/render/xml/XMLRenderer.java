/*
 * Copyright 1999-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: XMLRenderer.java,v 1.20 2004/04/25 04:45:28 gmazza Exp $ */

package org.apache.fop.render.xml;

// Java
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.awt.geom.Rectangle2D;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

// FOP
import org.apache.fop.render.AbstractRenderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.XMLHandler;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FOPException;
import org.apache.fop.area.Area;
import org.apache.fop.area.BeforeFloat;
import org.apache.fop.area.Block;
import org.apache.fop.area.BodyRegion;
import org.apache.fop.area.Flow;
import org.apache.fop.area.Footnote;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.MainReference;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.RegionReference;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Span;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.Container;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.Space;
import org.apache.fop.area.inline.Viewport;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.fonts.FontSetup;
import org.apache.fop.fonts.FontInfo;

/**
 * Renderer that renders areas to XML for debugging purposes.
 * This creates an xml that contains the information of the area
 * tree. It does not output any state or derived information.
 * The output can be used to build a new area tree (@see AreaTreeBuilder)
 * which can be rendered to any renderer.
 */
public class XMLRenderer extends AbstractRenderer {

    /** XML MIME type */
    public static final String XML_MIME_TYPE = "application/x-fop-areatree";

    /** Main namespace in use. */
    public static final String NS = "";
    
    /** CDATA type */
    public static final String CDATA = "CDATA";
    
    /** An empty Attributes object used when no attributes are needed. */
    public static final Attributes EMPTY_ATTS = new AttributesImpl();
    
    private boolean startedSequence = false;
    private RendererContext context;

    /** TransformerHandler that the generated XML is written to */
    protected TransformerHandler handler;
    
    /** AttributesImpl instance that can be used during XML generation. */
    protected AttributesImpl atts = new AttributesImpl();
    
    /** The OutputStream to write the generated XML to. */
    protected OutputStream out;
    
    /**
     * Creates a new XML renderer.
     */
    public XMLRenderer() {
        context = new RendererContext(XML_MIME_TYPE);
    }

    /**
     * @see org.apache.fop.render.Renderer#setUserAgent(FOUserAgent)
     */
    public void setUserAgent(FOUserAgent agent) {
        super.setUserAgent(agent);

        //
        //userAgent.addExtensionHandler();
        XMLHandler handler = new XMLXMLHandler();
        setDefaultXMLHandler(userAgent, XML_MIME_TYPE, handler);
        String svg = "http://www.w3.org/2000/svg";
        addXMLHandler(userAgent, XML_MIME_TYPE, svg, handler);
    }

    /**
     * Sets an outside TransformerHandler to use instead of the default one
     * create in this class in startRenderer().
     * @param handler Overriding TransformerHandler
     */
    public void setTransformerHandler(TransformerHandler handler) {
        this.handler = handler;
    }

    /**
     * set up the font info
     *
     * @param fontInfo the font info object to set up
     */
    public void setupFontInfo(FontInfo fontInfo) {
        FontSetup.setup(fontInfo, null);
    }

    private boolean isCoarseXml() {
        return ((Boolean) 
            userAgent.getRendererOptions().get("fineDetail")).booleanValue();
    }

    /**
     * Handles SAXExceptions.
     * @param saxe the SAXException to handle
     */
    protected void handleSAXException(SAXException saxe) {
        throw new RuntimeException(saxe.getMessage());
    }
    
    /**
     * Writes a comment to the generated XML.
     * @param comment the comment
     */
    protected void comment(String comment) {
        try {
            handler.comment(comment.toCharArray(), 0, comment.length());
        } catch (SAXException saxe) {
            handleSAXException(saxe);
        }
    }
    
    /**
     * Starts a new element (without attributes).
     * @param tagName tag name of the element
     */
    protected void startElement(String tagName) {
        startElement(tagName, EMPTY_ATTS);
    }
    
    /**
     * Starts a new element.
     * @param tagName tag name of the element
     * @param atts attributes to add
     */
    protected void startElement(String tagName, Attributes atts) {
        try {
            handler.startElement(NS, tagName, tagName, atts);
        } catch (SAXException saxe) {
            handleSAXException(saxe);
        }
    }
    
    /**
     * Ends an element.
     * @param tagName tag name of the element
     */
    protected void endElement(String tagName) {
        try {
            handler.endElement(NS, tagName, tagName);
        } catch (SAXException saxe) {
            handleSAXException(saxe);
        }
    }
    
    /**
     * Sends plain text to the XML
     * @param text the text
     */
    protected void characters(String text) {
        try {
            char[] ca = text.toCharArray();
            handler.characters(ca, 0, ca.length);
        } catch (SAXException saxe) {
            handleSAXException(saxe);
        }
    }
    
    /**
     * Adds a new attribute to the protected member variable "atts".
     * @param name name of the attribute
     * @param value value of the attribute
     */
    protected void addAttribute(String name, String value) {
        atts.addAttribute(NS, name, name, CDATA, value);
    }
    
    /**
     * Adds a new attribute to the protected member variable "atts".
     * @param name name of the attribute
     * @param value value of the attribute
     */
    protected void addAttribute(String name, int value) {
        addAttribute(name, Integer.toString(value));
    }
    
    /**
     * Adds a new attribute to the protected member variable "atts".
     * @param name name of the attribute
     * @param rect a Rectangle2D to format and use as attribute value
     */
    protected void addAttribute(String name, Rectangle2D rect) {
        addAttribute(name, createString(rect));
    }
    
    /**
     * Adds the general Area attributes.
     * @param area Area to extract attributes from
     */
    protected void addAreaAttributes(Area area) {
        addAttribute("ipd", area.getIPD());
        addAttribute("bpd", area.getBPD());
    }
    
    private String createString(Rectangle2D rect) {
        return "" + (int) rect.getX() + " " + (int) rect.getY() + " "
                  + (int) rect.getWidth() + " " + (int) rect.getHeight();
    }

    /**
     * @see org.apache.fop.render.Renderer#startRenderer(OutputStream)
     */
    public void startRenderer(OutputStream outputStream)
                throws IOException {
        getLogger().debug("Rendering areas to Area Tree XML");
    
        if (this.handler == null) {
            SAXTransformerFactory factory 
                = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
            try {
                this.handler = factory.newTransformerHandler();
                StreamResult res = new StreamResult(outputStream);
                handler.setResult(res);
            } catch (TransformerConfigurationException tce) {
                throw new RuntimeException(tce.getMessage());
            }
            
            this.out = outputStream;
        }
        
        try {
            handler.startDocument();
        } catch (SAXException saxe) {
            handleSAXException(saxe);
        }
        comment("Produced by " 
            + (userAgent.getProducer() != null ? userAgent.getProducer() : ""));
        startElement("areaTree");
    }

    /**
     * @see org.apache.fop.render.Renderer#stopRenderer()
     */
    public void stopRenderer() throws IOException {
        endElement("pageSequence");
        endElement("areaTree");
        try {
            handler.endDocument();
        } catch (SAXException saxe) {
            handleSAXException(saxe);
        }
        if (this.out != null) {
            this.out.flush();
        }
        getLogger().debug("Written out Area Tree XML");
    }

    /**
     * @see org.apache.fop.render.Renderer#renderPage(PageViewport)
     */
    public void renderPage(PageViewport page) throws IOException, FOPException {
        atts.clear();
        addAttribute("bounds", page.getViewArea());
        startElement("pageViewport", atts);
        startElement("page");
        super.renderPage(page);
        endElement("page");
        endElement("pageViewport");
    }

    /**
     * @see org.apache.fop.render.Renderer#startPageSequence(Title)
     */
    public void startPageSequence(LineArea seqTitle) {
        if (startedSequence) {
            endElement("pageSequence");
        }
        startedSequence = true;
        startElement("pageSequence");
        if (seqTitle != null) {
            startElement("title");
            List children = seqTitle.getInlineAreas();

            for (int count = 0; count < children.size(); count++) {
                InlineArea inline = (InlineArea) children.get(count);
                renderInlineArea(inline);
            }

            endElement("title");
        }
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderRegionViewport(RegionViewport)
     */
    protected void renderRegionViewport(RegionViewport port) {
        if (port != null) {
            atts.clear();
            addAttribute("rect", port.getViewArea());
            startElement("regionViewport", atts);
            RegionReference region = port.getRegion();
            if (region.getRegionClass() == FO_REGION_BEFORE) {
                startElement("regionBefore");
                renderRegion(region);
                endElement("regionBefore");
            } else if (region.getRegionClass() == FO_REGION_START) {
                startElement("regionStart");
                renderRegion(region);
                endElement("regionStart");
            } else if (region.getRegionClass() == FO_REGION_BODY) {
                startElement("regionBody");
                renderBodyRegion((BodyRegion) region);
                endElement("regionBody");
            } else if (region.getRegionClass() == FO_REGION_END) {
                startElement("regionEnd");
                renderRegion(region);
                endElement("regionEnd");
            } else if (region.getRegionClass() == FO_REGION_AFTER) {
                startElement("regionAfter");
                renderRegion(region);
                endElement("regionAfter");
            }
            endElement("regionViewport");
        }
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderBeforeFloat(BeforeFloat)
     */
    protected void renderBeforeFloat(BeforeFloat bf) {
        startElement("<beforeFloat>");
        super.renderBeforeFloat(bf);
        endElement("beforeFloat");
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderFootnote(Footnote)
     */
    protected void renderFootnote(Footnote footnote) {
        startElement("footnote");
        super.renderFootnote(footnote);
        endElement("footnote");
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderMainReference(MainReference)
     */
    protected void renderMainReference(MainReference mr) {
        atts.clear();
        addAttribute("columnGap", mr.getColumnGap());
        addAttribute("width", mr.getWidth());
        startElement("mainReference", atts);

        Span span = null;
        List spans = mr.getSpans();
        for (int count = 0; count < spans.size(); count++) {
            span = (Span) spans.get(count);
            startElement("span");
            for (int c = 0; c < span.getColumnCount(); c++) {
                Flow flow = (Flow) span.getFlow(c);

                renderFlow(flow);
            }
            endElement("span");
        }
        endElement("mainReference");
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderFlow(Flow)
     */
    protected void renderFlow(Flow flow) {
        // the normal flow reference area contains stacked blocks
        startElement("flow");
        super.renderFlow(flow);
        endElement("flow");
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderBlock(Block)
     */
    protected void renderBlock(Block block) {
        atts.clear();
        addAreaAttributes(block);
        Map map = block.getTraits();
        if (map != null) {
            addAttribute("props", getPropString(map));
        }
        startElement("block", atts);
        super.renderBlock(block);
        endElement("block");
    }

    /**
     * @see org.apache.fop.render.AbstractRenderer#renderLineArea(LineArea)
     */
    protected void renderLineArea(LineArea line) {
        atts.clear();
        addAreaAttributes(line);
        Map map = line.getTraits();
        if (map != null) {
            addAttribute("props", getPropString(map));
        }
        startElement("lineArea", atts);
        super.renderLineArea(line);
        endElement("lineArea");
    }

    /**
     * @see org.apache.fop.render.Renderer#renderViewport(Viewport)
     */
    protected void renderViewport(Viewport viewport) {
        startElement("viewport");
        super.renderViewport(viewport);
        endElement("viewport");
    }

    /**
     * Renders an image
     * @param image the image
     */
    public void renderImage(Image image) {
        atts.clear();
        addAreaAttributes(image);
        addAttribute("url", image.getURL());
        startElement("image", atts);
        endElement("image");
    }

    /**
     * @see org.apache.fop.render.Renderer#renderContainer(Container)
     */
    public void renderContainer(Container cont) {
        startElement("container");
        super.renderContainer(cont);
        endElement("container");
    }

    /**
     * Renders an fo:foreing-object.
     * @param fo the foreign object
     */
    public void renderForeignObject(ForeignObject fo) {
        atts.clear();
        addAreaAttributes(fo);
        startElement("foreignObject", atts);
        Document doc = fo.getDocument();
        String ns = fo.getNameSpace();
        context.setProperty(XMLXMLHandler.HANDLER, handler);
        renderXML(userAgent, context, doc, ns);
        endElement("foreignObject");
    }

    /**
     * @see org.apache.fop.render.Renderer#renderCharacter(Character)
     */
    protected void renderCharacter(org.apache.fop.area.inline.Character ch) {
        atts.clear();
        Map map = ch.getTraits();
        if (map != null) {
            addAttribute("props", getPropString(map));
        }
        startElement("char", atts);
        characters(ch.getChar());
        endElement("char");
    }

    /**
     * @see org.apache.fop.render.Renderer#renderInlineSpace(Space)
     */
    protected void renderInlineSpace(Space space) {
        atts.clear();
        addAreaAttributes(space);
        startElement("space", atts);
        endElement("space");
    }

    /**
     * @see org.apache.fop.render.Renderer#renderText(TextArea)
     */
    protected void renderText(TextArea text) {
        atts.clear();
        addAttribute("twsadjust", text.getTextWordSpaceAdjust());
        addAttribute("tlsadjust", text.getTextLetterSpaceAdjust());
        Map map = text.getTraits();
        if (map != null) {
            addAttribute("props", getPropString(map));
        }
        startElement("text", atts);
        characters(text.getTextArea());
        endElement("text");
        super.renderText(text);
    }

    /**
     * @see org.apache.fop.render.Renderer#renderInlineParent(InlineParent)
     */
    protected void renderInlineParent(InlineParent ip) {
        atts.clear();
        Map map = ip.getTraits();
        if (map != null) {
            addAttribute("props", getPropString(map));
        }
        startElement("inlineparent", atts);
        super.renderInlineParent(ip);
        endElement("inlineparent");
    }

    /**
     * @see org.apache.fop.render.Renderer#renderLeader(Leader)
     */
    protected void renderLeader(Leader area) {
        String style = "solid";
        switch (area.getRuleStyle()) {
            case EN_DOTTED:
                style = "dotted";
                break;
            case EN_DASHED:
                style = "dashed";
                break;
            case EN_SOLID:
                break;
            case EN_DOUBLE:
                style = "double";
                break;
            case EN_GROOVE:
                style = "groove";
                break;
            case EN_RIDGE:
                style = "ridge";
                break;
            default:
                style = "--NYI--";
        }
        atts.clear();
        addAreaAttributes(area);
        addAttribute("ruleStyle", style);
        addAttribute("ruleThickness", area.getRuleThickness());
        startElement("leader", atts);
        endElement("leader");
        super.renderLeader(area);
    }

    /**
     * Builds a String with attributes from the trait map.
     * @param traitMap the trait map
     * @return String the generated attributes
     */
    protected String getPropString(Map traitMap) {
        StringBuffer strbuf = new StringBuffer();
        Iterator iter = traitMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry traitEntry = (Map.Entry) iter.next();
            strbuf.append(Trait.getTraitName(traitEntry.getKey()));
            strbuf.append(':');
            strbuf.append(traitEntry.getValue().toString());
            strbuf.append(';');
        }
        return strbuf.toString();
    }

    /** @see org.apache.fop.render.AbstractRenderer */
    public String getMimeType() {
        return XML_MIME_TYPE;
    }

}

