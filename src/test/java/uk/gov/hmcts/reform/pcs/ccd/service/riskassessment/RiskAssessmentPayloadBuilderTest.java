package uk.gov.hmcts.reform.pcs.ccd.service.riskassessment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.riskassessment.RiskAssessmentPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentPayloadBuilderTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID ENFORCEMENT_ORDER_ID = UUID.randomUUID();

    @Mock
    private PartyService partyService;
    @Mock
    private CaseNameFormatter caseNameFormatter;
    @Mock
    private AddressMapper addressMapper;

    private RiskAssessmentPayloadBuilder underTest;

    private final AddressEntity propertyAddress = AddressEntity.builder().addressLine1("1 Rented Road").build();
    private final PartyEntity claimant = party();
    private final PartyEntity defendant1 = party();
    private final PartyEntity defendant2 = party();
    private PcsCaseEntity pcsCase;

    @BeforeEach
    void setUp() {
        underTest = new RiskAssessmentPayloadBuilder(partyService, new CaseReferenceFormatter(),
            caseNameFormatter, addressMapper, new AddressFormatter());

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
        EnforcementOrderEntity application = enforcementOrder(warrant, risk);

        RiskAssessmentPayload payload = underTest.build(application);

        assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-9012-3456");
        assertThat(payload.getCaseName()).isEqualTo("Acme Ltd v Bob Tenant");
        assertThat(payload.getApplicationType()).isEqualTo(RiskAssessmentPayloadBuilder.APPLICATION_TYPE);
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
    void shouldListTheRiskCategoriesTheClaimantTicked() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(new WarrantEntity(), new RiskProfileEntity());
        enforcementOrder.setEnforcementOrder(EnforcementOrder.builder()
            .warrantDetails(WarrantDetails.builder()
                .riskCategories(Set.of(RiskCategory.AGGRESSIVE_ANIMALS, RiskCategory.VIOLENT_OR_AGGRESSIVE))
                .build())
            .build());
        EnforcementOrderEntity application = enforcementOrder;

        RiskAssessmentPayload payload = underTest.build(application);

        assertThat(payload.getSelectedPropertyRisks()).isEqualTo(
            RiskCategory.VIOLENT_OR_AGGRESSIVE.getLabel() + "\n" + RiskCategory.AGGRESSIVE_ANIMALS.getLabel());
    }

    @Test
    void shouldListOnlyTheSelectedDefendantsWhenNotEvictingEveryone() {
        WarrantEntity warrant = new WarrantEntity();
        warrant.setEvictEveryone(VerticalYesNo.NO);
        EnforcementOrderEntity enforcementOrder = enforcementOrder(warrant, new RiskProfileEntity());
        enforcementOrder.addSelectedDefendants(List.of(SelectedDefendantEntity.builder().party(defendant2).build()));
        EnforcementOrderEntity application = enforcementOrder;

        RiskAssessmentPayload payload = underTest.build(application);

        assertThat(payload.getDefendantNames()).isEqualTo("Jo Tenant");
    }

    private EnforcementOrderEntity enforcementOrder(WarrantEntity warrant, RiskProfileEntity risk) {
        return EnforcementOrderEntity.builder()
            .id(ENFORCEMENT_ORDER_ID)
            .pcsCase(pcsCase)
            .warrantDetails(warrant)
            .riskProfileDetails(risk)
            .enforcementOrder(EnforcementOrder.builder().warrantDetails(WarrantDetails.builder().build()).build())
            .selectedDefendants(new ArrayList<>())
            .documents(new ArrayList<>())
            .build();
    }

    private static PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }
}
