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

// may combine with before float into a conditional area

/**
 * Footnote reference area.
 * This areas holds footnote areas and an optional separator area.
 */
public class Footnote extends BlockParent {
    private Block separator = null;

    // footnote has an optional separator
    // and a list of sub block areas that can be added/removed

    // this is the relative position of the footnote inside
    // the body region
    private int top;

    /**
     * Set the separator area for this footnote.
     *
     * @param sep the separator area
     */
    public void setSeparator(Block sep) {
        separator = sep;
    }

    /**
     * Get the separator area for this footnote area.
     *
     * @return the separator area
     */
    public Block getSeparator() {
        return separator;
    }

}

