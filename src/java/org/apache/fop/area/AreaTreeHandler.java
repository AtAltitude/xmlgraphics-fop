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
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.extensions.Outline;
import org.apache.fop.fo.extensions.Bookmarks;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.layoutmgr.PageSequenceLayoutManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class AreaTreeHandler extends FOEventHandler {

    // show statistics after document complete?
    private boolean outputStatistics;

    private static final boolean MEM_PROFILE_WITH_GC = false;
    
    // for statistics gathering
    private Runtime runtime;

    // heap memory allocated (for statistics)
    private long initialMemory;

    // time used in rendering (for statistics)
    private long startTime;

    // count of number of pages rendered
    private int pageCount;

    // AreaTreeModel in use
    private AreaTreeModel model;

    // hashmap of arraylists containing pages with id area
    private Map idLocations = new HashMap();

    // idref's whose corresponding id's have yet to be found
    // Each idref has a HashSet of Resolvable objects containing that idref
    private Map unresolvedIDRefs = new HashMap();

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

        model = new RenderPagesModel(userAgent, renderType, fontInfo,
            stream);
            
        outputStatistics = log.isDebugEnabled();

        if (outputStatistics) {
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

        Set todo = (Set) unresolvedIDRefs.get(id);
        if (todo != null) {
            for (Iterator iter = todo.iterator(); iter.hasNext();) {
                Resolvable res = (Resolvable)iter.next();
                res.resolve(id, list);
            }
            unresolvedIDRefs.remove(id);
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
     * Add an Resolvable object with an unresolved idref
     * @param idref the idref whose target id has not yet been located
     * @param res the Resolvable object with the unresolved idref
     */
    public void addUnresolvedIDRef(String idref, Resolvable res) {
        Set todo = (Set) unresolvedIDRefs.get(idref);
        if (todo == null) {
            todo = new HashSet();
            unresolvedIDRefs.put(idref, todo);
        }
        // add Resolvable object to this HashSet
        todo.add(res);
    }

    /**
     * Prepare AreaTreeHandler for document processing
     * This is called from FOTreeBuilder.startDocument()
     *
     * @throws SAXException if there is an error
     */
    public void startDocument() throws SAXException {
        //Initialize statistics
        if (outputStatistics) {
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
        // deal with unresolved references
        for (Iterator iter = unresolvedIDRefs.keySet().iterator(); 
                iter.hasNext();) {
            String id = (String)iter.next();
            Set list = (Set) unresolvedIDRefs.get(id);
            for (Iterator resIter = list.iterator(); resIter.hasNext();) {
                Resolvable res = (Resolvable)resIter.next();
                if (!res.isResolved()) {
                    res.resolve(id, null);
                }
            }
        }
        model.endDocument();

        if (outputStatistics) {
            if (MEM_PROFILE_WITH_GC) {
                // This takes time but gives better results
                System.gc();
            }
            long memoryNow = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = (memoryNow - initialMemory) / 1024L;
            long timeUsed = System.currentTimeMillis() - startTime;
            if (log != null && log.isDebugEnabled()) {
                log.debug("Initial heap size: " + (initialMemory / 1024L) + "Kb");
                log.debug("Current heap size: " + (memoryNow / 1024L) + "Kb");
                log.debug("Total memory used: " + memoryUsed + "Kb");
                if (!MEM_PROFILE_WITH_GC) {
                    log.debug("  Memory use is indicative; no GC was performed");
                    log.debug("  These figures should not be used comparatively");
                }
                log.debug("Total time used: " + timeUsed + "ms");
                log.debug("Pages rendered: " + pageCount);
                if (pageCount > 0) {
                    log.debug("Avg render time: " + (timeUsed / pageCount) + "ms/page");
                }
            }
        }
    }

    /**
     * End the PageSequence.
     * The PageSequence formats Pages and adds them to the AreaTree.
     * The area tree then handles what happens with the pages.
     *
     * @param pageSequence the page sequence ending
     */
    public void endPageSequence(PageSequence pageSequence) {

        if (outputStatistics) {
            if (MEM_PROFILE_WITH_GC) {
                // This takes time but gives better results
                System.gc();
            }
            long memoryNow = runtime.totalMemory() - runtime.freeMemory();
            if (log != null) {
                log.debug("Current heap size: " + (memoryNow / 1024L) + "Kb");
            }
        }

        // If no main flow, nothing to layout!
        if (pageSequence.getMainFlow() != null) {
            addBookmarks(pageSequence.getRoot().getBookmarks());
            PageSequenceLayoutManager pageSLM 
                = new PageSequenceLayoutManager(this, pageSequence);
            pageSLM.run();
            pageSequence.setCurrentPageNumber(pageSLM.getPageCount());
        }
    }

    /**
     * Create the bookmark data in the area tree.
     */
    private void addBookmarks(Bookmarks bookmarks) {
        if (bookmarks == null) {
            return;
        }

        log.debug("adding bookmarks to area tree");
        BookmarkData data = new BookmarkData();
        for (int count = 0; count < bookmarks.getOutlines().size(); count++) {
            Outline out = (Outline)(bookmarks.getOutlines()).get(count);
            data.addSubData(createBookmarkData(out));
        }
        addOffDocumentItem(data);
        data.setAreaTreeModel(model);
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
    private BookmarkData createBookmarkData(Outline outline) {
        BookmarkData data = new BookmarkData(outline.getInternalDestination());
        data.setLabel(outline.getLabel());
        for (int count = 0; count < outline.getOutlines().size(); count++) {
            Outline out = (Outline)(outline.getOutlines()).get(count);
            data.addSubData(createBookmarkData(out));
        }
        return data;
    }

    /**
     * Add a OffDocumentItem to the area tree model
     * This checks if the OffDocumentItem is resolvable and attempts
     * to resolve or add the resolvable ids for later resolution.
     * @param ext the OffDocumentItem to add.
     */
    private void addOffDocumentItem(OffDocumentItem ext) {
        if (ext instanceof Resolvable) {
            Resolvable res = (Resolvable)ext;
            String[] ids = res.getIDs();
            for (int count = 0; count < ids.length; count++) {
                if (idLocations.containsKey(ids[count])) {
                    res.resolve(ids[count], (List)idLocations.get(ids[count]));
                } else {
                    Set todo = (Set) unresolvedIDRefs.get(ids[count]);
                    if (todo == null) {
                        todo = new HashSet();
                        unresolvedIDRefs.put(ids[count], todo);
                    }
                    todo.add(ext);
                }
            }
        } else {
            model.handleOffDocumentItem(ext);
        }
    }
}

