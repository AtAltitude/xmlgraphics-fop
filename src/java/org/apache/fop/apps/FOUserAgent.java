/*
 * Copyright 1999-2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.apps;

// Java
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

// avalon configuration
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

// commons logging
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// FOP
import org.apache.fop.Version;
import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.hyphenation.HyphenationTreeResolver;
import org.apache.fop.layoutmgr.LayoutManagerMaker;
import org.apache.fop.pdf.PDFEncryptionParams;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.RendererFactory;
import org.apache.fop.render.XMLHandlerRegistry;

/**
 * The User Agent for fo.
 * This user agent is used by the processing to obtain user configurable
 * options.
 * <p>
 * Renderer specific extensions (that do not produce normal areas on
 * the output) will be done like so:
 * <br>
 * The extension will create an area, custom if necessary
 * <br>
 * this area will be added to the user agent with a key
 * <br>
 * the renderer will know keys for particular extensions
 * <br>
 * eg. bookmarks will be held in a special hierarchical area representing
 * the title and bookmark structure
 * <br>
 * These areas may contain resolvable areas that will be processed
 * with other resolvable areas
 */
public class FOUserAgent {

    /** Defines the default source resolution (72dpi) for FOP */
    public static final float DEFAULT_SOURCE_RESOLUTION = 72.0f; //dpi
    /** Defines the default target resolution (72dpi) for FOP */
    public static final float DEFAULT_TARGET_RESOLUTION = 72.0f; //dpi
    /** Defines the default page-height */
    public static final String DEFAULT_PAGE_HEIGHT = "11in";
    /** Defines the default page-width */
    public static final String DEFAULT_PAGE_WIDTH = "8.26in";
    
    /** Factory for Renderers and FOEventHandlers */
    private RendererFactory rendererFactory = new RendererFactory();
    
    /** Registry for XML handlers */
    private XMLHandlerRegistry xmlHandlers = new XMLHandlerRegistry();
    
    /** The resolver for user-supplied hyphenation patterns */
    private HyphenationTreeResolver hyphResolver;
    
    /** The base URL for all URL resolutions, especially for external-graphics */
    private String baseURL;
    
    /** The base URL for all font URL resolutions */
    private String fontBaseURL;
    
    /** A user settable URI Resolver */
    private URIResolver uriResolver = null;
    /** Our default resolver if none is set */
    private URIResolver foURIResolver = new FOURIResolver();
    
    private PDFEncryptionParams pdfEncryptionParams;
    private float sourceResolution = DEFAULT_SOURCE_RESOLUTION;
    private float targetResolution = DEFAULT_TARGET_RESOLUTION;
    private String pageHeight = DEFAULT_PAGE_HEIGHT;
    private String pageWidth = DEFAULT_PAGE_WIDTH;
    private Map rendererOptions = new java.util.HashMap();
    private File outputFile = null;
    private Renderer rendererOverride = null;
    private FOEventHandler foEventHandlerOverride = null;
    private LayoutManagerMaker lmMakerOverride = null;
    /* user configuration */
    private Configuration userConfig = null;
    private Log log = LogFactory.getLog("FOP");
    
    /* FOP has the ability, for some FO's, to continue processing even if the
     * input XSL violates that FO's content model.  This is the default  
     * behavior for FOP.  However, this flag, if set, provides the user the
     * ability for FOP to halt on all content model violations if desired.   
     */ 
    private boolean strictValidation = true;
    
    /** @see #setBreakIndentInheritanceOnReferenceAreaBoundary(boolean) */
    private boolean breakIndentInheritanceOnReferenceAreaBoundary = false;

    /** Allows enabling kerning on the base 14 fonts, default is false */
    private boolean enableBase14Kerning = false;
    
    /* Additional fo.ElementMapping subclasses set by user */
    private List additionalElementMappings = null;

    /** Producer:  Metadata element for the system/software that produces
     * the document. (Some renderers can store this in the document.)
     */
    protected String producer = "Apache FOP Version " + Version.getVersion();

    /** Creator:  Metadata element for the user that created the
     * document. (Some renderers can store this in the document.)
     */
    protected String creator = null;

    /** Creation Date:  Override of the date the document was created. 
     * (Some renderers can store this in the document.)
     */
    protected Date creationDate = null;
    
    /** Author of the content of the document. */
    protected String author = null;
    /** Title of the document. */
    protected String title = null;
    /** Set of keywords applicable to this document. */
    protected String keywords = null;
    
    /**
     * Add the element mapping with the given class name.
     * @param elementMapping the class name representing the element mapping.
     */
    public void addElementMapping(ElementMapping elementMapping) {
        if (additionalElementMappings == null) {
            additionalElementMappings = new java.util.ArrayList();
        }
        additionalElementMappings.add(elementMapping);
    }

    /**
     * Returns the List of user-added ElementMapping class names
     * @return List of Strings holding ElementMapping names.
     */
    public List getAdditionalElementMappings() {
        return additionalElementMappings;
    }

    /**
     * Sets an explicit renderer to use which overrides the one defined by the 
     * render type setting.  
     * @param renderer the Renderer instance to use
     */
    public void setRendererOverride(Renderer renderer) {
        this.rendererOverride = renderer;
    }

    /**
     * Returns the overriding Renderer instance, if any.
     * @return the overriding Renderer or null
     */
    public Renderer getRendererOverride() {
        return rendererOverride;
    }

    /**
     * Sets an explicit FOEventHandler instance which overrides the one
     * defined by the render type setting.  
     * @param handler the FOEventHandler instance
     */
    public void setFOEventHandlerOverride(FOEventHandler handler) {
        this.foEventHandlerOverride = handler;
    }

    /**
     * Returns the overriding FOEventHandler instance, if any.
     * @return the overriding FOEventHandler or null
     */
    public FOEventHandler getFOEventHandlerOverride() {
        return this.foEventHandlerOverride;
    }

    /**
     * Activates strict XSL content model validation for FOP
     * Default is false (FOP will continue processing where it can)
     * @param validateStrictly true to turn on strict validation
     */
    public void setStrictValidation(boolean validateStrictly) {
        this.strictValidation = validateStrictly;
    }

    /**
     * Returns whether FOP is strictly validating input XSL
     * @return true of strict validation turned on, false otherwise
     */
    public boolean validateStrictly() {
        return strictValidation;
    }

    /**
     * @return true if the indent inheritance should be broken when crossing reference area 
     *         boundaries (for more info, see the javadoc for the relative member variable)
     */
    public boolean isBreakIndentInheritanceOnReferenceAreaBoundary() {
        return breakIndentInheritanceOnReferenceAreaBoundary;
    }

    /**
     * Controls whether to enable a feature that breaks indent inheritance when crossing
     * reference area boundaries.
     * <p>
     * This flag controls whether FOP will enable special code that breaks property
     * inheritance for start-indent and end-indent when the evaluation of the inherited
     * value would cross a reference area. This is described under
     * http://wiki.apache.org/xmlgraphics-fop/IndentInheritance as is intended to
     * improve interoperability with commercial FO implementations and to produce
     * results that are more in line with the expectation of unexperienced FO users.
     * Note: Enabling this features violates the XSL specification!
     * @param value true to enable the feature
     */
    public void setBreakIndentInheritanceOnReferenceAreaBoundary(boolean value) {
        this.breakIndentInheritanceOnReferenceAreaBoundary = value;
    }
    
    /** @return true if kerning on base 14 fonts is enabled */
    public boolean isBase14KerningEnabled() {
        return this.enableBase14Kerning;
    }
    
    /**
     * Controls whether kerning is activated on base 14 fonts.
     * @param value true if kerning should be activated
     */
    public void setBase14KerningEnabled(boolean value) {
        this.enableBase14Kerning = value;
    }
    
    /**
     * Sets an explicit LayoutManagerMaker instance which overrides the one
     * defined by the AreaTreeHandler.
     * @param lmMaker the LayoutManagerMaker instance
     */
    public void setLayoutManagerMakerOverride(LayoutManagerMaker lmMaker) {
        this.lmMakerOverride = lmMaker;
    }

    /**
     * Returns the overriding LayoutManagerMaker instance, if any.
     * @return the overriding LayoutManagerMaker or null
     */
    public LayoutManagerMaker getLayoutManagerMakerOverride() {
        return this.lmMakerOverride;
    }

    /**
     * Sets the producer of the document.  
     * @param producer source of document
     */
    public void setProducer(String producer) {
        this.producer = producer;
    }

    /**
     * Returns the producer of the document
     * @return producer name
     */
    public String getProducer() {
        return producer;
    }

    /**
     * Sets the creator of the document.  
     * @param creator of document
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * Returns the creator of the document
     * @return creator name
     */
    public String getCreator() {
        return creator;
    }

    /**
     * Sets the creation date of the document.  
     * @param creationDate date of document
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Returns the creation date of the document
     * @return creation date of document
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the author of the document.  
     * @param author of document
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Returns the author of the document
     * @return author name
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the title of the document. This will override any title coming from
     * an fo:title element.  
     * @param title of document
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the title of the document
     * @return title name
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the keywords for the document.  
     * @param keywords for the document
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
     * Returns the keywords for the document
     * @return the keywords
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Returns the renderer options
     * @return renderer options
     */
    public Map getRendererOptions() {
        return rendererOptions;
    }

    /**
     * Set the user configuration.
     * @param userConfig configuration
     */
    public void setUserConfig(Configuration userConfig) {
        this.userConfig = userConfig;
        try {
            initUserConfig();
        } catch (ConfigurationException cfge) {
            log.error("Error initializing User Agent configuration: "
                    + cfge.getMessage());
        }
    }

    /**
     * Get the user configuration.
     * @return the user configuration
     */
    public Configuration getUserConfig() {
        return userConfig;
    }
    
    /**
     * Initializes user agent settings from the user configuration
     * file, if present: baseURL, resolution, default page size,...
     * 
     * @throws ConfigurationException when there is an entry that 
     *          misses the required attribute
     */
    public void initUserConfig() throws ConfigurationException {
        log.debug("Initializing User Agent Configuration");
        setBaseURL(getBaseURLfromConfig("base"));
        setFontBaseURL(getBaseURLfromConfig("font-base"));
        final String hyphBase = getBaseURLfromConfig("hyphenation-base");
        if (hyphBase != null) {
            this.hyphResolver = new HyphenationTreeResolver() {
                public Source resolve(String href) {
                    return resolveURI(href, hyphBase);
                }
            };
        }
        if (userConfig.getChild("source-resolution", false) != null) {
            this.sourceResolution 
                = userConfig.getChild("source-resolution").getValueAsFloat(
                        DEFAULT_SOURCE_RESOLUTION);
            log.info("Source resolution set to: " + sourceResolution 
                    + "dpi (px2mm=" + getSourcePixelUnitToMillimeter() + ")");
        }
        if (userConfig.getChild("target-resolution", false) != null) {
            this.targetResolution 
                = userConfig.getChild("target-resolution").getValueAsFloat(
                        DEFAULT_TARGET_RESOLUTION);
            log.info("Target resolution set to: " + targetResolution 
                    + "dpi (px2mm=" + getTargetPixelUnitToMillimeter() + ")");
        }
        if (userConfig.getChild("strict-validation", false) != null) {
            this.strictValidation = userConfig.getChild("strict-validation").getValueAsBoolean();
        }
        if (userConfig.getChild("break-indent-inheritance", false) != null) {
            this.breakIndentInheritanceOnReferenceAreaBoundary 
                = userConfig.getChild("break-indent-inheritance").getValueAsBoolean();
        }
        Configuration pageConfig = userConfig.getChild("default-page-settings");
        if (pageConfig.getAttribute("height", null) != null) {
            setPageHeight(pageConfig.getAttribute("height"));
            log.info("Default page-height set to: " + pageHeight);
        }
        if (pageConfig.getAttribute("width", null) != null) {
            setPageWidth(pageConfig.getAttribute("width"));
            log.info("Default page-width set to: " + pageWidth);
        }
    }

    private String getBaseURLfromConfig(String name) {
        if (userConfig.getChild(name, false) != null) {
            try {
                String cfgBaseDir = userConfig.getChild(name).getValue(null);
                if (cfgBaseDir != null) {
                    File dir = new File(cfgBaseDir);
                    if (dir.isDirectory()) {
                        cfgBaseDir = dir.toURL().toExternalForm(); 
                    } else {
                        //The next statement is for validation only
                        new URL(cfgBaseDir);
                    }
                }
                log.info(name + " set to: " + cfgBaseDir);
                return cfgBaseDir;
            } catch (MalformedURLException mue) {
                log.error("Base URL in user config is malformed!");
            }
        }
        return null;
    }
    
    /**
     * Returns the configuration subtree for a specific renderer.
     * @param mimeType MIME type of the renderer
     * @return the requested configuration subtree, null if there's no configuration
     */
    public Configuration getUserRendererConfig (String mimeType) {

        if (userConfig == null || mimeType == null) {
            return null;
        }

        Configuration userRendererConfig = null;

        Configuration[] cfgs
            = userConfig.getChild("renderers").getChildren("renderer");
        for (int i = 0; i < cfgs.length; ++i) {
            Configuration cfg = cfgs[i];
            try {
                if (cfg.getAttribute("mime").equals(mimeType)) {
                    userRendererConfig = cfg;
                    break;
                }
            } catch (ConfigurationException e) {
                // silently pass over configurations without mime type
            }
        }
        log.debug((userRendererConfig == null ? "No u" : "U")
                  + "ser configuration found for MIME type " + mimeType);
        return userRendererConfig;
    }

    /**
     * Sets the base URL.
     * @param baseURL base URL
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * Returns the base URL.
     * @return the base URL
     */
    public String getBaseURL() {
        return this.baseURL;
    }

    /**
     * Sets the font base URL.
     * @param fontBaseURL font base URL
     */
    public void setFontBaseURL(String fontBaseURL) {
        this.fontBaseURL = fontBaseURL;
    }

    /**
     * Returns the font base URL.
     * @return the font base URL
     */
    public String getFontBaseURL() {
        return this.fontBaseURL != null ? this.fontBaseURL : this.baseURL;
    }

    /**
     * Sets the URI Resolver.
     * @param uriResolver the new URI resolver
     */
    public void setURIResolver(URIResolver uriResolver) {
        this.uriResolver = uriResolver;
    }

    /**
     * Returns the URI Resolver.
     * @return the URI Resolver
     */
    public URIResolver getURIResolver() {
        return this.uriResolver;
    }

    /**
     * Returns the parameters for PDF encryption.
     * @return the PDF encryption parameters, null if not applicable
     */
    public PDFEncryptionParams getPDFEncryptionParams() {
        return pdfEncryptionParams;
    }

    /**
     * Sets the parameters for PDF encryption.
     * @param pdfEncryptionParams the PDF encryption parameters, null to
     * disable PDF encryption
     */
    public void setPDFEncryptionParams(PDFEncryptionParams pdfEncryptionParams) {
        this.pdfEncryptionParams = pdfEncryptionParams;
    }


    /**
     * Attempts to resolve the given URI.
     * Will use the configured resolver and if not successful fall back
     * to the default resolver.
     * @param uri URI to access
     * @return A {@link javax.xml.transform.Source} object, or null if the URI
     * cannot be resolved. 
     * @see org.apache.fop.apps.FOURIResolver
     */
    public Source resolveURI(String uri) {
        return resolveURI(uri, getBaseURL());
    }

    /**
     * Attempts to resolve the given URI.
     * Will use the configured resolver and if not successful fall back
     * to the default resolver.
     * @param uri URI to access
     * @param baseURL the base url to resolve against
     * @return A {@link javax.xml.transform.Source} object, or null if the URI
     * cannot be resolved. 
     * @see org.apache.fop.apps.FOURIResolver
     */
    public Source resolveURI(String uri, String baseURL) {
        Source source = null;
        if (uriResolver != null) {
            try {
                source = uriResolver.resolve(uri, baseURL);
            } catch (TransformerException te) {
                log.error("Attempt to resolve URI '" + uri + "' failed: ", te);
            }
        }
        if (source == null) {
            // URI Resolver not configured or returned null, use default resolver
            try {
                source = foURIResolver.resolve(uri, baseURL);
            } catch (TransformerException te) {
                log.error("Attempt to resolve URI '" + uri + "' failed: ", te);
            }
        }
        return source;
    }

    /**
     * Sets the output File.
     * @param f the output File
     */
    public void setOutputFile(File f) {
        this.outputFile = f;
    }

    /**
     * Gets the output File.
     * @return the output File
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Returns the conversion factor from pixel units to millimeters. This
     * depends on the desired source resolution.
     * @return float conversion factor
     * @see getSourceResolution()
     */
    public float getSourcePixelUnitToMillimeter() {
        return 25.4f / this.sourceResolution; 
    }
    
    /**
     * Returns the conversion factor from pixel units to millimeters. This
     * depends on the desired target resolution.
     * @return float conversion factor
     * @see getTargetResolution()
     */
    public float getTargetPixelUnitToMillimeter() {
        return 25.4f / this.targetResolution; 
    }
    
    /** @return the resolution for resolution-dependant input */
    public float getSourceResolution() {
        return this.sourceResolution;
    }

    /** @return the resolution for resolution-dependant output */
    public float getTargetResolution() {
        return this.targetResolution;
    }

    /**
     * Sets the source resolution in dpi. This value is used to interpret the pixel size
     * of source documents like SVG images and bitmap images without resolution information.
     * @param dpi resolution in dpi
     */
    public void setSourceResolution(int dpi) {
        this.sourceResolution = dpi;
    }
    
    /**
     * Sets the target resolution in dpi. This value defines the target resolution of
     * bitmap images generated by the bitmap renderers (such as the TIFF renderer) and of
     * bitmap images generated by filter effects in Apache Batik.
     * @param dpi resolution in dpi
     */
    public void setTargetResolution(int dpi) {
        this.targetResolution = dpi;
    }
    
    /**
     * Gets the default page-height to use as fallback,
     * in case page-height="auto"
     * 
     * @return the page-height, as a String
     */
    public String getPageHeight() {
        return this.pageHeight;
    }
    
    /**
     * Sets the page-height to use as fallback, in case
     * page-height="auto"
     * 
     * @param pageHeight    page-height as a String
     */
    public void setPageHeight(String pageHeight) {
        this.pageHeight = pageHeight;
    }
    
    /**
     * Gets the default page-width to use as fallback,
     * in case page-width="auto"
     * 
     * @return the page-width, as a String
     */
    public String getPageWidth() {
        return this.pageWidth;
    }
    
    /**
     * Sets the page-width to use as fallback, in case
     * page-width="auto"
     * 
     * @param pageWidth    page-width as a String
     */
    public void setPageWidth(String pageWidth) {
        this.pageWidth = pageWidth;
    }
    
    /**
     * If to create hot links to footnotes and before floats.
     * @return True if hot links should be created
     */
    /* TODO This method is never referenced!
    public boolean linkToFootnotes() {
        return true;
    }*/

    /**
     * @return the RendererFactory
     */
    public RendererFactory getRendererFactory() {
        return this.rendererFactory;
    }

    /**
     * @return the XML handler registry
     */
    public XMLHandlerRegistry getXMLHandlerRegistry() {
        return this.xmlHandlers;
    }

    /** @return the HyphenationTreeResolver for resolving user-supplied hyphenation patterns. */
    public HyphenationTreeResolver getHyphenationTreeResolver() {
        return this.hyphResolver;
    }
    
}

