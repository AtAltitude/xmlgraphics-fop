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

/**
 * Abstract base class for objects that are processed by the renderer outside
 * of the actual document.
 * This object can be handled by the renderer according to three
 * possibilities, IMMEDIATELY, AFTER_PAGE, or END_OF_DOC.
 */
public abstract class OffDocumentItem {

    /**
     * Process this extension immediately when
     * being handled by the area tree.
     */
    public static final int IMMEDIATELY = 0;

    /**
     * Process this extension after the next page is rendered
     * or prepared when being handled by the area tree.
     */
    public static final int AFTER_PAGE = 1;

    /**
     * Process this extension at the end of the document once
     * all pages have been fully rendered.
     */
    public static final int END_OF_DOC = 2;


    protected int whenToProcess = IMMEDIATELY;
    
    /**
     * Get an indicator of when this item should be processed
     * @return int constant (IMMEDIATELY, AFTER_PAGE, END_OF_DOC)
     */
    public int getWhenToProcess() {
        return whenToProcess;
    }
}
