/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.area;

import org.apache.fop.datatypes.ColorType;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.layout.FontState;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

// properties should be serialized by the holder
/**
 * Area traits used for rendering.
 * This class represents an area trait that specifies a value for rendering.
 */
public class Trait implements Serializable {
    /**
     * Id reference line, not resolved.
     * not sure if this is needed.
     */
    public static final Integer ID_LINK = new Integer(0);

    /**
     * Internal link trait.
     * This is resolved and provides a link to an internal area.
     */
    public static final Integer INTERNAL_LINK = new Integer(1); //resolved

    /**
     * External link. A URL link to an external resource.
     */
    public static final Integer EXTERNAL_LINK = new Integer(2);

    /**
     * The font name from the font setup.
     */
    public static final Integer FONT_NAME = new Integer(3);

    /**
     * Font size for the current font.
     */
    public static final Integer FONT_SIZE = new Integer(4);

    /**
     * The current colour.
     */
    public static final Integer COLOR = new Integer(7);

    /**
     * Don't think this is necessary.
     */
    public static final Integer ID_AREA = new Integer(8);

    /**
     * Background trait for an area.
     */
    public static final Integer BACKGROUND = new Integer(9);

    /**
     * Underline trait used when rendering inline parent.
     */
    public static final Integer UNDERLINE = new Integer(10);

    /**
     * Overline trait used when rendering inline parent.
     */
    public static final Integer OVERLINE = new Integer(11);

    /**
     * Linethrough trait used when rendering inline parent.
     */
    public static final Integer LINETHROUGH = new Integer(12);

    /**
     *
     */
    public static final Integer OFFSET = new Integer(13);

    /**
     * The shadow for text.
     */
    public static final Integer SHADOW = new Integer(14);

    /**
     * The border start.
     */
    public static final Integer BORDER_START = new Integer(15);
    public static final Integer BORDER_END = new Integer(16);
    public static final Integer BORDER_BEFORE = new Integer(17);
    public static final Integer BORDER_AFTER = new Integer(18);
    public static final Integer PADDING_START = new Integer(19);
    public static final Integer PADDING_END = new Integer(20);
    public static final Integer PADDING_BEFORE = new Integer(21);
    public static final Integer PADDING_AFTER = new Integer(22);

    static HashMap shmTraitInfo;

    private static class TraitInfo {
        String sName;
        Class sClass; // Class of trait data
        TraitInfo(String sName, Class sClass) {
            this.sName = sName;
            this.sClass = sClass;
        }
    }

    static {
        // Create a hashmap mapping trait code to name for external representation
        shmTraitInfo = new HashMap();
        shmTraitInfo.put(ID_LINK, new TraitInfo("id-link", String.class));
        shmTraitInfo.put(INTERNAL_LINK,
                          new TraitInfo("internal-link", PageViewport.class));
        shmTraitInfo.put(EXTERNAL_LINK,
                          new TraitInfo("external-link", String.class));
        shmTraitInfo.put(FONT_NAME,
                          new TraitInfo("font-family", String.class));
        shmTraitInfo.put(FONT_SIZE,
                          new TraitInfo("font-size", Integer.class));
        shmTraitInfo.put(COLOR, new TraitInfo("color", String.class));
        shmTraitInfo.put(ID_AREA, new TraitInfo("id-area", String.class));
        shmTraitInfo.put(BACKGROUND,
                          new TraitInfo("background", Background.class));
        shmTraitInfo.put(UNDERLINE,
                          new TraitInfo("underline", Boolean.class));
        shmTraitInfo.put(OVERLINE,
                          new TraitInfo("overline", Boolean.class));
        shmTraitInfo.put(LINETHROUGH,
                          new TraitInfo("linethrough", Boolean.class));
        shmTraitInfo.put(OFFSET, new TraitInfo("offset", Integer.class));
        shmTraitInfo.put(SHADOW, new TraitInfo("shadow", Integer.class));
        shmTraitInfo.put(BORDER_START,
                          new TraitInfo("border-start", BorderProps.class));
        shmTraitInfo.put(BORDER_END,
                          new TraitInfo("border-end", BorderProps.class));
        shmTraitInfo.put(BORDER_BEFORE,
                          new TraitInfo("border-before", BorderProps.class));
        shmTraitInfo.put(BORDER_AFTER,
                          new TraitInfo("border-after", BorderProps.class));
        shmTraitInfo.put(PADDING_START,
                          new TraitInfo("padding-start", Integer.class));
        shmTraitInfo.put(PADDING_END,
                          new TraitInfo("padding-end", Integer.class));
        shmTraitInfo.put(PADDING_BEFORE,
                          new TraitInfo("padding-before", Integer.class));
        shmTraitInfo.put(PADDING_AFTER,
                          new TraitInfo("padding-after", Integer.class));
    }

    public static String getTraitName(Object traitCode) {
        Object obj = shmTraitInfo.get(traitCode);
        if (obj != null) {
            return ((TraitInfo) obj).sName;
        } else {
            return "unknown-trait-" + traitCode.toString();
        }
    }

    public static Object getTraitCode(String sTraitName) {
        Iterator iter = shmTraitInfo.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            TraitInfo ti = (TraitInfo) entry.getValue();
            if (ti != null && ti.sName.equals(sTraitName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static Class getTraitClass(Object oTraitCode) {
        TraitInfo ti = (TraitInfo) shmTraitInfo.get(oTraitCode);
        return (ti != null ? ti.sClass : null);
    }

    public Object propType;
    public Object data;

    public Trait() {
        this.propType = null;
        this.data = null;
    }

    public Trait(Object propType, Object data) {
        this.propType = propType;
        this.data = data;
    }

    public String toString() {
        return data.toString();
    }

    public static Object makeTraitValue(Object oCode, String sTraitValue) {
        // Get the code from the name
        // See what type of object it is
        // Convert string value to an object of that type
        Class tclass = getTraitClass(oCode);
        if (tclass == null)
            return null;
        if (tclass.equals(String.class)) {
            return sTraitValue;
        }
        if (tclass.equals(Integer.class)) {
            return new Integer(sTraitValue);
        }
        // See if the class has a constructor from string or can read from a string
        try {
            Object o = tclass.newInstance();
            //return o.fromString(sTraitValue);
        } catch (IllegalAccessException e1) {
            System.err.println("Can't create instance of " +
                               tclass.getName());
            return null;
        }
        catch (InstantiationException e2) {
            System.err.println("Can't create instance of " +
                               tclass.getName());
            return null;
        }


        return null;
    }

    public static class Background {
        public ColorType color = null;
        public String url = null;
        public int repeat;
        public int horiz;
        public int vertical;
    }

}

