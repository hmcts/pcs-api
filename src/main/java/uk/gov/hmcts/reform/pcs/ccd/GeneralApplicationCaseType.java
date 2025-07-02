package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;


@Component
public class GeneralApplicationCaseType implements CCDConfig<GACase, State, UserRole> {
    public static final String CASE_TYPE_ID = "PCS-GA";
    private static final String CASE_TYPE_NAME = "Possessions Gen App";
    private static final String CASE_TYPE_DESCRIPTION = "Gen App Case Type";
    private static final String JURISDICTION_ID = "PCS";
    private static final String JURISDICTION_NAME = "Possessions";
    private static final String JURISDICTION_DESCRIPTION = "Possessions Jurisdiction";


    public static String getCaseType() {
        return withChangeId(CASE_TYPE_ID, "-");
    }

    public static String getCaseTypeName() {
        return withChangeId(CASE_TYPE_NAME, " ");
    }

    private static String withChangeId(String base, String separator) {
        return ofNullable(getenv().get("CHANGE_ID"))
            .map(changeId -> base + separator + changeId)
            .orElse(base);
    }

    @Override
    public void configure(ConfigBuilder<GACase, State, UserRole> configBuilder) {
        configBuilder.setCallbackHost(getenv().getOrDefault("CASE_API_URL", "http://localhost:3206"));

        configBuilder.decentralisedCaseType(getCaseType(), getCaseTypeName(), CASE_TYPE_DESCRIPTION);
        configBuilder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);

        var caseRefLabel = "Case reference";
        var overview = "Overview";
        var gaTypeLabel = "Type";
        var statusLabel = "Status";
        var parentCaseLabel = "Parent case";

        configBuilder.searchInputFields()
            .field(GACase::getGaType, gaTypeLabel);

        configBuilder.searchCasesFields()

            .field(GACase::getGaType, gaTypeLabel);

        configBuilder.searchResultFields()

            .field(GACase::getGaType, gaTypeLabel)
            .field(GACase::getStatus, statusLabel)
            .field(GACase::getCaseLink, parentCaseLabel);

        configBuilder.workBasketInputFields()
            .field(GACase::getCaseReference, caseRefLabel);

        configBuilder.workBasketResultFields()

            .field(GACase::getGaType, gaTypeLabel)
            .field(GACase::getStatus, statusLabel)
            .field(GACase::getCaseLink, parentCaseLabel);

        configBuilder.tab("Overview", overview)

            .field(GACase::getGaType)
            .field(GACase::getStatus)
            .field(GACase::getAdjustment)
            .field(GACase::getAdditionalInformation)
            .field(GACase::getCaseLink);
    }
}
