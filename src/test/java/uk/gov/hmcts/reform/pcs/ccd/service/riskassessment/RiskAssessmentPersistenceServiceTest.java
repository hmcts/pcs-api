package uk.gov.hmcts.reform.pcs.ccd.service.riskassessment;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.riskassessment.RiskAssessmentPayload;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;

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
class RiskAssessmentPersistenceServiceTest {

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

    private RiskAssessmentPersistenceService underTest;

    private final AddressEntity propertyAddress = AddressEntity.builder().addressLine1("1 Rented Road").build();
    private final PartyEntity claimant = party();
    private final PartyEntity defendant1 = party();
    private final PartyEntity defendant2 = party();
    private PcsCaseEntity pcsCase;

    @BeforeEach
    void setUp() {
        underTest = new RiskAssessmentPersistenceService(enforcementOrderRepository, documentImportService,
            claimActivityLogService, partyService, new CaseReferenceFormatter(), caseNameFormatter, addressMapper,
            new AddressFormatter());

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
    void shouldBuildNoPayloadWhenThereIsNoRiskProfile() {
        stubEnforcementOrder(enforcementOrder(new WarrantEntity(), null));

        assertThat(underTest.buildPayload(ENFORCEMENT_ORDER_ID)).isEmpty();
    }

    @Test
    void shouldBuildNoPayloadWhenTheApplicationIsNotAWarrant() {
        stubEnforcementOrder(enforcementOrder(null, new RiskProfileEntity()));

        assertThat(underTest.buildPayload(ENFORCEMENT_ORDER_ID)).isEmpty();
    }

    @Test
    void shouldBuildNoPayloadWhenTheRiskAssessmentHasAlreadyBeenAttached() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(new WarrantEntity(), new RiskProfileEntity());
        enforcementOrder.addDocuments(List.of(DocumentEntity.builder().type(DocumentType.RISK_ASSESSMENT).build()));
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
    void shouldBuildRiskAssessmentPayload() {
        when(caseNameFormatter.formatCaseName(pcsCase)).thenReturn("Acme Ltd v Bob Tenant");
        WarrantEntity warrant = new WarrantEntity();
        warrant.setEvictEveryone(VerticalYesNo.YES);
        warrant.setDefendantsDOBKnown(VerticalYesNo.YES);
        warrant.setDefendantsDOBDetails("Bob: 1 Jan 1980");
        warrant.setIsDifficultToAccessProperty(VerticalYesNo.YES);
        warrant.setClarificationOnAccessDifficultyText("Locked gate");
        warrant.setAdditionalInformationSelect(VerticalYesNo.NO);
        RiskProfileEntity risk = new RiskProfileEntity();
        risk.setAnyRiskToBailiff(YesNoNotSure.YES);
        risk.setViolentDetails("Shouted at agents");
        risk.setAnimalsDetails("Large dog");
        risk.setVulnerablePeoplePresent(YesNoNotSure.YES);
        risk.setVulnerableCategory(VulnerableCategory.VULNERABLE_CHILDREN);
        risk.setVulnerableReasonText("Young children");
        stubEnforcementOrder(enforcementOrder(warrant, risk));

        RiskAssessmentPayload payload = underTest.buildPayload(ENFORCEMENT_ORDER_ID).orElseThrow();

        assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-9012-3456");
        assertThat(payload.getCaseName()).isEqualTo("Acme Ltd v Bob Tenant");
        assertThat(payload.getApplicationType()).isEqualTo(RiskAssessmentPersistenceService.APPLICATION_TYPE);
        assertThat(payload.getClaimantName()).isEqualTo("Acme Ltd");
        assertThat(payload.getDefendantNames()).isEqualTo("Bob Tenant, Jo Tenant");
        assertThat(payload.getEvictionAddress()).isEqualTo("1 Rented Road\nCardiff");
        assertThat(payload.getDefendantDatesOfBirthKnown()).isEqualTo("Yes");
        assertThat(payload.getDefendantDatesOfBirth()).isEqualTo("Bob: 1 Jan 1980");
        assertThat(payload.getPropertyRiskResponse()).isEqualTo("Yes");
        assertThat(payload.getViolentAggressiveDetails()).isEqualTo("Shouted at agents");
        assertThat(payload.getAnimalDetails()).isEqualTo("Large dog");
        assertThat(payload.getVulnerabilityResponse()).isEqualTo("Yes");
        assertThat(payload.getVulnerablePeopleType()).isEqualTo(VulnerableCategory.VULNERABLE_CHILDREN.getLabel());
        assertThat(payload.getVulnerabilityDetails()).isEqualTo("Young children");
        assertThat(payload.getAccessDifficultyResponse()).isEqualTo("Yes");
        assertThat(payload.getAccessDifficultyDetails()).isEqualTo("Locked gate");
        assertThat(payload.getOtherEvictionInformationResponse()).isEqualTo("No");
    }

    @Test
    void shouldListTheSelectedRisksFromTheDetailsGiven() {
        RiskProfileEntity risk = new RiskProfileEntity();
        risk.setViolentDetails("Shouted at agents");
        risk.setAnimalsDetails("Large dog");
        stubEnforcementOrder(enforcementOrder(new WarrantEntity(), risk));

        RiskAssessmentPayload payload = underTest.buildPayload(ENFORCEMENT_ORDER_ID).orElseThrow();

        assertThat(payload.getSelectedPropertyRisks()).isEqualTo(
            RiskCategory.VIOLENT_OR_AGGRESSIVE.getLabel() + "\n" + RiskCategory.AGGRESSIVE_ANIMALS.getLabel());
    }

    @Test
    void shouldListOnlyTheSelectedDefendantsWhenNotEvictingEveryone() {
        WarrantEntity warrant = new WarrantEntity();
        warrant.setEvictEveryone(VerticalYesNo.NO);
        EnforcementOrderEntity enforcementOrder = enforcementOrder(warrant, new RiskProfileEntity());
        enforcementOrder.addSelectedDefendants(List.of(SelectedDefendantEntity.builder().party(defendant2).build()));
        stubEnforcementOrder(enforcementOrder);

        RiskAssessmentPayload payload = underTest.buildPayload(ENFORCEMENT_ORDER_ID).orElseThrow();

        assertThat(payload.getDefendantNames()).isEqualTo("Jo Tenant");
    }

    @Test
    void shouldAttachTheRiskAssessmentToTheApplicationInTheEnforcementFolder() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(new WarrantEntity(), new RiskProfileEntity());
        stubEnforcementOrder(enforcementOrder);

        underTest.attachDocument(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);

        verify(documentImportService).addDocumentToCase(pcsCase, DOCUMENT_URL, CaseFileCategory.ENFORCEMENT);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, claimant);
        assertThat(enforcementOrder.getDocuments()).singleElement()
            .satisfies(document -> {
                assertThat(document.getType()).isEqualTo(DocumentType.RISK_ASSESSMENT);
                assertThat(document.getEnfApplication()).isEqualTo(enforcementOrder);
            });
    }

    private void stubEnforcementOrder(EnforcementOrderEntity enforcementOrder) {
        when(enforcementOrderRepository.findById(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(enforcementOrder));
    }

    private EnforcementOrderEntity enforcementOrder(WarrantEntity warrant, RiskProfileEntity risk) {
        return EnforcementOrderEntity.builder()
            .id(ENFORCEMENT_ORDER_ID)
            .pcsCase(pcsCase)
            .warrantDetails(warrant)
            .riskProfileDetails(risk)
            .selectedDefendants(new ArrayList<>())
            .documents(new ArrayList<>())
            .build();
    }

    private static PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }
}
