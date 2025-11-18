package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.SupportTestCaseCreationPage;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.TestingSupportService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationNameService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestCase;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEE_CASE_ISSUED_TASK_DESCRIPTOR;

@Component
@Slf4j
@AllArgsConstructor
public class CaseCreationTestingSupport implements CCDConfig<PCSCase, State, UserRole> {

    private final SecurityContextService securityContextService;
    private final OrganisationNameService organisationNameService;
    private final SchedulerClient schedulerClient;
    private final PcsCaseService pcsCaseService;
    private final TestingSupportService testingSupportService;
    private final DraftCaseDataService draftCaseDataService;
    private final PartyService partyService;
    private final ClaimService claimService;
    private final SavingPageBuilderFactory savingPageBuilderFactory;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createTestCase.name(), this::submit, this::start)
                .initialState(DRAFT)
                .name("DA & QA - Test Case Creation")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();

        savingPageBuilderFactory.create(eventBuilder).add(new SupportTestCaseCreationPage());

    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        testingSupportService.generateTestPCSCase(pcsCase);
        long caseReference = System.currentTimeMillis();

        String userEmail = securityContextService.getCurrentUserDetails().getSub();
        String organisationName = organisationNameService.getOrganisationNameForCurrentUser();
        if (organisationName != null) {
            pcsCase.setOrganisationName(organisationName);
        } else {
            pcsCase.setOrganisationName(userEmail);
        }
        pcsCase.setClaimantContactEmail(userEmail);
        pcsCaseService.createCase(caseReference, pcsCase.getPropertyAddress(), pcsCase.getLegislativeCountry());
        draftCaseDataService.patchUnsubmittedCaseData(caseReference, pcsCase);
        return pcsCase;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        List<ListValue<DefendantDetails>> defendantsList = new ArrayList<>();
        if (pcsCase.getDefendant1() != null) {
            if (VerticalYesNo.YES == pcsCase.getDefendant1().getAddressSameAsPossession()) {
                pcsCase.getDefendant1().setCorrespondenceAddress(pcsCase.getPropertyAddress());
            }
            defendantsList.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant1()));
            pcsCaseService.clearHiddenDefendantDetailsFields(defendantsList);
            pcsCase.setDefendants(defendantsList);
        }

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        pcsCaseService.mergeCaseData(pcsCaseEntity, pcsCase);

        PartyEntity claimantPartyEntity = createClaimantPartyEntity(pcsCase);
        pcsCaseEntity.addParty(claimantPartyEntity);

        ClaimEntity claimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);
        pcsCaseEntity.addClaim(claimEntity);

        pcsCaseService.save(pcsCaseEntity);

        draftCaseDataService.deleteUnsubmittedCaseData(caseReference);

        scheduleCaseIssuedFeeTask(caseReference, pcsCase.getOrganisationName());

        return SubmitResponse.defaultResponse();
    }

    private PartyEntity createClaimantPartyEntity(PCSCase pcsCase) {
        UserInfo userDetails = securityContextService.getCurrentUserDetails();
        UUID userID = UUID.fromString(userDetails.getUid());

        String claimantName = isNotBlank(pcsCase.getOverriddenClaimantName())
            ? pcsCase.getOverriddenClaimantName() : pcsCase.getClaimantName();

        AddressUK contactAddress = pcsCase.getOverriddenClaimantContactAddress() != null
            ? pcsCase.getOverriddenClaimantContactAddress() : pcsCase.getPropertyAddress();

        String contactEmail = isNotBlank(pcsCase.getOverriddenClaimantContactEmail())
            ? pcsCase.getOverriddenClaimantContactEmail() : pcsCase.getClaimantContactEmail();

        return partyService.createPartyEntity(
            userID,
            claimantName,
            null,
            contactEmail,
            contactAddress,
            pcsCase.getClaimantContactPhoneNumber()
        );
    }

    private static final String CASE_ISSUED_FEE_TYPE = "caseIssueFee";

    private void scheduleCaseIssuedFeeTask(long caseReference, String responsibleParty) {
        String taskId = UUID.randomUUID().toString();

        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeType(CASE_ISSUED_FEE_TYPE)
            .ccdCaseNumber(String.valueOf(caseReference))
            .caseReference(String.valueOf(caseReference))
            .responsibleParty(responsibleParty)
            .build();

        schedulerClient.scheduleIfNotExists(
            FEE_CASE_ISSUED_TASK_DESCRIPTOR
                .instance(taskId)
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }

}
