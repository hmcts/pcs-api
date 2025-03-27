package uk.gov.hmcts.reform.pcs.location.service;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class LocationReferenceServiceTest {

    private static final String AUTHORIZATION = "Bearer some-auth-token";
    private static final String SERVICE_AUTH_TOKEN = "service-auth-token";
    private static final String BRENTFORD_COURT_EPIM_ID = "36791";
    private static final String LONDON_COURT_EPIM_ID = "20262";
    private static final int COUNTY_COURT_TYPE_ID = 10;
    public static final List<@NotNull Integer> EPIM_IDS = List.of(36791, 20262);

    @Mock
    private LocationReferenceApi locationReferenceApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @InjectMocks
    private LocationReferenceService locationReferenceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnCountyCourts_whenCalledWithValidSingleEpimId() {
        List<CourtVenue> expectedCourtVenues = List.of(
                new CourtVenue(36791, 40838, "Brentford County Court And Family Court")
        );
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(locationReferenceApi.getCountyCourts(
                AUTHORIZATION,
                SERVICE_AUTH_TOKEN,
                BRENTFORD_COURT_EPIM_ID,
                COUNTY_COURT_TYPE_ID))
                .thenReturn(expectedCourtVenues);

        List<CourtVenue> actualCourtVenues = locationReferenceService.getCountyCourts(AUTHORIZATION,  List.of(36791));

        assertEquals(expectedCourtVenues, actualCourtVenues);
        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCountyCourts(
                AUTHORIZATION,
                SERVICE_AUTH_TOKEN,
                BRENTFORD_COURT_EPIM_ID,
                COUNTY_COURT_TYPE_ID
        );
    }

    @Test
    void shouldReturnCountyCourts_whenCalledWithValidMultipleEpimIds() {
        List<CourtVenue> expectedCourtVenues = List.of(
                new CourtVenue(36791, 40838, "Brentford County Court And Family Court"),
                new CourtVenue(20262, 40827, "Central London County Court")
        );
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(locationReferenceApi.getCountyCourts(
            AUTHORIZATION,
            SERVICE_AUTH_TOKEN,
            String.join(",", BRENTFORD_COURT_EPIM_ID, LONDON_COURT_EPIM_ID),
            COUNTY_COURT_TYPE_ID))
                .thenReturn(expectedCourtVenues);

        List<CourtVenue> actualCourtVenues = locationReferenceService.getCountyCourts(AUTHORIZATION, EPIM_IDS);

        assertEquals(expectedCourtVenues, actualCourtVenues);
        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCountyCourts(
            AUTHORIZATION,
            SERVICE_AUTH_TOKEN,
            String.join(",", BRENTFORD_COURT_EPIM_ID, LONDON_COURT_EPIM_ID),
            COUNTY_COURT_TYPE_ID
        );
    }

    @Test
    void shouldReturnEmptyCountyCourtsList_whenCalledWithValidMultipleEpimIds() {
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(locationReferenceApi.getCountyCourts(
            AUTHORIZATION,
            SERVICE_AUTH_TOKEN,
            String.join(",", BRENTFORD_COURT_EPIM_ID, LONDON_COURT_EPIM_ID),
            COUNTY_COURT_TYPE_ID
        )).thenReturn(Collections.emptyList());

        List<CourtVenue> actualCourtVenues = locationReferenceService.getCountyCourts(AUTHORIZATION, EPIM_IDS);

        assertTrue(actualCourtVenues.isEmpty());
        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCountyCourts(
            AUTHORIZATION,
            SERVICE_AUTH_TOKEN,
            String.join(",", BRENTFORD_COURT_EPIM_ID, LONDON_COURT_EPIM_ID),
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
        Assertions.assertEquals("epimIds cannot be null or empty", exception.getMessage());
    }
}

