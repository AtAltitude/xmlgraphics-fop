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
 
package org.apache.fop.area.inline;

import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Resolveable;

import java.util.List;

/**
 * Unresolveable page number area.
 * This is a word area that resolves itself to a page number
 * from an id reference.
 */
public class UnresolvedPageNumber extends TextArea implements Resolveable {
    private boolean resolved = false;
    private String pageRefId;

    /**
     * Create a new unresolveable page number.
     *
     * @param id the id reference for resolving this
     */
    public UnresolvedPageNumber(String id) {
        pageRefId = id;
        text = "?";
    }

    /**
     * Get the id references for this area.
     *
     * @return the id reference for this unresolved page number
     */
    public String[] getIDs() {
        return new String[] {pageRefId};
    }

    /**
     * Resolve this page number reference.
     * This resolves the reference by getting the page number
     * string from the first page in the list of pages that apply
     * for the id reference. The word string is then set to the
     * page number string.
     *
     * @param id the id reference being resolved
     * @param pages the list of pages for the id reference
     */
    public void resolve(String id, List pages) {
        resolved = true;
        if (pages != null) {
            PageViewport page = (PageViewport)pages.get(0);
            String str = page.getPageNumber();
            text = str;

            /**@todo Update IPD ??? */
        }
    }

    /**
     * Check if this is resolved.
     *
     * @return true when this has been resolved
     */
    public boolean isResolved() {
       return resolved;
    }
}
