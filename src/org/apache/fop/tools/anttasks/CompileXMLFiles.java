/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.tools.anttasks;

/**
 * This class is an extension of Ant, a script utility from
 * jakarta.apache.org.
 * It takes a couple of xml files conforming to the xml-site dtd and
 * writes them all into one xml file, deleting any reference to
 * the proprietary protocol sbk. The configFile determines what files
 * are read in what sequence.
 */
// Ant
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;


// SAX
import org.xml.sax.Parser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.Locator;
import org.xml.sax.AttributeList;

// Java
import java.io.*;
import java.util.*;
import java.net.URL;

public class CompileXMLFiles extends Task
    implements org.xml.sax.EntityResolver, org.xml.sax.DTDHandler,
               org.xml.sax.DocumentHandler, org.xml.sax.ErrorHandler {
    private String configFile, outFile;
    private String[] filenameList;
    private String filenames;
    private Vector files = new Vector();

    // sets name of configuration file, which must
    // be an xml file conforming to the book.dtd used by xml-site
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public void setOutFile(String outFile) {
        this.outFile = outFile;
    }


    // main method of this task
    public void execute() throws BuildException {
        boolean errors = false;

        if (!(new File(configFile).exists())) {
            errors = true;
            System.err.println("Task CompileXMLFiles - ERROR: config file "
                               + configFile + " is missing.");
        }

        Parser parser = createParser();

        if (parser == null) {
            System.err.println("Task  CompileXMLFiles - ERROR: Unable to create SAX parser");
            errors = true;
        }
        parser.setDocumentHandler(this);
        try {
            parser.parse(CompileXMLFiles.fileInputSource(configFile));
        } catch (SAXException e) {
            System.out.println(e);
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }    // end: execute()


    /* the following methods belong to the sax parser and implement the Document Handler */
    public InputSource resolveEntity(String publicId,
                                     String systemId) throws SAXException {
        return null;
    }

    public void notationDecl(String name, String publicId, String systemId) {
        // no op
    }

    public void unparsedEntityDecl(String name, String publicId,
                                   String systemId, String notationName) {
        // no op
    }

    public void setDocumentLocator(Locator locator) {
        // no op
    }

    public void startDocument() throws SAXException {
        // no op
    }

    /*
     * After the cnfiguration file has been parsed all files which
     * have been collected in the ArrayList files are concatinated
     * and written to a new (temporary) file
     */
    public void endDocument() throws SAXException {
        String line, filename;
        BufferedReader in;
        Enumeration iterator = files.elements();
        try {
            BufferedWriter out =
                new BufferedWriter(new FileWriter("compileXMLFiles-tmp.xml"));
            out.write("<?xml version=\"1.0\"?>\n"
                      + "<!DOCTYPE documentation [\n"
                      + "<!ENTITY nbsp \" \">\n" + "]>\n<documentation>");
            while (iterator.hasMoreElements()) {
                filename = (String)iterator.nextElement();
                in = new BufferedReader(new FileReader(filename));
                while ((line = in.readLine()) != null) {
                    // kill the lines pointing to the sbk protocol and the xml declaration
                    if (line.indexOf("<!DOCTYPE ") != -1
                            || line.indexOf("<?xml ") != -1) {
                        line = "";
                    }
                    out.write(line + "\n");
                }
                out.flush();
            }
            out.write("\n</documentation>");
            out.close();
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public void startElement(String name,
                             AttributeList atts) throws SAXException {
        String id, label, source;
        if (name.equals("document") || name.equals("entry")) {
            source = atts.getValue("source");
            files.addElement(source);
        }
    }

    public void endElement(String name) throws SAXException {
        // no op
    }

    public void characters(char ch[], int start,
                           int length) throws SAXException {
        // no op
    }

    public void ignorableWhitespace(char ch[], int start,
                                    int length) throws SAXException {
        // no op
    }

    public void processingInstruction(String target,
                                      String data) throws SAXException {
        // no op
    }

    public void warning(SAXParseException e) throws SAXException {
        // no op
    }

    public void error(SAXParseException e) throws SAXException {
        // no op
    }

    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }

    /* ------------------------ */

    /**
     * creates a SAX parser, using the value of org.xml.sax.parser
     * defaulting to org.apache.xerces.parsers.SAXParser
     *
     * @return the created SAX parser
     */
    static Parser createParser() {
        String parserClassName = System.getProperty("org.xml.sax.parser");
        if (parserClassName == null) {
            parserClassName = "org.apache.xerces.parsers.SAXParser";
        }
        System.err.println("using SAX parser " + parserClassName);

        try {
            return (Parser)Class.forName(parserClassName).newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println("Could not find " + parserClassName);
        } catch (InstantiationException e) {
            System.err.println("Could not instantiate " + parserClassName);
        } catch (IllegalAccessException e) {
            System.err.println("Could not access " + parserClassName);
        } catch (ClassCastException e) {
            System.err.println(parserClassName + " is not a SAX driver");
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
            return new InputSource(new URL("file", null, path).toString());
        } catch (java.net.MalformedURLException e) {
            throw new Error("unexpected MalformedURLException");
        }
    }

}

