package uk.gov.hmcts.reform.pcs.bulkclaim.soap;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;
import uk.gov.hmcts.reform.pcs.bulkclaim.security.BulkClaimApimS2sFilter;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimV2Handler;
import uk.gov.hmcts.reform.pcs.bulkclaim.soap.BulkClaimSoapMessages.GetClaimStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.soap.BulkClaimSoapMessages.GetClaimStatusResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.soap.BulkClaimSoapMessages.GetClaimStatuses;
import uk.gov.hmcts.reform.pcs.bulkclaim.soap.BulkClaimSoapMessages.GetClaimStatusesResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.soap.BulkClaimSoapMessages.ProcessMortgageClaim;
import uk.gov.hmcts.reform.pcs.bulkclaim.soap.BulkClaimSoapMessages.ProcessMortgageClaimResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.soap.BulkClaimSoapMessages.ProcessRentalClaim;
import uk.gov.hmcts.reform.pcs.bulkclaim.soap.BulkClaimSoapMessages.ProcessRentalClaimResponse;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.nio.charset.StandardCharsets;

import static uk.gov.hmcts.reform.pcs.bulkclaim.soap.BulkClaimSoapMessages.NAMESPACE;

/**
 * SOAP front of the bulk claim handler. Operations are selected by the payload root element, so the SOAPAction
 * header, empty in PCOL's WSDL, plays no part.
 *
 * <p>Outcomes that PCOL reported inside the response string (schema errors, organisation not usable, payment
 * account not valid, per-claim failures) stay in the string; a request refused as a whole is a Client fault.
 */
@Slf4j
@Endpoint
@RequiredArgsConstructor
public class BulkClaimEndpoint {

    private final BulkClaimV2Handler handler;
    private final FeatureToggleService featureToggleService;

    @PayloadRoot(namespace = NAMESPACE, localPart = "processRentalClaim")
    @ResponsePayload
    public ProcessRentalClaimResponse processRentalClaim(@RequestPayload ProcessRentalClaim request) {
        BulkClaimResult result = handler.submitRental(
            bytes(request.getRentalRequest()), organisationId(), Boolean.TRUE.equals(request.getValidateOnly()));
        return new ProcessRentalClaimResponse(xml(result));
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "processMortgageClaim")
    @ResponsePayload
    public ProcessMortgageClaimResponse processMortgageClaim(@RequestPayload ProcessMortgageClaim request) {
        if (!featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2_MORTGAGE)) {
            throw new BulkClaimSoapRefusalException("Operation processMortgageClaim is not available");
        }
        BulkClaimResult result = handler.submitMortgage(
            bytes(request.getMortgageRequest()), organisationId(), Boolean.TRUE.equals(request.getValidateOnly()));
        return new ProcessMortgageClaimResponse(xml(result));
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "getClaimStatus")
    @ResponsePayload
    public GetClaimStatusResponse getClaimStatus(@RequestPayload GetClaimStatus request) {
        BulkClaimResult result = handler.status(organisationId(), request.getCustomerUniqueIdentifier());
        return new GetClaimStatusResponse(xml(result));
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "getClaimStatuses")
    @ResponsePayload
    public GetClaimStatusesResponse getClaimStatuses(@RequestPayload GetClaimStatuses request) {
        BulkClaimResult result = handler.statusBatch(bytes(request.getStatusRequest()), organisationId());
        return new GetClaimStatusesResponse(xml(result));
    }

    private static String xml(BulkClaimResult result) {
        if (result.xml() == null) {
            log.info("Refusing bulk claim request: {}", result.outcome());
            throw new BulkClaimSoapRefusalException("The request contains more claims than are allowed");
        }
        return new String(result.xml(), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String xml) {
        return (xml == null ? "" : xml).getBytes(StandardCharsets.UTF_8);
    }

    private static String organisationId() {
        HttpServletConnection connection = (HttpServletConnection) TransportContextHolder.getTransportContext()
            .getConnection();
        HttpServletRequest request = connection.getHttpServletRequest();
        return (String) request.getAttribute(BulkClaimApimS2sFilter.ORGANISATION_ID_ATTRIBUTE);
    }
}
