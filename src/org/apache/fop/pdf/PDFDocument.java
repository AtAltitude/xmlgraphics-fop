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
package org.apache.fop.pdf;

import org.apache.fop.util.StreamUtilities;

import org.apache.fop.fonts.CIDFont;
import org.apache.fop.fonts.CustomFont;
import org.apache.fop.fonts.FontDescriptor;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.fonts.FontType;
import org.apache.fop.fonts.LazyFont;
import org.apache.fop.fonts.MultiByteFont;
import org.apache.fop.fonts.truetype.FontFileReader;
import org.apache.fop.fonts.truetype.TTFSubSetFile;
import org.apache.fop.fonts.type1.PFBData;
import org.apache.fop.fonts.type1.PFBParser;

// Java
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.awt.geom.Rectangle2D;

/* image support modified from work of BoBoGi */
/* font support based on work by Takayuki Takeuchi */

/**
 * class representing a PDF document.
 *
 * The document is built up by calling various methods and then finally
 * output to given filehandle using output method.
 *
 * A PDF document consists of a series of numbered objects preceded by a
 * header and followed by an xref table and trailer. The xref table
 * allows for quick access to objects by listing their character
 * positions within the document. For this reason the PDF document must
 * keep track of the character position of each object.  The document
 * also keeps direct track of the /Root, /Info and /Resources objects.
 *
 * Modified by Mark Lillywhite, mark-fop@inomial.com. The changes
 * involve: ability to output pages one-at-a-time in a streaming
 * fashion (rather than storing them all for output at the end);
 * ability to write the /Pages object after writing the rest
 * of the document; ability to write to a stream and flush
 * the object list; enhanced trailer output; cleanups.
 *
 */
public class PDFDocument {
    private static final Integer LOCATION_PLACEHOLDER = new Integer(0);
    /**
     * the version of PDF supported which is 1.4
     */
    protected static final String PDF_VERSION = "1.4";

    /**
     * the encoding to use when converting strings to PDF commandos.
     */
    public static final String ENCODING = "ISO-8859-1";

    /**
     * the current character position
     */
    protected int position = 0;

    /**
     * the character position of each object
     */
    protected List location = new java.util.ArrayList();

    /** List of objects to write in the trailer */
    private List trailerObjects = new java.util.ArrayList();

    /**
     * the counter for object numbering
     */
    protected int objectcount = 0;

    /**
     * the objects themselves
     */
    protected List objects = new java.util.ArrayList();

    /**
     * character position of xref table
     */
    protected int xref;

    /**
     * the /Root object
     */
    protected PDFRoot root;

    /** The root outline object */
    private PDFOutline outlineRoot = null;

    /** The /Pages object (mark-fop@inomial.com) */
    private PDFPages pages;

    /**
     * the /Info object
     */
    protected PDFInfo info;

    /**
     * the /Resources object
     */
    protected PDFResources resources;

    /**
     * the colorspace (0=RGB, 1=CMYK)
     */
    protected PDFColorSpace colorspace = new PDFColorSpace(PDFColorSpace.DEVICE_RGB);

    /**
     * the counter for Pattern name numbering (e.g. 'Pattern1')
     */
    protected int patternCount = 0;

    /**
     * the counter for Shading name numbering
     */
    protected int shadingCount = 0;

    /**
     * the counter for XObject numbering
     */
    protected int xObjectCount = 0;

    /**
     * the XObjects Map.
     * Should be modified (works only for image subtype)
     */
    protected Map xObjectsMap = new java.util.HashMap();

    /**
     * the Font Map.
     */
    protected Map fontMap = new java.util.HashMap();

    /**
     * The filter map.
     */
    protected Map filterMap = new java.util.HashMap();

    /**
     * List of PDFGState objects.
     */
    protected List gstates = new java.util.ArrayList();

    /**
     * List of functions.
     */
    protected List functions = new java.util.ArrayList();

    /**
     * List of shadings.
     */
    protected List shadings = new java.util.ArrayList();

    /**
     * List of patterns.
     */
    protected List patterns = new java.util.ArrayList();

    /**
     * List of Links.
     */
    protected List links = new java.util.ArrayList();

    /**
     * List of FileSpecs.
     */
    protected List filespecs = new java.util.ArrayList();

    /**
     * List of GoToRemotes.
     */
    protected List gotoremotes = new java.util.ArrayList();

    /**
     * List of GoTos.
     */
    protected List gotos = new java.util.ArrayList();


    /**
     * creates an empty PDF document <p>
     *
     * The constructor creates a /Root and /Pages object to
     * track the document but does not write these objects until
     * the trailer is written. Note that the object ID of the
     * pages object is determined now, and the xref table is
     * updated later. This allows Pages to refer to their
     * Parent before we write it out.
     *
     * @param prod the name of the producer of this pdf document
     */
    public PDFDocument(String prod) {

        /* create the /Root, /Info and /Resources objects */
        this.pages = makePages();

        // Create the Root object
        this.root = makeRoot(pages);

        // Create the Resources object
        this.resources = makeResources();

        // Make the /Info record
        this.info = makeInfo(prod);
    }

    /**
     * set the producer of the document
     *
     * @param producer string indicating application producing the PDF
     */
    public void setProducer(String producer) {
        this.info.setProducer(producer);
    }

    /**
     * set the creator of the document
     *
     * @param creator string indicating application creating the document
     */
    public void setCreator(String creator) {
        this.info.setCreator(creator);
    }

    /**
     * Set the filter map to use for filters in this document.
     *
     * @param map the map of filter lists for each stream type
     */
    public void setFilterMap(Map map) {
        filterMap = map;
    }

    /**
     * Get the filter map used for filters in this document.
     *
     * @return the map of filters being used
     */
    public Map getFilterMap() {
        return filterMap;
    }

    /**
     * Make a /Catalog (Root) object. This object is written in
     * the trailer.
     *
     * @param pages the pages pdf object that the root points to
     * @return the new pdf root object for this document
     */
    public PDFRoot makeRoot(PDFPages pages) {

        /*
        * Make a /Pages object. This object is written in the trailer.
        */
        PDFRoot pdfRoot = new PDFRoot(++this.objectcount, pages);
        addTrailerObject(pdfRoot);
        return pdfRoot;
    }

    /**
     * Get the PDF root object.
     *
     * @return the PDFRoot object
     */
    public PDFRoot getRoot() {
        return this.root;
    }

    /**
     * Make a /Pages object. This object is written in the trailer.
     *
     * @return a new PDF Pages object for adding pages to
     */
    public PDFPages makePages() {
        PDFPages pdfPages = new PDFPages(++this.objectcount);
        addTrailerObject(pdfPages);
        return pdfPages;
    }

    /**
     * Make a /Resources object. This object is written in the trailer.
     *
     * @return a new PDF resources object
     */
    public PDFResources makeResources() {
        PDFResources pdfResources = new PDFResources(++this.objectcount);
        addTrailerObject(pdfResources);
        return pdfResources;
    }

    /**
     * make an /Info object
     *
     * @param prod string indicating application producing the PDF
     * @return the created /Info object
     */
    protected PDFInfo makeInfo(String prod) {

        /*
         * create a PDFInfo with the next object number and add to
         * list of objects
         */
        PDFInfo pdfInfo = new PDFInfo(++this.objectcount);
        // set the default producer
        pdfInfo.setProducer(prod);
        this.objects.add(pdfInfo);
        return pdfInfo;
    }

    /**
     * Get the pdf info object for this document.
     *
     * @return the PDF Info object for this document
     */
    public PDFInfo getInfo() {
        return info;
    }

    /**
     * Make a Type 0 sampled function
     *
     * @param theDomain List objects of Double objects.
     * This is the domain of the function.
     * See page 264 of the PDF 1.3 Spec.
     * @param theRange List objects of Double objects.
     * This is the Range of the function.
     * See page 264 of the PDF 1.3 Spec.
     * @param theSize A List object of Integer objects.
     * This is the number of samples in each input dimension.
     * I can't imagine there being more or less than two input dimensions,
     * so maybe this should be an array of length 2.
     *
     * See page 265 of the PDF 1.3 Spec.
     * @param theBitsPerSample An int specifying the number of bits user
     *                    to represent each sample value.
     * Limited to 1,2,4,8,12,16,24 or 32.
     * See page 265 of the 1.3 PDF Spec.
     * @param theOrder The order of interpolation between samples.
     *                 Default is 1 (one). Limited
     * to 1 (one) or 3, which means linear or cubic-spline interpolation.
     *
     * This attribute is optional.
     *
     * See page 265 in the PDF 1.3 spec.
     * @param theEncode List objects of Double objects.
     * This is the linear mapping of input values intop the domain
     * of the function's sample table. Default is hard to represent in
     * ascii, but basically [0 (Size0 1) 0 (Size1 1)...].
     * This attribute is optional.
     *
     * See page 265 in the PDF 1.3 spec.
     * @param theDecode List objects of Double objects.
     * This is a linear mapping of sample values into the range.
     * The default is just the range.
     *
     * This attribute is optional.
     * Read about it on page 265 of the PDF 1.3 spec.
     * @param theFunctionDataStream The sample values that specify
     *                        the function are provided in a stream.
     *
     * This is optional, but is almost always used.
     *
     * Page 265 of the PDF 1.3 spec has more.
     * @param theFilter This is a vector of String objects which
     *                  are the various filters that have are to be
     *                  applied to the stream to make sense of it.
     *                  Order matters, so watch out.
     *
     * This is not documented in the Function section of the PDF 1.3 spec,
     * it was deduced from samples that this is sometimes used, even if we may never
     * use it in FOP. It is added for completeness sake.
     * @param theFunctionType This is the type of function (0,2,3, or 4).
     * It should be 0 as this is the constructor for sampled functions.
     * @return the PDF function that was created
     */
    public PDFFunction makeFunction(int theFunctionType, List theDomain,
                                    List theRange, List theSize,
                                    int theBitsPerSample, int theOrder,
                                    List theEncode, List theDecode,
                                    StringBuffer theFunctionDataStream,
                                    List theFilter) {
        // Type 0 function
        PDFFunction function = new PDFFunction(++this.objectcount,
                                               theFunctionType, theDomain,
                                               theRange, theSize,
                                               theBitsPerSample, theOrder,
                                               theEncode, theDecode,
                                               theFunctionDataStream,
                                               theFilter);

        PDFFunction oldfunc = findFunction(function);
        if (oldfunc == null) {
            functions.add(function);
            this.objects.add(function);
        } else {
            this.objectcount--;
            function = oldfunc;
        }

        return (function);
    }

    /**
     * make a type Exponential interpolation function
     * (for shading usually)
     *
     * @param theDomain List objects of Double objects.
     * This is the domain of the function.
     * See page 264 of the PDF 1.3 Spec.
     * @param theRange List of Doubles that is the Range of the function.
     * See page 264 of the PDF 1.3 Spec.
     * @param theCZero This is a vector of Double objects which defines the function result
     * when x=0.
     *
     * This attribute is optional.
     * It's described on page 268 of the PDF 1.3 spec.
     * @param theCOne This is a vector of Double objects which defines the function result
     * when x=1.
     *
     * This attribute is optional.
     * It's described on page 268 of the PDF 1.3 spec.
     * @param theInterpolationExponentN This is the inerpolation exponent.
     *
     * This attribute is required.
     * PDF Spec page 268
     * @param theFunctionType The type of the function, which should be 2.
     * @return the PDF function that was created
     */
    public PDFFunction makeFunction(int theFunctionType, List theDomain,
                                    List theRange, List theCZero,
                                    List theCOne,
                                    double theInterpolationExponentN) {    // type 2
        PDFFunction function = new PDFFunction(++this.objectcount,
                                               theFunctionType, theDomain,
                                               theRange, theCZero, theCOne,
                                               theInterpolationExponentN);
        PDFFunction oldfunc = findFunction(function);
        if (oldfunc == null) {
            functions.add(function);
            this.objects.add(function);
        } else {
            this.objectcount--;
            function = oldfunc;
        }

        return (function);
    }

    private Object findPDFObject(List list, PDFObject compare) {
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (compare.equals(obj)) {
                return obj;
            }
        }
        return null;
    }

    private PDFFunction findFunction(PDFFunction compare) {
        return (PDFFunction)findPDFObject(functions, compare);
    }

    private PDFShading findShading(PDFShading compare) {
        return (PDFShading)findPDFObject(shadings, compare);
    }

    /**
     * Find a previous pattern.
     * The problem with this is for tiling patterns the pattern
     * data stream is stored and may use up memory, usually this
     * would only be a small amount of data.
     */
    private PDFPattern findPattern(PDFPattern compare) {
        return (PDFPattern)findPDFObject(patterns, compare);
    }

    /**
     * Make a Type 3 Stitching function
     *
     * @param theDomain List objects of Double objects.
     * This is the domain of the function.
     * See page 264 of the PDF 1.3 Spec.
     * @param theRange List objects of Double objects.
     * This is the Range of the function.
     * See page 264 of the PDF 1.3 Spec.
     * @param theFunctions An List of the PDFFunction objects
     *                     that the stitching function stitches.
     *
     * This attributed is required.
     * It is described on page 269 of the PDF spec.
     * @param theBounds This is a vector of Doubles representing
     *                  the numbers that, in conjunction with Domain
     *                  define the intervals to which each function from
     *                  the 'functions' object applies. It must be in
     *                  order of increasing magnitude, and each must be
     *                  within Domain.
     *
     * It basically sets how much of the gradient each function handles.
     *
     * This attributed is required.
     * It's described on page 269 of the PDF 1.3 spec.
     * @param theEncode List objects of Double objects.
     * This is the linear mapping of input values intop the domain
     * of the function's sample table. Default is hard to represent in
     * ascii, but basically [0 (Size0 1) 0 (Size1 1)...].
     * This attribute is required.
     *
     * See page 270 in the PDF 1.3 spec.
     * @param theFunctionType This is the function type. It should be 3,
     * for a stitching function.
     * @return the PDF function that was created
     */
    public PDFFunction makeFunction(int theFunctionType, List theDomain,
                                    List theRange, List theFunctions,
                                    List theBounds,
                                    List theEncode) {
        // Type 3

        PDFFunction function = new PDFFunction(++this.objectcount,
                                               theFunctionType, theDomain,
                                               theRange, theFunctions,
                                               theBounds, theEncode);

        PDFFunction oldfunc = findFunction(function);
        if (oldfunc == null) {
            functions.add(function);
            this.objects.add(function);
        } else {
            this.objectcount--;
            function = oldfunc;
        }

        return (function);
    }

    /**
     * make a postscript calculator function
     *
     * @param theNumber the PDF object number
     * @param theFunctionType the type of function to make
     * @param theDomain the domain values
     * @param theRange the range values of the function
     * @param theFunctionDataStream a string containing the pdf drawing
     * @return the PDF function that was created
     */
    public PDFFunction makeFunction(int theNumber, int theFunctionType,
                                    List theDomain, List theRange,
                                    StringBuffer theFunctionDataStream) {
        // Type 4
        PDFFunction function = new PDFFunction(++this.objectcount,
                                               theFunctionType, theDomain,
                                               theRange,
                                               theFunctionDataStream);

        PDFFunction oldfunc = findFunction(function);
        if (oldfunc == null) {
            functions.add(function);
            this.objects.add(function);
        } else {
            this.objectcount--;
            function = oldfunc;
        }

        return (function);

    }

    /**
     * make a function based shading object
     *
     * @param res the PDF resource context to add the shading, may be null
     * @param theShadingType The type of shading object, which should be 1 for function
     * based shading.
     * @param theColorSpace The colorspace is 'DeviceRGB' or something similar.
     * @param theBackground An array of color components appropriate to the
     * colorspace key specifying a single color value.
     * This key is used by the f operator buy ignored by the sh operator.
     * @param theBBox List of double's representing a rectangle
     * in the coordinate space that is current at the
     * time of shading is imaged. Temporary clipping
     * boundary.
     * @param theAntiAlias Whether or not to anti-alias.
     * @param theDomain Optional vector of Doubles specifying the domain.
     * @param theMatrix List of Doubles specifying the matrix.
     * If it's a pattern, then the matrix maps it to pattern space.
     * If it's a shading, then it maps it to current user space.
     * It's optional, the default is the identity matrix
     * @param theFunction The PDF Function that maps an (x,y) location to a color
     * @return the PDF shading that was created
     */
    public PDFShading makeShading(PDFResourceContext res, int theShadingType,
                                  PDFColorSpace theColorSpace,
                                  List theBackground, List theBBox,
                                  boolean theAntiAlias, List theDomain,
                                  List theMatrix,
                                  PDFFunction theFunction) {
        // make Shading of Type 1
        String theShadingName = new String("Sh" + (++this.shadingCount));

        PDFShading shading = new PDFShading(++this.objectcount,
                                            theShadingName, theShadingType,
                                            theColorSpace, theBackground,
                                            theBBox, theAntiAlias, theDomain,
                                            theMatrix, theFunction);

        PDFShading oldshad = findShading(shading);
        if (oldshad == null) {
            shadings.add(shading);
            this.objects.add(shading);
        } else {
            this.objectcount--;
            this.shadingCount--;
            shading = oldshad;
        }

        // add this shading to resources
        if (res != null) {
            res.getPDFResources().addShading(shading);
        } else {
            this.resources.addShading(shading);
        }

        return (shading);
    }

    /**
     * Make an axial or radial shading object.
     *
     * @param res the PDF resource context to add the shading, may be null
     * @param theShadingType 2 or 3 for axial or radial shading
     * @param theColorSpace "DeviceRGB" or similar.
     * @param theBackground theBackground An array of color components appropriate to the
     * colorspace key specifying a single color value.
     * This key is used by the f operator buy ignored by the sh operator.
     * @param theBBox List of double's representing a rectangle
     * in the coordinate space that is current at the
     * time of shading is imaged. Temporary clipping
     * boundary.
     * @param theAntiAlias Default is false
     * @param theCoords List of four (type 2) or 6 (type 3) Double
     * @param theDomain List of Doubles specifying the domain
     * @param theFunction the Stitching (PDFfunction type 3) function,
     *                    even if it's stitching a single function
     * @param theExtend List of Booleans of whether to extend the
     *                  start and end colors past the start and end points
     * The default is [false, false]
     * @return the PDF shading that was created
     */
    public PDFShading makeShading(PDFResourceContext res, int theShadingType,
                                  PDFColorSpace theColorSpace,
                                  List theBackground, List theBBox,
                                  boolean theAntiAlias, List theCoords,
                                  List theDomain, PDFFunction theFunction,
                                  List theExtend) {
        // make Shading of Type 2 or 3
        String theShadingName = new String("Sh" + (++this.shadingCount));

        PDFShading shading = new PDFShading(++this.objectcount,
                                            theShadingName, theShadingType,
                                            theColorSpace, theBackground,
                                            theBBox, theAntiAlias, theCoords,
                                            theDomain, theFunction,
                                            theExtend);

        PDFShading oldshad = findShading(shading);
        if (oldshad == null) {
            shadings.add(shading);
            this.objects.add(shading);
        } else {
            this.objectcount--;
            this.shadingCount--;
            shading = oldshad;
        }

        if (res != null) {
            res.getPDFResources().addShading(shading);
        } else {
            this.resources.addShading(shading);
        }

        return (shading);
    }

    /**
     * Make a free-form gouraud shaded triangle mesh, coons patch mesh, or tensor patch mesh
     * shading object
     *
     * @param res the PDF resource context to add the shading, may be null
     * @param theShadingType 4, 6, or 7 depending on whether it's
     * Free-form gouraud-shaded triangle meshes, coons patch meshes,
     * or tensor product patch meshes, respectively.
     * @param theColorSpace "DeviceRGB" or similar.
     * @param theBackground theBackground An array of color components appropriate to the
     * colorspace key specifying a single color value.
     * This key is used by the f operator buy ignored by the sh operator.
     * @param theBBox List of double's representing a rectangle
     * in the coordinate space that is current at the
     * time of shading is imaged. Temporary clipping
     * boundary.
     * @param theAntiAlias Default is false
     * @param theBitsPerCoordinate 1,2,4,8,12,16,24 or 32.
     * @param theBitsPerComponent 1,2,4,8,12, and 16
     * @param theBitsPerFlag 2,4,8.
     * @param theDecode List of Doubles see PDF 1.3 spec pages 303 to 312.
     * @param theFunction the PDFFunction
     * @return the PDF shading that was created
     */
    public PDFShading makeShading(PDFResourceContext res, int theShadingType,
                                  PDFColorSpace theColorSpace,
                                  List theBackground, List theBBox,
                                  boolean theAntiAlias,
                                  int theBitsPerCoordinate,
                                  int theBitsPerComponent,
                                  int theBitsPerFlag, List theDecode,
                                  PDFFunction theFunction) {
        // make Shading of type 4,6 or 7
        String theShadingName = new String("Sh" + (++this.shadingCount));

        PDFShading shading = new PDFShading(++this.objectcount,
                                            theShadingName, theShadingType,
                                            theColorSpace, theBackground,
                                            theBBox, theAntiAlias,
                                            theBitsPerCoordinate,
                                            theBitsPerComponent,
                                            theBitsPerFlag, theDecode,
                                            theFunction);

        PDFShading oldshad = findShading(shading);
        if (oldshad == null) {
            shadings.add(shading);
            this.objects.add(shading);
        } else {
            this.objectcount--;
            this.shadingCount--;
            shading = oldshad;
        }

        if (res != null) {
            res.getPDFResources().addShading(shading);
        } else {
            this.resources.addShading(shading);
        }

        return (shading);
    }

    /**
     * make a Lattice-Form Gouraud mesh shading object
     *
     * @param res the PDF resource context to add the shading, may be null
     * @param theShadingType 5 for lattice-Form Gouraud shaded-triangle mesh
     * without spaces. "Shading1" or "Sh1" are good examples.
     * @param theColorSpace "DeviceRGB" or similar.
     * @param theBackground theBackground An array of color components appropriate to the
     * colorspace key specifying a single color value.
     * This key is used by the f operator buy ignored by the sh operator.
     * @param theBBox List of double's representing a rectangle
     * in the coordinate space that is current at the
     * time of shading is imaged. Temporary clipping
     * boundary.
     * @param theAntiAlias Default is false
     * @param theBitsPerCoordinate 1,2,4,8,12,16, 24, or 32
     * @param theBitsPerComponent 1,2,4,8,12,24,32
     * @param theDecode List of Doubles. See page 305 in PDF 1.3 spec.
     * @param theVerticesPerRow number of vertices in each "row" of the lattice.
     * @param theFunction The PDFFunction that's mapped on to this shape
     * @return the PDF shading that was created
     */
    public PDFShading makeShading(PDFResourceContext res, int theShadingType,
                                  PDFColorSpace theColorSpace,
                                  List theBackground, List theBBox,
                                  boolean theAntiAlias,
                                  int theBitsPerCoordinate,
                                  int theBitsPerComponent, List theDecode,
                                  int theVerticesPerRow,
                                  PDFFunction theFunction) {
        // make shading of Type 5
        String theShadingName = new String("Sh" + (++this.shadingCount));

        PDFShading shading = new PDFShading(++this.objectcount,
                                            theShadingName, theShadingType,
                                            theColorSpace, theBackground,
                                            theBBox, theAntiAlias,
                                            theBitsPerCoordinate,
                                            theBitsPerComponent, theDecode,
                                            theVerticesPerRow, theFunction);

        PDFShading oldshad = findShading(shading);
        if (oldshad == null) {
            shadings.add(shading);
            this.objects.add(shading);
        } else {
            this.objectcount--;
            this.shadingCount--;
            shading = oldshad;
        }

        if (res != null) {
            res.getPDFResources().addShading(shading);
        } else {
            this.resources.addShading(shading);
        }

        return (shading);
    }

    /**
     * Make a tiling pattern
     *
     * @param res the PDF resource context to add the shading, may be null
     * @param thePatternType the type of pattern, which is 1 for tiling.
     * @param theResources the resources associated with this pattern
     * @param thePaintType 1 or 2, colored or uncolored.
     * @param theTilingType 1, 2, or 3, constant spacing, no distortion, or faster tiling
     * @param theBBox List of Doubles: The pattern cell bounding box
     * @param theXStep horizontal spacing
     * @param theYStep vertical spacing
     * @param theMatrix Optional List of Doubles transformation matrix
     * @param theXUID Optional vector of Integers that uniquely identify the pattern
     * @param thePatternDataStream The stream of pattern data to be tiled.
     * @return the PDF pattern that was created
     */
    public PDFPattern makePattern(PDFResourceContext res, int thePatternType,    // 1
                                  PDFResources theResources, int thePaintType, int theTilingType,
                                  List theBBox, double theXStep,
                                  double theYStep, List theMatrix,
                                  List theXUID, StringBuffer thePatternDataStream) {
        String thePatternName = new String("Pa" + (++this.patternCount));
        // int theNumber, String thePatternName,
        // PDFResources theResources
        PDFPattern pattern = new PDFPattern(++this.objectcount,
                                            thePatternName, theResources, 1,
                                            thePaintType, theTilingType,
                                            theBBox, theXStep, theYStep,
                                            theMatrix, theXUID,
                                            thePatternDataStream);

        PDFPattern oldpatt = findPattern(pattern);
        if (oldpatt == null) {
            patterns.add(pattern);
            this.objects.add(pattern);
        } else {
            this.objectcount--;
            this.patternCount--;
            pattern = oldpatt;
        }

        if (res != null) {
            res.getPDFResources().addPattern(pattern);
        } else {
            this.resources.addPattern(pattern);
        }

        return (pattern);
    }

    /**
     * Make a smooth shading pattern
     *
     * @param res the PDF resource context to add the shading, may be null
     * @param thePatternType the type of the pattern, which is 2, smooth shading
     * @param theShading the PDF Shading object that comprises this pattern
     * @param theXUID optional:the extended unique Identifier if used.
     * @param theExtGState optional: the extended graphics state, if used.
     * @param theMatrix Optional:List of Doubles that specify the matrix.
     * @return the PDF pattern that was created       
     */
    public PDFPattern makePattern(PDFResourceContext res,
                                  int thePatternType, PDFShading theShading,
                                  List theXUID, StringBuffer theExtGState,
                                  List theMatrix) {
        String thePatternName = new String("Pa" + (++this.patternCount));

        PDFPattern pattern = new PDFPattern(++this.objectcount,
                                            thePatternName, 2, theShading,
                                            theXUID, theExtGState, theMatrix);

        PDFPattern oldpatt = findPattern(pattern);
        if (oldpatt == null) {
            patterns.add(pattern);
            this.objects.add(pattern);
        } else {
            this.objectcount--;
            this.patternCount--;
            pattern = oldpatt;
        }

        if (res != null) {
            res.getPDFResources().addPattern(pattern);
        } else {
            this.resources.addPattern(pattern);
        }

        return (pattern);
    }

    /**
     * Get the color space.
     *
     * @return the color space
     */
    public int getColorSpace() {
        return (this.colorspace.getColorSpace());
    }

    /**
     * Set the color space.
     * This is used when creating gradients.
     *
     * @param theColorspace the new color space
     */
    public void setColorSpace(int theColorspace) {
        this.colorspace.setColorSpace(theColorspace);
        return;
    }

    /**
     * Make a gradient
     *  
     * @param res the PDF resource context to add the shading, may be null
     * @param radial if true a radial gradient will be created
     * @param theColorspace the colorspace of the gradient
     * @param theColors the list of colors for the gradient
     * @param theBounds the list of bounds associated with the colors
     * @param theCoords the coordinates for the gradient
     * @return the PDF pattern that was created
     */
    public PDFPattern createGradient(PDFResourceContext res, boolean radial,
                                     PDFColorSpace theColorspace,
                                     List theColors, List theBounds,
                                     List theCoords) {
        PDFShading myShad;
        PDFFunction myfunky;
        PDFFunction myfunc;
        List theCzero;
        List theCone;
        PDFPattern myPattern;
        //PDFColorSpace theColorSpace;
        double interpolation = (double)1.000;
        List theFunctions = new java.util.ArrayList();

        int currentPosition;
        int lastPosition = theColors.size() - 1;


        // if 5 elements, the penultimate element is 3.
        // do not go beyond that, because you always need
        // to have a next color when creating the function.

        for (currentPosition = 0; currentPosition < lastPosition;
                currentPosition++) {    // for every consecutive color pair
            PDFColor currentColor =
                (PDFColor)theColors.get(currentPosition);
            PDFColor nextColor = (PDFColor)theColors.get(currentPosition
                                 + 1);
            // colorspace must be consistant
            if (this.colorspace.getColorSpace()
                    != currentColor.getColorSpace()) {
                currentColor.setColorSpace(this.colorspace.getColorSpace());
            }

            if (this.colorspace.getColorSpace() != nextColor.getColorSpace()) {
                nextColor.setColorSpace(this.colorspace.getColorSpace());
            }

            theCzero = currentColor.getVector();
            theCone = nextColor.getVector();

            myfunc = this.makeFunction(2, null, null, theCzero, theCone,
                                       interpolation);

            theFunctions.add(myfunc);

        }                               // end of for every consecutive color pair

        myfunky = this.makeFunction(3, null, null, theFunctions, theBounds,
                                    null);

        if (radial) {
            if (theCoords.size() == 6) {
                myShad = this.makeShading(res, 3, this.colorspace, null, null,
                                          false, theCoords, null, myfunky,
                                          null);
            } else {    // if the center x, center y, and radius specifiy
                // the gradient, then assume the same center x, center y,
                // and radius of zero for the other necessary component
                List newCoords = new java.util.ArrayList();
                newCoords.add(theCoords.get(0));
                newCoords.add(theCoords.get(1));
                newCoords.add(theCoords.get(2));
                newCoords.add(theCoords.get(0));
                newCoords.add(theCoords.get(1));
                newCoords.add(new Double(0.0));

                myShad = this.makeShading(res, 3, this.colorspace, null, null,
                                          false, newCoords, null, myfunky,
                                          null);

            }
        } else {
            myShad = this.makeShading(res, 2, this.colorspace, null, null, false,
                                      theCoords, null, myfunky, null);

        }

        myPattern = this.makePattern(res, 2, myShad, null, null, null);

        return (myPattern);
    }


    /**
     * make a /Encoding object
     *
     * @param encodingName character encoding scheme name
     * @return the created /Encoding object
     */
    public PDFEncoding makeEncoding(String encodingName) {

        /*
         * create a PDFEncoding with the next object number and add to the
         * list of objects
         */
        PDFEncoding encoding = new PDFEncoding(++this.objectcount,
                                               encodingName);
        this.objects.add(encoding);
        return encoding;
    }

    /**
     * Create a PDFICCStream
     * @see PDFXObject
     * @see org.apache.fop.image.JpegImage
     * @see org.apache.fop.pdf.PDFColorSpace
     * @return the new PDF ICC stream object
     */
    public PDFICCStream makePDFICCStream() {
        PDFICCStream iccStream = new PDFICCStream(++this.objectcount);
        this.objects.add(iccStream);
        return iccStream;
    }

    /**
     * Get the font map for this document.
     *
     * @return the map of fonts used in this document
     */
    public Map getFontMap() {
        return fontMap;
    }

    /**
     * make a Type1 /Font object
     *
     * @param fontname internal name to use for this font (eg "F1")
     * @param basefont name of the base font (eg "Helvetica")
     * @param encoding character encoding scheme used by the font
     * @param metrics additional information about the font
     * @param descriptor additional information about the font
     * @return the created /Font object
     */
    public PDFFont makeFont(String fontname, String basefont,
                            String encoding, FontMetrics metrics,
                            FontDescriptor descriptor) {
        if (fontMap.containsKey(fontname)) {
            return (PDFFont)fontMap.get(fontname);
        }

        /*
         * create a PDFFont with the next object number and add to the
         * list of objects
         */
        if (descriptor == null) {
            PDFFont font = new PDFFont(++this.objectcount, fontname,
                                       FontType.TYPE1, basefont, encoding);
            this.objects.add(font);
            fontMap.put(fontname, font);
            return font;
        } else {
            FontType fonttype = metrics.getFontType();

            PDFFontDescriptor pdfdesc = makeFontDescriptor(descriptor);

            PDFFontNonBase14 font = null;
            if (fonttype == FontType.TYPE0) {
                /*
                 * Temporary commented out - customized CMaps
                 * isn't needed until /ToUnicode support is added
                 * PDFCMap cmap = new PDFCMap(++this.objectcount,
                 * "fop-ucs-H",
                 * new PDFCIDSystemInfo("Adobe",
                 * "Identity",
                 * 0));
                 * cmap.addContents();
                 * this.objects.add(cmap);
                 */
                font =
                    (PDFFontNonBase14)PDFFont.createFont(++this.objectcount,
                                                         fontname, fonttype,
                                                         basefont,
                                                         "Identity-H");
            } else {

                font =
                    (PDFFontNonBase14)PDFFont.createFont(++this.objectcount,
                                                         fontname, fonttype,
                                                         basefont, encoding);
            }
            this.objects.add(font);

            font.setDescriptor(pdfdesc);

            if (fonttype == FontType.TYPE0) {
                CIDFont cidMetrics;
                if (metrics instanceof LazyFont) {
                    cidMetrics = (CIDFont)((LazyFont) metrics).getRealFont();
                } else {
                    cidMetrics = (CIDFont)metrics;
                }
                PDFCIDSystemInfo sysInfo =
                    new PDFCIDSystemInfo(cidMetrics.getRegistry(),
                                         cidMetrics.getOrdering(),
                                         cidMetrics.getSupplement());
                PDFCIDFont cidFont =
                    new PDFCIDFont(++this.objectcount, basefont,
                                   cidMetrics.getCIDType(),
                                   cidMetrics.getDefaultWidth(),
                                   cidMetrics.getWidths(), sysInfo,
                                   (PDFCIDFontDescriptor)pdfdesc);
                this.objects.add(cidFont);

                ((PDFFontType0)font).setDescendantFonts(cidFont);
            } else {
                int firstChar = 0;
                int lastChar = 255;
                if (metrics instanceof CustomFont) {
                    CustomFont cf = (CustomFont)metrics;
                    firstChar = cf.getFirstChar();
                    lastChar = cf.getLastChar();
                }
                font.setWidthMetrics(firstChar,
                                     lastChar,
                                     makeArray(metrics.getWidths()));
            }

            fontMap.put(fontname, font);

            return font;
        }
    }


    /**
     * make a /FontDescriptor object
     *
     * @param desc the font descriptor
     * @return the new PDF font descriptor
     */
    public PDFFontDescriptor makeFontDescriptor(FontDescriptor desc) {
        PDFFontDescriptor font = null;

        if (desc.getFontType() == FontType.TYPE0) {
            // CID Font
            font = new PDFCIDFontDescriptor(++this.objectcount,
                                            desc.getFontName(),
                                            desc.getFontBBox(),
                                            desc.getCapHeight(), desc.getFlags(),
                                            desc.getItalicAngle(),
                                            desc.getStemV(), null);
        } else {
            // Create normal FontDescriptor
            font = new PDFFontDescriptor(++this.objectcount, desc.getFontName(),
                                         desc.getAscender(),
                                         desc.getDescender(),
                                         desc.getCapHeight(),
                                         desc.getFlags(),
                                         new PDFRectangle(desc.getFontBBox()),
                                         desc.getStemV(),
                                         desc.getItalicAngle());
        }
        this.objects.add(font);

        // Check if the font is embeddable
        if (desc.isEmbeddable()) {
            PDFStream stream = makeFontFile(this.objectcount + 1, desc);
            if (stream != null) {
                this.objectcount++;
                font.setFontFile(desc.getFontType(), stream);
                this.objects.add(stream);
            }
        }
        return font;
    }

    /**
     * Resolve a URI.
     *
     * @param uri the uri to resolve
     * @throws java.io.FileNotFoundException if the URI could not be resolved
     * @return the InputStream from the URI.
     */
    protected InputStream resolveURI(String uri) 
                throws java.io.FileNotFoundException {
        try {
            /**@todo Temporary hack to compile, improve later */
            return new java.net.URL(uri).openStream();
        } catch (Exception e) {
            throw new java.io.FileNotFoundException(
                "URI could not be resolved (" + e.getMessage() + "): " + uri);
        }
    }

    /**
     * Embeds a font.
     * @param obj PDF object number to use
     * @param desc FontDescriptor of the font.
     * @return PDFStream The embedded font file
     */
    public PDFStream makeFontFile(int obj, FontDescriptor desc) {
        if (desc.getFontType() == FontType.OTHER) {
            throw new IllegalArgumentException("Trying to embed unsupported font type: "
                                                + desc.getFontType());
        } 
        if (!(desc instanceof CustomFont)) {
            throw new IllegalArgumentException(
                      "FontDescriptor must be instance of CustomFont, but is a "
                       + desc.getClass().getName());
        }
        
        CustomFont font = (CustomFont)desc;
        
        InputStream in = null;
        try {
            // Get file first
            if (font.getEmbedFileName() != null) {
                try {
                    in = resolveURI(font.getEmbedFileName());
                } catch (Exception e) {
                    System.out.println("Failed to embed fontfile: "
                                       + font.getEmbedFileName());
                }
            }
    
            // Get resource
            if (in == null && font.getEmbedResourceName() != null) {
                try {
                    in = new java.io.BufferedInputStream(
                            this.getClass().getResourceAsStream(font.getEmbedResourceName()));
                } catch (Exception e) {
                    System.out.println("Failed to embed fontresource: "
                                       + font.getEmbedResourceName());
                }
            }
    
            if (in == null) {
                return null;
            } else {
                try {
                    PDFStream embeddedFont;
                    if (desc.getFontType() == FontType.TYPE0) {
                        MultiByteFont mbfont = (MultiByteFont)font;
                        FontFileReader reader = new FontFileReader(in);

                        TTFSubSetFile subset = new TTFSubSetFile();
        
                        byte[] subsetFont = subset.readFont(reader,
                                             mbfont.getTTCName(), mbfont.getUsedGlyphs());
                        // Only TrueType CID fonts are supported now

                        embeddedFont = new PDFTTFStream(obj, subsetFont.length);
                        ((PDFTTFStream)embeddedFont).setData(subsetFont, subsetFont.length);
                    } else if (desc.getFontType() == FontType.TYPE1) {
                        PFBParser parser = new PFBParser();
                        PFBData pfb = parser.parsePFB(in);
                        embeddedFont = new PDFT1Stream(obj);
                        ((PDFT1Stream)embeddedFont).setData(pfb);
                    } else {
                        byte[] file = StreamUtilities.toByteArray(in, 128000);
                        embeddedFont = new PDFTTFStream(obj, file.length);
                        ((PDFTTFStream)embeddedFont).setData(file, file.length);
                    }
                    embeddedFont.addFilter("flate");
                    embeddedFont.addFilter("ascii-85");
                    return embeddedFont;
                } finally {
                    in.close();
                }
            }
        } catch (IOException ioe) {
            //log.error("Failed to embed font [" + obj + "] "
            //                       + fontName + ": " + ioe.getMessage());
            return (PDFStream) null;
        }
    }

/*
    public PDFStream getFontFile(int i) {
        PDFStream embeddedFont = null;


        return (PDFStream)embeddedFont;
    }


    public PDFStream getFontFile(int i) {
    }

*/
    

    /**
     * make an Array object (ex. Widths array for a font)
     *
     * @param values the int array values
     * @return the PDF Array with the int values
     */
    public PDFArray makeArray(int[] values) {
        PDFArray array = new PDFArray(++this.objectcount, values);
        this.objects.add(array);
        return array;
    }

    /**
     * make an ExtGState for extra graphics options
     * This tries to find a GState that will setup the correct values
     * for the current context. If there is no suitable GState it will
     * create a new one.
     *
     * @param settings the settings required by the caller
     * @param current the current GState of the current PDF context
     * @return a PDF GState, either an existing GState or a new one
     */
    public PDFGState makeGState(Map settings, PDFGState current) {

        // try to locate a gstate that has all the settings
        // or will inherit from the current gstate
        // compare "DEFAULT + settings" with "current + each gstate"

        PDFGState wanted = new PDFGState(0);
        wanted.addValues(PDFGState.DEFAULT);
        wanted.addValues(settings);

        PDFGState poss;
        for (Iterator iter = gstates.iterator(); iter.hasNext();) {
            PDFGState avail = (PDFGState)iter.next();
            poss = new PDFGState(0);
            poss.addValues(current);
            poss.addValues(avail);
            if (poss.equals(wanted)) {
                return avail;
            }
        }

        PDFGState gstate = new PDFGState(++this.objectcount);
        gstate.addValues(settings);
        this.objects.add(gstate);
        gstates.add(gstate);
        return gstate;
    }

    /**
     * Get an image from the image map.
     *
     * @param key the image key to look for
     * @return the image or PDFXObject for the key if found
     */
    public PDFXObject getImage(String key) {
        PDFXObject xObject = (PDFXObject)xObjectsMap.get(key);
        return xObject;
    }

    /**
     * Add an image to the PDF document.
     * This adds an image to the PDF objects.
     * If an image with the same key already exists it will return the
     * old PDFXObject.
     *
     * @param res the PDF resource context to add to, may be null
     * @param img the PDF image to add
     * @return the PDF XObject that references the PDF image data
     */
    public PDFXObject addImage(PDFResourceContext res, PDFImage img) {
        // check if already created
        String key = img.getKey();
        PDFXObject xObject = (PDFXObject)xObjectsMap.get(key);
        if (xObject != null) {
            if (res != null) {
                res.getPDFResources().addXObject(xObject);
            }
            return xObject;
        }

        // setup image
        img.setup(this);
        // create a new XObject
        xObject = new PDFXObject(++this.objectcount, ++this.xObjectCount,
                                 img);
        this.objects.add(xObject);
        this.resources.addXObject(xObject);
        if (res != null) {
            res.getPDFResources().addXObject(xObject);
        }
        this.xObjectsMap.put(key, xObject);
        return xObject;
    }

    /** 
     * Add a form XObject to the PDF document.
     * This adds a Form XObject to the PDF objects.
     * If a Form XObject with the same key already exists it will return the
     * old PDFFormXObject.
     *  
     * @param res the PDF resource context to add to, may be null
     * @param cont the PDF Stream contents of the Form XObject
     * @param formres the PDF Resources for the Form XObject data
     * @param key the key for the object
     * @return the PDF Form XObject that references the PDF data
     */
    public PDFFormXObject addFormXObject(PDFResourceContext res, PDFStream cont,
                                         PDFResources formres, String key) {
        PDFFormXObject xObject;
        xObject = new PDFFormXObject(++this.objectcount, ++this.xObjectCount,
                                 cont, formres.referencePDF());
        this.objects.add(xObject);
        this.resources.addXObject(xObject);
        if (res != null) {
            res.getPDFResources().addXObject(xObject);
        }
        return xObject;
    }

    /**
     * make a /Page object
     *
     * @param resources resources object to use
     * @param pagewidth width of the page in points
     * @param pageheight height of the page in points
     *
     * @return the created /Page object
     */
    public PDFPage makePage(PDFResources resources,
                            int pagewidth, int pageheight) {

        /*
         * create a PDFPage with the next object number, the given
         * resources, contents and dimensions
         */
        PDFPage page = new PDFPage(this, ++this.objectcount, resources,
                                   pagewidth, pageheight);

        /* add it to the list of objects */
        pages.addPage(page);
        return page;
    }

    /**
     * Add a completed page to the PDF document.
     * The page is added to the object list.
     *
     * @param page the page to add
     */
    public void addPage(PDFPage page) {
        /* add it to the list of objects */
        this.objects.add(page);
    }

    private PDFLink findLink(PDFLink compare) {
        return (PDFLink)findPDFObject(links, compare);
    }

    private PDFFileSpec findFileSpec(PDFFileSpec compare) {
        return (PDFFileSpec)findPDFObject(filespecs, compare);
    }

    private PDFGoToRemote findGoToRemote(PDFGoToRemote compare) {
        return (PDFGoToRemote)findPDFObject(gotoremotes, compare);
    }

    private PDFGoTo findGoTo(PDFGoTo compare) {
        return (PDFGoTo)findPDFObject(gotos, compare);
    }

    /**
     * make a link object
     *
     * @param rect   the clickable rectangle
     * @param destination  the destination file
     * @param linkType the link type
     * @param yoffset the yoffset on the page for an internal link
     * @return the PDFLink object created
     */
    public PDFLink makeLink(Rectangle2D rect, String destination,
                            int linkType, float yoffset) {

        //PDFLink linkObject;
        int index;

        PDFLink link = new PDFLink(++this.objectcount, rect);

        if (linkType == PDFLink.EXTERNAL) {
            // check destination
            if (destination.startsWith("http://")) {
                PDFUri uri = new PDFUri(destination);
                link.setAction(uri);
            } else if (destination.endsWith(".pdf")) {    // FileSpec
                PDFGoToRemote remote = getGoToPDFAction(destination, null, -1);
                link.setAction(remote);
            } else if ((index = destination.indexOf(".pdf#page=")) > 0) {
                //String file = destination.substring(0, index + 4);
                int page = Integer.parseInt(destination.substring(index + 10));
                PDFGoToRemote remote = getGoToPDFAction(destination, null, page);
                link.setAction(remote);
            } else if ((index = destination.indexOf(".pdf#dest=")) > 0) {
                //String file = destination.substring(0, index + 4);
                String dest = destination.substring(index + 10);
                PDFGoToRemote remote = getGoToPDFAction(destination, dest, -1);
                link.setAction(remote);
            } else {                               // URI
                PDFUri uri = new PDFUri(destination);
                link.setAction(uri);
            }
        } else {
            // linkType is internal
            String goToReference = getGoToReference(destination, yoffset);
            PDFInternalLink internalLink = new PDFInternalLink(goToReference);
            link.setAction(internalLink);
        }

        PDFLink oldlink = findLink(link);
        if (oldlink == null) {
            links.add(link);
            this.objects.add(link);
        } else {
            this.objectcount--;
            link = oldlink;
        }

        return link;
    }

    /**
     * Create and return a goto pdf document action.
     * This creates a pdf files spec and pdf goto remote action.
     * It also checks available pdf objects so it will not create an
     * object if it already exists.
     *
     * @param file the pdf file name
     * @param dest the remote name destination, may be null
     * @param page the remote page number, -1 means not specified
     * @return the pdf goto remote object
     */
    private PDFGoToRemote getGoToPDFAction(String file, String dest, int page) {
        PDFFileSpec fileSpec = new PDFFileSpec(++this.objectcount, file);
        PDFFileSpec oldspec = findFileSpec(fileSpec);
        if (oldspec == null) {
            filespecs.add(fileSpec);
            this.objects.add(fileSpec);
        } else {
            this.objectcount--;
            fileSpec = oldspec;
        }
        PDFGoToRemote remote;

        if (dest == null && page == -1) {
            remote = new PDFGoToRemote(++this.objectcount, fileSpec);
        } else if (dest != null) {
            remote = new PDFGoToRemote(++this.objectcount, fileSpec, dest);
        } else {
            remote = new PDFGoToRemote(++this.objectcount, fileSpec, page);
        }
        PDFGoToRemote oldremote = findGoToRemote(remote);
        if (oldremote == null) {
            gotoremotes.add(remote);
            this.objects.add(remote);
        } else {
            this.objectcount--;
            remote = oldremote;
        }
        return remote;
    }

    private String getGoToReference(String destination, float yoffset) {
        String goToReference = null;
        PDFGoTo gt = new PDFGoTo(++this.objectcount, destination);
        gt.setYPosition(yoffset);
        PDFGoTo oldgt = findGoTo(gt);
        if (oldgt == null) {
            gotos.add(gt);
            addTrailerObject(gt);
        } else {
            this.objectcount--;
            gt = oldgt;
        }

        goToReference = gt.referencePDF();
        return goToReference;
    }

    /**
     * Add trailer object.
     * Adds an object to the list of trailer objects.
     *
     * @param object the PDF object to add
     */
    public void addTrailerObject(PDFObject object) {
        this.trailerObjects.add(object);
    }

    /**
     * Make an internal link.
     *
     * @param rect the hotspot position in absolute coordinates
     * @param page the target page reference value
     * @param dest the position destination
     * @return the new PDF link object
     */
    public PDFLink makeLink(Rectangle2D rect, String page, String dest) {
        PDFLink link = new PDFLink(++this.objectcount, rect);
        this.objects.add(link);

        PDFGoTo gt = new PDFGoTo(++this.objectcount, page);
        gt.setDestination(dest);
        addTrailerObject(gt);
        PDFInternalLink internalLink = new PDFInternalLink(gt.referencePDF());
        link.setAction(internalLink);

        return link;
    }

    /**
     * Ensure there is room in the locations xref for the number of
     * objects that have been created.
     */
    private void prepareLocations() {
        while (location.size() < objectcount) {
            location.add(LOCATION_PLACEHOLDER);
        }
    }

    /**
     * make a stream object
     *
     * @param type the type of stream to be created
     * @param add if true then the stream will be added immediately
     * @return the stream object created
     */
    public PDFStream makeStream(String type, boolean add) {

        /*
         * create a PDFStream with the next object number and add it
         *
         * to the list of objects
         */
        PDFStream obj = new PDFStream(++this.objectcount);
        obj.addDefaultFilters(filterMap, type);

        if (add) {
            this.objects.add(obj);
        }
        return obj;
    }

    /**
     * add a stream object
     *
     * @param obj the PDF Stream to add to this document
     */
    public void addStream(PDFStream obj) {
        this.objects.add(obj);
    }

    /**
     * make an annotation list object
     *
     * @return the annotation list object created
     */
    public PDFAnnotList makeAnnotList() {

        /*
         * create a PDFAnnotList with the next object number and add it
         * to the list of objects
         */
        PDFAnnotList obj = new PDFAnnotList(++this.objectcount);
        return obj;
    }

    /**
     * Add an annotation list object to the pdf document
     *
     * @param obj the annotation list to add
     */
    public void addAnnotList(PDFAnnotList obj) {
        this.objects.add(obj);
    }

    /**
     * Get the root Outlines object. This method does not write
     * the outline to the PDF document, it simply creates a
     * reference for later.
     *
     * @return the PDF Outline root object
     */
    public PDFOutline getOutlineRoot() {
        if (outlineRoot != null) {
            return outlineRoot;
        }

        outlineRoot = new PDFOutline(++this.objectcount, null, null);
        addTrailerObject(outlineRoot);
        root.setRootOutline(outlineRoot);
        return outlineRoot;
    }

    /**
     * Make an outline object and add it to the given outline
     *
     * @param parent parent PDFOutline object which may be null
     * @param label the title for the new outline object
     * @param destination the reference string for the action to go to
     * @param yoffset the yoffset on the destination page
     * @return the new PDF outline object
     */
    public PDFOutline makeOutline(PDFOutline parent, String label,
                                  String destination, float yoffset) {
        String goToRef = getGoToReference(destination, yoffset);

        PDFOutline obj = new PDFOutline(++this.objectcount, label, goToRef);

        if (parent != null) {
            parent.addOutline(obj);
        }
        this.objects.add(obj);
        return obj;
    }

    /**
     * get the /Resources object for the document
     *
     * @return the /Resources object
     */
    public PDFResources getResources() {
        return this.resources;
    }

    /**
     * write the entire document out
     *
     * @param stream the OutputStream to output the document to
     * @throws IOException if there is an exception writing to the output stream
     */
    public void output(OutputStream stream) throws IOException {

        prepareLocations();

        for (int count = 0; count < this.objects.size(); count++) {
            /* retrieve the object with the current number */
            PDFObject object = (PDFObject)this.objects.get(count);

            /*
             * add the position of this object to the list of object
             * locations
             */
            location.set(object.getNumber() - 1,
                         new Integer(this.position));

            /*
             * output the object and increment the character position
             * by the object's length
             */
            this.position += object.output(stream);
        }

        this.objects.clear();
    }

    /**
     * write the PDF header <P>
     *
     * This method must be called prior to formatting
     * and outputting AreaTrees.
     *
     * @param stream the OutputStream to write the header to
     * @throws IOException if there is an exception writing to the output stream
     */
    public void outputHeader(OutputStream stream)
    throws IOException {
        this.position = 0;

        byte[] pdf = ("%PDF-" + PDF_VERSION + "\n").getBytes();
        stream.write(pdf);
        this.position += pdf.length;

        // output a binary comment as recommended by the PDF spec (3.4.1)
        byte[] bin = {
            (byte)'%', (byte)0xAA, (byte)0xAB, (byte)0xAC, (byte)0xAD,
            (byte)'\n'
        };
        stream.write(bin);
        this.position += bin.length;
    }

    /**
     * write the trailer
     *
     * @param stream the OutputStream to write the trailer to
     * @throws IOException if there is an exception writing to the output stream
     */
    public void outputTrailer(OutputStream stream)
    throws IOException {
        output(stream);
        for (int count = 0; count < trailerObjects.size(); count++) {
            PDFObject o = (PDFObject) trailerObjects.get(count);
            this.location.set(o.getNumber() - 1,
                              new Integer(this.position));
            this.position += o.output(stream);
        }
        /* output the xref table and increment the character position
          by the table's length */
        this.position += outputXref(stream);

        /* construct the trailer */
        String pdf = "trailer\n" + "<<\n"
                     + "/Size " + (this.objectcount + 1) + "\n"
                     + "/Root " + this.root.number + " "
                     + this.root.generation + " R\n" + "/Info "
                     + this.info.number + " " + this.info.generation
                     + " R\n" + ">>\n" + "startxref\n" + this.xref
                     + "\n" + "%%EOF\n";

        /* write the trailer */
        stream.write(pdf.getBytes());
    }

    /**
     * write the xref table
     *
     * @param stream the OutputStream to write the xref table to
     * @return the number of characters written
     */
    private int outputXref(OutputStream stream) throws IOException {

        /* remember position of xref table */
        this.xref = this.position;

        /* construct initial part of xref */
        StringBuffer pdf = new StringBuffer("xref\n0 "
                                            + (this.objectcount + 1)
                                            + "\n0000000000 65535 f \n");

        for (int count = 0; count < this.location.size(); count++) {
            String x = this.location.get(count).toString();

            /* contruct xref entry for object */
            String padding = "0000000000";
            String loc = padding.substring(x.length()) + x;

            /* append to xref table */
            pdf = pdf.append(loc + " 00000 n \n");
        }

        /* write the xref table and return the character length */
        byte[] pdfBytes = pdf.toString().getBytes();
        stream.write(pdfBytes);
        return pdfBytes.length;
    }

}

