/*
 * $Id: CondLengthProperty.java,v 1.5 2003/03/05 21:48:01 jeremias Exp $
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 *
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package org.apache.fop.fo;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.CondLength;
import org.apache.fop.datatypes.Length;

/**
 * Superclass for properties that have conditional lengths
 */
public class CondLengthProperty extends Property {

    /**
     * Inner class for creating instances of CondLengthProperty
     */
    public static class Maker extends CompoundPropertyMaker {

        /**
         * @param name of property for which a Maker should be created
         */
        public Maker(int propId) {
            super(propId);
        }

        /**
         * Create a new empty instance of CondLengthProperty.
         * @return the new instance. 
         */
        public Property makeNewProperty() {
            return new CondLengthProperty(new CondLength());
        }

        /**
         * @see CompoundPropertyMaker#convertProperty
         */        
        public Property convertProperty(Property p, PropertyList propertyList, FObj fo)
            throws FOPException
        {
            if (p instanceof KeepProperty) {
                return p;
            }
            return super.convertProperty(p, propertyList, fo);
        }
    }

    private CondLength condLength = null;

    /**
     * @param condLength conditional length object which is to be wrapped in
     * this property
     */
    public CondLengthProperty(CondLength condLength) {
        this.condLength = condLength;
    }

    /**
     * @return this.condLength
     */
    public CondLength getCondLength() {
        return this.condLength;
    }

    /**
     * TODO: Should we allow this?
     * @return this.condLength cast as a Length
     */
    public Length getLength() {
        return this.condLength.getLength().getLength();
    }

    /**
     * @return this.condLength cast as an Object
     */
    public Object getObject() {
        return this.condLength;
    }

}
