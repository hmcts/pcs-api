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

        builder.decentralisedCaseType("PCS", "Civil Possessions", "Possessions");
        builder.jurisdiction("CIVIL", "Civil Possessions", "The new one");

        var label = "Case description";
        builder.searchInputFields()
            .field(PCSCase::getCaseDescription, label);
        builder.searchCasesFields()
            .field(PCSCase::getCaseDescription, label);

        builder.searchCasesFields()
            .field(PCSCase::getState, "State");


        builder.searchResultFields()
            .field(PCSCase::getCaseDescription, label);
        builder.searchResultFields()
                .field(PCSCase::getState, "State");

        builder.workBasketInputFields()
            .field(PCSCase::getCaseDescription, label);
        builder.workBasketResultFields()
            .field(PCSCase::getCaseDescription, label);

        builder.workBasketResultFields()
            .field(PCSCase::getState, "State");


        builder.tab("Example", "Example Tab")
            .label("myLabel", null, "${exampleTabMarkdown}")
            .field("exampleTabMarkdown", "exampleTabMarkdown=\"NEVER_SHOW\"");
    }
}
