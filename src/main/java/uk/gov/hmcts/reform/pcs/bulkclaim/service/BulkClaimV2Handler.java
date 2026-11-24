package uk.gov.hmcts.reform.pcs.bulkclaim.service;

/**
 * Extension point behind the bulk claim SOAP endpoint. The organisation id is the PRD id established by
 * the APIM filter, never client-supplied content.
 */
public interface BulkClaimV2Handler {

    BulkClaimResult submitRental(byte[] xml, String organisationId, boolean validateOnly);

    BulkClaimResult submitMortgage(byte[] xml, String organisationId, boolean validateOnly);

    BulkClaimResult status(String organisationId, String customerUniqueIdentifier);

    BulkClaimResult statusBatch(byte[] xml, String organisationId);
}
