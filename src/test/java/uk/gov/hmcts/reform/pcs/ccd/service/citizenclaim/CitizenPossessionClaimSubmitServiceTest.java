package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaimFixtures;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.CitizenClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.RentHistoryLedgerEntryEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.RentHistoryPeriodEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.CitizenClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.RentHistoryLedgerEntryRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.RentHistoryPeriodRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenPossessionClaimSubmitServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID CLAIMANT_PARTY_ID = UUID.randomUUID();
    private static final AddressUK CASE_ADDRESS = AddressUK.builder()
        .addressLine1("1 Case Street").postTown("Bradford").postCode("BD10 0JB").build();

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private PartyService partyService;
    @Mock
    private DocumentService documentService;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private RentHistoryLedgerEntryRepository ledgerEntryRepository;
    @Mock
    private RentHistoryPeriodRepository periodRepository;
    @Mock
    private CitizenClaimSubmissionRepository submissionRepository;
    @Mock
    private FeeService feeService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private FeePaymentRepository feePaymentRepository;

    @Captor
    private ArgumentCaptor<ClaimEntity> claimCaptor;
    @Captor
    private ArgumentCaptor<List<RentHistoryLedgerEntryEntity>> ledgerCaptor;
    @Captor
    private ArgumentCaptor<List<RentHistoryPeriodEntity>> periodCaptor;
    @Captor
    private ArgumentCaptor<CitizenClaimSubmissionEntity> submissionCaptor;
    @Captor
    private ArgumentCaptor<FeesAndPayTaskData> feeTaskCaptor;
    @Captor
    private ArgumentCaptor<PCSCase> documentCaseCaptor;

    private final ObjectMapper objectMapper = CitizenPossessionClaimFixtures.draftObjectMapper();
    private final FeeDetails caseIssueFee = FeeDetails.builder().code("FEE0412").feeAmount(new BigDecimal("404.00"))
        .build();

    private PcsCaseEntity pcsCaseEntity;
    private PartyEntity claimantStub;
    private CitizenPossessionClaimSubmitService underTest;

    @BeforeEach
    void setUp() {
        pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(CASE_REFERENCE);
        pcsCaseEntity.setLegislativeCountry(LegislativeCountry.ENGLAND);
        pcsCaseEntity.setPropertyAddress(new AddressEntity());
        claimantStub = new PartyEntity();
        claimantStub.setId(CLAIMANT_PARTY_ID);
        claimantStub.setClaimCreator(true);
        claimantStub.setIdamId("citizen-1");
        pcsCaseEntity.addParty(claimantStub);

        when(claimRepository.save(any(ClaimEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partyRepository.save(any(PartyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partyRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentService.createAllDocuments(any(PCSCase.class))).thenReturn(List.of());
        lenient().when(addressMapper.toAddressEntityAndNormalise(any())).thenAnswer(invocation -> new AddressEntity());
        lenient().when(addressMapper.toAddressUK(pcsCaseEntity.getPropertyAddress())).thenReturn(CASE_ADDRESS);
        lenient().when(feeService.getFee(FeeType.CASE_ISSUE_FEE)).thenReturn(caseIssueFee);
        lenient().when(partyService.getPartyName(claimantStub)).thenReturn("Jane Landlord");
        PaymentServiceResponse serviceRequest = new PaymentServiceResponse();
        serviceRequest.setServiceRequestReference("2026-1762441234567");
        lenient().when(paymentService.createServiceRequest(any())).thenReturn(serviceRequest);

        Clock clock = Clock.fixed(Instant.parse("2026-11-06T10:00:00Z"), ZoneOffset.UTC);
        underTest = new CitizenPossessionClaimSubmitService(
            pcsCaseService, pcsCaseRepository, claimRepository, partyRepository, partyService, documentService,
            addressMapper, ledgerEntryRepository, periodRepository, submissionRepository, feeService,
            paymentService, feePaymentRepository, objectMapper, clock);
    }

    @Test
    void shouldMapTheClaimOntoTheExistingDomain() {
        underTest.submit(pcsCaseEntity, CitizenPossessionClaimFixtures.completeClaim());

        verify(claimRepository).save(claimCaptor.capture());
        ClaimEntity claim = claimCaptor.getValue();
        assertThat(claim.getClaimantType()).isEqualTo(ClaimantType.PRIVATE_LANDLORD);
        assertThat(claim.getAgainstTrespassers()).isEqualTo(VerticalYesNo.NO);
        assertThat(claim.getDueToRentArrears()).isEqualTo(YesOrNo.YES);
        assertThat(claim.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH);
        assertThat(claim.getAdditionalReasons()).isEqualTo("The tenant has refused access for repairs.");
        assertThat(claim.getDefendantCircumstances()).isEqualTo("The tenant has lost their job.");
        assertThat(claim.getUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.YES);
        assertThat(claim.getGenAppExpected()).isEqualTo(VerticalYesNo.NO);
        assertThat(claim.getPcsCase()).isSameAs(pcsCaseEntity);

        assertThat(claim.getClaimGrounds())
            .extracting(ClaimGroundEntity::getCode, ClaimGroundEntity::getCategory, ClaimGroundEntity::getIsRentArrears)
            .containsExactlyInAnyOrder(
                tuple(
                    "GROUND_1A", ClaimGroundCategory.PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
                tuple(
                    "GROUND_8", ClaimGroundCategory.PRIVATE_LANDLORD_ASSURED_MANDATORY, true),
                tuple(
                    "GROUND_10", ClaimGroundCategory.PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, true),
                tuple(
                    "GROUND_14", ClaimGroundCategory.PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, false)
            );
        assertThat(claim.getClaimGrounds())
            .filteredOn(ground -> ground.getCode().equals("GROUND_1A"))
            .singleElement()
            .extracting(ClaimGroundEntity::getReason)
            .isEqualTo("I am selling the property.");

        assertThat(claim.getRentArrears().getTotalRentArrears()).isEqualByComparingTo("5000.00");
        assertThat(claim.getRentArrears().getArrearsJudgmentWanted()).isEqualTo(VerticalYesNo.YES);
        assertThat(claim.getRentArrears().getRecoveryAttemptDetails()).isEqualTo("Sent three reminder letters.");

        NoticeOfPossessionEntity notice = claim.getNoticeOfPossession();
        assertThat(notice.getNoticeServed()).isEqualTo(YesOrNo.YES);
        assertThat(notice.getNoticeType()).isEqualTo("Notice seeking possession");
        assertThat(notice.getServingMethod()).isEqualTo(NoticeServiceMethod.FIRST_CLASS_POST);
        assertThat(notice.getNoticeDate()).isEqualTo(LocalDate.of(2026, 8, 1));

        assertThat(claim.getStatementOfTruth().getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.CLAIMANT);
        assertThat(claim.getStatementOfTruth().getAccepted()).isEqualTo(YesOrNo.YES);
        assertThat(claim.getStatementOfTruth().getFullName()).isEqualTo("Jane Landlord");
    }

    @Test
    void shouldFileTheOtherGroundUnderItsOwnCategoryWithItsDetails() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setOtherGrounds(Set.of(PrivateLandlordAssuredGround.OTHER));
        claim.getGrounds().setOtherGroundsDetails("The tenant sublet without consent.");

        underTest.submit(pcsCaseEntity, claim);

        verify(claimRepository).save(claimCaptor.capture());
        assertThat(claimCaptor.getValue().getClaimGrounds())
            .filteredOn(ground -> ground.getCode().equals("OTHER"))
            .singleElement()
            .satisfies(ground -> {
                assertThat(ground.getCategory()).isEqualTo(ClaimGroundCategory.PRIVATE_LANDLORD_ASSURED_OTHER);
                assertThat(ground.getDescription()).isEqualTo("The tenant sublet without consent.");
                assertThat(ground.getIsRentArrears()).isFalse();
            });
    }

    @Test
    void shouldRecordTheDescriptionOfAnOtherNoticeType() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getNotice().setNoticeType(CitizenClaimNoticeType.OTHER);
        claim.getNotice().setNoticeTypeOther("Section 8 notice (old form)");

        underTest.submit(pcsCaseEntity, claim);

        verify(claimRepository).save(claimCaptor.capture());
        assertThat(claimCaptor.getValue().getNoticeOfPossession().getNoticeType())
            .isEqualTo("Section 8 notice (old form)");
    }

    @Test
    void shouldReportPaymentsApiFailuresAsPaymentUnavailable() {
        FeignException paymentsDown = mock(FeignException.class);
        when(paymentService.createServiceRequest(any())).thenThrow(paymentsDown);

        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        assertThatThrownBy(() -> underTest.submit(pcsCaseEntity, claim))
            .isInstanceOf(CitizenClaimPaymentUnavailableException.class)
            .hasCause(paymentsDown);
    }

    @Test
    void shouldNotReportADatabaseFailureWhileRecordingThePaymentAsAPaymentsOutage() {
        DataIntegrityViolationException dbError = new DataIntegrityViolationException("fee_payment insert failed");
        when(paymentService.createServiceRequest(any())).thenThrow(dbError);

        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        assertThatThrownBy(() -> underTest.submit(pcsCaseEntity, claim)).isSameAs(dbError);
    }

    @Test
    void shouldFailOnAClaimWriteErrorBeforeCallingPayments() {
        DataIntegrityViolationException dbError = new DataIntegrityViolationException("claim insert failed");
        doThrow(dbError).when(pcsCaseRepository).flush();

        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        assertThatThrownBy(() -> underTest.submit(pcsCaseEntity, claim)).isSameAs(dbError);
        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldFlushTheClaimBeforeCallingPayments() {
        underTest.submit(pcsCaseEntity, CitizenPossessionClaimFixtures.completeClaim());

        InOrder inOrder = inOrder(pcsCaseRepository, paymentService);
        inOrder.verify(pcsCaseRepository).flush();
        inOrder.verify(paymentService).createServiceRequest(any());
    }

    @Test
    void shouldReuseAClaimServiceRequestTheCaseAlreadyHas() {
        when(feePaymentRepository.findLatestServiceRequest(CASE_REFERENCE, PaymentCallbackHandlerType.CLAIM))
            .thenReturn(Optional.of(FeePaymentEntity.builder()
                                        .serviceRequestReference("2026-existing")
                                        .amount(new BigDecimal("404.00"))
                                        .build()));

        CitizenPossessionClaimSubmitService.SubmittedClaim submitted =
            underTest.submit(pcsCaseEntity, CitizenPossessionClaimFixtures.completeClaim());

        assertThat(submitted.serviceRequestReference()).isEqualTo("2026-existing");
        assertThat(submitted.feeDetails().getFeeAmount()).isEqualByComparingTo("404.00");
        verifyNoInteractions(paymentService, feeService);
    }

    @Test
    void shouldPopulateTheCitizenClaimantAndAddDefendantsAndUnderlessee() {
        underTest.submit(pcsCaseEntity, CitizenPossessionClaimFixtures.completeClaim());

        assertThat(claimantStub.getFirstName()).isEqualTo("Jane");
        assertThat(claimantStub.getLastName()).isEqualTo("Landlord");
        assertThat(claimantStub.getEmailAddress()).isEqualTo("jane.landlord@example.com");
        assertThat(claimantStub.getPhoneNumber()).isEqualTo("07700900000");
        assertThat(claimantStub.getContactPreferences().getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(claimantStub.getOrganisationId()).isNull();

        verify(claimRepository).save(claimCaptor.capture());
        List<ClaimPartyEntity> claimParties = claimCaptor.getValue().getClaimParties();
        assertThat(claimParties).extracting(ClaimPartyEntity::getRole).containsExactly(
            PartyRole.CLAIMANT, PartyRole.DEFENDANT, PartyRole.DEFENDANT, PartyRole.UNDERLESSEE_OR_MORTGAGEE);

        PartyEntity firstDefendant = claimParties.get(1).getParty();
        assertThat(firstDefendant.getFirstName()).isEqualTo("Tom");
        assertThat(firstDefendant.getAddressSameAsProperty()).isEqualTo(VerticalYesNo.YES);
        PartyEntity secondDefendant = claimParties.get(2).getParty();
        assertThat(secondDefendant.getNameKnown()).isEqualTo(VerticalYesNo.NO);
        assertThat(secondDefendant.getAddress()).isNotNull();
        assertThat(claimParties.get(3).getParty().getOrgName()).isEqualTo("Sub Tenant Ltd");
    }

    @Test
    void shouldMapTheTenancyAndRent() {
        underTest.submit(pcsCaseEntity, CitizenPossessionClaimFixtures.completeClaim());

        TenancyLicenceEntity tenancy = pcsCaseEntity.getTenancyLicence();
        assertThat(tenancy.getType()).isEqualTo(CombinedLicenceType.ASSURED_TENANCY);
        assertThat(tenancy.getStartDate()).isEqualTo(LocalDate.of(2023, 4, 1));
        assertThat(tenancy.getRentAmount()).isEqualByComparingTo("1250.00");
        assertThat(tenancy.getRentFrequency()).isEqualTo(RentPaymentFrequency.MONTHLY);
        verify(pcsCaseRepository).save(pcsCaseEntity);
    }

    @Test
    void shouldRecordExcludedAndRegulatedTenanciesAsOtherWithTheirName() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getTenancy().setTenancyType(CitizenClaimTenancyType.REGULATED);

        underTest.submit(pcsCaseEntity, claim);

        assertThat(pcsCaseEntity.getTenancyLicence().getType()).isEqualTo(CombinedLicenceType.OTHER);
        assertThat(pcsCaseEntity.getTenancyLicence().getOtherTypeDetails()).isEqualTo("Regulated tenancy");
    }

    @Test
    void shouldPersistTheLedgerRentHistory() {
        underTest.submit(pcsCaseEntity, CitizenPossessionClaimFixtures.completeClaim());

        verify(ledgerEntryRepository).saveAll(ledgerCaptor.capture());
        List<RentHistoryLedgerEntryEntity> ledger = ledgerCaptor.getValue();
        assertThat(ledger).hasSize(2);
        assertThat(ledger.getFirst().getRank()).isEqualTo(1);
        assertThat(ledger.getFirst().isRentCharge()).isTrue();
        assertThat(ledger.getFirst().isTenantPayment()).isFalse();
        assertThat(ledger.getFirst().getAmountCharged()).isEqualByComparingTo("1250.00");
        assertThat(ledger.get(1).isTenantPayment()).isTrue();
        assertThat(ledger.get(1).getAmountPaid()).isEqualByComparingTo("250.00");
        assertThat(ledger.get(1).getClaim()).isNotNull();
    }

    @Test
    void shouldPersistTheTableRentHistory() {
        underTest.submit(pcsCaseEntity, CitizenPossessionClaimFixtures.completeClaim());

        verify(periodRepository).saveAll(periodCaptor.capture());
        assertThat(periodCaptor.getValue()).singleElement().satisfies(period -> {
            assertThat(period.getRank()).isEqualTo(1);
            assertThat(period.getPeriodStart()).isEqualTo(LocalDate.of(2026, 10, 1));
            assertThat(period.getAmountDue()).isEqualByComparingTo("1250.00");
            assertThat(period.getDatePaid()).isEqualTo(LocalDate.of(2026, 10, 20));
            assertThat(period.getAmountPaid()).isEqualByComparingTo("500.00");
        });
    }

    @Test
    void shouldKeepTheSubmittedAnswersSoNothingIsDropped() throws Exception {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();

        underTest.submit(pcsCaseEntity, claim);

        verify(submissionRepository).save(submissionCaptor.capture());
        CitizenPossessionClaim stored = objectMapper.readValue(
            submissionCaptor.getValue().getSubmittedClaim(), CitizenPossessionClaim.class);
        assertThat(stored).isEqualTo(claim.toBuilder().propertyAddress(CASE_ADDRESS).build());
        assertThat(stored.getNotice().getEmailedTo()).isEqualTo("tom.tenant@example.com");
        assertThat(stored.getPropertyAddress()).isEqualTo(CASE_ADDRESS);
        assertThat(stored.getDraftVersion()).isNull();
    }

    @Test
    void shouldPassTheClaimDocumentsToTheDocumentService() {
        underTest.submit(pcsCaseEntity, CitizenPossessionClaimFixtures.completeClaim());

        verify(documentService).createAllDocuments(documentCaseCaptor.capture());
        PCSCase documentCase = documentCaseCaptor.getValue();
        assertThat(documentCase.getTenancyLicenceDetails().getTenancyLicenceDocuments()).hasSize(1);
        assertThat(documentCase.getNoticeServedDetails().getDocuments()).hasSize(1);
        assertThat(documentCase.getAdditionalDocuments())
            .extracting(ListValue::getValue)
            .extracting(AdditionalDocument::getDocument)
            .extracting(document -> document.getFilename())
            .containsExactly("letters.pdf");
        assertThat(documentCase.getAdditionalDocuments().getFirst().getValue().getDocumentType().getValueLabel())
            .isEqualTo("Other document");
    }

    @Test
    void shouldCreateTheClaimIssueFeeServiceRequestForTheClaimant() {
        CitizenPossessionClaimSubmitService.SubmittedClaim submitted =
            underTest.submit(pcsCaseEntity, CitizenPossessionClaimFixtures.completeClaim());

        verify(paymentService).createServiceRequest(feeTaskCaptor.capture());
        FeesAndPayTaskData task = feeTaskCaptor.getValue();
        assertThat(task.getFeeDetails()).isSameAs(caseIssueFee);
        assertThat(task.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(task.getCcdCaseNumber()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(task.getResponsiblePartyId()).isEqualTo(CLAIMANT_PARTY_ID);
        assertThat(task.getResponsiblePartyName()).isEqualTo("Jane Landlord");
        assertThat(task.getPaymentCallbackHandlerType()).isEqualTo(PaymentCallbackHandlerType.CLAIM);

        assertThat(submitted.serviceRequestReference()).isEqualTo("2026-1762441234567");
        assertThat(submitted.feeDetails()).isSameAs(caseIssueFee);
    }

}
