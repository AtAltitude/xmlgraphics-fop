/*
 * $Id: BlockViewport.java,v 1.3 2003/03/05 15:19:31 jeremias Exp $
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

/**
 * A BlockViewport.
 * This is used for block level Viewport/reference pairs.
 * The block-container creates this area.
 */
public class BlockViewport extends Block {
    // clipping for this viewport
    private boolean clip = false;
    // transform if rotated or absolute
    private CTM viewportCTM;

    /**
     * Create a new block viewport area.
     */
    public BlockViewport() {
    }

    /**
     * Set the transform of this viewport.
     * If the viewport is rotated or has an absolute positioning
     * this transform will do the work.
     *
     * @param ctm the transformation
     */
    public void setCTM(CTM ctm) {
        viewportCTM = ctm;
    }

    /**
     * Get the transform of this block viewport.
     *
     * @return the transformation of this viewport
     *         or null if normally stacked without rotation
     */
    public CTM getCTM() {
        return viewportCTM;
    }

    /**
     * Set the clipping for this viewport.
     *
     * @param cl the clipping for the viewport
     */
    public void setClip(boolean cl) {
        clip = cl;
    }

    /**
     * Get the clipping for this viewport.
     *
     * @return the clipping for the viewport
     *         true if the contents should be clipped for this viewport
     */
    public boolean getClip() {
        return clip;
    }
}

