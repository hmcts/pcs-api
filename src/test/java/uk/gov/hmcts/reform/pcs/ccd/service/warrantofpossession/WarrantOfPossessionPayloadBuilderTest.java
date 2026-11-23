package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.warrantofpossession.WarrantOfPossessionPayload;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantOfPossessionPayloadBuilderTest {

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

    private WarrantOfPossessionPayloadBuilder underTest;

    private final AddressEntity propertyAddress = AddressEntity.builder().addressLine1("1 Rented Road").build();
    private final PartyEntity claimant = party();
    private final PartyEntity defendant1 = party();
    private final PartyEntity defendant2 = party();
    private PcsCaseEntity pcsCase;

    @BeforeEach
    void setUp() {
        underTest = new WarrantOfPossessionPayloadBuilder(partyService, new CaseReferenceFormatter(),
            caseNameFormatter, addressMapper, new AddressFormatter(), recipientAddressResolver);

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
        EnforcementOrderEntity application = enforcementOrder;

        WarrantOfPossessionPayload payload = underTest.build(application);

        assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-9012-3456");
        assertThat(payload.getCaseName()).isEqualTo("Acme Ltd v Bob Tenant");
        assertThat(payload.getClaimantName()).isEqualTo("Acme Ltd");
        assertThat(payload.getApplicationType()).isEqualTo(WarrantOfPossessionPayloadBuilder.APPLICATION_TYPE);
        assertThat(payload.getPossessionOrderType()).isEqualTo(WarrantOfPossessionPayloadBuilder.SUSPENDED_ORDER);
        assertThat(payload.getBreathingSpaceAnswer()).isEqualTo("No");
        assertThat(payload.getMissedPaymentsAnswer()).isEqualTo("Yes");
        assertThat(payload.getPropertyAddress()).isEqualTo("1 Rented Road\nCardiff");
        assertThat(payload.getStatementOfTruthCompletedBy())
            .isEqualTo(WarrantOfPossessionPayloadBuilder.COMPLETED_BY_LEGAL_REPRESENTATIVE);
        assertThat(payload.getSignatoryFullName()).isEqualTo("Sam Solicitor");
        assertThat(payload.getSignatoryPosition()).isEqualTo("Partner");
        assertThat(payload.getSignatoryOrganisation()).isEqualTo("Solicitors LLP");
        assertThat(payload.getSubmissionDate()).isEqualTo("10 November 2026");
    }

    @Test
    void shouldShowOtherWhenTheOrderIsNotSuspended() {
        WarrantEntity warrant = new WarrantEntity();
        warrant.setIsSuspendedOrder(VerticalYesNo.NO);
        EnforcementOrderEntity application = enforcementOrder(warrant);

        WarrantOfPossessionPayload payload = underTest.build(application);

        assertThat(payload.getPossessionOrderType()).isEqualTo(WarrantOfPossessionPayloadBuilder.OTHER_ORDER);
    }

    @Test
    void shouldListEveryDefendantWhenEvictingEveryone() {
        WarrantEntity warrant = new WarrantEntity();
        warrant.setEvictEveryone(VerticalYesNo.YES);
        EnforcementOrderEntity application = enforcementOrder(warrant);

        WarrantOfPossessionPayload payload = underTest.build(application);

        assertThat(payload.getDefendantNames()).isEqualTo("Bob Tenant, Jo Tenant");
    }

    @Test
    void shouldListOnlyTheSelectedDefendantsWhenNotEvictingEveryone() {
        WarrantEntity warrant = new WarrantEntity();
        warrant.setEvictEveryone(VerticalYesNo.NO);
        EnforcementOrderEntity enforcementOrder = enforcementOrder(warrant);
        enforcementOrder.addSelectedDefendants(List.of(SelectedDefendantEntity.builder().party(defendant2).build()));
        EnforcementOrderEntity application = enforcementOrder;

        WarrantOfPossessionPayload payload = underTest.build(application);

        assertThat(payload.getDefendantNames()).isEqualTo("Jo Tenant");
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
