/*-- $Id$ -- 

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================
 
    Copyright (C) 1999 The Apache Software Foundation. All rights reserved.
 
 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:
 
 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.
 
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
 
 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.
 
 4. The names "Fop" and  "Apache Software Foundation"  must not be used to
    endorse  or promote  products derived  from this  software without  prior
    written permission. For written permission, please contact
    apache@apache.org.
 
 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.
 
 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
 Software Foundation, please see <http://www.apache.org/>.
 
 */
package org.apache.fop.dom.svg;

import org.apache.fop.fo.Property;

import java.util.*;

import org.w3c.dom.svg.*;

/**
 * a length quantity in SVG
 */
public class SVGLengthImpl implements SVGLength {
	short unitType = SVG_LENGTHTYPE_UNKNOWN;
	protected float millipoints = 0;
	protected float fontsize = 12; //??

	public short getUnitType( )
	{
		return unitType;
	}

	public float getValue( )
	{
		return millipoints;
	}

	public void setValue( float value )
	{
		millipoints = value;
	}

	public float getValueInSpecifiedUnits()
	{
		return 0;
	}

	public void setValueInSpecifiedUnits(float valueInSpecifiedUnits)
	{
	}

	public String getValueAsString()
	{
		return null;
	}

	public void setValueAsString( String valueAsString )
	{
		convert(valueAsString);
	}

	public float getAnimatedValue( )
	{
		return 0;
	}

	public void newValueSpecifiedUnits(short unitType, float valueInSpecifiedUnits) throws SVGException
	{
	}

	public void convertToSpecifiedUnits(short unitType) throws SVGException
	{
	}

	/**
	* set the length given a particular String specifying length and units
	*/
	public SVGLengthImpl (String len)
	{
		convert(len);
	}

	public SVGLengthImpl()
	{
	}

	/**
	* set the length given a particular String specifying length and units,
	* and the font-size (necessary for an em)
	*
	public SVGLengthImpl(String len/*, int fontsize*)
	{
//		this.fontsize = fontsize;
		convert(len);
	}*/

	protected void convert(String len)
	{
		int l = len.length();

		if (l == 0) {
			System.err.println("WARNING: empty length");
			this.millipoints = 0;
		} else {
			float dvalue = getFloatValue(len, l);
			this.millipoints = dvalue;
		}
	}

	protected float getFloatValue(String len, int l)
	{
		int assumed_resolution = 1; // points/pixel

		float dvalue;
		try {
			if(len.endsWith("in")) {
				dvalue = Float.valueOf(len.substring(0,(l-2))).floatValue();
				dvalue = dvalue * 72;
				unitType = SVG_LENGTHTYPE_IN;
			} else if(len.endsWith("cm")) {
				dvalue = Float.valueOf(len.substring(0,(l-2))).floatValue();
				dvalue = dvalue * 28.35f;
				unitType = SVG_LENGTHTYPE_CM;
			} else if(len.endsWith("mm")) {
				dvalue = Float.valueOf(len.substring(0,(l-2))).floatValue();
				dvalue = dvalue * 2.84f;
				unitType = SVG_LENGTHTYPE_MM;
			} else if(len.endsWith("pt")) {
				dvalue = Float.valueOf(len.substring(0,(l-2))).floatValue();
//				dvalue = dvalue;
				unitType = SVG_LENGTHTYPE_PT;
			} else if(len.endsWith("pc")) {
				dvalue = Float.valueOf(len.substring(0,(l-2))).floatValue();
				dvalue = dvalue * 12;
				unitType = SVG_LENGTHTYPE_PC;
			} else if(len.endsWith("em")) {
				dvalue = Float.valueOf(len.substring(0,(l-2))).floatValue();
				dvalue = dvalue * fontsize;
				unitType = SVG_LENGTHTYPE_EMS;
			} else if(len.endsWith("px")) {
				dvalue = Float.valueOf(len.substring(0,(l-2))).floatValue();
				dvalue = dvalue * assumed_resolution;
				unitType = SVG_LENGTHTYPE_PX;
			} else if(len.endsWith("%")) {
				dvalue = Float.valueOf(len.substring(0,(l-1))).floatValue() / 100f;
				unitType = SVG_LENGTHTYPE_PERCENTAGE;
			} else {
				dvalue = Float.valueOf(len).floatValue();
				unitType = SVG_LENGTHTYPE_NUMBER;
			}
		} catch (Exception e) {
			dvalue = 0;
			unitType = SVG_LENGTHTYPE_UNKNOWN;
			System.err.println("ERROR: unknown length units in " + len);
		}
		return dvalue;
	}

	public String toString()
	{
		String s = millipoints + "mpt";
		return s;
	}
}
