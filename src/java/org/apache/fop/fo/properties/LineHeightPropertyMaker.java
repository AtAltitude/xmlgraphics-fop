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

package org.apache.fop.fo.properties;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * A maker which calculates the line-height property.
 * This property maker is special because line-height inherit the specified
 * value, instead of the computed value.
 * So when a line-height is create based on a attribute, the specified value
 * is stored in the property and in compute() the stored specified value of
 * the nearest specified is used to recalculate the line-height.  
 */

public class LineHeightPropertyMaker extends SpaceProperty.Maker {
    /**
     * Create a maker for line-height.
     * @param propId the is for linehight.
     */
    public LineHeightPropertyMaker(int propId) {
        super(propId);
    }

    /**
     * Make a property as normal, and save the specified value.
     * @see PropertyMaker#make(PropertyList, String, FObj)
     */
    public Property make(PropertyList propertyList, String value,
                         FObj fo) throws FOPException {
        Property p = super.make(propertyList, value, fo);
        p.setSpecifiedValue(checkValueKeywords(value));
        return p;
    }
    
    /**
     * Recalculate the line-height value based on the nearest specified
     * value.
     * @see PropertyMaker#compute(PropertyList)
     */
    protected Property compute(PropertyList propertyList) throws FOPException {
        // recalculate based on last specified value
        // Climb up propertylist and find last spec'd value
        Property specProp = propertyList.getNearestSpecified(propId);
        if (specProp != null) {
            String specVal = specProp.getSpecifiedValue();
            if (specVal != null) {
                try {
                    return make(propertyList, specVal,
                            propertyList.getParentFObj());
                } catch (FOPException e) {
                    //getLogger()error("Error computing property value for "
                    //                       + propName + " from "
                    //                       + specVal);
                    return null;
                }
            }
        }
        return null;
    }

    public Property convertProperty(Property p,
            PropertyList propertyList,
            FObj fo) throws FOPException {
        Numeric numval = p.getNumeric();
        if (numval != null && numval.getDimension() == 0) {
            try {
                p = new PercentLength(numval.getNumericValue(), getPercentBase(fo,propertyList));
            } catch (PropertyException exc) {
                // log.error("exception", exc);
            }
        }
        return super.convertProperty(p, propertyList, fo);
    }
   
    /*
    protected Property convertPropertyDatatype(Property p, 
                                               PropertyList propertyList,
                                               FObj fo) {
        Number numval = p.getNumber();
        if (numval != null) {
            return new PercentLength(numval.doubleValue(), getPercentBase(fo,propertyList));
        }
        return super.convertPropertyDatatype(p, propertyList, fo);
    }
    */
}
