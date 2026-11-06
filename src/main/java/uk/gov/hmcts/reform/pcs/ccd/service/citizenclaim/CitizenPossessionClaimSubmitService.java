package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimDefendant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNotice;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimOtherInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentArrears;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentLedgerEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentPeriodEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentTransactionType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimantKind;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.CitizenClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.RentHistoryLedgerEntryEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.RentHistoryPeriodEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.CitizenClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.RentHistoryLedgerEntryRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.RentHistoryPeriodRepository;
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

import java.time.Clock;
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
    private final DocumentService documentService;
    private final AddressMapper addressMapper;
    private final RentHistoryLedgerEntryRepository ledgerEntryRepository;
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
                                               DocumentService documentService,
                                               AddressMapper addressMapper,
                                               RentHistoryLedgerEntryRepository ledgerEntryRepository,
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
        this.documentService = documentService;
        this.addressMapper = addressMapper;
        this.ledgerEntryRepository = ledgerEntryRepository;
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
            .build();
        final String submittedAnswers = writeSubmittedAnswers(claim);

        ClaimEntity newClaim = buildClaimEntity(applicable);
        newClaim.addClaimGrounds(buildClaimGrounds(applicable.getGrounds()));
        newClaim.setRentArrears(buildRentArrears(applicable));
        newClaim.setNoticeOfPossession(buildNotice(applicable.getNotice()));
        newClaim.setStatementOfTruth(buildStatementOfTruth(applicable));
        pcsCaseEntity.addClaim(newClaim);
        ClaimEntity claimEntity = claimRepository.save(newClaim);

        List<DocumentEntity> documents = documentService.createAllDocuments(toDocumentCase(applicable));
        documents.forEach(document -> document.setClaim(claimEntity));
        pcsCaseEntity.addDocuments(documents);
        claimEntity.addClaimDocuments(documents);

        PartyEntity claimant = populateClaimant(pcsCaseEntity, applicable.getClaimant());
        claimEntity.addParty(claimant, PartyRole.CLAIMANT);
        for (PartyEntity defendant : buildDefendants(applicable)) {
            pcsCaseEntity.addParty(defendant);
            claimEntity.addParty(defendant, PartyRole.DEFENDANT);
        }
        buildUnderlessee(applicable.getCircumstances()).ifPresent(underlessee -> {
            pcsCaseEntity.addParty(underlessee);
            claimEntity.addParty(underlessee, PartyRole.UNDERLESSEE_OR_MORTGAGEE);
        });

        pcsCaseEntity.setTenancyLicence(buildTenancyLicence(applicable));
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
            .additionalDefendants(claim.getAdditionalDefendants());

        if (circumstances != null) {
            builder
                .claimantCircumstancesProvided(providedIfText(circumstances.getClaimantCircumstances()))
                .claimantCircumstances(blankToNull(circumstances.getClaimantCircumstances()))
                .defendantCircumstancesProvided(providedIfText(circumstances.getDefendantCircumstances()))
                .defendantCircumstances(blankToNull(circumstances.getDefendantCircumstances()))
                .underlesseeOrMortgagee(circumstances.getUnderlesseeExists())
                .additionalUnderlesseesOrMortgagees(
                    circumstances.getUnderlesseeExists() != null ? VerticalYesNo.NO : null);
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

    private static List<ClaimGroundEntity> buildClaimGrounds(CitizenClaimGrounds grounds) {
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

    private static RentArrearsEntity buildRentArrears(CitizenPossessionClaim claim) {
        CitizenClaimRentArrears rentArrears = claim.getRentArrears();
        if (rentArrears == null || rentArrears.getCurrentArrears() == null) {
            return null;
        }
        boolean recoveryAttempted = StringUtils.isNotBlank(rentArrears.getRecoverySteps());
        return RentArrearsEntity.builder()
            .totalRentArrears(rentArrears.getCurrentArrears())
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
            return entity;
        }

        entity.setNoticeType(noticeTypeDescription(notice));
        entity.setIsAbleToUploadDocument(
            notice.getNoticeAvailable() != null ? YesOrNo.from(notice.getNoticeAvailable().toBoolean()) : null);
        if (notice.getNoticeAvailable() == VerticalYesNo.NO) {
            entity.setUnableToUploadReason(notice.getNoticeUnavailableReason());
        }

        primaryNoticeMethod(notice).ifPresentOrElse(
            method -> {
                entity.setServingMethod(method.getNoticeServiceMethod());
                switch (method) {
                    case FIRST_CLASS_POST -> entity.setNoticeDate(notice.getPostedDate());
                    case PERMITTED_PLACE -> entity.setNoticeDate(notice.getDeliveredDate());
                    case PERSONALLY_HANDED -> {
                        entity.setNoticeDate(notice.getHandedDate());
                        entity.setNoticeDetails(notice.getHandedToName());
                    }
                    case EMAIL -> {
                        entity.setNoticeDate(notice.getEmailedDate());
                        entity.setNoticeDetails(notice.getEmailedTo());
                    }
                    case OTHER_ELECTRONIC -> {
                        entity.setNoticeDate(notice.getOtherElectronicDate());
                        entity.setNoticeDetails(notice.getOtherElectronicDetails());
                    }
                    case OTHER -> {
                        entity.setNoticeDate(notice.getOtherMethodDate());
                        entity.setNoticeDetails(notice.getOtherMethodDetails());
                    }
                }
                if (entity.getNoticeDate() == null) {
                    entity.setNoticeDate(notice.getNoticeDate());
                }
            },
            () -> entity.setNoticeDate(notice.getNoticeDate())
        );

        return entity;
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

    private static Optional<CitizenClaimNoticeMethod> primaryNoticeMethod(CitizenClaimNotice notice) {
        Set<CitizenClaimNoticeMethod> methods = notice.getNoticeMethods();
        if (methods == null) {
            return Optional.empty();
        }
        return methods.stream().filter(Objects::nonNull).min(Comparator.naturalOrder());
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

    static String claimantDisplayName(CitizenClaimClaimant claimant) {
        if (claimant.getIndividualOrOrganisation() == CitizenClaimantKind.ORGANISATION) {
            return claimant.getOrganisationName();
        }
        return Stream.of(claimant.getFirstName(), claimant.getLastName())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" "));
    }

    private static PCSCase toDocumentCase(CitizenPossessionClaim claim) {
        CitizenClaimTenancy tenancy = claim.getTenancy();
        CitizenClaimNotice notice = claim.getNotice();
        CitizenClaimRentArrears rentArrears = claim.getRentArrears();
        final CitizenClaimOtherInformation otherInformation = claim.getOtherInformation();

        PCSCase.PCSCaseBuilder builder = PCSCase.builder();
        if (tenancy != null && hasItems(tenancy.getTenancyAgreementDocuments())) {
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

    private PartyEntity populateClaimant(PcsCaseEntity pcsCaseEntity, CitizenClaimClaimant claimant) {
        PartyEntity party = pcsCaseEntity.getParties().stream()
            .filter(PartyEntity::isClaimCreator)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No claimant party on case " + pcsCaseEntity.getCaseReference()));

        party.setRemoved(false);
        party.setNameKnown(VerticalYesNo.YES);
        party.setNameOverridden(YesOrNo.NO);
        // Claimant name readers (case list, claimant tab, the defendant's journey) use orgName, as ExUI sets it
        party.setOrgName(claimantDisplayName(claimant));
        if (claimant.getIndividualOrOrganisation() != CitizenClaimantKind.ORGANISATION) {
            party.setFirstName(claimant.getFirstName());
            party.setLastName(claimant.getLastName());
        }
        party.setAddressKnown(VerticalYesNo.YES);
        party.setAddress(partyService.mapAddress(claimant.getAddress()));
        party.setEmailAddress(blankToNull(claimant.getEmailAddress()));

        boolean phoneProvided = claimant.getContactByPhone() == VerticalYesNo.YES
            && StringUtils.isNotBlank(claimant.getPhoneNumber());
        party.setPhoneNumberProvided(VerticalYesNo.from(phoneProvided));
        party.setPhoneNumber(phoneProvided ? claimant.getPhoneNumber() : null);
        if (claimant.getContactByText() == VerticalYesNo.YES) {
            party.setTextMessageNumber(CitizenPossessionClaimApplicableAnswers.effectiveTextNumber(claimant));
        }

        ContactPreferencesEntity preferences = Optional.ofNullable(party.getContactPreferences())
            .orElseGet(ContactPreferencesEntity::new);
        preferences.setContactByEmail(VerticalYesNo.from(StringUtils.isNotBlank(claimant.getEmailAddress())));
        preferences.setContactByPhone(claimant.getContactByPhone());
        preferences.setContactByText(claimant.getContactByText());
        preferences.setContactByPost(VerticalYesNo.YES);
        party.setContactPreferences(preferences);

        return partyRepository.save(party);
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

    private Optional<PartyEntity> buildUnderlessee(CitizenClaimCircumstances circumstances) {
        if (circumstances == null || circumstances.getUnderlesseeExists() != VerticalYesNo.YES) {
            return Optional.empty();
        }
        boolean nameKnown = StringUtils.isNotBlank(circumstances.getUnderlesseeName());
        boolean addressKnown = circumstances.getUnderlesseeAddress() != null;
        UnderlesseeMortgageeDetails details = UnderlesseeMortgageeDetails.builder()
            .nameKnown(VerticalYesNo.from(nameKnown))
            .name(nameKnown ? circumstances.getUnderlesseeName() : null)
            .addressKnown(VerticalYesNo.from(addressKnown))
            .address(circumstances.getUnderlesseeAddress())
            .build();
        return Optional.of(partyRepository.save(partyService.createUnderlesseeOrMortgagee(details)));
    }

    private static TenancyLicenceEntity buildTenancyLicence(CitizenPossessionClaim claim) {
        CitizenClaimTenancy tenancy = claim.getTenancy();
        CitizenClaimTenancyType type = tenancy.getTenancyType();

        TenancyLicenceEntity entity = new TenancyLicenceEntity();
        entity.setType(type.getCombinedLicenceType());
        if (type == CitizenClaimTenancyType.OTHER) {
            entity.setOtherTypeDetails(tenancy.getOtherTenancyTypeDetails());
        } else if (type != CitizenClaimTenancyType.ASSURED) {
            entity.setOtherTypeDetails(type.getLabel());
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
        List<CitizenClaimRentLedgerEntry> ledger = values(rentArrears.getRentHistoryLedger());
        List<RentHistoryLedgerEntryEntity> ledgerEntities = new ArrayList<>();
        for (int i = 0; i < ledger.size(); i++) {
            CitizenClaimRentLedgerEntry entry = ledger.get(i);
            Set<CitizenClaimRentTransactionType> types = Optional.ofNullable(entry.getTransactionTypes())
                .orElse(Set.of());
            ledgerEntities.add(RentHistoryLedgerEntryEntity.builder()
                                   .claim(claimEntity)
                                   .rank(i + 1)
                                   .transactionDate(entry.getTransactionDate())
                                   .rentCharge(types.contains(CitizenClaimRentTransactionType.RENT_CHARGE))
                                   .tenantPayment(types.contains(CitizenClaimRentTransactionType.TENANT_PAYMENT))
                                   .description(entry.getDescription())
                                   .amountCharged(entry.getAmountCharged())
                                   .amountPaid(entry.getAmountPaid())
                                   .build());
        }
        if (!ledgerEntities.isEmpty()) {
            ledgerEntryRepository.saveAll(ledgerEntities);
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
