package uk.gov.hmcts.reform.pcs.location.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.postcodecourt.record.CourtVenue;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class LocationReferenceServiceTest {

    private static final String AUTHORIZATION = "Bearer some-auth-token";
    private static final String SERVICE_AUTH_TOKEN = "service-auth-token";
    private static final Integer EPIMMS_ID = 12345;
    private static final int COUNTY_COURT_TYPE_ID = 10;

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
    void shouldReturnCountyCourts_whenCalledWithValidInputs() {
        // Given
        List<CourtVenue> expectedCourtVenues = List.of(new CourtVenue(36791, 40838, "Brentford County Court And Family Court"),
                                                       new CourtVenue(20262, 40827, "Central London County Court"));
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(locationReferenceApi.getCountyCourts(AUTHORIZATION, SERVICE_AUTH_TOKEN, EPIMMS_ID, COUNTY_COURT_TYPE_ID))
            .thenReturn(expectedCourtVenues);

        // When
        List<CourtVenue> actualCourtVenues = locationReferenceService.getCountyCourts(AUTHORIZATION, EPIMMS_ID);

        // Then
        assertEquals(expectedCourtVenues, actualCourtVenues);
        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCountyCourts(AUTHORIZATION, SERVICE_AUTH_TOKEN, EPIMMS_ID, COUNTY_COURT_TYPE_ID);
    }

    @Test
    void shouldReturnEmptyCountyCourtsList_whenCalledWithValidInputs() {
        // Given
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(locationReferenceApi.getCountyCourts(AUTHORIZATION, SERVICE_AUTH_TOKEN, EPIMMS_ID, COUNTY_COURT_TYPE_ID))
            .thenReturn(Collections.emptyList());

        // When
        List<CourtVenue> actualCourtVenues = locationReferenceService.getCountyCourts(AUTHORIZATION, EPIMMS_ID);

        // Then
        assertTrue(actualCourtVenues.isEmpty());
        verify(authTokenGenerator).generate();
        verify(locationReferenceApi).getCountyCourts(AUTHORIZATION, SERVICE_AUTH_TOKEN, EPIMMS_ID, COUNTY_COURT_TYPE_ID);
    }
}

