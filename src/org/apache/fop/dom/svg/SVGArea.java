/*-- $Id$ -- 

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================
 
    Copyright (C) 1999 The Apache Software Foundation. All rights reserved.
 
 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:
 
 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.
 
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
 
 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.
 
 4. The names "Fop" and  "Apache Software Foundation"  must not be used to
    endorse  or promote  products derived  from this  software without  prior
    written permission. For written permission, please contact
    apache@apache.org.
 
 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.
 
 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
 Software Foundation, please see <http://www.apache.org/>.
 
 */
package org.apache.fop.dom.svg;

// FOP
import org.apache.fop.render.Renderer;
import org.apache.fop.layout.FontState;
import org.apache.fop.layout.Area;
import org.apache.fop.dom.svg.*;

import java.util.*;

import org.w3c.dom.svg.*;
import org.w3c.dom.*;


/**
 * class representing an SVG area in which the SVG graphics sit
 */
public class SVGArea extends Area implements GraphicImpl, GetSVGDocument {//, SVGSVGElement {

	public SVGDocument getSVGDocument() throws DOMException
	{
		return null;
	}

	/**
	 * construct an SVG area
	 *
	 * @param fontState the font state
	 * @param width the width of the area
	 * @param height the height of the area
	 */
	public SVGArea(FontState fontState, float width, float height)  {
		super(fontState, (int)width * 1000, (int)height * 1000);
		currentHeight = (int)height * 1000;
		contentRectangleWidth = (int)width * 1000;
	}

	/**
	 * add a graphic.
	 *
	 * Graphics include SVG Rectangles, Lines and Text
	 *
	 * @param graphic the Graphic to add
	 */
	public void addGraphic(GraphicImpl graphic) {
		graphic.setParent(this);
		this.children.addElement(graphic);
	}

	/**
	 * render the SVG.
	 *
	 * @param renderer the Renderer to use
	 */
	public void render(Renderer renderer) {
//		renderer.renderSVGArea(this);
	}


	Hashtable defs = new Hashtable();
	public void addDefs(Hashtable table)
	{
		for(Enumeration e = table.keys(); e.hasMoreElements(); ) {
			String str = (String)e.nextElement();
			defs.put(str, table.get(str));
		}
	}

	public Hashtable getDefs()
	{
		Hashtable ret = null;
		if(parent != null) {
			ret = parent.getDefs();
			if(ret != null)
				ret = (Hashtable)ret.clone();
		}
		if(ret == null) {
			ret = defs;
		} else {
			if(defs != null) {
				for(Enumeration e = defs.keys(); e.hasMoreElements(); ) {
					String str = (String)e.nextElement();
					ret.put(str, defs.get(str));
				}
			}
		}
		return ret;
	}

	public GraphicImpl locateDef(String str)
	{
		Object obj = null;
		if(defs != null) {
			obj = defs.get(str);
		}
		if(obj == null) {
			Enumeration e = getChildren().elements();
			while (e.hasMoreElements()) {
				Object o = e.nextElement();
				if(o instanceof SVGElement) {
					String s;
					s = ((SVGElement)o).getId();
					if(str.equals(s)) {
						obj = o;
						break;
					}
				}
			}
		}
		if(obj == null && parent != null) {
			obj = parent.locateDef(str);
		}
		return (GraphicImpl)obj;
	}

	public Hashtable getStyle()
	{
		Hashtable ret = null;
		if(parent != null) {
			ret = parent.getStyle();
			if(ret != null)
				ret = (Hashtable)ret.clone();
		}
		if(ret == null) {
			ret = style;
		} else {
			if(style != null) {
				for(Enumeration e = style.keys(); e.hasMoreElements(); ) {
					String str = (String)e.nextElement();
					ret.put(str, style.get(str));
				}
			}
		}
		return ret;
	}

	public Vector oldgetTransform()
	{
		return trans;
/*		Vector ret = null;
		if(parent != null) {
			ret = parent.oldgetTransform();
			if(ret != null)
				ret = (Vector)ret.clone();
		}
		if(ret == null) {
			ret = trans;
		} else {
			if(trans != null) {
				for(Enumeration e = trans.elements(); e.hasMoreElements(); ) {
					Object o = e.nextElement();
					ret.addElement(o);
				}
			}
		}
		return ret;*/
	}

	Hashtable style = null;
	public void setStyle(Hashtable st)
	{
		style = st;
	}

	Vector trans = null;
	public void setTransform(Vector tr)
	{
		trans = tr;
	}

	GraphicImpl parent = null;
	public void setParent(GraphicImpl g)
	{
		parent = g;
	}

	public GraphicImpl getGraphicParent()
	{
		return parent;
	}
}
