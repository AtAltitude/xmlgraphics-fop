/*-- $Id$ -- 

 ============================================================================
				   The Apache Software License, Version 1.1
 ============================================================================
 
	Copyright (C) 1999 The Apache Software Foundation. All rights reserved.
 
 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:
 
 1. Redistributions of	source code must  retain the above copyright  notice,
	this list of conditions and the following disclaimer.
 
 2. Redistributions in binary form must reproduce the above copyright notice,
	this list of conditions and the following disclaimer in the documentation
	and/or other materials provided with the distribution.
 
 3. The end-user documentation included with the redistribution, if any, must
	include  the following	acknowledgment:  "This product includes  software
	developed  by the  Apache Software Foundation  (http://www.apache.org/)."
	Alternately, this  acknowledgment may  appear in the software itself,  if
	and wherever such third-party acknowledgments normally appear.
 
 4. The names "FOP" and  "Apache Software Foundation"  must not be used to
	endorse  or promote  products derived  from this  software without	prior
	written permission. For written permission, please contact
	apache@apache.org.
 
 5. Products  derived from this software may not  be called "Apache", nor may
	"Apache" appear  in their name,  without prior written permission  of the
	Apache Software Foundation.
 
 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR	PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT	OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)	HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,	WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR	OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
 This software	consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software	Foundation and was	originally created by
 James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
 Software Foundation, please see <http://www.apache.org/>.
 
 */

package org.apache.fop.render.pdf;

// FOP
import org.apache.fop.render.Renderer;
import org.apache.fop.messaging.MessageHandler;
import org.apache.fop.image.ImageArea;
import org.apache.fop.image.FopImage;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.*;
import org.apache.fop.datatypes.*;
import org.apache.fop.svg.PathPoint;
import org.apache.fop.pdf.*;
import org.apache.fop.layout.*;
import org.apache.fop.image.*;

import org.w3c.dom.*;
import org.w3c.dom.svg.*;
import org.w3c.dom.css.*;
import org.w3c.dom.svg.SVGLength;

import org.apache.fop.dom.svg.*;
import org.apache.fop.dom.svg.SVGRectElementImpl;
import org.apache.fop.dom.svg.SVGTextElementImpl;
import org.apache.fop.dom.svg.SVGLineElementImpl;
import org.apache.fop.dom.svg.SVGArea;

// Java
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.awt.Rectangle;
import java.util.Vector;
import java.util.Hashtable;

/**
 * Renderer that renders areas to PDF
 */
public class PDFRenderer implements Renderer {
			
	/** the PDF Document being created */
	protected PDFDocument pdfDoc;

	/** the /Resources object of the PDF document being created */
	protected PDFResources pdfResources;

	/** the IDReferences for this document */
	protected IDReferences idReferences;

	/** the current stream to add PDF commands to */
	PDFStream currentStream;

	/** the current annotation list to add annotations to */
	PDFAnnotList currentAnnotList;

	/** the current page to add annotations to */
	PDFPage currentPage;

	/** the current (internal) font name */
	protected String currentFontName;

	/** the current font size in millipoints */
	protected int currentFontSize;

	/** the current color/gradient for borders, letters, etc. */
	protected PDFPathPaint currentStroke = null;
	
	/** the current color/gradient to fill shapes with */
	protected PDFPathPaint currentFill = null;
	
	/** the current colour's red component */
	protected float currentRed = 0;

	/** the current colour's green component */
	protected float currentGreen = 0;

	/** the current colour's blue component */
	protected float currentBlue = 0;
	
	/** the current vertical position in millipoints from bottom */
	protected int currentYPosition = 0;

	/** the current horizontal position in millipoints from left */
	protected int currentXPosition = 0;

	/** the horizontal position of the current area container */
	private int currentAreaContainerXPosition = 0;

	/**
	 * create the PDF renderer
	 */
	public PDFRenderer() {
	this.pdfDoc = new PDFDocument();
	}

	/**
	 * set the PDF document's producer
	 *
	 * @param producer string indicating application producing PDF
	 */
	public void setProducer(String producer) {
	this.pdfDoc.setProducer(producer);
	}

	/**
	 * render the areas into PDF
	 *
	 * @param areaTree the laid-out area tree
	 * @param writer the PrintWriter to write the PDF with
	 */
	public void render(AreaTree areaTree, PrintWriter writer)
        throws IOException, FOPException {      
            MessageHandler.logln("rendering areas to PDF");
            idReferences=areaTree.getIDReferences();           
            this.pdfResources = this.pdfDoc.getResources();            
            this.pdfDoc.setIDReferences(idReferences);
	Enumeration e = areaTree.getPages().elements();
            while ( e.hasMoreElements() ) {
		this.renderPage((Page) e.nextElement());
	}
    
            if ( !idReferences.isEveryIdValid() ) {                          
                throw new FOPException("The following id's were referenced but not found: "+idReferences.getInvalidIds()+"\n");
            }
    
            MessageHandler.logln("writing out PDF");
	this.pdfDoc.output(writer);
    }

    /**
     * add a line to the current stream
     *
     * @param x1 the start x location in millipoints
     * @param y1 the start y location in millipoints
     * @param x2 the end x location in millipoints
     * @param y2 the end y location in millipoints
     * @param th the thickness in millipoints
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     */
    protected void addLine(int x1, int y1, int x2, int y2, int th, 
		PDFPathPaint stroke)
    {
		currentStream.add("ET\nq\n"
			+ stroke.getColorSpaceOut(false)
			+ (x1/1000f) + " "+ (y1/1000f) + " m "
			+ (x2/1000f) + " "+ (y2/1000f) + " l "
			+ (th / 1000f) + " w S\n"
			+ "Q\nBT\n");
    }

	/**
	 * add a rectangle to the current stream
	 *
	 * @param x the x position of left edge in millipoints
	 * @param y the y position of top edge in millipoints
	 * @param w the width in millipoints
	 * @param h the height in millipoints
	 * @param stroke the stroke color/gradient
	 */
	protected void addRect(int x, int y, int w, int h,
			   PDFPathPaint stroke) { 
	currentStream.add("ET\nq\n"
			+ stroke.getColorSpaceOut(false)
			  + (x/1000f) + " " + (y/1000f) + " "
			  + (w/1000f) + " " + (h/1000f) + " re s\n"
			  + "Q\nBT\n");
	}

	/**
	 * add a filled rectangle to the current stream
	 *
	 * @param x the x position of left edge in millipoints
	 * @param y the y position of top edge in millipoints
	 * @param w the width in millipoints
	 * @param h the height in millipoints
	 * @param fill the fill color/gradient
	 * @param stroke the stroke color/gradient
	 */
	protected void addRect(int x, int y, int w, int h,
			   PDFPathPaint stroke,
			   PDFPathPaint fill) {
	currentStream.add("ET\nq\n"
			+ fill.getColorSpaceOut(true)
			  + stroke.getColorSpaceOut(false)
			  + (x/1000f) + " " + (y/1000f) + " "
			  + (w/1000f) + " " + (h/1000f) + " re b\n"
			  + "Q\nBT\n");
    }

    /**
     * render area container to PDF
     *
     * @param area the area container to render
     */
    public void renderAreaContainer(AreaContainer area) {
        
        int saveY = this.currentYPosition;
        int saveX = this.currentAreaContainerXPosition;
        
        if (area.getPosition() == Position.ABSOLUTE) {
            // Y position is computed assuming positive Y axis, adjust for negative postscript one
 	  this.currentYPosition = area.getYPosition() - 2 * area.getPaddingTop() - 2 * area.borderWidthTop;
	  this.currentAreaContainerXPosition = area.getXPosition();
		} else if (area.getPosition() == Position.RELATIVE) {
	  this.currentYPosition -= area.getYPosition();
		  this.currentAreaContainerXPosition += area.getXPosition();
		} else if (area.getPosition() == Position.STATIC) {
	  this.currentYPosition -= area.getPaddingTop() + area.borderWidthTop;
		  this.currentAreaContainerXPosition += area.getPaddingLeft() + area.borderWidthLeft;
		}

		doFrame(area);
		
	Enumeration e = area.getChildren().elements();
	while (e.hasMoreElements()) {
		Box b = (Box) e.nextElement();
		b.render(this);
	}
		if (area.getPosition() != Position.STATIC) {
		  this.currentYPosition = saveY;
		  this.currentAreaContainerXPosition = saveX;
		} else 
		  this.currentYPosition -= area.getHeight();
	}
	
	private void doFrame(Area area) {
		int w, h;
	int rx = this.currentAreaContainerXPosition;
	w = area.getContentWidth();
		if (area instanceof BlockArea)
	  rx += ((BlockArea)area).getStartIndent();
		h = area.getContentHeight();
	int ry = this.currentYPosition;
	ColorType bg = area.getBackgroundColor();
		
		rx = rx - area.getPaddingLeft();
		ry = ry + area.getPaddingTop();
		w = w + area.getPaddingLeft() + area.getPaddingRight();
		h = h + area.getPaddingTop() + area.getPaddingBottom();
		
	// I'm not sure I should have to check for bg being null
	// but I do
	if ((bg != null) && (bg.alpha() == 0)) {
		this.addRect(rx, ry, w, -h,
			 new PDFColor(bg),
			 new PDFColor(bg));
	}
	
		rx = rx - area.borderWidthLeft;
		ry = ry + area.borderWidthTop;
		w = w + area.borderWidthLeft + area.borderWidthRight;
		h = h + area.borderWidthTop + area.borderWidthBottom;
		
		if (area.borderWidthTop != 0)
		  addLine(rx, ry, rx + w, ry, 
				area.borderWidthTop,  
				new PDFColor(area.borderColorTop));
		if (area.borderWidthLeft != 0)
		  addLine(rx, ry, rx, ry - h, 
				area.borderWidthLeft,  
				new PDFColor(area.borderColorLeft));
		if (area.borderWidthRight != 0)
		  addLine(rx + w, ry, rx + w, ry - h, 
				area.borderWidthRight,	
				new PDFColor(area.borderColorRight));
		if (area.borderWidthBottom != 0)
		  addLine(rx, ry - h, rx + w, ry - h, 
				area.borderWidthBottom,  
				new PDFColor(area.borderColorBottom));

	} 
   

	/**
	 * render block area to PDF
	 *
	 * @param area the block area to render
	 */
	public void renderBlockArea(BlockArea area) {
		doFrame(area);
	Enumeration e = area.getChildren().elements();
	while (e.hasMoreElements()) {
		Box b = (Box) e.nextElement();
		b.render(this);
	}
	}

	/**
	 * render display space to PDF
	 *
	 * @param space the display space to render
	 */
	public void renderDisplaySpace(DisplaySpace space) {
	int d = space.getSize();
	this.currentYPosition -= d;
	}

	/**
	 * render image area to PDF
	 *
	 * @param area the image area to render
	 */
	public void renderImageArea(ImageArea area) {
	// adapted from contribution by BoBoGi
	int x = this.currentAreaContainerXPosition +
		area.getXOffset();
	int y = this.currentYPosition;
	int w = area.getContentWidth();
	int h = area.getHeight();

	this.currentYPosition -= h;

	FopImage img = area.getImage();

	int xObjectNum = this.pdfDoc.addImage(img);

	currentStream.add("ET\nq\n" + (((float) w) / 1000f) + " 0 0 " +
			  (((float) h) / 1000f) + " " + 
			  (((float) x) / 1000f) + " " + 
			  (((float) (y - h)) / 1000f) + " cm\n" +
			  "/Im" + xObjectNum + " Do\nQ\nBT\n"); 
    }

    /** render a foreign object area */
    public void renderForeignObjectArea(ForeignObjectArea area)
    {
        // if necessary need to scale and align the content
		int x = this.currentAreaContainerXPosition;
		switch(area.getAlign()) {
		    case TextAlign.START:
		    break;
		    case TextAlign.END:
		    break;
		    case TextAlign.CENTER:
		    case TextAlign.JUSTIFY:
		    break;
		}
		switch(area.getVerticalAlign()) {
		    case VerticalAlign.BASELINE:
		    break;
		    case VerticalAlign.MIDDLE:
		    break;
		    case VerticalAlign.SUB:
		    break;
		    case VerticalAlign.SUPER:
		    break;
		    case VerticalAlign.TEXT_TOP:
		    break;
		    case VerticalAlign.TEXT_BOTTOM:
		    break;
		    case VerticalAlign.TOP:
		    break;
		    case VerticalAlign.BOTTOM:
		    break;
		}
		// in general the content will not be text
		currentStream.add("ET\n");
		// align and scale
		currentStream.add("q\n");
		switch(area.scalingMethod()) {
		    case Scaling.UNIFORM:
		    break;
		    case Scaling.NON_UNIFORM:
		    break;
		}
		// if the overflow is auto (default), scroll or visible
		// then the contents should not be clipped, since this
		// is considered a printing medium.
		switch(area.getOverflow()) {
		    case Overflow.VISIBLE:
		    case Overflow.SCROLL:
		    case Overflow.AUTO:
		    break;
		    case Overflow.HIDDEN:
		    break;
		}
        area.getObject().render(this);
		currentStream.add("Q\n");
		currentStream.add("BT\n");
		this.currentYPosition -= area.getEffectiveHeight();
    }

    /**
     * render SVG area to PDF
     *
     * @param area the SVG area to render
     */
	public void renderSVGArea(SVGArea area) {
		int x = this.currentAreaContainerXPosition;
		int y = this.currentYPosition;
		SVGSVGElement svg = area.getSVGDocument().getRootElement();
		int w = (int)(svg.getWidth().getBaseVal().getValue() * 1000);
		int h = (int)(svg.getHeight().getBaseVal().getValue() * 1000);

		/*
		 * Clip to the svg area.
		 * Note: To have the svg overlay (under) a text area then use
		 * an fo:block-container
		 */
		currentStream.add("q\n");
		currentStream.add(x / 1000f + " " + y / 1000f + " m\n");
		currentStream.add((x + w) / 1000f + " " + y / 1000f + " l\n");
		currentStream.add((x + w) / 1000f + " " + (y - h) / 1000f + " l\n");
		currentStream.add(x / 1000f + " " + (y - h) / 1000f + " l\n");
		currentStream.add("h\n");
		currentStream.add("W\n");
		currentStream.add("n\n");
		// transform so that the coordinates (0,0) is from the top left
		// and positive is down and to the right
		currentStream.add(1 + " " + 0 + " " + 0 + " " + (-1) + " " + x / 1000f + " " + y / 1000f + " cm\n");

		// TODO - translate and clip to viewbox

		renderSVG(area.getFontState(), svg, x, y);

		currentStream.add("Q\n");
	}

    /**
     * render inline area to PDF
     *
     * @param area inline area to render
     */
    public void renderInlineArea(InlineArea area) {
	char ch;
	StringBuffer pdf = new StringBuffer();
		
	String name = area.getFontState().getFontName();
	int size = area.getFontState().getFontSize();
	
	PDFColor theAreaColor = new PDFColor(
				(double)area.getRed(),
				(double)area.getGreen(),
				(double)area.getBlue()  );
		
	if ((!name.equals(this.currentFontName))
		|| (size != this.currentFontSize)) {
		this.currentFontName = name;
		this.currentFontSize = size;
		pdf = pdf.append("/" + name + " " + (size/1000) + " Tf\n");
	}

	//if (theAreaColor.isEquivalent(this.currentFill)) {
		this.currentFill = theAreaColor;
		
		pdf = pdf.append(this.currentFill.getColorSpaceOut(true));
	//}
		
	int rx = this.currentXPosition;
	int bl = this.currentYPosition;

	pdf = pdf.append("1 0 0 1 "
			 +(rx/1000f) + " " + (bl/1000f)
			 + " Tm (");

	String s;
	if ( area.getPageNumberID()!=null ) { // this text is a page number, so resolve it
            s = idReferences.getPageNumber(area.getPageNumberID());            
            if(s==null)
            {
                s="";
            }
        }
        else {
            s = area.getText();
        }
	
	int l = s.length();

	for (int i=0; i < l; i++) {
		ch = s.charAt(i);
		if (ch > 127) {
		pdf = pdf.append("\\");
		pdf = pdf.append(Integer.toOctalString((int)ch));
		} else {
		switch (ch) {
		case '('  : pdf = pdf.append("\\("); break;
		case ')'  : pdf = pdf.append("\\)"); break;
		case '\\' : pdf = pdf.append("\\\\"); break;
		default   : pdf = pdf.append(ch); break;
		}
		}
	}
	pdf = pdf.append(") Tj\n");

	currentStream.add(pdf.toString());

	this.currentXPosition += area.getContentWidth();
	}

	/**
	 * render inline space to PDF
	 *
	 * @param space space to render
	 */
	public void renderInlineSpace(InlineSpace space) {
	this.currentXPosition += space.getSize();
	}

	/**
	 * render line area to PDF
	 *
	 * @param area area to render
	 */
	public void renderLineArea(LineArea area) {
	int rx = this.currentAreaContainerXPosition
		+ area.getStartIndent();
	int ry = this.currentYPosition;
	int w = area.getContentWidth();
	int h = area.getHeight();

	this.currentYPosition -= area.getPlacementOffset();
	this.currentXPosition = rx;

	int bl = this.currentYPosition;

	Enumeration e = area.getChildren().elements();
	while (e.hasMoreElements()) {
		Box b = (Box) e.nextElement();
		b.render(this);
	}

	this.currentYPosition = ry-h;
	}

	/**
	 * render page into PDF
	 *
	 * @param page page to render
	 */
	public void renderPage(Page page) {
	AreaContainer body, before, after;
	  
	currentStream = this.pdfDoc.makeStream();
	body = page.getBody();
	before = page.getBefore();
	after = page.getAfter();

	this.currentFontName = "";
	this.currentFontSize = 0;

	currentStream.add("BT\n");
	renderAreaContainer(body);

	if (before != null) {
		renderAreaContainer(before);
	}

	if (after != null) {
		renderAreaContainer(after);
	}
	
	currentStream.add("ET\n");

	currentPage = this.pdfDoc.makePage(this.pdfResources, currentStream,
				 page.getWidth()/1000,
				 page.getHeight()/1000, page);

	if (page.hasLinks()) {
		currentAnnotList = this.pdfDoc.makeAnnotList();
		currentPage.setAnnotList(currentAnnotList);

		Enumeration e = page.getLinkSets().elements();
		while (e.hasMoreElements()) {
		LinkSet linkSet = (LinkSet) e.nextElement();

		linkSet.align();
		String dest = linkSet.getDest();
                int linkType = linkSet.getLinkType();
		Enumeration f = linkSet.getRects().elements();
		while (f.hasMoreElements()) {
			LinkedRectangle lrect = (LinkedRectangle) f.nextElement();
			currentAnnotList.addLink(
				this.pdfDoc.makeLink(lrect.getRectangle(), dest, linkType));
		}
		}
	} else {
		// just to be on the safe side
		currentAnnotList = null;
	}
	}

	/**
	 * render rule area into PDF
	 *
	 * @param area area to render
	 */
	public void renderRuleArea(RuleArea area) {
	int rx = this.currentAreaContainerXPosition
		+ area.getStartIndent();
	int ry = this.currentYPosition;
	int w = area.getContentWidth();
	int h = area.getHeight();
	int th = area.getRuleThickness();
	
	addLine(rx, ry, rx+w, ry, th, new PDFColor(area.getRed(), area.getGreen(),area.getBlue()));
	}

	/**
	 * set up the font info
	 *
	 * @param fontInfo font info to set up
	 */
	public void setupFontInfo(FontInfo fontInfo) {
	FontSetup.setup(fontInfo);
	FontSetup.addToResources(this.pdfDoc, fontInfo);
	}

	// SVG Stuff

	public void renderGArea(FontState fontState, SVGGElement area, int posx, int posy)
	{
		NodeList nl = area.getChildNodes();
		for(int count = 0; count < nl.getLength(); count++) {
			Node n = nl.item(count);
			if(n instanceof SVGElement) {
				renderElement(fontState, (SVGElement)n, posx, posy);
			}
		}
	}

	void handleSwitchElement(FontState fontState, int posx, int posy, SVGSwitchElement ael)
	{
		SVGList relist = ael.getRequiredExtensions();
		SVGList rflist = ael.getRequiredFeatures();
		SVGList sllist = ael.getSystemLanguage();
		org.w3c.dom.NodeList nl = ael.getChildNodes();
		for(int count = 0; count < nl.getLength(); count++) {
			org.w3c.dom.Node n = nl.item(count);
			// only render the first child that has a valid
			// test data
			if(n instanceof GraphicElement) {
				GraphicElement graphic = (GraphicElement)n;
				SVGList grelist = graphic.getRequiredExtensions();
				// if null it evaluates to true
				if(grelist != null) {
					for(int i = 0; i < grelist.getNumberOfItems(); i++) {
						String str = (String)grelist.getItem(i);
						if(relist == null) {
							// use default extension set
							// currently no extensions are supported
//							if(!(str.equals("http:// ??"))) {
								continue;
//							}
						} else {
						}
					}
				}
				SVGList grflist = graphic.getRequiredFeatures();
				if(grflist != null) {
					for(int i = 0; i < grflist.getNumberOfItems(); i++) {
						String str = (String)grflist.getItem(i);
						if(rflist == null) {
							// use default feature set
							if(!(str.equals("org.w3c.svg.static")
								|| str.equals("org.w3c.dom.svg.all"))) {
								continue;
							}
						} else {
							boolean found = false;
							for(int j = 0; j < rflist.getNumberOfItems(); j++) {
								if(rflist.getItem(j).equals(str)) {
									found = true;
									break;
								}
							}
							if(!found)
								continue;
						}
					}
				}
				SVGList gsllist = graphic.getSystemLanguage();
				if(gsllist != null) {
					for(int i = 0; i < gsllist.getNumberOfItems(); i++) {
						String str = (String)gsllist.getItem(i);
						if(sllist == null) {
							// use default feature set
							if(!(str.equals("en"))) {
								continue;
							}
						} else {
							boolean found = false;
							for(int j = 0; j < sllist.getNumberOfItems(); j++) {
								if(sllist.getItem(j).equals(str)) {
									found = true;
									break;
								}
							}
							if(!found)
								continue;
						}
					}
				}
				renderElement(fontState, (SVGElement)n, posx, posy);
				// only render the first valid one
				break;
			}
		}
	}

    /**
     * add a line to the current stream
     *
     * @param x1 the start x location in millipoints
     * @param y1 the start y location in millipoints
     * @param x2 the end x location in millipoints
     * @param y2 the end y location in millipoints
     * @param th the thickness in millipoints
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     */
    protected void addLine(float x1, float y1, float x2, float y2, DrawingInstruction di)
	{
    	String str;
    	str = "" + x1 + " " + y1 + " m "
			  + x2 + " " + y2 + " l";
		if(di != null && di.fill)
			currentStream.add(str + " f\n"); // ??
		currentStream.add(str + " S\n");
    }

    protected void addCircle(float cx, float cy, float r, DrawingInstruction di)
    {
    	String str;
    	str = "" + cx + " " + (cy - r) + " m\n"
			+ "" + (cx + 21 * r / 40f) + " " + (cy - r) + " " + (cx + r) + " " + (cy - 21 * r / 40f) + " " + (cx + r) + " " + cy + " c\n"
			+ "" + (cx + r) + " " + (cy + 21 * r / 40f) + " " + (cx + 21 * r / 40f) + " " + (cy + r) + " " + cx + " " + (cy + r) + " c\n"
			+ "" + (cx - 21 * r / 40f) + " " + (cy + r) + " " + (cx - r) + " " + (cy + 21 * r / 40f) + " " + (cx - r) + " " + cy + " c\n"
			+ "" + (cx - r) + " " + (cy - 21 * r / 40f) + " " + (cx - 21 * r / 40f) + " " + (cy - r) + " " + cx + " " + (cy - r) + " c\n";

		currentStream.add(str);
		doDrawing(di);
    }

    protected void addEllipse(float cx, float cy, float rx, float ry, DrawingInstruction di)
    {
    	String str;
    	str = "" + cx + " " + (cy - ry) + " m\n"
			+ "" + (cx + 21 * rx / 40f) + " " + (cy - ry) + " " + (cx + rx) + " " + (cy - 21 * ry / 40f) + " " + (cx + rx) + " " + cy + " c\n"
			+ "" + (cx + rx) + " " + (cy + 21 * ry / 40f) + " " + (cx + 21 * rx / 40f) + " " + (cy + ry) + " " + cx + " " + (cy + ry) + " c\n"
			+ "" + (cx - 21 * rx / 40f) + " " + (cy + ry) + " " + (cx - rx) + " " + (cy + 21 * ry / 40f) + " " + (cx - rx) + " " + cy + " c\n"
			+ "" + (cx - rx) + " " + (cy - 21 * ry / 40f) + " " + (cx - 21 * rx / 40f) + " " + (cy - ry) + " " + cx + " " + (cy - ry) + " c\n";
		currentStream.add(str);
		doDrawing(di);
    }

    /**
     * add a filled rectangle to the current stream
     *
     * @param x the x position of left edge in millipoints
     * @param y the y position of top edge in millipoints
     * @param w the width in millipoints
     * @param h the height in millipoints
     * @param r the red component of edges
     * @param g the green component of edges
     * @param b the blue component of edges
     * @param fr the red component of the fill
     * @param fg the green component of the fill
     * @param fb the blue component of the fill
     */
    protected void addRect(float x, float y, float w, float h, float rx, float ry, DrawingInstruction di)
    {
    	String str = "";
    	if(rx == 0.0 && ry == 0.0) {
        	str = "" + x + " " + y + " " + w + " " + h + " re\n";
        } else {
            str = "" + (x + rx) + " " + y + " m\n";
            str += "" + (x + w - rx) + " " + y + " l\n";
            str += "" + (x + w) + " " + y + " " + (x + w) + " " + y + " " + (x + w) + " " + (y + ry) + " c\n";
            str += "" + (x + w) + " " + (y + h - ry) + " l\n";
            str += "" + (x + w) + " " + (y + h) + " " + (x + w) + " " + (y + h) + " " + (x + w - rx) + " " + (y + h) + " c\n";
            str += "" + (x + rx) + " " + (y + h) + " l\n";
            str += "" + x + " " + (y + h) + " " + x + " " + (y + h) + " " + x + " " + (y + h - ry) + " c\n";
            str += "" + x + " " + (y + ry) + " l\n";
            str += "" + x + " " + y + " " + x + " " + y + " " + (x + rx) + " " + y + " c\n";
        }
		currentStream.add(str);
		doDrawing(di);
    }

    protected void addPath(Vector points, int posx, int posy, DrawingInstruction di)
    {
    	SVGPathSegImpl pathmoveto = null;
    	float lastx = 0;
    	float lasty = 0;
    	for(Enumeration e = points.elements(); e.hasMoreElements(); ) {
    		SVGPathSegImpl pc = (SVGPathSegImpl)e.nextElement();
    		float[] vals = pc.getValues();
    		float lastcx = 0;
    		float lastcy = 0;
    		switch(pc.getPathSegType()) {
    			case SVGPathSeg.PATHSEG_MOVETO_ABS:
    				pathmoveto = pc;
	    			lastx = vals[0];
	    			lasty = vals[1];
					currentStream.add(lastx + " " + lasty + " m\n");
    			break;
    			case SVGPathSeg.PATHSEG_MOVETO_REL:
	    			if(pathmoveto == null) {
	    				lastx = vals[0];
		    			lasty = vals[1];
		    			pathmoveto = pc;
						currentStream.add(lastx + " " + lasty + " m\n");
	    			} else {
	    				lastx += vals[0];
		    			lasty += vals[1];
						currentStream.add(lastx + " " + lasty + " l\n");
	    			}
    			break;
    			case SVGPathSeg.PATHSEG_LINETO_ABS:
	    			lastx = vals[0];
	    			lasty = vals[1];
					currentStream.add(lastx + " " + lasty + " l\n");
    			break;
    			case SVGPathSeg.PATHSEG_LINETO_REL:
	    			lastx += vals[0];
	    			lasty += vals[1];
					currentStream.add(lastx + " " + lasty + " l\n");
    			break;
    			case SVGPathSeg.PATHSEG_LINETO_VERTICAL_ABS:
	    			lasty = vals[0];
					currentStream.add(lastx + " " + lasty + " l\n");
    			break;
    			case SVGPathSeg.PATHSEG_LINETO_VERTICAL_REL:
	    			lasty += vals[0];
					currentStream.add(lastx + " " + lasty + " l\n");
    			break;
    			case SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_ABS:
	    			lastx = vals[0];
					currentStream.add(lastx + " " + lasty + " l\n");
    			break;
    			case SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_REL:
	    			lastx += vals[0];
					currentStream.add(lastx + " " + lasty + " l\n");
    			break;
    			case SVGPathSeg.PATHSEG_CURVETO_CUBIC_ABS:
	    			lastx = vals[4];
	    			lasty = vals[5];
	    			lastcx = vals[2];
	    			lastcy = vals[3];
					currentStream.add((vals[0]) + " " + (vals[1]) + " " +
										(vals[2]) + " " + (vals[3]) + " " +
										lastx + " " + lasty +
										" c\n");
    			break;
    			case SVGPathSeg.PATHSEG_CURVETO_CUBIC_REL:
					currentStream.add((vals[0] + lastx) + " " + (vals[1] + lasty) + " " +
										(vals[2] + lastx) + " " + (vals[3] + lasty) + " " +
										(vals[4] + lastx) + " " + (vals[5] + lasty) +
										" c\n");
	    			lastcx = vals[2] + lastx;
	    			lastcy = vals[3] + lasty;
	    			lastx += vals[4];
	    			lasty += vals[5];
    			break;
    			case SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_ABS:
    				if(lastcx == 0) {
    					lastcx = lastx;
    				}
    				if(lastcy == 0) {
    					lastcy = lasty;
    				}
	    			lastx = vals[2];
	    			lasty = vals[3];
					currentStream.add(lastcx + " " + lastcy + " " +
										(vals[0]) + " " + (vals[1]) + " " +
										lastx + " " + lasty +
										" c\n");
    			break;
    			case SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_REL:
    				if(lastcx == 0) {
    					lastcx = lastx;
    				}
    				if(lastcy == 0) {
    					lastcy = lasty;
    				}
					currentStream.add(lastcx + " " + lastcy + " " +
										(vals[0] + lastx) + " " + (vals[1] + lasty) + " " +
										(vals[2] + lastx) + " " + (vals[3] + lasty) +
										" c\n");
	    			lastx += vals[2];
	    			lasty += vals[3];
    			break;
    			case SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_ABS:
    				if(lastcx == 0) {
    					lastcx = lastx;
    				}
    				if(lastcy == 0) {
    					lastcy = lasty;
    				}
	    			lastx = vals[0];
	    			lasty = vals[1];
	    			lastcx = 0;
	    			lastcy = 0;
					currentStream.add(lastcx + " " + lastcy + " " +
										lastx + " " + lasty +
										" y\n");
    			break;
    			case SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_REL:
    				if(lastcx == 0) {
    					lastcx = lastx;
    				}
    				if(lastcy == 0) {
    					lastcy = lasty;
    				}
					currentStream.add(lastcx + " " + lastcy + " " +
										(vals[0] + lastx) + " " + (vals[1] + lasty) +
										" y\n");
	    			lastcx = 0;
	    			lastcy = 0;
	    			lastx += vals[0];
	    			lasty += vals[1];
    			break;
				// get angle between the two points
				// then get angle of points to centre (ie. both points are on the
				// apogee and perigee of the ellipse)
				// then work out the direction from flags
    			case SVGPathSeg.PATHSEG_ARC_ABS:
    				{
	    				double rx = vals[0];
	    				double ry = vals[1];
	    				double theta = vals[2];
	    				boolean largearcflag = (vals[3] == 1.0);
	    				boolean sweepflag = (vals[4] == 1.0);

	    				double angle = Math.atan((vals[6] - lasty) / (vals[5] - lastx));
	    				double relangle = Math.acos(rx / Math.sqrt((vals[6] - lasty) * (vals[6] - lasty) + (vals[5] - lastx) * (vals[5] - lastx)));
	    				double absangle = angle + relangle;
	    				// change sign depending on flags
	    				double contrx1;
	    				double contry1;
	    				double contrx2;
	    				double contry2;
	    				if(largearcflag) {
	    					if(sweepflag) {
			    				contrx1 = lastx - rx * Math.cos(absangle);
			    				contry1 = lasty + rx * Math.sin(absangle);
			    				contrx2 = vals[5] + ry * Math.cos(absangle);
			    				contry2 = vals[6] + ry * Math.sin(absangle);
		    				} else {
			    				contrx1 = lastx - rx * Math.cos(absangle);
			    				contry1 = lasty - rx * Math.sin(absangle);
			    				contrx2 = vals[5] + ry * Math.cos(absangle);
			    				contry2 = vals[6] - ry * Math.sin(absangle);
		    				}
	    				} else {
	    					if(sweepflag) {
			    				contrx1 = lastx + rx * Math.cos(absangle);
			    				contry1 = lasty + rx * Math.sin(absangle);
			    				contrx2 = contrx1;
			    				contry2 = contry1;
		    				} else {
			    				contrx1 = lastx + ry * Math.cos(absangle);
			    				contry1 = lasty - ry * Math.sin(absangle);
			    				contrx2 = contrx1;
			    				contry2 = contry1;
		    				}
	    				}

						double cx = lastx;
						double cy = lasty;
						currentStream.add(contrx1 + " " + contry1 + " " +
											contrx2 + " " + contry2 + " " +
											vals[5] + " " + vals[6] +
											" c\n");
		    			lastcx = 0; //??
		    			lastcy = 0; //??
		    			lastx = vals[5];
		    			lasty = vals[6];
					}
    			break;
    			case SVGPathSeg.PATHSEG_ARC_REL:
    				{
	    				double rx = vals[0];
	    				double ry = vals[1];
	    				double theta = vals[2];
	    				boolean largearcflag = (vals[3] == 1.0);
	    				boolean sweepflag = (vals[4] == 1.0);

	    				double angle = Math.atan(vals[6] / vals[5]);
	    				double relangle = Math.atan(ry / rx);
//	    				System.out.println((theta * Math.PI / 180f) + ":" + relangle + ":" + largearcflag + ":" + sweepflag);
	    				double absangle = (theta * Math.PI / 180f);//angle + relangle;
	    				// change sign depending on flags
	    				double contrx1;
	    				double contry1;
	    				double contrx2;
	    				double contry2;
	    				if(largearcflag) {
	    				    // in a large arc we need to do at least 2 and a bit
	    				    // segments or curves.
	    					if(sweepflag) {
			    				contrx1 = lastx + rx * Math.cos(absangle);
			    				contry1 = lasty + rx * Math.sin(absangle);
			    				contrx2 = lastx + vals[5] + ry * Math.cos(absangle);
			    				contry2 = lasty + vals[6] - ry * Math.sin(absangle);
		    				} else {
			    				contrx1 = lastx + rx * Math.sin(absangle);
			    				contry1 = lasty + rx * Math.cos(absangle);
			    				contrx2 = lastx + vals[5] + ry * Math.cos(absangle);
			    				contry2 = lasty + vals[6] + ry * Math.sin(absangle);
		    				}
	    				} else {
	    				    // only need at most two segments
	    					if(sweepflag) {
			    				contrx1 = lastx + rx * Math.cos(absangle);
			    				contry1 = lasty - rx * Math.sin(absangle);
			    				contrx2 = contrx1;
			    				contry2 = contry1;
		    				} else {
			    				contrx1 = lastx - ry * Math.cos(absangle);
			    				contry1 = lasty + ry * Math.sin(absangle);
			    				contrx2 = contrx1;
			    				contry2 = contry1;
		    				}
	    				}
	    				//System.out.println(contrx1 + ":" + contry1 + ":" + contrx2 + ":" + contry2);

						double cx = lastx;
						double cy = lasty;
						currentStream.add(contrx1 + " " + contry1 + " " +
											contrx2 + " " + contry2 + " " +
											(vals[5] + lastx) + " " + (vals[6] + lasty) +
											" c\n");

		    			lastcx = 0; //??
		    			lastcy = 0; //??
		    			lastx += vals[5];
		    			lasty += vals[6];
		    		}
    			break;
    			case SVGPathSeg.PATHSEG_CLOSEPATH:
					currentStream.add("h\n");
    			break;
    		}
    	}
		doDrawing(di);
    }

    protected void addPolyline(Vector points, int posx, int posy, DrawingInstruction di, boolean close)
    {
		PathPoint pc;
    	float lastx = 0;
    	float lasty = 0;
    	Enumeration e = points.elements();
    	if(e.hasMoreElements()) {
    		pc = (PathPoint)e.nextElement();
			lastx = pc.x;
			lasty = pc.y;
			currentStream.add(lastx + " " + lasty + " m\n");
    	}
    	while(e.hasMoreElements()) {
    		pc = (PathPoint)e.nextElement();
			lastx = pc.x;
			lasty = pc.y;
			currentStream.add(lastx + " " + lasty + " l\n");
    	}
    	if(close)
			currentStream.add("h\n");
		doDrawing(di);
    }

	protected void doDrawing(DrawingInstruction di)
	{
		if(di == null) {
			currentStream.add("S\n");
		} else {
			if(di.fill) {
				if(di.stroke) {
					if(!di.nonzero)
						currentStream.add("B*\n");
					else
						currentStream.add("B\n");
				} else {
					if(!di.nonzero)
						currentStream.add("f*\n");
					else
						currentStream.add("f\n");
				}
			} else {
//				if(di.stroke)
					currentStream.add("S\n");
			}
		}
	}

	public void renderImage(FontState fontState, String href, float x, float y, float width, float height)
	{
		try {
		    if(href.indexOf(":") == -1) {
		        href = "file:" + href;
		    }
			FopImage img = FopImageFactory.Make(href);
			if(img instanceof SVGImage) {
				SVGSVGElement svg = ((SVGImage)img).getSVGDocument().getRootElement();
				currentStream.add("q\n" + width / svg.getWidth().getBaseVal().getValue() + " 0 0 " + height / svg.getHeight().getBaseVal().getValue() + " 0 0 cm\n");
				renderSVG(fontState, svg, (int)x * 1000, (int)y * 1000);
				currentStream.add("Q\n");
//				renderSVG(svg);
			} else if(img != null) {
				int xObjectNum = this.pdfDoc.addImage(img);
				currentStream.add("q\n1 0 0 -1 " + 0
				          + " " + (2 * y + height) + " cm\n" + width + " 0 0 " +
						  height + " " +
						  x + " " +
						  y + " cm\n" +
						  "/Im" + xObjectNum + " Do\nQ\n");
//				img.close();
			}
		} catch(Exception e) {
e.printStackTrace();
			System.err.println("could not add image to SVG: " + href);
		}
	}

	void renderSVG(FontState fontState, SVGSVGElement svg, int x, int y)
	{
		NodeList nl = svg.getChildNodes();
		for(int count = 0; count < nl.getLength(); count++) {
			Node n = nl.item(count);
			if(n instanceof SVGElement) {
				renderElement(fontState, (SVGElement)n, x, y);
			}
		}
	}

	void handleGradient(String sp, boolean fill, SVGElement area)
	{
		// should be a url to a gradient
		String url = (String)sp;
		if(url.startsWith("url(")) {
			String address;
			int b1 = url.indexOf("(");
			int b2 = url.indexOf(")");
			address = url.substring(b1 + 1, b2);
			address = address.trim();
			// local reference
			if(address.startsWith("#")) {
				// find the gradient element
				SVGElement gi = null;
//				gi = area.locateDef(address.substring(1, address.length()));
//						System.out.println(gi + ":" + address.substring(1, address.length()));
				if(gi instanceof SVGLinearGradientElement) {
					SVGLinearGradientElement linear = (SVGLinearGradientElement)gi;

					Vector theCoords = new Vector();
					theCoords.addElement(new Double(linear.getX1().getBaseVal().getValue()));
					theCoords.addElement(new Double(linear.getY1().getBaseVal().getValue()));
					theCoords.addElement(new Double(linear.getX2().getBaseVal().getValue()));
					theCoords.addElement(new Double(linear.getY2().getBaseVal().getValue()));

					Vector theExtend = new Vector();
					theExtend.addElement(new Boolean(true));
					theExtend.addElement(new Boolean(true));

					Vector theDomain = new Vector();
					theDomain.addElement(new Double(0));
					theDomain.addElement(new Double(1));

					Vector theEncode = new Vector();
					theEncode.addElement(new Double(0));
					theEncode.addElement(new Double(1));
					theEncode.addElement(new Double(0));
					theEncode.addElement(new Double(1));

					Vector theBounds = new Vector();
					theBounds.addElement(new Double(0));
					theBounds.addElement(new Double(1));

					Vector theFunctions = new Vector();

					NodeList nl = linear.getChildNodes();
					Vector someColors = new Vector();
					float lastoffset = 0;
					Vector lastVector = null;
					SVGStopElementImpl stop;
					Hashtable table = null;
					for(int count = 0; count < nl.getLength(); count++) {
						stop = (SVGStopElementImpl)nl.item(count);
//						table = stop.oldgetStyle();
						ColorType sc = (ColorType)table.get("stop-color");
						if(sc == null) {
							// maybe using color
							sc = (ColorType)table.get("color");
						}
						if(sc == null) {
							// problems
							System.err.println("no stop-color or color in stop element");
							continue;
						}
						float offset = stop.getOffset().getBaseVal();
						PDFColor color = new PDFColor(sc.red(), sc.green(), sc.blue());
						Vector colVector = color.getVector();
						// create bounds from last to offset
						if(lastVector != null) {
							Vector theCzero = lastVector;
							Vector theCone = colVector;
							PDFFunction myfunc = this.pdfDoc.makeFunction(2, theDomain, null,
													theCzero, theCone, 1.0);
							theFunctions.addElement(myfunc);
						}
						lastoffset = offset;
						lastVector = colVector;
						someColors.addElement(color);
					}
					ColorSpace aColorSpace = new ColorSpace(ColorSpace.DEVICE_RGB);
/*							PDFFunction myfunky = this.pdfDoc.makeFunction(3,
						theDomain, null,
						theFunctions, null,
						theEncode);
					PDFShading myShad = null;
					myShad = this.pdfDoc.makeShading(
						2, aColorSpace,
						null, null, false,
						theCoords, null, myfunky,theExtend);
					PDFPattern myPat = this.pdfDoc.makePattern(2, myShad, null, null, null);*/
					PDFPattern myPat = this.pdfDoc.createGradient(false, aColorSpace, someColors,null,theCoords);
					currentStream.add(myPat.getColorSpaceOut(fill));
				} else if(gi instanceof SVGRadialGradientElement) {
					SVGRadialGradientElement radial = (SVGRadialGradientElement)gi;
					ColorSpace aColorSpace = new ColorSpace(ColorSpace.DEVICE_RGB);
					org.w3c.dom.NodeList nl = radial.getChildNodes();
					SVGStopElementImpl stop;
					Hashtable table = null;
					Vector someColors = new Vector();
					Vector theCoords = new Vector();
					Vector theBounds = new Vector();
					// todo handle gradient units
					SVGRect bbox = null;
					if(area instanceof SVGRectElement) {
						bbox = ((SVGRectElement)area).getBBox();
					}
					short units = radial.getGradientUnits().getBaseVal();
					switch(units) {
						case SVGUnitTypes.SVG_UNIT_TYPE_OBJECTBOUNDINGBOX:
						break;
						case SVGUnitTypes.SVG_UNIT_TYPE_UNKNOWN:
						default:
					}
					// the coords should be relative to the current object
					// check value types, eg. %
					if(bbox != null) {
						if(false) {
							theCoords.addElement(new Double(bbox.getX() +
								radial.getCx().getBaseVal().getValue() * bbox.getWidth()));
							theCoords.addElement(new Double(bbox.getY() +
								radial.getCy().getBaseVal().getValue() * bbox.getHeight()));
							theCoords.addElement(new Double(radial.getR().getBaseVal().getValue() *
								bbox.getHeight()));
							theCoords.addElement(new Double(bbox.getX() +
								radial.getFx().getBaseVal().getValue() * bbox.getWidth()));
							theCoords.addElement(new Double(bbox.getY() +
								radial.getFy().getBaseVal().getValue() * bbox.getHeight()));
							theCoords.addElement(new Double(radial.getR().getBaseVal().getValue() *
								bbox.getHeight()));
						} else {
							theCoords.addElement(new Double(-bbox.getX() + radial.getCx().getBaseVal().getValue()));
							theCoords.addElement(new Double(-bbox.getY() + radial.getCy().getBaseVal().getValue()));
							theCoords.addElement(new Double(radial.getR().getBaseVal().getValue()));
							theCoords.addElement(new Double(-bbox.getX() + radial.getFx().getBaseVal().getValue())); // Fx
							theCoords.addElement(new Double(-bbox.getY() + radial.getFy().getBaseVal().getValue())); // Fy
							theCoords.addElement(new Double(radial.getR().getBaseVal().getValue()));
/*							theCoords.addElement(new Double(bbox.getX() +
								radial.getCx().getBaseVal().getValue()));
							theCoords.addElement(new Double(bbox.getY() +
								radial.getCy().getBaseVal().getValue()));
							theCoords.addElement(new Double(radial.getR().getBaseVal().getValue()));
							theCoords.addElement(new Double(bbox.getX() +
								radial.getFx().getBaseVal().getValue()));
							theCoords.addElement(new Double(bbox.getY() +
								radial.getFy().getBaseVal().getValue()));
							theCoords.addElement(new Double(radial.getR().getBaseVal().getValue()));*/
						}
					} else {
						theCoords.addElement(new Double(radial.getCx().getBaseVal().getValue()));
						theCoords.addElement(new Double(radial.getCy().getBaseVal().getValue()));
						theCoords.addElement(new Double(radial.getR().getBaseVal().getValue()));
						theCoords.addElement(new Double(radial.getFx().getBaseVal().getValue())); // Fx
						theCoords.addElement(new Double(radial.getFy().getBaseVal().getValue())); // Fy
						theCoords.addElement(new Double(radial.getR().getBaseVal().getValue()));
					}
					float lastoffset = 0;
					for(int count = 0; count < nl.getLength(); count++) {
						stop = (SVGStopElementImpl)nl.item(count);
//						table = stop.oldgetStyle();
						ColorType sc = (ColorType)table.get("stop-color");
						if(sc == null) {
							// maybe using color
							sc = (ColorType)table.get("color");
						}
						if(sc == null) {
							// problems
							System.err.println("no stop-color or color in stop element");
							continue;
						}
						float offset = stop.getOffset().getBaseVal();
						PDFColor color = new PDFColor(sc.red(), sc.green(), sc.blue());
						// create bounds from last to offset
						lastoffset = offset;
						someColors.addElement(color);
					}
					PDFPattern myPat = this.pdfDoc.createGradient(true, aColorSpace, someColors,null,theCoords);

					currentStream.add(myPat.getColorSpaceOut(fill));
				} else {
					System.err.println("WARNING Invalid fill reference :" + gi + ":" + address);
				}
			}
		}
	}

	/*
	 * This sets up the style for drawing objects.
	 * Should only set style for elements that have changes.
	 *
	 */
	// need mask drawing
	class DrawingInstruction {
		boolean stroke = false;
		boolean nonzero = false; // non-zero fill rule "f*", "B*" operator
		boolean fill = false;
		int linecap = 0; // butt
		int linejoin = 0; // miter
		int miterwidth = 8;
	}
	protected DrawingInstruction applyStyle(SVGElement area, CSSStyleDeclaration style)
	{
		DrawingInstruction di = new DrawingInstruction();
		CSSValue sp;
		sp = style.getPropertyCSSValue("fill");
		if(sp != null) {
		    if(sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
			    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_RGBCOLOR) {
			        RGBColor col = ((CSSPrimitiveValue)sp).getRGBColorValue();
			        CSSPrimitiveValue val;
			        val = col.getRed();
			        float red = val.getFloatValue(CSSPrimitiveValue.CSS_NUMBER);
			        val = col.getGreen();
			        float green = val.getFloatValue(CSSPrimitiveValue.CSS_NUMBER);
				        val = col.getBlue();
			        float blue = val.getFloatValue(CSSPrimitiveValue.CSS_NUMBER);
					PDFColor fillColour = new PDFColor(red, green, blue);
					currentStream.add(fillColour.getColorSpaceOut(true));
					di.fill = true;
			    }
		    }
/*			if(sp instanceof ColorType) {
				ColorType ct = (ColorType)sp;
				PDFColor fillColour = new PDFColor(ct.red(), ct.green(), ct.blue());
				currentStream.add(fillColour.getColorSpaceOut(true));
			} else if (sp instanceof String) {
				if(sp.equals("none")) {
					di.fill = false;
				} else {
					handleGradient((String)sp, true, area);
				}
			}*/
		} else {
		}
		sp = style.getPropertyCSSValue("fill-rule");
		if(sp != null) {
		    if(sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
			    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_STRING) {
					if(sp.getCssText().equals("nonzero")) {
						di.nonzero = true;
					}
			    }
		    }
		} else {
		}
		sp = style.getPropertyCSSValue("stroke");
		if(sp != null) {
		    if(sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
			    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_RGBCOLOR) {
			        RGBColor col = ((CSSPrimitiveValue)sp).getRGBColorValue();
			        CSSPrimitiveValue val;
			        val = col.getRed();
			        float red = val.getFloatValue(CSSPrimitiveValue.CSS_NUMBER);
			        val = col.getGreen();
			        float green = val.getFloatValue(CSSPrimitiveValue.CSS_NUMBER);
				        val = col.getBlue();
			        float blue = val.getFloatValue(CSSPrimitiveValue.CSS_NUMBER);
					PDFColor fillColour = new PDFColor(red, green, blue);
					currentStream.add(fillColour.getColorSpaceOut(false));
					di.stroke = true;
			    }
		    }
/*			if(sp instanceof ColorType) {
				ColorType ct = (ColorType)sp;
				PDFColor fillColour = new PDFColor(ct.red(), ct.green(), ct.blue());
				currentStream.add(fillColour.getColorSpaceOut(false));
			} else if(sp instanceof String) {
				if(sp.equals("none")) {
					di.stroke = false;
				} else {
					handleGradient((String)sp, false, area);
				}
			}*/
		} else {
		}
		sp = style.getPropertyCSSValue("stroke-linecap");
		if(sp != null) {
		    if(sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
			    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_STRING) {
			        String str = sp.getCssText();
					// butt, round ,square
					if(str.equals("butt")) {
						currentStream.add(0 + " J\n");
					} else if(str.equals("round")) {
						currentStream.add(1 + " J\n");
					} else if(str.equals("square")) {
						currentStream.add(2 + " J\n");
					}
			    }
		    }
		} else {
		}
		sp = style.getPropertyCSSValue("stroke-linejoin");
		if(sp != null) {
		    if(sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
			    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_STRING) {
			        String str = sp.getCssText();
					if(str.equals("miter")) {
						currentStream.add(0 + " j\n");
					} else if(str.equals("round")) {
						currentStream.add(1 + " j\n");
					} else if(str.equals("bevel")) {
						currentStream.add(2 + " j\n");
					}
			    }
		    }
		} else {
		}
		sp = style.getPropertyCSSValue("stroke-miterlimit");
		if(sp != null) {
		    if(sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
				float width;
		        width = ((CSSPrimitiveValue)sp).getFloatValue(CSSPrimitiveValue.CSS_PT);
				PDFNumber pdfNumber = new PDFNumber();
				currentStream.add(pdfNumber.doubleOut(width) + " M\n");
			}
		} else {
		}
		sp = style.getPropertyCSSValue("stroke-width");
		if(sp != null) {
		    if(sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
				float width;
		        width = ((CSSPrimitiveValue)sp).getFloatValue(CSSPrimitiveValue.CSS_PT);
				PDFNumber pdfNumber = new PDFNumber();
				currentStream.add(pdfNumber.doubleOut(width) + " w\n");
			}
		}
		sp = style.getPropertyCSSValue("stroke-dasharray");
		if(sp != null) {
		    if(sp.getValueType() == CSSValue.CSS_VALUE_LIST) {
			    currentStream.add("[ ");
		        CSSValueList list = (CSSValueList)sp;
		        for(int count = 0; count < list.getLength(); count++) {
		            CSSValue val = list.item(count);
		            if(val.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
				        currentStream.add(((CSSPrimitiveValue)val).getFloatValue(CSSPrimitiveValue.CSS_NUMBER) + " ");
		            }
		        }
				currentStream.add("] ");
				sp = style.getPropertyCSSValue("stroke-offset");
				if(sp != null && sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
			        currentStream.add(((CSSPrimitiveValue)sp).getFloatValue(CSSPrimitiveValue.CSS_NUMBER) + " d\n");
				} else {
			        currentStream.add("0 d\n");
				}
		    }
/*			Vector list;
			list = (Vector)sp;
			currentStream.add("[ ");
			for(Enumeration e = list.elements(); e.hasMoreElements(); ) {
				Integer val = (Integer)e.nextElement();
				currentStream.add(val.intValue() + " ");
			}
			sp = style.getPropertyCSSValue("stroke-offset");
			if(sp != null) {
				float width;
				width = ((SVGLengthImpl)sp).getValue();
				PDFNumber pdfNumber = new PDFNumber();
				currentStream.add("] " + pdfNumber.doubleOut(width) + " d\n");
			} else {
				currentStream.add("] 0 d\n");
			}*/

		}
		sp = style.getPropertyCSSValue("mask");
		if(sp != null) {
			String maskurl;
//			maskurl = (String)sp;
//			System.out.println("mask: " + maskurl);
//			maskurl = maskurl.substring(1, maskurl.length());
			// get def of mask and set mask
//			SVGElement graph = null;
//			graph = area.locateDef(maskurl);
//			if(graph != null) {
//				System.out.println("mask: " + graph);
//				SVGElement parent = graph.getGraphicParent();
//				graph.setParent(area);
//				renderElement(svgarea, graph, posx, posy);
//				graph.setParent(parent);
//			}
		}
		return di;
	}

	// need to transform about the origin of the current object
	protected void applyTransform(SVGAnimatedTransformList trans, SVGRect bbox)
	{
		// need to rotate and scale about the bbox top left
		PDFNumber pdfNumber = new PDFNumber();
		if(bbox != null) {
//			currentStream.add("1 0 0 1 " + pdfNumber.doubleOut(bbox.getX()) + " " + pdfNumber.doubleOut(bbox.getY()) + " cm\n");
		}
		SVGTransformList list = trans.getBaseVal();
		for(int count = 0; count < list.getNumberOfItems(); count++) {
			SVGMatrix matrix = ((SVGTransform)list.getItem(count)).getMatrix();
			currentStream.add(pdfNumber.doubleOut(matrix.getA()) + " " + pdfNumber.doubleOut(matrix.getB()) + " " + pdfNumber.doubleOut(matrix.getC())
					+ " " + pdfNumber.doubleOut(matrix.getD()) + " " + pdfNumber.doubleOut(matrix.getE()) + " " + pdfNumber.doubleOut(matrix.getF()) + " cm\n");
		}
		if(bbox != null) {
//			currentStream.add("1 0 0 1 " + pdfNumber.doubleOut(-bbox.getX()) + " " + pdfNumber.doubleOut(-bbox.getY()) + " cm\n");
		}
	}

	public void renderElement(FontState fontState, SVGElement area, int posx, int posy)
	{
		int x = posx;
		int y = posy;
		CSSStyleDeclaration style = null;
		if(area instanceof SVGStylable)
			style = ((SVGStylable)area).getStyle();
		DrawingInstruction di = null;

		currentStream.add("q\n");
		if(area instanceof SVGTransformable) {
			SVGTransformable tf = (SVGTransformable)area;
			SVGAnimatedTransformList trans = tf.getTransform();
			SVGRect bbox = tf.getBBox();
			if(trans != null) {
				applyTransform(trans, bbox);
			}
		}

		if(style != null) {
			di = applyStyle(area, style);
		}

		if (area instanceof SVGRectElement) {
			SVGRectElement rg = (SVGRectElement)area;
			float rectx = rg.getX().getBaseVal().getValue();
			float recty = rg.getY().getBaseVal().getValue();
			float rx = rg.getRx().getBaseVal().getValue();
			float ry = rg.getRy().getBaseVal().getValue();
			float rw = rg.getWidth().getBaseVal().getValue();
			float rh = rg.getHeight().getBaseVal().getValue();
			addRect(rectx, recty, rw, rh, rx, ry, di);
		} else if (area instanceof SVGLineElement) {
			SVGLineElement lg = (SVGLineElement)area;
			float x1 = lg.getX1().getBaseVal().getValue();
			float y1 = lg.getY1().getBaseVal().getValue();
			float x2 = lg.getX2().getBaseVal().getValue();
			float y2 = lg.getY2().getBaseVal().getValue();
			addLine(x1,y1,x2,y2, di);
		} else if (area instanceof SVGTextElementImpl) {
//			currentStream.add("q\n");
//			currentStream.add(1 + " " + 0 + " " + 0 + " " + 1 + " " + 0 + " " + 0 + " cm\n");
			currentStream.add("BT\n");
			renderText(fontState, (SVGTextElementImpl)area, 0, 0/*, di*/);
			currentStream.add("ET\n");
//			currentStream.add("Q\n");
		} else if (area instanceof SVGCircleElement) {
			SVGCircleElement cg = (SVGCircleElement)area;
			float cx = cg.getCx().getBaseVal().getValue();
			float cy = cg.getCy().getBaseVal().getValue();
			float r = cg.getR().getBaseVal().getValue();
			addCircle(cx,cy,r, di);
		} else if (area instanceof SVGEllipseElement) {
			SVGEllipseElement cg = (SVGEllipseElement)area;
			float cx = cg.getCx().getBaseVal().getValue();
			float cy = cg.getCy().getBaseVal().getValue();
			float rx = cg.getRx().getBaseVal().getValue();
			float ry = cg.getRy().getBaseVal().getValue();
			addEllipse(cx,cy,rx,ry, di);
		} else if (area instanceof SVGPathElementImpl) {
			addPath(((SVGPathElementImpl)area).pathElements, posx, posy, di);
		} else if (area instanceof SVGPolylineElementImpl) {
			addPolyline(((SVGPolylineElementImpl)area).points, posx, posy, di, false);
		} else if (area instanceof SVGPolygonElementImpl) {
			addPolyline(((SVGPolygonElementImpl)area).points, posx, posy, di, true);
		} else if (area instanceof SVGGElementImpl) {
			renderGArea(fontState, (SVGGElementImpl)area, x, y);
		} else if(area instanceof SVGUseElementImpl) {
			SVGUseElementImpl ug = (SVGUseElementImpl)area;
			String ref = ug.link;
			ref = ref.substring(1, ref.length());
			SVGElement graph = null;
//			graph = area.locateDef(ref);
			if(graph != null) {
				// probably not the best way to do this, should be able
				// to render without the style being set.
//				SVGElement parent = graph.getGraphicParent();
//				graph.setParent(area);
				// need to clip (if necessary) to the use area
				// the style of the linked element is as if is was
				// a direct descendant of the use element.
				renderElement(fontState, graph, posx, posy);
//				graph.setParent(parent);
			}
		} else if (area instanceof SVGImageElementImpl) {
			SVGImageElementImpl ig = (SVGImageElementImpl)area;
			renderImage(fontState, ig.link, ig.x, ig.y, ig.width, ig.height);
		} else if (area instanceof SVGSVGElement) {
			// the x and y pos will be wrong!
			currentStream.add("q\n");
			renderSVG(fontState, (SVGSVGElement)area, x, y);
			currentStream.add("Q\n");
		} else if (area instanceof SVGAElement) {
			SVGAElement ael = (SVGAElement)area;
			org.w3c.dom.NodeList nl = ael.getChildNodes();
			for(int count = 0; count < nl.getLength(); count++) {
				org.w3c.dom.Node n = nl.item(count);
				if(n instanceof SVGElement) {
					if(n instanceof GraphicElement) {
						SVGRect rect = ((GraphicElement)n).getBBox();
						if(rect != null) {
/*							currentAnnotList = this.pdfDoc.makeAnnotList();
							currentPage.setAnnotList(currentAnnotList);
							String dest = linkSet.getDest();
							int linkType = linkSet.getLinkType();
							currentAnnotList.addLink(
								this.pdfDoc.makeLink(lrect.getRectangle(), dest, linkType));
							currentAnnotList = null;
*/						}
					}
					renderElement(fontState, (SVGElement)n, posx, posy);
				}
			}
		} else if (area instanceof SVGSwitchElement) {
			handleSwitchElement(fontState, posx, posy, (SVGSwitchElement)area);
		}
		// should be done with some cleanup code, so only
		// required values are reset.
		currentStream.add("Q\n");
	}

	public void renderText(FontState fontState, SVGTextElementImpl tg, float x, float y)
	{
		PDFNumber pdfNumber = new PDFNumber();

		CSSStyleDeclaration styles;
		styles = tg.getStyle();
		applyStyle(tg, styles);
		// apply transform
		// text has a Tm and need to handle each element
		SVGTransformList trans = tg.getTransform().getBaseVal();
		SVGMatrix matrix = trans.consolidate().getMatrix();
		String transstr = (pdfNumber.doubleOut(matrix.getA())
							+ " " + pdfNumber.doubleOut(matrix.getB())
							+ " " + pdfNumber.doubleOut(matrix.getC())
							+ " " + pdfNumber.doubleOut(-matrix.getD()) + " ");

		String fontFamily = null;
		CSSValue sp = styles.getPropertyCSSValue("font-family");
	    if(sp != null && sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
		    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_STRING) {
				fontFamily = sp.getCssText();
		    }
	    }
		if(fontFamily == null)
			fontFamily = fontState.getFontFamily();
		String fontStyle = null;
		sp = styles.getPropertyCSSValue("font-style");
	    if(sp != null && sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
		    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_STRING) {
				fontStyle = sp.getCssText();
		    }
	    }
		if(fontStyle == null)
			fontStyle = fontState.getFontStyle();
		String fontWeight = null;
		sp = styles.getPropertyCSSValue("font-weight");
	    if(sp != null && sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
		    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_STRING) {
				fontWeight = sp.getCssText();
		    }
	    }
		if(fontWeight == null)
			fontWeight = fontState.getFontWeight();
		float fontSize;
		sp = styles.getPropertyCSSValue("font-size");
	    if(sp != null && sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
//		    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_NUMBER) {
				fontSize = ((CSSPrimitiveValue)sp).getFloatValue(CSSPrimitiveValue.CSS_PT);
//		    }
	    } else {
			fontSize = fontState.getFontSize() / 1000f;
		}
		FontState fs = fontState;
		try {
			fs = new FontState(fontState.getFontInfo(), fontFamily, fontStyle,
									fontWeight, (int)(fontSize * 1000));
		} catch(Exception fope) {
//			fope.printStackTrace();
		}

		currentStream.add("/" + fs.getFontName() + " " + fontSize + " Tf\n");

		float tx = tg.x;
		float ty = tg.y;
		float currentX = x + tx;
		float currentY = y + ty;
		Vector list = tg.textList;
		for(Enumeration e = list.elements(); e.hasMoreElements(); ) {
			Object o = e.nextElement();
			styles = tg.getStyle();
			applyStyle(tg, styles);
			if(o instanceof String) {
				String str = (String)o;
				currentStream.add(transstr
					+ (currentX + matrix.getE()) + " "
					+ (y+ty + matrix.getF()) + " Tm " 
					+ "(");
				boolean spacing = "preserve".equals(tg.getXMLspace());
				currentX = addSVGStr(fs, currentX, str, spacing);
				currentStream.add(") Tj\n");
//				for(int count = 0; count < str.length(); count++) {
//				}
//				currentX += fs.width(' ') / 1000f;
			} else if(o instanceof SVGTextPathElementImpl) {
				SVGTextPathElementImpl tpg = (SVGTextPathElementImpl)o;
				String ref = tpg.str;
				SVGElement graph = null;
//				graph = tpg.locateDef(ref);
				if(graph != null && graph instanceof SVGPathElementImpl) {
					// probably not the best way to do this, should be able
					// to render without the style being set.
//					GraphicImpl parent = graph.getGraphicParent();
//					graph.setParent(tpg);
					// set text path??
					// how should this work
//					graph.setParent(parent);
				}
			} else if(o instanceof SVGTRefElementImpl) {
				SVGTRefElementImpl trg = (SVGTRefElementImpl)o;
				String ref = trg.ref;
				ref = ref.substring(1, ref.length());
				SVGElement graph = null;
//				graph = trg.locateDef(ref);
				if(graph != null && graph instanceof SVGTextElementImpl) {
//					GraphicImpl parent = graph.getGraphicParent();
//					graph.setParent(trg);
					SVGTextElementImpl te = (SVGTextElementImpl)graph;
					renderText(fs, te, (int)(x + tx), (int)(y + ty));
//					graph.setParent(parent);
				}
			} else if(o instanceof SVGTSpanElementImpl) {
				SVGTSpanElementImpl tsg = (SVGTSpanElementImpl)o;
				styles = tsg.getStyle();
				applyStyle(tsg, styles);
				boolean changed = false;

				String newprop = null;
				sp = styles.getPropertyCSSValue("font-family");
			    if(sp != null && sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
				    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_STRING) {
						newprop = sp.getCssText();
				    }
			    }
				if(newprop != null && !newprop.equals(fontFamily)) {
					fontFamily = newprop;
					changed = true;
				}
				sp = styles.getPropertyCSSValue("font-style");
			    if(sp != null && sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
				    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_STRING) {
						newprop = sp.getCssText();
				    }
			    }
				if(newprop != null && !newprop.equals(fontStyle)) {
					fontStyle = newprop;
					changed = true;
				}
				sp = styles.getPropertyCSSValue("font-weight");
			    if(sp != null && sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
				    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_STRING) {
						newprop = sp.getCssText();
				    }
			    }
				if(newprop != null && !newprop.equals(fontWeight)) {
					fontWeight = newprop;
					changed = true;
				}
				float newSize = fontSize;
				sp = styles.getPropertyCSSValue("font-size");
			    if(sp != null && sp.getValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
//				    if(((CSSPrimitiveValue)sp).getPrimitiveType() == CSSPrimitiveValue.CSS_NUMBER) {
						newSize = ((CSSPrimitiveValue)sp).getFloatValue(CSSPrimitiveValue.CSS_PT);
//				    }
			    }
				if(fontSize != newSize) {
					fontSize = newSize;
					changed = true;
				}
				FontState oldfs = null;
				if(changed) {
					oldfs = fs;
					try {
						fs = new FontState(fontState.getFontInfo(), fontFamily, fontStyle,
											fontWeight, (int)(fontSize * 1000));
					} catch(Exception fope) {
					}

					currentStream.add("/" + fs.getFontName() + " " + fontSize + " Tf\n");
				}

				float baseX;
				float baseY;

				StringBuffer pdf = new StringBuffer();
				boolean spacing = "preserve".equals(tsg.getXMLspace());
				boolean inbetween = false;
				boolean addedspace = false;
				int charPos = 0;
				float xpos = currentX;
				float ypos = currentY;
				for (int i=0; i < tsg.str.length(); i++) {
					char ch = tsg.str.charAt(i);
					xpos = currentX;
					ypos = currentY;
					if(tsg.ylist.size() > charPos) {
						ypos = y + ty + ((Float)tsg.ylist.elementAt(charPos)).floatValue();
					}
					if(tsg.dylist.size() > charPos) {
						ypos = ypos + ((Float)tsg.dylist.elementAt(charPos)).floatValue();
					}
					if(tsg.xlist.size() > charPos) {
						xpos = x + tx + ((Float)tsg.xlist.elementAt(charPos)).floatValue();
					}
					if(tsg.dxlist.size() > charPos) {
						xpos = xpos + ((Float)tsg.dxlist.elementAt(charPos)).floatValue();
					}
					if (ch > 127) {
							pdf = pdf.append(transstr
								+ (xpos + matrix.getE()) + " "
								+ (ypos + matrix.getF()) + " Tm " 
								+ "(" + "\\" + Integer.toOctalString((int)ch) + ") Tj\n");
						currentX = xpos + fs.width(ch) / 1000f;
						currentY = ypos;
						charPos++;
						inbetween = true;
				        addedspace = false;
					} else {
						switch (ch) {
						case '('  :
							pdf = pdf.append(transstr
								+ (xpos + matrix.getE()) + " "
								+ (ypos + matrix.getF()) + " Tm " 
								+ "(" + "\\(" + ") Tj\n");
							currentX = xpos + fs.width(ch) / 1000f;
							currentY = ypos;
							charPos++;
							inbetween = true;
					        addedspace = false;
						break;
						case ')'  :
							pdf = pdf.append(transstr
								+ (xpos + matrix.getE()) + " "
								+ (ypos + matrix.getF()) + " Tm " 
								+ "(" + "\\)" + ") Tj\n");
							currentX = xpos + fs.width(ch) / 1000f;
							currentY = ypos;
							charPos++;
							inbetween = true;
					        addedspace = false;
						break;
						case '\\' :
							pdf = pdf.append(transstr
								+ (xpos + matrix.getE()) + " "
								+ (ypos + matrix.getF()) + " Tm " 
								+ "(" + "\\\\" + ") Tj\n");
							currentX = xpos + fs.width(ch) / 1000f;
							currentY = ypos;
							charPos++;
							inbetween = true;
					        addedspace = false;
						break;
						case '	':
						case ' ':
						    if(spacing) {
								currentX = xpos + fs.width(' ') / 1000f;
								currentY = ypos;
								charPos++;
						    } else {
						        if(inbetween && !addedspace) {
							        addedspace = true;
									currentX = xpos + fs.width(' ') / 1000f;
									currentY = ypos;
									charPos++;
						        }
						    }
						break;
						case '\n':
						case '\r':
						    if(spacing) {
								currentX = xpos + fs.width(' ') / 1000f;
								currentY = ypos;
								charPos++;
						    }
						break;
						default:
					        addedspace = false;
							pdf = pdf.append(transstr
								+ (xpos + matrix.getE()) + " "
								+ (ypos + matrix.getF()) + " Tm " 
								+ "(" + ch + ") Tj\n");
							currentX = xpos + fs.width(ch) / 1000f;
							currentY = ypos;
							charPos++;
							inbetween = true;
						break;
					}
				}
				currentStream.add(pdf.toString());
				}
//				currentX += fs.width(' ') / 1000f;
				if(changed) {
					fs = oldfs;
					currentStream.add("/" + fs.getFontName() + " " + fs.getFontSize() / 1000f + " Tf\n");
				}
			} else {
				System.err.println("Error: unknown text element " + o);
			}
		}
	}

	protected float addSVGStr(FontState fs, float currentX, String str, boolean spacing)
	{
		boolean inbetween = false;
		boolean addedspace = false;
		StringBuffer pdf = new StringBuffer();
		for (int i=0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (ch > 127) {
				pdf = pdf.append("\\");
				pdf = pdf.append(Integer.toOctalString((int)ch));
				currentX += fs.width(ch) / 1000f;
				inbetween = true;
		        addedspace = false;
			} else {
				switch (ch) {
				case '('  :
					pdf = pdf.append("\\(");
					currentX += fs.width(ch) / 1000f;
					inbetween = true;
			        addedspace = false;
				break;
				case ')'  :
					pdf = pdf.append("\\)");
					currentX += fs.width(ch) / 1000f;
					inbetween = true;
			        addedspace = false;
				break;
				case '\\' :
					pdf = pdf.append("\\\\");
					currentX += fs.width(ch) / 1000f;
					inbetween = true;
			        addedspace = false;
				break;
				case '	':
				case ' ':
				    if(spacing) {
				        pdf = pdf.append(' ');
						currentX += fs.width(' ') / 1000f;
				    } else {
				        if(inbetween && !addedspace) {
					        addedspace = true;
					        pdf = pdf.append(' ');
							currentX += fs.width(' ') / 1000f;
				        }
				    }
				break;
				case '\n':
				case '\r':
				    if(spacing) {
				        pdf = pdf.append(' ');
						currentX += fs.width(' ') / 1000f;
				    }
				break;
				default:
			        addedspace = false;
					pdf = pdf.append(ch);
					currentX += fs.width(ch) / 1000f;
					inbetween = true;
				break;
				}
			}
		}
		currentStream.add(pdf.toString());
		return currentX;
	}

}
