/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.layoutmgr;

import org.apache.fop.fo.FObj;
import org.apache.fop.area.Area;
import org.apache.fop.area.BlockParent;
import org.apache.fop.area.Block;
import org.apache.fop.area.MinOptMax;

import java.util.Iterator;

/**
 * Base LayoutManager class for all areas which stack their child
 * areas in the block-progression direction, such as Flow, Block, ListBlock.
 */
public abstract class BlockStackingLayoutManager extends AbstractLayoutManager {
    /** Reference to FO whose areas it's managing or to the traits
     * of the FO.
     */
    LayoutManager curChildLM = null;
    BlockParent parentArea = null;

    public BlockStackingLayoutManager(FObj fobj) {
        super(fobj);
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
        // 	Area prevArea = getCurrentArea().getLast();
        // 	if (prevArea != null) {
        // 	    spaceSpec.addSpace(prevArea.getSpaceAfter());
        // 	}
        // 	spaceSpec.addSpace(nextArea.getSpaceBefore());
        return spaceSpec.resolve(false);
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
    protected boolean addChildToArea(Area childArea,
                                     BlockParent parentArea) {
        // This should be a block-level Area (Block in the generic sense)
        if (!(childArea instanceof Block)) {
            //log.error("Child not a Block in BlockStackingLM!");
            return false;
        }

        // See if the whole thing fits, including space before
        // Calculate space between last child in curFlow and childArea
        MinOptMax targetDim = parentArea.getAvailBPD();
        MinOptMax spaceBefore = resolveSpaceSpecifier(childArea);
        targetDim.subtract(spaceBefore);
        if (targetDim.max >= childArea.getAllocationBPD().min) {
            //parentArea.addBlock(new InterBlockSpace(spaceBefore));
            parentArea.addBlock((Block) childArea);
            return false;
        } else {
            parentArea.addBlock((Block) childArea);
            flush(); // hand off current area to parent
            // Probably need something like max BPD so we don't get into
            // infinite loops with large unbreakable chunks

            /*LayoutManager childLM =
              childArea.getGeneratingFObj(). getLayoutManager();
            if (childLM.splitArea(childArea, splitContext)) {
                //parentArea.addBlock(new InterBlockSpace(spaceBefore));
                parentArea.addBlock((Block) childArea);
        }*/
            //flush(); // hand off current area to parent
            //getParentArea(splitContext.nextArea);
            //getParentArea(childArea);
            // Check that reference IPD hasn't changed!!!
            // If it has, we must "reflow" the content
            //addChild(splitContext.nextArea);
            //addChild(childArea);
            return true;
        }
    }


    /**
     * Add the childArea to the current area.
     * Called by child LayoutManager when it has filled one of its areas.
     * The LM should already have an Area in which to put the child.
     * See if the area will fit in the current area.
     * If so, add it. Otherwise initiate breaking.
     * @param childArea the area to add: will be some block-stacked Area.
     */
    public boolean addChild(Area childArea) {
        return addChildToArea(childArea, getCurrentArea());
    }

    /**
     * Force current area to be added to parent area.
     */
    protected boolean flush() {
        if (getCurrentArea() != null)
            return parentLM.addChild(getCurrentArea());
        return false;
    }

}

