<!-- 
This files writes the class files for the Adobe Type 1 fonts.
It uses the information in the font description files (Courier.xml, Helvetica.xml) to this
In these font description files each character is referenced by its adobe name:
      <char name="A" width="667"/>
To resolve this name and to find the code for this character it looks up the adobe name in the
file charlist.xml and extracts the WinAnsi code.
-->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:lxslt="http://xml.apache.org/xslt"
                xmlns:redirect="org.apache.xalan.xslt.extensions.Redirect"
                extension-element-prefixes="redirect">
<xsl:output method="text" />

<xsl:template match="font-metrics">
<xsl:variable name="class-name" select="class-name"/>
<!--<redirect:write select="concat('org/apache/fop/render/pdf/fonts/', $class-name, '.java')">-->
package org.apache.fop.render.pdf.fonts;

import org.apache.fop.render.pdf.Font;
import org.apache.fop.layout.FontDescriptor;

public class <xsl:value-of select="class-name"/> extends Font implements FontDescriptor {
    private final static String fontName = "<xsl:value-of select="font-name"/>";
    private final static String encoding = "<xsl:value-of select="encoding"/>";
    private final static int capHeight = <xsl:value-of select="cap-height"/>;
    private final static int xHeight = <xsl:value-of select="x-height"/>;
    private final static int ascender = <xsl:value-of select="ascender"/>;
    private final static int descender = <xsl:value-of select="descender"/>;
    private final static int[] fontBBox = {
        <xsl:value-of select="bbox/left"/>,
        <xsl:value-of select="bbox/bottom"/>,
        <xsl:value-of select="bbox/right"/>,
        <xsl:value-of select="bbox/top"/>
    };
    private final static int flags = <xsl:value-of select="flags"/>;
    private final static int stemV = <xsl:value-of select="stemv"/>;
    private final static int italicAngle = <xsl:value-of select="italicangle"/>;
    private final static int firstChar = <xsl:value-of select="first-char"/>;
    private final static int lastChar = <xsl:value-of select="last-char"/>;
    private final static int[] width;

    static {
        width = new int[256];
<xsl:for-each select="widths/char"><xsl:variable name="char-name" select="@name"/><xsl:variable name="char-num" select="document('charlist.xml')/font-mappings/map[@adobe-name=$char-name]/@win-ansi"/><xsl:if test="$char-num!='-1'">        width[<xsl:value-of select="$char-num"/>] = <xsl:value-of select="@width"/>;
</xsl:if></xsl:for-each>
    }

    public String encoding() {
        return encoding;
    }
    
    public String fontName() {
        return fontName;
    }

    public int getAscender() {
        return ascender;
    }

    public int getCapHeight() {
        return capHeight;
    }

    public int getDescender() {
        return descender;
    }

    public int getXHeight() {
        return xHeight;
    }

    public int getFlags() {
        return flags;
    }

    public int[] getFontBBox() {
        return fontBBox;
    }

    public int getItalicAngle() {
        return italicAngle;
    }

    public int getStemV() {
        return stemV;
    }

    public int getFirstChar() {
        return firstChar;
    }

    public int getLastChar() {
        return lastChar;
    }

    public int width(int i) {
        return width[i];
    }

    public int[] getWidths() {
        int[] arr = new int[getLastChar()-getFirstChar()+1];
        System.arraycopy(width, getFirstChar(), arr, 0, getLastChar()-getFirstChar()+1);
        return arr;
    }
}
<!--</redirect:write>-->
</xsl:template>
</xsl:stylesheet>

