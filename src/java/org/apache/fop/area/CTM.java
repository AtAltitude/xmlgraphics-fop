/*
 * $Id: CTM.java,v 1.8 2003/03/05 15:19:31 jeremias Exp $
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */ 
package org.apache.fop.area;

import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;
import java.io.Serializable;

import org.apache.fop.fo.properties.WritingMode;

/**
 * Describe a PDF or PostScript style coordinate transformation matrix (CTM).
 * The matrix encodes translations, scaling and rotations of the coordinate
 * system used to render pages.
 */
public class CTM implements Serializable {
    
    private double a, b, c, d, e, f;

    private static final CTM CTM_LRTB = new CTM(1, 0, 0, 1, 0, 0);
    private static final CTM CTM_RLTB = new CTM(-1, 0, 0, 1, 0, 0);
    private static final CTM CTM_TBRL = new CTM(0, 1, -1, 0, 0, 0);

    /**
     * Create the identity matrix
     */
    public CTM() {
        a = 1;
        b = 0;
        c = 0;
        d = 1;
        e = 0;
        f = 0;
    }

    /**
     * Initialize a CTM from the passed arguments.
     *
     * @param a the x scale
     * @param b the x shear
     * @param c the y shear
     * @param d the y scale
     * @param e the x shift
     * @param f the y shift
     */
    public CTM(double a, double b, double c, double d, double e, double f) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
    }

    /**
     * Initialize a CTM to the identity matrix with a translation
     * specified by x and y
     *
     * @param x the x shift
     * @param y the y shift.
     */
    public CTM(double x, double y) {
        this.a = 1;
        this.b = 0;
        this.c = 0;
        this.d = 1;
        this.e = x;
        this.f = y;
    }

    /**
     * Initialize a CTM with the values of another CTM.
     *
     * @param ctm another CTM
     */
    protected CTM(CTM ctm) {
        this.a = ctm.a;
        this.b = ctm.b;
        this.c = ctm.c;
        this.d = ctm.d;
        this.e = ctm.e;
        this.f = ctm.f;
    }

    /**
     * Return a CTM which will transform coordinates for a particular writing-mode
     * into normalized first quandrant coordinates.
     * @param wm A writing mode constant from fo.properties.WritingMode, ie.
     * one of LR_TB, RL_TB, TB_RL.
     * @param ipd The inline-progression dimension of the reference area whose
     * CTM is being set..
     * @param bpd The block-progression dimension of the reference area whose
     * CTM is being set.
     * @return a new CTM with the required transform
     */
    public static CTM getWMctm(int wm, int ipd, int bpd) {
        CTM wmctm;
        switch (wm) {
            case WritingMode.LR_TB:
                return new CTM(CTM_LRTB);
            case WritingMode.RL_TB:
                {
                    wmctm = new CTM(CTM_RLTB);
                    wmctm.e = ipd;
                    return wmctm;
                }
                //return  CTM_RLTB.translate(ipd, 0);
            case WritingMode.TB_RL: // CJK
                {
                    wmctm = new CTM(CTM_TBRL);
                    wmctm.e = bpd;
                    return wmctm;
                }
                //return CTM_TBRL.translate(0, ipd);
            default:
                return null;
        }
    }

    /**
     * Multiply new passed CTM with this one and generate a new result CTM.
     * @param premult The CTM to multiply with this one. The new one will be
     * the first multiplicand.
     * @return CTM The result of multiplying premult * this.
     */
    public CTM multiply(CTM premult) {
        CTM rslt = new CTM ((premult.a * a) + (premult.b * c),
                            (premult.a * b) + (premult.b * d),
                            (premult.c * a) + (premult.d * c),
                            (premult.c * b) + (premult.d * d),
                            (premult.e * a) + (premult.f * c) + e,
                            (premult.e * b) + (premult.f * d) + f);
        return rslt;
    }

    /**
     * Rotate this CTM by "angle" radians and return a new result CTM.
     * This is used to account for reference-orientation.
     * @param angle The angle in radians. Positive angles are measured counter-
     * clockwise.
     * @return CTM The result of rotating this CTM.
     */
    public CTM rotate(double angle) {
        double cos, sin;
        if (angle == 90.0) {
            cos = 0.0;
            sin = 1.0;
        } else if (angle == 270.0) {
            cos = 0.0;
            sin = -1.0;
        } else if (angle == 180.0) {
            cos = -1.0;
            sin = 0.0;
        } else {
            double rad = Math.toRadians(angle);
            cos = Math.cos(rad);
            sin = Math.sin(rad);
        }
        CTM rotate = new CTM(cos, -sin, sin, cos, 0, 0);
        return multiply(rotate);
    }

    /**
     * Translate this CTM by the passed x and y values and return a new result CTM.
     * @param x The amount to translate along the x axis.
     * @param y The amount to translate along the y axis.
     * @return CTM The result of translating this CTM.
     */
    public CTM translate(double x, double y) {
        CTM translate = new CTM(1, 0, 0, 1, x, y);
        return multiply(translate);
    }

    /**
     * Scale this CTM by the passed x and y values and return a new result CTM.
     * @param x The amount to scale along the x axis.
     * @param y The amount to scale along the y axis.
     * @return CTM The result of scaling this CTM.
     */
    public CTM scale(double x, double y) {
        CTM scale = new CTM(x, 0, 0, y, 0, 0);
        return multiply(scale);
    }

    /**
     * Transform a rectangle by the CTM to produce a rectangle in the transformed
     * coordinate system.
     * @param inRect The rectangle in the original coordinate system
     * @return Rectangle2D The rectangle in the transformed coordinate system.
     */
    public Rectangle2D transform(Rectangle2D inRect) {
        // Store as 2 sets of 2 points and transform those, then
        // recalculate the width and height
        int x1t = (int)(inRect.getX() * a + inRect.getY() * c + e);
        int y1t = (int)(inRect.getX() * b + inRect.getY() * d + f);
        int x2t = (int)((inRect.getX() + inRect.getWidth()) * a
                        + (inRect.getY() + inRect.getHeight()) * c + e);
        int y2t = (int)((inRect.getX() + inRect.getWidth()) * b
                        + (inRect.getY() + inRect.getHeight()) * d + f);
        // Normalize with x1 < x2
        if (x1t > x2t) {
            int tmp = x2t;
            x2t = x1t;
            x1t = tmp;
        }
        if (y1t > y2t) {
            int tmp = y2t;
            y2t = y1t;
            y1t = tmp;
        }
        return new Rectangle(x1t, y1t, x2t - x1t, y2t - y1t);
    }

    /**
     * Get string for this transform.
     *
     * @return a string with the transform values
     */
    public String toString() {
        return "[" + a + " " + b + " " + c + " " + d + " " + e + " "
               + f + "]";
    }

    /**
     * Get an array containing the values of this transform.
     * This creates and returns a new transform with the values in it.
     *
     * @return an array containing the transform values
     */
    public double[] toArray() {
        return new double[]{a, b, c, d, e, f};
    }
}

