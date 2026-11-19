package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimantOrganisationResolver;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.util.Optional;

/**
 * Checks a per-claim payment account override against the organisation's accounts. The request-level account
 * is checked once for the whole request, before any claim is prepared.
 */
@Component
public class BulkPbaCheck {

    private final BulkClaimantOrganisationResolver organisationResolver;

    public BulkPbaCheck(BulkClaimantOrganisationResolver organisationResolver) {
        this.organisationResolver = organisationResolver;
    }

    /**
     * Validates the claim's own payment account against the organisation's accounts.
     *
     * @param overridePba the claim's own payment account, or null when it uses the request-level account
     * @param path absolute path of the override element, used in the error
     * @return an error when an override is supplied and is not one of the organisation's accounts
     */
    public Optional<BulkValidationError> validateOverride(OrganisationDetailsResponse organisation,
                                                          String overridePba,
                                                          String path) {
        if (overridePba == null || organisationResolver.isPbaAccountValid(organisation, overridePba)) {
            return Optional.empty();
        }
        return Optional.of(new BulkValidationError(
            path, BulkErrorCollector.INVALID_VALUE,
            "The payment account is not valid for this organisation"));
    }

    public static String effectiveAccount(String requestLevelPba, String overridePba) {
        return StringUtils.defaultIfBlank(overridePba, requestLevelPba);
    }
}
