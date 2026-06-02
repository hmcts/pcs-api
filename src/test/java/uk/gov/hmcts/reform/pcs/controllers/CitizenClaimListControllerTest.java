package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CitizenClaimListService;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenClaimListControllerTest {

    private static final String AUTH_HEADER = "Bearer user-token";
    private static final String S2S_TOKEN = "service-token";
    private static final UUID IDAM_ID = UUID.randomUUID();

    @Mock
    private IdamAuthenticator idamAuthenticator;

    @Mock
    private CitizenClaimListService citizenClaimListService;

    @InjectMocks
    private CitizenClaimListController underTest;

    @Test
    void shouldReturn200WithClaims_WhenClaimsExist() {
        // Given
        UserInfo userDetails = mock(UserInfo.class);
        when(userDetails.getUid()).thenReturn(IDAM_ID.toString());
        User user = mock(User.class);
        when(user.getUserDetails()).thenReturn(userDetails);
        when(idamAuthenticator.validateAuthToken(AUTH_HEADER)).thenReturn(user);

        List<ClaimSummary> claims = List.of(
            ClaimSummary.builder().caseReference("123").claimantName("Smith & Co").propertyPostcode("SW1A 1AA").build()
        );
        when(citizenClaimListService.getClaimsAgainst(IDAM_ID)).thenReturn(claims);

        // When
        ResponseEntity<List<ClaimSummary>> response = underTest.getDefendantClaims(AUTH_HEADER, S2S_TOKEN);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(claims);
    }

    @Test
    void shouldReturn200WithEmptyList_WhenNoClaimsExist() {
        // Given
        UserInfo userDetails = mock(UserInfo.class);
        when(userDetails.getUid()).thenReturn(IDAM_ID.toString());
        User user = mock(User.class);
        when(user.getUserDetails()).thenReturn(userDetails);
        when(idamAuthenticator.validateAuthToken(AUTH_HEADER)).thenReturn(user);

        when(citizenClaimListService.getClaimsAgainst(IDAM_ID)).thenReturn(List.of());

        // When
        ResponseEntity<List<ClaimSummary>> response = underTest.getDefendantClaims(AUTH_HEADER, S2S_TOKEN);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
