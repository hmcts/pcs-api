package uk.gov.hmcts.reform.pcs.location.service;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LocationReferenceServiceTest {

    private static final String SERVICE_AUTH_TOKEN = "service-auth-token";
    private static final String SYSTEM_USER_TOKEN = "Bearer system-user-token";
    private static final String BRENTFORD_COURT_EPIM_ID = "36791";
    private static final String LONDON_COURT_EPIM_ID = "20262";
    private static final String SERVICE_CODE = "AAA3";
    public static final List<@NotNull Integer> EPIM_IDS = List.of(
        Integer.valueOf(BRENTFORD_COURT_EPIM_ID), Integer.valueOf(LONDON_COURT_EPIM_ID));
    private final String multipleEpimIdsJoined = String.join(",", BRENTFORD_COURT_EPIM_ID,
            LONDON_COURT_EPIM_ID);

    @Mock
    private LocationReferenceApi locationReferenceApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;

    @InjectMocks
    private LocationReferenceService locationReferenceService;

    @BeforeEach
    void beforeEach() {
        lenient().when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(SYSTEM_USER_TOKEN);
        lenient().when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        ReflectionTestUtils.setField(locationReferenceService, "serviceCode", SERVICE_CODE);
    }

    @Test
    void shouldReturnCourtVenues_whenCalledWithValidSingleEpimId() {
        List<CourtVenue> expectedCourtVenues = List.of(
            newCourtVenue(
                "123",
                "Brentford County Court And Family Court",
                BRENTFORD_COURT_EPIM_ID));
        when(locationReferenceApi.getCourtVenues(
            SYSTEM_USER_TOKEN,
            SERVICE_AUTH_TOKEN,
            BRENTFORD_COURT_EPIM_ID,
            SERVICE_CODE))
            .thenReturn(expectedCourtVenues);

        List<CourtVenue> actualCourtVenues = locationReferenceService
            .getCourtVenues(List.of(Integer.valueOf(BRENTFORD_COURT_EPIM_ID)));

        assertThat(expectedCourtVenues).isEqualTo(actualCourtVenues);
        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCourtVenues(
            SYSTEM_USER_TOKEN,
            SERVICE_AUTH_TOKEN,
            BRENTFORD_COURT_EPIM_ID,
            SERVICE_CODE
        );
    }

    @Test
    void shouldReturnCourtVenues_whenCalledWithValidMultipleEpimIds() {
        List<CourtVenue> expectedCourtVenues = List.of(
            newCourtVenue(
                "123",
                "Brentford County Court And Family Court",
                BRENTFORD_COURT_EPIM_ID),
            newCourtVenue(
                "456",
                "Central London County Court",
                LONDON_COURT_EPIM_ID)
        );
        when(locationReferenceApi.getCourtVenues(
            SYSTEM_USER_TOKEN,
            SERVICE_AUTH_TOKEN,
            multipleEpimIdsJoined,
            SERVICE_CODE))
                .thenReturn(expectedCourtVenues);

        List<CourtVenue> actualCourtVenues = locationReferenceService.getCourtVenues(EPIM_IDS);

        assertThat(expectedCourtVenues).isEqualTo(actualCourtVenues);
        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCourtVenues(
            SYSTEM_USER_TOKEN,
            SERVICE_AUTH_TOKEN,
            multipleEpimIdsJoined,
            SERVICE_CODE
        );
    }

    @Test
    void shouldReturnEmptyCourtVenuesList_whenCalledWithValidMultipleEpimIds() {
        when(locationReferenceApi.getCourtVenues(
            SYSTEM_USER_TOKEN,
            SERVICE_AUTH_TOKEN,
            multipleEpimIdsJoined,
            SERVICE_CODE
        )).thenReturn(Collections.emptyList());

        List<CourtVenue> actualCourtVenues = locationReferenceService.getCourtVenues(EPIM_IDS);

        assertThat(actualCourtVenues.isEmpty()).isTrue();

        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCourtVenues(
            SYSTEM_USER_TOKEN,
            SERVICE_AUTH_TOKEN,
            multipleEpimIdsJoined,
            SERVICE_CODE
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowExceptionWhenEpimIdsIsNullOrEmpty(List<Integer> epimIds) {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> locationReferenceService.getCourtVenues(epimIds)
        );
        assertThat("epimIds cannot be null or empty").isEqualTo(exception.getMessage());
    }

    @Test
    void shouldThrow404NotfoundExceptionFromLocationReferenceThenReceiveEmptyListInResponse() {
        when(locationReferenceApi.getCourtVenues(
            SYSTEM_USER_TOKEN,
            SERVICE_AUTH_TOKEN,
            "425094",
            SERVICE_CODE
        )).thenThrow(new RuntimeException("No matching courts found for LE2 0QB", null));

        assertThatThrownBy(() ->
                locationReferenceService.getCourtVenues(List.of(425094))
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No matching courts found for LE2 0QB");

        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCourtVenues(
            SYSTEM_USER_TOKEN,
            SERVICE_AUTH_TOKEN,
            "425094",
            SERVICE_CODE
        );
    }

    CourtVenue newCourtVenue(String courtVenueId, String siteName, String epimmsId) {
        return new CourtVenue(
            courtVenueId,
            epimmsId,
            siteName,
            "1",
            "London",
            "Central London County Court",
            "7",
            null,
            null,
            "YES",
            "Brentford County Court, Alexandra Road",
            "TW8 0JJ",
            "020 1234 5678",
            null,
            "",
            "DX 12345 Brentford",
            "",
            "",
            "Open",
            null,
            siteName,
            siteName,
            "N",
            "Y",
            "",
            "N",
            "N",
            "COURT",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        );
    }
}

