/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.layoutmgr;

import org.apache.fop.fo.FObj;
import org.apache.fop.area.Area;

import java.util.ListIterator;

/**
 * The base class for all LayoutManagers.
 */
public abstract class AbstractLayoutManager implements LayoutManager {
    protected LayoutManager parentLM;
    protected FObj fobj;


    public AbstractLayoutManager(FObj fobj) {
	this.fobj = fobj;
	this.parentLM = null;
    }

    public void setParentLM(LayoutManager lm) {
	this.parentLM = lm;
    }


    /**
     * Propagates to lower level layout managers. It iterates over the
     * children of its FO, asks each for its LayoutManager and calls
     * its generateAreas method.
     */
    public void generateAreas() {
	ListIterator children = fobj.getChildren();
	while (children.hasNext()) {
	    LayoutManager lm = ((FObj)children.next()).getLayoutManager();
 	    if (lm != null) {
		lm.setParentLM(this);
		lm.generateAreas();
	    }
	}
	flush(); // Add last area to parent
    }

//     /**
//      * Ask the parent LayoutManager to add the current (full) area to the
//      * appropriate parent area.
//      * @param bFinished If true, this area is finished, either because it's
//      * completely full or because there is no more content to put in it.
//      * If false, we are in the middle of this area. This can happen,
//      * for example, if we find floats in a line. We stop the current area,
//      * and add it (temporarily) to its parent so that we can see if there
//      * is enough space to place the float(s) anchored in the line.
//      */
//     protected void flush(Area area, boolean bFinished) {
// 	if (area != null) {
// 	    // area.setFinished(true);
// 	    parentLM.addChild(area, bFinished); // ????
// 	    if (bFinished) {
// 		setCurrentArea(null);
// 	    }
// 	}
//     }

    /** 
     * Force current area to be added to parent area.
     */
    abstract protected void flush();


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
    abstract public Area getParentArea(Area childArea);
	


//     public boolean generatesInlineAreas() {
// 	return false;
//     }


    /**
     * Add a child area to the current area. If this causes the maximum
     * dimension of the current area to be exceeded, the parent LM is called
     * to add it.
     */
    abstract public void addChild(Area childArea) ;

    /** Do nothing */
    public boolean splitArea(Area areaToSplit, SplitContext context) {
	context.nextArea = areaToSplit;
	return false;
    }

}
