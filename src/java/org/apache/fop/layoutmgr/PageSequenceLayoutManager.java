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
import org.apache.fop.area.Flow;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.Page;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.RegionReference;
import org.apache.fop.area.BodyRegion;
import org.apache.fop.area.MainReference;
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
import org.apache.fop.fo.pagination.PageNumberGenerator;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.Region;
import org.apache.fop.fo.pagination.RegionBody;
import org.apache.fop.fo.pagination.SimplePageMaster;
import org.apache.fop.fo.pagination.StaticContent;
import org.apache.fop.fo.pagination.Title;
import org.apache.fop.fo.properties.CommonMarginBlock;

import java.util.List;
import java.util.Map;
import java.awt.Rectangle;
import java.util.Iterator;
import java.awt.geom.Rectangle2D;
import org.apache.fop.traits.MinOptMax;

/**
 * LayoutManager for a PageSequence and its flow.
 * It manages all page-related layout.
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

    private PageNumberGenerator pageNumberGenerator;
    private int pageCount = 1;
    private String pageNumberString;
    private boolean isFirstPage = true;

    /** True if haven't yet laid out any pages.*/
    private boolean bFirstPage;
    /** Current page being worked on. */
    private PageViewport curPage;

    /** Body region of the current page */
    private BodyRegion curBody;

    /** Current span being filled */
    private Span curSpan;

    /** Number of columns in current span area. */
    private int curSpanColumns;

    /** Current flow-reference-area (column) being filled. */
    private Flow curFlow;

    private int flowBPD = 0;
    private int flowIPD = 0;

    /** Manager which handles a queue of all pages which are completely
     * laid out and ready for rendering, except for resolution of ID
     * references?
     */
    private AreaTreeHandler areaTreeHandler;
    private AreaTreeModel areaTreeModel;

    /**
     * This is the SimplePageMaster that should be used to create the page. It
     * will be equal to the PageSequence's simplePageMaster, if it exists, or
     * to the correct member of the PageSequence's pageSequenceMaster, if that
     * exists instead.
     */
    private SimplePageMaster currentSimplePageMaster;

    /**
     * The collection of StaticContentLayoutManager objects that are associated
     * with this Page Sequence, keyed by flow-name.
     */
    //private HashMap staticContentLMs = new HashMap(4);

    /**
     * This is the top level layout manager.
     * It is created by the PageSequence FO.
     *
     * @param pageseq the page sequence fo
     */
    public PageSequenceLayoutManager(PageSequence pageSeq) {
        super(pageSeq);
        this.pageSeq = pageSeq;
        if (pageSeq.getPageSequenceMaster() != null) {
            pageSeq.getPageSequenceMaster().reset();
        }
    }

    /**
     * Set the AreaTreeHandler
     * @param areaTreeHandler the area tree handler to add pages to
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
     * Get the page count.
     * Used to get the last page number for reference for
     * the next page sequence.
     *
     * @return the page number
     */
    public int getPageCount() {
        return pageCount;
    }

    /**
     * @return the Title area
     */
    private LineArea getTitleArea(Title foTitle) {
        // get breaks then add areas to title
        LineArea title = new LineArea();

        ContentLayoutManager clm = new ContentLayoutManager(title);
        clm.setUserAgent(foTitle.getUserAgent());
        clm.setAreaTreeHandler(areaTreeHandler);

        // use special layout manager to add the inline areas
        // to the Title.
        InlineLayoutManager lm;
        lm = new InlineLayoutManager(foTitle);
        clm.addChildLM(lm);

        clm.fillArea(lm);

        return title;
    }

    /**
     * Start the layout of this page sequence.
     * This completes the layout of the page sequence
     * which creates and adds all the pages to the area tree.
     */
    public void activateLayout() {
        pageSeq.initPageNumber();
        pageCount = pageSeq.getCurrentPageNumber();
        pageNumberGenerator = pageSeq.getPageNumberGenerator();
        pageNumberString = pageNumberGenerator.makeFormattedPageNumber(pageCount);

        LineArea title = null;
        if (pageSeq.getTitleFO() != null) {
            title = getTitleArea(pageSeq.getTitleFO());
        }

        areaTreeModel.startPageSequence(title);
        log.debug("Starting layout");

        // this should be done another way
        makeNewPage(false, false);
        createBodyMainReferenceArea();
        createSpan(1);
        flowIPD = curFlow.getIPD();

        BreakPoss bp;
        LayoutContext childLC = new LayoutContext(0);
        while (!isFinished()) {
            if ((bp = getNextBreakPoss(childLC)) != null) {
                addAreas((BlockBreakPosition)bp.getPosition());
                // add static areas and resolve any new id areas

                // finish page and add to area tree
                finishPage();
                pageCount++;
                pageNumberString = pageNumberGenerator.makeFormattedPageNumber(pageCount);
            }
        }
        pageCount--;
        log.debug("Ending layout");
        flush();
        pageSeq.setCurrentPageNumber(getPageCount());
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
    public String getCurrentPageNumber() {
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
        // add unresolved to tree
        // adds to the page viewport so it can serialize
        curPage.addUnresolvedIDRef(id, res);
        areaTreeHandler.addUnresolvedIDRef(id, curPage);
    }

    /**
     * Add the marker to the page layout manager.
     *
     * @param name the marker class name
     * @param lm the layout manager for the marker contents
     * @param start true if starting marker area, false for ending
     */
    public void addMarkerMap(Map marks, boolean start, boolean isfirst) {
        //getLogger().debug("adding markers: " + marks + ":" + start);
        // add markers to page on area tree
        curPage.addMarkers(marks, start, isfirst);
    }

    /**
     * Retrieve a marker from this layout manager.
     * If the boundary is page then it will only check the
     * current page. For page-sequence and document it will
     * lookup preceding pages from the area tree and try to find
     * a marker.
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
                mark = (Marker)pv.getMarker(name, pos);
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
            placeFlowRefArea(childArea);
        } else {
             // todo: all the others!
        }
    }

    /**
     * Place a FlowReferenceArea into the current span. The FlowLM is
     * responsible for making sure that it will actually fit in the
     * current span area. In fact the area has already been added to the
     * current span, so we are just checking to see if the span is "full",
     * possibly moving to the next column or to the next page.
     *
     * @param area the area to place
     */
    protected void placeFlowRefArea(Area area) {
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
            if (curSpan.getColumnCount() == curSpanColumns) {
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
            curPage = createPage(bIsBlank, bIsLast);
            isFirstPage = false;
        } catch (FOPException fopex) {
            //TODO this exception is fatal, isn't it?
            log.error("Cannot create page", fopex);
        }

        curPage.setPageNumber(getCurrentPageNumber());
        if (log.isDebugEnabled()) {
            log.debug("[" + curPage.getPageNumber() + "]");
        }
        RegionViewport rv = curPage.getPage().getRegionViewport(
                    FO_REGION_BODY);
        curBody = (BodyRegion) rv.getRegion();
        flowBPD = (int) curBody.getBPD() -
            rv.getBorderAndPaddingWidthBefore() - rv.getBorderAndPaddingWidthAfter();

        return curPage;
    }

    private void layoutStaticContent(Region region) {
        if (region == null) {
            return;
        }
        StaticContent flow = pageSeq.getStaticContent(region.getRegionName());
        if (flow == null) {
            return;
        }
        
        RegionViewport reg = curPage.getPage().getRegionViewport(region.getNameId());
        StaticContentLayoutManager lm;
        try {
            lm = getStaticContentLayoutManager(flow);
        } catch (FOPException e) {
            log.error
                ("Failed to create a StaticContentLayoutManager for flow "
                 + flow.getFlowName()
                 + "; no static content will be laid out:");
            log.error(e.getMessage());
            return;
        }
        lm.initialize();
        lm.setRegionReference(reg.getRegion());
        lm.setParent(this);
        LayoutContext childLC = new LayoutContext(0);
        childLC.setStackLimit(new MinOptMax((int)curPage.getViewArea().getHeight()));
        childLC.setRefIPD(reg.getRegion().getIPD());
        while (!lm.isFinished()) {
            BreakPoss bp = lm.getNextBreakPoss(childLC);
            if (bp != null) {
                List vecBreakPoss = new java.util.ArrayList();
                vecBreakPoss.add(bp);
                lm.addAreas(new BreakPossPosIter(vecBreakPoss, 0,
                                                 vecBreakPoss.size()), null);
            } else {
                log.error("bp==null  cls=" + region.getRegionName());
            }
        }
        //lm.flush();
        lm.reset(null);
    }

    private void finishPage() {
        if (curPage == null) {
            return;
        }
        // Layout static content into the regions
        // Need help from pageseq for this
        layoutStaticContent(currentSimplePageMaster.getRegion(FO_REGION_BEFORE));
        layoutStaticContent(currentSimplePageMaster.getRegion(FO_REGION_AFTER));
        layoutStaticContent(currentSimplePageMaster.getRegion(FO_REGION_START));
        layoutStaticContent(currentSimplePageMaster.getRegion(FO_REGION_END));
        // Queue for ID resolution and rendering
        areaTreeModel.addPage(curPage);
        curPage = null;
        curBody = null;
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
                //numCols = curBody.getProperty(NUMBER_OF_COLUMNS);
            }
            if (curSpan == null) {
                createBodyMainReferenceArea();
                bNeedSpan = true;
            } else if (numCols != curSpanColumns) {
                // todo: BALANCE EXISTING COLUMNS
                if (curSpanColumns > 1) {
                    // balanceColumns();
                }
                bNeedSpan = true;
            }
            if (bNeedSpan) {
                // Make a new span and the first flow
                createSpan(numCols);
            } else if (curFlow == null) {
                createFlow();
            }
            return curFlow;
        } else {
            if (curPage == null) {
                makeNewPage(false, false);
            }
            // Now handle different kinds of areas
            if (aclass == Area.CLASS_BEFORE_FLOAT) {
                BeforeFloat bf = curBody.getBeforeFloat();
                if (bf == null) {
                    bf = new BeforeFloat();
                    curBody.setBeforeFloat(bf);
                }
                return bf;
            } else if (aclass == Area.CLASS_FOOTNOTE) {
                Footnote fn = curBody.getFootnote();
                if (fn == null) {
                    fn = new Footnote();
                    curBody.setFootnote(fn);
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
            if (curSpan != null
                    && curSpan.getColumnCount() != curSpanColumns) {
                // Move to next column
                createFlow();
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
            if (pageCount%2 != 0) {
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
            else if (pageCount%2 != 0) {
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

    private void createBodyMainReferenceArea() {
        MainReference mainRef = new MainReference();
        mainRef.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
        curBody.setMainReference(mainRef);
    }

    private Flow createFlow() {
        curFlow = new Flow();
        curFlow.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
        curFlow.setIPD(curSpan.getIPD()); // adjust for columns
        //curFlow.setBPD(100000);
        // Set IPD and max BPD on the curFlow from curBody
        curSpan.addFlow(curFlow);
        return curFlow;
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
        curSpan.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
        curSpanColumns = numCols;
        // get Width or Height as IPD for span

        RegionViewport rv = curPage.getPage().getRegionViewport(FO_REGION_BODY);
        int ipdWidth = (int) rv.getRegion().getIPD() -
            rv.getBorderAndPaddingWidthStart() - rv.getBorderAndPaddingWidthEnd();

        curSpan.setIPD(ipdWidth);
        //curSpan.setPosition(BPD, newpos);
        curBody.getMainReference().addSpan(curSpan);
        createFlow();
    }

    // See finishPage...
    protected void flush() {
        finishPage();
    }

    /**
     * Called when a new page is needed.
     *
     * @param bIsBlank If true, use a master for a blank page.
     * @param bIsLast If true, use the master for the last page in the sequence.
     * @return the page viewport created for the page number
     * @throws FOPException if there is an error creating page
     */
    private PageViewport createPage(boolean bIsBlank, boolean bIsLast)
                                   throws FOPException {
        currentSimplePageMaster = getSimplePageMasterToUse(bIsBlank);
        Region body = currentSimplePageMaster.getRegion(FO_REGION_BODY);
        if (!pageSeq.getMainFlow().getFlowName().equals(body.getRegionName())) {
          throw new FOPException("Flow '" + pageSeq.getMainFlow().getFlowName()
                                 + "' does not map to the region-body in page-master '"
                                 + currentSimplePageMaster.getMasterName() + "'");
        }
        PageViewport p = createPageAreas(currentSimplePageMaster);
        return p;
        // The page will have a viewport/reference area pair defined
        // for each region in the master.
        // Set up the page itself
// SKIP ALL THIS FOR NOW!!!
//             //pageSequence.root.setRunningPageNumberCounter(pageSequence.currentPageNumber);

//             pageSequence.pageCount++;    // used for 'force-page-count' calculations

        // handle the 'force-page-count'
        //forcePage(areaTree, firstAvailPageNumber);
    }

    private SimplePageMaster getSimplePageMasterToUse(boolean bIsBlank)
            throws FOPException {
        if (pageSeq.getPageSequenceMaster() == null) {
            return pageSeq.getSimplePageMaster();
        }
        boolean isOddPage = ((pageCount % 2) == 1);
        return pageSeq.getPageSequenceMaster()
              .getNextSimplePageMaster(isOddPage, isFirstPage, bIsBlank);
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
       for (Iterator regenum = spm.getRegions().values().iterator();
            regenum.hasNext();) {
           Region r = (Region)regenum.next();
           RegionViewport rvp = makeRegionViewport(r, reldims, pageCTM);
           r.setLayoutDimension(PercentBase.BLOCK_IPD, rvp.getIPD());
           r.setLayoutDimension(PercentBase.BLOCK_BPD, rvp.getBPD());
           if (r.getNameId() == FO_REGION_BODY) {
               rvp.setRegion(makeRegionBodyReferenceArea((RegionBody) r, rvp.getViewArea()));
           } else {
               rvp.setRegion(makeRegionReferenceArea(r, rvp.getViewArea()));
           }
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
        rv.addTrait(Trait.IS_VIEWPORT_AREA, Boolean.TRUE);
        if (r.getCommonBorderPaddingBackground().getBPPaddingAndBorder(false) != 0
                || r.getCommonBorderPaddingBackground().getBPPaddingAndBorder(false) != 0) {
            log.error("Border and padding for a region must be '0'.");
            //See 6.4.13 in XSL 1.0
        }
        rv.setBPD((int)relRegionRect.getHeight());
        rv.setIPD((int)relRegionRect.getWidth());
        setRegionViewportTraits(r, rv);
        return rv;
    }

    /**
     * Set the region viewport traits.
     * The viewport has the border, background and
     * clipping overflow traits.
     *
     * @param r the region viewport
     */
    private void setRegionViewportTraits(Region r, RegionViewport rv) {
        // Common Border, Padding, and Background Properties
        TraitSetter.addBorders(rv, r.getCommonBorderPaddingBackground());
        TraitSetter.addBackground(rv, r.getCommonBorderPaddingBackground());
    }

    private RegionReference makeRegionBodyReferenceArea(RegionBody r,
            Rectangle2D absRegVPRect) {
        // Should set some column stuff here I think, or put it elsewhere
        BodyRegion body = new BodyRegion();
        body.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
        setRegionPosition(r, body, absRegVPRect);
        int columnCount = r.getColumnCount();
        if ((columnCount > 1) && (r.getOverflow() == EN_SCROLL)) {
            // recover by setting 'column-count' to 1. This is allowed but
            // not required by the spec.
            log.error("Setting 'column-count' to 1 because "
                    + "'overflow' is set to 'scroll'");
            columnCount = 1;
        }
        body.setColumnCount(columnCount);

        int columnGap = r.getColumnGap();
        body.setColumnGap(columnGap);
        return body;
    }

    /**
     * Create the region reference area for this region master.
     * @param absRegVPRect The region viewport rectangle is "absolute" coordinates
     * where x=distance from left, y=distance from bottom, width=right-left
     * height=top-bottom
     * @return a new region reference area
     */
    private RegionReference makeRegionReferenceArea(Region r,
            Rectangle2D absRegVPRect) {
        RegionReference rr = new RegionReference(r.getNameId());
        rr.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
        setRegionPosition(r, rr, absRegVPRect);
        return rr;
    }

    /**
     * Set the region position inside the region viewport.
     * This sets the trasnform that is used to place the contents of
     * the region.
     *
     * @param r the region reference area
     * @param absRegVPRect the rectangle to place the region contents
     */
    private void setRegionPosition(Region r, RegionReference rr,
                                  Rectangle2D absRegVPRect) {
        FODimension reldims = new FODimension(0, 0);
        rr.setCTM(CTM.getCTMandRelDims(r.getReferenceOrientation(),
                r.getWritingMode(), absRegVPRect, reldims));
        rr.setIPD(reldims.ipd);
        rr.setBPD(reldims.bpd);
    }

    /**
     * @return a StaticContent layout manager
     */
    private StaticContentLayoutManager getStaticContentLayoutManager(StaticContent sc)
        throws FOPException {
        StaticContentLayoutManager lm;
        //lm = (StaticContentLayoutManager) staticContentLMs.get(sc.getFlowName());
        //if (lm == null) {
            lm = (StaticContentLayoutManager)
                getAreaTreeHandler().getLayoutManagerMaker().makeLayoutManager(sc);
            //staticContentLMs.put(sc.getFlowName(), lm);
        //}
        return lm;
    }
}
