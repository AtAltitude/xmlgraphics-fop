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

// XML
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

// FOP
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;

/**
 * A conditional-page-master-reference formatting object.
 * This is a reference to a page master with a set of conditions.
 * The conditions must be satisfied for the referenced master to
 * be used.
 * This element is must be the child of a repeatable-page-master-alternatives
 * element.
 */
public class ConditionalPageMasterReference extends FObj {

    private RepeatablePageMasterAlternatives repeatablePageMasterAlternatives;

    private String masterName;

    private int pagePosition;
    private int oddOrEven;
    private int blankOrNotBlank;

    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    public ConditionalPageMasterReference(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
     * XSL Content Model: empty
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws SAXParseException {
       invalidChildError(loc, nsURI, localName);
    }

    /**
     * @see org.apache.fop.fo.FObj#addProperties
     */
    protected void addProperties(Attributes attlist) throws SAXParseException {
        super.addProperties(attlist);
        if (getProperty(PR_MASTER_REFERENCE) != null) {
            setMasterName(getProperty(PR_MASTER_REFERENCE).getString());
        }

        validateParent(parent);

        this.pagePosition = getPropEnum(PR_PAGE_POSITION);
        this.oddOrEven = getPropEnum(PR_ODD_OR_EVEN);
        this.blankOrNotBlank = getPropEnum(PR_BLANK_OR_NOT_BLANK);
    }

    /**
     * Sets the master name.
     * @param masterName name for the master
     */
    protected void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    /**
     * Returns the "master-name" attribute of this page master reference
     * @return the master name
     */
    public String getMasterName() {
        return masterName;
    }

    /**
     * Check if the conditions for this reference are met.
     * checks the page number and emptyness to determine if this
     * matches.
     * @param isOddPage True if page number odd
     * @param isFirstPage True if page is first page
     * @param isBlankPage True if page is blank
     * @return True if the conditions for this reference are met
     */
    protected boolean isValid(boolean isOddPage,
                              boolean isFirstPage,
                              boolean isBlankPage) {
        // page-position
        if (isFirstPage) {
            if (pagePosition == PagePosition.REST) {
                return false;
            } else if (pagePosition == PagePosition.LAST) {
                // ?? how can one know at this point?
                getLogger().debug("LAST PagePosition NYI");
                return false;
            }
        } else {
            if (pagePosition == PagePosition.FIRST) {
                return false;
            } else if (pagePosition == PagePosition.LAST) {
                // ?? how can one know at this point?
                getLogger().debug("LAST PagePosition NYI");
                // potentially valid, don't return
            }
        }

        // odd-or-even
        if (isOddPage) {
            if (oddOrEven == OddOrEven.EVEN) {
              return false;
            }
        } else {
            if (oddOrEven == OddOrEven.ODD) {
              return false;
            }
        }

        // blank-or-not-blank
        if (isBlankPage) {
            if (blankOrNotBlank == BlankOrNotBlank.NOT_BLANK) {
                return false;
            }
        } else {
            if (blankOrNotBlank == BlankOrNotBlank.BLANK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check that the parent is the right type of formatting object
     * repeatable-page-master-alternatives.
     * @param parent parent node
     * @throws SAXParseException If the parent is invalid
     */
    protected void validateParent(FONode parent) throws SAXParseException {
        if (parent.getName().equals("fo:repeatable-page-master-alternatives")) {
            this.repeatablePageMasterAlternatives =
                (RepeatablePageMasterAlternatives)parent;

            if (getMasterName() == null) {
                getLogger().warn("single-page-master-reference"
                                       + "does not have a master-name and so is being ignored");
            } else {
                this.repeatablePageMasterAlternatives.addConditionalPageMasterReference(this);
            }
        } else {
            throw new SAXParseException("fo:conditional-page-master-reference must be child "
                                   + "of fo:repeatable-page-master-alternatives, not "
                                   + parent.getName(), locator);
        }
    }

    /**
     * @see org.apache.fop.fo.FObj#getName()
     */
    public String getName() {
        return "fo:conditional-page-master-reference";
    }
    
    /**
     * @see org.apache.fop.fo.FObj#getNameId()
     */
    public int getNameId() {
        return FO_CONDITIONAL_PAGE_MASTER_REFERENCE;
    }
}
