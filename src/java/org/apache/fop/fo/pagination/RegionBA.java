/*
 * Copyright 1999-2004 The Apache Software Foundation.
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

/* $Id$ */

package org.apache.fop.fo.pagination;

// Java
import java.awt.Rectangle;

// XML
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

// FOP
import org.apache.fop.fo.FONode;

/**
 * Abstract base class for fo:region-before and fo:region-after.
 */
public abstract class RegionBA extends Region {

    private boolean bPrecedence;

    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    protected RegionBA(FONode parent, int regionId) {
        super(parent, regionId);
    }

    protected void addProperties(Attributes attlist) throws SAXParseException {
        super.addProperties(attlist);
        
        bPrecedence =
            (this.propertyList.get(PR_PRECEDENCE).getEnum() == Precedence.TRUE);

        this.extent = getPropLength(PR_EXTENT);
    }

    /**
     * @see org.apache.fop.fo.pagination.Region#getPrecedence()
     */
    public boolean getPrecedence() {
        return bPrecedence;
    }

    /**
     * Adjust the viewport reference rectangle for a region as a function
     * of precedence.
     * If precedence is false on a before or after region, its
     * inline-progression-dimension is limited by the extent of the start
     * and end regions if they are present.
     * @param vpRefRect viewport reference rectangle
     * @param wm writing mode
     */
    protected void adjustIPD(Rectangle vpRefRect, int wm) {
        int offset = 0;
        Region start = getSiblingRegion(Region.START);
        if (start != null) {
            offset = start.getExtent();
            vpRefRect.translate(offset, 0);
        }
        Region end = getSiblingRegion(Region.END);
        if (end != null) {
            offset += end.getExtent();
        }
        if (offset > 0) {
            if (wm == WritingMode.LR_TB || wm == WritingMode.RL_TB) {
                vpRefRect.width -= offset;
            } else {
                vpRefRect.height -= offset;
            }
        }
    }
}

