package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;


@Component
public class GeneralApplicationCaseType implements CCDConfig<GeneralApplication, State, UserRole> {
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
    public void configure(ConfigBuilder<GeneralApplication, State, UserRole> configBuilder) {
        configBuilder.setCallbackHost(getenv().getOrDefault("CASE_API_URL", "http://localhost:3206"));

        configBuilder.decentralisedCaseType(getCaseType(), getCaseTypeName(), CASE_TYPE_DESCRIPTION);
        configBuilder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);

        var applicationLabel = "Applicantion ID";
        var adjustmentLabel = "Adjusments";
        var statusLabel = "Status";

        configBuilder.searchInputFields()
            .field(GeneralApplication::getApplicationId, applicationLabel);

        configBuilder.searchCasesFields()
            .field(GeneralApplication::getApplicationId, applicationLabel);

        configBuilder.searchResultFields()
            .field(GeneralApplication::getApplicationId, applicationLabel)
            .field(GeneralApplication::getApplicationId, adjustmentLabel)
            .field(GeneralApplication::getStatus, statusLabel);

        configBuilder.searchResultFields()
            .field(GeneralApplication::getApplicationId, applicationLabel)
            .field(GeneralApplication::getApplicationId, adjustmentLabel);

        configBuilder.workBasketInputFields()
            .field(GeneralApplication::getApplicationId, applicationLabel);

        configBuilder.workBasketResultFields()
            .field(GeneralApplication::getApplicationId, applicationLabel)
            .field(GeneralApplication::getApplicationId, adjustmentLabel);



    }
}
