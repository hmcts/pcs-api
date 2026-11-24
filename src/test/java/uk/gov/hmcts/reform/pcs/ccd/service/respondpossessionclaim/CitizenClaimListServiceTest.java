package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimantClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class CitizenClaimListServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

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
    void shouldListClaimsMadeByTheCitizenNewestFirstWithStateAndAddress() {
        // Given
        PcsCaseEntity draft = caseWithAddress(2000000000000002L, "1 Garden Drive", "LU1 1AB");
        PcsCaseEntity submitted = caseWithAddress(1000000000000001L, "2 Park Road", "SW1A 1AA");
        when(partyRepository.findCasesCreatedBy(IDAM_ID)).thenReturn(List.of(submitted, draft));
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(coreCaseDataApi.getCase("Bearer token", "s2s", "2000000000000002"))
            .thenReturn(CaseDetails.builder().state("AWAITING_SUBMISSION_TO_HMCTS").build());
        when(coreCaseDataApi.getCase("Bearer token", "s2s", "1000000000000001"))
            .thenReturn(CaseDetails.builder().state("CASE_ISSUED").build());

        // When
        List<ClaimantClaimSummary> result = underTest.getClaimsMadeBy(IDAM_ID, "Bearer token");

        // Then
        assertThat(result).extracting(ClaimantClaimSummary::getCaseReference)
            .containsExactly("2000000000000002", "1000000000000001");
        assertThat(result).extracting(ClaimantClaimSummary::getState)
            .containsExactly("AWAITING_SUBMISSION_TO_HMCTS", "CASE_ISSUED");
        assertThat(result.get(0).getPropertyAddress().getAddressLine1()).isEqualTo("1 Garden Drive");
        assertThat(result.get(0).getPropertyAddress().getPostcode()).isEqualTo("LU1 1AB");
    }

    @Test
    void shouldLeaveOutACaseCcdWillNotShowTheClaimant() {
        // Given
        PcsCaseEntity hidden = caseWithAddress(1000000000000001L, "2 Park Road", "SW1A 1AA");
        when(partyRepository.findCasesCreatedBy(IDAM_ID)).thenReturn(List.of(hidden));
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(coreCaseDataApi.getCase(anyString(), anyString(), anyString())).thenThrow(
            new FeignException.Forbidden("Forbidden", forbiddenRequest(), null, null));

        // When
        List<ClaimantClaimSummary> result = underTest.getClaimsMadeBy(IDAM_ID, "Bearer token");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnNoAddressWhenTheCaseHasNone() {
        // Given
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1000000000000001L).build();
        when(partyRepository.findCasesCreatedBy(IDAM_ID)).thenReturn(List.of(pcsCase));
        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(coreCaseDataApi.getCase(anyString(), anyString(), anyString()))
            .thenReturn(CaseDetails.builder().state("CASE_ISSUED").build());

        // When
        List<ClaimantClaimSummary> result = underTest.getClaimsMadeBy(IDAM_ID, "Bearer token");

        // Then
        assertThat(result.get(0).getPropertyAddress()).isNull();
    }

    private static PcsCaseEntity caseWithAddress(long caseReference, String line1, String postcode) {
        return PcsCaseEntity.builder()
            .caseReference(caseReference)
            .propertyAddress(AddressEntity.builder().addressLine1(line1).postcode(postcode).build())
            .build();
    }

    private static Request forbiddenRequest() {
        return Request.create(Request.HttpMethod.GET, "/cases/1", Collections.emptyMap(), null,
                              StandardCharsets.UTF_8, null);
    }
}
