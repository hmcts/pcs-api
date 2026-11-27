package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.writofpossession.WritOfPossessionPayload;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WritOfPossessionPayloadBuilderTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID ENFORCEMENT_ORDER_ID = UUID.randomUUID();

    @Mock
    private PartyService partyService;
    @Mock
    private CaseNameFormatter caseNameFormatter;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private RecipientAddressResolver recipientAddressResolver;

    private WritOfPossessionPayloadBuilder underTest;

    private final AddressEntity propertyAddress = AddressEntity.builder().addressLine1("1 Rented Road").build();
    private final AddressEntity claimantAddress = AddressEntity.builder().addressLine1("2 Landlord Lane").build();
    private final PartyEntity claimant = party();
    private final PartyEntity defendant1 = party();
    private final PartyEntity defendant2 = party();
    private final PartyEntity removedDefendant = PartyEntity.builder().id(UUID.randomUUID()).removed(true).build();
    private PcsCaseEntity pcsCase;

    @BeforeEach
    void setUp() {
        underTest = new WritOfPossessionPayloadBuilder(partyService, new CaseReferenceFormatter(),
            caseNameFormatter, addressMapper, new AddressFormatter(), recipientAddressResolver);

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
        EnforcementOrderEntity application = enforcementOrder(writ, statementOfTruth);

        WritOfPossessionPayload payload = underTest.build(application);

        assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-9012-3456");
        assertThat(payload.getCaseName()).isEqualTo("Acme Ltd v Bob Tenant");
        assertThat(payload.getClaimantName()).isEqualTo("Acme Ltd");
        assertThat(payload.getClaimantAddress()).isEqualTo("2 Landlord Lane\nBristol");
        assertThat(payload.getClaimantTelephoneNumber()).isEqualTo("01234 567890");
        assertThat(payload.getClaimantEmailAddress()).isEqualTo("claims@acme.test");
        assertThat(payload.getApplicationType()).isEqualTo("Writ of possession");
        assertThat(payload.getPropertyAddress()).isEqualTo("1 Rented Road\nCardiff");
        assertThat(payload.getBreathingSpaceAnswer()).isEqualTo("No");
        assertThat(payload.getHighCourtPermissionAnswer()).isEqualTo("Yes");
        assertThat(payload.getHceoHiredAnswer()).isEqualTo("Yes");
        assertThat(payload.getHceoName()).isEqualTo("Jane Officer");
        assertThat(payload.getHceoSelectionOutcome()).isNull();
        assertThat(payload.getStatementOfTruthCompletedBy())
            .isEqualTo(WritOfPossessionPayloadBuilder.COMPLETED_BY_LEGAL_REPRESENTATIVE);
        assertThat(payload.getSignatoryFullName()).isEqualTo("Sam Solicitor");
        assertThat(payload.getSignatoryPosition()).isEqualTo("Partner");
        assertThat(payload.getSignatoryOrganisation()).isEqualTo("Solicitors LLP");
        assertThat(payload.getSubmissionDate()).isEqualTo("10 November 2026");
    }

    @Test
    void shouldJoinTheNamesOfEveryActiveDefendantOnTheClaim() {
        when(partyService.getPartyName(defendant1)).thenReturn("Bob Tenant");
        when(partyService.getPartyName(defendant2)).thenReturn("Jo Tenant");
        EnforcementOrderEntity application = enforcementOrder(WritEntity.builder().build());

        WritOfPossessionPayload payload = underTest.build(application);

        assertThat(payload.getDefendantNames()).isEqualTo("Bob Tenant, Jo Tenant");
    }

    @Test
    void shouldSayTheOfficerWillBeChosenWhenNoneHasBeenHired() {
        WritEntity writ = WritEntity.builder()
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.NO)
            .hceoDetails("Ignored Officer")
            .build();
        EnforcementOrderEntity application = enforcementOrder(writ);

        WritOfPossessionPayload payload = underTest.build(application);

        assertThat(payload.getHceoName()).isNull();
        assertThat(payload.getHceoSelectionOutcome())
            .isEqualTo(WritOfPossessionPayloadBuilder.HCEO_SELECTED_BY_NICE);
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
