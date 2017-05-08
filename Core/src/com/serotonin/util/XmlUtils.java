package com.serotonin.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * These utils are not thread-safe. They should be used in a single-threaded context only.
 * 
 * @author mlohbihler
 * @deprecated use XmlUtilsTS instead
 */
@Deprecated
public class XmlUtils {
    private final DocumentBuilder builder;
    private TransformerFactory transformerFactory;

    public XmlUtils() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Document newDocument() {
        return builder.newDocument();
    }

    public Document parse(String in) throws SAXException, IOException {
        return builder.parse(new InputSource(new StringReader(in)));
    }

    public Document parse(Reader in) throws SAXException, IOException {
        return builder.parse(new InputSource(in));
    }

    public Document parse(InputStream in) throws SAXException, IOException {
        return builder.parse(new InputSource(in));
    }

    public Document parse(File in) throws SAXException, IOException {
        return builder.parse(in);
    }

    public Transformer newTransformer() throws TransformerConfigurationException {
        if (transformerFactory == null)
            transformerFactory = TransformerFactory.newInstance();
        return transformerFactory.newTransformer();
    }

    public void setTransformerFactoryAttribute(String name, Object value) {
        if (transformerFactory == null)
            transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(name, value);
    }

    public String toXml(Document xmlDoc) throws TransformerException {
        return toXml(xmlDoc, newTransformer());
    }

    public String toXml(Document xmlDoc, Transformer transformer) throws TransformerException {
        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(xmlDoc), new StreamResult(out));
        return out.toString();
    }

    public void setEncoding(Transformer transformer, String encoding) {
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
    }

    public void setIndent(Transformer transformer, boolean indent) {
        transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
    }

    public void setOmitDeclaration(Transformer transformer, boolean omit) {
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omit ? "yes" : "no");
    }

    public void writeXml(Document xmlDoc, Transformer transformer, Writer out) throws TransformerException {
        transformer.transform(new DOMSource(xmlDoc), new StreamResult(out));
    }

    public void writeXml(Document xmlDoc, Transformer transformer, OutputStream out) throws TransformerException {
        transformer.transform(new DOMSource(xmlDoc), new StreamResult(out));
    }

    //
    //
    ///
    /// Some convenience methods.
    ///
    //
    //
    public Text createTextNode(Document xmlDoc, String value) {
        if (value == null)
            value = "";
        return xmlDoc.createTextNode(value);
    }

    public void setTextInElement(Element element, String value) {
        Text text = createTextNode(element.getOwnerDocument(), value);
        element.appendChild(text);
    }

    public void appendTextElement(Node parent, String tagName, String value) {
        Element element = appendElement(parent, tagName);
        setTextInElement(element, value);
    }

    public Element appendElement(Node parent, String tagName) {
        Document document;
        if (parent instanceof Document)
            document = (Document) parent;
        else
            document = parent.getOwnerDocument();

        Element element = document.createElement(tagName);
        parent.appendChild(element);
        return element;
    }

    public String getElementText(Element element, String defaultText) {
        NodeList children = element.getChildNodes();
        if (children.getLength() == 0)
            return defaultText;

        Node child = children.item(0);
        try {
            return child.getTextContent();
        }
        catch (AbstractMethodError e) {
            return child.getNodeValue();
        }
    }

    public long getLongAttribute(Element element, String name, long defaultValue) {
        try {
            return Long.parseLong(element.getAttribute(name));
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getIntAttribute(Element element, String name, int defaultValue) {
        try {
            return Integer.parseInt(element.getAttribute(name));
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDoubleAttribute(Element element, String name, double defaultValue) {
        try {
            return Double.parseDouble(element.getAttribute(name));
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanAttribute(Element element, String name, boolean defaultValue) {
        String s = getStringAttribute(element, name, null);
        if (s == null)
            return defaultValue;
        return new Boolean(s).booleanValue();
    }

    public String getStringAttribute(Element element, String name, String defaultValue) {
        String a = element.getAttribute(name);
        if (org.apache.commons.lang3.StringUtils.isBlank(a))
            return defaultValue;
        return a;
    }

    public String getElementTextByTagName(Element parent, String name) {
        return getElementTextByTagName(parent, name, null);
    }

    public String getElementTextByTagName(Element parent, String name, String defaultText) {
        Element child = getElementByTagName(parent, name);
        if (child == null)
            return null;
        return getElementText(child, defaultText);
    }

    public double getElementDoubleByTagName(Element parent, String name, double defaultValue) {
        Element child = getElementByTagName(parent, name);
        if (child == null)
            return defaultValue;
        String text = getElementText(child, null);
        if (text == null)
            return defaultValue;

        try {
            return Double.parseDouble(text);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getElementIntByTagName(Element parent, String name, int defaultValue) {
        Element child = getElementByTagName(parent, name);
        if (child == null)
            return defaultValue;
        String text = getElementText(child, null);
        if (text == null)
            return defaultValue;

        try {
            return Integer.parseInt(text);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Element getElementByTagName(Element parent, String name) {
        NodeList list = parent.getElementsByTagName(name);
        if (list.getLength() == 0)
            return null;
        return (Element) list.item(0);
    }

    public Element getElementByTagNameShallow(Element parent, String name) {
        return getElementByNameShallow(parent, name, true);
    }

    public Element getElementByNodeNameShallow(Element parent, String name) {
        return getElementByNameShallow(parent, name, false);
    }

    private Element getElementByNameShallow(Element parent, String name, boolean tagName) {
        NodeList children = parent.getChildNodes();
        Node node;
        Element child;
        for (int i = 0; i < children.getLength(); i++) {
            node = children.item(i);
            if (!(node instanceof Element))
                continue;

            child = (Element) node;
            if (tagName) {
                if (name.equals(child.getTagName()))
                    return child;
            }
            else {
                if (name.equals(child.getNodeName()))
                    return child;
            }
        }
        return null;
    }

    public List<Element> getElementsByTagName(Element parent, String name) {
        return toElements(parent.getElementsByTagName(name));
    }

    public List<Element> getElementsByTagNameShallow(Element parent, String name) {
        return getElementsByNameShallow(parent, name, true);
    }

    public List<Element> getElementsByNodeNameShallow(Element parent, String name) {
        return getElementsByNameShallow(parent, name, false);
    }

    private List<Element> getElementsByNameShallow(Element parent, String name, boolean tagName) {
        NodeList children = parent.getChildNodes();
        List<Element> result = new ArrayList<Element>();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element))
                continue;

            Element child = (Element) node;
            if (tagName) {
                if (name.equals(child.getTagName()))
                    result.add(child);
            }
            else {
                if (name.equals(child.getNodeName()))
                    result.add(child);
            }
        }
        return result;
    }

    public List<Element> getElements(Element parent, String... path) {
        List<Element> elements = new ArrayList<Element>();
        elements.add(parent);

        for (String s : path) {
            if (s.startsWith("[") && s.endsWith("]")) {
                try {
                    int index = Integer.parseInt(s.substring(1, s.length() - 2));
                    if (index < 0 || index >= elements.size())
                        return Collections.emptyList();
                    Element e = elements.get(index);
                    elements.clear();
                    elements.add(e);
                }
                catch (NumberFormatException e) {
                    return Collections.emptyList();
                }
            }
            else if (elements.isEmpty())
                break;
            else {
                Element e = elements.get(0);
                elements = getElementsByTagNameShallow(e, s);
            }
        }

        return elements;
    }

    public Element getElementWithAttr(Element parent, String[] path, String attrName, String attrValue) {
        List<Element> es = getElements(parent, path);
        for (Element e : es) {
            if (attrValue.equals(e.getAttribute(attrName)))
                return e;
        }
        return null;
    }

    public List<Element> toElements(NodeList nodeList) {
        List<Element> elements = new ArrayList<Element>();
        for (int i = 0; i < nodeList.getLength(); i++)
            elements.add((Element) nodeList.item(i));
        return elements;
    }
}
