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
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ASBQuestionsWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AdditionalReasonsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AlternativesToPossessionOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.CheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantCircumstancesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimingCosts;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.CompletingYourClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DailyRentAmount;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantCircumstancesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DemotionOfTenancyHousingActOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DemotionOfTenancyOrderReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.GroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.IntroductoryDemotedOrOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.IntroductoryDemotedOtherGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.MediationAndSettlement;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.MoneyJudgment;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoRentArrearsGroundsForPossessionOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoRentArrearsGroundsForPossessionReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.PreActionProtocol;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrears;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundForPossessionAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsOrBreachOfTenancyGround;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ResumeClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SecureOrFlexibleGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SecureOrFlexibleGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.StatementOfExpressTerms;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionOfRightToBuyHousingActOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionOfRightToBuyOrderReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionToBuyDemotionOfTenancyActs;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionToBuyDemotionOfTenancyOrderReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UnderlesseeMortgageeEntitledToClaimRelief;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UploadAdditionalDocumentsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.WalesCheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.WantToUploadDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.OccupationLicenceDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ReasonsForPosessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationNameService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_FURTHER_CLAIM_DETAILS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEE_CASE_ISSUED_TASK_DESCRIPTOR;

@Slf4j
@Component
@AllArgsConstructor
public class ResumePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final PartyService partyService;
    private final ClaimService claimService;
    private final SavingPageBuilderFactory savingPageBuilderFactory;
    private final ResumeClaim resumeClaim;
    private final SelectClaimantType selectClaimantType;
    private final NoticeDetails noticeDetails;
    private final UploadAdditionalDocumentsDetails uploadAdditionalDocumentsDetails;
    private final TenancyLicenceDetails tenancyLicenceDetails;
    private final ContactPreferences contactPreferences;
    private final DefendantsDetails defendantsDetails;
    private final NoRentArrearsGroundsForPossessionReason noRentArrearsGroundsForPossessionReason;
    private final AdditionalReasonsForPossession additionalReasonsForPossession;
    private final SecureOrFlexibleGroundsForPossessionReasons secureOrFlexibleGroundsForPossessionReasons;
    private final MediationAndSettlement mediationAndSettlement;
    private final ClaimantCircumstancesPage claimantCircumstancesPage;
    private final IntroductoryDemotedOtherGroundsReasons introductoryDemotedOtherGroundsReasons;
    private final DefendantCircumstancesPage defendantCircumstancesPage;
    private final SuspensionOfRightToBuyOrderReason suspensionOfRightToBuyOrderReason;
    private final StatementOfExpressTerms statementOfExpressTerms;
    private final DemotionOfTenancyOrderReason demotionOfTenancyOrderReason;
    private final OrganisationNameService organisationNameService;
    private final ClaimantDetailsWalesPage claimantDetailsWales;
    private final ProhibitedConductWales prohibitedConductWalesPage;
    private final SchedulerClient schedulerClient;
    private final DraftCaseDataService draftCaseDataService;
    private final OccupationLicenceDetailsWalesPage occupationLicenceDetailsWalesPage;
    private final GroundsForPossessionWales groundsForPossessionWales;
    private final SecureContractGroundsForPossessionWales secureContractGroundsForPossessionWales;
    private final ReasonsForPosessionWales reasonsForPosessionWales;
    private final AddressFormatter addressFormatter;
    private final RentArrearsGroundsForPossession rentArrearsGroundsForPossession;
    private final RentArrearsGroundForPossessionAdditionalGrounds rentArrearsGroundForPossessionAdditionalGrounds;
    private final NoRentArrearsGroundsForPossessionOptions noRentArrearsGroundsForPossessionOptions;
    private final IntroductoryDemotedOrOtherGroundsForPossession introductoryDemotedOrOtherGroundsForPossession;

    private static final String CASE_ISSUED_FEE_TYPE = "caseIssueFee";

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(resumePossessionClaim.name(), this::submit, this::start)
                .forStateTransition(AWAITING_FURTHER_CLAIM_DETAILS, AWAITING_SUBMISSION_TO_HMCTS)
                .name("Make a claim")
                .showCondition(ShowConditions.NEVER_SHOW)
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();

        savingPageBuilderFactory.create(eventBuilder)
            .add(resumeClaim)
            .add(selectClaimantType)
            .add(new ClaimantTypeNotEligibleEngland())
            .add(new ClaimantTypeNotEligibleWales())
            .add(new SelectClaimType())
            .add(new ClaimTypeNotEligibleEngland())
            .add(new ClaimTypeNotEligibleWales())
            .add(new ClaimantInformation())
            .add(claimantDetailsWales)
            .add(contactPreferences)
            .add(defendantsDetails)
            .add(tenancyLicenceDetails)
            .add(occupationLicenceDetailsWalesPage)
            .add(groundsForPossessionWales)
            .add(secureContractGroundsForPossessionWales)
            .add(reasonsForPosessionWales)
            .add(new ASBQuestionsWales())
            .add(new SecureOrFlexibleGroundsForPossession())
            .add(new RentArrearsOrBreachOfTenancyGround())
            .add(secureOrFlexibleGroundsForPossessionReasons)
            .add(introductoryDemotedOrOtherGroundsForPossession)
            .add(introductoryDemotedOtherGroundsReasons)
            .add(new GroundsForPossession())
            .add(rentArrearsGroundsForPossession)
            .add(rentArrearsGroundForPossessionAdditionalGrounds)
            .add(new RentArrearsGroundsForPossessionReasons())
            .add(noRentArrearsGroundsForPossessionOptions)
            .add(noRentArrearsGroundsForPossessionReason)
            .add(new PreActionProtocol())
            .add(mediationAndSettlement)
            .add(new CheckingNotice())
            .add(new WalesCheckingNotice())
            .add(noticeDetails)
            .add(new RentDetails())
            .add(new DailyRentAmount())
            .add(new RentArrears())
            .add(new MoneyJudgment())
            .add(claimantCircumstancesPage)
            .add(defendantCircumstancesPage)
            .add(prohibitedConductWalesPage)
            .add(new AlternativesToPossessionOptions())
            .add(new SuspensionOfRightToBuyHousingActOptions())
            .add(suspensionOfRightToBuyOrderReason)
            .add(new DemotionOfTenancyHousingActOptions())
            .add(new SuspensionToBuyDemotionOfTenancyActs())
            .add(statementOfExpressTerms)
            .add(demotionOfTenancyOrderReason)
            .add(new SuspensionToBuyDemotionOfTenancyOrderReasons())
            .add(new ClaimingCosts())
            .add(additionalReasonsForPossession)
            .add(new UnderlesseeMortgageeEntitledToClaimRelief())
            .add(new UnderlesseeMortgageeDetails())
            //TO DO will be routed later on  correctly using tech debt ticket
            .add(new WantToUploadDocuments())
            .add(uploadAdditionalDocumentsDetails)
            .add(new GeneralApplication())
            .add(new LanguageUsed())
            .add(new CompletingYourClaim())
            .add(new StatementOfTruth());

    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        String userEmail = securityContextService.getCurrentUserDetails().getSub();

        // Fetch organisation name from rd-professional API
        String organisationName = organisationNameService.getOrganisationNameForCurrentUser();
        if (organisationName != null) {
            caseData.setOrganisationName(organisationName);
        } else {
            // Fallback to user details if organisation name cannot be retrieved
            caseData.setOrganisationName(userEmail);
            log.warn("Could not retrieve organisation name, using user details as fallback");
        }

        caseData.setClaimantContactEmail(userEmail);

        AddressUK propertyAddress = caseData.getPropertyAddress();
        if (propertyAddress == null) {
            throw new IllegalStateException("Cannot resume claim without property address already set");
        }

        LegislativeCountry legislativeCountry = caseData.getLegislativeCountry();
        if (legislativeCountry == null) {
            throw new IllegalStateException("Cannot resume claim without legislative country already set");
        }

        List<DynamicStringListElement> listItems = Arrays.stream(ClaimantType.values())
            .filter(value -> value.isApplicableFor(legislativeCountry))
            .map(value -> DynamicStringListElement.builder().code(value.name()).label(value.getLabel())
                .build())
            .toList();

        DynamicStringList claimantTypeList = DynamicStringList.builder()
            .listItems(listItems)
            .build();
        caseData.setClaimantType(claimantTypeList);
        caseData.setFormattedClaimantContactAddress(addressFormatter.getFormattedAddress(caseData));

        return caseData;
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
