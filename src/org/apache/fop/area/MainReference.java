/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.area;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// the area that contains the flow via the span areas
public class MainReference implements Serializable {
    List spanAreas = new ArrayList();
    int columnGap;
    int width;

    public void addSpan(Span span) {
        spanAreas.add(span);
    }

    public List getSpans() {
        return spanAreas;
    }

    public int getColumnGap() {
        return columnGap;
    }

    public int getWidth() {
        return width;
    }
}
