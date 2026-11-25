package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.ClaimantClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.ClaimantCaseSummaryRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenClaimListServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private ClaimantCaseSummaryRepository claimantCaseSummaryRepository;

    @InjectMocks
    private CitizenClaimListService underTest;

    private static final String IDAM_ID = UUID.randomUUID().toString();

    @Test
    void shouldReturnMappedClaimSummariesWhenClaimsExist() {
        // Given
        PartyEntity claimant = PartyEntity.builder().orgName("Smith & Co").build();

        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .caseReference(1234567890L)
            .propertyAddress(AddressEntity.builder().postcode("SW1A 1AA").build())
            .build();

        ClaimEntity claim = ClaimEntity.builder().pcsCase(pcsCase).build();
        claim.addParty(claimant, PartyRole.CLAIMANT);
        when(partyRepository.findClaimsByIdamIdAndRole(IDAM_ID, PartyRole.DEFENDANT))
            .thenReturn(List.of(claim));

        // When
        List<ClaimSummary> result = underTest.getClaimsAgainst(IDAM_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCaseReference()).isEqualTo("1234567890");
        assertThat(result.get(0).getClaimantName()).isEqualTo("Smith & Co");
        assertThat(result.get(0).getPropertyPostcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    void shouldReturnEmptyListWhenNoClaimsExist() {
        // Given
        when(partyRepository.findClaimsByIdamIdAndRole(IDAM_ID, PartyRole.DEFENDANT))
            .thenReturn(List.of());
        // When
        List<ClaimSummary> result = underTest.getClaimsAgainst(IDAM_ID);
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldIgnoreDefendantPartyWhenExtractingClaimantName() {
        // Given
        PartyEntity defendant = PartyEntity.builder().orgName("Defendant Org").build();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .caseReference(1234567890L)
            .propertyAddress(AddressEntity.builder().postcode("SW1A 1AA").build())
            .build();

        ClaimEntity claim = ClaimEntity.builder().pcsCase(pcsCase).build();
        claim.addParty(defendant, PartyRole.DEFENDANT);
        when(partyRepository.findClaimsByIdamIdAndRole(IDAM_ID, PartyRole.DEFENDANT))
            .thenReturn(List.of(claim));

        // When
        List<ClaimSummary> result = underTest.getClaimsAgainst(IDAM_ID);

        // Then
        assertThat(result.get(0).getClaimantName()).isNull();
    }

    @Test
    void shouldReturnNullClaimantNameWhenNoClaimantPartyExists() {
        // Given
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .caseReference(1234567890L)
            .propertyAddress(AddressEntity.builder().postcode("SW1A 1AA").build())
            .build();

        ClaimEntity claim = ClaimEntity.builder().pcsCase(pcsCase).build();
        when(partyRepository.findClaimsByIdamIdAndRole(IDAM_ID, PartyRole.DEFENDANT))
            .thenReturn(List.of(claim));

        // When
        List<ClaimSummary> result = underTest.getClaimsAgainst(IDAM_ID);

        // Then
        assertThat(result.get(0).getClaimantName()).isNull();
    }

    @Test
    void shouldReturnNullPostcodeWhenPropertyAddressIsAbsent() {
        // Given
        PartyEntity claimant = PartyEntity.builder().orgName("Smith & Co").build();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .caseReference(1234567890L)
            .build();

        ClaimEntity claim = ClaimEntity.builder().pcsCase(pcsCase).build();
        claim.addParty(claimant, PartyRole.CLAIMANT);
        when(partyRepository.findClaimsByIdamIdAndRole(IDAM_ID, PartyRole.DEFENDANT))
            .thenReturn(List.of(claim));

        // When
        List<ClaimSummary> result = underTest.getClaimsAgainst(IDAM_ID);

        // Then
        assertThat(result.get(0).getPropertyPostcode()).isNull();
    }

    @Test
    void shouldListClaimsMadeByTheCitizenFromTheRepositoryWithoutCallingCcd() {
        // Given
        ClaimantClaimSummary newest = ClaimantClaimSummary.builder()
            .caseReference("2000000000000002").state("AWAITING_SUBMISSION_TO_HMCTS").build();
        ClaimantClaimSummary oldest = ClaimantClaimSummary.builder()
            .caseReference("1000000000000001").state("CASE_ISSUED").paymentStatus("PAID").build();
        when(claimantCaseSummaryRepository.findCasesCreatedBy(IDAM_ID)).thenReturn(List.of(newest, oldest));

        // When
        List<ClaimantClaimSummary> result = underTest.getClaimsMadeBy(IDAM_ID);

        // Then
        assertThat(result).containsExactly(newest, oldest);
    }
}
