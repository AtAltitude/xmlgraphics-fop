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
package org.apache.fop.area;

// Java
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

// XML
import org.xml.sax.SAXException;

// Apache
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.extensions.BookmarkData;
import org.apache.fop.fo.FOInputHandler;
import org.apache.fop.fo.extensions.Outline;
import org.apache.fop.fo.extensions.Bookmarks;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.layoutmgr.AddLMVisitor;
import org.apache.fop.layoutmgr.ContentLayoutManager;
import org.apache.fop.layoutmgr.InlineStackingLayoutManager;
import org.apache.fop.layoutmgr.LMiter;
import org.apache.fop.layoutmgr.PageLayoutManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// Java
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Area tree handler for formatting objects.
 *
 * Concepts:
 * The area tree is to be as small as possible. With minimal classes
 * and data to fully represent an area tree for formatting objects.
 * The area tree needs to be simple to render and follow the spec
 * closely.
 * This area tree has the concept of page sequences.
 * Where ever possible information is discarded or optimized to
 * keep memory use low. The data is also organized to make it
 * possible for renderers to minimize their output.
 * A page can be saved if not fully resolved and once rendered
 * a page contains only size and id reference information.
 * The area tree pages are organized in a model that depends on the
 * type of renderer.
 */
public class AreaTreeHandler extends FOInputHandler {

    // TODO: Collecting of statistics should be configurable
    private final boolean collectStatistics = true;
    private static final boolean MEM_PROFILE_WITH_GC = false;
    private boolean pageSequenceFound = false;
    
    // for statistics gathering
    private Runtime runtime;

    // heap memory allocated (for statistics)
    private long initialMemory;

    // time used in rendering (for statistics)
    private long startTime;

    // count of number of pages rendered
    private int pageCount;

    /** Useful only for allowing subclasses of AddLMVisitor to be set by those
     extending FOP **/
    private AddLMVisitor addLMVisitor = null;

    // AreaTreeModel in use
    private AreaTreeModel model;

    // hashmap of arraylists containing pages with id area
    private Map idLocations = new HashMap();

    // list of id's yet to be resolved and arraylists of pages
    private Map resolve = new HashMap();

    private List treeExtensions = new ArrayList();

    private static Log log = LogFactory.getLog(AreaTreeHandler.class);

    /**
     * Constructor.
     * @param userAgent FOUserAgent object for process
     * @param renderType Desired fo.Constants output type (RENDER_PDF, 
     *   RENDER_PS, etc.)
     * @param stream OutputStream
     */
    public AreaTreeHandler (FOUserAgent userAgent, int renderType, 
        OutputStream stream) throws FOPException {
        super(userAgent);

        // model = new CachedRenderPagesModel(userAgent, renderType,
        //  fontInfo, stream);
        model = new RenderPagesModel(userAgent, renderType, fontInfo,
            stream);
            
        if (collectStatistics) {
            runtime = Runtime.getRuntime();
        }
    }

    /**
     * Get the area tree model for this area tree.
     *
     * @return AreaTreeModel the model being used for this area tree
     */
    public AreaTreeModel getAreaTreeModel() {
        return model;
    }

    /**
     * Add a new page to the area tree.
     * @param page the page to add
     */
    public void addPage(PageViewport page) {
        model.addPage(page);
    }

    /**
     * Add an id reference pointing to a page viewport.
     * @param id the id of the reference
     * @param pv the page viewport that contains the id reference
     */
    public void addIDRef(String id, PageViewport pv) {
        List list = (List)idLocations.get(id);
        if (list == null) {
            list = new ArrayList();
            idLocations.put(id, list);
        }
        list.add(pv);

        Set todo = (Set)resolve.get(id);
        if (todo != null) {
            for (Iterator iter = todo.iterator(); iter.hasNext();) {
                Resolveable res = (Resolveable)iter.next();
                res.resolve(id, list);
            }
            resolve.remove(id);
        }
    }

    /**
     * Get the list of id references for an id.
     * @param id the id to lookup
     * @return the list of id references.
     */
    public List getIDReferences(String id) {
        return (List)idLocations.get(id);
    }

    /**
     * Add an unresolved object with a given id.
     * @param id the id reference that needs resolving
     * @param res the Resolveable object to resolve
     */
    public void addUnresolvedID(String id, Resolveable res) {
        Set todo = (Set)resolve.get(id);
        if (todo == null) {
            todo = new HashSet();
            resolve.put(id, todo);
        }
        todo.add(res);
    }

    /**
     * Add a tree extension.
     * This checks if the extension is resolveable and attempts
     * to resolve or add the resolveable ids for later resolution.
     * @param ext the tree extension to add.
     */
    public void addTreeExtension(TreeExt ext) {
        treeExtensions.add(ext);
        if (ext.isResolveable()) {
            Resolveable res = (Resolveable)ext;
            String[] ids = res.getIDs();
            for (int count = 0; count < ids.length; count++) {
                if (idLocations.containsKey(ids[count])) {
                    res.resolve(ids[count], (List)idLocations.get(ids[count]));
                } else {
                    Set todo = (Set)resolve.get(ids[count]);
                    if (todo == null) {
                        todo = new HashSet();
                        resolve.put(ids[count], todo);
                    }
                    todo.add(ext);
                }
            }
        } else {
            handleTreeExtension(ext, TreeExt.IMMEDIATELY);
        }
    }

    /**
     * Handle a tree extension.
     * This sends the extension to the model for handling.
     * @param ext the tree extension to handle
     * @param when when the extension should be handled by the model
     */
    public void handleTreeExtension(TreeExt ext, int when) {
        // queue tree extension according to the when
        model.addExtension(ext, when);
    }

    /**
     * Prepare AreaTreeHandler for document processing
     * This is called from FOTreeBuilder.startDocument()
     *
     * @throws SAXException if there is an error
     */
    public void startDocument() throws SAXException {
        //Initialize statistics
        if (collectStatistics) {
            pageCount = 0;
            if (MEM_PROFILE_WITH_GC) {
                System.gc(); // This takes time but gives better results
            }

            initialMemory = runtime.totalMemory() - runtime.freeMemory();
            startTime = System.currentTimeMillis();
        }
    }

    /**
     * End the document.
     *
     * @throws SAXException if there is some error
     */
    public void endDocument() throws SAXException {
        if (pageSequenceFound == false) {
            throw new SAXException("Error: No fo:page-sequence child " +
                "found within fo:root element.");
        }

        // deal with unresolved references
        for (Iterator iter = resolve.keySet().iterator(); iter.hasNext();) {
            String id = (String)iter.next();
            Set list = (Set)resolve.get(id);
            for (Iterator resIter = list.iterator(); resIter.hasNext();) {
                Resolveable res = (Resolveable)resIter.next();
                if (!res.isResolved()) {
                    res.resolve(id, null);
                }
            }
        }
        model.endDocument();

        if (collectStatistics) {
            if (MEM_PROFILE_WITH_GC) {
                // This takes time but gives better results
                System.gc();
            }
            long memoryNow = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = (memoryNow - initialMemory) / 1024L;
            long timeUsed = System.currentTimeMillis() - startTime;
            if (logger != null && logger.isDebugEnabled()) {
                logger.debug("Initial heap size: " + (initialMemory / 1024L) + "Kb");
                logger.debug("Current heap size: " + (memoryNow / 1024L) + "Kb");
                logger.debug("Total memory used: " + memoryUsed + "Kb");
                if (!MEM_PROFILE_WITH_GC) {
                    logger.debug("  Memory use is indicative; no GC was performed");
                    logger.debug("  These figures should not be used comparatively");
                }
                logger.debug("Total time used: " + timeUsed + "ms");
                logger.debug("Pages rendered: " + pageCount);
                if (pageCount > 0) {
                    logger.debug("Avg render time: " + (timeUsed / pageCount) + "ms/page");
                }
            }
        }
    }

    /**
     * Create the bookmark data in the area tree.
     */
    public void addBookmarks(Bookmarks bookmarks) {
        if (bookmarks == null) {
            return;
        }

        log.debug("adding bookmarks to area tree");
        BookmarkData data = new BookmarkData();
        for (int count = 0; count < bookmarks.getOutlines().size(); count++) {
            Outline out = (Outline)(bookmarks.getOutlines()).get(count);
            data.addSubData(createBookmarkData(out));
        }
        addTreeExtension(data);
        data.setAreaTreeHandler(this);
    }

    /**
     * Create and return the bookmark data for this outline.
     * This creates a bookmark data with the destination
     * and adds all the data from child outlines.
     *
     * @param outline the Outline object for which a bookmark entry should be
     * created
     * @return the new bookmark data
     */
    public BookmarkData createBookmarkData(Outline outline) {
        BookmarkData data = new BookmarkData(outline.getInternalDestination());
        data.setLabel(outline.getLabel());
        for (int count = 0; count < outline.getOutlines().size(); count++) {
            Outline out = (Outline)(outline.getOutlines()).get(count);
            data.addSubData(createBookmarkData(out));
        }
        return data;
    }

    /**
     * Start a page sequence.
     * At the start of a page sequence it can start the page sequence
     * on the area tree with the page sequence title.
     *
     * @param pageSeq the page sequence starting
     */
    public void startPageSequence(PageSequence pageSeq) {
        pageSequenceFound = true;
    }

    /**
     * End the PageSequence.
     * The PageSequence formats Pages and adds them to the AreaTree.
     * The area tree then handles what happens with the pages.
     *
     * @param pageSequence the page sequence ending
     * @throws FOPException if there is an error formatting the pages
     */
    public void endPageSequence(PageSequence pageSequence)
                throws FOPException {
        //areaTree.setFontInfo(fontInfo);

        if (collectStatistics) {
            if (MEM_PROFILE_WITH_GC) {
                // This takes time but gives better results
                System.gc();
            }
            long memoryNow = runtime.totalMemory() - runtime.freeMemory();
            if (logger != null) {
                logger.debug("Current heap size: " + (memoryNow / 1024L) + "Kb");
            }
        }

        addBookmarks(pageSequence.getRoot().getBookmarks());
        formatPageSequence(pageSequence);
    }

    /**
     * Runs the formatting of this page sequence into the given area tree
     *
     * @param pageSeq the PageSequence to be formatted
     * @param areaTree the area tree to format this page sequence into
     * @throws FOPException if there is an error formatting the contents
     */
    private void formatPageSequence(PageSequence pageSeq) 
            throws FOPException {
        Title title = null;
        if (pageSeq.getTitleFO() != null) {
            title = getTitleArea(pageSeq.getTitleFO());
        }
        
        model.startPageSequence(title);

        // Make a new PageLayoutManager and a FlowLayoutManager
        // Run the PLM in a thread
        // Wait for them to finish.

        // If no main flow, nothing to layout!
        if (pageSeq.getMainFlow() == null) {
            return;
        }

        // Initialize if already used?
        //    this.layoutMasterSet.resetPageMasters();
        if (pageSeq.getPageSequenceMaster() != null) {
            pageSeq.getPageSequenceMaster().reset();
        }

        pageSeq.initPageNumber();

        // This will layout pages and add them to the area tree
        PageLayoutManager pageLM = new PageLayoutManager(this, pageSeq);
        pageLM.setPageCounting(pageSeq.getCurrentPageNumber(),
                               pageSeq.getPageNumberGenerator());

        // For now, skip the threading and just call run directly.
        pageLM.run();

        // Thread layoutThread = new Thread(pageLM);
        //  layoutThread.start();
        // log.debug("Layout thread started");
        
        // // wait on both managers
        // try {
        //     layoutThread.join();
        //     log.debug("Layout thread done");
        // } catch (InterruptedException ie) {
        //     log.error("PageSequence.format() interrupted waiting on layout");
        // }
        
        pageSeq.setCurrentPageNumber(pageLM.getPageCount());
        // Tell the root the last page number we created.
        pageSeq.getRoot().setRunningPageNumberCounter(pageSeq.getCurrentPageNumber());
    }

    /**
     * @return the Title area
     */
    private org.apache.fop.area.Title getTitleArea(org.apache.fop.fo.pagination.Title foTitle) {
        // use special layout manager to add the inline areas
        // to the Title.
        InlineStackingLayoutManager lm;
        lm = new InlineStackingLayoutManager(foTitle);
        lm.setLMiter(new LMiter(lm, foTitle.childNodes.listIterator()));
        lm.initialize();

        // get breaks then add areas to title
        org.apache.fop.area.Title title =
                 new org.apache.fop.area.Title();

        ContentLayoutManager clm = new ContentLayoutManager(title);
        clm.setUserAgent(foTitle.getUserAgent());
        lm.setParent(clm);

        clm.fillArea(lm);

        return title;
    }

    /**
     * Public accessor to get the AddLMVisitor object that should be used.
     * @return the AddLMVisitor object that should be used.
     */
    public AddLMVisitor getAddLMVisitor() {
        if (this.addLMVisitor == null) {
            this.addLMVisitor = new AddLMVisitor();
        }
        return this.addLMVisitor;
    }
    
}
