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

 4. The names "FOP" and  "Apache Software Foundation"  must not be used to
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

package org.apache.fop.svg;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.layout.Area;
import org.apache.fop.layout.FontState;
import org.apache.fop.apps.FOPException;

import org.apache.fop.dom.svg.*;
import org.apache.fop.dom.svg.SVGArea;

import org.w3c.dom.svg.SVGLength;
import org.w3c.dom.svg.SVGElement;

/**
 * class representing svg:Ellipse pseudo flow object.
 *
 */
public class Ellipse extends SVGObj {

    /**
     * inner class for making Ellipse objects.
     */
    public static class Maker extends FObj.Maker {

        /**
         * make a Ellipse object.
         *
         * @param parent the parent formatting object
         * @param propertyList the explicit properties of this object
         *
         * @return the Ellipse object
         */
        public FObj make(FObj parent,
                         PropertyList propertyList) throws FOPException {
            return new Ellipse(parent, propertyList);
        }
    }

    /**
     * returns the maker for this object.
     *
     * @return the maker for Ellipse objects
     */
    public static FObj.Maker maker() {
        return new Ellipse.Maker();
    }

    /**
     * constructs a Ellipse object (called by Maker).
     *
     * @param parent the parent formatting object
     * @param propertyList the explicit properties of this object
     */
    protected Ellipse(FObj parent, PropertyList propertyList) {
        super(parent, propertyList);
        this.name = "svg:ellipse";
    }

    public SVGElement createGraphic() {
        /* retrieve properties */
        SVGLength cx = ((SVGLengthProperty) this.properties.get("cx")).
                       getSVGLength();
        if (cx == null)
            cx = new SVGLengthImpl();
        SVGLength cy = ((SVGLengthProperty) this.properties.get("cy")).
                       getSVGLength();
        if (cy == null)
            cy = new SVGLengthImpl();
        SVGLength rx = ((SVGLengthProperty) this.properties.get("rx")).
                       getSVGLength();
        if (rx == null)
            rx = new SVGLengthImpl();
        SVGLength ry = ((SVGLengthProperty) this.properties.get("ry")).
                       getSVGLength();
        if (ry == null)
            ry = new SVGLengthImpl();
        SVGEllipseElementImpl graph = new SVGEllipseElementImpl();
        graph.setCx(new SVGAnimatedLengthImpl(cx));
        graph.setCy(new SVGAnimatedLengthImpl(cy));
        graph.setRx(new SVGAnimatedLengthImpl(rx));
        graph.setRy(new SVGAnimatedLengthImpl(ry));
        graph.setStyle(
          ((SVGStyle) this.properties.get("style")).getStyle());
        graph.setTransform(
          ((SVGTransform) this.properties.get("transform")).
          getTransform());
        graph.setId(this.properties.get("id").getString());
        return graph;
    }
}
