/*

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


package org.apache.fop.apps;

// SAX
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


// Java
import java.io.FileReader;
import java.io.File;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;

// Xalan
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTProcessorFactory;
import org.apache.xalan.xslt.XSLTResultTarget;

// FOP
import org.apache.fop.messaging.MessageHandler;


/**
 * mainline class.
 *
 * Gets input and output filenames from the command line.
 * Creates a SAX Parser (defaulting to Xerces).
 *
 */
public class XalanCommandLine {

  /**
   * creates a SAX parser, using the value of org.xml.sax.parser
   * defaulting to org.apache.xerces.parsers.SAXParser
   *
   * @return the created SAX parser
   */
  static XMLReader createParser() {
    String parserClassName =
        System.getProperty("org.xml.sax.parser");
    if (parserClassName == null) {
        parserClassName = "org.apache.xerces.parsers.SAXParser";
    }
    org.apache.fop.messaging.MessageHandler.logln("using SAX parser " + parserClassName);

    try {
        return (XMLReader)
      Class.forName(parserClassName).newInstance();
    } catch (ClassNotFoundException e) {
        org.apache.fop.messaging.MessageHandler.errorln("Could not find " + parserClassName);
    } catch (InstantiationException e) {
        org.apache.fop.messaging.MessageHandler.errorln("Could not instantiate "
               + parserClassName);
    } catch (IllegalAccessException e) {
        org.apache.fop.messaging.MessageHandler.errorln("Could not access " + parserClassName);
    } catch (ClassCastException e) {
        org.apache.fop.messaging.MessageHandler.errorln(parserClassName + " is not a SAX driver");
    }
    return null;
  }

  /**
   * create an InputSource from a file name
   *
   * @param filename the name of the file
   * @return the InputSource created
   */
  protected static InputSource fileInputSource(String filename) {

    /* this code adapted from James Clark's in XT */
    File file = new File(filename);
    String path = file.getAbsolutePath();
    String fSep = System.getProperty("file.separator");
    if (fSep != null && fSep.length() == 1)
        path = path.replace(fSep.charAt(0), '/');
    if (path.length() > 0 && path.charAt(0) != '/')
        path = '/' + path;
    try {
        return new InputSource(new URL("file", null,
               path).toString());
    }
    catch (java.net.MalformedURLException e) {
        throw new Error("unexpected MalformedURLException");
    }
  }

  /**
   * mainline method
   *
   * first command line argument is xml input file
   * second command line argument is xslt file which commands the conversion from xml to xsl:fo
   * third command line argument is the output file
   * 
   * @param command line arguments
   */
  public static void main(String[] args) {
    String version = Version.getVersion();
    MessageHandler.logln(version);


    if (args.length != 3) {
        MessageHandler.errorln("usage: java "
               + "org.apache.fop.apps.XalanCommandLine "
               + "xml-file xslt-file pdf-file");
        System.exit(1);
    }

    XMLReader parser = createParser();

    if (parser == null) {
        MessageHandler.errorln("ERROR: Unable to create SAX parser");
        System.exit(1);
    }

    // setting the parser features
    try {
      parser.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
    } catch (SAXException e) {
      MessageHandler.errorln("Error in setting up parser feature namespace-prefixes");
      MessageHandler.errorln("You need a parser which supports SAX version 2");
      System.exit(1);
    }


    try {
      java.io.Writer writer;
      java.io.Reader reader;
      boolean usefile = false;

      MessageHandler.logln("transforming to xsl:fo markup");


      // create a Writer      
      // the following is an ugly hack to allow processing of larger files
      // if xml file size is larger than 700 kb write the fo:file to disk
      if ((new File(args[0]).length()) > 500000) {
        writer = new FileWriter(args[2]+".tmp");
        usefile = true;
      } else {
        writer = new StringWriter();
      }

      // Use XSLTProcessorFactory to instantiate an XSLTProcessor.
      XSLTProcessor processor = XSLTProcessorFactory.getProcessor();

      // Create the 3 objects the XSLTProcessor needs to perform the transformation.
      XSLTInputSource xmlSource  = new XSLTInputSource (args[0]);
      XSLTInputSource xslSheet   = new XSLTInputSource (args[1]);
      XSLTResultTarget xmlResult = new XSLTResultTarget (writer);

      // Perform the transformation.
      processor.process(xmlSource, xslSheet, xmlResult);
      
      if (usefile) {
        reader = new FileReader(args[2]+".tmp");
      } else {
        // create a input source containing the xsl:fo file which can be fed to Fop  
        reader = new StringReader(writer.toString());   
      }
      writer.flush();
      writer.close();

      //set Driver methods to start Fop processing
      Driver driver = new Driver();
      driver.setRenderer("org.apache.fop.render.pdf.PDFRenderer", version);
      driver.addElementMapping("org.apache.fop.fo.StandardElementMapping");
      driver.addElementMapping("org.apache.fop.svg.SVGElementMapping");
      driver.setWriter(new PrintWriter(new FileWriter(args[2])));
      driver.buildFOTree(parser, new InputSource(reader));
      reader.close();
      driver.format();
      driver.render();
      if (usefile) {
        new File (args[2]+".tmp").delete();
      }
    } catch (Exception e) {
      MessageHandler.errorln("FATAL ERROR: " + e.getMessage());
      System.exit(1);
    }
  }
}
