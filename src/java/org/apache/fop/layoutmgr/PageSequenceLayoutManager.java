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

import org.apache.fop.area.CTM;
import org.apache.fop.area.AreaTreeHandler;
import org.apache.fop.area.AreaTreeModel;
import org.apache.fop.area.Area;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.NormalFlow;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.Page;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.RegionReference;
import org.apache.fop.area.BodyRegion;
import org.apache.fop.area.Span;
import org.apache.fop.area.BeforeFloat;
import org.apache.fop.area.Footnote;
import org.apache.fop.area.Resolvable;
import org.apache.fop.area.Trait;

import org.apache.fop.datatypes.PercentBase;
import org.apache.fop.datatypes.FODimension;

import org.apache.fop.fo.FObj;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.flow.Marker;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.Region;
import org.apache.fop.fo.pagination.RegionBody;
import org.apache.fop.fo.pagination.SimplePageMaster;
import org.apache.fop.fo.pagination.StaticContent;
import org.apache.fop.fo.properties.CommonMarginBlock;

import java.util.List;
import java.util.Map;
import java.awt.Rectangle;
import java.util.Iterator;
import java.awt.geom.Rectangle2D;
import org.apache.fop.traits.MinOptMax;

/**
 * LayoutManager for a PageSequence.
 */
public class PageSequenceLayoutManager extends AbstractLayoutManager {
    private PageSequence pageSeq;

    private static class BlockBreakPosition extends LeafPosition {
        protected BreakPoss breakps;

        protected BlockBreakPosition(LayoutManager lm, BreakPoss bp) {
            super(lm, 0);
            breakps = bp;
        }
    }

    private int startPageNum = 0;
    private int currentPageNum = 0;
    private String pageNumberString;
    private boolean isFirstPage = true;

    /** Current page being worked on. */
    private PageViewport curPage;

    /** Current span being filled */
    private Span curSpan;

    /** Current normal-flow-reference-area being filled. */
    private NormalFlow curFlow;

    private int flowBPD = 0;
    private int flowIPD = 0;

    /** 
     * AreaTreeHandler which activates this PSLM.
     */
    private AreaTreeHandler areaTreeHandler;

    /** 
     * AreaTreeModel that this PSLM sends pages to.
     */
    private AreaTreeModel areaTreeModel;

    /**
     * This is the SimplePageMaster that should be used to create the page. It
     * will be equal to the PageSequence's simplePageMaster, if it exists, or
     * to the correct member of the PageSequence's pageSequenceMaster, if that
     * is in effect instead.
     */
    private SimplePageMaster currentSimplePageMaster;

    /**
     * Constructor - activated by AreaTreeHandler for each
     * fo:page-sequence in the input FO stream
     *
     * @param pageseq the page-sequence formatting object
     */
    public PageSequenceLayoutManager(PageSequence pageSeq) {
        super(pageSeq);
        this.pageSeq = pageSeq;
    }

    /**
     * Set the AreaTreeHandler
     * @param areaTreeHandler the area tree handler object
     */
    public void setAreaTreeHandler(AreaTreeHandler areaTreeHandler) {
        this.areaTreeHandler = areaTreeHandler;
        areaTreeModel = areaTreeHandler.getAreaTreeModel();
    }

    /**
     * @see org.apache.fop.layoutmgr.LayoutManager
     * @return the AreaTreeHandler object
     */
    public AreaTreeHandler getAreaTreeHandler() {
        return areaTreeHandler;
    }

    /**
     * Start the layout of this page sequence.
     * This completes the layout of the page sequence
     * which creates and adds all the pages to the area tree.
     */
    public void activateLayout() {
        startPageNum = pageSeq.getStartingPageNumber();
        currentPageNum = startPageNum;
        pageNumberString = pageSeq.makeFormattedPageNumber(currentPageNum);

        LineArea title = null;

        if (pageSeq.getTitleFO() != null) {
            ContentLayoutManager clm = 
                new ContentLayoutManager(pageSeq.getTitleFO(), this);
            title = (LineArea) clm.getParentArea(null); // can improve
        }

        areaTreeModel.startPageSequence(title);
        log.debug("Starting layout");

        makeNewPage(false, false);
        flowIPD = curFlow.getIPD();

        BreakPoss bp;
        LayoutContext childLC = new LayoutContext(0);
        while (!isFinished()) {
            if ((bp = getNextBreakPoss(childLC)) != null) {
                addAreas((BlockBreakPosition)bp.getPosition());
                // add static areas and resolve any new id areas
                // finish page and add to area tree
                finishPage();
                currentPageNum++;
                pageNumberString = pageSeq.makeFormattedPageNumber(currentPageNum);
            }
        }
        // TODO: Don't decrement currentPageNum when no pages are generated
        currentPageNum--;
        finishPage();
        pageSeq.getRoot().notifyPageSequenceFinished(currentPageNum, (currentPageNum - startPageNum) + 1);
        log.debug("Ending layout");
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
    public BreakPoss getNextBreakPoss(LayoutContext context) {

        LayoutManager curLM; // currently active LM

        while ((curLM = getChildLM()) != null) {
            BreakPoss bp = null;

            LayoutContext childLC = new LayoutContext(0);
            childLC.setStackLimit(new MinOptMax(flowBPD));
            childLC.setRefIPD(flowIPD);

            if (!curLM.isFinished()) {
                pageSeq.setLayoutDimension(PercentBase.REFERENCE_AREA_IPD, flowIPD);
                pageSeq.setLayoutDimension(PercentBase.REFERENCE_AREA_BPD, flowBPD);
                bp = curLM.getNextBreakPoss(childLC);
            }
            if (bp != null) {
                return new BreakPoss(
                         new BlockBreakPosition(curLM, bp));
            }
        }
        setFinished(true);
        return null;
    }

    /**
     * Get the current page number string.
     * This returns the formatted string for the current page.
     *
     * @return the formatted page number string
     */
    public String getCurrentPageNumberString() {
        return pageNumberString;
    }
    
    /**
     * Provides access to the current page.
     * @return the current PageViewport
     */
    public PageViewport getCurrentPageViewport() {
        return this.curPage;
    }

    /**
     * Resolve a reference ID.
     * This resolves a reference ID and returns the first PageViewport
     * that contains the reference ID or null if reference not found.
     *
     * @param id the reference ID to lookup
     * @return the first page viewport that contains the reference
     */
    public PageViewport resolveRefID(String id) {
        List list = areaTreeHandler.getPageViewportsContainingID(id);
        if (list != null && list.size() > 0) {
            return (PageViewport) list.get(0);
        }
        return null;
    }

    /**
     * Add the areas to the current page.
     * Given the page break position this adds the areas to the current
     * page.
     *
     * @param bbp the block break position
     */
    public void addAreas(BlockBreakPosition bbp) {
        List list = new java.util.ArrayList();
        list.add(bbp.breakps);
        bbp.getLM().addAreas(new BreakPossPosIter(list, 0,
                              1), null);
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
        areaTreeHandler.associateIDWithPageViewport(id, curPage);
    }

    /**
     * Add an unresolved area to the layout manager.
     * The Page layout manager handles the unresolved ID
     * reference by adding to the current page and then adding
     * the page as a resolvable to the area tree.
     * This is so that the area tree can resolve the reference
     * and the page can serialize the resolvers if required.
     *
     * @param id the ID reference to add
     * @param res the resolvable object that needs resolving
     */
    public void addUnresolvedArea(String id, Resolvable res) {
        // add to the page viewport so it can serialize
        curPage.addUnresolvedIDRef(id, res);
        // add unresolved to tree
        areaTreeHandler.addUnresolvedIDRef(id, curPage);
    }

    /**
     * Add the marker to the page layout manager.
     *
     * @see org.apache.fop.layoutmgr.LayoutManager
     */
    public void addMarkerMap(Map marks, boolean starting, boolean isfirst, boolean islast) {
        //getLogger().debug("adding markers: " + marks + ":" + start);
        // add markers to page on area tree
        curPage.addMarkers(marks, starting, isfirst, islast);
    }

    /**
     * Retrieve a marker from this layout manager.
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
     * @param name the marker class name to lookup
     * @param pos the position to locate the marker
     * @param boundary the boundary for locating the marker
     * @return the layout manager for the marker contents
     */
    public Marker retrieveMarker(String name, int pos, int boundary) {
        // get marker from the current markers on area tree
        Marker mark = (Marker)curPage.getMarker(name, pos);
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
                    return mark;
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
        }

        return mark;
    }

    /**
     * For now, only handle normal flow areas.
     *
     * @param childArea the child area to add
     */
    public void addChild(Area childArea) {
        if (childArea == null) {
            return;
        }
        if (childArea.getAreaClass() == Area.CLASS_NORMAL) {
            placeNormalFlowRefArea(childArea);
        } else {
             // todo: all the others!
        }
    }

    /**
     * Place a normal-flow-reference-area into the current span. The FlowLM is
     * responsible for making sure that it will actually fit in the
     * current span area. In fact the area has already been added to the
     * current span, so we are just checking to see if the span is "full",
     * possibly moving to the next column or to the next page.
     *
     * @param area the area to place
     */
    protected void placeNormalFlowRefArea(Area area) {
        // assert (curSpan != null);
        // assert (area == curFlow);
        // assert (curFlow == curSpan.getFlow(curSpan.getColumnCount()-1));
        // assert (area.getBPD().min < curSpan.getHeight());
        // Last column on this page is filled
        // See if the flow is full. The Flow LM can add an area before
        // it's full in the case of a break or a span.
        // Also in the case of a float to be placed. In that case, there
        // may be further material added later.
        // The Flow LM sets the "finished" flag on the Flow Area if it has
        // completely filled it. In this case, if on the last column
        // end the page.
        getParentArea(area);
        // Alternatively the child LM indicates to parent that it's full?
        //getLogger().debug("size: " + area.getAllocationBPD().max +
        //                   ":" + curSpan.getMaxBPD().min);
        /*if (area.getAllocationBPD().max >= curSpan.getMaxBPD().min) {
            // Consider it filled
            if (curSpan.getColumnCount() == curSpan.getNormalFlowCount()) {
                finishPage();
            } else
                curFlow = null; // Create new flow on next getParentArea()
        }*/
    }

    protected void placeAbsoluteArea(Area area) {
    }


    protected void placeBeforeFloat(Area area) {
    }

    protected void placeSideFloat(Area area) {
    }

    protected void placeFootnote(Area area) {
        // After doing this, reduce available space on the curSpan.
        // This has to be propagated to the curFlow (FlowLM) so that
        // it can adjust its limit for composition (or it just asks
        // curSpan for BPD before doing the break?)
        // If multi-column, we may have to balance to find more space
        // for a float. When?
    }

    private PageViewport makeNewPage(boolean bIsBlank, boolean bIsLast) {
        finishPage();

        try {
            // create a new page
            currentSimplePageMaster = pageSeq.getSimplePageMasterToUse(
                currentPageNum, isFirstPage, bIsBlank);
            Region body = currentSimplePageMaster.getRegion(FO_REGION_BODY);
            if (!pageSeq.getMainFlow().getFlowName().equals(body.getRegionName())) {
              throw new FOPException("Flow '" + pageSeq.getMainFlow().getFlowName()
                 + "' does not map to the region-body in page-master '"
                 + currentSimplePageMaster.getMasterName() + "'");
            }
            curPage = createPageAreas(currentSimplePageMaster);
            isFirstPage = false;
        } catch (FOPException fopex) {
            throw new IllegalArgumentException("Cannot create page: " + fopex.getMessage());
        }

        curPage.setPageNumberString(pageNumberString);
        if (log.isDebugEnabled()) {
            log.debug("[" + curPage.getPageNumberString() + "]");
        }

        flowBPD = (int) curPage.getBodyRegion().getBPD();
        createSpan(1); // todo determine actual # of NormalFlows needed
        return curPage;
    }

    private void createSpan(int numCols) {
        // check number of columns (= all in Body or 1)
        // If already have a span, get its size and position (as MinMaxOpt)
        // This determines the position of the new span area
        // Attention: space calculation between the span areas.

        //MinOptMax newpos ;
        //if (curSpan != null) {
        //newpos = curSpan.getPosition(BPD);
        //newpos.add(curSpan.getDimension(BPD));
        //}
        //else newpos = new MinOptMax();
        curSpan = new Span(numCols);
        // get Width or Height as IPD for span

        RegionViewport rv = curPage.getPage().getRegionViewport(FO_REGION_BODY);
        int ipdWidth = (int) rv.getRegion().getIPD() -
            rv.getBorderAndPaddingWidthStart() - rv.getBorderAndPaddingWidthEnd();

        curSpan.setIPD(ipdWidth);
        //curSpan.setPosition(BPD, newpos);
        curPage.getBodyRegion().getMainReference().addSpan(curSpan);
        curFlow = curSpan.addNewNormalFlow();
    }

    private void layoutStaticContent(int regionID) {
        Region reg = currentSimplePageMaster.getRegion(regionID);
        if (reg == null) {
            return;
        }
        StaticContent sc = pageSeq.getStaticContent(reg.getRegionName());
        if (sc == null) {
            return;
        }
        
        RegionViewport rv = curPage.getPage().getRegionViewport(regionID);
        StaticContentLayoutManager lm;
        try {
            lm = (StaticContentLayoutManager)
                areaTreeHandler.getLayoutManagerMaker().makeLayoutManager(sc);
        } catch (FOPException e) {
            log.error
                ("Failed to create a StaticContentLayoutManager for flow "
                 + sc.getFlowName()
                 + "; no static content will be laid out:");
            log.error(e.getMessage());
            return;
        }
        lm.initialize();
        lm.setRegionReference(rv.getRegion());
        lm.setParent(this);
        LayoutContext childLC = new LayoutContext(0);
        childLC.setStackLimit(new MinOptMax((int)curPage.getViewArea().getHeight()));
        childLC.setRefIPD(rv.getRegion().getIPD());
        while (!lm.isFinished()) {
            BreakPoss bp = lm.getNextBreakPoss(childLC);
            if (bp != null) {
                List vecBreakPoss = new java.util.ArrayList();
                vecBreakPoss.add(bp);
                lm.addAreas(new BreakPossPosIter(vecBreakPoss, 0,
                                                 vecBreakPoss.size()), null);
            } else {
                log.error("bp==null  cls=" + reg.getRegionName());
            }
        }
        //lm.flush();
        lm.reset(null);
    }

    private void finishPage() {
        if (curPage == null) {
            curSpan = null;
            curFlow = null;
            return;
        }
        // Layout static content into the regions
        layoutStaticContent(FO_REGION_BEFORE); 
        layoutStaticContent(FO_REGION_AFTER);
        layoutStaticContent(FO_REGION_START);
        layoutStaticContent(FO_REGION_END);
        // Queue for ID resolution and rendering
        areaTreeModel.addPage(curPage);
        curPage = null;
        curSpan = null;
        curFlow = null;
    }

    /**
     * This is called from FlowLayoutManager when it needs to start
     * a new flow container (while generating areas).
     *
     * @param childArea The area for which a container is needed. It must be
     * some kind of block-level area. It must have area-class, break-before
     * and span properties set.
     * @return the parent area
     */
    public Area getParentArea(Area childArea) {
        int aclass = childArea.getAreaClass();
        if (aclass == Area.CLASS_NORMAL) {
            // todo: how to get properties from the Area???
            // Need span, break
            int breakVal = Constants.EN_AUTO;
            Integer breakBefore = (Integer)childArea.getTrait(Trait.BREAK_BEFORE);
            if (breakBefore != null) {
                breakVal = breakBefore.intValue();
            }
            if (breakVal != Constants.EN_AUTO) {
                // We may be forced to make new page
                handleBreak(breakVal);
            } else if (curPage == null) {
                log.debug("curPage is null. Making new page");
                makeNewPage(false, false);
            }
            // Now we should be on the right kind of page
            boolean bNeedSpan = false;
            int span = Constants.EN_NONE; // childArea.getSpan()
            int numCols = 1;
            if (span == Constants.EN_ALL) {
                // Assume the number of columns is stored on the curBody object.
                //numCols = curPage.getBodyRegion().getProperty(NUMBER_OF_COLUMNS);
            }
            if (curSpan == null) {
                bNeedSpan = true;
            } else if (numCols != curSpan.getNormalFlowCount()) {
                // todo: BALANCE EXISTING COLUMNS
                if (curSpan.getNormalFlowCount() > 1) {
                    // balanceColumns();
                }
                bNeedSpan = true;
            }
            if (bNeedSpan) {
                // Make a new span and the first flow
                createSpan(numCols);
            } else if (curFlow == null) {
                curFlow = curSpan.addNewNormalFlow();
            }
            return curFlow;
        } else {
            if (curPage == null) {
                makeNewPage(false, false);
            }
            // Now handle different kinds of areas
            if (aclass == Area.CLASS_BEFORE_FLOAT) {
                BeforeFloat bf = curPage.getBodyRegion().getBeforeFloat();
                if (bf == null) {
                    bf = new BeforeFloat();
                    curPage.getBodyRegion().setBeforeFloat(bf);
                }
                return bf;
            } else if (aclass == Area.CLASS_FOOTNOTE) {
                Footnote fn = curPage.getBodyRegion().getFootnote();
                if (fn == null) {
                    fn = new Footnote();
                    curPage.getBodyRegion().setFootnote(fn);
                }
                return fn;
            }
            // todo!!! other area classes (side-float, absolute, fixed)
            return null;
        }
    }

    /**
     * Depending on the kind of break condition, make new column
     * or page. May need to make an empty page if next page would
     * not have the desired "handedness".
     *
     * @param breakVal the break value to handle
     */
    private void handleBreak(int breakVal) {
        if (breakVal == Constants.EN_COLUMN) {
            if (curSpan != null // TODO: change below to < or <=
                    && curSpan.getNormalFlowCount() != curSpan.getColumnCount()) {
                // Move to next column
                curFlow = curSpan.addNewNormalFlow();
                return;
            }
            // else need new page
            breakVal = Constants.EN_PAGE;
        }
        if (needEmptyPage(breakVal)) {
            curPage = makeNewPage(true, false);
        }
        if (needNewPage(breakVal)) {
            curPage = makeNewPage(false, false);
        }
    }

    /**
     * If we have already started to layout content on a page,
     * and there is a forced break, see if we need to generate
     * an empty page.
     * Note that if not all content is placed, we aren't sure whether
     * it will flow onto another page or not, so we'd probably better
     * block until the queue of layoutable stuff is empty!
     */
    private boolean needEmptyPage(int breakValue) {

        if (breakValue == Constants.EN_PAGE || curPage.getPage().isEmpty()) {
            // any page is OK or we already have an empty page
            return false;
        }
        else {
            /* IF we are on the kind of page we need, we'll need a new page. */
            if (currentPageNum%2 != 0) {
                // Current page is odd
                return (breakValue == Constants.EN_ODD_PAGE);
            }
            else {
                return (breakValue == Constants.EN_EVEN_PAGE);
            }
        }
    }

    /**
     * See if need to generate a new page for a forced break condition.
     */
    private boolean needNewPage(int breakValue) {
        if (curPage != null && curPage.getPage().isEmpty()) {
            if (breakValue == Constants.EN_PAGE) {
                return false;
            }
            else if (currentPageNum%2 != 0) {
                // Current page is odd
                return (breakValue == Constants.EN_EVEN_PAGE);
            }
            else {
                return (breakValue == Constants.EN_ODD_PAGE);
            }
        }
        else {
            return true;
        }
    }

    private PageViewport createPageAreas(SimplePageMaster spm) {
        int pageWidth = spm.getPageWidth().getValue();
        int pageHeight = spm.getPageHeight().getValue();

        // Set the page dimension as the toplevel containing block for margin.
        ((FObj) pageSeq.getParent()).setLayoutDimension(PercentBase.BLOCK_IPD, pageWidth);
        ((FObj) pageSeq.getParent()).setLayoutDimension(PercentBase.BLOCK_BPD, pageHeight);

        // Get absolute margin properties (top, left, bottom, right)
        CommonMarginBlock mProps = spm.getCommonMarginBlock();

      /* Create the page reference area rectangle (0,0 is at top left
       * of the "page media" and y increases
       * when moving towards the bottom of the page.
       * The media rectangle itself is (0,0,pageWidth,pageHeight).
       */
       Rectangle pageRefRect =
               new Rectangle(mProps.marginLeft.getValue(), mProps.marginTop.getValue(),
                       pageWidth - mProps.marginLeft.getValue() - mProps.marginRight.getValue(),
                       pageHeight - mProps.marginTop.getValue() - mProps.marginBottom.getValue());

       Page page = new Page();  // page reference area

       // Set up the CTM on the page reference area based on writing-mode
       // and reference-orientation
       FODimension reldims = new FODimension(0, 0);
       CTM pageCTM = CTM.getCTMandRelDims(spm.getReferenceOrientation(),
               spm.getWritingMode(), pageRefRect, reldims);

       // Create a RegionViewport/ reference area pair for each page region
       RegionReference rr = null;
       for (Iterator regenum = spm.getRegions().values().iterator();
            regenum.hasNext();) {
           Region r = (Region)regenum.next();
           RegionViewport rvp = makeRegionViewport(r, reldims, pageCTM);
           r.setLayoutDimension(PercentBase.BLOCK_IPD, rvp.getIPD());
           r.setLayoutDimension(PercentBase.BLOCK_BPD, rvp.getBPD());
           if (r.getNameId() == FO_REGION_BODY) {
               rr = new BodyRegion((RegionBody) r);
           } else {
               rr = new RegionReference(r.getNameId());
           }
           setRegionPosition(r, rr, rvp.getViewArea());
           rvp.setRegion(rr);
           page.setRegionViewport(r.getNameId(), rvp);
       }

       return new PageViewport(page, new Rectangle(0, 0, pageWidth, pageHeight));
    }  
    
    /**
     * Creates a RegionViewport Area object for this pagination Region.
     * @param reldims relative dimensions
     * @param pageCTM page coordinate transformation matrix
     * @return the new region viewport
     */
    private RegionViewport makeRegionViewport(Region r, FODimension reldims, CTM pageCTM) {
        Rectangle2D relRegionRect = r.getViewportRectangle(reldims);
        Rectangle2D absRegionRect = pageCTM.transform(relRegionRect);
        // Get the region viewport rectangle in absolute coords by
        // transforming it using the page CTM
        RegionViewport rv = new RegionViewport(absRegionRect);
        rv.setBPD((int)relRegionRect.getHeight());
        rv.setIPD((int)relRegionRect.getWidth());
        TraitSetter.addBackground(rv, r.getCommonBorderPaddingBackground());
        return rv;
    }
   
    /**
     * Set the region position inside the region viewport.
     * This sets the trasnform that is used to place the contents of
     * the region.
     *
     * @param r the region reference area
     * @param absRegVPRect The region viewport rectangle in "absolute" coordinates
     * where x=distance from left, y=distance from bottom, width=right-left
     * height=top-bottom
     */
    private void setRegionPosition(Region r, RegionReference rr,
                                  Rectangle2D absRegVPRect) {
        FODimension reldims = new FODimension(0, 0);
        rr.setCTM(CTM.getCTMandRelDims(r.getReferenceOrientation(),
                r.getWritingMode(), absRegVPRect, reldims));
        rr.setIPD(reldims.ipd);
        rr.setBPD(reldims.bpd);
    }
}
