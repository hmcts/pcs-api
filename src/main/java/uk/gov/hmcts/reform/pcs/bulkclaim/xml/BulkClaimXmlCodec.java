package uk.gov.hmcts.reform.pcs.bulkclaim.xml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventLocator;
import org.springframework.stereotype.Component;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.BulkClaimResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.BulkClaimStatusResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.MortgageClaimSet;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ObjectFactory;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentalClaimSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

@Component
public class BulkClaimXmlCodec {

    public static final String NAMESPACE = "http://hmcts.gov.uk/pcs/bulkclaim/v2";

    private static final String SCHEMA_DIR = "xsd/bulkclaim/v2/";

    private final JAXBContext context;
    private final Schema rentalSchema;
    private final Schema mortgageSchema;

    public BulkClaimXmlCodec() {
        try {
            this.context = JAXBContext.newInstance(ObjectFactory.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXB context for bulk claims", e);
        }
        this.rentalSchema = loadSchema("RentalClaim.xsd");
        this.mortgageSchema = loadSchema("MortgageClaim.xsd");
    }

    public RentalClaimSet parseRental(byte[] xml) {
        return parse(xml, rentalSchema, RentalClaimSet.class);
    }

    public MortgageClaimSet parseMortgage(byte[] xml) {
        return parse(xml, mortgageSchema, MortgageClaimSet.class);
    }

    public byte[] write(BulkClaimResponse response) {
        return marshal(response, false);
    }

    public byte[] write(BulkClaimStatusResponse response) {
        return marshal(response, false);
    }

    public String sha256Hex(byte[] bytes) {
        return HexFormat.of().formatHex(sha256().digest(bytes));
    }

    /**
     * Hashes the claim re-marshalled as an XML fragment, so formatting differences in the
     * submitted document do not change the hash.
     */
    public String claimHash(Object claimElement) {
        JAXBElement<?> element = switch (claimElement) {
            case RentalClaimSet.RentalClaim claim -> element("RentalClaim", RentalClaimSet.RentalClaim.class, claim);
            case MortgageClaimSet.MortgageClaim claim ->
                element("MortgageClaim", MortgageClaimSet.MortgageClaim.class, claim);
            default -> throw new IllegalArgumentException(
                "Unsupported claim element: " + claimElement.getClass().getName());
        };
        return sha256Hex(marshal(element, true));
    }

    private static <T> JAXBElement<T> element(String name, Class<T> type, T value) {
        return new JAXBElement<>(new QName(NAMESPACE, name), type, value);
    }

    private <T> T parse(byte[] xml, Schema schema, Class<T> rootType) {
        List<String> errors = new ArrayList<>();
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            unmarshaller.setSchema(schema);
            unmarshaller.setEventHandler(event -> {
                if (event.getSeverity() != ValidationEvent.WARNING) {
                    errors.add(describe(event));
                }
                return true;
            });
            Object result = unmarshaller.unmarshal(
                new SAXSource(hardenedReader(), new InputSource(new ByteArrayInputStream(xml))));
            if (!errors.isEmpty()) {
                throw new BulkClaimSchemaValidationException(errors);
            }
            if (rootType.isInstance(result)) {
                return rootType.cast(result);
            }
            throw new BulkClaimSchemaValidationException(
                List.of("Unexpected root element, expected " + rootType.getSimpleName()));
        } catch (JAXBException e) {
            if (errors.isEmpty()) {
                errors.add(e.getMessage());
            }
            throw new BulkClaimSchemaValidationException(errors);
        }
    }

    private static String describe(ValidationEvent event) {
        ValidationEventLocator locator = event.getLocator();
        return locator.getLineNumber() + ":" + locator.getColumnNumber() + " " + event.getMessage();
    }

    private XMLReader hardenedReader() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return reader;
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("Unable to create hardened XML parser", e);
        }
    }

    private byte[] marshal(Object value, boolean fragment) {
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, fragment);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            marshaller.marshal(value, out);
            return out.toByteArray();
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to marshal " + value.getClass().getSimpleName(), e);
        }
    }

    private Schema loadSchema(String name) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DOMImplementationLS dom = (DOMImplementationLS) DOMImplementationRegistry.newInstance()
                .getDOMImplementation("LS");
            factory.setResourceResolver((type, namespaceUri, publicId, systemId, baseUri) ->
                                            classpathInput(dom, systemId));
            return factory.newSchema(new javax.xml.transform.stream.StreamSource(
                open(name), SCHEMA_DIR + name));
        } catch (SAXException | ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to compile bulk claim schema " + name, e);
        }
    }

    private LSInput classpathInput(DOMImplementationLS dom, String systemId) {
        LSInput input = dom.createLSInput();
        input.setSystemId(systemId);
        input.setByteStream(open(systemId));
        return input;
    }

    private InputStream open(String name) {
        String resource = SCHEMA_DIR + name.substring(name.lastIndexOf('/') + 1);
        InputStream stream = BulkClaimXmlCodec.class.getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("Missing bulk claim schema on classpath: " + resource);
        }
        return stream;
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
