/*
 * $Id$
 * Copyright (C) 2003 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.List;

import org.apache.fop.area.Area;
import org.apache.fop.fo.flow.Marker;

/**
 * LayoutManager for a block FO.
 */
public class RetrieveMarkerLayoutManager extends AbstractLayoutManager {
    private LayoutManager replaceLM = null;
    private boolean loaded = false;
    private String name;
    private int position;
    private int boundary;

    /**
     * Create a new block container layout manager.
     */
    public RetrieveMarkerLayoutManager(String n, int pos, int bound) {
        name = n;
        position = pos;
        boundary = bound;
    }

    public boolean generatesInlineAreas() {
        loadLM();
        if (replaceLM == null) {
            return true;
        }
        return replaceLM.generatesInlineAreas();
    }

    public BreakPoss getNextBreakPoss(LayoutContext context) {
        loadLM();
        if (replaceLM == null) {
            return null;
        }
        return replaceLM.getNextBreakPoss(context);
    }

    public void addAreas(PositionIterator parentIter,
                         LayoutContext layoutContext) {

        loadLM();
        addID();
        replaceLM.addAreas(parentIter, layoutContext);

    }

    public boolean isFinished() {
        loadLM();
        if (replaceLM == null) {
            return true;
        }
        return replaceLM.isFinished();
    }

    public void setFinished(boolean fin) {
        if (replaceLM != null) {
            replaceLM.setFinished(fin);
        }
    }

    protected void loadLM() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (replaceLM == null) {
            List list = new ArrayList();
            Marker marker = retrieveMarker(name, position, boundary);
            if (marker != null) {
                marker.addLayoutManager(list);
                if (list.size() > 0) {
                    replaceLM =  (LayoutManager)list.get(0);
                    replaceLM.setParentLM(this);
                    replaceLM.init();
                    getLogger().debug("retrieved: " + replaceLM + ":" + list.size());
                } else {
                    getLogger().debug("found no marker with name: " + name);
                }
            }
        }
    }

    /**
     * Get the parent area for children of this block container.
     * This returns the current block container area
     * and creates it if required.
     *
     * @see org.apache.fop.layoutmgr.LayoutManager#getParentArea(Area)
     */
    public Area getParentArea(Area childArea) {
        return parentLM.getParentArea(childArea);
    }

    /**
     * Add the child to the block container.
     *
     * @see org.apache.fop.layoutmgr.LayoutManager#addChild(Area)
     */
    public void addChild(Area childArea) {
        parentLM.addChild(childArea);
    }

    public void resetPosition(Position resetPos) {
        loadLM();
        if (resetPos == null) {
            reset(null);
        }
        if (replaceLM != null) {
            replaceLM.resetPosition(resetPos);
        }
        loaded = false;
        replaceLM = null;
    }

}

