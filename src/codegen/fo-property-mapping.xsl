<!--
$Id$
============================================================================
                   The Apache Software License, Version 1.1
============================================================================

Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without modifica-
tion, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. The end-user documentation included with the redistribution, if any, must
   include the following acknowledgment: "This product includes software
   developed by the Apache Software Foundation (http://www.apache.org/)."
   Alternately, this acknowledgment may appear in the software itself, if
   and wherever such third-party acknowledgments normally appear.

4. The names "FOP" and "Apache Software Foundation" must not be used to
   endorse or promote products derived from this software without prior
   written permission. For written permission, please contact
   apache@apache.org.

5. Products derived from this software may not be called "Apache", nor may
   "Apache" appear in their name, without prior written permission of the
   Apache Software Foundation.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
============================================================================

This software consists of voluntary contributions made by many individuals
on behalf of the Apache Software Foundation and was originally created by
James Tauber <jtauber@jtauber.com>. For more information on the Apache
Software Foundation, please see <http://www.apache.org/>.
--> 
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:lxslt="http://xml.apache.org/xslt">

<xsl:include href="propinc.xsl"/>

<xsl:output method="text" />


<xsl:template name="genmaker">
  <xsl:param name="prop" select="."/>
  <xsl:param name="htname"/>

  <xsl:variable name="makerclass">
   <xsl:choose>
    <xsl:when test="$prop/use-generic and count($prop/*)=2">
     <xsl:value-of select="$prop/use-generic"/>
    </xsl:when>
    <xsl:when test="$prop/class-name">
     <xsl:value-of select="$prop/class-name"/><xsl:text>Maker</xsl:text>
    </xsl:when>
    <xsl:otherwise> <!-- make from name -->
      <xsl:call-template name="makeClassName">
        <xsl:with-param name="propstr" select="$prop/name"/>
      </xsl:call-template><xsl:text>Maker</xsl:text>
    </xsl:otherwise>
   </xsl:choose>
  </xsl:variable>
  <xsl:variable name="lcletters" select="'abcdefghijklmnopqrstuvwxyz-:'" />
  <xsl:variable name="ucletters" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ__'" />
  <xsl:variable name="enum" select="translate($prop/name, $lcletters, $ucletters)"/>
<xsl:text>    </xsl:text><xsl:value-of select="$htname"/>[PR_<xsl:value-of select="$enum"/>] =<xsl:value-of select="$makerclass"/>.maker("<xsl:value-of select="$prop/name"/>");
<xsl:text>    addPropertyName("</xsl:text><xsl:value-of select="$prop/name"/>", PR_<xsl:value-of select="$enum"/>);
</xsl:template>


<xsl:template name="genenum">
  <xsl:param name="prop" select="."/>
  <xsl:variable name="lcletters" select="'abcdefghijklmnopqrstuvwxyz-:'" />
  <xsl:variable name="ucletters" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ__'" />
  <xsl:variable name="num" select="count(preceding-sibling::property)"/>
  <xsl:variable name="enum" select="translate($prop/name, $lcletters, $ucletters)"/>
<!--
<xsl:text>  public final static short </xsl:text><xsl:value-of select="$enum"/> = <xsl:value-of select="$num"/>;
-->
</xsl:template>


<xsl:template match="text()"/>
<xsl:template match="text()" mode="enums"/>

<xsl:template match="property-list">
package org.apache.fop.fo.properties;

import java.util.HashMap;
import java.util.Set;
import org.apache.fop.fo.Property;
//import org.apache.fop.svg.*;

public class <xsl:value-of select="@family"/>PropertyMapping implements Constants {

  private static Property.Maker[] s_htGeneric = new Property.Maker[PROPERTY_COUNT+1];
  private static HashMap s_htElementStringLists = new HashMap();    // temporary
  private static HashMap s_htElementLists = new HashMap();
  private static HashMap s_htSubPropNames = new HashMap();
  private static HashMap s_htPropNames = new HashMap();
  private static HashMap s_htPropIds = new HashMap();
  <xsl:for-each select="element-property-list">
  private static HashMap s_ht<xsl:value-of select="localname[1]"/>;</xsl:for-each>

  <xsl:apply-templates/>

  public static Set getElementStringMappings() { // temporary
    return s_htElementStringLists.keySet();
  }

  public static HashMap getElementStringMapping(String elemName) {  // temporary
    return (HashMap) s_htElementStringLists.get(elemName);
  }

  public static Property.Maker[] getGenericMappings() {
    return s_htGeneric;
  }

  public static Set getElementMappings() {
    return s_htElementLists.keySet();
  }

  public static Property.Maker[] getElementMapping(int elemName) {
    return (Property.Maker[])s_htElementLists.get(new Integer(elemName));
  }

  public static int getPropertyId(String name) {
  	Integer i = (Integer) s_htPropNames.get(name);
  	if (i == null)
  		return -1;
    return i.intValue();
  }

  public static int getSubPropertyId(String name) {
  	Integer i = (Integer) s_htSubPropNames.get(name);
  	if (i == null)
  		return -1;
    return i.intValue();
  }
  
  public static String getPropertyName(int id) {
    return (String) s_htPropIds.get(new Integer(id));
  }

  static {
    addSubPropertyName("length", CP_LENGTH);
    addSubPropertyName("conditionality", CP_CONDITIONALITY);
    addSubPropertyName("block-progression-direction", CP_BLOCK_PROGRESSION_DIRECTION);
    addSubPropertyName("inline-progression-direction", CP_INLINE_PROGRESSION_DIRECTION);
    addSubPropertyName("within-line", CP_WITHIN_LINE);
    addSubPropertyName("within-column", CP_WITHIN_COLUMN);
    addSubPropertyName("within-page", CP_WITHIN_PAGE);
    addSubPropertyName("minimum", CP_MINIMUM);
    addSubPropertyName("maximum", CP_MAXIMUM);
    addSubPropertyName("optimum", CP_OPTIMUM);
    addSubPropertyName("precedence", CP_PRECEDENCE);
  
  }
  
  public static void addPropertyName(String name, int id) {
    s_htPropNames.put(name, new Integer(id));
    s_htPropIds.put(new Integer(id), name);
  }

  public static void addSubPropertyName(String name, int id) {
    s_htSubPropNames.put(name, new Integer(id));
    s_htPropIds.put(new Integer(id), name);
  }
}
</xsl:template>

<xsl:template match="generic-property-list">
  <xsl:apply-templates mode="enums"/>
  static {
    // Generate the generic mapping
<xsl:apply-templates>
    <xsl:with-param name="htname" select='"s_htGeneric"'/>
  </xsl:apply-templates>
  }
</xsl:template>

<xsl:template match="element-property-list">
  <xsl:variable name="ename" select="localname[1]"/>
  static {
    s_ht<xsl:value-of select="$ename"/> = new HashMap();
   <xsl:for-each select="localname">
    s_htElementLists.put("<xsl:value-of select='.'/>", s_ht<xsl:value-of select='$ename'/>);
   </xsl:for-each>

<xsl:apply-templates>
    <xsl:with-param name='htname'>s_ht<xsl:value-of select="$ename"/></xsl:with-param>
    </xsl:apply-templates>
  }
</xsl:template>

<xsl:template match="property[@type='generic']" mode="enums">
  /* PROPCLASS = <xsl:call-template name="propclass"/> */
</xsl:template>

<xsl:template match="property" mode="enums">
  <xsl:param name="htname"/>
  <xsl:variable name="refname" select="name"/>
  <xsl:choose>
    <xsl:when test="@type='ref'">
      <xsl:call-template name="genenum">
        <xsl:with-param name="htname" select="$htname"/>
        <xsl:with-param name="prop"
          select='document(concat(@family, "properties.xml"))//property[name=$refname]'/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="not(@type)">
      <xsl:call-template name="genenum">
    <xsl:with-param name="htname" select="$htname"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise/>
  </xsl:choose>
</xsl:template>

<xsl:template match="subproperty" mode="enums">
  <xsl:call-template name="genenum"/>
</xsl:template>

<xsl:template match="property">
  <xsl:param name="htname"/>
  <xsl:variable name="refname" select="name"/>
  <xsl:choose>
    <xsl:when test="@type='ref'">
      <xsl:call-template name="genmaker">
        <xsl:with-param name="htname" select="$htname"/>
        <xsl:with-param name="prop"
          select='document(concat(@family, "properties.xml"))//property[name=$refname]'/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="not(@type)">
      <xsl:call-template name="genmaker">
    <xsl:with-param name="htname" select="$htname"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise/>
  </xsl:choose>
</xsl:template>

<xsl:template match="property[@type='generic']">
  /* PROPCLASS = <xsl:call-template name="propclass"/> */
</xsl:template>

</xsl:stylesheet>


