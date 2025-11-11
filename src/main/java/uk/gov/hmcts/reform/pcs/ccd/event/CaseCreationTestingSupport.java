package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Profile("develop")
public class CaseCreationTestingSupport implements CCDConfig<PCSCase, State, UserRole> {

    private static final String ENVIRONMENT_PROD = "prod";
    public static final String TEST_CREATE = "caseworker-create-case";

    private static final CcdPageConfiguration categorisationDetails = new CaseCategorisationDetails();
    private static final CcdPageConfiguration dateOfReceipt = new DateOfReceipt();
    private static final CcdPageConfiguration selectParties = new SelectParties();
    private static final CcdPageConfiguration caseUploadDocuments = new CaseUploadDocuments();
    private static final CcdPageConfiguration subjectDetails = new SubjectDetails();
    private static final CcdPageConfiguration applicantDetails = new ApplicantDetails();
    private static final CcdPageConfiguration representativeDetails = new RepresentativeDetails();
    private static final CcdPageConfiguration furtherDetails = new FurtherDetails();
    private static final CcdPageConfiguration contactPreferenceDetails = new ContactPreferenceDetails();



    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        PageBuilder pageBuilder = new PageBuilder(configBuilder
                                                      .event(TEST_CREATE)
                                                      .initialState(Draft)
                                                      .name("Create Case")
                                                      .showSummary()
                                                      .aboutToSubmitCallback(this::aboutToSubmit)
                                                      .submittedCallback(this::submitted)
                                                      .grant(Permission.CRUD, UserRole.PCS_SOLICITOR));

        categorisationDetails.addTo(pageBuilder);
        dateOfReceipt.addTo(pageBuilder);
        selectParties.addTo(pageBuilder);
        subjectDetails.addTo(pageBuilder);
        applicantDetails.addTo(pageBuilder);
        representativeDetails.addTo(pageBuilder);
        contactPreferenceDetails.addTo(pageBuilder);
        caseUploadDocuments.addTo(pageBuilder);
        furtherDetails.addTo(pageBuilder);
    }


    @SneakyThrows
    public AboutToStartOrSubmitResponse<CaseData, State> aboutToSubmit(CaseDetails<CaseData, State> details,
                                                                       CaseDetails<CaseData, State> beforeDetails) {

        final CaseDetails<CaseData, State> submittedDetails = submissionService.submitApplication(details);
        final CaseData caseData = submittedDetails.getData();

        updateCategoryToCaseworkerDocument(caseData.getCicCase().getApplicantDocumentsUploaded());
        setIsRepresentativePresent(caseData);
        caseData.setSecurityClass(SecurityClass.PUBLIC);
        caseData.setCaseNameHmctsInternal(caseData.getCicCase().getFullName());

        initialiseFlags(caseData);

        return AboutToStartOrSubmitResponse.<CaseData, State>builder()
            .data(caseData)
            .state(submittedDetails.getState())
            .build();
    }

    public SubmitResponse<State> submitted(CaseDetails<PCSCase, State> details,
                                    CaseDetails<PCSCase, State> beforeDetails) {

        long caseReference = eventPayload.caseReference();
        setSupplementaryData(details.getId());
        final String caseReference = caseData.get();

        try {
            sendApplicationReceivedNotification(caseReference, caseData);
        } catch (Exception notificationException) {
            log.error("Create case notification failed with exception : {}", notificationException.getMessage());
            return SubmittedCallbackResponse.builder()
                .confirmationHeader(format("# Create case notification failed %n## Please resend the notification"))
                .build();
        }

        return SubmittedCallbackResponse.builder()
            .confirmationHeader(format("# Case Created %n## Case reference number: %n## %s", caseReference))
            .build();
    }

    private void initialiseFlags(CaseData data) {
        data.setCaseFlags(Flags.builder()
                              .details(new ArrayList<>())
                              .partyName(null)
                              .roleOnCase(null)
                              .build());

        if (data.getCicCase().getFullName() != null) {
            data.setSubjectFlags(Flags.builder()
                                     .details(new ArrayList<>())
                                     .partyName(data.getCicCase().getFullName())
                                     .roleOnCase("subject")
                                     .build()
            );
        }

        if (data.getCicCase().getApplicantFullName() != null) {
            data.setApplicantFlags(Flags.builder()
                                       .details(new ArrayList<>())
                                       .partyName(data.getCicCase().getApplicantFullName())
                                       .roleOnCase("applicant")
                                       .build()
            );
        }

        if (data.getCicCase().getRepresentativeFullName() != null) {
            data.setRepresentativeFlags(Flags.builder()
                                            .details(new ArrayList<>())
                                            .partyName(data.getCicCase().getRepresentativeFullName())
                                            .roleOnCase("Representative")
                                            .build()
            );
        }
    }

    private void setSupplementaryData(Long caseId) {
        try {
            ccdSupplementaryDataService.submitSupplementaryDataToCcd(caseId.toString());
        } catch (Exception exception) {
            log.error("Unable to set Supplementary data with exception : {}", exception.getMessage());
        }
    }

    private void sendApplicationReceivedNotification(String caseNumber, PCSCase data) {
        final CicCase cicCase = data.getCicCase();

        if (isNotEmpty(cicCase.getSubjectCIC())) {
            applicationReceivedNotification.sendToSubject(data, caseNumber);
        }

        if (isNotEmpty(cicCase.getApplicantCIC())) {
            applicationReceivedNotification.sendToApplicant(data, caseNumber);
        }

        if (isNotEmpty(cicCase.getRepresentativeCIC())) {
            applicationReceivedNotification.sendToRepresentative(data, caseNumber);
        }
    }

}
