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

//Author:       Eric SCHAEFFER
//Description:  implement ImageConsumer for FopImage classes

package org.apache.fop.image;

// Java
import java.util.Hashtable;
import java.awt.image.*;
import java.awt.*;

import java.lang.reflect.Array;

// CONSUMER CLASS
public class FopImageConsumer implements ImageConsumer {
	protected int width = -1;
	protected int height = -1;
	protected Integer imageStatus = new Integer(-1);
	protected int hints = 0;
	protected Hashtable properties = null;
	protected ColorModel cm = null;
	protected ImageProducer ip = null;

	public FopImageConsumer(ImageProducer iprod) {
		this.ip = iprod;
	}

	public void imageComplete(int status) {
/*
System.err.print("Status ");
if (status == ImageConsumer.COMPLETESCANLINES) {
	System.err.println("CompleteScanLines");
} else if (status == ImageConsumer.IMAGEABORTED) {
	System.err.println("ImageAborted");
} else if (status == ImageConsumer.IMAGEERROR) {
	System.err.println("ImageError");
} else if (status == ImageConsumer.RANDOMPIXELORDER) {
	System.err.println("RandomPixelOrder");
} else if (status == ImageConsumer.SINGLEFRAME) {
	System.err.println("SingleFrame");
} else if (status == ImageConsumer.SINGLEFRAMEDONE) {
	System.err.println("SingleFrameDone");
} else if (status == ImageConsumer.SINGLEPASS) {
	System.err.println("SinglePass");
} else if (status == ImageConsumer.STATICIMAGEDONE) {
	System.err.println("StaticImageDone");
} else if (status == ImageConsumer.TOPDOWNLEFTRIGHT) {
	System.err.println("TopDownLeftRight");
}
*/
		synchronized(this.imageStatus) {
			// Need to stop status if image done
			if (this.imageStatus.intValue() != ImageConsumer.STATICIMAGEDONE)
				this.imageStatus = new Integer(status);
		}
	}

	public void setColorModel(ColorModel model) {
//System.err.println("setColorModel: " + model);
		this.cm = model;
	}

	public void setDimensions(int width, int height) {
//System.err.println("setDimension: w=" + width + " h=" + height);
		this.width = width;
		this.height = height;
	}

	public void setHints(int hintflags) {
//System.err.println("setHints: " + hintflags);
		this.hints = hintflags;
	}

	public void setProperties(Hashtable props) {
//System.err.println("setProperties: " + props);
		this.properties = props;
	}

	public void setPixels(int x, int y, int w, int h,
	   ColorModel model, byte[] pixels,int off,
	   int scansize) {}

	public void setPixels(int x, int y, int w, int h,
	   ColorModel model, int[] pixels, int off,
	   int scansize) {}

	public boolean isImageReady() throws Exception {
		synchronized(this.imageStatus) {
			if (this.imageStatus.intValue() == ImageConsumer.IMAGEABORTED)
				throw new Exception("Image aborted");
			if (this.imageStatus.intValue() == ImageConsumer.IMAGEERROR)
				throw new Exception("Image error");

			if (this.imageStatus.intValue() == ImageConsumer.STATICIMAGEDONE)
				return true;

			return false;
		}
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public ColorModel getColorModel() {
		return this.cm;
	}

	public int[] getImage() throws Exception {
		int tmpMap[] = new int[this.width * this.height];
		PixelGrabber pg = new PixelGrabber(
									this.ip,
									0, 0,
									this.width, this.height,
									tmpMap,
									0, this.width
									);
		pg.setDimensions(this.width, this.height);
		pg.setColorModel(this.cm);
		pg.setHints(this.hints);
		pg.setProperties(this.properties);
		try {
			pg.grabPixels();
		} catch (InterruptedException intex) {
			throw new Exception("Image grabbing interrupted : " + intex.getMessage());
		}
		return tmpMap;
	}
}
