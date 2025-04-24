package uk.gov.hmcts.reform.pcs.location.service;

import com.azure.core.exception.ResourceNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LocationReferenceServiceTest {

    private static final String AUTHORIZATION = "Bearer some-auth-token";
    private static final String SERVICE_AUTH_TOKEN = "service-auth-token";
    private static final Integer BRENTFORD_COURT_EPIM_ID = 36791;
    private static final Integer LONDON_COURT_EPIM_ID = 20262;
    private static final int COUNTY_COURT_TYPE_ID = 10;
    public static final List<@NotNull Integer> EPIM_IDS = List.of(BRENTFORD_COURT_EPIM_ID, LONDON_COURT_EPIM_ID);
    private final String multipleEpimIdsJoined = String.join(",", BRENTFORD_COURT_EPIM_ID.toString(),
            LONDON_COURT_EPIM_ID.toString());

    @Mock
    private LocationReferenceApi locationReferenceApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @InjectMocks
    private LocationReferenceService locationReferenceService;

    @Test
    void shouldReturnCountyCourts_whenCalledWithValidSingleEpimId() {
        List<CourtVenue> expectedCourtVenues = List.of(
                new CourtVenue(BRENTFORD_COURT_EPIM_ID, 40838, "Brentford County Court And Family Court")
        );
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(locationReferenceApi.getCountyCourts(
                AUTHORIZATION,
                SERVICE_AUTH_TOKEN,
                BRENTFORD_COURT_EPIM_ID.toString(),
                COUNTY_COURT_TYPE_ID))
                .thenReturn(expectedCourtVenues);

        List<CourtVenue> actualCourtVenues = locationReferenceService.getCountyCourts(AUTHORIZATION,
                List.of(BRENTFORD_COURT_EPIM_ID));

        assertThat(expectedCourtVenues).isEqualTo(actualCourtVenues);
        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCountyCourts(
                AUTHORIZATION,
                SERVICE_AUTH_TOKEN,
                BRENTFORD_COURT_EPIM_ID.toString(),
                COUNTY_COURT_TYPE_ID
        );
    }

    @Test
    void shouldReturnCountyCourts_whenCalledWithValidMultipleEpimIds() {
        List<CourtVenue> expectedCourtVenues = List.of(
                new CourtVenue(BRENTFORD_COURT_EPIM_ID, 40838, "Brentford County Court And Family Court"),
                new CourtVenue(LONDON_COURT_EPIM_ID, 40827, "Central London County Court")
        );
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(locationReferenceApi.getCountyCourts(
            AUTHORIZATION,
            SERVICE_AUTH_TOKEN,
            multipleEpimIdsJoined,
            COUNTY_COURT_TYPE_ID))
                .thenReturn(expectedCourtVenues);

        List<CourtVenue> actualCourtVenues = locationReferenceService.getCountyCourts(AUTHORIZATION, EPIM_IDS);

        assertThat(expectedCourtVenues).isEqualTo(actualCourtVenues);
        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCountyCourts(
            AUTHORIZATION,
            SERVICE_AUTH_TOKEN,
            multipleEpimIdsJoined,
            COUNTY_COURT_TYPE_ID
        );
    }

    @Test
    void shouldReturnEmptyCountyCourtsList_whenCalledWithValidMultipleEpimIds() {
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);

        when(locationReferenceApi.getCountyCourts(
            AUTHORIZATION,
            SERVICE_AUTH_TOKEN,
            multipleEpimIdsJoined,
            COUNTY_COURT_TYPE_ID
        )).thenReturn(Collections.emptyList());

        List<CourtVenue> actualCourtVenues = locationReferenceService.getCountyCourts(AUTHORIZATION, EPIM_IDS);

        assertThat(actualCourtVenues.isEmpty()).isTrue();

        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCountyCourts(
            AUTHORIZATION,
            SERVICE_AUTH_TOKEN,
            multipleEpimIdsJoined,
            COUNTY_COURT_TYPE_ID
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowExceptionWhenEpimIdsIsNullOrEmpty(List<Integer> epimIds) {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> locationReferenceService.getCountyCourts(AUTHORIZATION, epimIds)
        );
        assertThat("epimIds cannot be null or empty").isEqualTo(exception.getMessage());
    }

    @Test
    void shouldThrow404NotfoundExceptionFromLocationReferenceThenReceiveEmptyListInResponse() {
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);

        when(locationReferenceApi.getCountyCourts(
                AUTHORIZATION,
                SERVICE_AUTH_TOKEN,
                "425094",
                COUNTY_COURT_TYPE_ID
        )).thenThrow(new ResourceNotFoundException("No matching courts found for LE2 0QB", null));

        assertThatThrownBy(() ->
                locationReferenceService.getCountyCourts(AUTHORIZATION, List.of(425094))
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No matching courts found for LE2 0QB");

        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCountyCourts(
                AUTHORIZATION,
                SERVICE_AUTH_TOKEN,
                "425094",
                COUNTY_COURT_TYPE_ID
        );
    }
}

