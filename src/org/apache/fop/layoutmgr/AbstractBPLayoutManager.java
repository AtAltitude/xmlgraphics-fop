/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.layoutmgr;

import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyManager;
import org.apache.fop.fo.FONode;
import org.apache.fop.area.Area;

import java.util.ListIterator;
import java.util.ArrayList;

/**
 * The base class for all BPLayoutManagers.
 */
public abstract class AbstractBPLayoutManager extends AbstractLayoutManager implements BPLayoutManager {


    /** True if this LayoutManager has handled all of its content. */
    private boolean m_bFinished = false;
    protected BPLayoutManager m_curChildLM = null;
    protected ListIterator m_childLMiter;
    protected boolean m_bInited = false;


    public AbstractBPLayoutManager(FObj fobj) {
        this(fobj, new LMiter(fobj.getChildren()));
    }


    public AbstractBPLayoutManager(FObj fobj, ListIterator lmIter) {
        super(fobj);
        m_childLMiter = lmIter;
    }


    /**
     * Return currently active child LayoutManager or null if
     * all children have finished layout.
     * Note: child must implement BPLayoutManager! If it doesn't, skip it
     * and print a warning.
     */
    protected BPLayoutManager getChildLM() {
        if (m_curChildLM != null && !m_curChildLM.isFinished()) {
            return m_curChildLM;
        }
        while (m_childLMiter.hasNext()) {
            Object obj = m_childLMiter.next();
            if (obj instanceof BPLayoutManager) {
                m_curChildLM = (BPLayoutManager) obj;
                m_curChildLM.setParentLM(this);
                m_curChildLM.init();
                return m_curChildLM;
            } else {
                m_childLMiter.remove();
                //log.warn(
                //  "child LM not a BPLayoutManager: " +
                //  obj.getClass().getName());
            }
        }
        return null;
    }

    protected boolean hasMoreLM(BPLayoutManager prevLM) {
        // prevLM should = m_curChildLM
        if (prevLM != m_curChildLM) {
            //log.debug("AbstractBPLayoutManager.peekNextLM: " +
            //                   "passed LM is not current child LM!");
            return false;
        }
        return !m_childLMiter.hasNext();
    }


    /**
     * Reset the layoutmanager "iterator" so that it will start
     * with the passed Position's generating LM
     * on the next call to getChildLM.
     * @param pos a Position returned by a child layout manager
     * representing a potential break decision.
     * If pos is null, then back up to the first child LM.
     */
    protected void reset(Position pos) {
        //if (lm == null) return;
        BPLayoutManager lm = (pos != null) ? pos.getLM() : null;
        if (m_curChildLM != lm) {
            // ASSERT m_curChildLM == (BPLayoutManager)m_childLMiter.previous()
            if (m_childLMiter.hasPrevious() && m_curChildLM !=
                    (BPLayoutManager) m_childLMiter.previous()) {
                //log.error("LMiter problem!");
            }
            while (m_curChildLM != lm && m_childLMiter.hasPrevious()) {
                m_curChildLM.resetPosition(null);
                m_curChildLM = (BPLayoutManager) m_childLMiter.previous();
            }
            m_childLMiter.next(); // Otherwise next returns same object
        }
        if(m_curChildLM != null) {
            m_curChildLM.resetPosition(pos);
        }
        if (isFinished()) {
            setFinished(false);
        }
    }

    public void resetPosition(Position resetPos) {
        // 	if (resetPos == null) {
        // 	    reset(null);
        // 	}
    }


    /**
     * This method provides a hook for a LayoutManager to intialize traits
     * for the areas it will create, based on Properties set on its FO.
     */
    public void init() {
        if (fobj != null && m_bInited == false) {
            initProperties(fobj.getPropertyManager());
            m_bInited = true;
        }
    }


    /**
     * This method provides a hook for a LayoutManager to intialize traits
     * for the areas it will create, based on Properties set on its FO.
     */
    protected void initProperties(PropertyManager pm) {
        //log.debug("AbstractBPLayoutManager.initProperties");
    }


    /**
     * Tell whether this LayoutManager has handled all of its content.
     * @return True if there are no more break possibilities,
     * ie. the last one returned represents the end of the content.
     */
    public boolean isFinished() {
        return m_bFinished;
    }

    public void setFinished(boolean bFinished) {
        m_bFinished = bFinished;
    }


    /**
     * Generate and return the next break possibility.
     * Each layout manager must implement this.
     * TODO: should this be abstract or is there some reasonable
     * default implementation?
     */
    public BreakPoss getNextBreakPoss(LayoutContext context) {
        return getNextBreakPoss(context, null);
    }


    public BreakPoss getNextBreakPoss(LayoutContext context,
                                      Position prevBreakPoss) {
        return null;
    }

    /**
     * Return value indicating whether the next area to be generated could
     * start a new line or flow area.
     * In general, if can't break at the current level, delegate to
     * the first child LM.
     * NOTE: should only be called if the START_AREA flag is set in context,
     * since the previous sibling LM must have returned a BreakPoss which
     * does not allow break-after.
     * QUESTION: in block-stacked areas, does this mean some kind of keep
     * condition, or is it only used for inline-stacked areas?
     * Default implementation always returns true.
     */
    public boolean canBreakBefore(LayoutContext context) {
        return true;
    }


    public void addAreas(PositionIterator posIter, LayoutContext context) {
    }


    public void getWordChars(StringBuffer sbChars, Position bp1,
                             Position bp2) {
    }

    /* ---------------------------------------------------------
     * PROVIDE NULL IMPLEMENTATIONS OF METHODS from LayoutManager
     * interface which are declared abstract in AbstractLayoutManager.
     * ---------------------------------------------------------*/
    public Area getParentArea(Area childArea) {
        return null;
    }

    protected boolean flush() {
        return false;
    }



    public boolean addChild(Area childArea) {
        return false;
    }
}

