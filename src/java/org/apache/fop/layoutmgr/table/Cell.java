/*
 * Copyright 1999-2005 The Apache Software Foundation.
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
 
package org.apache.fop.layoutmgr.table;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.flow.Table;
import org.apache.fop.fo.flow.TableCell;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.apache.fop.layoutmgr.BlockStackingLayoutManager;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.LeafPosition;
import org.apache.fop.layoutmgr.BreakPoss;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.MinOptMaxUtil;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.BreakPossPosIter;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.Trait;
import org.apache.fop.traits.MinOptMax;

import java.util.ArrayList;
import java.util.List;

/**
 * LayoutManager for a table-cell FO.
 * A cell contains blocks. These blocks fill the cell.
 */
public class Cell extends BlockStackingLayoutManager {
    private TableCell fobj;
    
    private Block curBlockArea;

    private List childBreaks = new ArrayList();

    private int inRowIPDOffset;
    
    private int xoffset;
    private int yoffset;
    private int referenceIPD;
    private int cellIPD;
    private int rowHeight;
    private int usedBPD;
    private int startBorderWidth;
    private int endBorderWidth;
    private int borderAndPaddingBPD;
    private boolean emptyCell = true;

    /** List of Lists containing GridUnit instances, one List per row. */
    private List rows = new java.util.ArrayList(); 
    
    /**
     * Create a new Cell layout manager.
     * @node table-cell FO for which to create the LM
     */
    public Cell(TableCell node) {
        super(node);
        fobj = node;
    }

    /** @return the table-cell FO */
    public TableCell getFObj() {
        return this.fobj;
    }
    
    /**
     * @see org.apache.fop.layoutmgr.AbstractLayoutManager#initProperties()
     */
    protected void initProperties() {
        super.initProperties();
        borderAndPaddingBPD = 0;
        borderAndPaddingBPD += fobj.getCommonBorderPaddingBackground().getBorderBeforeWidth(false);
        borderAndPaddingBPD += fobj.getCommonBorderPaddingBackground().getBorderAfterWidth(false);
        if (!fobj.isSeparateBorderModel()) {
            borderAndPaddingBPD /= 2;
        }
        borderAndPaddingBPD += fobj.getCommonBorderPaddingBackground().getPaddingBefore(false);
        borderAndPaddingBPD += fobj.getCommonBorderPaddingBackground().getPaddingAfter(false);
    }
    
    /**
     * @return the table owning this cell
     */
    public Table getTable() {
        FONode node = fobj.getParent();
        while (!(node instanceof Table)) {
            node = node.getParent();
        }
        return (Table)node;
    }
    

    /**
     * Called by Row LM to register the grid units occupied by this cell for a row.
     * @param spannedGridUnits a List of GridUnits
     */
    public void addGridUnitsFromRow(List spannedGridUnits) {
        log.debug("Getting another row, " + spannedGridUnits.size() + " grid units");
        this.rows.add(spannedGridUnits);
        
    }

    private int getIPIndents() {
        int iIndents = 0;
        startBorderWidth = 0;
        endBorderWidth = 0;
        for (int i = 0; i < rows.size(); i++) {
            List gridUnits = (List)rows.get(i);
            startBorderWidth = Math.max(startBorderWidth, 
                    ((GridUnit)gridUnits.get(0)).
                        effBorders.getBorderStartWidth(false));
            endBorderWidth = Math.max(endBorderWidth, 
                    ((GridUnit)gridUnits.get(gridUnits.size() - 1)).
                        effBorders.getBorderEndWidth(false));
        }
        //iIndents += fobj.getCommonBorderPaddingBackground().getBorderStartWidth(false);
        iIndents += startBorderWidth;
        //iIndents += fobj.getCommonBorderPaddingBackground().getBorderEndWidth(false);
        iIndents += endBorderWidth;
        if (!fobj.isSeparateBorderModel()) {
            iIndents /= 2;
        }
        iIndents += fobj.getCommonBorderPaddingBackground().getPaddingStart(false);
        iIndents += fobj.getCommonBorderPaddingBackground().getPaddingEnd(false);
        return iIndents;
    }
    
    /**
     * Get the next break possibility for this cell.
     * A cell contains blocks so there are breaks around the blocks
     * and inside the blocks.
     *
     * @param context the layout context
     * @return the next break possibility
     */
    public BreakPoss getNextBreakPoss(LayoutContext context) {
        LayoutManager curLM; // currently active LM

        MinOptMax stackSize = new MinOptMax();
        BreakPoss lastPos = null;

        referenceIPD = context.getRefIPD(); 
        cellIPD = referenceIPD;
        cellIPD -= getIPIndents();
        if (fobj.isSeparateBorderModel()) {
            int borderSep = fobj.getBorderSeparation().getLengthPair()
                    .getIPD().getLength().getValue();
            cellIPD -= borderSep;
        }

        while ((curLM = getChildLM()) != null) {
            if (curLM.generatesInlineAreas()) {
                log.error("table-cell must contain block areas - ignoring");
                curLM.setFinished(true);
                continue;
            }
            // Set up a LayoutContext
            BreakPoss bp;

            LayoutContext childLC = new LayoutContext(0);
            childLC.setStackLimit(MinOptMax.subtract(context.getStackLimit(),
                                     stackSize));
            childLC.setRefIPD(cellIPD);

            boolean over = false;

            while (!curLM.isFinished()) {
                if ((bp = curLM.getNextBreakPoss(childLC)) != null) {
                    if (stackSize.opt + bp.getStackingSize().opt > context.getStackLimit().max) {
                        // reset to last break
                        if (lastPos != null) {
                            LayoutManager lm = lastPos.getLayoutManager();
                            lm.resetPosition(lastPos.getPosition());
                            if (lm != curLM) {
                                curLM.resetPosition(null);
                            }
                        } else {
                            curLM.resetPosition(null);
                        }
                        over = true;
                        break;
                    }
                    stackSize.add(bp.getStackingSize());
                    lastPos = bp;
                    childBreaks.add(bp);

                    if (bp.nextBreakOverflows()) {
                        over = true;
                        break;
                    }

                    childLC.setStackLimit(MinOptMax.subtract(
                                             context.getStackLimit(), stackSize));
                }
            }
            
            usedBPD = stackSize.opt;
            if (usedBPD > 0) {
                emptyCell = false;
            }
            
            LengthRangeProperty specifiedBPD = fobj.getBlockProgressionDimension();
            if (specifiedBPD.getEnum() != EN_AUTO) {
                if ((specifiedBPD.getMaximum().getEnum() != EN_AUTO)
                        && (specifiedBPD.getMaximum().getLength().getValue() < stackSize.min)) {
                    log.warn("maximum height of cell is smaller than the minimum "
                            + "height of its contents");
                }
                MinOptMaxUtil.restrict(stackSize, specifiedBPD);
            }
            stackSize = MinOptMax.add(stackSize, new MinOptMax(borderAndPaddingBPD));

            BreakPoss breakPoss = new BreakPoss(
                                    new LeafPosition(this, childBreaks.size() - 1));
            if (over) {
                breakPoss.setFlag(BreakPoss.NEXT_OVERFLOWS, true);
            }
            breakPoss.setStackingSize(stackSize);
            return breakPoss;
        }
        setFinished(true);
        return null;
    }

    /**
     * Set the y offset of this cell.
     * This offset is used to set the absolute position of the cell.
     *
     * @param off the y direction offset
     */
    public void setYOffset(int off) {
        yoffset = off;
    }

    /**
     * Set the x offset of this cell (usually the same as its parent row).
     * This offset is used to determine the absolute position of the cell.
     *
     * @param off the x offset
     */
    public void setXOffset(int off) {
        xoffset = off;
    }

    /**
     * Set the IPD offset of this cell inside the table-row.
     * This offset is used to determine the absolute position of the cell.
     * @param off the IPD offset
     */
    public void setInRowIPDOffset(int off) {
        this.inRowIPDOffset = off;
    }
    
    /**
     * Set the row height that contains this cell. This method is used during
     * addAreas() stage.
     *
     * @param h the height of the row
     */
    public void setRowHeight(int h) {
        rowHeight = h;
    }

    /**
     * Add the areas for the break points.
     * The cell contains block stacking layout managers
     * that add block areas.
     *
     * @param parentIter the iterator of the break positions
     * @param layoutContext the layout context for adding the areas
     */
    public void addAreas(PositionIterator parentIter,
                         LayoutContext layoutContext) {
        getParentArea(null);
        BreakPoss bp1 = (BreakPoss)parentIter.peekNext();
        bBogus = !bp1.generatesAreas(); 

        if (!isBogus()) {
            addID(fobj.getId());
        }

        if (fobj.isSeparateBorderModel()) {
            if (!emptyCell || fobj.showEmptyCells()) {
                TraitSetter.addBorders(curBlockArea, fobj.getCommonBorderPaddingBackground());
                TraitSetter.addBackground(curBlockArea, fobj.getCommonBorderPaddingBackground());
            }
        } else {
            TraitSetter.addBackground(curBlockArea, fobj.getCommonBorderPaddingBackground());
            //TODO Set these booleans right
            boolean[] outer = new boolean[] {false, false, false, false};
            if (rows.size() == 1 && ((List)rows.get(0)).size() == 1) {
                //Can set the borders directly if there's no span
                CommonBorderPaddingBackground effBorders =
                    ((GridUnit)((List)rows.get(0)).get(0)).effBorders;
                //TODO Next line is a temporary hack!
                TraitSetter.addCollapsingBorders(curBlockArea, 
                        fobj.getCommonBorderPaddingBackground(), outer);
                TraitSetter.addCollapsingBorders(curBlockArea, 
                        effBorders, outer);
            } else {
                int dy = yoffset;
                for (int y = 0; y < rows.size(); y++) {
                    List gridUnits = (List)rows.get(y);
                    int dx = xoffset;
                    int lastRowHeight = 0;
                    for (int x = 0; x < gridUnits.size(); x++) {
                        GridUnit gu = (GridUnit)gridUnits.get(x);
                        if (!gu.effBorders.hasBorder()) {
                            continue;
                        }
                        
                        //Blocks for painting grid unit borders
                        Block block = new Block();
                        block.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
                        block.setPositioning(Block.ABSOLUTE);
                        block.setBPD(gu.row.getRowHeight());
                        lastRowHeight = gu.row.getRowHeight();
                        int ipd = gu.column.getWidth().getValue();
                        int borderStartWidth = gu.effBorders.getBorderStartWidth(false) / 2; 
                        ipd -= borderStartWidth;
                        ipd -= gu.effBorders.getBorderEndWidth(false) / 2;
                        block.setIPD(ipd);
                        block.setXOffset(dx + borderStartWidth);
                        block.setYOffset(dy);
                        TraitSetter.addCollapsingBorders(block, gu.effBorders, outer);
                        parentLM.addChildArea(block);
                        dx += gu.column.getWidth().getValue();
                    }
                    dy += lastRowHeight;
                }
                log.warn("TODO Add collapsed border painting for spanned cells");
            }
        }

        //Handle display-align
        if (usedBPD < rowHeight) {
            if (fobj.getDisplayAlign() == EN_CENTER) {
                Block space = new Block();
                space.setBPD((rowHeight - usedBPD) / 2);
                curBlockArea.addBlock(space);
            } else if (fobj.getDisplayAlign() == EN_AFTER) {
                Block space = new Block();
                space.setBPD((rowHeight - usedBPD));
                curBlockArea.addBlock(space);
            }
        }

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

        
        int contentBPD = rowHeight;
        contentBPD -= borderAndPaddingBPD;
        curBlockArea.setBPD(contentBPD);

        flush();

        childBreaks.clear();
        curBlockArea = null;
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
     *
     * @param childArea the child area to get the parent for
     * @return the parent area
     */
    public Area getParentArea(Area childArea) {
        if (curBlockArea == null) {
            curBlockArea = new Block();
            curBlockArea.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
            curBlockArea.setPositioning(Block.ABSOLUTE);
            int indent = 0;
            indent += startBorderWidth;
            if (!fobj.isSeparateBorderModel()) {
                indent /= 2;
            }
            indent += fobj.getCommonBorderPaddingBackground().getPaddingStart(false);
            // set position
            int halfBorderSep = 0;
            if (fobj.isSeparateBorderModel()) {
                halfBorderSep = fobj.getBorderSeparation().getLengthPair()
                        .getIPD().getLength().getValue() / 2;
            }
            int halfCollapsingBorderHeight = 0;
            if (!fobj.isSeparateBorderModel()) {
                halfCollapsingBorderHeight += 
                    fobj.getCommonBorderPaddingBackground().getBorderBeforeWidth(false) / 2;
            }
            curBlockArea.setXOffset(xoffset + inRowIPDOffset + halfBorderSep + indent);
            curBlockArea.setYOffset(yoffset - halfCollapsingBorderHeight);
            curBlockArea.setIPD(cellIPD);
            //curBlockArea.setHeight();

            // Set up dimensions
            Area parentArea = parentLM.getParentArea(curBlockArea);
            // Get reference IPD from parentArea
            setCurrentArea(curBlockArea); // ??? for generic operations
        }
        return curBlockArea;
    }

    /**
     * Add the child to the cell block area.
     *
     * @param childArea the child to add to the cell
     */
    public void addChildArea(Area childArea) {
        if (curBlockArea != null) {
            curBlockArea.addBlock((Block) childArea);
        }
    }

    /**
     * Reset the position of the layout.
     *
     * @param resetPos the position to reset to
     */
    public void resetPosition(Position resetPos) {
        if (resetPos == null) {
            reset(null);
            childBreaks.clear();
        }
    }

}

