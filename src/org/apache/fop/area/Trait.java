/*
 * $Id$
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
package org.apache.fop.area;

import org.apache.fop.datatypes.ColorType;
import org.apache.fop.traits.BorderProps;

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
     * Shadow offset.
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

    /**
     * The border end.
     */
    public static final Integer BORDER_END = new Integer(16);

    /**
     * The border before.
     */
    public static final Integer BORDER_BEFORE = new Integer(17);

    /**
     * The border after.
     */
    public static final Integer BORDER_AFTER = new Integer(18);

    /**
     * The padding start.
     */
    public static final Integer PADDING_START = new Integer(19);

    /**
     * The padding end.
     */
    public static final Integer PADDING_END = new Integer(20);

    /**
     * The padding before.
     */
    public static final Integer PADDING_BEFORE = new Integer(21);

    /**
     * The padding after.
     */
    public static final Integer PADDING_AFTER = new Integer(22);

    private static final Map TRAIT_INFO = new HashMap();

    private static class TraitInfo {
        private String name;
        private Class clazz; // Class of trait data
        
        public TraitInfo(String name, Class clazz) {
            this.name = name;
            this.clazz = clazz;
        }
        
        public String getName() {
            return this.name;
        }
        
        public Class getClazz() {
            return this.clazz;
        }
    }

    static {
        // Create a hashmap mapping trait code to name for external representation
        TRAIT_INFO.put(ID_LINK, new TraitInfo("id-link", String.class));
        TRAIT_INFO.put(INTERNAL_LINK,
                          new TraitInfo("internal-link", String.class));
        TRAIT_INFO.put(EXTERNAL_LINK,
                          new TraitInfo("external-link", String.class));
        TRAIT_INFO.put(FONT_NAME,
                          new TraitInfo("font-family", String.class));
        TRAIT_INFO.put(FONT_SIZE,
                          new TraitInfo("font-size", Integer.class));
        TRAIT_INFO.put(COLOR, new TraitInfo("color", String.class));
        TRAIT_INFO.put(ID_AREA, new TraitInfo("id-area", String.class));
        TRAIT_INFO.put(BACKGROUND,
                          new TraitInfo("background", Background.class));
        TRAIT_INFO.put(UNDERLINE,
                          new TraitInfo("underline", Boolean.class));
        TRAIT_INFO.put(OVERLINE,
                          new TraitInfo("overline", Boolean.class));
        TRAIT_INFO.put(LINETHROUGH,
                          new TraitInfo("linethrough", Boolean.class));
        TRAIT_INFO.put(OFFSET, new TraitInfo("offset", Integer.class));
        TRAIT_INFO.put(SHADOW, new TraitInfo("shadow", Integer.class));
        TRAIT_INFO.put(BORDER_START,
                          new TraitInfo("border-start", BorderProps.class));
        TRAIT_INFO.put(BORDER_END,
                          new TraitInfo("border-end", BorderProps.class));
        TRAIT_INFO.put(BORDER_BEFORE,
                          new TraitInfo("border-before", BorderProps.class));
        TRAIT_INFO.put(BORDER_AFTER,
                          new TraitInfo("border-after", BorderProps.class));
        TRAIT_INFO.put(PADDING_START,
                          new TraitInfo("padding-start", Integer.class));
        TRAIT_INFO.put(PADDING_END,
                          new TraitInfo("padding-end", Integer.class));
        TRAIT_INFO.put(PADDING_BEFORE,
                          new TraitInfo("padding-before", Integer.class));
        TRAIT_INFO.put(PADDING_AFTER,
                          new TraitInfo("padding-after", Integer.class));
    }

    /**
     * Get the trait name for a trait code.
     *
     * @param traitCode the trait code to get the name for
     * @return the trait name
     */
    public static String getTraitName(Object traitCode) {
        Object obj = TRAIT_INFO.get(traitCode);
        if (obj != null) {
            return ((TraitInfo) obj).getName();
        } else {
            return "unknown-trait-" + traitCode.toString();
        }
    }

    /**
     * Get the trait code for a trait name.
     *
     * @param sTraitName the name of the trait to find
     * @return the trait code object
     */
    public static Object getTraitCode(String sTraitName) {
        Iterator iter = TRAIT_INFO.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            TraitInfo ti = (TraitInfo) entry.getValue();
            if (ti != null && ti.getName().equals(sTraitName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Get the data storage class for the trait.
     *
     * @param oTraitCode the trait code to lookup
     * @return the class type for the trait
     */
    private static Class getTraitClass(Object oTraitCode) {
        TraitInfo ti = (TraitInfo) TRAIT_INFO.get(oTraitCode);
        return (ti != null ? ti.getClazz() : null);
    }

    /**
     * The type of trait for an area.
     */
    private Object propType;

    /**
     * The data value of the trait.
     */
    private Object data;

    /**
     * Create a new empty trait.
     */
    public Trait() {
        this.propType = null;
        this.data = null;
    }

    /**
     * Create a trait with the value and type.
     *
     * @param propType the type of trait
     * @param data the data value
     */
    public Trait(Object propType, Object data) {
        this.propType = propType;
        this.data = data;
    }

    /**
     * Returns the trait data value.
     * @return the trait data value
     */
    public Object getData() {
        return this.data;
    }

    /**
     * Returns the property type.
     * @return the property type
     */
    public Object getPropType() {
        return this.propType;
    }

    /**
     * Return the string for debugging.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return data.toString();
    }

    /**
     * Make a trait value.
     *
     * @param oCode trait code
     * @param sTraitValue trait value as String
     * @return the trait value as object
     */
    public static Object makeTraitValue(Object oCode, String sTraitValue) {
        // Get the code from the name
        // See what type of object it is
        // Convert string value to an object of that type
        Class tclass = getTraitClass(oCode);
        if (tclass == null) {
            return null;
        }
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
            System.err.println("Can't create instance of " 
                               + tclass.getName());
            return null;
        } catch (InstantiationException e2) {
            System.err.println("Can't create instance of " 
                               + tclass.getName());
            return null;
        }


        return null;
    }

    /**
     * Background trait structure.
     * Used for storing back trait information which are related.
     */
    public static class Background implements Serializable {
        
        /** The background color if any. */
        private ColorType color = null;

        /** The background image url if any. */
        private String url = null;

        /** Background repeat enum for images. */
        private int repeat;

        /** Background horizontal offset for images. */
        private int horiz;

        /** Background vertical offset for images. */
        private int vertical;
        
        /**
         * Returns the background color.
         * @return background color, null if n/a
         */
        public ColorType getColor() {
            return color;
        }

        /**
         * Returns the horizontal offset for images.
         * @return the horizontal offset
         */
        public int getHoriz() {
            return horiz;
        }

        /**
         * Returns the image repetition behaviour for images.
         * @return the image repetition behaviour
         */
        public int getRepeat() {
            return repeat;
        }

        /**
         * Returns the URL to the background image
         * @return URL to the background image, null if n/a
         */
        public String getURL() {
            return url;
        }

        /**
         * Returns the vertical offset for images.
         * @return the vertical offset
         */
        public int getVertical() {
            return vertical;
        }

        /**
         * Sets the color.
         * @param color The color to set
         */
        public void setColor(ColorType color) {
            this.color = color;
        }

        /**
         * Sets the horizontal offset.
         * @param horiz The horizontal offset to set
         */
        public void setHoriz(int horiz) {
            this.horiz = horiz;
        }

        /**
         * Sets the image repetition behaviour for images.
         * @param repeat The image repetition behaviour to set
         */
        public void setRepeat(int repeat) {
            this.repeat = repeat;
        }

        /**
         * Sets the URL to the background image.
         * @param url The URL to set
         */
        public void setURL(String url) {
            this.url = url;
        }

        /**
         * Sets the vertical offset for images.
         * @param vertical The vertical offset to set
         */
        public void setVertical(int vertical) {
            this.vertical = vertical;
        }

    }

}

