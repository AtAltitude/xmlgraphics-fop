/*-- $Id$ -- 

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
 
 4. The names "FOP" and  "Apache Software Foundation"  must not be used to
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

package org.apache.fop.layout;

// FOP
import org.apache.fop.render.Renderer;
import org.apache.fop.fo.flow.*;
import org.apache.fop.fo.*;
import org.apache.fop.apps.*;

// Java
import java.util.Vector;
import java.util.Enumeration;

public class Page {

    private int height;
    private int width;

    private BodyAreaContainer body;
    private AreaContainer before;
    private AreaContainer after;
    private AreaContainer start;
    private AreaContainer end;
	
    private AreaTree areaTree;

    protected int pageNumber = 0;

    protected Vector linkSets = new Vector();

    private Vector idList = new Vector();

    private Vector footnotes = null;

    Page(AreaTree areaTree, int height, int width) {
	this.areaTree = areaTree;
	this.height = height;
	this.width = width;
    }

    public void setNumber(int number) {
	this.pageNumber = number;
    }

    public int getNumber() {
	return this.pageNumber;
    }

    void addAfter(AreaContainer area) {
	this.after = area;
	area.setPage(this);
    }

    void addBefore(AreaContainer area) {
	this.before = area;
	area.setPage(this);
    }

	/**
	 * Ensure that page is set not only on B.A.C. but also on the
	 * three top-level reference areas.
	 * @param area The region-body area container (special)
	 */
    public void addBody(BodyAreaContainer area) {
	this.body = area;
	area.setPage(this);
	((BodyAreaContainer)area).getMainReferenceArea().setPage(this);
	((BodyAreaContainer)area).getBeforeFloatReferenceArea().setPage(this);
	((BodyAreaContainer)area).getFootnoteReferenceArea().setPage(this);
    }
		
    void addEnd(AreaContainer area) {
	this.end = area;
	area.setPage(this);
    }

    void addStart(AreaContainer area) {
	this.start = area;
	area.setPage(this);
    }

    public void render(Renderer renderer) {
	renderer.renderPage(this);
    }

    public AreaContainer getAfter() {
	return this.after;
    }

    public AreaContainer getBefore() {
	return this.before;
    }

    public BodyAreaContainer getBody() {
	return this.body;
    }

    public int getHeight() {
	return this.height;
    }

    public int getWidth() {
	return this.width;
    }

    public FontInfo getFontInfo() {
	return this.areaTree.getFontInfo();
    }

    public void addLinkSet(LinkSet linkSet) {
	this.linkSets.addElement(linkSet);
    }

    public Vector getLinkSets() {
	return this.linkSets;
    }

    public boolean hasLinks() {
	return (!this.linkSets.isEmpty());
    }

    public void addToIDList(String id){
        idList.addElement(id);
    }

    public Vector getIDList(){
        return idList;
    }

    public Vector getPendingFootnotes() {
        return footnotes;
    }

    public void setPendingFootnotes(Vector v) {
        footnotes = v;
    	if(footnotes != null) {
            for(Enumeration e = footnotes.elements(); e.hasMoreElements(); ) {
                FootnoteBody fb = (FootnoteBody)e.nextElement();
                if(!Footnote.layoutFootnote(this, fb, null)) {
                    // footnotes are too large to fit on empty page
                }
            }
	        footnotes = null;
	    }
    }

    public void addPendingFootnote(FootnoteBody fb) {
        if(footnotes == null) {
            footnotes = new Vector();
        }
        footnotes.addElement(fb);
    }
}
