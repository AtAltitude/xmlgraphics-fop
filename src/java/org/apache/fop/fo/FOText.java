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

package org.apache.fop.fo;

// Java
import java.util.NoSuchElementException;

// FOP
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.pagination.Root;

/**
 * A text node in the formatting object tree.
 *
 * Unfortunately the BufferManager implementatation holds
 * onto references to the character data in this object
 * longer than the lifetime of the object itself, causing
 * excessive memory consumption and OOM errors.
 *
 * @author unascribed
 * @author <a href="mailto:mark-fop@inomial.com">Mark Lillywhite</a>
 */
public class FOText extends FObj {

    /**
     * the character array containing the text
     */
    public char[] ca;

    /**
     * The actual length of the text to be rendered within ca,
     * starting from position 0 of the array.  
     *
     * This value is originally equal to ca.length, but becomes decremented
     * during whitespace removal by the flow.Block class, via the 
     * TextCharIterator.remove() method below.
     */
    public int length;

    /**
     * The TextInfo object attached to the text
     */
    public TextInfo textInfo;

    /**
     * Keeps track of the last FOText object created within the current
     * block. This is used to create pointers between such objects.
     * TODO: As soon as the control hierarchy is straightened out, this static
     * variable needs to become an instance variable in some parent object,
     * probably the page-sequence.
     */
    private static FOText lastFOTextProcessed = null;

    /**
     * Points to the previous FOText object created within the current
     * block. If this is "null", this is the first such object.
     */
    private FOText prevFOTextThisBlock = null;

    /**
     * Points to the next FOText object created within the current
     * block. If this is "null", this is the last such object.
     */
    private FOText nextFOTextThisBlock = null;

    /**
     * Points to the ancestor Block object. This is used to keep track of
     * which FOText nodes are descendants of the same block.
     */
    private Block ancestorBlock = null;

    private static final int IS_WORD_CHAR_FALSE = 0;
    private static final int IS_WORD_CHAR_TRUE = 1;
    private static final int IS_WORD_CHAR_MAYBE = 2;

    /**
     *
     * @param chars array of chars which contains the text in this object (may
     * be a superset of the text in this object)
     * @param start starting index into char[] for the text in this object
     * @param end ending index into char[] for the text in this object
     * @param ti TextInfo object for the text in this object
     * @param parent FONode that is the parent of this object
     */
    public FOText(char[] chars, int start, int end, TextInfo ti, FONode parent) {
        super(parent);
        length = end - start;
        this.ca = new char[length];
        System.arraycopy(chars, start, ca, 0, length);
        textInfo = ti;
        createBlockPointers();
        textTransform();
    }

    /**
     * Check if this text node will create an area.
     * This means either there is non-whitespace or it is
     * preserved whitespace.
     * Maybe this just needs to check length > 0, since char iterators
     * handle whitespace.
     *
     * @return true if this will create an area in the output
     */
    public boolean willCreateArea() {
        if (textInfo.whiteSpaceCollapse == WhiteSpaceCollapse.FALSE
                && length > 0) {
            return true;
        }

        for (int i = 0; i < length; i++) {
            char ch = ca[i];
            if (!((ch == ' ')
                    || (ch == '\n')
                    || (ch == '\r')
                    || (ch == '\t'))) { // whitespace
                return true;
            }
        }
        return false;
    }

    /**
     * @return a new TextCharIterator
     */
    public CharIterator charIterator() {
        return new TextCharIterator();
    }

    private class TextCharIterator extends AbstractCharIterator {
        private int curIndex = 0;

        public boolean hasNext() {
            return (curIndex < length);
        }

        public char nextChar() {
            if (curIndex < length) {
                // Just a char class? Don't actually care about the value!
                return ca[curIndex++];
            } else {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            if (curIndex > 0 && curIndex < length) {
                // copy from curIndex to end to curIndex-1
                System.arraycopy(ca, curIndex, ca, curIndex - 1,
                                 length - curIndex);
                length--;
                curIndex--;
            } else if (curIndex == length) {
                curIndex = --length;
            }
//          Temporary until leading space problem in 1.0 fixed
//          System.out.println("\n\nremove called: ca = \"" + 
//              new String(ca) + "\", length/node length: " + length 
//              + ", " + ca.length);
        }


        public void replaceChar(char c) {
            if (curIndex > 0 && curIndex <= length) {
                ca[curIndex - 1] = c;
            }
        }


    }

    /**
     * This method is run as part of the Constructor, to create xref pointers to
     * the previous FOText objects within the same Block
     */
    private void createBlockPointers() {
        // build pointers between the FOText objects withing the same Block
        //
        // find the ancestorBlock of the current node
        FONode ancestorFONode = this;
        while (this.ancestorBlock == null) {
            ancestorFONode = ancestorFONode.parent;
            Class myclass = ancestorFONode.getClass();
            if (ancestorFONode instanceof Root) {
                getLogger().warn("Unexpected: fo:text with no fo:block ancestor");
            }
            if (ancestorFONode instanceof Block) {
                this.ancestorBlock = (Block)ancestorFONode;
            }
        }
        // if the last FOText is a sibling, point to it, and have it point here
        if (lastFOTextProcessed != null) {
            if (lastFOTextProcessed.ancestorBlock == this.ancestorBlock) {
                prevFOTextThisBlock = lastFOTextProcessed;
                prevFOTextThisBlock.nextFOTextThisBlock = this;
            } else {
                prevFOTextThisBlock = null;
            }
        }
        // save the current node in static field so the next guy knows where
        // to look
        lastFOTextProcessed = this;
        return;
    }

    /**
     * This method is run as part of the Constructor, to handle the
     * text-transform property.
     */
    private void textTransform() {
        if (textInfo.textTransform == TextTransform.NONE) {
            return;
        }
        for (int i = 0; i < ca.length; i++) {
            ca[i] = charTransform(i);
        }
    }

    /**
     * Determines whether a particular location in an FOText object's text is
     * the start of a new "word". The use of "word" here is specifically for
     * the text-transform property, but may be useful for other things as
     * well, such as word-spacing. The definition of "word" is somewhat ambiguous
     * and appears to be definable by the user agent.
     *
     * @param i index into ca[]
     *
     * @return True if the character at this location is the start of a new
     * word.
     */
    public boolean isStartOfWord (int i) {
        char prevChar = getRelativeCharInBlock(i, -1);
        /* All we are really concerned about here is of what type prevChar
           is. If inputChar is not part of a word, then the Java
           conversions will (we hope) simply return inputChar.
        */
        switch (isWordChar(prevChar)) {
        case IS_WORD_CHAR_TRUE:
            return false;
        case IS_WORD_CHAR_FALSE:
            return true;
        /* "MAYBE" implies that additional context is needed. An example is a
         * single-quote, either straight or closing, which might be interpreted
         * as a possessive or a contraction, or might be a closing quote.
         */
        case IS_WORD_CHAR_MAYBE:
            char prevPrevChar = getRelativeCharInBlock(i, -2);
            switch (isWordChar(prevPrevChar)) {
            case IS_WORD_CHAR_TRUE:
                return false;
            case IS_WORD_CHAR_FALSE:
                return true;
            case IS_WORD_CHAR_MAYBE:
                return true;
            default:
                return false;
        }
        default:
            return false;
        }
    }

    /**
     * Finds a character within the current Block that is relative in location
     * to a character in the current FOText. Treats all FOText objects within a
     * block as one unit, allowing text in adjoining FOText objects to be
     * returned if the parameters are outside of the current object.
     *
     * @param i index into ca[]
     * @param offset signed integer with relative position within the
     *   block of the character to return. To return the character immediately
     *   preceding i, pass -1. To return the character immediately after i,
     *   pass 1.
     * @return the character in the offset position within the block; \u0000 if
     * the offset points to an area outside of the block.
     */
    public char getRelativeCharInBlock(int i, int offset) {
        // The easy case is where the desired character is in the same FOText
        if (((i + offset) >= 0) && ((i + offset) <= this.length)) {
            return ca[i + offset];
        }
        // For now, we can't look at following FOText nodes
        if (offset > 0) {
            return '\u0000';
        }
        // Remaining case has the text in some previous FOText node
        boolean foundChar = false;
        char charToReturn = '\u0000';
        FOText nodeToTest = this;
        int remainingOffset = offset + i;
        while (!foundChar) {
            if (nodeToTest.prevFOTextThisBlock == null) {
                foundChar = true;
                break;
            }
            nodeToTest = nodeToTest.prevFOTextThisBlock;
            if ((nodeToTest.ca.length + remainingOffset) >= 0) {
                charToReturn = nodeToTest.ca[nodeToTest.ca.length + remainingOffset];
                foundChar = true;
            } else {
                remainingOffset = remainingOffset + nodeToTest.ca.length;
            }
        }
        return charToReturn;
    }

    /**
     * @return The previous FOText node in this Block; null, if this is the
     * first FOText in this Block.
     */
    public FOText getPrevFOTextThisBlock () {
        return prevFOTextThisBlock;
    }

    /**
     * @return The next FOText node in this Block; null if this is the last
     * FOText in this Block; null if subsequent FOText nodes have not yet been
     * processed.
     */
    public FOText getNextFOTextThisBlock () {
        return nextFOTextThisBlock;
    }

    /**
     * @return The nearest ancestor block object which contains this FOText.
     */
    public Block getAncestorBlock () {
        return ancestorBlock;
    }

    /**
     * Transforms one character in ca[] using the text-transform property.
     *
     * @param i the index into ca[]
     * @return char with transformed value
     */
    public char charTransform(int i) {
        switch (textInfo.textTransform) {
        /* put NONE first, as this is probably the common case */
        case TextTransform.NONE:
            return ca[i];
        case TextTransform.UPPERCASE:
            return Character.toUpperCase(ca[i]);
        case TextTransform.LOWERCASE:
            return Character.toLowerCase(ca[i]);
        case TextTransform.CAPITALIZE:
            if (isStartOfWord(i)) {
                /*
                 Use toTitleCase here. Apparently, some languages use
                 a different character to represent a letter when using
                 initial caps than when all of the letters in the word
                 are capitalized. We will try to let Java handle this.
                */
                return Character.toTitleCase(ca[i]);
            } else {
                return Character.toLowerCase(ca[i]);
            }
        default:
            getLogger().warn("Invalid text-tranform value: "
                    + textInfo.textTransform);
            return ca[i];
        }
    }

    /**
     * Determines whether the input char should be considered part of a
     * "word". This is used primarily to determine whether the character
     * immediately following starts a new word, but may have other uses.
     * We have not found a definition of "word" in the standard (1.0), so the
     * logic used here is based on the programmer's best guess.
     *
     * @param inputChar the character to be tested.
     * @return int IS_WORD_CHAR_TRUE, IS_WORD_CHAR_FALSE, or IS_WORD_CHAR_MAYBE,
     * depending on whether the character should be considered part of a word
     * or not.
     */
    public static int isWordChar(char inputChar) {
        switch (Character.getType(inputChar)) {
        case Character.COMBINING_SPACING_MARK:
            return IS_WORD_CHAR_TRUE;
        case Character.CONNECTOR_PUNCTUATION:
            return IS_WORD_CHAR_TRUE;
        case Character.CONTROL:
            return IS_WORD_CHAR_FALSE;
        case Character.CURRENCY_SYMBOL:
            return IS_WORD_CHAR_TRUE;
        case Character.DASH_PUNCTUATION:
            if (inputChar == '-') {
                return IS_WORD_CHAR_TRUE; //hyphen
            }
            return IS_WORD_CHAR_FALSE;
        case Character.DECIMAL_DIGIT_NUMBER:
            return IS_WORD_CHAR_TRUE;
        case Character.ENCLOSING_MARK:
            return IS_WORD_CHAR_FALSE;
        case Character.END_PUNCTUATION:
            if (inputChar == '\u2019') {
                return IS_WORD_CHAR_MAYBE; //apostrophe, right single quote
            }
            return IS_WORD_CHAR_FALSE;
        case Character.FORMAT:
            return IS_WORD_CHAR_FALSE;
        case Character.LETTER_NUMBER:
            return IS_WORD_CHAR_TRUE;
        case Character.LINE_SEPARATOR:
            return IS_WORD_CHAR_FALSE;
        case Character.LOWERCASE_LETTER:
            return IS_WORD_CHAR_TRUE;
        case Character.MATH_SYMBOL:
            return IS_WORD_CHAR_FALSE;
        case Character.MODIFIER_LETTER:
            return IS_WORD_CHAR_TRUE;
        case Character.MODIFIER_SYMBOL:
            return IS_WORD_CHAR_TRUE;
        case Character.NON_SPACING_MARK:
            return IS_WORD_CHAR_TRUE;
        case Character.OTHER_LETTER:
            return IS_WORD_CHAR_TRUE;
        case Character.OTHER_NUMBER:
            return IS_WORD_CHAR_TRUE;
        case Character.OTHER_PUNCTUATION:
            if (inputChar == '\'') {
                return IS_WORD_CHAR_MAYBE; //ASCII apostrophe
            }
            return IS_WORD_CHAR_FALSE;
        case Character.OTHER_SYMBOL:
            return IS_WORD_CHAR_TRUE;
        case Character.PARAGRAPH_SEPARATOR:
            return IS_WORD_CHAR_FALSE;
        case Character.PRIVATE_USE:
            return IS_WORD_CHAR_FALSE;
        case Character.SPACE_SEPARATOR:
            return IS_WORD_CHAR_FALSE;
        case Character.START_PUNCTUATION:
            return IS_WORD_CHAR_FALSE;
        case Character.SURROGATE:
            return IS_WORD_CHAR_FALSE;
        case Character.TITLECASE_LETTER:
            return IS_WORD_CHAR_TRUE;
        case Character.UNASSIGNED:
            return IS_WORD_CHAR_FALSE;
        case Character.UPPERCASE_LETTER:
            return IS_WORD_CHAR_TRUE;
        default:
            return IS_WORD_CHAR_FALSE;
        }
    }

    /**
     * This is a hook for an FOTreeVisitor subclass to be able to access
     * this object.
     * @param fotv the FOTreeVisitor subclass that can access this object.
     * @see org.apache.fop.fo.FOTreeVisitor
     */
    public void acceptVisitor(FOTreeVisitor fotv) {
        fotv.serveFOText(this);
    }
}
