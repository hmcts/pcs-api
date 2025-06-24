package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;

/**
 * Setup some common possessions case type configuration.
 */
@Component
public class PCSCaseType implements CCDConfig<PCSCase, State, UserRole> {

    private static final String CASE_TYPE_ID = "PCS";
    private static final String CASE_TYPE_NAME = "Civil Possessions";
    private static final String CASE_TYPE_DESCRIPTION = "Civil Possessions Case Type";
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
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> builder) {
        builder.setCallbackHost(getenv().getOrDefault("CASE_API_URL", "http://localhost:3206"));

        builder.decentralisedCaseType(getCaseType(), getCaseTypeName(), CASE_TYPE_DESCRIPTION);
        builder.jurisdiction(JURISDICTION_ID, JURISDICTION_NAME, JURISDICTION_DESCRIPTION);

        var nameLabel = "Applicant's first name";
        var addressLabel = "Property Address";
        var claimantLabel = "Claimant Information";
        builder.searchInputFields()
            .field(PCSCase::getApplicantForename, nameLabel);

        builder.searchCasesFields()
            .field(PCSCase::getApplicantForename, nameLabel);

        builder.searchResultFields()
            .field(PCSCase::getApplicantForename, nameLabel)
            .field(PCSCase::getPropertyAddress, addressLabel);

        builder.workBasketInputFields()
            .field(PCSCase::getApplicantForename, nameLabel);

        builder.workBasketResultFields()
            .field(PCSCase::getApplicantForename, nameLabel)
            .field(PCSCase::getPropertyAddress, addressLabel);

        builder.tab("claimantInformation", claimantLabel)
            .field(PCSCase::getApplicantForename);

        builder.tab("propertyAddress", addressLabel)
            .field(PCSCase::getPropertyAddress);

        builder.tab("generalApplicationsTab", "General Applications")
            .field(PCSCase::getGeneralApplicationList);


        /**
         *  builder.tab("generalApplicationsTab", "General Applications")
         *  .collection(PCSCase::getGeneralApplicationList, "General Applications")
         *  .field(GeneralApplication::getAdjustment);
         *
         * need wrapper class to make this work e.g. GeneralApplicationwWrapper with
         * fields of case reference and adjustment
         * Pcs list of this wrapper that we populate rather than the gen app list
         */

    }
    /**
     * private void createGeneralApplicationEvent(ConfigBuilder<PCSCase, State, UserRole> builder) {
     builder.event("createGeneralApplication")
     .initialState(State.Submitted)
     .name("Create Draft General Application")
     .showSummary()
     .showEventNotes()
     .grant(Permission.CRUD, UserRole.CASE_WORKER)
     .aboutToSubmitCallback(this::createDraftGA);
     }

     private void submitGeneralApplicationEvent(ConfigBuilder<PCSCase, State, UserRole> builder) {
     builder.event("submitGeneralApplication")
     .initialState(State.Submitted)
     .name("Submit General Application")
     .showSummary()
     .showEventNotes()
     .grant(Permission.CRUD, UserRole.CASE_WORKER)
     .aboutToSubmitCallback(this::submitDraftGA);
     }

     private void deleteGeneralApplicationEvent(ConfigBuilder<PCSCase, State, UserRole> builder) {
     builder.event("deleteGeneralApplication")
     .initialState(State.Submitted)
     .name("Delete Draft General Application")
     .showSummary()
     .showEventNotes()
     .grant(Permission.CRUD, UserRole.CASE_WORKER)
     .aboutToSubmitCallback(this::deleteDraftGA);
     }
     */
}
