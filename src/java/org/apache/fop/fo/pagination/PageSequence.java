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

package org.apache.fop.fo.pagination;

// Java
import java.util.HashMap;

// XML
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

// FOP
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.FOElementMapping;
import org.apache.fop.fo.FOTreeVisitor;
import org.apache.fop.apps.FOPException;

/**
 * This provides pagination of flows onto pages. Much of the
 * logic for paginating flows is contained in this class.
 * The main entry point is the format method.
 */
public class PageSequence extends FObj {
    //
    // intial-page-number types
    //
    public static final int EXPLICIT = 0;
    public static final int AUTO = 1;
    public static final int AUTO_EVEN = 2;
    public static final int AUTO_ODD = 3;

    //
    // associations
    //
    /**
     * The parent root object
     */
    private Root root;

    /**
     * the set of layout masters (provided by the root object)
     */
    private LayoutMasterSet layoutMasterSet;

    // There doesn't seem to be anything in the spec requiring flows
    // to be in the order given, only that they map to the regions
    // defined in the page sequence, so all we need is this one hashmap
    // the set of flows includes StaticContent flows also

    /**
     * Map of flows to their flow name (flow-name, Flow)
     */
    public HashMap flowMap;

    // according to communication from Paul Grosso (XSL-List,
    // 001228, Number 406), confusion in spec section 6.4.5 about
    // multiplicity of fo:flow in XSL 1.0 is cleared up - one (1)
    // fo:flow per fo:page-sequence only.
//    private boolean isFlowSet = false;

    // for structure handler
    private boolean sequenceStarted = false;

    //
    // state attributes used during layout
    //

    // page number and related formatting variables
    private String ipnValue;
    public int currentPageNumber = 0;
    private int explicitFirstNumber = 0; // explicitly specified
    public int firstPageNumber = 0; // actual
    public PageNumberGenerator pageNumberGenerator;

    public int forcePageCount = 0;
    private int pageCount = 0;
    private boolean isForcing = false;

    /**
     * specifies page numbering type (auto|auto-even|auto-odd|explicit)
     */
    public int pageNumberType;

    /**
     * used to determine whether to calculate auto, auto-even, auto-odd
     */
    private boolean thisIsFirstPage;

    /**
     * The currentSimplePageMaster is either the page master for the
     * whole page sequence if master-reference refers to a simple-page-master,
     * or the simple page master produced by the page sequence master otherwise.
     * The pageSequenceMaster is null if master-reference refers to a
     * simple-page-master.
     */
    public SimplePageMaster simplePageMaster;
    public PageSequenceMaster pageSequenceMaster;

    /**
     * The main content flow for this page-sequence.
     */
    private Flow mainFlow = null;

    /**
     * The fo:title object for this page-sequence.
     */
    private Title titleFO;

    /**
     * Create a page sequence FO node.
     *
     * @param parent the parent FO node
     */
    public PageSequence(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
        XSL/FOP Content Model: (title?,static-content*,flow)
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) {
        if (nsURI == FOElementMapping.URI) {
            if (localName.equals("title")) {
                if (titleFO != null) {
                    tooManyNodesError(loc, "fo:title");
                } else if (flowMap.size() > 0) {
                    nodesOutOfOrderError(loc, "fo:title", "fo:static-content");
                } else if (mainFlow != null) {
                    nodesOutOfOrderError(loc, "fo:title", "fo:flow");
                }
            } else if (localName.equals("static-content")) {
                if (mainFlow != null) {
                    nodesOutOfOrderError(loc, "fo:static-content", "fo:flow");
                }                
            } else if (localName.equals("flow")) {
                if (mainFlow != null) {
                    tooManyNodesError(loc, "fo:flow");
                }
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        } else {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /**
     * Signal end of this xml element.
     * This passes the end page sequence to the structure handler
     * so it can act upon that.
     */
    protected void end() {
        if (mainFlow == null) {
           missingChildElementError("(title?,static-content*,flow)");
        }
        try {
            getFOInputHandler().endPageSequence(this);
        } catch (FOPException fopex) {
            getLogger().error("Error in PageSequence.end(): "
              + fopex.getMessage(), fopex);
        }
    }

    /**
     * Validate the child being added and initialize internal variables.
     * XSL content model for page-sequence:
     * <pre>(title?,static-content*,flow)</pre>
     *
     * @param child The flow object child to be added to the PageSequence.
     */
    public void addChild(FONode child) {
        try {
            String childName = child.getName();
            if (childName.equals("fo:title")) {
               this.titleFO = (Title)child;
            } else if (childName.equals("fo:flow")) {
                this.mainFlow = (Flow)child;
                String flowName = this.mainFlow.getFlowName();
                if (flowMap.containsKey(flowName)) {
                    throw new FOPException("flow-name "
                        + flowName
                        + " is not unique within an fo:page-sequence");
                }
                if (!this.layoutMasterSet.regionNameExists(flowName)) {
                    getLogger().error("region-name '"
                        + flowName
                        + "' doesn't exist in the layout-master-set.");
                }
                // Don't add main flow to the flow map
//              addFlow(mainFlow);
                startStructuredPageSequence();
                super.addChild(child); // For getChildren
            } else if (childName.equals("fo:static-content")) {
                String flowName = ((StaticContent)child).getFlowName();
                if (flowMap.containsKey(flowName)) {
                    throw new FOPException("flow-name " + flowName
                              + " is not unique within an fo:page-sequence");
                }
                if (!this.layoutMasterSet.regionNameExists(flowName)) {
                    throw new FOPException("region-name '" + flowName
                              + "' doesn't exist in the layout-master-set.");
                }
                flowMap.put(flowName, child);
//              addFlow((Flow)child);
                startStructuredPageSequence();
            } 
        } catch (FOPException fopex) {
            getLogger().error("Error in PageSequence.addChild(): "
                + fopex.getMessage(), fopex);
        }
    }


    /**
     * @see org.apache.fop.fo.FObj#addProperties
     */
    protected void addProperties(Attributes attlist) throws FOPException {
        super.addProperties(attlist);

        this.root = (Root) parent;
//      this.root.addPageSequence(this);
        layoutMasterSet = root.getLayoutMasterSet();
        
        // best time to run some checks on LayoutMasterSet
        layoutMasterSet.checkRegionNames();

        flowMap = new HashMap();

        // we are now on the first page of the page sequence
        thisIsFirstPage = true;
        ipnValue = this.propertyList.get(PR_INITIAL_PAGE_NUMBER).getString();

        if (ipnValue.equals("auto")) {
            pageNumberType = AUTO;
        } else if (ipnValue.equals("auto-even")) {
            pageNumberType = AUTO_EVEN;
        } else if (ipnValue.equals("auto-odd")) {
            pageNumberType = AUTO_ODD;
        } else {
            pageNumberType = EXPLICIT;
            try {
                int pageStart = new Integer(ipnValue).intValue();
                this.explicitFirstNumber = (pageStart > 0) ? pageStart : 1;
            } catch (NumberFormatException nfe) {
                throw new FOPException("\"" + ipnValue
                                       + "\" is not a valid value for initial-page-number");
            }
        }

        String masterName = this.propertyList.get(PR_MASTER_REFERENCE).getString();
        this.simplePageMaster =
                this.layoutMasterSet.getSimplePageMaster(masterName);
        if (this.simplePageMaster == null) {
            this.pageSequenceMaster =
                    this.layoutMasterSet.getPageSequenceMaster(masterName);
            if (this.pageSequenceMaster == null) {
                throw new FOPException("master-reference '" + masterName
                                       + "' for fo:page-sequence matches no"
                                       + " simple-page-master or page-sequence-master");
            }
        }

        // get the 'format' properties
        this.pageNumberGenerator =
            new PageNumberGenerator(this.propertyList.get(PR_FORMAT).getString(),
                                    this.propertyList.get(PR_GROUPING_SEPARATOR).getCharacter(),
                                    this.propertyList.get(PR_GROUPING_SIZE).getNumber().intValue(),
                                    this.propertyList.get(PR_LETTER_VALUE).getEnum());
        this.pageNumberGenerator.setLogger(getLogger());

        this.forcePageCount =
            this.propertyList.get(PR_FORCE_PAGE_COUNT).getEnum();

        // this.propertyList.get("country");
        // this.propertyList.get("language");
        setupID();

        //call startStructuredPageSequence to ensure, that startPageSequence is called
        //before startFlow.
        startStructuredPageSequence();
    }


    /**
     * Add a flow or static content, mapped by its flow-name.
     * The flow-name is used to associate the flow with a region on a page,
     * based on the names given to the regions in the page-master used to
     * generate that page.
     */
//      private void addFlow(Flow flow) throws FOPException {
//          if (flowMap.containsKey(flow.getFlowName())) {
//              throw new FOPException("flow-names must be unique within an fo:page-sequence");
//          }
//          if (!this.layoutMasterSet.regionNameExists(flow.getFlowName())) {
//              getLogger().error("region-name '"
//                                     + flow.getFlowName()
//                                     + "' doesn't exist in the layout-master-set.");
//          }
//          flowMap.put(flow.getFlowName(), flow);
//          //setIsFlowSet(true);
//      }


    /**
     * Start the page-sequence logic in the Structured Handler
     */
    private void startStructuredPageSequence() {
        if (!sequenceStarted) {
            getFOInputHandler().startPageSequence(this);
            sequenceStarted = true;
        }
    }


    /**
     * Initialize the current page number for the start of the page sequence.
     */
    public void initPageNumber() {
        this.currentPageNumber = this.root.getRunningPageNumberCounter() + 1;

        if (this.pageNumberType == AUTO_ODD) {
            // Next page but force odd. May force empty page creation!
            // Whose master is used for this??? Assume no.
            // Use force-page-count = auto
            // on preceding page-sequence to make sure that there is no gap!
            if (currentPageNumber % 2 == 0) {
                this.currentPageNumber++;
            }
        } else if (pageNumberType == AUTO_EVEN) {
            if (currentPageNumber % 2 == 1) {
                this.currentPageNumber++;
            }
        } else if (pageNumberType == EXPLICIT) {
            this.currentPageNumber = this.explicitFirstNumber;
        }
        this.firstPageNumber = this.currentPageNumber;
    }

    /**
     * Creates a new page area for the given parameters
     * @param areaTree the area tree the page should be contained in
     * @param firstAvailPageNumber the page number for this page
     * @param isFirstPage true when this is the first page in the sequence
     * @param isEmptyPage true if this page will be empty
     * (e.g. forced even or odd break)
     * @return a Page layout object based on the page master selected
     * from the params
     * @todo modify the other methods to use even/odd flag and bIsLast
     */
//      private PageViewport makePage(int firstAvailPageNumber,
//                boolean isFirstPage, boolean bIsLast,
//                boolean isEmptyPage) throws FOPException {
//          // layout this page sequence

//          // while there is still stuff in the flow, ask the
//          // layoutMasterSet for a new page

//          // page number is 0-indexed
//          PageMaster pageMaster = getNextPageMaster(masterName,
//                                  firstAvailPageNumber,
//                                  isFirstPage, isEmptyPage);

//          // a legal alternative is to use the last sub-sequence
//          // specification which should be handled in getNextSubsequence.
//      // That's not done here.
//          if (pageMaster == null) {
//              throw new FOPException("page masters exhausted. Cannot recover.");
//          }
//          PageViewport p = pageMaster.makePage();
//          return p;
//      }

    /**
     * Returns the next SubSequenceSpecifier for the given page sequence master.
     * The result is bassed on the current state of this page sequence.
     */
//      private SubSequenceSpecifier getNextSubsequence(PageSequenceMaster master) {
//          if (master.getSubSequenceSpecifierCount()
//                  > currentSubsequenceNumber + 1) {

//              currentSubsequence =
//                  master.getSubSequenceSpecifier(currentSubsequenceNumber + 1);
//              currentSubsequenceNumber++;
//              return currentSubsequence;
//          } else {
//              return null;
//          }
//      }

    /**
     * Returns the next simple page master for the given sequence master, page number and
     * other state information
     */
//      private SimplePageMaster getNextSimplePageMaster(PageSequenceMaster sequenceMaster,
//              int pageNumber, boolean thisIsFirstPage,
//              boolean isEmptyPage) {
//          // handle forcing
//          if (isForcing) {
//              String nextPageMaster = getNextPageMasterName(sequenceMaster,
//                                      pageNumber, false, true);
//              return this.layoutMasterSet.getSimplePageMaster(nextPageMaster);
//          }
//          String nextPageMaster = getNextPageMasterName(sequenceMaster,
//                                  pageNumber, thisIsFirstPage, isEmptyPage);
//          return this.layoutMasterSet.getSimplePageMaster(nextPageMaster);

//      }

    /**
     * Get the next page master name.
     * This gets the name of the next page master. If the sequence
     * is exhausted then an error is indicated and the last page
     * master name is used.
     */
//      private String getNextPageMasterName(PageSequenceMaster sequenceMaster,
//                                           int pageNumber,
//                                           boolean thisIsFirstPage,
//                                           boolean isEmptyPage) {

//          if (null == currentSubsequence) {
//              currentSubsequence = getNextSubsequence(sequenceMaster);
//          }

//          String nextPageMaster =
//              currentSubsequence.getNextPageMaster(pageNumber,
//                                                   thisIsFirstPage,
//                                                   isEmptyPage);


//          if (null == nextPageMaster
//                  || isFlowForMasterNameDone(currentPageMasterName)) {
//              SubSequenceSpecifier nextSubsequence =
//                  getNextSubsequence(sequenceMaster);
//              if (nextSubsequence == null) {
//                  getLogger().error("Page subsequences exhausted. Using previous subsequence.");
//                  thisIsFirstPage =
//                      true;    // this becomes the first page in the new (old really) page master
//                  currentSubsequence.reset();

//                  // we leave currentSubsequence alone
//              }
//              else {
//                  currentSubsequence = nextSubsequence;
//              }

//              nextPageMaster =
//                  currentSubsequence.getNextPageMaster(pageNumber,
//                                                       thisIsFirstPage,
//                                                       isEmptyPage);
//          }
//          currentPageMasterName = nextPageMaster;

//          return nextPageMaster;

//      }

//      private SimplePageMaster getCurrentSimplePageMaster() {
//          return this.layoutMasterSet.getSimplePageMaster(currentPageMasterName);
//      }

//      private String getCurrentPageMasterName() {
//          return currentPageMasterName;
//      }

    // refactored from LayoutMasterSet
//      private PageMaster getNextPageMaster(String pageSequenceName,
//                                           int pageNumber,
//                                           boolean thisIsFirstPage,
//                                           boolean isEmptyPage) throws FOPException {
//          PageMaster pageMaster = null;

//          // see if there is a page master sequence for this master name
//          PageSequenceMaster sequenceMaster =
//              this.layoutMasterSet.getPageSequenceMaster(pageSequenceName);

//          if (sequenceMaster != null) {
//              pageMaster = getNextSimplePageMaster(sequenceMaster,
//                                                   pageNumber,
//                                                   thisIsFirstPage,
//                                                   isEmptyPage).getPageMaster();

//          } else {    // otherwise see if there's a simple master by the given name
//              SimplePageMaster simpleMaster =
//                  this.layoutMasterSet.getSimplePageMaster(pageSequenceName);
//              if (simpleMaster == null) {
//                  throw new FOPException("'master-reference' for 'fo:page-sequence'"
//                                         + "matches no 'simple-page-master'"
//                                         + " or 'page-sequence-master'");
//              }
//              currentPageMasterName = pageSequenceName;

//              pageMaster = simpleMaster.getNextPageMaster();
//          }
//          return pageMaster;
//      }


//     /**
//      * Returns true when there is more flow elements left to lay out.
//      */
//     private boolean flowsAreIncomplete() {
//         boolean isIncomplete = false;

//         for (Iterator e = flowMap.values().iterator(); e.hasNext(); ) {
//             Flow flow = (Flow)e.next();
//             if (flow instanceof StaticContent) {
//                 continue;
//             }

//             Status status = flow.getStatus();
//             isIncomplete |= status.isIncomplete();
//         }
//         return isIncomplete;
//     }

//     /**
//      * Returns the flow that maps to the given region class for the current
//      * page master.
//      */
//     private Flow getCurrentFlow(String regionClass) {
//         Region region = getCurrentSimplePageMaster().getRegion(regionClass);
//         if (region != null) {
//             Flow flow = (Flow)flowMap.get(region.getRegionName());
//             return flow;

//         } else {

//             getLogger().error("flow is null. regionClass = '" + regionClass
//                                + "' currentSPM = "
//                                + getCurrentSimplePageMaster());

//             return null;
//         }

//     }

//      private boolean isFlowForMasterNameDone(String masterName) {
//          // parameter is master-name of PMR; we need to locate PM
//          // referenced by this, and determine whether flow(s) are OK
//          if (isForcing)
//              return false;
//          if (masterName != null) {

//              SimplePageMaster spm =
//                  this.layoutMasterSet.getSimplePageMaster(masterName);
//              Region region = spm.getRegion(Region.BODY);


//              Flow flow = (Flow)flowMap.get(region.getRegionName());
//              /*if ((null == flow) || flow.getStatus().isIncomplete())
//                  return false;
//              else
//                  return true;*/
//          }
//          return false;
//      }

//      public boolean isFlowSet() {
//          return isFlowSet;
//      }

//      public void setIsFlowSet(boolean isFlowSet) {
//          this.isFlowSet = isFlowSet;
//      }

    /**
     * Get the "initial-page-number" value.
     *
     * @return the initial-page-number property value
     */
    public String getIpnValue() {
        return ipnValue;
    }

    /**
     * Get the current page number for this page sequence.
     *
     * @return the current page number
     */
    public int getCurrentPageNumber() {
        return currentPageNumber;
    }

//     private void forcePage(AreaTree areaTree, int firstAvailPageNumber) {
//         boolean makePage = false;
//         if (this.forcePageCount == ForcePageCount.AUTO) {
//             PageSequence nextSequence =
//                 this.root.getSucceedingPageSequence(this);
//             if (nextSequence != null) {
//                 if (nextSequence.getIpnValue().equals("auto")) {
//                     // do nothing special
//                 }
//                 else if (nextSequence.getIpnValue().equals("auto-odd")) {
//                     if (firstAvailPageNumber % 2 == 0) {
//                         makePage = true;
//                     }
//                 } else if (nextSequence.getIpnValue().equals("auto-even")) {
//                     if (firstAvailPageNumber % 2 != 0) {
//                         makePage = true;
//                     }
//                 } else {
//                     int nextSequenceStartPageNumber =
//                         nextSequence.getCurrentPageNumber();
//                     if ((nextSequenceStartPageNumber % 2 == 0)
//                             && (firstAvailPageNumber % 2 == 0)) {
//                         makePage = true;
//                     } else if ((nextSequenceStartPageNumber % 2 != 0)
//                                && (firstAvailPageNumber % 2 != 0)) {
//                         makePage = true;
//                     }
//                 }
//             }
//         } else if ((this.forcePageCount == ForcePageCount.EVEN)
//                    && (this.pageCount % 2 != 0)) {
//             makePage = true;
//         } else if ((this.forcePageCount == ForcePageCount.ODD)
//                    && (this.pageCount % 2 == 0)) {
//             makePage = true;
//         } else if ((this.forcePageCount == ForcePageCount.END_ON_EVEN)
//                    && (firstAvailPageNumber % 2 == 0)) {
//             makePage = true;
//         } else if ((this.forcePageCount == ForcePageCount.END_ON_ODD)
//                    && (firstAvailPageNumber % 2 != 0)) {
//             makePage = true;
//         } else if (this.forcePageCount == ForcePageCount.NO_FORCE) {
//             // do nothing
//         }

//         if (makePage) {
//             try {
//                 this.isForcing = true;
//                 this.currentPageNumber++;
//                 firstAvailPageNumber = this.currentPageNumber;
//                 currentPage = makePage(areaTree, firstAvailPageNumber, false,
//                                        true);
//                 String formattedPageNumber =
//                     pageNumberGenerator.makeFormattedPageNumber(this.currentPageNumber);
//                 currentPage.setFormattedNumber(formattedPageNumber);
//                 currentPage.setPageSequence(this);
//                 formatStaticContent(areaTree);
//                 log.debug("[forced-" + firstAvailPageNumber + "]");
//                 areaTree.addPage(currentPage);
//                 this.root.setRunningPageNumberCounter(this.currentPageNumber);
//                 this.isForcing = false;
//             } catch (FOPException fopex) {
//                 log.debug("'force-page-count' failure");
//             }
//         }
//     }

    /**
     * Get the static content FO node from the flow map.
     * This gets the static content flow for the given flow name.
     *
     * @param name the flow name to find
     * @return the static content FO node
     */
    public StaticContent getStaticContent(String name) {
        return (StaticContent)flowMap.get(name);
    }

    /**
     * Accessor method for layoutMasterSet
     * @return layoutMasterSet for this object
     */
    public LayoutMasterSet getLayoutMasterSet() {
        return layoutMasterSet;
    }

    /**
     * Accessor method for titleFO
     * @return titleFO for this object
     */
    public Title getTitleFO() {
        return titleFO;
    }

    /**
     * Hook for Visitor objects accessing the FO Tree.
     * @param fotv the FOTreeVisitor object accessing this node of the FO Tree
     */
    public void acceptVisitor(FOTreeVisitor fotv) {
        fotv.servePageSequence(this);
    }

    /**
     * Public accessor for getting the MainFlow to which this PageSequence is
     * attached.
     * @return the MainFlow object to which this PageSequence is attached.
     */
    public Flow getMainFlow() {
        return mainFlow;
    }

    /**
     * Public accessor for getting the PageSequenceMaster (if any) to which this
     * PageSequence is attached.
     * @return the PageSequenceMaster to which this PageSequence is attached, or
     * null if there is none.
     */
    public PageSequenceMaster getPageSequenceMaster() {
        return pageSequenceMaster;
    }

    /**
     * Public accessor for getting the SimplePageMaster (if any) to which this
     * PageSequence is attached.
     * @return the SimplePageeMaster to which this PageSequence is attached or
     * null if there is none.
     */
    public SimplePageMaster getSimplePageMaster() {
        return simplePageMaster;
    }

    /**
     * Public accessor for getting the PageNumberGenerator.
     * @return the PageNumberGenerator
     */
    public PageNumberGenerator getPageNumberGenerator() {
        return pageNumberGenerator;
    }

    /**
     * Public accessor for setting the currentPageNumber.
     * @param currentPageNumber the value to which currentPageNumber should be
     * set.
     */
    public void setCurrentPageNumber(int currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
    }

    /**
     * Public accessor for the ancestor Root.
     * @return the ancestor Root
     */
    public Root getRoot() {
        return root;
    }

    public String getName() {
        return "fo:page-sequence";
    }
}
