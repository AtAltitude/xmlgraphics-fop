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
package org.apache.fop.dom.svg;

import org.apache.fop.datatypes.*;

import org.w3c.dom.*;

import java.util.*;

/**
 *
 *
 */
public class ElementImpl implements Element {
	Vector childs = new Vector();
	Node parent = null;

	public Node replaceChild(Node n, Node no)
	{
		return null;
	}

    public String getNodeName()
	{
		return null;
	}

    public short getNodeType()
	{
		return 0;
	}

    public Node getParentNode()
	{
		return parent;
	}

    public NodeList getChildNodes()
	{
		return new NodeListImpl(childs);
	}

    public Node getFirstChild()
	{
		return null;
	}

    public Node getLastChild()
	{
		return null;
	}

    public Node getPreviousSibling()
	{
		return null;
	}

    public Node getNextSibling()
	{
		return null;
	}

    public NamedNodeMap getAttributes()
	{
		return null;
	}

    public Document getOwnerDocument()
	{
		return null;
	}

    public Node insertBefore(Node newChild,
                                     Node refChild)
                                     throws DOMException
	{
		return null;
	}

    public Node removeChild(Node oldChild)
                                    throws DOMException
	{
		return null;
	}

    public Node appendChild(Node newChild)
                                    throws DOMException
	{
		childs.addElement(newChild);
		if(newChild instanceof ElementImpl) {
			((ElementImpl)newChild).parent = this;
		}
		return newChild;
	}

    public boolean hasChildNodes()
	{
		return childs.size() > 0;
	}

    public Node cloneNode(boolean deep)
	{
		return null;
	}

    public void normalize()
	{
	}

    public boolean supports(String feature,
                                 String version)
	{
		return false;
	}

    public String getNamespaceURI()
	{
		return SVGDocumentImpl.namespaceURI;
	}

    public String getPrefix()
	{
		return "svg";
	}

    public void setPrefix(String prefix) throws DOMException
	{
	}

    public String getLocalName()
	{
		return null;
	}

    public String getNodeValue() throws DOMException
	{
		return null;
	}

    public void setNodeValue(String nodeValue) throws DOMException
	{
	}

    public String getTagName()
	{
		return null;
	}

    public String getAttribute(String name)
	{
		return null;
	}

    public void setAttribute(String name, String value) throws DOMException
	{
	}

    public void removeAttribute(String name) throws DOMException
	{
	}

    public Attr getAttributeNode(String name)
	{
		return null;
	}

    public Attr setAttributeNode(Attr newAttr)
                                         throws DOMException
	{
		return null;
	}

    public Attr removeAttributeNode(Attr oldAttr)
                                            throws DOMException
	{
		return null;
	}

    public NodeList getElementsByTagName(String name)
	{
		return null;
	}

    public String getAttributeNS(String namespaceURI,
                                       String localName)
	{
		return null;
	}

    public void setAttributeNS(String namespaceURI,
                                       String qualifiedName,
                                       String value)
                                       throws DOMException
	{
	}

    public void removeAttributeNS(String namespaceURI,
                                          String localName)
                                          throws DOMException
	{
	}

    public Attr getAttributeNodeNS(String namespaceURI,
                                           String localName)
	{
		return null;
	}

    public Attr setAttributeNodeNS(Attr newAttr)
                                           throws DOMException
	{
		return null;
	}

    public NodeList getElementsByTagNameNS(String namespaceURI,
                                               String localName)
	{
		return null;
	}

    public boolean hasAttributeNS (String namespaceURI,
                                   String localName)
  {
    return false;
  }

    public boolean hasAttribute (String name)
  {
    return false;
  }

    public boolean hasAttributes()
    {
        return false;
    }
}

class NodeListImpl implements NodeList
{
	Vector vect = null;

	NodeListImpl(Vector v)
	{
		vect = v;
	}

	public int getLength()
	{
		return vect.size();
	}

	public Node item(int i)
	{
		return (Node)vect.elementAt(i);
	}
}
