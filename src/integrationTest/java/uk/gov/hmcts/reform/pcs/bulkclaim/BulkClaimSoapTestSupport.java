package uk.gov.hmcts.reform.pcs.bulkclaim;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

/** Posts SOAP 1.1 envelopes shaped the way an Axis client generated from PCOL's WSDL would send them. */
final class BulkClaimSoapTestSupport {

    static final String SERVICE_PATH = "/v2/bulk-claims/services/BulkClaim";
    static final String NAMESPACE = "http://impl.service.bulkclaim.webservice.pcol.dca.gov.uk";

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private BulkClaimSoapTestSupport() {
    }

    static String envelope(String operation, String child, String value) {
        return envelope("<ns1:" + operation + " xmlns:ns1=\"" + NAMESPACE + "\"><ns1:" + child + ">" + escape(value)
            + "</ns1:" + child + "></ns1:" + operation + ">");
    }

    static String envelope(String body) {
        return "<?xml version='1.0' encoding='UTF-8'?><soapenv:Envelope "
            + "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>" + body
            + "</soapenv:Body></soapenv:Envelope>";
    }

    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static HttpResponse<String> post(int port, String path, String contentType, String body, String... headers)
        throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", contentType)
            .POST(BodyPublishers.ofString(body));
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        return CLIENT.send(request.build(), BodyHandlers.ofString());
    }

    static HttpResponse<String> get(int port, String pathAndQuery, String... headers)
        throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathAndQuery))
            .GET();
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        return CLIENT.send(request.build(), BodyHandlers.ofString());
    }
}
