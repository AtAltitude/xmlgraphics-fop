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

/* $Id: $ */

package org.apache.fop.render.afp.modca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.render.afp.DataObjectInfo;
import org.apache.fop.render.afp.modca.triplets.FullyQualifiedNameTriplet;
import org.apache.fop.render.afp.modca.triplets.ObjectClassificationTriplet;
import org.apache.fop.render.afp.tools.BinaryUtils;

/**
 * Object containers are MO:DCA objects that envelop and carry object data.
 */
public class ObjectContainer extends AbstractNamedAFPObject implements DataObjectAccessor {
                
    private static final String DEFAULT_NAME = "OC000001";

    /**
     * the data object
     */
    private AbstractDataObject dataObj = null;
    
    /**
     * the object data
     */
    private byte[] objectData = null;

    /**
     * the data object info
     */
    private DataObjectInfo dataObjectInfo;
    
    /**
     * Default constructor
     */
    public ObjectContainer() {
        super(DEFAULT_NAME);
    }

    /**
     * Main constructor
     * @param name the name of this object container
     */
    public ObjectContainer(String name) {
        super(name);        
    }
    
    /**
     * Sets the data object for this object container
     * @param dataObj the data object to reside within this object container
     */
    public void setDataObject(AbstractDataObject dataObj) {
        this.dataObj = dataObj;        
    }

    /**
     * {@inheritDoc}
     */
    public AbstractNamedAFPObject getDataObject() {
        return this.dataObj;
    }
    
    /**
     * {@inheritDoc}
     */
    public DataObjectInfo getDataObjectInfo() {
        return this.dataObjectInfo;
    }

    /**
     * {@inheritDoc}
     */
    public void setDataObjectInfo(DataObjectInfo dataObjectInfo) {
        this.dataObjectInfo = dataObjectInfo;
        
        Registry registry = Registry.getInstance();
        Registry.ObjectType objectType = registry.getObjectType(dataObjectInfo);
        if (objectType != null) {
            super.setObjectClassification(
                    ObjectClassificationTriplet.CLASS_TIME_VARIANT_PRESENTATION_OBJECT,
                    objectType);
        } else {
            log.warn("no object type for " + dataObjectInfo.getUri());
        }
        super.setFullyQualifiedName(
                FullyQualifiedNameTriplet.TYPE_REPLACE_FIRST_GID_NAME,
                FullyQualifiedNameTriplet.FORMAT_CHARSTR,
                dataObjectInfo.getUri());
    }

    /**
     * {@inheritDoc}
     */
    protected void writeStart(OutputStream os) throws IOException {
        super.writeStart(os);
        
        // create object data from data object
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        dataObj.write(bos);
        this.objectData = bos.toByteArray();

        // Set the total record length
        byte[] len = BinaryUtils.convert(16 + getTripletDataLength(), 2);
        byte[] data = new byte[] {
            0x5A, // Structured field identifier
            len[0], // Length byte 1
            len[1], // Length byte 2
            (byte)0xD3, // Structured field id byte 1
            (byte)0xA8, // Structured field id byte 2
            (byte)0x92, // Structured field id byte 3
            0x00, // Flags
            0x00, // Reserved
            0x00, // Reserved
            nameBytes[0],
            nameBytes[1],
            nameBytes[2],
            nameBytes[3],
            nameBytes[4],
            nameBytes[5],
            nameBytes[6],
            nameBytes[7]
        };
        os.write(data);
    }
    
    /**
     * {@inheritDoc}
     */
    protected void writeContent(OutputStream os) throws IOException {
        super.writeContent(os);
        
        // write out object data in chunks of object container data
        for (int i = 0; i <= objectData.length; i += ObjectContainerData.MAX_DATA_LEN) {
            ObjectContainerData objectContainerData = new ObjectContainerData(objectData, i);
            objectContainerData.write(os);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected void writeEnd(OutputStream os) throws IOException {
        byte[] data = new byte[] {
           0x5A, // Structured field identifier
           0x00, // Length byte 1
           0x10, // Length byte 2
           (byte)0xD3, // Structured field id byte 1
           (byte)0xA9, // Structured field id byte 2
           (byte)0x92, // Structured field id byte 3
           0x00, // Flags
           0x00, // Reserved
           0x00, // Reserved
           nameBytes[0],            
           nameBytes[1],
           nameBytes[2],
           nameBytes[3],
           nameBytes[4],
           nameBytes[5],
           nameBytes[6],
           nameBytes[7],
        };
        os.write(data);
    }
    
    /**
     * An Object Container Data holds a chunk of the data object
     */
    private class ObjectContainerData extends AbstractPreparedAFPObject {
        
        /**
         * The maximum object container data length
         */ 
        private static final int MAX_DATA_LEN = 32759;
        
        /**
         * Main constructor
         * @param objData the object data
         */
        public ObjectContainerData(byte[] objData, int startIndex) {
            int dataLen = MAX_DATA_LEN;
            if (startIndex + MAX_DATA_LEN >= objData.length) {
                dataLen = objData.length - startIndex - 1;
            }
            byte[] len = BinaryUtils.convert(8 + dataLen, 2);
            byte[] data = new byte[9 + dataLen];
            data[0] = 0x5A; // Structured field identifier 
            data[1] = len[0]; // Length byte 1
            data[2] = len[1]; // Length byte 2
            data[3] = (byte)0xD3; // Structured field id byte 1
            data[4] = (byte)0xEE; // Structured field id byte 2
            data[5] = (byte)0x92; // Structured field id byte 3
            data[6] = 0x00; // Flags
            data[7] = 0x00; // Reserved
            data[8] = 0x00; // Reserved
            
            // copy object data chunk
            System.arraycopy(objData, startIndex, data, 9, dataLen);
            
            super.setData(data);
        }
        
        /**
         * {@inheritDoc}
         */
        public String toString() {
            return "ObjectContainerData("
                + (data != null ? "" + (data.length - 2) : "null")
                + ")";
        }
    }
}
