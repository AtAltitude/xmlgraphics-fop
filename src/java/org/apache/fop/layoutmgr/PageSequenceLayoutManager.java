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

package org.apache.fop.layoutmgr;

import org.apache.fop.apps.FOPException;

import org.apache.fop.area.AreaTreeHandler;
import org.apache.fop.area.AreaTreeModel;
import org.apache.fop.area.Block;
import org.apache.fop.area.Footnote;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.Resolvable;

import org.apache.fop.datatypes.PercentBase;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.flow.Marker;
import org.apache.fop.fo.flow.RetrieveMarker;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.Region;
import org.apache.fop.fo.pagination.SideRegion;
import org.apache.fop.fo.pagination.SimplePageMaster;
import org.apache.fop.fo.pagination.StaticContent;

import org.apache.fop.traits.MinOptMax;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * LayoutManager for a PageSequence.  This class is instantiated by
 * area.AreaTreeHandler for each fo:page-sequence found in the
 * input document.
 */
public class PageSequenceLayoutManager extends AbstractLayoutManager {

    /** 
     * AreaTreeHandler which activates the PSLM and controls
     * the rendering of its pages.
     */
    private AreaTreeHandler areaTreeHandler;

    /** 
     * fo:page-sequence formatting object being
     * processed by this class
     */
    private PageSequence pageSeq;

    /** 
     * Current page-viewport-area being filled by
     * the PSLM.
     */
    private PageViewport curPV = null;

    /**
     * The FlowLayoutManager object, which processes
     * the single fo:flow of the fo:page-sequence
     */
    private FlowLayoutManager childFLM = null;

    /**
     * The collection of StaticContentLayoutManager objects that
     * are associated with this Page Sequence, keyed by flow-name.
     */
    //private HashMap staticContentLMs = new HashMap(4);

    private int startPageNum = 0;
    private int currentPageNum = 0;

    private Block separatorArea = null;
    
    /**
     * Constructor
     *
     * @param ath the area tree handler object
     * @param pseq fo:page-sequence to process
     */
    public PageSequenceLayoutManager(AreaTreeHandler ath, PageSequence pseq) {
        super(pseq);
        this.areaTreeHandler = ath;
        this.pageSeq = pseq;
    }

    /**
     * @see org.apache.fop.layoutmgr.LayoutManager
     * @return the LayoutManagerMaker object
     */
    public LayoutManagerMaker getLayoutManagerMaker() {
        return areaTreeHandler.getLayoutManagerMaker();
    }

    /**
     * Start the layout of this page sequence.
     * This completes the layout of the page sequence
     * which creates and adds all the pages to the area tree.
     */
    public void activateLayout() {
        startPageNum = pageSeq.getStartingPageNumber();
        currentPageNum = startPageNum - 1;

        LineArea title = null;

        if (pageSeq.getTitleFO() != null) {
            ContentLayoutManager clm = new ContentLayoutManager(pageSeq
                    .getTitleFO(), this);
            title = (LineArea) clm.getParentArea(null);
        }

        areaTreeHandler.getAreaTreeModel().startPageSequence(title);
        log.debug("Starting layout");

        curPV = makeNewPage(false, true, false);

        PageBreaker breaker = new PageBreaker(this);
        int flowBPD = (int) curPV.getBodyRegion().getBPD();
        breaker.doLayout(flowBPD);
        
        finishPage();
        pageSeq.getRoot().notifyPageSequenceFinished(currentPageNum,
                (currentPageNum - startPageNum) + 1);
        log.debug("Ending layout");
    }

    private class PageBreaker extends AbstractBreaker {
        
        private PageSequenceLayoutManager pslm;
        private boolean firstPart = true;
        
        private StaticContentLayoutManager footnoteSeparatorLM = null;
        private LinkedList footnoteSeparatorList = null;

        public PageBreaker(PageSequenceLayoutManager pslm) {
            this.pslm = pslm;
        }
        
        protected LayoutContext createLayoutContext() {
            LayoutContext lc = new LayoutContext(0);
            int flowIPD = curPV.getCurrentSpan().getColumnWidth();
            lc.setRefIPD(flowIPD);
            return lc;
        }
        
        protected LayoutManager getTopLevelLM() {
            return pslm;
        }
        
        protected LinkedList getNextKnuthElements(LayoutContext context, int alignment) {
            LinkedList contentList = pslm.getNextKnuthElements(context, alignment);

            // scan contentList, searching for footnotes
            boolean bFootnotesPresent = false;
            if (contentList != null) {
                ListIterator contentListIterator = contentList.listIterator();
                while (contentListIterator.hasNext()) {
                    KnuthElement element = (KnuthElement) contentListIterator.next();
                    if (element instanceof KnuthBlockBox
                        && ((KnuthBlockBox) element).hasAnchors()) {
                        // element represents a line with footnote citations
                        bFootnotesPresent = true;
                        LinkedList footnoteBodyLMs = ((KnuthBlockBox) element).getFootnoteBodyLMs();
                        ListIterator footnoteBodyIterator = footnoteBodyLMs.listIterator();
                        // store the lists of elements representing the footnote bodies
                        // in the box representing the line containing their references
                        while (footnoteBodyIterator.hasNext()) {
                            FootnoteBodyLayoutManager fblm = (FootnoteBodyLayoutManager) footnoteBodyIterator.next();
                            fblm.setParent(childFLM);
                            ((KnuthBlockBox) element).addElementList(fblm.getNextKnuthElements(context, alignment));
                        }
                    }
                }
            }

            // handle the footnote separator
            StaticContent footnoteSeparator;
            if (bFootnotesPresent
                && (footnoteSeparator = pageSeq.getStaticContent("xsl-footnote-separator")) != null) {
                // the footnote separator can contain page-dependent content such as
                // page numbers or retrieve markers, so its areas cannot simply be 
                // obtained now and repeated in each page;
                // we need to know in advance the separator bpd: the actual separator
                // could be different from page to page, but its bpd would likely be
                // always the same

                // create a Block area that will contain the separator areas
                separatorArea = new Block();
                separatorArea.setIPD(context.getRefIPD());
                // create a StaticContentLM for the footnote separator
                footnoteSeparatorLM = (StaticContentLayoutManager)
                    getLayoutManagerMaker().makeStaticContentLayoutManager(
                    pslm, footnoteSeparator, separatorArea);
                footnoteSeparatorLM.doLayout();

                footnoteSeparatorLength = new MinOptMax(separatorArea.getBPD());
            }
            return contentList;
        }
        
        protected int getCurrentDisplayAlign() {
            return curPV.getSPM().getRegion(Constants.FO_REGION_BODY).getDisplayAlign();
        }
        
        protected boolean hasMoreContent() {
            return !isFinished();
        }
        
        protected void addAreas(PositionIterator posIter, LayoutContext context) {
            if (footnoteSeparatorLM != null) {
                StaticContent footnoteSeparator = pageSeq.getStaticContent("xsl-footnote-separator");
                // create a Block area that will contain the separator areas
                separatorArea = new Block();
                separatorArea.setIPD(curPV.getCurrentSpan().getColumnWidth());
                // create a StaticContentLM for the footnote separator
                footnoteSeparatorLM = (StaticContentLayoutManager)
                    getLayoutManagerMaker().makeStaticContentLayoutManager(
                    pslm, footnoteSeparator, separatorArea);
                footnoteSeparatorLM.doLayout();
            }

            getCurrentChildLM().addAreas(posIter, context);    
        }
        
        protected void doPhase3(PageBreakingAlgorithm alg, int partCount, 
                BlockSequence originalList, BlockSequence effectiveList) {
            //Directly add areas after finding the breaks
            addAreas(alg, partCount, originalList, effectiveList);
        }
        
        protected void startPart(BlockSequence list, boolean bIsFirstPage) {
            if (curPV == null) {
                throw new IllegalStateException("curPV must not be null");
            } else {
                //firstPart is necessary because we need the first page before we start the 
                //algorithm so we have a BPD and IPD. This may subject to change later when we
                //start handling more complex cases.
                if (!firstPart) {
                    if (curPV.getCurrentSpan().hasMoreFlows()) {
                        curPV.getCurrentSpan().moveToNextFlow();
                    } else  {
                        // if this is the first page that will be created by
                        // the current BlockSequence, it could have a break
                        // condition that must be satisfied;
                        // otherwise, we may simply need a new page
                        handleBreakTrait(bIsFirstPage ? list.getStartOn() : Constants.EN_PAGE);
                    }
                }
            }
            // add static areas and resolve any new id areas
            // finish page and add to area tree
            firstPart = false;
        }
        
        protected void finishPart(PageBreakingAlgorithm alg, PageBreakPosition pbp) {
            // add footnote areas
            if (pbp.footnoteFirstListIndex < pbp.footnoteLastListIndex
                || pbp.footnoteFirstElementIndex <= pbp.footnoteLastElementIndex) {
                // call addAreas() for each FootnoteBodyLM
                for (int i = pbp.footnoteFirstListIndex; i <= pbp.footnoteLastListIndex; i++) {
                    LinkedList elementList = alg.getFootnoteList(i);
                    int firstIndex = (i == pbp.footnoteFirstListIndex ? pbp.footnoteFirstElementIndex : 0);
                    int lastIndex = (i == pbp.footnoteLastListIndex ? pbp.footnoteLastElementIndex : elementList.size() - 1);

                    FootnoteBodyLayoutManager fblm = (FootnoteBodyLayoutManager)
                                                     ((KnuthElement) elementList.getFirst()).getLayoutManager();
                    LayoutContext childLC = new LayoutContext(0);
                    fblm.addAreas(new KnuthPossPosIter(elementList, firstIndex, lastIndex + 1), childLC);
                }
                // set the offset from the top margin
                Footnote parentArea = (Footnote) getCurrentPV().getBodyRegion().getFootnote();
                int topOffset = (int) curPV.getBodyRegion().getBPD() - parentArea.getBPD();
                if (separatorArea != null) {
                    topOffset -= separatorArea.getBPD();
                }
                parentArea.setTop(topOffset);
                parentArea.setSeparator(separatorArea);
            }
        }
        
        protected LayoutManager getCurrentChildLM() {
            return childFLM;
        }
        
    }
    
    /** @see org.apache.fop.layoutmgr.LayoutManager#isBogus() */
    public boolean isBogus() {
        return false;
    }
    
    /**
     * Get the next break possibility.
     * This finds the next break for a page which is always at the end
     * of the page.
     *
     * @param context the layout context for finding breaks
     * @return the break for the page
     */
    public LinkedList getNextKnuthElements(LayoutContext context, int alignment) {

        LayoutManager curLM; // currently active LM

        while ((curLM = getChildLM()) != null) {
            LinkedList returnedList = null;
            if (childFLM == null && (curLM instanceof FlowLayoutManager)) {
                childFLM = (FlowLayoutManager)curLM;
            } else {
                if (curLM != childFLM) {
                    log.error("PSLM> invalid child LM");
                }
            }

            LayoutContext childLC = new LayoutContext(0);
            childLC.setStackLimit(context.getStackLimit());
            childLC.setRefIPD(context.getRefIPD());

            if (!curLM.isFinished()) {
                int flowIPD = curPV.getCurrentSpan().getColumnWidth();
                int flowBPD = (int) curPV.getBodyRegion().getBPD();
                pageSeq.setLayoutDimension(PercentBase.REFERENCE_AREA_IPD, flowIPD);
                pageSeq.setLayoutDimension(PercentBase.REFERENCE_AREA_BPD, flowBPD);
                returnedList = curLM.getNextKnuthElements(childLC, alignment);
            }
            if (returnedList != null) {
                return returnedList;
            }
        }
        setFinished(true);
        return null;
    }

    /**
     * Provides access to the current page.
     * @return the current PageViewport
     */
    public PageViewport getCurrentPV() {
        return curPV;
    }

    /**
     * Provides access to this object
     * @return this PageSequenceLayoutManager instance
     */
    public PageSequenceLayoutManager getPSLM() {
        return this;
    }
    
    /**
     * This returns the first PageViewport that contains an id trait
     * matching the idref argument, or null if no such PV exists.
     *
     * @param idref the idref trait needing to be resolved 
     * @return the first PageViewport that contains the ID trait
     */
    public PageViewport getFirstPVWithID(String idref) {
        List list = areaTreeHandler.getPageViewportsContainingID(idref);
        if (list != null && list.size() > 0) {
            return (PageViewport) list.get(0);
        }
        return null;
    }

    /**
     * Add an ID reference to the current page.
     * When adding areas the area adds its ID reference.
     * For the page layout manager it adds the id reference
     * with the current page to the area tree.
     *
     * @param id the ID reference to add
     */
    public void addIDToPage(String id) {
        if (id != null && id.length() > 0) {
            areaTreeHandler.associateIDWithPageViewport(id, curPV);
        }
    }

    /**
     * Identify an unresolved area (one needing an idref to be 
     * resolved, e.g. the internal-destination of an fo:basic-link)
     * for both the AreaTreeHandler and PageViewport object.
     * 
     * The AreaTreeHandler keeps a document-wide list of idref's
     * and the PV's needing them to be resolved.  It uses this to  
     * send notifications to the PV's when an id has been resolved.
     * 
     * The PageViewport keeps lists of id's needing resolving, along
     * with the child areas (page-number-citation, basic-link, etc.)
     * of the PV needing their resolution.
     *
     * @param id the ID reference to add
     * @param res the resolvable object that needs resolving
     */
    public void addUnresolvedArea(String id, Resolvable res) {
        curPV.addUnresolvedIDRef(id, res);
        areaTreeHandler.addUnresolvedIDRef(id, curPV);
    }

    /**
     * Bind the RetrieveMarker to the corresponding Marker subtree.
     * If the boundary is page then it will only check the
     * current page. For page-sequence and document it will
     * lookup preceding pages from the area tree and try to find
     * a marker.
     * If we retrieve a marker from a preceding page,
     * then the containing page does not have a qualifying area,
     * and all qualifying areas have ended.
     * Therefore we use last-ending-within-page (Constants.EN_LEWP)
     * as the position. 
     *
     * @param rm the RetrieveMarker instance whose properties are to
     * used to find the matching Marker.
     * @return a bound RetrieveMarker instance, or null if no Marker
     * could be found.
     */
    public RetrieveMarker resolveRetrieveMarker(RetrieveMarker rm) {
        AreaTreeModel areaTreeModel = areaTreeHandler.getAreaTreeModel();
        String name = rm.getRetrieveClassName();
        int pos = rm.getRetrievePosition();
        int boundary = rm.getRetrieveBoundary();               
        
        // get marker from the current markers on area tree
        Marker mark = (Marker)curPV.getMarker(name, pos);
        if (mark == null && boundary != EN_PAGE) {
            // go back over pages until mark found
            // if document boundary then keep going
            boolean doc = boundary == EN_DOCUMENT;
            int seq = areaTreeModel.getPageSequenceCount();
            int page = areaTreeModel.getPageCount(seq) - 1;
            while (page < 0 && doc && seq > 1) {
                seq--;
                page = areaTreeModel.getPageCount(seq) - 1;
            }
            while (page >= 0) {
                PageViewport pv = areaTreeModel.getPage(seq, page);
                mark = (Marker)pv.getMarker(name, Constants.EN_LEWP);
                if (mark != null) {
                    break;
                }
                page--;
                if (page < 0 && doc && seq > 1) {
                    seq--;
                    page = areaTreeModel.getPageCount(seq) - 1;
                }
            }
        }

        if (mark == null) {
            log.debug("found no marker with name: " + name);
            return null;
        } else {
            rm.bindMarker(mark);
            return rm;
        }
    }

    private PageViewport makeNewPage(boolean bIsBlank, boolean bIsFirst, boolean bIsLast) {
        if (curPV != null) {
            finishPage();
        }

        currentPageNum++;
        String pageNumberString = pageSeq.makeFormattedPageNumber(currentPageNum);

        try {
            // create a new page
            SimplePageMaster spm = pageSeq.getSimplePageMasterToUse(
                currentPageNum, bIsFirst, bIsBlank);
            
            Region body = spm.getRegion(FO_REGION_BODY);
            if (!pageSeq.getMainFlow().getFlowName().equals(body.getRegionName())) {
              // this is fine by the XSL Rec (fo:flow's flow-name can be mapped to
              // any region), but we don't support it yet.
              throw new FOPException("Flow '" + pageSeq.getMainFlow().getFlowName()
                 + "' does not map to the region-body in page-master '"
                 + spm.getMasterName() + "'.  FOP presently "
                 + "does not support this.");
            }
            curPV = new PageViewport(spm, pageNumberString);
        } catch (FOPException fopex) {
            throw new IllegalArgumentException("Cannot create page: " + fopex.getMessage());
        }

        if (log.isDebugEnabled()) {
            log.debug("[" + curPV.getPageNumberString() + (bIsBlank ? "*" : "") + "]");
        }

        curPV.createSpan(false);
        return curPV;
    }

    /* TODO: See if can initialize the SCLM's just once for
     * the page sequence, instead of after every page.
     */
    private void layoutSideRegion(int regionID) {
        SideRegion reg = (SideRegion)curPV.getSPM().getRegion(regionID);
        if (reg == null) {
            return;
        }
        StaticContent sc = pageSeq.getStaticContent(reg.getRegionName());
        if (sc == null) {
            return;
        }

        StaticContentLayoutManager lm = (StaticContentLayoutManager)
            getLayoutManagerMaker().makeStaticContentLayoutManager(
                    this, sc, reg);
        lm.doLayout();
        lm.reset(null);
    }

    private void finishPage() {
        // Layout side regions
        layoutSideRegion(FO_REGION_BEFORE); 
        layoutSideRegion(FO_REGION_AFTER);
        layoutSideRegion(FO_REGION_START);
        layoutSideRegion(FO_REGION_END);
        
        // Try to resolve any unresolved IDs for the current page.
        // 
        areaTreeHandler.tryIDResolution(curPV);
        // Queue for ID resolution and rendering
        areaTreeHandler.getAreaTreeModel().addPage(curPV);
        log.debug("page finished: " + curPV.getPageNumberString() 
                + ", current num: " + currentPageNum);
        curPV = null;
    }
    
    /**
     * Depending on the kind of break condition, move to next column
     * or page. May need to make an empty page if next page would
     * not have the desired "handedness".
     * @param breakVal - value of break-before or break-after trait.
     */
    private void handleBreakTrait(int breakVal) {
        if (breakVal == Constants.EN_COLUMN) {
            if (curPV.getCurrentSpan().hasMoreFlows()) {
                curPV.getCurrentSpan().moveToNextFlow();
            } else {
                curPV = makeNewPage(false, false, false);
            }
            return;
        }
        log.debug("handling break-before after page " + currentPageNum 
            + " breakVal=" + breakVal);
        if (needBlankPageBeforeNew(breakVal)) {
            curPV = makeNewPage(true, false, false);
        }
        if (needNewPage(breakVal)) {
            curPV = makeNewPage(false, false, false);
        }
    }

    /**
     * If we have already started to layout content on a page,
     * and there is a forced break, see if we need to generate
     * an empty page.
     * Note that if not all content is placed, we aren't sure whether
     * it will flow onto another page or not, so we'd probably better
     * block until the queue of layoutable stuff is empty!
     * @param breakVal - value of break-before or break-after trait.
     */
    private boolean needBlankPageBeforeNew(int breakVal) {
        if (breakVal == Constants.EN_PAGE || (curPV.getPage().isEmpty())) {
            // any page is OK or we already have an empty page
            return false;
        } else {
            /* IF we are on the kind of page we need, we'll need a new page. */
            if (currentPageNum % 2 == 0) { // even page
                return (breakVal == Constants.EN_EVEN_PAGE);
            } else { // odd page
                return (breakVal == Constants.EN_ODD_PAGE);
            }
        }
    }

    /**
     * See if need to generate a new page
     * @param breakVal - value of break-before or break-after trait.
     */
    private boolean needNewPage(int breakVal) {
        if (curPV.getPage().isEmpty()) {
            if (breakVal == Constants.EN_PAGE) {
                return false;
            }
            else if (currentPageNum % 2 == 0) { // even page
                return (breakVal == Constants.EN_ODD_PAGE);
            }
            else { // odd page
                return (breakVal == Constants.EN_EVEN_PAGE);
            }
        } else {
            return true;
        }
    }
}
