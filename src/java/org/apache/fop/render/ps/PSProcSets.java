/*
 * $Id: PSProcSets.java,v 1.3 2003/03/11 08:42:24 jeremias Exp $
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
package org.apache.fop.render.ps;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.Glyphs;
import org.apache.fop.layout.FontInfo;

/**
 * This class defines the basic resources (procsets) used by FOP's PostScript
 * renderer and SVG transcoder.
 * 
 * @author <a href="mailto:fop-dev@xml.apache.org">Apache XML FOP Development Team</a>
 * @author <a href="mailto:jeremias@apache.org">Jeremias Maerki</a>
 * @version $Id: PSProcSets.java,v 1.3 2003/03/11 08:42:24 jeremias Exp $
 */
public final class PSProcSets {

    /**
     * Generates a resource defining standard procset for FOP.
     * @param gen PSGenerator to use for output
     * @throws IOException In case of an I/O problem
     */
    public static final void writeFOPStdProcSet(PSGenerator gen) throws IOException {
        gen.writeln("%%BeginResource: procset (Apache FOP Std ProcSet) 1.0 0");
        gen.writeln("%%Version: 1.0 0");
        gen.writeln("%%Copyright: Copyright (C) 2001-2003 "
                    + "The Apache Software Foundation. All rights reserved.");
        gen.writeln("%%Title: Basic set of procedures used by FOP");

        gen.writeln("/bd{bind def}bind def");
        gen.writeln("/ld{load def}bd");
        gen.writeln("/M/moveto ld");
        gen.writeln("/RM/rmoveto ld");
        gen.writeln("/t/show ld");

        gen.writeln("/_ctm matrix def"); //Holds the current matrix
        gen.writeln("/_tm matrix def");
        //BT: save currentmatrix, set _tm to identitymatrix and move to 0/0
        gen.writeln("/BT { _ctm currentmatrix pop matrix _tm copy pop 0 0 moveto } bd"); 
        //ET: restore last currentmatrix
        gen.writeln("/ET { _ctm setmatrix } bd");
        gen.writeln("/iTm { _ctm setmatrix _tm concat } bd");
        gen.writeln("/Tm { _tm astore pop iTm 0 0 moveto } bd");
        
        gen.writeln("/ux 0.0 def");
        gen.writeln("/uy 0.0 def");

        // <font> <size> F
        gen.writeln("/F {");
        gen.writeln("  /Tp exch def");
        // gen.writeln("  currentdict exch get");
        gen.writeln("  /Tf exch def");
        gen.writeln("  Tf findfont Tp scalefont setfont");
        gen.writeln("  /cf Tf def  /cs Tp def  /cw ( ) stringwidth pop def");
        gen.writeln("} bd");

        gen.writeln("/ULS {currentpoint /uy exch def /ux exch def} bd");
        gen.writeln("/ULE {");
        gen.writeln("  /Tcx currentpoint pop def");
        gen.writeln("  gsave");
        gen.writeln("  newpath");
        gen.writeln("  cf findfont cs scalefont dup");
        gen.writeln("  /FontMatrix get 0 get /Ts exch def /FontInfo get dup");
        gen.writeln("  /UnderlinePosition get Ts mul /To exch def");
        gen.writeln("  /UnderlineThickness get Ts mul /Tt exch def");
        gen.writeln("  ux uy To add moveto  Tcx uy To add lineto");
        gen.writeln("  Tt setlinewidth stroke");
        gen.writeln("  grestore");
        gen.writeln("} bd");

        gen.writeln("/OLE {");
        gen.writeln("  /Tcx currentpoint pop def");
        gen.writeln("  gsave");
        gen.writeln("  newpath");
        gen.writeln("  cf findfont cs scalefont dup");
        gen.writeln("  /FontMatrix get 0 get /Ts exch def /FontInfo get dup");
        gen.writeln("  /UnderlinePosition get Ts mul /To exch def");
        gen.writeln("  /UnderlineThickness get Ts mul /Tt exch def");
        gen.writeln("  ux uy To add cs add moveto Tcx uy To add cs add lineto");
        gen.writeln("  Tt setlinewidth stroke");
        gen.writeln("  grestore");
        gen.writeln("} bd");

        gen.writeln("/SOE {");
        gen.writeln("  /Tcx currentpoint pop def");
        gen.writeln("  gsave");
        gen.writeln("  newpath");
        gen.writeln("  cf findfont cs scalefont dup");
        gen.writeln("  /FontMatrix get 0 get /Ts exch def /FontInfo get dup");
        gen.writeln("  /UnderlinePosition get Ts mul /To exch def");
        gen.writeln("  /UnderlineThickness get Ts mul /Tt exch def");
        gen.writeln("  ux uy To add cs 10 mul 26 idiv add moveto "
                    + "Tcx uy To add cs 10 mul 26 idiv add lineto");
        gen.writeln("  Tt setlinewidth stroke");
        gen.writeln("  grestore");
        gen.writeln("} bd");
        
        gen.writeln("/QUADTO {");
        gen.writeln("/Y22 exch store");
        gen.writeln("/X22 exch store");
        gen.writeln("/Y21 exch store");
        gen.writeln("/X21 exch store");
        gen.writeln("currentpoint");
        gen.writeln("/Y21 load 2 mul add 3 div exch");
        gen.writeln("/X21 load 2 mul add 3 div exch");
        gen.writeln("/X21 load 2 mul /X22 load add 3 div");
        gen.writeln("/Y21 load 2 mul /Y22 load add 3 div");
        gen.writeln("/X22 load /Y22 load curveto");
        gen.writeln("} bd");
        
        gen.writeln("%%EndResource");
    }


    /**
     * Generates a resource defining a procset for including EPS graphics.
     * @param gen PSGenerator to use for output
     * @throws IOException In case of an I/O problem
     */
    public static final void writeFOPEPSProcSet(PSGenerator gen) throws IOException {
        gen.writeln("%%BeginResource: procset (Apache FOP EPS ProcSet) 1.0 0");
        gen.writeln("%%Version: 1.0 0");
        gen.writeln("%%Copyright: Copyright (C) 2002-2003 "
                    + "The Apache Software Foundation. All rights reserved.");
        gen.writeln("%%Title: EPS procedures used by FOP");

        gen.writeln("/BeginEPSF { %def");
        gen.writeln("/b4_Inc_state save def         % Save state for cleanup");
        gen.writeln("/dict_count countdictstack def % Count objects on dict stack");
        gen.writeln("/op_count count 1 sub def      % Count objects on operand stack");
        gen.writeln("userdict begin                 % Push userdict on dict stack");
        gen.writeln("/showpage { } def              % Redefine showpage, { } = null proc");
        gen.writeln("0 setgray 0 setlinecap         % Prepare graphics state");
        gen.writeln("1 setlinewidth 0 setlinejoin");
        gen.writeln("10 setmiterlimit [ ] 0 setdash newpath");
        gen.writeln("/languagelevel where           % If level not equal to 1 then");
        gen.writeln("{pop languagelevel             % set strokeadjust and");
        gen.writeln("1 ne                           % overprint to their defaults.");
        gen.writeln("{false setstrokeadjust false setoverprint");
        gen.writeln("} if");
        gen.writeln("} if");
        gen.writeln("} bd");

        gen.writeln("/EndEPSF { %def");
        gen.writeln("count op_count sub {pop} repeat            % Clean up stacks");
        gen.writeln("countdictstack dict_count sub {end} repeat");
        gen.writeln("b4_Inc_state restore");
        gen.writeln("} bd");
        
        gen.writeln("%%EndResource");
    }

    /**
     * Generates the PostScript code for the font dictionary.
     * @param gen PostScript generator to use for output
     * @param fontInfo available fonts
     * @throws IOException in case of an I/O problem
     */
    public static void writeFontDict(PSGenerator gen, FontInfo fontInfo) 
                throws IOException {
        gen.writeln("%%BeginResource: procset FOPFonts");
        gen.writeln("%%Title: Font setup (shortcuts) for this file");
        gen.writeln("/FOPFonts 100 dict dup begin");

        // write("/gfF1{/Helvetica findfont} bd");
        // write("/gfF3{/Helvetica-Bold findfont} bd");
        Map fonts = fontInfo.getFonts();
        Iterator enum = fonts.keySet().iterator();
        while (enum.hasNext()) {
            String key = (String)enum.next();
            Font fm = (Font)fonts.get(key);
            gen.writeln("/" + key + " /" + fm.getFontName() + " def");
        }
        gen.writeln("end def");
        gen.writeln("%%EndResource");
        defineWinAnsiEncoding(gen);
        
        //Rewrite font encodings
        enum = fonts.keySet().iterator();
        while (enum.hasNext()) {
            String key = (String)enum.next();
            Font fm = (Font)fonts.get(key);
            if (null == fm.getEncoding()) {
                //ignore (ZapfDingbats and Symbol run through here
                //TODO: ZapfDingbats and Symbol should get getEncoding() fixed!
            } else if ("WinAnsiEncoding".equals(fm.getEncoding())) {
                gen.writeln("/" + fm.getFontName() + " findfont");
                gen.writeln("dup length dict begin");
                gen.writeln("  {1 index /FID ne {def} {pop pop} ifelse} forall");
                gen.writeln("  /Encoding " + fm.getEncoding() + " def");
                gen.writeln("  currentdict");
                gen.writeln("end");
                gen.writeln("/" + fm.getFontName() + " exch definefont pop");
            } else {
                System.out.println("Only WinAnsiEncoding is supported. Font '" 
                    + fm.getFontName() + "' asks for: " + fm.getEncoding());
            }
        }
    }

    private static void defineWinAnsiEncoding(PSGenerator gen) throws IOException {
        gen.writeln("/WinAnsiEncoding [");
        for (int i = 0; i < Glyphs.WINANSI_ENCODING.length; i++) {
            if (i > 0) {
                if ((i % 5) == 0) {
                    gen.newLine();
                } else {
                    gen.write(" ");
                }
            }
            final char ch = Glyphs.WINANSI_ENCODING[i];
            final String glyphname = Glyphs.charToGlyphName(ch);
            if ("".equals(glyphname)) {
                gen.write("/" + Glyphs.NOTDEF);
            } else {
                gen.write("/");
                gen.write(glyphname);
            }
        }
        gen.newLine();
        gen.writeln("] def");
    }

}
