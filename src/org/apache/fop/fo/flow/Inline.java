/* $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.*;
import org.apache.fop.layout.Area;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.properties.*;

// Java
import java.util.Enumeration;

public class Inline extends FObjMixed {

  public static class Maker extends FObj.Maker {
    public FObj make(FObj parent, PropertyList propertyList)
        throws FOPException { 
      return new Inline(parent, propertyList);
    }
  }

  public static FObj.Maker maker() {
      return new Inline.Maker();
  }

  // Textdecoration
  protected boolean underlined = false;
  protected boolean overlined = false;
  protected boolean lineThrough = false;

    
  public Inline(FObj parent, PropertyList propertyList)
      throws FOPException {
    super(parent, propertyList);
    this.name = "fo:inline";

    int textDecoration =
      this.properties.get("text-decoration").getEnum();

    if (textDecoration == TextDecoration.UNDERLINE) {
        this.underlined = true;
    }

    if (textDecoration == TextDecoration.OVERLINE) {
        this.overlined = true;
    }

    if (textDecoration == TextDecoration.LINE_THROUGH) {
        this.lineThrough = true;
    }

    if (parent.getName().equals("fo:flow")) {
      throw new FOPException("fo:inline can't be directly"
                   + " under flow"); 
    }

  }

  protected void addCharacters(char data[], int start, int length) { 
      FOText ft = new FOText(data,start,length, this);
      ft.setUnderlined(underlined);
      ft.setOverlined(overlined);
      ft.setLineThrough(lineThrough);
      children.addElement(ft);
  }

}
