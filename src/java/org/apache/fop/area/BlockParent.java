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
 
package org.apache.fop.area;

import java.util.ArrayList;
import java.util.List;

/**
 * A BlockParent holds block-level areas.
 */
public class BlockParent extends Area {

    // this position is used for absolute position
    // or as an indent
    // this has the size in the block progression dimension

    /**
     * The x offset position of this block parent.
     * Used for relative and absolute positioning.
     */
    protected int xOffset = 0;

    /**
     * The y offset position of this block parent.
     * Used for relative and absolute positioning.
     */
    protected int yOffset = 0;

    /**
     * The width of this block parent.
     */
    protected int width = 0;

    /**
     * The height of this block parent.
     */
    protected int height = 0;

    /**
     * The children of this block parent area.
     */
    protected List children = null;

    // orientation if reference area
    private int orientation = ORIENT_0;

    /**
     * Add the block area to this block parent.
     *
     * @param block the child block area to add
     */
    public void addBlock(Block block) {
        if (children == null) {
            children = new ArrayList();
        }
        children.add(block);
    }

    /**
     * Get the list of child areas for this block area.
     *
     * @return the list of child areas
     */
    public List getChildAreas() {
        return children;
    }

    /**
     * Set the X offset of this block parent area.
     *
     * @param off the x offset of the block parent area
     */
    public void setXOffset(int off) {
        xOffset = off;
    }

    /**
     * Set the Y offset of this block parent area.
     *
     * @param off the y offset of the block parent area
     */
    public void setYOffset(int off) {
        yOffset = off;
    }

    /**
     * Set the width of this block parent area.
     *
     * @param w the width of the area
     */
    public void setWidth(int w) {
        width = w;
    }

    /**
     * Set the height of this block parent area.
     *
     * @param h the height of the block parent area
     */
    public void setHeight(int h) {
        height = h;
    }

    /**
     * Get the X offset of this block parent area.
     *
     * @return the x offset of the block parent area
     */
    public int getXOffset() {
        return xOffset;
    }

    /**
     * Get the Y offset of this block parent area.
     *
     * @return the y offset of the block parent area
     */
    public int getYOffset() {
        return yOffset;
    }

    /**
     * Get the width of this block parent area.
     *
     * @return the width of the area
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the height of this block parent area.
     *
     * @return the height of the block parent area
     */
    public int getHeight() {
        return height;
    }

}
