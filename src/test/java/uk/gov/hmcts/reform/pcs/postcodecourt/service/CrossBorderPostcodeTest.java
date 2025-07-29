package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.PostcodeCourtMapping;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrossBorderPostcodeTest {

    private static final String CROSS_BORDER_POSTCODE = "SY13 2LH";
    private static final LocalDate TODAY = LocalDate.of(2025, 7, 15);
    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2025-07-15T10:00:00Z"),
        ZoneId.of("UTC")
    );

    @Mock
    private PostCodeCourtRepository postCodeCourtRepository;

    @Mock
    private PartialPostcodesGenerator partialPostcodesGenerator;

    private EligibilityService underTest;

    @BeforeEach
    void setUp() {
        underTest = new EligibilityService(postCodeCourtRepository, partialPostcodesGenerator, FIXED_CLOCK);
    }

    @Test
    void shouldDetectCrossBorderPostcode() {
        // Given
        List<String> partialPostcodes = List.of("SY132LH", "SY132L", "SY132", "SY13");
        when(partialPostcodesGenerator.generateForPostcode(CROSS_BORDER_POSTCODE)).thenReturn(partialPostcodes);

        PostcodeCourtMapping englandMapping = createMapping("SY132LH", 20262, LegislativeCountry.ENGLAND);
        PostcodeCourtMapping walesMapping = createMapping("SY132LH", 28837, LegislativeCountry.WALES);
        when(postCodeCourtRepository.findByPostCodeIn(partialPostcodes))
            .thenReturn(List.of(englandMapping, walesMapping));

        // When
        EligibilityResult result = underTest.checkEligibility(CROSS_BORDER_POSTCODE, null);

        // Then
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED);
        assertThat(result.getLegislativeCountries())
            .containsExactlyInAnyOrder(LegislativeCountry.ENGLAND, LegislativeCountry.WALES);
    }

    @Test
    void shouldReturnEligibleResultForCrossBorderPostcodeWithLegislativeCountry() {
        // Given
        List<String> partialPostcodes = List.of("SY132LH", "SY132L", "SY132", "SY13");
        when(partialPostcodesGenerator.generateForPostcode(CROSS_BORDER_POSTCODE)).thenReturn(partialPostcodes);

        PostcodeCourtMapping englandMapping = createMapping("SY132LH", 20262, LegislativeCountry.ENGLAND);
        when(postCodeCourtRepository.findByPostCodeIn(partialPostcodes, LegislativeCountry.ENGLAND))
            .thenReturn(List.of(englandMapping));

        // When
        EligibilityResult result = underTest.checkEligibility(CROSS_BORDER_POSTCODE, LegislativeCountry.ENGLAND);

        // Then
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.getEpimsId()).isEqualTo(20262);
        assertThat(result.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
    }

    @Test
    void shouldHandleMultipleCrossBorderCountries() {
        // Given
        List<String> partialPostcodes = List.of("TD151", "TD15", "TD1", "TD");
        when(partialPostcodesGenerator.generateForPostcode("TD15 1UU")).thenReturn(partialPostcodes);

        PostcodeCourtMapping englandMapping = createMapping("TD151", 144641, LegislativeCountry.ENGLAND);
        PostcodeCourtMapping scotlandMapping = createMapping("TD151", 425094, LegislativeCountry.SCOTLAND);
        when(postCodeCourtRepository.findByPostCodeIn(partialPostcodes))
            .thenReturn(List.of(englandMapping, scotlandMapping));

        // When
        EligibilityResult result = underTest.checkEligibility("TD15 1UU", null);

        // Then
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED);
        assertThat(result.getLegislativeCountries())
            .containsExactlyInAnyOrder(LegislativeCountry.ENGLAND, LegislativeCountry.SCOTLAND);
    }

    private PostcodeCourtMapping createMapping(String postcode, Integer epimsId, LegislativeCountry country) {
        return PostcodeCourtMapping.builder()
            .postcode(postcode)
            .epimsId(epimsId)
            .legislativeCountry(country)
            .mappingEffectiveFrom(TODAY.minusDays(1))
            .courtEligibleFrom(TODAY.minusDays(1))
            .build();
    }
} 