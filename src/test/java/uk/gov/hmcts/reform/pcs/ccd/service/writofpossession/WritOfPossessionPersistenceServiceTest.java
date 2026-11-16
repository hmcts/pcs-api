package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.enforcement.EnforcementDefendant;
import uk.gov.hmcts.reform.pcs.document.model.writofpossession.WritOfPossessionPayload;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WritOfPossessionPersistenceServiceTest {

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
    private DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    @Mock
    private RecipientAddressResolver recipientAddressResolver;
    @Mock
    private NotificationService notificationService;

    private WritOfPossessionPersistenceService underTest;

    private final AddressEntity propertyAddress = AddressEntity.builder().addressLine1("1 Rented Road").build();
    private final AddressEntity claimantAddress = AddressEntity.builder().addressLine1("2 Landlord Lane").build();
    private final PartyEntity claimant = party();
    private final PartyEntity defendant1 = party();
    private final PartyEntity defendant2 = party();
    private final PartyEntity removedDefendant = PartyEntity.builder().id(UUID.randomUUID()).removed(true).build();
    private PcsCaseEntity pcsCase;

    @BeforeEach
    void setUp() {
        underTest = new WritOfPossessionPersistenceService(enforcementOrderRepository, documentImportService,
            claimActivityLogService, partyService, new CaseReferenceFormatter(), caseNameFormatter, addressMapper,
            new AddressFormatter(), defenceCorrespondenceAddressResolver, recipientAddressResolver,
            notificationService);

        ClaimEntity claim = ClaimEntity.builder().claimParties(List.of(
            ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant2).role(PartyRole.DEFENDANT).rank(2).build(),
            ClaimPartyEntity.builder().party(defendant1).role(PartyRole.DEFENDANT).rank(1).build(),
            ClaimPartyEntity.builder().party(removedDefendant).role(PartyRole.DEFENDANT).rank(3).build()
        )).build();
        pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).propertyAddress(propertyAddress)
            .claims(List.of(claim)).build();

        lenient().when(partyService.getPrimaryClaimantPartyEntity(pcsCase)).thenReturn(claimant);
        lenient().when(partyService.getPartyName(claimant)).thenReturn("Acme Ltd");
        lenient().when(addressMapper.toAddressUK(propertyAddress))
            .thenReturn(AddressUK.builder().addressLine1("1 Rented Road").postTown("Cardiff").build());
        lenient().when(addressMapper.toAddressUK(claimantAddress))
            .thenReturn(AddressUK.builder().addressLine1("2 Landlord Lane").postTown("Bristol").build());
        lenient().when(documentImportService.addDocumentToCase(any(PcsCaseEntity.class), any(), any()))
            .thenAnswer(invocation -> DocumentEntity.builder().id(UUID.randomUUID()).build());
    }

    @Test
    void shouldBuildNoPayloadWhenTheApplicationIsNotAWrit() {
        EnforcementOrderEntity warrantOrder = enforcementOrder(null);
        warrantOrder.setWarrantDetails(new WarrantEntity());
        stubEnforcementOrder(warrantOrder);

        assertThat(underTest.buildPayload(ENFORCEMENT_ORDER_ID)).isEmpty();
    }

    @Test
    void shouldBuildNoPayloadWhenTheWritHasAlreadyBeenAttached() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(WritEntity.builder().build());
        enforcementOrder.addDocuments(List.of(DocumentEntity.builder().type(DocumentType.WRIT_OF_POSSESSION).build()));
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
    void shouldBuildWritPayload() {
        when(caseNameFormatter.formatCaseName(pcsCase)).thenReturn("Acme Ltd v Bob Tenant");
        when(recipientAddressResolver.resolvePostalAddress(claimant, PartyRole.CLAIMANT, propertyAddress))
            .thenReturn(claimantAddress);
        claimant.setPhoneNumber("01234 567890");
        claimant.setEmailAddress("claims@acme.test");
        WritEntity writ = WritEntity.builder()
            .defendantInBreathingSpace(YesNoNotSure.NO)
            .hasClaimTransferredToHighCourt(YesOrNo.YES)
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.YES)
            .hceoDetails("Jane Officer")
            .build();
        StatementOfTruthEntity statementOfTruth = StatementOfTruthEntity.builder()
            .completedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE)
            .fullName("Sam Solicitor")
            .positionHeld("Partner")
            .firmName("Solicitors LLP")
            .completedDate(LocalDateTime.of(2026, 11, 10, 9, 30))
            .build();
        stubEnforcementOrder(enforcementOrder(writ, statementOfTruth));

        WritOfPossessionPayload payload = underTest.buildPayload(ENFORCEMENT_ORDER_ID).orElseThrow();

        assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-9012-3456");
        assertThat(payload.getCaseName()).isEqualTo("Acme Ltd v Bob Tenant");
        assertThat(payload.getClaimantName()).isEqualTo("Acme Ltd");
        assertThat(payload.getClaimantAddress()).isEqualTo("2 Landlord Lane\nBristol");
        assertThat(payload.getClaimantTelephoneNumber()).isEqualTo("01234 567890");
        assertThat(payload.getClaimantEmailAddress()).isEqualTo("claims@acme.test");
        assertThat(payload.getPropertyAddress()).isEqualTo("1 Rented Road\nCardiff");
        assertThat(payload.getBreathingSpaceAnswer()).isEqualTo("No");
        assertThat(payload.getHighCourtPermissionAnswer()).isEqualTo("Yes");
        assertThat(payload.getHceoHiredAnswer()).isEqualTo("Yes");
        assertThat(payload.getHceoName()).isEqualTo("Jane Officer");
        assertThat(payload.getHceoSelectionOutcome()).isNull();
        assertThat(payload.getStatementOfTruthCompletedBy())
            .isEqualTo(WritOfPossessionPersistenceService.COMPLETED_BY_LEGAL_REPRESENTATIVE);
        assertThat(payload.getSignatoryFullName()).isEqualTo("Sam Solicitor");
        assertThat(payload.getSignatoryPosition()).isEqualTo("Partner");
        assertThat(payload.getSignatoryOrganisation()).isEqualTo("Solicitors LLP");
        assertThat(payload.getSubmissionDate()).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void shouldListEveryActiveDefendantOnTheClaimWithTheirOwnAddress() {
        when(partyService.getPartyName(defendant1)).thenReturn("Bob Tenant");
        when(partyService.getPartyName(defendant2)).thenReturn("Jo Tenant");
        when(defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(defendant1, propertyAddress))
            .thenReturn(AddressUK.builder().addressLine1("1 Rented Road").postCode("CF10 1AA").build());
        when(defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(defendant2, propertyAddress))
            .thenReturn(AddressUK.builder().addressLine1("9 Elsewhere Street").postCode("LS1 2BB").build());
        stubEnforcementOrder(enforcementOrder(WritEntity.builder().build()));

        WritOfPossessionPayload payload = underTest.buildPayload(ENFORCEMENT_ORDER_ID).orElseThrow();

        assertThat(payload.getDefendants())
            .extracting(EnforcementDefendant::getName, EnforcementDefendant::getAddress)
            .containsExactly(
                tuple("Bob Tenant", "1 Rented Road\nCF10 1AA"),
                tuple("Jo Tenant", "9 Elsewhere Street\nLS1 2BB"));
    }

    @Test
    void shouldSayTheOfficerWillBeChosenWhenNoneHasBeenHired() {
        WritEntity writ = WritEntity.builder()
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.NO)
            .hceoDetails("Ignored Officer")
            .build();
        stubEnforcementOrder(enforcementOrder(writ));

        WritOfPossessionPayload payload = underTest.buildPayload(ENFORCEMENT_ORDER_ID).orElseThrow();

        assertThat(payload.getHceoName()).isNull();
        assertThat(payload.getHceoSelectionOutcome())
            .isEqualTo(WritOfPossessionPersistenceService.HCEO_SELECTED_BY_NICE);
    }

    @Test
    void shouldAttachTheWritToTheApplicationInTheEnforcementFolder() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(WritEntity.builder().build());
        stubEnforcementOrder(enforcementOrder);

        underTest.attachDocumentAndQueueEmail(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);

        verify(documentImportService).addDocumentToCase(pcsCase, DOCUMENT_URL, CaseFileCategory.ENFORCEMENT);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, claimant);
        assertThat(enforcementOrder.getDocuments()).singleElement()
            .satisfies(document -> {
                assertThat(document.getType()).isEqualTo(DocumentType.WRIT_OF_POSSESSION);
                assertThat(document.getEnfApplication()).isEqualTo(enforcementOrder);
            });
    }

    @Test
    void shouldEmailTheClaimantOnceTheWritIsAttached() {
        stubEnforcementOrder(enforcementOrder(WritEntity.builder().build()));

        underTest.attachDocumentAndQueueEmail(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);

        verify(notificationService).sendWritOfPossessionEmail(claimant);
    }

    private void stubEnforcementOrder(EnforcementOrderEntity enforcementOrder) {
        when(enforcementOrderRepository.findById(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(enforcementOrder));
    }

    private EnforcementOrderEntity enforcementOrder(WritEntity writ) {
        return enforcementOrder(writ, StatementOfTruthEntity.builder()
            .completedDate(LocalDateTime.of(2026, 11, 10, 9, 30))
            .build());
    }

    private EnforcementOrderEntity enforcementOrder(WritEntity writ, StatementOfTruthEntity statementOfTruth) {
        return EnforcementOrderEntity.builder()
            .id(ENFORCEMENT_ORDER_ID)
            .pcsCase(pcsCase)
            .writDetails(writ)
            .statementOfTruth(statementOfTruth)
            .selectedDefendants(new ArrayList<>())
            .documents(new ArrayList<>())
            .build();
    }

    private static PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }
}
