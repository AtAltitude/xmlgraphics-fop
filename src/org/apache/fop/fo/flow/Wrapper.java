/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.properties.*;
import org.apache.fop.layout.*;

/**
 * Implementation for fo:wrapper formatting object.
 * The wrapper object serves as
 * a property holder for it's children objects.
 *
 * Content: (#PCDATA|%inline;|%block;)*
 * Properties: id
 */
public class Wrapper extends FObjMixed {

    public Wrapper(FObj parent) {
        super(parent);
        // check that this occurs inside an fo:flow
    }

    protected void addCharacters(char data[], int start, int length) {
        FOText ft = new FOText(data, start, length, this);
        ft.setLogger(log);
        children.addElement(ft);
    }

}
