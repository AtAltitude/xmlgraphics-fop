/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.area.inline;

import org.apache.fop.render.Renderer;

import java.util.List;
import java.util.ArrayList;

public class Container extends InlineArea {
    ArrayList blocks = new ArrayList();

    // this is an inline area that can have blocks as children

    public void render(Renderer renderer) {
        renderer.renderContainer(this);
    }

    public List getBlocks() {
        return blocks;
    }

}
