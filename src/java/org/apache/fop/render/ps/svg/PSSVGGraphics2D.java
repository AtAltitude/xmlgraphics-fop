/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.render.ps.svg;

import java.awt.Graphics;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.batik.ext.awt.LinearGradientPaint;
import org.apache.batik.ext.awt.RadialGradientPaint;

import org.apache.xmlgraphics.java2d.ps.PSGraphics2D;
import org.apache.xmlgraphics.ps.PSGenerator;

import org.apache.fop.render.shading.Function;
import org.apache.fop.render.shading.GradientRegistrar;
import org.apache.fop.render.shading.PSGradientFactory;
import org.apache.fop.render.shading.Pattern;
import org.apache.fop.render.shading.Shading;


public class PSSVGGraphics2D extends PSGraphics2D implements GradientRegistrar {

    private static final Log LOG = LogFactory.getLog(PSSVGGraphics2D.class);

    /**
     * Create a new Graphics2D that generates PostScript code.
     * @param textAsShapes True if text should be rendered as graphics
     * @see org.apache.xmlgraphics.java2d.AbstractGraphics2D#AbstractGraphics2D(boolean)
     */
    public PSSVGGraphics2D(boolean textAsShapes) {
        super(textAsShapes);
    }

    /**
     * Create a new Graphics2D that generates PostScript code.
     * @param textAsShapes True if text should be rendered as graphics
     * @param gen PostScript generator to use for output
     * @see org.apache.xmlgraphics.java2d.AbstractGraphics2D#AbstractGraphics2D(boolean)
     */
    public PSSVGGraphics2D(boolean textAsShapes, PSGenerator gen) {
        super(textAsShapes, gen);
    }

    /**
     * Constructor for creating copies
     * @param g parent PostScript Graphics2D
     */
    public PSSVGGraphics2D(PSGraphics2D g) {
        super(g);
    }

    protected void applyPaint(Paint paint, boolean fill) {
        super.applyPaint(paint, fill);
        if (paint instanceof LinearGradientPaint) {
            Pattern pattern = new PSGradientFactory()
                    .createLinearGradient((LinearGradientPaint) paint, getBaseTransform(), getTransform());
            try {
                gen.write(pattern.toString());
            } catch (IOException ioe) {
                handleIOException(ioe);
            }
        } else if (paint instanceof RadialGradientPaint) {
            Pattern pattern = new PSGradientFactory()
                    .createRadialGradient((RadialGradientPaint) paint, getBaseTransform(), getTransform());
            try {
                gen.write(pattern.toString());
            } catch (IOException ioe) {
                handleIOException(ioe);
            }
        }
    }

    protected AffineTransform getBaseTransform() {
        AffineTransform at = new AffineTransform(this.getTransform());
        return at;
    }

    /**
     * Creates a new <code>Graphics</code> object that is
     * a copy of this <code>Graphics</code> object.
     * @return     a new graphics context that is a copy of
     * this graphics context.
     */
    @Override
    public Graphics create() {
        preparePainting();
        return new PSSVGGraphics2D(this);
    }

    /**
     * Registers a function object against the output format document
     * @param function The function object to register
     * @return Returns either the function which has already been registered
     * or the current new registered object.
     */
    public Function registerFunction(Function function) {
        //Objects aren't needed to be registered in Postscript
        return function;
    }

    /**
     * Registers a shading object against the otuput format document
     * @param shading The shading object to register
     * @return Returs either the shading which has already been registered
     * or the current new registered object
     */
    public Shading registerShading(Shading shading) {
        //Objects aren't needed to be registered in Postscript
        return shading;
    }

    /**
     * Registers a pattern object against the output format document
     * @param pattern The pattern object to register
     * @return Returns either the pattern which has already been registered
     * or the current new registered object
     */
    public Pattern registerPattern(Pattern pattern) {
        // TODO Auto-generated method stub
        return pattern;
    }
}
