/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

package org.apache.fop.render.afp.modca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.fop.render.afp.modca.triplets.FullyQualifiedNameTriplet;
import org.apache.fop.render.afp.modca.triplets.MeasurementUnitsTriplet;
import org.apache.fop.render.afp.modca.triplets.ObjectAreaSizeTriplet;
import org.apache.fop.render.afp.modca.triplets.ObjectClassificationTriplet;
import org.apache.fop.render.afp.modca.triplets.StrucFlgs;
import org.apache.fop.render.afp.modca.triplets.Triplet;

/**
 * An abstract class encapsulating an MODCA structured object
 */
public abstract class AbstractStructuredAFPObject extends AbstractAFPObject {
   
    /**
     * list of object triplets
     */
    protected List/*<Triplet>*/ triplets = null;
    
    /**
     * triplet data created from triplet list
     */
    protected byte[] tripletData = null;

    /**
     * Default constructor
     */
    protected AbstractStructuredAFPObject() {
    }
    
    /**
     * @return the triplet data length
     */
    protected int getTripletDataLength() {
        if (tripletData == null) {
            try {
                getTripletData();
            } catch (IOException e) {
                log.error("failed to get triplet data");
            }
        }
        if (tripletData != null) {
            return tripletData.length;
        }
        return 0;
    }
    
    /**
     * @return the triplet data
     * @throws IOException throws an I/O exception if one occurred
     */
    protected byte[] getTripletData() throws IOException {
        if (tripletData == null && triplets != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeObjects(triplets, baos);
            this.tripletData = baos.toByteArray();
        }
        return this.tripletData;
    }
    
    /**
     * Writes any triplet data
     * @param os The stream to write to
     * @throws IOException The stream to write to
     */
    protected void writeTriplets(OutputStream os) throws IOException {
        if (tripletData != null) {
            os.write(tripletData);
        } else if (triplets != null) {
            writeObjects(triplets, os);
        }        
    }

    /**
     * Helper method to write the start of the Object.
     * @param os The stream to write to
     * @throws IOException throws an I/O exception if one occurred
     */
    protected void writeStart(OutputStream os) throws IOException {
        getTripletData();
    }

    /**
     * Helper method to write the contents of the Object.
     * @param os The stream to write to
     * @throws IOException throws an I/O exception if one occurred
     */
    protected void writeContent(OutputStream os) throws IOException {
        writeTriplets(os);
    }
    
    /**
     * Helper method to write the end of the Object.
     * @param os The stream to write to
     * @throws IOException an I/O exception if one occurred
     */
    protected void writeEnd(OutputStream os) throws IOException {
    }    
    
    /**
     * Accessor method to write the AFP datastream for the Image Object
     * @param os The stream to write to
     * @throws IOException in the event that an I/O exception occurred
     */
    public void write(OutputStream os) throws IOException {
        writeStart(os);
        writeContent(os);
        writeEnd(os);
    }

    /**
     * Returns the first matching triplet found in the structured field triplet list 
     * @param tripletId the triplet identifier
     */
    private Triplet getTriplet(byte tripletId) {
        Iterator it = getTriplets().iterator();
        while (it.hasNext()) {
            Triplet triplet = (Triplet)it.next();
            if (triplet.getId() == tripletId) {
                return triplet;
            }
        }
        return null;
    }
    
    /**
     * @param tripletId the triplet identifier
     * @return true if the structured field has the given triplet
     */
    private boolean hasTriplet(byte tripletId) {
        return getTriplet(tripletId) != null;
    }

    /**
     * Adds a triplet to this structured object
     * @param triplet the triplet to add
     */
    private void addTriplet(Triplet triplet) {
        getTriplets().add(triplet);
    }

    /**
     * Adds a list of triplets to the triplets contained within this structured field
     * @param tripletCollection a collection of triplets
     */
    private void addTriplets(Collection/*<Triplet>*/ tripletCollection) {
        if (tripletCollection != null) {
            getTriplets().addAll(tripletCollection);
        }
    }

    /**
     * @return the triplet list pertaining to this resource
     */
    protected List/*<Triplet>*/ getTriplets() {
        if (triplets == null) {
            triplets = new java.util.ArrayList();
        }
        return triplets;
    }
        
    /**
     * Sets the fully qualified name of this resource
     * @param fqnType the fully qualified name type of this resource
     * @param fqnFormat the fully qualified name format of this resource
     * @param fqName the fully qualified name of this resource
     */
    public void setFullyQualifiedName(byte fqnType, byte fqnFormat, String fqName) {
        addTriplet(new FullyQualifiedNameTriplet(fqnType, fqnFormat, fqName));
    }

    /**
     * @return the fully qualified name of this triplet or null if it does not exist
     */
    public String getFullyQualifiedName() {
        FullyQualifiedNameTriplet fqNameTriplet
            = (FullyQualifiedNameTriplet)getTriplet(Triplet.FULLY_QUALIFIED_NAME);
        if (fqNameTriplet != null) {
            return fqNameTriplet.getFullyQualifiedName();
        }
        log.warn(this + " has no fully qualified name");
        return null;
    }
    
    /**
     * Sets the objects classification
     * @param objectClass the classification of the object
     * @param objectType the MOD:CA registry object type entry for the given
     *        object/component type of the object
     * @param strucFlgs information on the structure of the object and its container
     */
    public void setObjectClassification(byte objectClass, Registry.ObjectType objectType,
            StrucFlgs strucFlgs) {
        addTriplet(new ObjectClassificationTriplet(objectClass, objectType, strucFlgs));
    }

    /**
     * Sets the objects classification with the default structure flags
     * @param objectClass the classification of the object
     * @param objectType the MOD:CA registry object type entry for the given
     *        object/component type of the object
     */
    public void setObjectClassification(byte objectClass, Registry.ObjectType objectType) {
        setObjectClassification(objectClass, objectType, StrucFlgs.getDefault());
    }
        
    /**
     * Specifies the extent of an object area in the X and Y directions
     * @param x the x direction extent
     * @param y the y direction extent
     */
    public void setObjectAreaSize(int x, int y) {
        addTriplet(new ObjectAreaSizeTriplet(x, y));
    }

    /**
     * Sets the measurement units used to specify the units of measure
     */
    public void setMeasurementUnits() {
        addTriplet(new MeasurementUnitsTriplet());
    }

    /**
     * Sets a comment on this resource
     * @param comment a comment string
     */
    public void setComment(String comment) {
        try {
            addTriplet(new Triplet(Triplet.COMMENT, comment));
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
    }
}
