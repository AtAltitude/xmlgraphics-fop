/*
 * $Id$
 * Copyright (C) 2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.area.inline;

import org.apache.fop.area.Area;
import org.apache.fop.area.Trait;
import org.apache.fop.render.Renderer;
import org.apache.fop.traits.BorderProps;

import java.util.ArrayList;

/**
 * Inline Area
 * This area is for all inline areas that can be placed
 * in a line area.
 * Extensions of this class should render themselves with the
 * requested renderer.
 */
public class InlineArea extends Area {
    // int width;
    private int height;
    protected int contentIPD = 0;

    // offset position from top of parent area
    int verticalPosition = 0;

    // store properties in array list, need better solution
    private ArrayList props = null;

    /**
     * Render this inline area.
     * Inline areas that extend this class are expected
     * to implement this method to render themselves in
     * the renderer.
     *
     * @param renderer the renderer to render this inline area
     */
    public void render(Renderer renderer) {
    }

    public void setWidth(int w) {
        contentIPD = w;
    }

    public int getWidth() {
        return contentIPD;
    }

    public void setIPD(int ipd) {
        this.contentIPD = ipd;
    }

    public int getIPD() {
        return this.contentIPD;
    }

    public void increaseIPD(int ipd) {
        this.contentIPD += ipd;
    }

    public void setHeight(int h) {
        height = h;
    }

    public int getHeight() {
        return height;
    }

    public int getAllocIPD() {
        // If start or end border or padding is non-zero, add to content IPD
        int iBP = contentIPD;
        Object t;
        if ((t = getTrait(Trait.PADDING_START)) != null) {
            iBP += ((Integer) t).intValue();
        }
        if ((t = getTrait(Trait.PADDING_END)) != null) {
            iBP += ((Integer) t).intValue();
        }
        if ((t = getTrait(Trait.BORDER_START)) != null) {
            iBP += ((BorderProps) t).width;
        }
        if ((t = getTrait(Trait.BORDER_END)) != null) {
            iBP += ((BorderProps) t).width;
        }
        return iBP;
    }

    public void setOffset(int v) {
        verticalPosition = v;
    }

    public int getOffset() {
        return verticalPosition;
    }
}

