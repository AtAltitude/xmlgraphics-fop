/*
 * $Id: RegionStart.java,v 1.12 2003/03/11 04:08:17 vmote Exp $
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
package org.apache.fop.fo.pagination;

// Java
import java.awt.Rectangle;

// FOP
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.properties.WritingMode;
import org.apache.fop.datatypes.FODimension;
import org.apache.fop.area.RegionReference;

/**
 * The fo:region-start element.
 */
public class RegionStart extends RegionSE {

    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    public RegionStart(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.pagination.Region#getViewportRectangle(FODimension)
     */
    protected Rectangle getViewportRectangle (FODimension reldims) {
        // Depends on extent, precedence and writing mode
        // This is the rectangle relative to the page-reference area in
        // writing-mode relative coordinates
        Rectangle vpRect;
        if (this.wm == WritingMode.LR_TB || this.wm == WritingMode.RL_TB) {
            vpRect = new Rectangle(0, 0, getExtent(), reldims.bpd);
        } else {
            vpRect = new Rectangle(0, 0, reldims.bpd, getExtent());
        }
        adjustIPD(vpRect, this.wm);
        return vpRect;
    }

    /**
     * @see org.apache.fop.fo.pagination.Region#getDefaultRegionName()
     */
    protected String getDefaultRegionName() {
        return "xsl-region-start";
    }

    /**
     * @see org.apache.fop.fo.pagination.Region#getRegionClass()
     */
    public String getRegionClass() {
        return Region.START;
    }

    /**
     * @see org.apache.fop.fo.pagination.Region#getRegionAreaClass()
     */
    public int getRegionAreaClass() {
        return RegionReference.START;
    }
}

