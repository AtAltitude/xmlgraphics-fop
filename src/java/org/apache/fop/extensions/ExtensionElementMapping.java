/*
 * $Id: ExtensionElementMapping.java,v 1.10 2003/03/05 20:40:18 jeremias Exp $
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
package org.apache.fop.extensions;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FOTreeBuilder;

import java.util.HashMap;

/**
 * Element mapping for the pdf bookmark extension.
 * This sets up the mapping for the classes that handle the
 * pdf bookmark extension.
 */
public class ExtensionElementMapping implements ElementMapping {
    /**
     * The pdf bookmark extension uri
     */
    public static final String URI = "http://xml.apache.org/fop/extensions";

    // the mappings are only setup once and resued after that
    private static HashMap foObjs = null;

    private static synchronized void setupExt() {
        if (foObjs == null) {
            foObjs = new HashMap();
            foObjs.put("bookmarks", new B());
            foObjs.put("outline", new O());
            foObjs.put("label", new L());
        }
    }

    /**
     * Add the mappings to the fo tree builder.
     *
     * @param builder the fo tree builder to add the mappings
     */
    public void addToBuilder(FOTreeBuilder builder) {
        if (foObjs == null) {
            setupExt();
        }
        builder.addMapping(URI, foObjs);
    }

    static class B extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new Bookmarks(parent);
        }
    }

    static class O extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new Outline(parent);
        }
    }

    static class L extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new Label(parent);
        }
    }
}
