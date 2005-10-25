/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

/**
 * A string of characters without spaces
 */
public class WordArea extends InlineArea {

    /**
     * The text for this word area
     */
    protected String word;
    
    /**
     * The correction offset for the next area
     */
    protected int offset = 0;

    /**
     * Create a text inline area
     * @param w the word string
     */
    public WordArea(String w) {
        word = w;
    }

    /**
     * @return Returns the word.
     */
    public String getWord() {
        return word;
    }

    /**
     * @return Returns the offset.
     */
    public int getOffset() {
        return offset;
    }
    /**
     * @param o The offset to set.
     */
    public void setOffset(int o) {
        offset = o;
    }
}
