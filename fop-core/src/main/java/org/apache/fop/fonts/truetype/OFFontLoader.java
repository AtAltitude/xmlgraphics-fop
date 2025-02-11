/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

package org.apache.fop.fonts.truetype;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.List;

import org.apache.commons.io.IOUtils;

import org.apache.fop.apps.io.InternalResourceResolver;
import org.apache.fop.fonts.CFFToType1Font;
import org.apache.fop.fonts.CIDFontType;
import org.apache.fop.fonts.CMapSegment;
import org.apache.fop.fonts.CustomFont;
import org.apache.fop.fonts.EmbeddingMode;
import org.apache.fop.fonts.EncodingMode;
import org.apache.fop.fonts.FontLoader;
import org.apache.fop.fonts.FontType;
import org.apache.fop.fonts.MultiByteFont;
import org.apache.fop.fonts.NamedCharacter;
import org.apache.fop.fonts.SingleByteFont;
import org.apache.fop.fonts.truetype.OpenFont.PostScriptVersion;
import org.apache.fop.util.HexEncoder;

/**
 * Loads a TrueType font into memory directly from the original font file.
 */
public class OFFontLoader extends FontLoader {

    private MultiByteFont multiFont;
    private SingleByteFont singleFont;
    private final String subFontName;
    private EncodingMode encodingMode;
    private EmbeddingMode embeddingMode;
    private boolean simulateStyle;
    private boolean embedAsType1;
    private boolean useSVG;

    /**
     * Default constructor
     * @param fontFileURI the URI representing the font file
     * @param resourceResolver the resource resolver for font URI resolution
     */
    public OFFontLoader(URI fontFileURI, InternalResourceResolver resourceResolver) {
        this(fontFileURI, null, true, EmbeddingMode.AUTO, EncodingMode.AUTO, true, true, resourceResolver, false,
                false, true);
    }

    /**
     * Additional constructor for TrueType Collections.
     * @param fontFileURI the URI representing the font file
     * @param subFontName the sub-fontname of a font in a TrueType Collection (or null for normal
     *          TrueType fonts)
     * @param embedded indicates whether the font is embedded or referenced
     * @param embeddingMode the embedding mode of the font
     * @param encodingMode the requested encoding mode
     * @param useKerning true to enable loading kerning info if available, false to disable
     * @param useAdvanced true to enable loading advanced info if available, false to disable
     * @param resolver the FontResolver for font URI resolution
     * @param simulateStyle Determines whether to simulate font styles if a font does not support those by default.
     */
    public OFFontLoader(URI fontFileURI, String subFontName, boolean embedded, EmbeddingMode embeddingMode,
        EncodingMode encodingMode, boolean useKerning, boolean useAdvanced, InternalResourceResolver resolver,
        boolean simulateStyle, boolean embedAsType1, boolean useSVG) {
        super(fontFileURI, embedded, useKerning, useAdvanced, resolver);
        this.subFontName = subFontName;
        this.encodingMode = encodingMode;
        this.embeddingMode = embeddingMode;
        this.simulateStyle = simulateStyle;
        this.embedAsType1 = embedAsType1;
        this.useSVG = useSVG;
        if (this.encodingMode == EncodingMode.AUTO) {
            this.encodingMode = EncodingMode.CID; //Default to CID mode for TrueType
        }
        if (this.embeddingMode == EmbeddingMode.AUTO) {
            this.embeddingMode = EmbeddingMode.SUBSET;
        }
    }

    /** {@inheritDoc} */
    protected void read() throws IOException {
        read(this.subFontName);
    }

    /**
     * Reads a TrueType font.
     * @param ttcFontName the TrueType sub-font name of TrueType Collection (may be null for
     *    normal TrueType fonts)
     * @throws IOException if an I/O error occurs
     */
    private void read(String ttcFontName) throws IOException {
		/*if (ttcFontName == null) {
			//If no TTC font name is defined, load all available fonts
			InputStream in = resourceResolver.getResource(this.fontFileURI);
			try {
				FontFileReader reader = new FontFileReader(in);
				String header = readHeader(reader);
				boolean isCFF = header.equals("OTTO");
				OpenFont otf = (isCFF) ? new OTFFile(useKerning, useAdvanced) : new TTFFile(useKerning, useAdvanced);
				
				reader.seekSet(0);
				//Get list of font names
				final List<String> ttcFontNames = otf.getTTCnames(reader);
				
				//Reset seeker to end of header
				reader.seekSet(4);
				
				//Load font
				if (ttcFontNames == null) {
					//Not a TTC font, just load the font as-is
					boolean supported = otf.readFont(reader, header, ttcFontName);
					if (!supported) {
						throw new IOException("The font does not have a Unicode cmap table: " + fontFileURI);
					}
					buildFont(otf, ttcFontName);
					loaded = true;
				} else {
					//TTC font, load all
					//TODO: Actually load all fonts contained in the package
					boolean supported = otf.readFont(reader, header, ttcFontNames.get(0));
					if (!supported) {
						throw new IOException("The font does not have a Unicode cmap table: " + fontFileURI);
					}
					buildFont(otf, ttcFontName);
					loaded = true;
				}
			} finally {
				IOUtils.closeQuietly(in);
			}
		} else {*/
			//If a TTC font name is defined, use default behavior
			InputStream in = resourceResolver.getResource(this.fontFileURI);
			try {
				FontFileReader reader = new FontFileReader(in);
				String header = readHeader(reader);
				boolean isCFF = header.equals("OTTO");
				OpenFont otf = (isCFF) ? new OTFFile(useKerning, useAdvanced) : new TTFFile(useKerning, useAdvanced);
				boolean supported = otf.readFont(reader, header, ttcFontName);
				if (!supported) {
					throw new IOException("The font does not have a Unicode cmap table: " + fontFileURI);
				}
				buildFont(otf, ttcFontName);
				loaded = true;
			} finally {
				IOUtils.closeQuietly(in);
			}
		//}
    }

    public static String readHeader(FontFileReader fontFile) throws IOException {
        if (fontFile != null) {
            fontFile.seekSet(0);
            return fontFile.readTTFString(4); // TTF_FIXED_SIZE (4 bytes)
        }
        return null;
    }

    private void buildFont(OpenFont otf, String ttcFontName) {
        boolean isCid = this.embedded;
        if (this.encodingMode == EncodingMode.SINGLE_BYTE) {
            isCid = false;
        }

        CustomFont font;
        if (isCid) {
            if (otf instanceof OTFFile && embedAsType1) {
                multiFont = new CFFToType1Font(resourceResolver, embeddingMode);
            } else {
                multiFont = new MultiByteFont(resourceResolver, embeddingMode);
            }
            multiFont.setIsOTFFile(otf instanceof OTFFile);
            returnFont = multiFont;
            multiFont.setTTCName(ttcFontName);
            font = multiFont;
        } else {
            singleFont = new SingleByteFont(resourceResolver, embeddingMode);
            returnFont = singleFont;
            font = singleFont;
        }
        font.setSimulateStyle(simulateStyle);

        returnFont.setFontURI(fontFileURI);
        if (!otf.getEmbedFontName().equals("")) {
            returnFont.setFontName(otf.getEmbedFontName());
        } else {
            returnFont.setFontName(otf.getPostScriptName());
        }
        returnFont.setFullName(otf.getFullName());
        returnFont.setFamilyNames(otf.getFamilyNames());
        returnFont.setFontSubFamilyName(otf.getSubFamilyName());
        returnFont.setCapHeight(otf.getCapHeight());
        returnFont.setXHeight(otf.getXHeight());
        returnFont.setAscender(otf.getLowerCaseAscent());
        returnFont.setDescender(otf.getLowerCaseDescent());
        returnFont.setFontBBox(otf.getFontBBox());
        returnFont.setUnderlinePosition(otf.getUnderlinePosition() - otf.getUnderlineThickness() / 2);
        returnFont.setUnderlineThickness(otf.getUnderlineThickness());
        returnFont.setStrikeoutPosition(otf.getStrikeoutPosition() - otf.getStrikeoutThickness() / 2);
        returnFont.setStrikeoutThickness(otf.getStrikeoutThickness());
        returnFont.setFlags(otf.getFlags());
        returnFont.setStemV(Integer.parseInt(otf.getStemV())); //not used for TTF
        returnFont.setItalicAngle(Integer.parseInt(otf.getItalicAngle()));
        returnFont.setMissingWidth(0);
        returnFont.setWeight(otf.getWeightClass());
        if (isCid) {
            if (otf instanceof OTFFile) {
                if (((OTFFile) otf).isType1() && embeddingMode == EmbeddingMode.SUBSET && !embedAsType1) {
                    multiFont.setFontType(FontType.TYPE1C);
                    copyGlyphMetricsSingleByte(otf);
                }
                multiFont.setCIDType(CIDFontType.CIDTYPE0);
            } else {
                multiFont.setCIDType(CIDFontType.CIDTYPE2);
            }
            multiFont.setWidthArray(otf.getWidths());
            multiFont.setBBoxArray(otf.getBoundingBoxes());
        } else {
            singleFont.setFontType(FontType.TRUETYPE);
            singleFont.setEncoding(otf.getCharSetName());
            returnFont.setFirstChar(otf.getFirstChar());
            returnFont.setLastChar(otf.getLastChar());
            singleFont.setTrueTypePostScriptVersion(otf.getPostScriptVersion());
            copyGlyphMetricsSingleByte(otf);
        }
        returnFont.setCMap(getCMap(otf));
        if (useSVG) {
            returnFont.setSVG(otf.svgs);
        }

        if (otf.getKerning() != null && useKerning) {
            copyKerning(otf, isCid);
        }
        if (useAdvanced) {
            copyAdvanced(otf);
        }
        if (this.embedded) {
            if (otf.isEmbeddable()) {
                returnFont.setEmbedURI(this.fontFileURI);
            } else {
                String msg = "The font " + this.fontFileURI + " is not embeddable due to a"
                        + " licensing restriction.";
                throw new RuntimeException(msg);
            }
        }
    }

    private CMapSegment[] getCMap(OpenFont otf) {
        CMapSegment[] array = new CMapSegment[otf.getCMaps().size()];
        return otf.getCMaps().toArray(array);
    }

    private void copyGlyphMetricsSingleByte(OpenFont otf) {
        int[] wx = otf.getWidths();
        Rectangle[] bboxes = otf.getBoundingBoxes();
        if (singleFont != null) {
            for (int i = singleFont.getFirstChar(); i <= singleFont.getLastChar(); i++) {
                singleFont.setWidth(i, otf.getCharWidth(i));
                int[] bbox = otf.getBBox(i);
                singleFont.setBoundingBox(i,
                        new Rectangle(bbox[0], bbox[1], bbox[2] - bbox[0], bbox[3] - bbox[1]));
            }
        }
        for (CMapSegment segment : otf.getCMaps()) {
            if (segment.getUnicodeStart() < 0xFFFE) {
                for (char u = (char)segment.getUnicodeStart(); u <= segment.getUnicodeEnd(); u++) {
                    int codePoint = 0;
                    if (singleFont != null) {
                        codePoint = singleFont.getEncoding().mapChar(u);
                    }
                    if (codePoint <= 0) {
                        int glyphIndex = segment.getGlyphStartIndex() + u - segment.getUnicodeStart();
                        String glyphName = otf.getGlyphName(glyphIndex);
                        if (glyphName.length() == 0 && otf.getPostScriptVersion() != PostScriptVersion.V2) {
                            glyphName = "u" + HexEncoder.encode(u);
                        }
                        if (glyphName.length() > 0) {
                            String unicode = Character.toString(u);
                            NamedCharacter nc = new NamedCharacter(glyphName, unicode);
                            returnFont.addUnencodedCharacter(nc, wx[glyphIndex], bboxes[glyphIndex]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Copy kerning information.
     */
    private void copyKerning(OpenFont otf, boolean isCid) {

        // Get kerning
        Set<Integer> kerningSet;
        if (isCid) {
            kerningSet = otf.getKerning().keySet();
        } else {
            kerningSet = otf.getAnsiKerning().keySet();
        }

        for (Integer kpx1 : kerningSet) {
            Map<Integer, Integer> h2;
            if (isCid) {
                h2 = otf.getKerning().get(kpx1);
            } else {
                h2 = otf.getAnsiKerning().get(kpx1);
            }
            returnFont.putKerningEntry(kpx1, h2);
        }
    }

    /**
     * Copy advanced typographic information.
     */
    private void copyAdvanced(OpenFont otf) {
        if (returnFont instanceof MultiByteFont) {
            MultiByteFont mbf = (MultiByteFont) returnFont;
            mbf.setGDEF(otf.getGDEF());
            mbf.setGSUB(otf.getGSUB());
            mbf.setGPOS(otf.getGPOS());
        }
    }

}
