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
import org.apache.fop.area.LineArea;

import java.util.ListIterator;

/**
 * LayoutManager for a block FO.
 */
public class BlockLayoutManager extends BlockStackingLayoutManager {

    private Block curBlockArea;

    public BlockLayoutManager(FObj fobj) {
	super(fobj);
    }

    // DESIGN. Potential alternative to getParentArea() scheme
//     /**
//      * Called by child layout manager to get the available space for
//      * content in the inline progression direction.
//      * Note that a manager may need to ask its parent for this.
//      * For a block area, available IPD is determined by indents.
//      */
//     public int getContentIPD() {
// 	getArea(); // make if not existing
// 	return blockArea.getIPD();
//     }

    /**
     * Generate areas by tellings all layout managers for its FO's
     * children to generate areas.
     */
    public void generateAreas() {
	ListIterator children = fobj.getChildren();
	LayoutManager lm=null;
	while (children.hasNext()) {
	    FObj childFO = (FObj)children.next();
	    if (childFO.generatesInlineAreas()) {
		children.previous();
		lm = new LineLayoutManager(children);
	    }
	    else {
		lm = childFO.getLayoutManager();
	    }
	    if (lm != null) {
		lm.setParentLM(this);
		lm.generateAreas();
	    }
	}
	flush(); // Add last area to parent
    }


    /**
     * Return an Area which can contain the passed childArea. The childArea
     * may not yet have any content, but it has essential traits set.
     * In general, if the LayoutManager already has an Area it simply returns
     * it. Otherwise, it makes a new Area of the appropriate class.
     * It gets a parent area for its area by calling its parent LM.
     * Finally, based on the dimensions of the parent area, it initializes
     * its own area. This includes setting the content IPD and the maximum
     * BPD.
     */
    public Area getParentArea(Area childArea) {
	if (curBlockArea == null) {
	    curBlockArea = new Block();
	    // Set up dimensions
	    // Must get dimensions from parent area
	    //MinOptMax referenceIPD = parentLM.getReferenceIPD();
	    Area parentArea = parentLM.getParentArea(curBlockArea);
	    // Get reference IPD from parentArea
	    setCurrentArea(curBlockArea); // ??? for generic operations
	}
	return curBlockArea;
    }
	

    public void addChild(Area childArea) {
	if (curBlockArea != null) {
	    if (childArea instanceof LineArea) {
		// Something about widows and orphans
		// Position the line area and calculate size...
		curBlockArea.addLineArea((LineArea)childArea);
	    }
	    else {
		super.addChild(childArea);
	    }
	}
    }



//     /**
//      * Called by child LayoutManager when it has filled one of its areas.
//      * If no current container, make one.
//      * See if the area will fit in the current container.
//      * If so, add it.
//      * @param childArea the area to add: will either be a LineArea or
//      * a BlockArea.
//      */
//     public void  addChild(Area childArea) {
// 	/* If the childArea fits entirely in the maximum available BPD
// 	 * add it and return an OK status.
// 	 * If it doesn't all fit, overrun or ask for split?
// 	 * Might as well just add it since the page layout process
// 	 * may need to make other adjustments, resulting in changing
// 	 * split point.
// 	 */
// 	// Things like breaks on child area can cause premature
// 	// termination of the current area.
// 	/* We go past the theoretical maximum to be able to handle things
// 	 * like widows. 
// 	 */
// 	// WARNING: this doesn't take into account space-specifier
// 	// adujstment between childArea and last content of blockArea!
// 	if (blockArea.getContentBPD().min + childArea.getAllocationBPD().min
// 	    > blockArea.getAvailBPD().max) {
// 	    if (++extraLines <= iWidows) {
// 		blockArea.add(childArea);
// 	    }
// 	    else {
// 		blockArea.setIsLast(false);
// 		parentLM.addChildArea(blockArea);
// 		// Make a new one for this area
// 		blockArea = makeAreaForChild(childArea);
// 		extraLines = 0; // Count potential widows
// 		blockArea.add(childArea);
// 	    }
// 	}
// 	else {
// 	    blockArea.add(childArea);
// 	}
//     }

}
