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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Thread-safe version of XmlUtils.
 * 
 * @author mlohbihler
 */
public class XmlUtilsTS {
    private XmlUtilsTS() {
        // Static access only.
    }

    private static DocumentBuilder builder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Document newDocument() {
        return builder().newDocument();
    }

    public static DocumentFragment newDocumentFragment() {
        return builder().newDocument().createDocumentFragment();
    }

    public static Document parse(String xml) throws SAXException {
        try {
            return builder().parse(new InputSource(new StringReader(xml)));
        }
        catch (IOException e) {
            // Should never happen because we're using a string reader.
            throw new RuntimeException(e);
        }
    }

    public static Document parse(Reader in) throws SAXException, IOException {
        return builder().parse(new InputSource(in));
    }

    public static Document parse(InputStream in) throws SAXException, IOException {
        return builder().parse(new InputSource(in));
    }

    public static Document parse(File in) throws SAXException, IOException {
        return builder().parse(in);
    }

    public static DocumentFragment parseFragment(String xmlFrag) throws SAXException {
        Document doc = parse("<root>" + xmlFrag + "</root>");
        DocumentFragment frag = doc.createDocumentFragment();

        NodeList children = doc.getDocumentElement().getChildNodes();
        while (children.getLength() > 0)
            frag.appendChild(children.item(0));

        return frag;
    }

    public static void stripWhitespace(Node node) {
        try {
            node.normalize();

            // XPath to find empty text nodes.
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPathExpression xpathExp = xpathFactory.newXPath().compile("//text()[normalize-space(.) = '']");
            NodeList emptyTextNodes = (NodeList) xpathExp.evaluate(node, XPathConstants.NODESET);

            // Remove each empty text node from document.
            for (int i = 0; i < emptyTextNodes.getLength(); i++) {
                Node emptyTextNode = emptyTextNodes.item(i);
                emptyTextNode.getParentNode().removeChild(emptyTextNode);
            }
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        catch (DOMException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toString(Node xml) {
        return toString(xml, 2, true);
    }

    public static String toString(Node xml, Integer indent, boolean omitDeclaration) {
        StringWriter out = new StringWriter();
        writeXml(xml, new StreamResult(out), null, indent, omitDeclaration, null);
        return out.toString();
    }

    public static void writeXml(Node xml, Writer out) {
        writeXml(xml, new StreamResult(out), null, null, false, null);
    }

    public void writeXml(Node xml, OutputStream out) {
        writeXml(xml, new StreamResult(out), null, null, false, null);
    }

    public static void writeXml(Node xml, StreamResult result, String encoding, Integer indent,
            boolean omitDeclaration, Map<String, Object> factoryAttributes) {
        try {
            createTransformer(encoding, indent, omitDeclaration, factoryAttributes).transform(new DOMSource(xml),
                    result);
        }
        catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private static Transformer createTransformer(String encoding, Integer indent, boolean omitDeclaration,
            Map<String, Object> factoryAttributes) {
        TransformerFactory factory = TransformerFactory.newInstance();

        if (factoryAttributes != null) {
            for (Map.Entry<String, Object> attr : factoryAttributes.entrySet())
                factory.setAttribute(attr.getKey(), attr.getValue());
        }

        if (indent != null) {
            try {
                factory.setAttribute("indent-number", indent);
            }
            catch (IllegalArgumentException e) {
                // Ignore
            }
        }

        Transformer transformer;
        try {
            transformer = factory.newTransformer();
        }
        catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }

        if (indent != null) {
            try {
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent.toString());
            }
            catch (IllegalArgumentException e) {
                // Ignore. At least one of the indent settings should work.
            }
        }
        if (encoding != null)
            transformer.setOutputProperty(OutputKeys.ENCODING, encoding);

        transformer.setOutputProperty(OutputKeys.INDENT, indent != null ? "yes" : "no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitDeclaration ? "yes" : "no");

        return transformer;
    }

    //
    //
    // Some convenience methods.
    //
    public static Text createTextNode(Document xmlDoc, String value) {
        if (value == null)
            value = "";
        return xmlDoc.createTextNode(value);
    }

    public static void setTextInElement(Element element, String value) {
        Text text = createTextNode(element.getOwnerDocument(), value);
        element.appendChild(text);
    }

    public static Element appendTextElement(Node parent, String tagName, String value) {
        Element element = appendElement(parent, tagName);
        setTextInElement(element, value);
        return element;
    }

    public static CDATASection createCDataNode(Document xmlDoc, String value) {
        if (value == null)
            value = "";
        return xmlDoc.createCDATASection(value);
    }

    public static void setCDataInElement(Element element, String value) {
        CDATASection cdata = createCDataNode(element.getOwnerDocument(), value);
        element.appendChild(cdata);
    }

    public static Element appendCDataElement(Node parent, String tagName, String value) {
        Element element = appendElement(parent, tagName);
        setCDataInElement(element, value);
        return element;
    }

    public static Element appendElement(Node parent, String tagName) {
        Document document;
        if (parent instanceof Document)
            document = (Document) parent;
        else
            document = parent.getOwnerDocument();

        Element element = document.createElement(tagName);
        parent.appendChild(element);
        return element;
    }

    public static String getElementText(Element element, String defaultText) {
        NodeList children = element.getChildNodes();
        if (children.getLength() == 0)
            return defaultText;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            try {
                sb.append(child.getTextContent());
            }
            catch (AbstractMethodError e) {
                sb.append(child.getNodeValue());
            }
        }

        return sb.toString();
    }

    public static long getLongAttribute(Element element, String name, long defaultValue) {
        try {
            return Long.parseLong(element.getAttribute(name));
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int getIntAttribute(Element element, String name, int defaultValue) {
        try {
            return Integer.parseInt(element.getAttribute(name));
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static double getDoubleAttribute(Element element, String name, double defaultValue) {
        try {
            return Double.parseDouble(element.getAttribute(name));
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBooleanAttribute(Element element, String name, boolean defaultValue) {
        String s = getStringAttribute(element, name, null);
        if (s == null)
            return defaultValue;
        return new Boolean(s).booleanValue();
    }

    public static String getStringAttribute(Element element, String name, String defaultValue) {
        String a = element.getAttribute(name);
        if (org.apache.commons.lang3.StringUtils.isBlank(a))
            return defaultValue;
        return a;
    }

    public static long getTextAsLong(Element element, long defaultValue) {
        if (element == null)
            return defaultValue;
        try {
            return Long.parseLong(getElementText(element, null));
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int getTextAsInt(Element element, int defaultValue) {
        if (element == null)
            return defaultValue;
        try {
            return Integer.parseInt(getElementText(element, null));
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static double getTextAsDouble(Element element, double defaultValue) {
        if (element == null)
            return defaultValue;
        try {
            return Double.parseDouble(getElementText(element, null));
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getTextAsBoolean(Element element, boolean defaultValue) {
        if (element == null)
            return defaultValue;
        String s = getElementText(element, null);
        if (s == null)
            return defaultValue;
        return new Boolean(s).booleanValue();
    }

    public static List<Element> toElements(NodeList nodeList) {
        List<Element> elements = new ArrayList<Element>();
        for (int i = 0; i < nodeList.getLength(); i++)
            elements.add((Element) nodeList.item(i));
        return elements;
    }

    //
    //
    // Elements shallow
    //
    public static List<Element> getChildElements(Node parent) {
        NodeList children = parent.getChildNodes();
        List<Element> result = new ArrayList<Element>();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element)
                result.add((Element) node);
        }
        return result;
    }

    public static Element getChildElement(Node parent, String tagName) {
        return getChildElementByName(parent, tagName, true);
    }

    public static Element getChildElementByNodeName(Node parent, String name) {
        return getChildElementByName(parent, name, false);
    }

    private static Element getChildElementByName(Node parent, String name, boolean tagName) {
        NodeList children = parent.getChildNodes();
        Node node;
        Element child;
        for (int i = 0; i < children.getLength(); i++) {
            node = children.item(i);
            if (!(node instanceof Element))
                continue;

            child = (Element) node;
            if (elementMatches(child, name, tagName))
                return child;
        }
        return null;
    }

    public static List<Element> getChildElements(Node parent, String tagName) {
        return getChildElementsByName(parent, tagName, true);
    }

    public static List<Element> getChildElementsByNodeName(Node parent, String name) {
        return getChildElementsByName(parent, name, false);
    }

    private static List<Element> getChildElementsByName(Node parent, String name, boolean tagName) {
        NodeList children = parent.getChildNodes();
        List<Element> result = new ArrayList<Element>();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element))
                continue;

            Element child = (Element) node;
            if (elementMatches(child, name, tagName))
                result.add(child);
        }
        return result;
    }

    private static boolean elementMatches(Element e, String name, boolean tagName) {
        String elementName = tagName ? e.getTagName() : e.getNodeName();

        if (!name.contains(":")) {
            // The requested name does not include a namespace, so if the element name does, remove it.
            int pos = elementName.indexOf(':');
            if (pos != -1)
                elementName = elementName.substring(pos + 1);
        }

        return name.equals(elementName);
    }

    public static String getChildElementText(Node parent, String tagName) {
        Element child = getChildElementByName(parent, tagName, true);
        if (child == null)
            return null;
        return getElementText(child, "");
    }

    public static long getChildElementTextAsLong(Node parent, String tagName, long defaultValue) {
        return getTextAsLong(getChildElementByName(parent, tagName, true), defaultValue);
    }

    public static int getChildElementTextAsInt(Node parent, String tagName, int defaultValue) {
        return getTextAsInt(getChildElementByName(parent, tagName, true), defaultValue);
    }

    public static double getChildElementTextAsDouble(Node parent, String tagName, double defaultValue) {
        return getTextAsDouble(getChildElementByName(parent, tagName, true), defaultValue);
    }

    public static boolean getChildElementTextAsBoolean(Node parent, String tagName, boolean defaultValue) {
        return getTextAsBoolean(getChildElementByName(parent, tagName, true), defaultValue);
    }

    //
    //
    // Elements deep
    //
    public static Element getDeepElement(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0)
            return null;
        return (Element) list.item(0);
    }

    public static String getDeepElementText(Element parent, String tagName) {
        return getDeepElementText(parent, tagName, null);
    }

    public static String getDeepElementText(Element parent, String tagName, String defaultText) {
        Element child = getDeepElement(parent, tagName);
        if (child == null)
            return null;
        return getElementText(child, defaultText);
    }

    public static long getDeepElementTextAsLong(Element parent, String tagName, long defaultValue) {
        return getTextAsLong(getDeepElement(parent, tagName), defaultValue);
    }

    public static int getDeepElementTextAsInt(Element parent, String tagName, int defaultValue) {
        return getTextAsInt(getDeepElement(parent, tagName), defaultValue);
    }

    public static double getDeepElementTextAsDouble(Element parent, String tagName, double defaultValue) {
        return getTextAsDouble(getDeepElement(parent, tagName), defaultValue);
    }

    public static boolean getDeepElementTextAsBoolean(Element parent, String tagName, boolean defaultValue) {
        return getTextAsBoolean(getDeepElement(parent, tagName), defaultValue);
    }

    public static List<Element> getDeepElements(Element parent, String tagName) {
        return toElements(parent.getElementsByTagName(tagName));
    }

    //
    //
    // Elements by path
    //
    public static final TagNameComparator TAG_NAME_COMPARATOR = new TagNameComparator();

    static class TagNameComparator implements Comparator<Element> {
        @Override
        public int compare(Element e1, Element e2) {
            return e1.getTagName().compareToIgnoreCase(e2.getTagName());
        }
    }

    public static List<Element> getElements(Element parent, String... path) {
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
                elements = getChildElements(e, s);
            }
        }

        return elements;
    }

    public static Element getElement(Element parent, String... path) {
        List<Element> elements = getElements(parent, path);
        if (elements.isEmpty())
            return null;
        return elements.get(0);
    }

    public static Element getElementWithAttr(Element parent, String[] path, String attrName, String attrValue) {
        List<Element> es = getElements(parent, path);
        for (Element e : es) {
            if (attrValue.equals(e.getAttribute(attrName)))
                return e;
        }
        return null;
    }

    public static Element getParentElement(Element child, String name) {
        Node parent = child.getParentNode();
        while (true) {
            if (parent == null)
                return null;

            if (parent instanceof Element) {
                if (name.equals(parent.getNodeName()))
                    return (Element) parent;
                if (name.equals(((Element) parent).getTagName()))
                    return (Element) parent;
            }

            parent = parent.getParentNode();
        }
    }
}
