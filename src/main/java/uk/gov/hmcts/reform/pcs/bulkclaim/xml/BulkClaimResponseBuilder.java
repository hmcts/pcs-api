package uk.gov.hmcts.reform.pcs.bulkclaim.xml;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.BulkClaimResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimResponseType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.StatusType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ValidationErrorType;

import java.util.List;

@Component
public class BulkClaimResponseBuilder {

    public BulkClaimResponse overallSuccess(List<ClaimResponseType> claims) {
        return overall(BulkFailReason.BULK_SUCCESS, claims);
    }

    public BulkClaimResponse overallFailure(BulkFailReason reason, List<ClaimResponseType> claims) {
        return overall(reason, claims);
    }

    public ClaimResponseType claimSuccess(String customerUniqueIdentifier, String caseReference) {
        return claimSuccess(customerUniqueIdentifier, caseReference, false);
    }

    /**
     * A successful claim response.
     *
     * @param noticeOfActingRequired whether the claim was accepted with its Solicitor block ignored because the
     *     organisation is not a solicitor firm
     */
    public ClaimResponseType claimSuccess(String customerUniqueIdentifier,
                                          String caseReference,
                                          boolean noticeOfActingRequired) {
        ClaimResponseType claim = claim(customerUniqueIdentifier, successReason(noticeOfActingRequired));
        claim.setCaseReference(caseReference);
        claim.setClaimNumber(formatClaimNumber(caseReference));
        return claim;
    }

    public ClaimResponseType claimFailure(String customerUniqueIdentifier,
                                          BulkFailReason reason,
                                          List<BulkValidationError> errors) {
        ClaimResponseType claim = claim(customerUniqueIdentifier, reason);
        if (errors != null && !errors.isEmpty()) {
            ClaimResponseType.ValidationErrors validationErrors = new ClaimResponseType.ValidationErrors();
            errors.stream()
                .map(this::toXml)
                .forEach(validationErrors.getValidationError()::add);
            claim.setValidationErrors(validationErrors);
        }
        return claim;
    }

    public ClaimResponseType claimPending(String customerUniqueIdentifier) {
        return claim(customerUniqueIdentifier, BulkFailReason.CLAIM_PENDING);
    }

    public StatusType status(BulkFailReason reason) {
        StatusType status = new StatusType();
        status.setStatusCode(reason.getStatusCode());
        status.setReason(reason.getStatusReason());
        status.setFailReasonCode(reason.getFailReasonCode());
        status.setFailReason(reason.getReason());
        return status;
    }

    public static BulkFailReason successReason(boolean noticeOfActingRequired) {
        return noticeOfActingRequired ? BulkFailReason.CLAIM_SUCCESS_NOTICE_OF_ACTING : BulkFailReason.CLAIM_SUCCESS;
    }

    public static String formatClaimNumber(String caseReference) {
        if (caseReference == null || !caseReference.matches("\\d{16}")) {
            return caseReference;
        }
        return String.join("-",
                           caseReference.substring(0, 4),
                           caseReference.substring(4, 8),
                           caseReference.substring(8, 12),
                           caseReference.substring(12, 16));
    }

    private BulkClaimResponse overall(BulkFailReason reason, List<ClaimResponseType> claims) {
        BulkClaimResponse response = new BulkClaimResponse();
        response.setBulkStatus(status(reason));
        if (claims != null) {
            response.getClaimResponse().addAll(claims);
        }
        return response;
    }

    private ClaimResponseType claim(String customerUniqueIdentifier, BulkFailReason reason) {
        ClaimResponseType claim = new ClaimResponseType();
        claim.setCustomerUniqueIdentifier(customerUniqueIdentifier);
        claim.setClaimStatus(status(reason));
        return claim;
    }

    private ValidationErrorType toXml(BulkValidationError error) {
        ValidationErrorType xml = new ValidationErrorType();
        xml.setPath(error.path());
        xml.setCode(error.code());
        xml.setMessage(error.message());
        return xml;
    }
}
