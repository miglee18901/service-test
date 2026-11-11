package org.example.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dao.CommandRequest;
import org.example.dao.OcsVcParameterDTO;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import org.xml.sax.InputSource;
import java.util.List;

public final class OcsToolUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OcsToolUtil() { }

    // Helper implementation is local: the source of these methods is not shown.
    public static String parseParameter(String code, List<OcsVcParameterDTO> parameters) {
        for (OcsVcParameterDTO parameter : parameters) {
            String value = escapeXml(parameter.getValueString());
            code = code.replace("${" + parameter.getName() + "}", value);
        }
        return code;
    }

    public static CommandRequest convertXmlToCommandRequest(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            if (!"request".equals(root.getTagName())) throw new IllegalArgumentException("Expected request XML");
            CommandRequest request = new CommandRequest();
            request.setPath(text(root, "path"));
            request.setMethod(text(root, "method"));
            request.setBody(text(root, "body"));
            NodeList headers = root.getElementsByTagName("header");
            for (int i = 0; i < headers.getLength(); i++) {
                Element header = (Element) headers.item(i);
                request.getHeader().put(header.getAttribute("name"), header.getTextContent());
            }
            return request;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid command XML", exception);
        }
    }

    public static boolean checkResultCode(String response, String successCode) {
        try {
            JsonNode code = MAPPER.readTree(response).get("code");
            return code != null && successCode != null && successCode.equals(code.asText());
        } catch (Exception exception) {
            return false;
        }
    }

    private static String text(Element root, String name) {
        NodeList nodes = root.getElementsByTagName(name);
        if (nodes.getLength() == 0) throw new IllegalArgumentException("Missing command " + name);
        return nodes.item(0).getTextContent();
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
