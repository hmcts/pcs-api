package uk.gov.hmcts.reform.pcs.bulkclaim.soap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.HtmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.security.BulkClaimApimS2sFilter;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * Guards the bulk claim SOAP service ahead of the SOAP stack: the service is invisible (404) while the
 * BULK_CLAIMS_V2 flag is off; the request is read at most up to the configured size (413 beyond it) and must be
 * well-formed XML with no DOCTYPE, so the SOAP parser never sees a DTD or external entity; and the WSDL and
 * schemas are served on GET. Runs after the security chain, so the APIM S2S checks have already happened.
 */
@Slf4j
@RequiredArgsConstructor
public class BulkClaimSoapFilter extends OncePerRequestFilter {

    public static final String SERVICES_PATH = BulkClaimApimS2sFilter.BULK_CLAIMS_PATH + "/services";
    public static final String ENDPOINT_PATH = SERVICES_PATH + "/BulkClaim";
    public static final String WSDL_PATH = ENDPOINT_PATH + ".wsdl";
    public static final String SCHEMA_PATH = SERVICES_PATH + "/schema/";

    static final Set<String> SCHEMA_FILES = Set.of(
        "Common.xsd", "RentalClaim.xsd", "MortgageClaim.xsd", "BulkClaimResponse.xsd", "BulkClaimStatusResponse.xsd",
        "BulkClaimStatusRequest.xsd", "BulkClaimStatusListResponse.xsd");

    private static final String WSDL_RESOURCE = "wsdl/bulkclaim/v2/BulkClaimService.wsdl";
    private static final String SCHEMA_RESOURCE_DIR = "xsd/bulkclaim/v2/";
    private static final String WSDL_ADDRESS_PLACEHOLDER = "http://localhost:8080" + ENDPOINT_PATH;
    private static final String SOAP_11_ENVELOPE_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_12_ENVELOPE_NAMESPACE = "http://www.w3.org/2003/05/soap-envelope";
    private static final Pattern HOST_NAME = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9.-]{0,252}[A-Za-z0-9])?");
    private static final Pattern CONTEXT_PATH = Pattern.compile("[A-Za-z0-9/._-]*+");
    private static final String XML_CONTENT_TYPE = "text/xml;charset=UTF-8";
    private static final String FAULT = """
        <?xml version="1.0" encoding="UTF-8"?>\
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"><soapenv:Body><soapenv:Fault>\
        <faultcode>soapenv:%s</faultcode><faultstring>%s</faultstring></soapenv:Fault></soapenv:Body>\
        </soapenv:Envelope>""";

    private final FeatureToggleService featureToggleService;
    private final BulkClaimProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (HttpMethod.GET.matches(request.getMethod())) {
            if (serve(path, request, response)) {
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        Optional<byte[]> bounded = readBounded(request);
        if (bounded.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        byte[] body = bounded.get();
        String rootNamespace = rootNamespaceOfSafeXml(body);
        if (rootNamespace == null) {
            fault(response, "Client", "The request is not a well-formed SOAP message");
            return;
        }
        if (SOAP_12_ENVELOPE_NAMESPACE.equals(rootNamespace)) {
            fault(response, "VersionMismatch", "SOAP 1.2 is not supported, send a SOAP 1.1 envelope");
            return;
        }
        if (!SOAP_11_ENVELOPE_NAMESPACE.equals(rootNamespace)) {
            fault(response, "Client", "The request is not a SOAP 1.1 envelope");
            return;
        }
        filterChain.doFilter(new BufferedBodyRequest(request, body), response);
    }

    private boolean serve(String path, HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        if (path.equals(WSDL_PATH) || (path.equals(ENDPOINT_PATH) && request.getParameter("wsdl") != null)) {
            writeWsdl(request, response);
            return true;
        }
        if (path.startsWith(SCHEMA_PATH)) {
            writeSchema(path.substring(SCHEMA_PATH.length()), response);
            return true;
        }
        return false;
    }

    private void writeWsdl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String wsdl = new String(resource(WSDL_RESOURCE), StandardCharsets.UTF_8)
            .replace(WSDL_ADDRESS_PLACEHOLDER, endpointUrl(request));
        response.setContentType(XML_CONTENT_TYPE);
        response.getOutputStream().write(wsdl.getBytes(StandardCharsets.UTF_8));
    }

    private void writeSchema(String fileName, HttpServletResponse response) throws IOException {
        if (!SCHEMA_FILES.contains(fileName)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(XML_CONTENT_TYPE);
        response.getOutputStream().write(resource(SCHEMA_RESOURCE_DIR + fileName));
    }

    private static byte[] resource(String name) throws IOException {
        try (InputStream stream = new ClassPathResource(name).getInputStream()) {
            return stream.readAllBytes();
        }
    }

    /** The endpoint address as the caller reached it, or the placeholder when the Host is not a plain hostname. */
    private static String endpointUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        String contextPath = request.getContextPath();
        if (!("http".equals(scheme) || "https".equals(scheme))
            || host == null || !HOST_NAME.matcher(host).matches()
            || !CONTEXT_PATH.matcher(contextPath).matches()) {
            return WSDL_ADDRESS_PLACEHOLDER;
        }
        boolean defaultPort = request.getServerPort() == ("https".equals(scheme) ? 443 : 80);
        return HtmlUtils.htmlEscape(scheme + "://" + host + (defaultPort ? "" : ":" + request.getServerPort())
            + contextPath + ENDPOINT_PATH);
    }

    /** Reads the body, never holding more than the configured maximum; empty when it is over the limit. */
    private Optional<byte[]> readBounded(HttpServletRequest request) throws IOException {
        long maxBytes = properties.getMaxRequestSize().toBytes();
        if (request.getContentLengthLong() > maxBytes) {
            return Optional.empty();
        }
        try (InputStream in = request.getInputStream()) {
            byte[] body = in.readNBytes((int) Math.min(maxBytes + 1, Integer.MAX_VALUE - 8L));
            return body.length > maxBytes ? Optional.empty() : Optional.of(body);
        }
    }

    /** The namespace of the root element, or null when the body is not well-formed XML or declares a DOCTYPE. */
    private static String rootNamespaceOfSafeXml(byte[] body) {
        RootNamespaceHandler handler = new RootNamespaceHandler();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.newSAXParser().parse(new ByteArrayInputStream(body), handler);
            return handler.rootNamespace == null ? "" : handler.rootNamespace;
        } catch (SAXException | IOException e) {
            log.info("Rejecting SOAP request that is not well-formed XML or declares a DOCTYPE");
            return null;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to configure a secure XML parser", e);
        }
    }

    private static void fault(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(XML_CONTENT_TYPE);
        response.getOutputStream().write(FAULT.formatted(code, message).getBytes(StandardCharsets.UTF_8));
    }

    private static final class RootNamespaceHandler extends DefaultHandler {

        private String rootNamespace;

        @Override
        public void startElement(String uri, String localName, String qualifiedName, Attributes attributes) {
            if (rootNamespace == null) {
                rootNamespace = uri;
            }
        }
    }

    /** Replays the body already read for the checks above. */
    private static final class BufferedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        BufferedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream source = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return source.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    return source.read(buffer, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return source.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("Asynchronous reads are not supported");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
