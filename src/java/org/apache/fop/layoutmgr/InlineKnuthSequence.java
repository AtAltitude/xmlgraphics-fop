/*
 * Copyright 2005 The Apache Software Foundation.
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

import java.util.LinkedList;
import java.util.List;

import org.apache.fop.layoutmgr.inline.InlineLevelLayoutManager;
import org.apache.fop.layoutmgr.inline.KnuthInlineBox;


/**
 * 
 */
public class InlineKnuthSequence extends KnuthSequence  {

    private boolean isClosed = false;

    /**
     * Creates a new and empty list. 
     */
    public InlineKnuthSequence() {
        super();
    }
    
    /**
     * Creates a new list from an existing list.
     * @param list The list from which to create the new list.
     */
    public InlineKnuthSequence(List list) {
        super(list);
    }

    /**
     * Is this an inline or a block sequence?
     * @return false
     */
    public boolean isInlineSequence() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.fop.layoutmgr.KnuthSequence#canAppendSequence(org.apache.fop.layoutmgr.KnuthSequence)
     */
    public boolean canAppendSequence(KnuthSequence sequence) {
        return sequence.isInlineSequence() && !isClosed;
    }

    /* (non-Javadoc)
     * @see org.apache.fop.layoutmgr.KnuthSequence#appendSequence(org.apache.fop.layoutmgr.KnuthSequence, org.apache.fop.layoutmgr.LayoutManager)
     */
    public boolean appendSequence(KnuthSequence sequence, LayoutManager lm) {
        if (!canAppendSequence(sequence)) {
            return false;
        }
        // does the first element of the first paragraph add to an existing word?
        ListElement lastOldElement, firstNewElement;
        lastOldElement = getLast();
        firstNewElement = sequence.getElement(0);
        if (firstNewElement.isBox() && !((KnuthElement) firstNewElement).isAuxiliary()
                && lastOldElement.isBox() && ((KnuthElement) lastOldElement).getW() != 0) {
            addALetterSpace();
        }
        addAll(sequence);
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.fop.layoutmgr.KnuthSequence#appendSequenceOrClose(org.apache.fop.layoutmgr.KnuthSequence, org.apache.fop.layoutmgr.LayoutManager)
     */
    public boolean appendSequenceOrClose(KnuthSequence sequence, LayoutManager lm) {
        if (!appendSequence(sequence, lm)) {
            endSequence();
            return false;
        } else {
            return true;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.fop.layoutmgr.KnuthSequence#endSequence()
     */
    public KnuthSequence endSequence() {
        if (!isClosed) {
            add(new KnuthPenalty(0, -KnuthElement.INFINITE, false, null, false));
            isClosed = true;
        }
        return this;
    }

    public void addALetterSpace() {
        KnuthBox prevBox = (KnuthBox) removeLast();
        LinkedList oldList = new LinkedList();
        // if there are two consecutive KnuthBoxes the
        // first one does not represent a whole word,
        // so it must be given one more letter space
        if (!prevBox.isAuxiliary()) {
            // if letter spacing is constant,
            // only prevBox needs to be replaced;
            oldList.add(prevBox);
        } else {
            // prevBox is the last element
            // in the sub-sequence
            //   <box> <aux penalty> <aux glue> <aux box>
            // the letter space is added to <aux glue>,
            // while the other elements are not changed
            oldList.add(prevBox);
            oldList.addFirst((KnuthGlue) removeLast());
            oldList.addFirst((KnuthPenalty) removeLast());
            oldList.addFirst((KnuthBox) removeLast());
        }
        // adding a letter space could involve, according to the text
        // represented by oldList, replacing a glue element or adding
        // new elements
        addAll(((InlineLevelLayoutManager)
                     prevBox.getLayoutManager())
                    .addALetterSpaceTo(oldList));
        if (((KnuthInlineBox) prevBox).isAnchor()) {
            // prevBox represents a footnote citation: copy footnote info
            // from prevBox to the new box
            KnuthInlineBox newBox = (KnuthInlineBox) getLast();
            newBox.setFootnoteBodyLM(((KnuthInlineBox) prevBox).getFootnoteBodyLM());
        }
    }

}
