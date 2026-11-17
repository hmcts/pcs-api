package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.warrantofpossession.WarrantOfPossessionPayload;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantOfPossessionPersistenceServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID ENFORCEMENT_ORDER_ID = UUID.randomUUID();
    private static final String DOCUMENT_URL = "http://dm-store/documents/123";

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private DocumentImportService documentImportService;
    @Mock
    private ClaimActivityLogService claimActivityLogService;
    @Mock
    private PartyService partyService;
    @Mock
    private CaseNameFormatter caseNameFormatter;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private RecipientAddressResolver recipientAddressResolver;
    @Mock
    private NotificationService notificationService;

    private WarrantOfPossessionPersistenceService underTest;

    private final AddressEntity propertyAddress = AddressEntity.builder().addressLine1("1 Rented Road").build();
    private final PartyEntity claimant = party();
    private final PartyEntity defendant1 = party();
    private final PartyEntity defendant2 = party();
    private PcsCaseEntity pcsCase;

    @BeforeEach
    void setUp() {
        underTest = new WarrantOfPossessionPersistenceService(enforcementOrderRepository, documentImportService,
            claimActivityLogService, partyService, new CaseReferenceFormatter(), caseNameFormatter, addressMapper,
            new AddressFormatter(), recipientAddressResolver,
            notificationService);

        ClaimEntity claim = ClaimEntity.builder().claimParties(List.of(
            ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant1).role(PartyRole.DEFENDANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant2).role(PartyRole.DEFENDANT).rank(2).build()
        )).build();
        pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).propertyAddress(propertyAddress)
            .claims(List.of(claim)).build();

        lenient().when(partyService.getPrimaryClaimantPartyEntity(pcsCase)).thenReturn(claimant);
        lenient().when(partyService.getPartyName(claimant)).thenReturn("Acme Ltd");
        lenient().when(partyService.getPartyName(defendant1)).thenReturn("Bob Tenant");
        lenient().when(partyService.getPartyName(defendant2)).thenReturn("Jo Tenant");
        lenient().when(addressMapper.toAddressUK(propertyAddress))
            .thenReturn(AddressUK.builder().addressLine1("1 Rented Road").postTown("Cardiff").build());
        lenient().when(documentImportService.addDocumentToCase(any(PcsCaseEntity.class), any(), any()))
            .thenAnswer(invocation -> DocumentEntity.builder().id(UUID.randomUUID()).build());
    }

    @Test
    void shouldBuildNoPayloadWhenTheApplicationIsNotAWarrant() {
        EnforcementOrderEntity writOrder = enforcementOrder(null);
        writOrder.setWritDetails(WritEntity.builder().build());
        stubEnforcementOrder(writOrder);

        assertThat(underTest.buildPayload(ENFORCEMENT_ORDER_ID)).isEmpty();
    }

    @Test
    void shouldBuildNoPayloadWhenTheWarrantHasAlreadyBeenAttached() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(new WarrantEntity());
        enforcementOrder.addDocuments(
            List.of(DocumentEntity.builder().type(DocumentType.WARRANT_OF_POSSESSION).build()));
        stubEnforcementOrder(enforcementOrder);

        assertThat(underTest.buildPayload(ENFORCEMENT_ORDER_ID)).isEmpty();
    }

    @Test
    void shouldThrowWhenTheEnforcementApplicationIsNotFound() {
        when(enforcementOrderRepository.findById(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.buildPayload(ENFORCEMENT_ORDER_ID))
            .isInstanceOf(EnforcementApplicationNotFoundException.class)
            .hasMessageContaining(ENFORCEMENT_ORDER_ID.toString());
    }

    @Test
    void shouldBuildWarrantPayload() {
        when(caseNameFormatter.formatCaseName(pcsCase)).thenReturn("Acme Ltd v Bob Tenant");
        WarrantEntity warrant = new WarrantEntity();
        warrant.setIsSuspendedOrder(VerticalYesNo.YES);
        warrant.setDefendantInBreathingSpace(YesNoNotSure.NO);
        warrant.setDefendantMissedPayments(VerticalYesNo.YES);
        EnforcementOrderEntity enforcementOrder = enforcementOrder(warrant);
        enforcementOrder.setStatementOfTruth(StatementOfTruthEntity.builder()
            .completedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE)
            .fullName("Sam Solicitor")
            .positionHeld("Partner")
            .firmName("Solicitors LLP")
            .completedDate(LocalDateTime.of(2026, 11, 10, 9, 30))
            .build());
        stubEnforcementOrder(enforcementOrder);

        WarrantOfPossessionPayload payload = underTest.buildPayload(ENFORCEMENT_ORDER_ID).orElseThrow();

        assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-9012-3456");
        assertThat(payload.getCaseName()).isEqualTo("Acme Ltd v Bob Tenant");
        assertThat(payload.getClaimantName()).isEqualTo("Acme Ltd");
        assertThat(payload.getApplicationType()).isEqualTo(WarrantOfPossessionPersistenceService.APPLICATION_TYPE);
        assertThat(payload.getPossessionOrderType()).isEqualTo(WarrantOfPossessionPersistenceService.SUSPENDED_ORDER);
        assertThat(payload.getBreathingSpaceAnswer()).isEqualTo("No");
        assertThat(payload.getMissedPaymentsAnswer()).isEqualTo("Yes");
        assertThat(payload.getPropertyAddress()).isEqualTo("1 Rented Road\nCardiff");
        assertThat(payload.getStatementOfTruthCompletedBy())
            .isEqualTo(WarrantOfPossessionPersistenceService.COMPLETED_BY_LEGAL_REPRESENTATIVE);
        assertThat(payload.getSignatoryFullName()).isEqualTo("Sam Solicitor");
        assertThat(payload.getSignatoryPosition()).isEqualTo("Partner");
        assertThat(payload.getSignatoryOrganisation()).isEqualTo("Solicitors LLP");
        assertThat(payload.getSubmissionDate()).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void shouldShowOtherWhenTheOrderIsNotSuspended() {
        WarrantEntity warrant = new WarrantEntity();
        warrant.setIsSuspendedOrder(VerticalYesNo.NO);
        stubEnforcementOrder(enforcementOrder(warrant));

        WarrantOfPossessionPayload payload = underTest.buildPayload(ENFORCEMENT_ORDER_ID).orElseThrow();

        assertThat(payload.getPossessionOrderType()).isEqualTo(WarrantOfPossessionPersistenceService.OTHER_ORDER);
    }

    @Test
    void shouldListEveryDefendantWhenEvictingEveryone() {
        WarrantEntity warrant = new WarrantEntity();
        warrant.setEvictEveryone(VerticalYesNo.YES);
        stubEnforcementOrder(enforcementOrder(warrant));

        WarrantOfPossessionPayload payload = underTest.buildPayload(ENFORCEMENT_ORDER_ID).orElseThrow();

        assertThat(payload.getDefendantNames()).isEqualTo("Bob Tenant, Jo Tenant");
    }

    @Test
    void shouldListOnlyTheSelectedDefendantsWhenNotEvictingEveryone() {
        WarrantEntity warrant = new WarrantEntity();
        warrant.setEvictEveryone(VerticalYesNo.NO);
        EnforcementOrderEntity enforcementOrder = enforcementOrder(warrant);
        enforcementOrder.addSelectedDefendants(List.of(SelectedDefendantEntity.builder().party(defendant2).build()));
        stubEnforcementOrder(enforcementOrder);

        WarrantOfPossessionPayload payload = underTest.buildPayload(ENFORCEMENT_ORDER_ID).orElseThrow();

        assertThat(payload.getDefendantNames()).isEqualTo("Jo Tenant");
    }

    @Test
    void shouldAttachTheWarrantToTheApplicationInTheEnforcementFolder() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(new WarrantEntity());
        stubEnforcementOrder(enforcementOrder);

        underTest.attachDocumentAndQueueEmail(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);

        verify(documentImportService).addDocumentToCase(pcsCase, DOCUMENT_URL, CaseFileCategory.ENFORCEMENT);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, claimant);
        assertThat(enforcementOrder.getDocuments()).singleElement()
            .satisfies(document -> {
                assertThat(document.getType()).isEqualTo(DocumentType.WARRANT_OF_POSSESSION);
                assertThat(document.getEnfApplication()).isEqualTo(enforcementOrder);
            });
    }

    @Test
    void shouldEmailTheClaimantOnceTheWarrantIsAttached() {
        stubEnforcementOrder(enforcementOrder(new WarrantEntity()));

        underTest.attachDocumentAndQueueEmail(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);

        verify(notificationService).sendWarrantOfPossessionEmail(claimant);
    }

    private void stubEnforcementOrder(EnforcementOrderEntity enforcementOrder) {
        when(enforcementOrderRepository.findById(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(enforcementOrder));
    }

    private EnforcementOrderEntity enforcementOrder(WarrantEntity warrant) {
        return EnforcementOrderEntity.builder()
            .id(ENFORCEMENT_ORDER_ID)
            .pcsCase(pcsCase)
            .warrantDetails(warrant)
            .statementOfTruth(StatementOfTruthEntity.builder()
                                  .completedDate(LocalDateTime.of(2026, 11, 10, 9, 30))
                                  .build())
            .selectedDefendants(new ArrayList<>())
            .documents(new ArrayList<>())
            .build();
    }

    private static PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }
}
