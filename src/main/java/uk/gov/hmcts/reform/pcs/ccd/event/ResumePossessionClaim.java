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
import uk.gov.hmcts.ccd.sdk.type.Organisation;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.assigncaseaccess.AssignCaseAccessService;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.AccessCodeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AdditionalReasonsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AlternativesToPossessionOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.CheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantCircumstancesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantInformationPage;
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
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UnderlesseeOrMortgageeDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UnderlesseeOrMortgageeEntitledToClaimRelief;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UploadAdditionalDocumentsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.WalesCheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.WantToUploadDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ASBQuestionsWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.OccupationLicenceDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ReasonsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.SecureContractGroundsForPossessionWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep.SUBMIT_AND_PAY_NOW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.task.AccessCodeGenerationComponent.ACCESS_CODE_TASK_DESCRIPTOR;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;
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
    private final IntroductoryDemotedOrOtherGroundsForPossession introductoryDemotedOrOtherGroundsForPossession;
    private final RentArrearsGroundsForPossessionReasons rentArrearsGroundsForPossessionReasons;
    private final SuspensionToBuyDemotionOfTenancyOrderReasons suspensionToBuyDemotionOfTenancyOrderReasons;
    private final DefendantCircumstancesPage defendantCircumstancesPage;
    private final SuspensionOfRightToBuyOrderReason suspensionOfRightToBuyOrderReason;
    private final StatementOfExpressTerms statementOfExpressTerms;
    private final DemotionOfTenancyOrderReason demotionOfTenancyOrderReason;
    private final OrganisationService organisationService;
    private final ClaimantDetailsWalesPage claimantDetailsWales;
    private final ProhibitedConductWales prohibitedConductWalesPage;
    private final SchedulerClient schedulerClient;
    private final DraftCaseDataService draftCaseDataService;
    private final OccupationLicenceDetailsWalesPage occupationLicenceDetailsWalesPage;
    private final GroundsForPossessionWales groundsForPossessionWales;
    private final SecureContractGroundsForPossessionWalesPage secureContractGroundsForPossessionWales;
    private final ReasonsForPossessionWales reasonsForPossessionWales;
    private final AddressFormatter addressFormatter;
    private final RentArrearsGroundsForPossession rentArrearsGroundsForPossession;
    private final RentArrearsGroundForPossessionAdditionalGrounds rentArrearsGroundForPossessionAdditionalGrounds;
    private final NoRentArrearsGroundsForPossessionOptions noRentArrearsGroundsForPossessionOptions;
    private final CheckingNotice checkingNotice;
    private final WalesCheckingNotice walesCheckingNotice;
    private final ASBQuestionsWales asbQuestionsWales;
    private final UnderlesseeOrMortgageeDetailsPage underlesseeOrMortgageeDetailsPage;
    private final AssignCaseAccessService assignCaseAccessService;
    private final OrganisationDetailsService organisationDetailsService;
    private final FeeService feeService;
    private final FeeFormatter feeFormatter;


    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(resumePossessionClaim.name(), this::submit, this::start)
                .forState(AWAITING_SUBMISSION_TO_HMCTS)
                .name("Make a claim")
                .showCondition(ShowConditions.NEVER_SHOW)
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();

        savingPageBuilderFactory.create(eventBuilder, resumePossessionClaim)
            .add(resumeClaim)
            .add(selectClaimantType)
            .add(new ClaimantTypeNotEligibleEngland())
            .add(new ClaimantTypeNotEligibleWales())
            .add(new SelectClaimType())
            .add(new ClaimTypeNotEligibleEngland())
            .add(new ClaimTypeNotEligibleWales())
            .add(new ClaimantInformationPage())
            .add(claimantDetailsWales)
            .add(contactPreferences)
            .add(defendantsDetails)
            .add(tenancyLicenceDetails)
            .add(occupationLicenceDetailsWalesPage)
            .add(groundsForPossessionWales)
            .add(secureContractGroundsForPossessionWales)
            .add(reasonsForPossessionWales)
            .add(asbQuestionsWales)
            .add(new SecureOrFlexibleGroundsForPossession())
            .add(new RentArrearsOrBreachOfTenancyGround())
            .add(secureOrFlexibleGroundsForPossessionReasons)
            .add(introductoryDemotedOrOtherGroundsForPossession)
            .add(introductoryDemotedOtherGroundsReasons)
            .add(new GroundsForPossession())
            .add(rentArrearsGroundsForPossession)
            .add(rentArrearsGroundForPossessionAdditionalGrounds)
            .add(rentArrearsGroundsForPossessionReasons)
            .add(noRentArrearsGroundsForPossessionOptions)
            .add(noRentArrearsGroundsForPossessionReason)
            .add(new PreActionProtocol())
            .add(mediationAndSettlement)
            .add(checkingNotice)
            .add(walesCheckingNotice)
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
            .add(suspensionToBuyDemotionOfTenancyOrderReasons)
            .add(new ClaimingCosts())
            .add(additionalReasonsForPossession)
            .add(new UnderlesseeOrMortgageeEntitledToClaimRelief())
            .add(underlesseeOrMortgageeDetailsPage)
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
        String organisationName = organisationService.getOrganisationNameForCurrentUser();
        ClaimantInformation claimantInfo = getClaimantInfo(caseData);

        if (organisationName != null) {
            claimantInfo.setOrganisationName(organisationName);
        } else {
            // Fallback to user details if organisation name cannot be retrieved
            claimantInfo.setOrganisationName(userEmail);
            log.warn("Could not retrieve organisation name, using user details as fallback");
        }

        ClaimantContactPreferences contactPreferences = caseData.getContactPreferencesDetails();
        if (contactPreferences == null) {
            contactPreferences = ClaimantContactPreferences.builder().build();
        }
        contactPreferences.setClaimantContactEmail(userEmail);
        caseData.setContactPreferencesDetails(contactPreferences);
        caseData.setClaimantInformation(claimantInfo);
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

        contactPreferences.setFormattedClaimantContactAddress(addressFormatter
            .formatMediumAddress(organisationService.getOrganisationAddressForCurrentUser(), BR_DELIMITER));

        caseData.setContactPreferencesDetails(contactPreferences);

        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        if (pcsCase.getCompletionNextStep() == SUBMIT_AND_PAY_NOW) {
            return submitClaim(caseReference, pcsCase);
        } else {
            return saveForLater();
        }
    }

    private SubmitResponse<State> submitClaim(long caseReference, PCSCase pcsCase) {
        String orgId = Optional.ofNullable(organisationDetailsService
                                               .getOrganisationIdentifier(securityContextService.getCurrentUserId()
                                                                              .toString())).orElse("E71FH4Q");
        pcsCase.setOrganisationPolicy(
            OrganisationPolicy.builder()
                .organisation(Organisation.builder()
                    .organisationId(orgId)
                    .organisationName(organisationService.getOrganisationNameForCurrentUser()).build())
                .prepopulateToUsersOrganisation(YesOrNo.YES)
                .orgPolicyReference("AUTO")
                .orgPolicyCaseAssignedRole(UserRole.CLAIMANT_SOLICITOR)
                .build());

        log.warn("Organisation ID : {}", orgId);
        log.warn("Organisation Name : {}", organisationService.getOrganisationNameForCurrentUser());
        log.warn("Organisation role : {}", UserRole.CLAIMANT);
        log.warn("Organisation Policy : {}", pcsCase.getOrganisationPolicy());

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        pcsCaseService.mergeCaseData(pcsCaseEntity, pcsCase);

        PartyEntity claimantPartyEntity = createClaimantPartyEntity(pcsCase);
        pcsCaseEntity.addParty(claimantPartyEntity);

        ClaimEntity claimEntity = claimService.createMainClaimEntity(pcsCase, claimantPartyEntity);
        pcsCaseEntity.addClaim(claimEntity);

        pcsCaseService.save(pcsCaseEntity);

        schedulePartyAccessCodeGeneration(caseReference);

        String responsibleParty = getClaimantInfo(pcsCase).getOrganisationName();
        FeeDetails feeDetails = scheduleCaseIssueFeePayment(caseReference, responsibleParty);
        String caseIssueFee = feeFormatter.formatFee(feeDetails.getFeeAmount());

        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, resumePossessionClaim);

        //assignCaseAccessService.assignRole(String.valueOf(caseReference), pcsCase);
        return SubmitResponse.<State>builder()
            .confirmationBody(getPaymentConfirmationMarkdown(caseIssueFee, caseReference))
            .state(State.PENDING_CASE_ISSUED)
            .build();
    }

    private SubmitResponse<State> saveForLater() {
        return SubmitResponse.<State>builder()
            .confirmationBody(getClaimSavedMarkdown())
            .state(AWAITING_SUBMISSION_TO_HMCTS)
            .build();
    }

    private PartyEntity createClaimantPartyEntity(PCSCase pcsCase) {
        UserInfo userDetails = securityContextService.getCurrentUserDetails();
        UUID userID = UUID.fromString(userDetails.getUid());

        ClaimantInformation claimantInfo = getClaimantInfo(pcsCase);

        String claimantName = isNotBlank(claimantInfo.getOverriddenClaimantName())
            ? claimantInfo.getOverriddenClaimantName()
            : claimantInfo.getClaimantName();

        ClaimantContactPreferences contactPreferences = getContactPreferences(pcsCase);

        AddressUK contactAddress = contactPreferences.getOverriddenClaimantContactAddress() != null
            ? contactPreferences.getOverriddenClaimantContactAddress() : pcsCase.getPropertyAddress();

        String contactEmail = isNotBlank(contactPreferences.getOverriddenClaimantContactEmail())
            ? contactPreferences.getOverriddenClaimantContactEmail() : contactPreferences.getClaimantContactEmail();

        String organisationName = isNotBlank(claimantInfo.getOverriddenClaimantName())
            ? claimantInfo.getOverriddenClaimantName() : claimantInfo.getOrganisationName();
        if (isBlank(organisationName)) {
            organisationName = claimantName;
        }

        return partyService.createPartyEntity(
            userID,
            claimantName,
            null,
            organisationName,
            contactEmail,
            contactAddress,
            contactPreferences.getClaimantContactPhoneNumber()
        );
    }

    private ClaimantInformation getClaimantInfo(PCSCase caseData) {
        return Optional.ofNullable(caseData.getClaimantInformation())
            .orElse(ClaimantInformation.builder().build());
    }

    private ClaimantContactPreferences getContactPreferences(PCSCase caseData) {
        return Optional.ofNullable(caseData.getContactPreferencesDetails())
            .orElse(ClaimantContactPreferences.builder().build());
    }

    private FeeDetails scheduleCaseIssueFeePayment(long caseReference, String responsibleParty) {

        FeeDetails feeDetails = feeService.getFee(FeeTypes.CASE_ISSUE_FEE.getCode());

        String taskId = UUID.randomUUID().toString();

        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeType(FeeTypes.CASE_ISSUE_FEE.getCode())
            .feeDetails(feeDetails)
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

        return feeDetails;
    }

    private void schedulePartyAccessCodeGeneration(long caseReference) {

        String taskId = UUID.randomUUID().toString();

        AccessCodeTaskData taskData = AccessCodeTaskData.builder()
            .caseReference(String.valueOf(caseReference))
            .build();

        schedulerClient.scheduleIfNotExists(
            ACCESS_CODE_TASK_DESCRIPTOR
            .instance(taskId)
                .data(taskData)
                .scheduledTo(Instant.now())
        );
    }

    private static String getPaymentConfirmationMarkdown(String caseIssueFee, long caseReference) {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Pay %s claim fee</span>
            </div>

            <h3>Make a payment</h3>

            You must pay the claim fee of %s. Your claim will not progress until this
            fee has been paid.
            <a href="/cases/case-details/%d#Service%%20Request"
                    class="govuk-link govuk-link--no-visited-state">Pay the claim fee</a>.
            """.formatted(caseIssueFee, caseIssueFee, caseReference);
    }

    private static String getClaimSavedMarkdown() {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
              <span class="govuk-panel__title govuk-!-font-size-36">Claim saved</span>
            </div>

            A draft of your claim has been saved. To sign, submit and pay for your claim:

            <ol class="govuk-list govuk-list--number">
            <li>Resume your claim.</li>
            <li>Click through the questions.</li>
            <li>Choose the ‘Submit and pay for my claim now’ option when asked
                how you’d like to complete your claim.</li>
            <li>Select the ‘Pay the claim fee’ link on the confirmation screen.</li>
            </ol>
            """;
    }

}
