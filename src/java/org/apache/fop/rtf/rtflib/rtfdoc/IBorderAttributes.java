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

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

package org.apache.fop.rtf.rtflib.rtfdoc;

/** Constants for RTF table attribute names */

public interface IBorderAttributes {

    String BORDER_SINGLE_THICKNESS = "brdrs";
    String BORDER_DOUBLE_THICKNESS = "brdrth";
    String BORDER_SHADOWED ="brdrsh";
    String BORDER_DOUBLE ="brdrdb";
    String BORDER_DOTTED ="brdrdot";
    String BORDER_DASH ="brdrdash";
    String BORDER_HAIRLINE ="brdrhair";
    String BORDER_DASH_SMALL ="brdrdashsm";
    String BORDER_DOT_DASH = "brdrdashd";
    String BORDER_DOT_DOT_DASH = "brdrdashdd";
    String BORDER_TRIPLE = "brdrtriple";
    String BORDER_THINK_THIN_SMALL = "brdrtnthsg";
    String BORDER_THIN_THICK_SMALL = "brdrthtnsg";
    String BORDER_THIN_THICK_THIN_SMALL = "brdrthtnthsg";
    String BORDER_THINK_THIN_MEDIUM = "brdrtnthmg";
    String BORDER_THIN_THICK_MEDIUM = "brdrthtnmg";
    String BORDER_THIN_THICK_THIN_MEDIUM = "brdrthtnthmg";
    String BORDER_THINK_THIN_LARGE = "brdrtnthlg";
    String BORDER_THIN_THICK_LARGE = "brdrthtnlg";
    String BORDER_THIN_THICK_THIN_LARGE = "brdrthtnthlg";
    String BORDER_WAVY = "brdrwavy";
    String BORDER_WAVY_DOUBLE = "brdrwavydb";
    String BORDER_STRIPED = "brdrdashdotstr";
    String BORDER_EMBOSS = "brdremboss";
    String BORDER_ENGRAVE = "brdrengrave";
    String BORDER_COLOR = "brdrcf";
    String BORDER_SPACE = "brsp";
}