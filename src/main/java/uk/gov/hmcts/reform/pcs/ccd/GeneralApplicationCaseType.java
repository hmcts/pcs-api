package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;


@Component
public class GeneralApplicationCaseType implements CCDConfig<GACase, State, UserRole> {
    private static final String CASE_TYPE_ID = "GA";
    private static final String CASE_TYPE_NAME = "Possessions Gen Application";
    private static final String CASE_TYPE_DESCRIPTION = "General Application Case Type";
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

        var applicationLabel = "Applicantion ID";
        var adjustmentLabel = "Adjusments";
        var additionalInfoLabel = "Additional Info";
        var statusLabel = "Status";

        configBuilder.searchInputFields()
            .field(GACase::getCaseReference, applicationLabel);

        configBuilder.searchCasesFields()
            .field(GACase::getCaseReference, applicationLabel);

        configBuilder.searchResultFields()
            .field(GACase::getCaseReference, applicationLabel)
            .field(GACase::getCaseReference, adjustmentLabel)
            .field(GACase::getStatus, statusLabel);

        configBuilder.searchResultFields()
            .field(GACase::getCaseReference, applicationLabel)
            .field(GACase::getAdjustment, adjustmentLabel);

        configBuilder.workBasketInputFields()
            .field(GACase::getCaseReference, applicationLabel)
            .field(GACase::getAdjustment, adjustmentLabel);

        configBuilder.workBasketResultFields()
            .field(GACase::getCaseReference, applicationLabel)
            .field(GACase::getAdjustment, adjustmentLabel);

        configBuilder.tab("Adjustments", adjustmentLabel)
            .field(GACase::getAdjustment);

        configBuilder.tab("AdditionalInfo", additionalInfoLabel)
            .field(GACase::getAdditionalInformation);
        //.field("generalApplicationsSummaryMarkdown", "[STATE]=\"NEVER_SHOW\"");



    }
}
