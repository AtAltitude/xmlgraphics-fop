/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.util.List;
import java.util.ListIterator;

import org.apache.fop.layoutmgr.ElementListUtils;
import org.apache.fop.layoutmgr.KnuthBox;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthPenalty;

class ActiveCell {
        private PrimaryGridUnit pgu;
        /** Knuth elements for this active cell. */
        private List elementList;
        /** Iterator over the Knuth element list. */
        private ListIterator knuthIter;
        private boolean prevIsBox = false;
        /** Number of the row where the row-span ends, zero-based. */
        private int endRowIndex;
        /** Index, in the list of Knuth elements, of the element starting the current step. */
        private int start;
        /** Index, in the list of Knuth elements, of the element ending the current step. */
        private int end;
        /**
         * Total length of the Knuth elements already included in the steps, up to the
         * current one.
         */
        private int width;
        private int remainingLength;
        private int previousRowsLength;
        private int totalLength;
        private int includedLength;
        private int borderBefore;
        private int borderAfter;
        private int paddingBefore;
        private int paddingAfter;
        private boolean keepWithNextSignal;
        private int lastPenaltyLength;

        ActiveCell(PrimaryGridUnit pgu, EffRow row, int rowIndex, int previousRowsLength, TableLayoutManager tableLM) {
            this.pgu = pgu;
            boolean makeBoxForWholeRow = false;
            if (row.getExplicitHeight().min > 0) {
                boolean contentsSmaller = ElementListUtils.removeLegalBreaks(
                        pgu.getElements(), row.getExplicitHeight());
                if (contentsSmaller) {
                    makeBoxForWholeRow = true;
                }
            }
            if (pgu.isLastGridUnitRowSpan() && pgu.getRow() != null) {
                makeBoxForWholeRow |= pgu.getRow().mustKeepTogether();
                makeBoxForWholeRow |= pgu.getTable().mustKeepTogether();
            }
            if (makeBoxForWholeRow) {
                elementList = new java.util.ArrayList(1);
                int height = row.getExplicitHeight().opt;
                if (height == 0) {
                    height = row.getHeight().opt;
                }
                elementList.add(new KnuthBoxCellWithBPD(height));
            } else {
                elementList = pgu.getElements();
//                if (log.isTraceEnabled()) {
//                    log.trace("column " + (column+1) + ": recording " + elementLists.size() + " element(s)");
//                }
            }
            knuthIter = elementList.listIterator();
            includedLength = -1;  // Avoid troubles with cells having content of zero length
            this.previousRowsLength = previousRowsLength;
            width = previousRowsLength;
            totalLength = ElementListUtils.calcContentLength(elementList);
            if (pgu.getTable().isSeparateBorderModel()) {
                borderBefore = pgu.getBorders().getBorderBeforeWidth(false)
                        + tableLM.getHalfBorderSeparationBPD();
                borderAfter = pgu.getBorders().getBorderAfterWidth(false)
                        + tableLM.getHalfBorderSeparationBPD();
            } else {
                borderBefore = pgu.getHalfMaxBeforeBorderWidth();
                borderAfter = pgu.getHalfMaxAfterBorderWidth();
            }
            paddingBefore = pgu.getBorders().getPaddingBefore(false, pgu.getCellLM());
            paddingAfter = pgu.getBorders().getPaddingAfter(false, pgu.getCellLM());
            start = 0;
            end = -1;
            endRowIndex = rowIndex + pgu.getCell().getNumberRowsSpanned() - 1;
            keepWithNextSignal = false;
            remainingLength = totalLength;
            goToNextLegalBreak();
        }

        boolean endsOnRow(int rowIndex) {
            return rowIndex == endRowIndex;
        }

        int getRemainingHeight(int activeRowIndex, EffRow[] rowGroup) {
            if (!endsOnRow(activeRowIndex)) {
                return 0;
            } else if (includedLength == totalLength) {
                return 0;
            } else {
                return remainingLength + borderBefore + borderAfter + paddingBefore + paddingAfter;
            }
        }

        private void goToNextLegalBreak() {
            lastPenaltyLength = 0;
            boolean breakFound = false;
            while (!breakFound && knuthIter.hasNext()) {
                KnuthElement el = (KnuthElement) knuthIter.next();
                if (el.isPenalty()) {
                    prevIsBox = false;
                    if (el.getP() < KnuthElement.INFINITE) {
                        //First legal break point
                        lastPenaltyLength = el.getW();
                        breakFound = true;
                    }
                } else if (el.isGlue()) {
                    if (prevIsBox) {
                        //Second legal break point
                        breakFound = true;
                    } else {
                        width += el.getW();
                    }
                    prevIsBox = false;
                } else {
                    prevIsBox = true;
                    width += el.getW();
                }
            }
            end = knuthIter.nextIndex() - 1;
        }

        int getNextStep() {
            if (!includedInLastStep()) {
                return width + lastPenaltyLength + borderBefore + borderAfter + paddingBefore + paddingAfter;
            } else {
                start = end + 1;
                if (knuthIter.hasNext()) {
                    goToNextLegalBreak();
                    return width + lastPenaltyLength + borderBefore + borderAfter + paddingBefore + paddingAfter; 
                } else {
                    return 0;
                }
            }
        }

        private boolean includedInLastStep() {
            return includedLength == width;
        }

        boolean signalMinStep(int minStep) {
            if (width + lastPenaltyLength + borderBefore + borderAfter + paddingBefore + paddingAfter <= minStep) {
                includedLength = width;
                computeRemainingLength();
                return false;
            } else {
                return previousRowsLength + borderBefore + borderAfter + paddingBefore + paddingAfter > minStep;
            }
        }

        private void computeRemainingLength() {
            remainingLength = totalLength - width;
            // Save the current location in the element list
            int oldIndex = knuthIter.nextIndex();
            KnuthElement el;
            while (knuthIter.hasNext()
                    && !(el = (KnuthElement) knuthIter.next()).isBox()) {
                if (el.isGlue()) {
                    remainingLength -= el.getW();
                }
            }
            // Reset the iterator to the current location
            while (knuthIter.nextIndex() > oldIndex) {
                knuthIter.previous();
            }
        }

        boolean contributesContent() {
            return includedInLastStep() && end >= start;
        }

        boolean hasStarted() {
            return includedLength > 0;
        }

        boolean isFinished() {
            return includedInLastStep() && (end == elementList.size() - 1);
        }

        GridUnitPart createGridUnitPart() {
            if (end + 1 == elementList.size()) {
                if (pgu.getFlag(GridUnit.KEEP_WITH_NEXT_PENDING)) {
                    keepWithNextSignal = true;
                }
                if (pgu.getRow() != null && pgu.getRow().mustKeepWithNext()) {
                    keepWithNextSignal = true;
                }
            }
            if (start == 0 && end == 0
                    && elementList.size() == 1
                    && elementList.get(0) instanceof KnuthBoxCellWithBPD) {
                //Special case: Cell with fixed BPD
                return new GridUnitPart(pgu, 0, pgu.getElements().size() - 1);
            } else {
                return new GridUnitPart(pgu, start, end);
            }
        }

        boolean isLastForcedBreak() {
            return ((KnuthElement)elementList.get(end)).isForcedBreak();
        }

        int getLastBreakClass() {
            return ((KnuthPenalty)elementList.get(end)).getBreakClass();
        }

        boolean keepWithNextSignal() {
            return keepWithNextSignal;
        }

        /**
         * Marker class denoting table cells fitting in just one box (no legal break inside).
         */
        private static class KnuthBoxCellWithBPD extends KnuthBox {

            public KnuthBoxCellWithBPD(int w) {
                super(w, null, true);
            }
        }
}
