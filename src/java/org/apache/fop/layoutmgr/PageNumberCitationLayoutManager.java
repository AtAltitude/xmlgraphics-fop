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

package org.apache.fop.layoutmgr;

import org.apache.fop.fo.flow.PageNumberCitation;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Resolveable;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.UnresolvedPageNumber;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.fonts.Font;

/**
 * LayoutManager for the fo:page-number-citation formatting object
 */
public class PageNumberCitationLayoutManager extends LeafNodeLayoutManager {

    PageNumberCitation pncNode;
    Font font = null;
    
    // whether the page referred to by the citation has been resolved yet
    private boolean resolved = false;
    
    /**
     * Constructor
     *
     * @param node the formatting object that creates this area
     * @todo better retrieval of font info
     */
    public PageNumberCitationLayoutManager(PageNumberCitation node) {
        super(node);
        font = node.getPropertyManager().getFontState(node.getFOInputHandler().getFontInfo());
        pncNode = node;
    }

    public InlineArea get(LayoutContext context) {
        curArea = getPageNumberCitationInlineArea(parentLM);
        return curArea;
    }
    
    public void addAreas(PositionIterator posIter, LayoutContext context) {
        super.addAreas(posIter, context);
        if (!resolved) {
            parentLM.addUnresolvedArea(pncNode.getPropString(PR_REF_ID),
                (Resolveable) curArea);
        }
    }
    
    protected void offsetArea(LayoutContext context) {
        curArea.setOffset(context.getBaseline());
    }

    /**
     * if id can be resolved then simply return a word, otherwise
     * return a resolveable area
     */
    private InlineArea getPageNumberCitationInlineArea(LayoutManager parentLM) {
        PageViewport page = parentLM.resolveRefID(pncNode.getPropString(PR_REF_ID));
        InlineArea inline = null;
        if (page != null) {
            String str = page.getPageNumber();
            // get page string from parent, build area
            TextArea text = new TextArea();
            inline = text;
            int width = getStringWidth(str);
            text.setTextArea(str);
            inline.setIPD(width);
            inline.setHeight(font.getAscender() - font.getDescender());
            inline.setOffset(font.getAscender());
            
            inline.addTrait(Trait.FONT_NAME, font.getFontName());
            inline.addTrait(Trait.FONT_SIZE,
                         new Integer(font.getFontSize()));
            resolved = true;
        } else {
            resolved = false;
            inline = new UnresolvedPageNumber(pncNode.getPropString(PR_REF_ID));
            String str = "MMM"; // reserve three spaces for page number
            int width = getStringWidth(str);
            inline.setIPD(width);
            inline.setHeight(font.getAscender() - font.getDescender());
            inline.setOffset(font.getAscender());
            
            inline.addTrait(Trait.FONT_NAME, font.getFontName());
            inline.addTrait(Trait.FONT_SIZE, new Integer(font.getFontSize()));
        }
        return inline;
    }
    
    /**
     * @param str string to be measured
     * @return width (in millipoints ??) of the string
     */
    private int getStringWidth(String str) {
        int width = 0;
        for (int count = 0; count < str.length(); count++) {
            width += font.getCharWidth(str.charAt(count));
        }
        return width;
    }
}

