package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class CaseType implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.setCallbackHost("http://localhost:3206");

        builder.caseType("PCS", "Civil Possessions", "Possessions");
        builder.jurisdiction("CIVIL", "Civil Possessions", "The new one");

        var label = "Applicant Forename";
        builder.searchInputFields()
            .field(PCSCase::getApplicantForename, label);
        builder.searchCasesFields()
            .field(PCSCase::getApplicantForename, label);

        builder.searchResultFields()
            .field(PCSCase::getApplicantForename, label);
        builder.workBasketInputFields()
            .field(PCSCase::getApplicantForename, label);
        builder.workBasketResultFields()
            .field(PCSCase::getApplicantForename, label);

        builder.tab("Example", "Example Tab")
            .field(PCSCase::getApplicantForename)
            .field(PCSCase::getPartyA);
    }
}
