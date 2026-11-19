package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class NotImplementedBulkClaimV2Handler implements BulkClaimV2Handler {

    @Override
    public ResponseEntity<byte[]> submitRental(byte[] xml, String organisationId, boolean validateOnly) {
        return notImplemented();
    }

    @Override
    public ResponseEntity<byte[]> submitMortgage(byte[] xml, String organisationId, boolean validateOnly) {
        return notImplemented();
    }

    @Override
    public ResponseEntity<byte[]> status(String organisationId, String customerUniqueIdentifier) {
        return notImplemented();
    }

    @Override
    public ResponseEntity<byte[]> schema(String fileName) {
        return notImplemented();
    }

    private static ResponseEntity<byte[]> notImplemented() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
