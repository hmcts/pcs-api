package uk.gov.hmcts.reform.pcs.bulkclaim.soap;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The service WSDL keeps PCOL's binding, so a client generated from PCOL's original WSDL only needs a new
 * endpoint address.
 */
class BulkClaimWsdlTest {

    private static final String WSDL = "wsdl/bulkclaim/v2/BulkClaimService.wsdl";
    private static final String PCOL_WSDL = "bulkclaim/pcol/BulkClaimService.wsdl";
    private static final String WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/wsdl/soap/";
    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

    private final Document pcol = load(PCOL_WSDL);
    private final Document service = load(WSDL);

    @Test
    void shouldKeepPcolsNamespacesAndBindingStyle() {
        assertThat(service.getDocumentElement().getAttribute("targetNamespace"))
            .isEqualTo(pcol.getDocumentElement().getAttribute("targetNamespace"));
        assertThat(typesNamespace(service)).isEqualTo(typesNamespace(pcol)).isEqualTo(BulkClaimSoapMessages.NAMESPACE);

        Element pcolBinding = first(pcol, SOAP_NS, "binding");
        Element binding = first(service, SOAP_NS, "binding");
        assertThat(binding.getAttribute("style")).isEqualTo(pcolBinding.getAttribute("style")).isEqualTo("document");
        assertThat(binding.getAttribute("transport")).isEqualTo(pcolBinding.getAttribute("transport"));
        assertThat(first(service, WSDL_NS, "binding").getAttribute("name")).isEqualTo("BulkClaimSoapBinding");
        assertThat(first(service, WSDL_NS, "portType").getAttribute("name"))
            .isEqualTo(first(pcol, WSDL_NS, "portType").getAttribute("name"));
        assertThat(first(service, WSDL_NS, "service").getAttribute("name"))
            .isEqualTo(first(pcol, WSDL_NS, "service").getAttribute("name"));
    }

    @Test
    void shouldKeepEveryPcolOperationWithTheSameSoapActionAndLiteralUse() {
        for (String operation : operationNames(pcol)) {
            assertThat(operationNames(service)).contains(operation);
            assertThat(soapAction(service, operation)).isEqualTo(soapAction(pcol, operation));
        }
        assertThat(bodyUses(service)).containsOnly("literal");
    }

    @Test
    void shouldKeepEveryPcolWrapperElementAndChild() {
        for (Element element : elements(pcol)) {
            Element ours = elements(service).stream()
                .filter(candidate -> candidate.getAttribute("name").equals(element.getAttribute("name")))
                .findFirst().orElseThrow();
            List<String> pcolChildren = children(element);
            assertThat(children(ours)).startsWith(pcolChildren.toArray(String[]::new));
        }
    }

    @Test
    void shouldDeclareTheStatusOperationsInTheSameStyle() {
        assertThat(operationNames(service))
            .containsExactlyInAnyOrder("processRentalClaim", "processMortgageClaim", "getClaimStatus",
                                       "getClaimStatuses");
        assertThat(children(element(service, "getClaimStatus"))).containsExactly("customerUniqueIdentifier:string");
        assertThat(children(element(service, "getClaimStatusResponse")))
            .containsExactly("getClaimStatusReturn:string");
        assertThat(children(element(service, "getClaimStatuses"))).containsExactly("statusRequest:string");
        assertThat(children(element(service, "getClaimStatusesResponse")))
            .containsExactly("getClaimStatusesReturn:string");
        assertThat(children(element(service, "processRentalClaim")))
            .containsExactly("rentalRequest:string", "validateOnly:boolean");
    }

    private static String typesNamespace(Document wsdl) {
        return ((Element) wsdl.getElementsByTagNameNS(XSD_NS, "schema").item(0)).getAttribute("targetNamespace");
    }

    private static List<String> operationNames(Document wsdl) {
        List<String> names = new ArrayList<>();
        NodeList operations = first(wsdl, WSDL_NS, "portType").getElementsByTagNameNS(WSDL_NS, "operation");
        for (int i = 0; i < operations.getLength(); i++) {
            names.add(((Element) operations.item(i)).getAttribute("name"));
        }
        return names;
    }

    private static String soapAction(Document wsdl, String operation) {
        NodeList operations = first(wsdl, WSDL_NS, "binding").getElementsByTagNameNS(WSDL_NS, "operation");
        for (int i = 0; i < operations.getLength(); i++) {
            Element candidate = (Element) operations.item(i);
            if (candidate.getAttribute("name").equals(operation)) {
                return ((Element) candidate.getElementsByTagNameNS(SOAP_NS, "operation").item(0))
                    .getAttribute("soapAction");
            }
        }
        throw new AssertionError("No binding operation " + operation);
    }

    private static List<String> bodyUses(Document wsdl) {
        List<String> uses = new ArrayList<>();
        NodeList bodies = wsdl.getElementsByTagNameNS(SOAP_NS, "body");
        for (int i = 0; i < bodies.getLength(); i++) {
            uses.add(((Element) bodies.item(i)).getAttribute("use"));
        }
        return uses;
    }

    private static List<Element> elements(Document wsdl) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = ((Element) wsdl.getElementsByTagNameNS(XSD_NS, "schema").item(0))
            .getElementsByTagNameNS(XSD_NS, "element");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (node.getParentNode() == wsdl.getElementsByTagNameNS(XSD_NS, "schema").item(0)) {
                result.add(node);
            }
        }
        return result;
    }

    private static Element element(Document wsdl, String name) {
        return elements(wsdl).stream().filter(e -> e.getAttribute("name").equals(name)).findFirst().orElseThrow();
    }

    private static List<String> children(Element wrapper) {
        List<String> result = new ArrayList<>();
        NodeList nodes = wrapper.getElementsByTagNameNS(XSD_NS, "element");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element child = (Element) nodes.item(i);
            result.add(child.getAttribute("name") + ":" + child.getAttribute("type").replaceFirst("^.*:", ""));
        }
        return result;
    }

    private static Element first(Document wsdl, String namespace, String localName) {
        return (Element) wsdl.getElementsByTagNameNS(namespace, localName).item(0);
    }

    private static Document load(String resource) {
        try (InputStream stream = BulkClaimWsdlTest.class.getClassLoader().getResourceAsStream(resource)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(stream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
