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

import java.util.List;
import java.awt.geom.Rectangle2D;

import org.apache.fop.area.Area;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.Block;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyManager;
import org.apache.fop.fo.properties.CommonAbsolutePosition;
import org.apache.fop.fo.properties.CommonBorderAndPadding;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.area.CTM;
import org.apache.fop.datatypes.FODimension;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.PercentBase;
import org.apache.fop.traits.MinOptMax;

/**
 * LayoutManager for a block FO.
 */
public class BlockContainerLayoutManager extends BlockStackingLayoutManager {

    private BlockViewport viewportBlockArea;
    private Block curBlockArea;

    private List childBreaks = new java.util.ArrayList();

    private CommonAbsolutePosition abProps;
    private CommonBorderAndPadding borderProps;
    private CommonMarginBlock marginProps;
    private FODimension relDims;
    private CTM absoluteCTM;
    private boolean clip = false;
    private int overflow;
    private PropertyManager propManager;
    private Length width;
    private Length height;

    // When viewport should grow with the content.
    private boolean autoHeight = true; 

    /**
     * Create a new block container layout manager.
     */
    public BlockContainerLayoutManager(FObj node) {
        super(node);
    }

    /**
     * @see org.apache.fop.layoutmgr.AbstractLayoutManager#initProperties()
     */
    protected void initProperties() {
        propManager = fobj.getPropertyManager();

        abProps = propManager.getAbsolutePositionProps();
        if (abProps.absolutePosition == AbsolutePosition.ABSOLUTE) {
            Rectangle2D rect = new Rectangle2D.Double(abProps.left,
                                abProps.top, abProps.right - abProps.left,
                                abProps.bottom - abProps.top);
            relDims = new FODimension(0, 0);
            absoluteCTM = CTM.getCTMandRelDims(propManager.getAbsRefOrient(),
                propManager.getWritingMode(), rect, relDims);
        }
        
        marginProps = propManager.getMarginProps();
        borderProps = propManager.getBorderAndPadding();
        height = propManager.getPropertyList().get(
            PR_BLOCK_PROGRESSION_DIMENSION | CP_OPTIMUM).getLength();
        width = propManager.getPropertyList().get(
            PR_INLINE_PROGRESSION_DIMENSION | CP_OPTIMUM).getLength();
    }

    public void setOverflow(int of) {
        overflow = of;
    }

    protected int getRotatedIPD() {
        PropertyList props = fobj.getPropertyList();
        int height = props.get(PR_HEIGHT).getLength().getValue();
        height = props.get(PR_INLINE_PROGRESSION_DIMENSION | CP_OPTIMUM).getLength().getValue();

        return height;
    }

    public BreakPoss getNextBreakPoss(LayoutContext context) {

        if (abProps.absolutePosition == AbsolutePosition.ABSOLUTE) {
            return getAbsoluteBreakPoss(context);
        }

        int bIndents = borderProps.getBPPaddingAndBorder(false);
        int iIndents = marginProps.startIndent + marginProps.endIndent; 

        int ipd = context.getRefIPD();
        int bpd = context.getStackLimit().opt;
        if (!width.isAuto()) {
            ipd = width.getValue() + iIndents;
        }
        if (!height.isAuto()) {
            bpd = height.getValue() + bIndents;
        }
        Rectangle2D rect = new Rectangle2D.Double(0, 0, ipd, bpd);
        relDims = new FODimension(0, 0);
        absoluteCTM = CTM.getCTMandRelDims(propManager.getAbsRefOrient(),
                propManager.getWritingMode(), rect, relDims);
        double[] vals = absoluteCTM.toArray();

        ipd -= iIndents;

        MinOptMax stackLimit;
        boolean rotated = vals[0] == 0.0;
        if (rotated) {
            // rotated 90 degrees
            if (relDims.ipd > context.getRefIPD()) {
                relDims.ipd = context.getRefIPD();
            }
            stackLimit = new MinOptMax(relDims.ipd);
            if (width.isAuto()) {
                relDims.bpd = context.getStackLimit().opt;
            }
            absoluteCTM = new CTM(vals[0], vals[1], vals[2], vals[3], 0, 0);
        } else {
            if (vals[0] == -1.0) {
                absoluteCTM = new CTM(vals[0], vals[1], vals[2], vals[3], 0, 0);
            }
            stackLimit = context.getStackLimit();
        }

        LayoutManager curLM ; // currently active LM

        MinOptMax stackSize = new MinOptMax();
        // if starting add space before
        // stackSize.add(spaceBefore);
        BreakPoss lastPos = null;

        fobj.setLayoutDimension(PercentBase.BLOCK_IPD, ipd);
        fobj.setLayoutDimension(PercentBase.BLOCK_BPD, bpd - bIndents);
        fobj.setLayoutDimension(PercentBase.REFERENCE_AREA_IPD, ipd);
        fobj.setLayoutDimension(PercentBase.REFERENCE_AREA_BPD, bpd - bIndents);

        while ((curLM = getChildLM()) != null) {
            // Make break positions and return blocks!
            // Set up a LayoutContext
            BreakPoss bp;

            LayoutContext childLC = new LayoutContext(0);
                childLC.setStackLimit(
                  MinOptMax.subtract(stackLimit,
                                     stackSize));
                childLC.setRefIPD(ipd);

            boolean over = false;
            while (!curLM.isFinished()) {
                if ((bp = curLM.getNextBreakPoss(childLC)) != null) {
                    stackSize.add(bp.getStackingSize());
                    if (stackSize.opt > stackLimit.max) {
                        // reset to last break
                        if (lastPos != null) {
                            reset(lastPos.getPosition());
                        } else {
                            curLM.resetPosition(null);
                        }
                        over = true;
                        break;
                    }
                    lastPos = bp;
                    childBreaks.add(bp);

                    if (bp.nextBreakOverflows()) {
                        over = true;
                        break;
                    }                    
                    
                    childLC.setStackLimit(MinOptMax.subtract(
                                           stackLimit, stackSize));
                }
            }
            if (!rotated) {
                BreakPoss breakPoss;
                breakPoss = new BreakPoss(new LeafPosition(this,
                                                   childBreaks.size() - 1));
                breakPoss.setStackingSize(stackSize);
                if (over) {
                    breakPoss.setFlag(BreakPoss.NEXT_OVERFLOWS, true);
                }
                return breakPoss;
            }
        }
        setFinished(true);
        if (rotated) {
            BreakPoss breakPoss;
            breakPoss = new BreakPoss(new LeafPosition(this,
                                               childBreaks.size() - 1));
            breakPoss.setStackingSize(new MinOptMax(ipd));
            return breakPoss;
        }
        return null;
    }

    public BreakPoss getAbsoluteBreakPoss(LayoutContext context) {

        LayoutManager curLM ; // currently active LM

        MinOptMax stackSize = new MinOptMax();

        int ipd = relDims.ipd;

        while ((curLM = getChildLM()) != null) {
            // Make break positions and return blocks!
            // Set up a LayoutContext
            BreakPoss bp;

            LayoutContext childLC = new LayoutContext(0);
                childLC.setStackLimit(new MinOptMax(1000000));
                childLC.setRefIPD(ipd);

            while (!curLM.isFinished()) {
                if ((bp = curLM.getNextBreakPoss(childLC)) != null) {
                    stackSize.add(bp.getStackingSize());
                    childBreaks.add(bp);
                }
            }
        }
        setFinished(true);
        BreakPoss breakPoss = new BreakPoss(
                                new LeafPosition(this, childBreaks.size() - 1));
        // absolutely positioned areas do not contribute
        // to the normal stacking
        breakPoss.setStackingSize(new MinOptMax(0));

        if (stackSize.opt > relDims.bpd) {
            if (overflow == Overflow.HIDDEN) {
                clip = true;
            } else if (overflow == Overflow.ERROR_IF_OVERFLOW) {
                log.error("contents overflows block-container viewport: clipping");
                clip = true;
            }
        }

        return breakPoss;
    }

    public void addAreas(PositionIterator parentIter,
                         LayoutContext layoutContext) {
        getParentArea(null);

        addID();
        addMarkers(true, true);

        LayoutManager childLM;
        int iStartPos = 0;
        LayoutContext lc = new LayoutContext(0);
        while (parentIter.hasNext()) {
            LeafPosition lfp = (LeafPosition) parentIter.next();
            // Add the block areas to Area
            PositionIterator breakPosIter =
              new BreakPossPosIter(childBreaks, iStartPos,
                                   lfp.getLeafPos() + 1);
            iStartPos = lfp.getLeafPos() + 1;
            while ((childLM = breakPosIter.getNextChildLM()) != null) {
                childLM.addAreas(breakPosIter, lc);
            }
        }

        flush();
        addMarkers(true, true);

        childBreaks.clear();
        viewportBlockArea = null;
        curBlockArea = null;
    }

    /**
     * Get the parent area for children of this block container.
     * This returns the current block container area
     * and creates it if required.
     *
     * @see org.apache.fop.layoutmgr.LayoutManager#getParentArea(Area)
     */
    public Area getParentArea(Area childArea) {
        if (curBlockArea == null) {
            viewportBlockArea = new BlockViewport();
            TraitSetter.addBorders(viewportBlockArea,
                                   propManager.getBorderAndPadding());
            TraitSetter.addBackground(viewportBlockArea,
                                      propManager.getBackgroundProps());
            
            if (abProps.absolutePosition == AbsolutePosition.ABSOLUTE) {
                viewportBlockArea.setXOffset(abProps.left);
                viewportBlockArea.setYOffset(abProps.top);
                viewportBlockArea.setWidth(abProps.right - abProps.left);
                viewportBlockArea.setHeight(abProps.bottom - abProps.top);

                viewportBlockArea.setCTM(absoluteCTM);
                viewportBlockArea.setClip(clip);
                autoHeight = false;
            } else {
                double[] vals = absoluteCTM.toArray();
                boolean rotated = vals[0] == 0.0;
                if (rotated) {
                    viewportBlockArea.setWidth(relDims.ipd);
                    viewportBlockArea.setHeight(relDims.bpd);
                    viewportBlockArea.setCTM(absoluteCTM);
                    viewportBlockArea.setClip(clip);
                    autoHeight = false;
                } else if (vals[0] == -1.0) {
                    // need to set bpd to actual size for rotation
                    // and stacking
                    viewportBlockArea.setWidth(relDims.ipd);
                    if (!height.isAuto()) {
                        viewportBlockArea.setHeight(relDims.bpd);
                        autoHeight = false;
                    }
                    viewportBlockArea.setCTM(absoluteCTM);
                    viewportBlockArea.setClip(clip);
                } else {
                    viewportBlockArea.setWidth(relDims.ipd);
                    if (!height.isAuto()) {
                        viewportBlockArea.setHeight(relDims.bpd);
                        autoHeight = false;
                    }
                }
            }

            curBlockArea = new Block();

            if (abProps.absolutePosition == AbsolutePosition.ABSOLUTE) {
                viewportBlockArea.setPositioning(Block.ABSOLUTE);
            }

            // Set up dimensions
            // Must get dimensions from parent area
            Area parentArea = parentLM.getParentArea(curBlockArea);
            //int referenceIPD = parentArea.getIPD();
            curBlockArea.setIPD(relDims.ipd);
            // Get reference IPD from parentArea
            setCurrentArea(viewportBlockArea); // ??? for generic operations
        }
        return curBlockArea;
    }

    /**
     * Add the child to the block container.
     *
     * @see org.apache.fop.layoutmgr.LayoutManager#addChild(Area)
     */
    public void addChild(Area childArea) {
        if (curBlockArea != null) {
            curBlockArea.addBlock((Block) childArea);
        }
    }

    public void resetPosition(Position resetPos) {
        if (resetPos == null) {
            reset(null);
        }
    }

    /*
     * Force current area to be added to parent area.
     */
    protected void flush() {
        viewportBlockArea.addBlock(curBlockArea, autoHeight);
        
        // Fake a 0 height for absolute positioned blocks.
        int height = viewportBlockArea.getHeight();
        if (viewportBlockArea.getPositioning() == Block.ABSOLUTE) {
            viewportBlockArea.setHeight(0);
        }
        super.flush();
        // Restore the right height.
        if (viewportBlockArea.getPositioning() == Block.ABSOLUTE) {
            viewportBlockArea.setHeight(height);
        }
    }
    
}

