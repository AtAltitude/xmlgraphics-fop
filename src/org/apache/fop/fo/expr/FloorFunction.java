/*
 * $Id$
 * Copyright (C) 2001-2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.expr;

import org.apache.fop.fo.Property;
import org.apache.fop.fo.NumberProperty;


class FloorFunction extends FunctionBase {

    public int nbArgs() {
        return 1;
    }

    public Property eval(Property[] args,
                         PropertyInfo pInfo) throws PropertyException {
        Number dbl = args[0].getNumber();
        if (dbl == null) {
            throw new PropertyException("Non number operand to floor function");
        }
        return new NumberProperty(Math.floor(dbl.doubleValue()));
    }

}

