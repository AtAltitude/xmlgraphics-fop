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

// Java
import java.util.ArrayList;

// XML
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

// FOP
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.properties.Property;

/**
 * A repeatable-page-master-alternatives formatting object.
 * This contains a list of conditional-page-master-reference
 * and the page master is found from the reference that
 * matches the page number and emptyness.
 */
public class RepeatablePageMasterAlternatives extends FObj
    implements SubSequenceSpecifier {
    // The value of properties relevant for fo:repeatable-page-master-alternatives.
    private Property maximumRepeats;
    // End of property values
    
    private static final int INFINITE = -1;

    /**
     * Max times this page master can be repeated.
     * INFINITE is used for the unbounded case
     */
    private int _maximumRepeats;
    private int numberConsumed = 0;

    private ArrayList conditionalPageMasterRefs;

    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    public RepeatablePageMasterAlternatives(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FObj#bind(PropertyList)
     */
    public void bind(PropertyList pList) {
        maximumRepeats = pList.get(PR_MAXIMUM_REPEATS);
    }

    /**
     * @see org.apache.fop.fo.FONode#startOfNode
     */
    protected void startOfNode() throws SAXParseException {
        conditionalPageMasterRefs = new ArrayList();

        if (parent.getName().equals("fo:page-sequence-master")) {
            PageSequenceMaster pageSequenceMaster = (PageSequenceMaster)parent;
            pageSequenceMaster.addSubsequenceSpecifier(this);
        } else {
            throw new SAXParseException("fo:repeatable-page-master-alternatives "
                                   + "must be child of fo:page-sequence-master, not "
                                   + parent.getName(), locator);
        }
    }

    /**
     * @see org.apache.fop.fo.FONode#endOfNode
     */
    protected void endOfNode() throws SAXParseException {
        if (childNodes == null) {
           missingChildElementError("(conditional-page-master-reference+)");
        }
    }

    /**
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
        XSL/FOP: (conditional-page-master-reference+)
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName) 
        throws SAXParseException {
        if (!(nsURI == FO_URI &&
            localName.equals("conditional-page-master-reference"))) {
                invalidChildError(loc, nsURI, localName);
        }
    }

    /**
     * Return the "maximum-repeats" property.
     */
    public int getMaximumRepeats() {
        if (maximumRepeats.getEnum() == NO_LIMIT) {
            return INFINITE;
        } else {
            int mr = maximumRepeats.getNumeric().getValue();
            if (mr < 0) {
                getLogger().debug("negative maximum-repeats: "
                        + this.maximumRepeats);
                mr = 0;
            }
            return mr;
        }
    }

    /**
     * @see org.apache.fop.fo.FObj#addProperties
     */
    protected void addProperties(Attributes attlist) throws SAXParseException {
        super.addProperties(attlist);
        conditionalPageMasterRefs = new ArrayList();

        if (parent.getName().equals("fo:page-sequence-master")) {
            PageSequenceMaster pageSequenceMaster = (PageSequenceMaster)parent;
            pageSequenceMaster.addSubsequenceSpecifier(this);
        } else {
            throw new SAXParseException("fo:repeatable-page-master-alternatives "
                                   + "must be child of fo:page-sequence-master, not "
                                   + parent.getName(), locator);
        }

        Property mr = getProperty(PR_MAXIMUM_REPEATS);

        if (mr.getEnum() == NO_LIMIT) {
            this._maximumRepeats = INFINITE;
        } else {
            this._maximumRepeats = mr.getNumber().intValue();
            if (this._maximumRepeats < 0) {
                getLogger().debug("negative maximum-repeats: "
                                  + this.maximumRepeats);
                this._maximumRepeats = 0;
            }
        }
    }

    /**
     * Get the next matching page master from the conditional
     * page master references.
     * @see org.apache.fop.fo.pagination.SubSequenceSpecifier
     */
    public String getNextPageMasterName(boolean isOddPage,
                                        boolean isFirstPage,
                                        boolean isBlankPage) {
        if (_maximumRepeats != INFINITE) {
            if (numberConsumed < _maximumRepeats) {
                numberConsumed++;
            } else {
                return null;
            }
        }

        for (int i = 0; i < conditionalPageMasterRefs.size(); i++) {
            ConditionalPageMasterReference cpmr =
                (ConditionalPageMasterReference)conditionalPageMasterRefs.get(i);
            if (cpmr.isValid(isOddPage, isFirstPage, isBlankPage)) {
                return cpmr.getMasterName();
            }
        }
        return null;
    }


    /**
     * Adds a new conditional page master reference.
     * @param cpmr the new conditional reference
     */
    public void addConditionalPageMasterReference(ConditionalPageMasterReference cpmr) {
        this.conditionalPageMasterRefs.add(cpmr);
    }

    /**
     * @see org.apache.fop.fo.pagination.SubSequenceSpecifier#reset()
     */
    public void reset() {
        this.numberConsumed = 0;
    }

    public String getName() {
        return "fo:repeatable-page-master-alternatives";
    }

    /**
     * @see org.apache.fop.fo.FObj#getNameId()
     */
    public int getNameId() {
        return FO_REPEATABLE_PAGE_MASTER_ALTERNATIVES;
    }
}
