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
import org.apache.fop.fo.flow.TableRow;
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

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * LayoutManager for a table-row FO.
 * The row contains cells that are organised according to the columns.
 * A break in a table row will contain breaks for each table cell.
 * If there are row spanning cells then these cells belong to this row
 * but effect the occupied columns of future rows.
 */
public class Row extends BlockStackingLayoutManager {
    private TableRow fobj;
    
    private List cellList = null;
    private List columns = null;
    private int referenceIPD;
    private int rowHeight;
    private int xoffset;
    private int yoffset;

    private class RowPosition extends LeafPosition {
        protected List cellBreaks;
        protected RowPosition(LayoutManager lm, int pos, List l) {
            super(lm, pos);
            cellBreaks = l;
        }
    }

    /**
     * Create a new row layout manager.
     *
     */
    public Row(TableRow node) {
        super(node);
        fobj = node;
    }

    /**
     * @return the table owning this row
     */
    public Table getTable() {
        FONode node = fobj.getParent();
        while (!(node instanceof Table)) {
            node = node.getParent();
        }
        return (Table)node;
    }
    
    /**
     * Set the columns from the table.
     *
     * @param cols the list of columns for this table
     */
    public void setColumns(List cols) {
        columns = cols;
    }

    private void setupCells() {
        cellList = new ArrayList();
        // add cells to list
        while (childLMiter.hasNext()) {
            curChildLM = (LayoutManager) childLMiter.next();
            curChildLM.setParent(this);
            curChildLM.initialize();
            cellList.add(curChildLM);
        }
    }

    /**
     * Get the layout manager for a cell.
     *
     * @param pos the position of the cell
     * @return the cell layout manager
     */
    protected Cell getCellLM(int pos) {
        if (cellList == null) {
            setupCells();
        }
        if (pos < cellList.size()) {
            return (Cell)cellList.get(pos);
        }
        return null;
    }

    /**
     * Get the next break possibility.
     * A row needs to get the possible breaks for each cell
     * in the row and find a suitable break across all cells.
     *
     * @param context the layout context for getting breaks
     * @return the next break possibility
     */
    public BreakPoss getNextBreakPoss(LayoutContext context) {
        LayoutManager curLM; // currently active LM

        BreakPoss lastPos = null;
        List breakList = new java.util.ArrayList();
        List spannedColumns = new java.util.ArrayList();

        int min = 0;
        int opt = 0;
        int max = 0;

        int startColumn = 1;
        int cellLMIndex = 0;
        boolean over = false;

        while ((curLM = getCellLM(cellLMIndex++)) != null) {
            Cell cellLM = (Cell)curLM;
            
            List childBreaks = new ArrayList();
            MinOptMax stackSize = new MinOptMax();

            // Set up a LayoutContext
            // the ipd is from the current column
            referenceIPD = context.getRefIPD();
            BreakPoss bp;

            LayoutContext childLC = new LayoutContext(0);
            childLC.setStackLimit(
                  MinOptMax.subtract(context.getStackLimit(),
                                     stackSize));

            getColumnsForCell(cellLM, startColumn, spannedColumns);
            int childRefIPD = 0;
            Iterator i = spannedColumns.iterator();
            while (i.hasNext()) {
                Column col = (Column)i.next();
                childRefIPD += col.getWidth().getValue();
            }
            //Handle border-separation when border-collapse="separate"
            if (getTable().getBorderCollapse() == EN_SEPARATE) {
                childRefIPD += (spannedColumns.size() - 1) 
                    * getTable().getBorderSeparation().getIPD().getLength().getValue();
            }
            childLC.setRefIPD(childRefIPD);

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
            // the min is the maximum min of all cells
            if (stackSize.min > min) {
                min = stackSize.min;
            }
            // the optimum is the maximum of all optimums
            if (stackSize.opt > opt) {
                opt = stackSize.opt;
            }
            // the maximum is the largest maximum
            if (stackSize.max > max) {
                max = stackSize.max;
            }

            breakList.add(childBreaks);
            
            startColumn += cellLM.getFObj().getNumberColumnsSpanned();
        }
        MinOptMax rowSize = new MinOptMax(min, opt, max);
        LengthRangeProperty specifiedBPD = fobj.getBlockProgressionDimension();
        if (specifiedBPD.getEnum() != EN_AUTO) {
            if ((specifiedBPD.getMaximum().getEnum() != EN_AUTO)
                    && (specifiedBPD.getMaximum().getLength().getValue() < rowSize.min)) {
                log.warn("maximum height of row is smaller than the minimum "
                        + "height of its contents");
            }
            MinOptMaxUtil.restrict(rowSize, specifiedBPD);
        }
        rowHeight = rowSize.opt;

        boolean fin = true;
        cellLMIndex = 0;
        //Check if any of the cell LMs haven't finished, yet
        while ((curLM = getCellLM(cellLMIndex++)) != null) {
            if (!curLM.isFinished()) {
                fin = false;
                break;
            }
        }

        setFinished(fin);
        RowPosition rp = new RowPosition(this, breakList.size() - 1, breakList);
        BreakPoss breakPoss = new BreakPoss(rp);
        if (over) {
            breakPoss.setFlag(BreakPoss.NEXT_OVERFLOWS, true);
        }
        breakPoss.setStackingSize(rowSize);
        return breakPoss;
    }

    /**
     * Gets the Column at a given index.
     * @param index index of the column (index must be >= 1)
     * @return the requested Column
     */
    private Column getColumn(int index) {
        int size = columns.size();
        if (index > size - 1) {
            return (Column)columns.get(size - 1);
        } else {
            return (Column)columns.get(index - 1);
        }
    }
    
    /**
     * Determines the columns that are spanned by the given cell.
     * @param cellLM table-cell LM
     * @param startCell starting cell index (must be >= 1)
     * @param spannedColumns List to receive the applicable columns
     */
    private void getColumnsForCell(Cell cellLM, int startCell, List spannedColumns) {
        int count = cellLM.getFObj().getNumberColumnsSpanned();
        spannedColumns.clear();
        for (int i = 0; i < count; i++) {
            spannedColumns.add(getColumn(startCell + i));
        }
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
        LayoutManager curLM; // currently active LM
        int cellcount = 0;

        if (pos == null) {
            while ((curLM = getCellLM(cellcount)) != null) {
                curLM.resetPosition(null);
                cellcount++;
            }
        } else {
            RowPosition rpos = (RowPosition)pos;
            List breaks = rpos.cellBreaks;

            while ((curLM = getCellLM(cellcount)) != null) {
                List childbreaks = (List)breaks.get(cellcount);
                curLM.resetPosition((Position)childbreaks.get(childbreaks.size() - 1));
                cellcount++;
            }
        }

        setFinished(false);
    }

    /**
     * Set the x position offset of this row.
     * This is used to set the position of the areas returned by this row.
     *
     * @param off the x offset
     */
    public void setXOffset(int off) {
        xoffset = off;
    }
    
    /**
     * Set the y position offset of this row.
     * This is used to set the position of the areas returned by this row.
     *
     * @param off the y offset
     */
    public void setYOffset(int off) {
        yoffset = off;
    }

    /**
     * Add the areas for the break points.
     * This sets the offset of each cell as it is added.
     *
     * @param parentIter the position iterator
     * @param layoutContext the layout context for adding areas
     */
    public void addAreas(PositionIterator parentIter,
                         LayoutContext layoutContext) {
        getParentArea(null);
        BreakPoss bp1 = (BreakPoss)parentIter.peekNext();
        bBogus = !bp1.generatesAreas();
        if (!isBogus()) {
            addID(fobj.getId());
        }

        Cell childLM;
        int iStartPos = 0;
        LayoutContext lc = new LayoutContext(0);
        while (parentIter.hasNext()) {
            RowPosition lfp = (RowPosition) parentIter.next();
            
            //area exclusively for painting the row background
            Block rowArea = getRowArea();
            if (rowArea != null) {
                rowArea.setBPD(rowHeight);
                rowArea.setIPD(referenceIPD);
                rowArea.setXOffset(xoffset);
                rowArea.setYOffset(yoffset);
                parentLM.addChild(rowArea);
            }

            int cellcount = 0;
            int x = this.xoffset;
            //int x = (TableLayoutManager)getParent()).;
            for (Iterator iter = lfp.cellBreaks.iterator(); iter.hasNext();) {
                List cellsbr = (List)iter.next();
                BreakPossPosIter breakPosIter;
                breakPosIter = new BreakPossPosIter(cellsbr, 0, cellsbr.size());
                iStartPos = lfp.getLeafPos() + 1;

                int cellWidth = 0;
                while ((childLM = (Cell)breakPosIter.getNextChildLM()) != null) {
                    cellWidth = childLM.getReferenceIPD();
                    childLM.setXOffset(x);
                    childLM.setYOffset(yoffset);
                    childLM.setRowHeight(rowHeight);
                    childLM.addAreas(breakPosIter, lc);
                }
                x += cellWidth;
                
                //Handle border-separation
                Table table = getTable();
                if (table.getBorderCollapse() == EN_SEPARATE) {
                    x += table.getBorderSeparation().getIPD().getLength().getValue();
                }
            }
        }

        flush();
    }

    /**
     * Get the row height of the row after adjusting.
     * Should only be called after adding the row areas.
     *
     * @return the row height of this row after adjustment
     */
    public int getRowHeight() {
        return rowHeight;
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
     * @param childArea the child area
     * @return the parent are for the child
     */
    public Area getParentArea(Area childArea) {
        return parentLM.getParentArea(childArea);
    }

    /**
     * Add the child.
     * Rows return the areas returned by the child elements.
     * This simply adds the area to the parent layout manager.
     *
     * @param childArea the child area
     */
    public void addChild(Area childArea) {
        parentLM.addChild(childArea);
    }

    /**
     * Reset the position of this layout manager.
     *
     * @param resetPos the position to reset to
     */
    public void resetPosition(Position resetPos) {
        if (resetPos == null) {
            reset(null);
        }
    }


    /**
     * Get the area for this row for background.
     *
     * @return the row area
     */
    public Block getRowArea() {
        if (fobj.getCommonBorderPaddingBackground().hasBackground()) {
            Block block = new Block();
            block.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
            block.setPositioning(Block.ABSOLUTE);
            TraitSetter.addBackground(block, fobj.getCommonBorderPaddingBackground());
            return block;
        } else {
            return null;
        }
    }

}

