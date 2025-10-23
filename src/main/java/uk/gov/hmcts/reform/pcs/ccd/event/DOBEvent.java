package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDOBPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ResumeClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UploadAdditionalDocumentsDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.UnsubmittedCaseDataService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationNameService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.dobEvent;

@Slf4j
@Component
@AllArgsConstructor
public class DOBEvent implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final PartyService partyService;
    private final ClaimService claimService;
    private final SavingPageBuilderFactory savingPageBuilderFactory;
    private final ResumeClaim resumeClaim;
    private final UnsubmittedCaseDataService unsubmittedCaseDataService;
    private final SelectClaimantType selectClaimantType;
    private final NoticeDetails noticeDetails;
    private final UploadAdditionalDocumentsDetails uploadAdditionalDocumentsDetails;
    private final TenancyLicenceDetails tenancyLicenceDetails;
    private final ContactPreferences contactPreferences;
    private final DefendantsDetails defendantsDetails;
    private final OrganisationNameService organisationNameService;
    private final ClaimantDetailsWalesPage claimantDetailsWales;
    private final SchedulerClient schedulerClient;

    private static final String CASE_ISSUED_FEE_TYPE = "caseIssueFee";
    private final DefendantsDetails defendantDetails;
    private final DefendantsDOBPage defendantsDOBPage;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(dobEvent.name(), this::submit, this::start)
                .forStateTransition(AWAITING_SUBMISSION_TO_HMCTS, CASE_ISSUED)
                .name("DOB Event")
                .showCondition(ShowConditions.NEVER_SHOW)
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();

        new PageBuilder(eventBuilder).add(defendantsDOBPage);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        return caseData;
    }

    private SubmitResponse submit(EventPayload<PCSCase, State> eventPayload) {

        return SubmitResponse.builder().build();
    }
}
