package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import org.springframework.http.ResponseEntity;

/**
 * Extension point behind {@code BulkClaimV2Controller}. The organisation id is the PRD id established by
 * the APIM filter, never client-supplied content.
 */
public interface BulkClaimV2Handler {

    ResponseEntity<byte[]> submitRental(byte[] xml, String organisationId, boolean validateOnly);

    ResponseEntity<byte[]> submitMortgage(byte[] xml, String organisationId, boolean validateOnly);

    ResponseEntity<byte[]> status(String organisationId, String customerUniqueIdentifier);

    ResponseEntity<byte[]> statusBatch(byte[] xml, String organisationId);

    ResponseEntity<byte[]> schema(String fileName);
}
