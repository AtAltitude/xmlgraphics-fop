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

import org.apache.fop.area.Area;

import java.util.List;
import java.util.ArrayList;

/**
 * Inline parent area.
 * This is an inline area that can have other inlines as children.
 */
public class InlineParent extends InlineArea {
    /**
     * The list of inline areas added to this inline parent.
     */
    protected List inlines = new ArrayList();

    /**
     * An inline parent is a reference area somay have clipping
     */
    protected boolean clip = false;

    /**
     * Create a new inline parent to add areas to.
     */
    public InlineParent() {
    }

    /**
     * Override generic Area method.
     *
     * @param childArea the child area to add
     */
    public void addChild(Area childArea) {
        if (childArea instanceof InlineArea) {
            inlines.add(childArea);
            increaseIPD(((InlineArea) childArea).getAllocIPD());
        }
    }

    /**
     * Get the child areas for this inline parent.
     *
     * @return the list of child areas
     */
    public List getChildAreas() {
        return inlines;
    }

}

