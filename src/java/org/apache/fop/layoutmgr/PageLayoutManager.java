/*
 * $Id: PageLayoutManager.java,v 1.33 2003/03/07 07:58:51 jeremias Exp $
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 *
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package org.apache.fop.layoutmgr;

import org.apache.fop.apps.FOPException;

import org.apache.fop.area.CTM;
import org.apache.fop.area.AreaTree;
import org.apache.fop.area.AreaTreeModel;
import org.apache.fop.area.Area;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Flow;
import org.apache.fop.area.Page;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.RegionReference;
import org.apache.fop.area.BodyRegion;
import org.apache.fop.area.MainReference;
import org.apache.fop.area.Span;
import org.apache.fop.area.BeforeFloat;
import org.apache.fop.area.Footnote;
import org.apache.fop.area.Resolveable;

import org.apache.fop.datatypes.FODimension;

import org.apache.fop.fo.flow.Marker;

import org.apache.fop.fo.pagination.PageNumberGenerator;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.Region;
import org.apache.fop.fo.properties.RetrieveBoundary;
import org.apache.fop.fo.pagination.SimplePageMaster;
import org.apache.fop.fo.pagination.StaticContent;

import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.awt.Rectangle;
import java.util.Iterator;

/**
 * LayoutManager for a PageSequence and its flow.
 * It manages all page-related layout.
 */
public class PageLayoutManager extends AbstractLayoutManager implements Runnable {

    private static class BlockBreakPosition extends LeafPosition {
        protected BreakPoss breakps;

        protected BlockBreakPosition(LayoutProcessor lm, BreakPoss bp) {
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
    private AreaTree areaTree;
    private PageSequence pageSequence;

    /**
     * This is the SimplePageMaster that should be used to create the page. It
     * will be equal to the PageSequence's simplePageMaster, if it exists, or
     * to the correct member of the PageSequence's pageSequenceMaster, if that
     * exists instead.
     */
    private SimplePageMaster currentSimplePageMaster;

    /**
     * This is the top level layout manager.
     * It is created by the PageSequence FO.
     *
     * @param areaTree the area tree to add pages to
     * @param pageseq the page sequence fo
     */
    public PageLayoutManager(AreaTree areaTree, PageSequence pageseq) {
        this.areaTree = areaTree;
        pageSequence = pageseq;
    }

    /**
     * Set the page counting for this page sequence.
     * This sets the initial page number and the page number formatter.
     *
     * @param pc the starting page number
     * @param generator the page number generator
     */
    public void setPageCounting(int pc, PageNumberGenerator generator) {
        pageCount = pc;
        pageNumberGenerator = generator;
        pageNumberString = pageNumberGenerator.makeFormattedPageNumber(pageCount);
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
     * The layout process is designed to be able to be run in a thread.
     * In theory it can run at the same
     * time as FO tree generation, once the layout-master-set has been read.
     * We can arrange it so that the iterator over the fobj children waits
     * until the next child is available.
     * As it produces pages, it adds them to the AreaTree, where the
     * rendering process can also run in a parallel thread.
     */
    public void run() {
        doLayout();
        flush();
    }

    /**
     * Do the layout of this page sequence.
     * This completes the layout of the page sequence
     * which creates and adds all the pages to the area tree.
     */
    protected void doLayout() {

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

        LayoutProcessor curLM ; // currently active LM

        while ((curLM = getChildLM()) != null) {
            BreakPoss bp = null;

            LayoutContext childLC = new LayoutContext(0);
            childLC.setStackLimit(new MinOptMax(flowBPD));
            childLC.setRefIPD(flowIPD);

            if (!curLM.isFinished()) {
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
     * Resolve a reference ID.
     * This resolves a reference ID and returns the first PageViewport
     * that contains the reference ID or null if reference not found.
     *
     * @param ref the reference ID to lookup
     * @return the first page viewport that contains the reference
     */
    public PageViewport resolveRefID(String ref) {
        List list = areaTree.getIDReferences(ref);
        if (list != null && list.size() > 0) {
            return (PageViewport)list.get(0);
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
        List list = new ArrayList();
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
        areaTree.addIDRef(id, curPage);
    }

    /**
     * Add an unresolved area to the layout manager.
     * The Page layout manager handles the unresolved ID
     * reference by adding to the current page and then adding
     * the page as a resolveable to the area tree.
     * This is so that the area tree can resolve the reference
     * and the page can serialize the resolvers if required.
     *
     * @param id the ID reference to add
     * @param res the resolveable object that needs resolving
     */
    public void addUnresolvedArea(String id, Resolveable res) {
        // add unresolved to tree
        // adds to the page viewport so it can serialize
        curPage.addUnresolvedID(id, res);
        areaTree.addUnresolvedID(id, curPage);
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
        if (mark == null && boundary != RetrieveBoundary.PAGE) {
            // go back over pages until mark found
            // if document boundary then keep going
            boolean doc = boundary == RetrieveBoundary.DOCUMENT;
            AreaTreeModel atm = areaTree.getAreaTreeModel();
            int seq = atm.getPageSequenceCount();
            int page = atm.getPageCount(seq) - 1;
            while (page >= 0) {
                PageViewport pv = atm.getPage(seq, page);
                mark = (Marker)curPage.getMarker(name, pos);
                if (mark != null) {
                    return mark;
                }
                page--;
                if (page == -1 && doc && seq > 0) {
                    seq--;
                    page = atm.getPageCount(seq) - 1;
                }
            }
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
            ; // todo: all the others!
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
        } catch (FOPException fopex) { /* ???? */
            fopex.printStackTrace();
        }

        curPage.setPageNumber(getCurrentPageNumber());
        RegionViewport reg = curPage.getPage().getRegion(
                    RegionReference.BODY);
        curBody = (BodyRegion) reg.getRegion();
        flowBPD = (int)reg.getViewArea().getHeight();
        return curPage;
    }

    private void layoutStaticContent(Region region, int regionClass) {
        if (region != null) {
            StaticContent flow = pageSequence
              .getStaticContent(region.getRegionName());
            if (flow != null) {
                RegionViewport reg = curPage.getPage()
                  .getRegion(regionClass);
                reg.getRegion().setIPD((int)reg.getViewArea().getWidth());
                if (reg == null) {
                    getLogger().error("no region viewport: shouldn't happen");
                }
                StaticContentLayoutManager lm = flow.getLayoutManager();
                lm.setUserAgent(getUserAgent());
                lm.init();
                lm.setRegionReference(reg.getRegion());
                lm.setParent(this);
                LayoutContext childLC = new LayoutContext(0);
                childLC.setStackLimit(new MinOptMax((int)curPage.getViewArea().getHeight()));
                childLC.setRefIPD((int)reg.getViewArea().getWidth());

                while (!lm.isFinished()) {
                    BreakPoss bp = lm.getNextBreakPoss(childLC);
                    if (bp != null) {
                        List vecBreakPoss = new ArrayList();
                        vecBreakPoss.add(bp);
                        lm.addAreas(new BreakPossPosIter(vecBreakPoss, 0,
                                                          vecBreakPoss.size()), null);
                    } else {
                      getLogger().error("bp==null  cls=" + regionClass);
                    }
                }
                //lm.flush();
                lm.reset(null);
            }
        }
    }

    private void finishPage() {
        if (curPage != null) {
            // Layout static content into the regions
            // Need help from pageseq for this
            layoutStaticContent(currentSimplePageMaster.getRegion(Region.BEFORE),
                                RegionReference.BEFORE);
            layoutStaticContent(currentSimplePageMaster.getRegion(Region.AFTER),
                                RegionReference.AFTER);
            layoutStaticContent(currentSimplePageMaster.getRegion(Region.START),
                                RegionReference.START);
            layoutStaticContent(currentSimplePageMaster.getRegion(Region.END),
                                RegionReference.END);
            // Queue for ID resolution and rendering
            areaTree.addPage(curPage);
            curPage = null;
            curBody = null;
            curSpan = null;
            curFlow = null;
        }
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
            int breakVal = Constants.AUTO; // childArea.getBreakBefore();
            if (breakVal != Constants.AUTO) {
                // We may be forced to make new page
                handleBreak(breakVal);
            } else if (curPage == null) {
                makeNewPage(false, false);
            }
            // Now we should be on the right kind of page
            boolean bNeedSpan = false;
            int span = Constants.NONE; // childArea.getSpan()
            int numCols = 1;
            if (span == Constants.ALL) {
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
    protected void handleBreak(int breakVal) {
        if (breakVal == Constants.COLUMN) {
            if (curSpan != null
                    && curSpan.getColumnCount() != curSpanColumns) {
                // Move to next column
                createFlow();
                return;
            }
            // else need new page
            breakVal = Constants.PAGE;
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
        return false;
        // if (breakValue == Constants.PAGE || curPage.isEmpty()) {
        //     // any page is OK or we already have an empty page
        //     return false;
        // }
        // else {
        //     /* IF we are on the kind of page we need, we'll need a new page. */
        //     if (curPage.getPageNumber()%2 != 0) {
        // // Current page is odd
        // return (breakValue == Constants.ODD_PAGE);
        //     }
        //     else {
        // return (breakValue == Constants.EVEN_PAGE);
        //     }
        // }
    }

    /**
     * See if need to generate a new page for a forced break condition.
     * todo: methods to see if the current page is empty and to get
     * its number.
     */
    private boolean needNewPage(int breakValue) {
        return false;
        //if (curPage.isEmpty()) {
        //if (breakValue == Constants.PAGE) {
        //return false;
        //}
        //else if (curPage.getPageNumber()%2 != 0) {
        //// Current page is odd
        //return (breakValue == Constants.EVEN_PAGE);
        //}
        //else {
        //return (breakValue == Constants.ODD_PAGE);
        //}
        //}
        //else {
        //    return true;
        //}
    }

    private void createBodyMainReferenceArea() {
        curBody.setMainReference(new MainReference());
    }

    private Flow createFlow() {
        curFlow = new Flow();
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
        // get Width or Height as IPD for span
        curSpan.setIPD((int) curPage.getPage().getRegion(
                          RegionReference.BODY).getViewArea().getWidth());

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
    public PageViewport createPage(boolean bIsBlank, boolean bIsLast)
                                   throws FOPException {
        currentSimplePageMaster = getSimplePageMasterToUse(bIsBlank);
        Region body = currentSimplePageMaster.getRegion(Region.BODY);
        if (!pageSequence.getMainFlow().getFlowName().equals(body.getRegionName())) {
          throw new FOPException("Flow '" + pageSequence.getMainFlow().getFlowName()
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
        if (pageSequence.getPageSequenceMaster() == null) {
            return pageSequence.getSimplePageMaster();
        }
        boolean isOddPage = ((pageCount % 2) == 1);
        return pageSequence.getPageSequenceMaster()
              .getNextSimplePageMaster(isOddPage, isFirstPage, bIsBlank);
    }

    public PageViewport createPageAreas(SimplePageMaster spm) {
        int pageWidth =
                spm.properties.get("page-width").getLength().getValue();
        int pageHeight =
                spm.properties.get("page-height").getLength().getValue();
        // Get absolute margin properties (top, left, bottom, right)
        CommonMarginBlock mProps = spm.getPropertyManager().getMarginProps();

      /* Create the page reference area rectangle (0,0 is at top left
       * of the "page media" and y increases
       * when moving towards the bottom of the page.
       * The media rectangle itself is (0,0,pageWidth,pageHeight).
       */
       Rectangle pageRefRect =
               new Rectangle(mProps.marginLeft, mProps.marginTop,
                       pageWidth - mProps.marginLeft - mProps.marginRight,
                       pageHeight - mProps.marginTop - mProps.marginBottom);

       Page page = new Page();  // page reference area

       // Set up the CTM on the page reference area based on writing-mode
       // and reference-orientation
       FODimension reldims = new FODimension(0, 0);
       CTM pageCTM = CTM.getCTMandRelDims(spm.getPropertyManager().getAbsRefOrient(),
               spm.getPropertyManager().getWritingMode(), pageRefRect, reldims);

       // Create a RegionViewport/ reference area pair for each page region

       boolean bHasBody = false;

       for (Iterator regenum = spm.getRegions().values().iterator();
            regenum.hasNext();) {
           Region r = (Region)regenum.next();
           RegionViewport rvp = r.makeRegionViewport(reldims, pageCTM);
           rvp.setRegion(r.makeRegionReferenceArea(rvp.getViewArea()));
           page.setRegion(r.getRegionAreaClass(), rvp);
           if (r.getRegionAreaClass() == RegionReference.BODY) {
               bHasBody = true;
           }
       }

       if (!bHasBody) {
           spm.getLogger().error("simple-page-master has no region-body");
       }

       return new PageViewport(page, new Rectangle(0, 0, pageWidth, pageHeight));

    }

}

