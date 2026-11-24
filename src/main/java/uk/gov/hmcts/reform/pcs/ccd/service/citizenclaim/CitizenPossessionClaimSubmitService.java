package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherNoGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimDefendant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNotice;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimOtherInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentArrears;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentPeriodEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimantKind;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.CitizenClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.RentHistoryPeriodEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeServiceMethodEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.CitizenClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.RentHistoryPeriodRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseFlagService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.CLAIM;

/**
 * Maps a citizen private-landlord claim onto the claim domain the ExUI claim uses, and raises the
 * claim-issue fee service request.
 */
@Slf4j
@Service
public class CitizenPossessionClaimSubmitService {

    private final PcsCaseService pcsCaseService;
    private final PcsCaseRepository pcsCaseRepository;
    private final ClaimRepository claimRepository;
    private final PartyRepository partyRepository;
    private final PartyService partyService;
    private final CaseFlagService caseFlagService;
    private final DocumentService documentService;
    private final AddressMapper addressMapper;
    private final RentHistoryPeriodRepository periodRepository;
    private final CitizenClaimSubmissionRepository submissionRepository;
    private final FeeService feeService;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;

    public CitizenPossessionClaimSubmitService(PcsCaseService pcsCaseService,
                                               PcsCaseRepository pcsCaseRepository,
                                               ClaimRepository claimRepository,
                                               PartyRepository partyRepository,
                                               PartyService partyService,
                                               CaseFlagService caseFlagService,
                                               DocumentService documentService,
                                               AddressMapper addressMapper,
                                               RentHistoryPeriodRepository periodRepository,
                                               CitizenClaimSubmissionRepository submissionRepository,
                                               FeeService feeService,
                                               PaymentService paymentService,
                                               @Qualifier("draftCaseDataObjectMapper") ObjectMapper objectMapper,
                                               Clock utcClock) {
        this.pcsCaseService = pcsCaseService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.claimRepository = claimRepository;
        this.partyRepository = partyRepository;
        this.partyService = partyService;
        this.caseFlagService = caseFlagService;
        this.documentService = documentService;
        this.addressMapper = addressMapper;
        this.periodRepository = periodRepository;
        this.submissionRepository = submissionRepository;
        this.feeService = feeService;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
        this.utcClock = utcClock;
    }

    public record SubmittedClaim(String serviceRequestReference, FeeDetails feeDetails) {
    }

    /**
     * Writes and flushes the claim, then raises its fee service request, inside the submit
     * transaction, so a Payments failure rolls the claim back with the rest of the event and a
     * database failure surfaces before Payments is called. Only the answers that apply to the claim
     * are mapped (no arrears details without a rent arrears ground, no notice details unless notice
     * was served); the submitted answers are kept in full.
     *
     * @throws CitizenClaimPaymentUnavailableException if the Fees Register or Payments call failed
     */
    public SubmittedClaim submit(PcsCaseEntity pcsCaseEntity, CitizenPossessionClaim answers,
                                 CitizenPossessionClaim applicable) {
        final long caseReference = pcsCaseEntity.getCaseReference();
        final CitizenPossessionClaim claim = answers.toBuilder()
            .propertyAddress(addressMapper.toAddressUK(pcsCaseEntity.getPropertyAddress()))
            .draftVersion(null)
            .confirmedSections(null)
            .build();
        final String submittedAnswers = writeSubmittedAnswers(claim);
        final boolean wales = pcsCaseEntity.getLegislativeCountry() == LegislativeCountry.WALES;

        ClaimEntity newClaim = buildClaimEntity(applicable);
        newClaim.addClaimGrounds(buildClaimGrounds(applicable));
        newClaim.setRentArrears(buildRentArrears(applicable));
        newClaim.setNoticeOfPossession(buildNotice(applicable.getNotice()));
        newClaim.setStatementOfTruth(buildStatementOfTruth(applicable));
        pcsCaseEntity.addClaim(newClaim);
        ClaimEntity claimEntity = claimRepository.save(newClaim);

        List<DocumentEntity> documents = documentService.createAllDocuments(toDocumentCase(applicable, wales));
        documents.forEach(document -> document.setClaim(claimEntity));
        pcsCaseEntity.addDocuments(documents);
        claimEntity.addClaimDocuments(documents);

        PartyEntity claimant = populateClaimant(pcsCaseEntity, applicable);
        claimEntity.addParty(claimant, PartyRole.CLAIMANT);
        for (PartyEntity additionalClaimant : buildAdditionalClaimants(applicable)) {
            pcsCaseEntity.addParty(additionalClaimant);
            claimEntity.addParty(additionalClaimant, PartyRole.CLAIMANT);
        }
        for (PartyEntity defendant : buildDefendants(applicable)) {
            pcsCaseEntity.addParty(defendant);
            claimEntity.addParty(defendant, PartyRole.DEFENDANT);
        }

        pcsCaseEntity.setTenancyLicence(buildTenancyLicence(applicable, wales));
        allocateLocation(pcsCaseEntity);
        pcsCaseRepository.save(pcsCaseEntity);

        saveRentHistory(claimEntity, applicable.getRentArrears());
        submissionRepository.save(CitizenClaimSubmissionEntity.builder()
                                      .claim(claimEntity)
                                      .submittedClaim(submittedAnswers)
                                      .build());
        pcsCaseRepository.flush();

        return createClaimServiceRequest(caseReference, claimant);
    }

    private SubmittedClaim createClaimServiceRequest(long caseReference, PartyEntity claimant) {
        FeeDetails feeDetails;
        try {
            feeDetails = feeService.getFee(FeeType.CASE_ISSUE_FEE);
        } catch (FeeNotFoundException e) {
            throw new CitizenClaimPaymentUnavailableException(caseReference, e);
        }
        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .feeDetails(feeDetails)
            .ccdCaseNumber(String.valueOf(caseReference))
            .caseReference(caseReference)
            .responsiblePartyId(claimant.getId())
            .responsiblePartyName(partyService.getPartyName(claimant))
            .paymentCallbackHandlerType(CLAIM)
            .build();
        try {
            // Payments has no lookup by case, so if the submit rolls back after this call succeeds the
            // service request it created is orphaned in Payments, and a retry raises a second one.
            PaymentServiceResponse serviceRequest = paymentService.createServiceRequest(taskData);
            return new SubmittedClaim(serviceRequest.getServiceRequestReference(), feeDetails);
        } catch (FeignException e) {
            throw new CitizenClaimPaymentUnavailableException(caseReference, e);
        }
    }

    private static ClaimEntity buildClaimEntity(CitizenPossessionClaim claim) {
        CitizenClaimCircumstances circumstances = claim.getCircumstances();
        CitizenClaimOtherInformation otherInformation = claim.getOtherInformation();
        boolean rentArrearsClaim = CitizenPossessionClaimApplicableAnswers.hasRentArrearsGround(claim);

        ClaimEntity.ClaimEntityBuilder builder = ClaimEntity.builder()
            .claimantType(ClaimantType.PRIVATE_LANDLORD)
            .againstTrespassers(VerticalYesNo.NO)
            .dueToRentArrears(YesOrNo.from(rentArrearsClaim))
            .additionalDefendants(claim.getAdditionalDefendants())
            // The citizen journey no longer asks it, but the claim form always shows the question
            .underlesseeOrMortgagee(VerticalYesNo.NO);

        if (circumstances != null) {
            builder
                .claimantCircumstancesProvided(providedIfText(circumstances.getClaimantCircumstances()))
                .claimantCircumstances(blankToNull(circumstances.getClaimantCircumstances()))
                .defendantCircumstancesProvided(providedIfText(circumstances.getDefendantCircumstances()))
                .defendantCircumstances(blankToNull(circumstances.getDefendantCircumstances()));
        }

        if (otherInformation != null) {
            builder
                .additionalReasonsProvided(otherInformation.getHasOtherReasons())
                .additionalReasons(otherInformation.getHasOtherReasons() == VerticalYesNo.YES
                                       ? otherInformation.getOtherReasonsDetail() : null)
                .additionalDocsProvided(VerticalYesNo.from(hasItems(otherInformation.getOtherDocuments())))
                .genAppExpected(otherInformation.getGeneralApplicationIntent())
                .languageUsed(otherInformation.getLanguageUsed());
        }

        return builder.build();
    }

    private static List<ClaimGroundEntity> buildClaimGrounds(CitizenPossessionClaim claim) {
        CitizenClaimGrounds grounds = claim.getGrounds();
        if (CitizenPossessionClaimApplicableAnswers.isExcludedTenancy(claim)) {
            return buildExcludedClaimGrounds(grounds);
        }
        Map<PrivateLandlordAssuredGround, String> reasons = CitizenPossessionClaimValidator.groundReasons(grounds);
        String otherGroundsDetails = grounds != null ? blankToNull(grounds.getOtherGroundsDetails()) : null;

        return CitizenPossessionClaimValidator.allGrounds(grounds).stream()
            .sorted(Comparator.comparingInt(PrivateLandlordAssuredGround::getRank))
            .map(ground -> ClaimGroundEntity.builder()
                .category(ground.getCategory())
                .code(ground.name())
                .reason(reasons.get(ground))
                .description(ground == PrivateLandlordAssuredGround.OTHER ? otherGroundsDetails : null)
                .isRentArrears(ground.isRentArrears())
                .build())
            .toList();
    }

    /**
     * The grounds for an excluded tenancy, as legal-rep claims for introductory, demoted and other tenancies hold
     * them. The one reason the claimant gives is kept on each ground that takes a reason, or on rent arrears when
     * that is the only ground.
     */
    private static List<ClaimGroundEntity> buildExcludedClaimGrounds(CitizenClaimGrounds grounds) {
        String reason = blankToNull(grounds.getExcludedReason());
        if (grounds.getHasExcludedGrounds() != VerticalYesNo.YES) {
            return List.of(ClaimGroundEntity.builder()
                               .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS)
                               .code(IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS.name())
                               .reason(reason)
                               .isRentArrears(false)
                               .build());
        }
        Set<IntroductoryDemotedOrOtherGrounds> chosen = grounds.getExcludedGrounds();
        boolean onlyRentArrears = chosen.size() == 1 && chosen.contains(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS);
        return chosen.stream()
            .sorted(Comparator.comparingInt(IntroductoryDemotedOrOtherGrounds::getRank))
            .map(ground -> ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                .code(ground.name())
                .reason(ground != IntroductoryDemotedOrOtherGrounds.RENT_ARREARS || onlyRentArrears ? reason : null)
                .description(ground == IntroductoryDemotedOrOtherGrounds.OTHER
                                 ? blankToNull(grounds.getExcludedOtherGroundDetails()) : null)
                .isRentArrears(ground == IntroductoryDemotedOrOtherGrounds.RENT_ARREARS)
                .build())
            .toList();
    }

    private static RentArrearsEntity buildRentArrears(CitizenPossessionClaim claim) {
        CitizenClaimRentArrears rentArrears = claim.getRentArrears();
        if (rentArrears == null || rentArrears.getCurrentArrears() == null) {
            return null;
        }
        boolean recoveryAttempted = StringUtils.isNotBlank(rentArrears.getRecoverySteps());
        return RentArrearsEntity.builder()
            .totalRentArrears(rentArrears.getCurrentArrears())
            .totalRentArrearsDate(rentArrears.getCurrentArrearsDate())
            .arrearsAtNoticeDate(rentArrears.getArrearsAtNoticeDate())
            .proofType(rentArrears.getProofType())
            .arrearsJudgmentWanted(rentArrears.getMoneyJudgmentWanted())
            .recoveryAttempted(VerticalYesNo.from(recoveryAttempted))
            .recoveryAttemptDetails(recoveryAttempted ? rentArrears.getRecoverySteps() : null)
            .build();
    }

    private static NoticeOfPossessionEntity buildNotice(CitizenClaimNotice notice) {
        if (notice == null || notice.getNoticeServed() == null) {
            return null;
        }
        NoticeOfPossessionEntity entity = new NoticeOfPossessionEntity();
        boolean served = notice.getNoticeServed() == VerticalYesNo.YES;
        entity.setNoticeServed(YesOrNo.from(served));
        if (!served) {
            entity.setNoticeStatement(blankToNull(notice.getNoNoticeReason()));
            return entity;
        }

        entity.setNoticeType(noticeTypeDescription(notice));
        entity.setIsAbleToUploadDocument(
            notice.getNoticeAvailable() != null ? YesOrNo.from(notice.getNoticeAvailable().toBoolean()) : null);
        if (notice.getNoticeAvailable() == VerticalYesNo.NO) {
            entity.setUnableToUploadReason(notice.getNoticeUnavailableReason());
        }

        List<CitizenClaimNoticeMethod> methods = noticeMethodsInOrder(notice);
        if (methods.isEmpty()) {
            return entity;
        }

        CitizenClaimNoticeMethod primary = methods.getFirst();
        entity.setServingMethod(primary.getNoticeServiceMethod());
        // The served date is the one given under the primary method; each method keeps its own below
        entity.setNoticeDate(noticeMethodDate(notice, primary));
        entity.setNoticeDetails(noticeMethodDetails(notice, primary));

        for (int i = 0; i < methods.size(); i++) {
            CitizenClaimNoticeMethod method = methods.get(i);
            entity.addServiceMethod(NoticeServiceMethodEntity.builder()
                                        .method(method.getNoticeServiceMethod())
                                        .servedDate(noticeMethodDate(notice, method))
                                        .details(blankToNull(noticeMethodDetails(notice, method)))
                                        .displayOrder(i + 1)
                                        .build());
        }

        return entity;
    }

    private static LocalDate noticeMethodDate(CitizenClaimNotice notice, CitizenClaimNoticeMethod method) {
        return switch (method) {
            case FIRST_CLASS_POST -> notice.getPostedDate();
            case PERMITTED_PLACE -> notice.getDeliveredDate();
            case PERSONALLY_HANDED -> notice.getHandedDate();
            case EMAIL -> notice.getEmailedDate();
            case OTHER_ELECTRONIC -> notice.getOtherElectronicDate();
            case OTHER -> notice.getOtherMethodDate();
        };
    }

    private static String noticeMethodDetails(CitizenClaimNotice notice, CitizenClaimNoticeMethod method) {
        return switch (method) {
            case FIRST_CLASS_POST, PERMITTED_PLACE -> null;
            case PERSONALLY_HANDED -> notice.getHandedToName();
            case EMAIL -> notice.getEmailedTo();
            case OTHER_ELECTRONIC -> notice.getOtherElectronicDetails();
            case OTHER -> notice.getOtherMethodDetails();
        };
    }

    private static String noticeTypeDescription(CitizenClaimNotice notice) {
        CitizenClaimNoticeType noticeType = notice.getNoticeType();
        if (noticeType == null) {
            return null;
        }
        if (noticeType == CitizenClaimNoticeType.OTHER && StringUtils.isNotBlank(notice.getNoticeTypeOther())) {
            return notice.getNoticeTypeOther();
        }
        return noticeType.getLabel();
    }

    // In the order the frontend offers them; the first is the primary method.
    private static List<CitizenClaimNoticeMethod> noticeMethodsInOrder(CitizenClaimNotice notice) {
        Set<CitizenClaimNoticeMethod> methods = notice.getNoticeMethods();
        if (methods == null) {
            return List.of();
        }
        return methods.stream().filter(Objects::nonNull).distinct().sorted().toList();
    }

    private StatementOfTruthEntity buildStatementOfTruth(CitizenPossessionClaim claim) {
        CitizenClaimSubmission submission = claim.getSubmission();
        return StatementOfTruthEntity.builder()
            .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
            .accepted(YesOrNo.YES)
            .fullName(submission.getStatementOfTruthFullName())
            .positionHeld(blankToNull(submission.getStatementOfTruthPosition()))
            .completedDate(LocalDateTime.now(utcClock))
            .build();
    }

    private static PCSCase toDocumentCase(CitizenPossessionClaim claim, boolean wales) {
        CitizenClaimTenancy tenancy = claim.getTenancy();
        CitizenClaimNotice notice = claim.getNotice();
        CitizenClaimRentArrears rentArrears = claim.getRentArrears();
        final CitizenClaimOtherInformation otherInformation = claim.getOtherInformation();

        PCSCase.PCSCaseBuilder builder = PCSCase.builder();
        if (tenancy != null && hasItems(tenancy.getTenancyAgreementDocuments()) && wales) {
            builder.occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                                      .licenceDocuments(tenancy.getTenancyAgreementDocuments())
                                                      .build());
        } else if (tenancy != null && hasItems(tenancy.getTenancyAgreementDocuments())) {
            builder.tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                              .tenancyLicenceDocuments(tenancy.getTenancyAgreementDocuments())
                                              .build());
        }
        if (notice != null && hasItems(notice.getNoticeDocuments())) {
            builder.noticeServedDetails(NoticeServedDetails.builder()
                                            .documents(notice.getNoticeDocuments())
                                            .build());
        }
        if (rentArrears != null && hasItems(rentArrears.getRentStatementDocuments())) {
            builder.rentArrears(RentArrearsSection.builder()
                                    .statementDocuments(rentArrears.getRentStatementDocuments())
                                    .build());
        }
        if (otherInformation != null && hasItems(otherInformation.getOtherDocuments())) {
            builder.additionalDocuments(otherInformation.getOtherDocuments().stream()
                                            .filter(item -> item != null && item.getValue() != null)
                                            .map(item -> toAdditionalDocument(item.getValue()))
                                            .toList());
        }
        return builder.build();
    }

    private static ListValue<AdditionalDocument> toAdditionalDocument(Document document) {
        DynamicListElement otherType = DynamicListElement.builder()
            .code(UUID.randomUUID())
            .label(AdditionalDocumentType.OTHER.getLabel())
            .build();
        return ListValue.<AdditionalDocument>builder()
            .id(UUID.randomUUID().toString())
            .value(AdditionalDocument.builder()
                       .documentType(DynamicList.builder().value(otherType).listItems(List.of(otherType)).build())
                       .document(document)
                       .build())
            .build();
    }

    private PartyEntity populateClaimant(PcsCaseEntity pcsCaseEntity, CitizenPossessionClaim claim) {
        CitizenClaimClaimant claimant = claim.getClaimant();
        PartyEntity party = pcsCaseEntity.getParties().stream()
            .filter(PartyEntity::isClaimCreator)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No claimant party on case " + pcsCaseEntity.getCaseReference()));

        party.setRemoved(false);
        applyClaimantDetails(party, new ClaimantDetails(
            claimant.getIndividualOrOrganisation(), claimant.getFirstName(), claimant.getLastName(),
            claimant.getOrganisationName(), claimant.getAddress(), claimant.getEmailAddress(),
            claimant.getContactByPhone(), claimant.getPhoneNumber(), claimant.getContactByText(),
            claimant.getTextMessageNumber()));

        CitizenClaimOtherInformation otherInformation = claim.getOtherInformation();
        if (otherInformation != null && StringUtils.isNotBlank(otherInformation.getPcqId())) {
            party.setPcqId(otherInformation.getPcqId());
        }

        PartyEntity saved = partyRepository.save(party);
        if (otherInformation != null) {
            caseFlagService.saveReasonableAdjustmentFlags(
                saved, otherInformation.getSupportFlags(), pcsCaseEntity.getCaseReference());
        }
        return saved;
    }

    /**
     * Claimants claiming alongside the claimant, as parties with the same claimant role legal-rep claims give
     * the claimants they add.
     */
    private List<PartyEntity> buildAdditionalClaimants(CitizenPossessionClaim claim) {
        List<PartyEntity> parties = values(claim.getAdditionalClaimants()).stream()
            .map(additional -> {
                PartyEntity party = new PartyEntity();
                party.setRemoved(false);
                applyClaimantDetails(party, new ClaimantDetails(
                    additional.getIndividualOrOrganisation(), additional.getFirstName(), additional.getLastName(),
                    additional.getOrganisationName(), additional.getAddress(), additional.getEmailAddress(),
                    additional.getContactByPhone(), additional.getPhoneNumber(), additional.getContactByText(),
                    additional.getTextMessageNumber()));
                return party;
            })
            .toList();
        return partyRepository.saveAll(parties);
    }

    private record ClaimantDetails(CitizenClaimantKind kind, String firstName, String lastName,
                                   String organisationName, AddressUK address, String emailAddress,
                                   VerticalYesNo contactByPhone, String phoneNumber,
                                   VerticalYesNo contactByText, String textMessageNumber) {

        String displayName() {
            if (kind == CitizenClaimantKind.ORGANISATION) {
                return organisationName;
            }
            return Stream.of(firstName, lastName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));
        }
    }

    private void applyClaimantDetails(PartyEntity party, ClaimantDetails details) {
        party.setNameKnown(VerticalYesNo.YES);
        party.setNameOverridden(YesOrNo.NO);
        // Claimant name readers (case list, claimant tab, the defendant's journey) use orgName, as ExUI sets it
        party.setOrgName(details.displayName());
        if (details.kind() != CitizenClaimantKind.ORGANISATION) {
            party.setFirstName(details.firstName());
            party.setLastName(details.lastName());
        }
        party.setAddressKnown(VerticalYesNo.YES);
        party.setAddress(partyService.mapAddress(details.address()));
        party.setEmailAddress(blankToNull(details.emailAddress()));

        boolean phoneProvided = details.contactByPhone() == VerticalYesNo.YES
            && StringUtils.isNotBlank(details.phoneNumber());
        party.setPhoneNumberProvided(VerticalYesNo.from(phoneProvided));
        party.setPhoneNumber(phoneProvided ? details.phoneNumber() : null);
        if (details.contactByText() == VerticalYesNo.YES) {
            party.setTextMessageNumber(CitizenPossessionClaimApplicableAnswers.effectiveTextNumber(
                details.textMessageNumber(), details.contactByPhone(), details.phoneNumber()));
        }

        ContactPreferencesEntity preferences = Optional.ofNullable(party.getContactPreferences())
            .orElseGet(ContactPreferencesEntity::new);
        preferences.setContactByEmail(VerticalYesNo.from(StringUtils.isNotBlank(details.emailAddress())));
        preferences.setContactByPhone(details.contactByPhone());
        preferences.setContactByText(details.contactByText());
        preferences.setContactByPost(VerticalYesNo.YES);
        party.setContactPreferences(preferences);
    }


    private List<PartyEntity> buildDefendants(CitizenPossessionClaim claim) {
        List<PartyEntity> defendants = claim.getDefendants().stream()
            .filter(Objects::nonNull)
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(CitizenPossessionClaimSubmitService::toDefendantDetails)
            .map(partyService::createDefendant)
            .toList();
        return partyRepository.saveAll(defendants);
    }

    private static DefendantDetails toDefendantDetails(CitizenClaimDefendant defendant) {
        return DefendantDetails.builder()
            .nameKnown(defendant.getNameKnown())
            .firstName(defendant.getFirstName())
            .lastName(defendant.getLastName())
            .addressKnown(defendant.getAddressKnown())
            .addressSameAsPossession(defendant.getAddressSameAsProperty())
            .correspondenceAddress(defendant.getAddress())
            .build();
    }

    private static TenancyLicenceEntity buildTenancyLicence(CitizenPossessionClaim claim, boolean wales) {
        CitizenClaimTenancy tenancy = claim.getTenancy();

        TenancyLicenceEntity entity = new TenancyLicenceEntity();
        if (wales) {
            OccupationLicenceTypeWales type = tenancy.getOccupationContractType();
            entity.setType(type.getCombinedLicenceType());
            if (type == OccupationLicenceTypeWales.OTHER) {
                entity.setOtherTypeDetails(tenancy.getOtherOccupationContractDetails());
            }
        } else {
            CitizenClaimTenancyType type = tenancy.getTenancyType();
            entity.setType(type.getCombinedLicenceType());
            if (type == CitizenClaimTenancyType.OTHER) {
                entity.setOtherTypeDetails(tenancy.getOtherTenancyTypeDetails());
            } else if (type != CitizenClaimTenancyType.ASSURED) {
                entity.setOtherTypeDetails(type.getLabel());
            }
        }
        entity.setStartDate(tenancy.getTenancyStartDate());
        entity.setHasCopyOfTenancyLicence(tenancy.getTenancyAgreementAvailable());
        if (tenancy.getTenancyAgreementAvailable() == VerticalYesNo.NO) {
            entity.setReasonsForNoTenancyLicence(tenancy.getNoTenancyAgreementReason());
        }

        CitizenClaimRentArrears rentArrears = claim.getRentArrears();
        if (rentArrears != null && rentArrears.getRentFrequency() != null) {
            entity.setRentAmount(rentArrears.getRentAmount());
            entity.setRentFrequency(rentArrears.getRentFrequency());
            entity.setRentDueDay(rentArrears.getRentDueDay());
            if (rentArrears.getRentFrequency() == RentPaymentFrequency.OTHER) {
                entity.setOtherRentFrequency(rentArrears.getOtherFrequencyDetails());
            }
        }
        return entity;
    }

    private void allocateLocation(PcsCaseEntity pcsCaseEntity) {
        PCSCase locationCase = PCSCase.builder()
            .propertyAddress(addressMapper.toAddressUK(pcsCaseEntity.getPropertyAddress()))
            .legislativeCountry(pcsCaseEntity.getLegislativeCountry())
            .build();
        pcsCaseService.allocateRegionId(locationCase);
        pcsCaseEntity.setRegionId(locationCase.getRegionId());
        pcsCaseEntity.setBaseLocation(locationCase.getCaseManagementLocationNumber());
        pcsCaseEntity.setCaseManagementLocation(locationCase.getCaseManagementLocationNumber());
    }

    private void saveRentHistory(ClaimEntity claimEntity, CitizenClaimRentArrears rentArrears) {
        if (rentArrears == null) {
            return;
        }
        List<CitizenClaimRentPeriodEntry> table = values(rentArrears.getRentHistoryTable());
        List<RentHistoryPeriodEntity> periodEntities = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            CitizenClaimRentPeriodEntry entry = table.get(i);
            periodEntities.add(RentHistoryPeriodEntity.builder()
                                   .claim(claimEntity)
                                   .rank(i + 1)
                                   .periodStart(entry.getPeriodStart())
                                   .dateDue(entry.getDateDue())
                                   .amountDue(entry.getAmountDue())
                                   .datePaid(entry.getDatePaid())
                                   .amountPaid(entry.getAmountPaid())
                                   .build());
        }
        if (!periodEntities.isEmpty()) {
            periodRepository.saveAll(periodEntities);
        }
    }

    private String writeSubmittedAnswers(CitizenPossessionClaim claim) {
        try {
            return objectMapper.writeValueAsString(claim);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise the submitted citizen claim", e);
        }
    }


    private static <T> List<T> values(List<ListValue<T>> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
            .filter(Objects::nonNull)
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .toList();
    }

    private static boolean hasItems(Collection<?> items) {
        return items != null && !items.isEmpty();
    }

    private static VerticalYesNo providedIfText(String text) {
        return VerticalYesNo.from(StringUtils.isNotBlank(text));
    }

    private static String blankToNull(String text) {
        return StringUtils.isBlank(text) ? null : text;
    }

}
