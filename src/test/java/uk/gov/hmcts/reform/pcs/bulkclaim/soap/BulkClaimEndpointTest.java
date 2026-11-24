package uk.gov.hmcts.reform.pcs.bulkclaim.soap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ws.soap.server.SoapMessageDispatcher;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.ws.test.server.RequestCreator;
import org.springframework.ws.test.server.RequestCreators;
import org.springframework.ws.test.server.ResponseMatchers;
import org.springframework.ws.transport.context.DefaultTransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;
import org.springframework.xml.transform.StringSource;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.security.BulkClaimApimS2sFilter;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimResult.Outcome;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimV2Handler;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BulkClaimEndpointTest {

    private static final String NS = "http://impl.service.bulkclaim.webservice.pcol.dca.gov.uk";
    private static final Map<String, String> NAMESPACES = Map.of("ns1", NS);
    private static final String ORGANISATION_ID = "ORG1";

    private AnnotationConfigApplicationContext context;
    private BulkClaimV2Handler handler;
    private FeatureToggleService featureToggleService;
    private MockWebServiceClient client;

    @BeforeEach
    void setUp() {
        handler = mock(BulkClaimV2Handler.class);
        featureToggleService = mock(FeatureToggleService.class);
        when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2_MORTGAGE)).thenReturn(true);

        context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("handler", handler);
        context.getBeanFactory().registerSingleton("featureToggleService", featureToggleService);
        context.getBeanFactory().registerSingleton("properties", new BulkClaimProperties());
        context.register(TestConfig.class);
        context.refresh();
        client = MockWebServiceClient.createClient(context);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v2/bulk-claims/services/BulkClaim");
        request.setAttribute(BulkClaimApimS2sFilter.ORGANISATION_ID_ATTRIBUTE, ORGANISATION_ID);
        TransportContextHolder.setTransportContext(
            new DefaultTransportContext(
                new HttpServletConnection(request, new MockHttpServletResponse()) { }));
    }

    @AfterEach
    void tearDown() {
        TransportContextHolder.setTransportContext(null);
        context.close();
    }

    @Test
    void shouldAnswerAnAxisStyleRentalRequestWithPcolsResponseWrapper() {
        when(handler.submitRental(any(), eq(ORGANISATION_ID), eq(false)))
            .thenReturn(new BulkClaimResult(Outcome.OK, bytes("<BulkClaimResponse/>")));

        client.sendRequest(axisRequest("processRentalClaim", "rentalRequest", "<RentalClaimSet a=\"1\"/>"))
            .andExpect(ResponseMatchers.noFault())
            .andExpect(ResponseMatchers.xpath(
                "/ns1:processRentalClaimResponse/ns1:processRentalClaimReturn", NAMESPACES)
                .evaluatesTo("<BulkClaimResponse/>"));

        verify(handler).submitRental(bytes("<RentalClaimSet a=\"1\"/>"), ORGANISATION_ID, false);
    }

    @Test
    void shouldPassValidateOnlyToTheHandler() {
        when(handler.submitRental(any(), any(), eq(true)))
            .thenReturn(new BulkClaimResult(Outcome.OK, bytes("<ok/>")));

        client.sendRequest(withBody("<ns1:processRentalClaim xmlns:ns1=\"" + NS + "\"><ns1:rentalRequest>&lt;a/&gt;"
                + "</ns1:rentalRequest><ns1:validateOnly>true</ns1:validateOnly></ns1:processRentalClaim>"))
            .andExpect(ResponseMatchers.noFault());

        verify(handler).submitRental(bytes("<a/>"), ORGANISATION_ID, true);
    }

    @Test
    void shouldAnswerMortgageRequest() {
        when(handler.submitMortgage(any(), eq(ORGANISATION_ID), eq(false)))
            .thenReturn(new BulkClaimResult(Outcome.OK, bytes("<m/>")));

        client.sendRequest(axisRequest("processMortgageClaim", "mortgageRequest", "<MortgageClaimSet/>"))
            .andExpect(ResponseMatchers.xpath("//ns1:processMortgageClaimResponse/ns1:processMortgageClaimReturn",
                NAMESPACES).evaluatesTo("<m/>"));
    }

    @Test
    void shouldAnswerClaimStatus() {
        when(handler.status(ORGANISATION_ID, "CLAIM-1"))
            .thenReturn(new BulkClaimResult(Outcome.OK, bytes("<BulkClaimStatusResponse/>")));

        client.sendRequest(axisRequest("getClaimStatus", "customerUniqueIdentifier", "CLAIM-1"))
            .andExpect(ResponseMatchers.xpath("//ns1:getClaimStatusResponse/ns1:getClaimStatusReturn", NAMESPACES)
                .evaluatesTo("<BulkClaimStatusResponse/>"));
    }

    @Test
    void shouldReportAnUnknownClaimInTheReturnString() {
        when(handler.status(ORGANISATION_ID, "NOPE"))
            .thenReturn(new BulkClaimResult(Outcome.CLAIM_NOT_FOUND, bytes("<BulkClaimStatusResponse/>")));

        client.sendRequest(axisRequest("getClaimStatus", "customerUniqueIdentifier", "NOPE"))
            .andExpect(ResponseMatchers.noFault())
            .andExpect(ResponseMatchers.xpath("//ns1:getClaimStatusReturn", NAMESPACES)
                .evaluatesTo("<BulkClaimStatusResponse/>"));
    }

    @Test
    void shouldAnswerClaimStatuses() {
        when(handler.statusBatch(any(), eq(ORGANISATION_ID)))
            .thenReturn(new BulkClaimResult(Outcome.OK, bytes("<BulkClaimStatusListResponse/>")));

        client.sendRequest(axisRequest("getClaimStatuses", "statusRequest", "<BulkClaimStatusRequest/>"))
            .andExpect(ResponseMatchers.xpath("//ns1:getClaimStatusesResponse/ns1:getClaimStatusesReturn",
                NAMESPACES).evaluatesTo("<BulkClaimStatusListResponse/>"));

        verify(handler).statusBatch(bytes("<BulkClaimStatusRequest/>"), ORGANISATION_ID);
    }

    @Test
    void shouldReportSchemaInvalidInTheReturnStringNotAsAFault() {
        when(handler.submitRental(any(), any(), eq(false)))
            .thenReturn(
                new BulkClaimResult(Outcome.SCHEMA_INVALID, bytes("<BulkClaimResponse>3</BulkClaimResponse>")));

        client.sendRequest(axisRequest("processRentalClaim", "rentalRequest", "<bad/>"))
            .andExpect(ResponseMatchers.noFault())
            .andExpect(ResponseMatchers.xpath("//ns1:processRentalClaimReturn", NAMESPACES)
                .evaluatesTo("<BulkClaimResponse>3</BulkClaimResponse>"));
    }

    @Test
    void shouldReportAnOrganisationThatCannotSubmitInTheReturnString() {
        when(handler.submitRental(any(), any(), eq(false)))
            .thenReturn(
                new BulkClaimResult(Outcome.ORGANISATION_NOT_USABLE,
                bytes("<BulkClaimResponse>2</BulkClaimResponse>")));

        client.sendRequest(axisRequest("processRentalClaim", "rentalRequest", "<a/>"))
            .andExpect(ResponseMatchers.noFault())
            .andExpect(ResponseMatchers.xpath("//ns1:processRentalClaimReturn", NAMESPACES)
                .evaluatesTo("<BulkClaimResponse>2</BulkClaimResponse>"));
    }

    @Test
    void shouldFaultWhenTooManyClaims() {
        when(handler.submitRental(any(), any(), eq(false)))
            .thenReturn(new BulkClaimResult(Outcome.TOO_MANY_CLAIMS, null));

        client.sendRequest(axisRequest("processRentalClaim", "rentalRequest", "<a/>"))
            .andExpect(ResponseMatchers.clientOrSenderFault());
    }

    @Test
    void shouldFaultForMortgageWhenMortgageFlagIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2_MORTGAGE)).thenReturn(false);

        client.sendRequest(axisRequest("processMortgageClaim", "mortgageRequest", "<a/>"))
            .andExpect(ResponseMatchers.clientOrSenderFault("Operation processMortgageClaim is not available"));

        verify(handler, never()).submitMortgage(any(), any(), eq(false));
    }

    @Test
    void shouldFaultForAnUnknownOperation() {
        client.sendRequest(axisRequest("cancelClaim", "id", "1"))
            .andExpect(ResponseMatchers.clientOrSenderFault());

        verifyNoInteractions(handler);
    }

    @Test
    void shouldReportAnUnexpectedFailureAsAServerFaultWithoutItsCause() {
        when(handler.submitRental(any(), any(), eq(false))).thenThrow(new IllegalStateException("db password"));

        client.sendRequest(axisRequest("processRentalClaim", "rentalRequest", "<a/>"))
            .andExpect(ResponseMatchers.serverOrReceiverFault("The service was unable to process the request"));
    }

    private static RequestCreator axisRequest(String operation, String child, String value) {
        return withBody("<ns1:" + operation + " xmlns:ns1=\"" + NS + "\"><ns1:" + child + ">" + escape(value)
            + "</ns1:" + child + "></ns1:" + operation + ">");
    }

    private static RequestCreator withBody(String body) {
        return RequestCreators.withSoapEnvelope(new StringSource("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soapenv:Body>" + body + "</soapenv:Body></soapenv:Envelope>"));
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Configuration
    @Import(BulkClaimSoapConfiguration.class)
    static class TestConfig {

        @Bean
        BulkClaimEndpoint bulkClaimEndpoint(BulkClaimV2Handler handler, FeatureToggleService featureToggleService) {
            return new BulkClaimEndpoint(handler, featureToggleService);
        }

        @Bean
        SoapMessageDispatcher messageDispatcher() {
            return new SoapMessageDispatcher();
        }
    }
}
