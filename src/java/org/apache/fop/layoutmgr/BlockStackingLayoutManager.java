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

import org.apache.fop.area.Area;
import org.apache.fop.area.BlockParent;
import org.apache.fop.area.Block;
import org.apache.fop.fo.FObj;
import org.apache.fop.traits.MinOptMax;

/**
 * Base LayoutManager class for all areas which stack their child
 * areas in the block-progression direction, such as Flow, Block, ListBlock.
 */
public abstract class BlockStackingLayoutManager extends AbstractLayoutManager {
    /**
     * Reference to FO whose areas it's managing or to the traits
     * of the FO.
     */
    protected LayoutManager curChildLM = null;
    protected BlockParent parentArea = null;

    public BlockStackingLayoutManager() {
    }

    public BlockStackingLayoutManager(FObj node) {
        super(node);
    }

    private BreakCost evaluateBreakCost(Area parent, Area child) {
        return new BreakCost(child, 0);
    }

    /** return current area being filled
     */
    protected BlockParent getCurrentArea() {
        return this.parentArea;
    }


    /**
     * Set the current area being filled.
     */
    protected void setCurrentArea(BlockParent parentArea) {
        this.parentArea = parentArea;
    }

    protected MinOptMax resolveSpaceSpecifier(Area nextArea) {
        SpaceSpecifier spaceSpec = new SpaceSpecifier(false);
        // Area prevArea = getCurrentArea().getLast();
        // if (prevArea != null) {
        //     spaceSpec.addSpace(prevArea.getSpaceAfter());
        // }
        // spaceSpec.addSpace(nextArea.getSpaceBefore());
        return spaceSpec.resolve(false);
    }

    /**
     * Add a block spacer for space before and space after a block.
     * This adds an empty Block area that acts as a block space.
     *
     * @param adjust the adjustment value
     * @param minoptmax the min/opt/max value of the spacing
     */
    public void addBlockSpacing(double adjust, MinOptMax minoptmax) {
        if (minoptmax == null) {
            return;
        }
        int sp = minoptmax.opt;
        if (adjust > 0) {
            sp = sp + (int)(adjust * (minoptmax.max - minoptmax.opt));
        } else {
            sp = sp + (int)(adjust * (minoptmax.opt - minoptmax.min));
        }
        if (sp != 0) {
            Block spacer = new Block();
            spacer.setHeight(sp);
            parentLM.addChild(spacer);
        }
    }

    /**
     * Add the childArea to the passed area.
     * Called by child LayoutManager when it has filled one of its areas.
     * The LM should already have an Area in which to put the child.
     * See if the area will fit in the current area.
     * If so, add it. Otherwise initiate breaking.
     * @param childArea the area to add: will be some block-stacked Area.
     * @param parentArea the area in which to add the childArea
     */
    protected void addChildToArea(Area childArea,
                                     BlockParent parentArea) {
        // This should be a block-level Area (Block in the generic sense)
        if (!(childArea instanceof Block)) {
            //log.error("Child not a Block in BlockStackingLM!");
        }

        MinOptMax spaceBefore = resolveSpaceSpecifier(childArea);
        parentArea.addBlock((Block) childArea);
        flush(); // hand off current area to parent
    }


    /**
     * Add the childArea to the current area.
     * Called by child LayoutManager when it has filled one of its areas.
     * The LM should already have an Area in which to put the child.
     * See if the area will fit in the current area.
     * If so, add it. Otherwise initiate breaking.
     * @param childArea the area to add: will be some block-stacked Area.
     */
    public void addChild(Area childArea) {
        addChildToArea(childArea, getCurrentArea());
    }

    /**
     * Force current area to be added to parent area.
     */
    protected void flush() {
        if (getCurrentArea() != null) {
            parentLM.addChild(getCurrentArea());
        }
    }

}

