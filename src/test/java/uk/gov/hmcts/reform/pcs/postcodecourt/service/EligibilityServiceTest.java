package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.InvalidPostCodeException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.PostcodeCourtMapping;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

    private static final String TEST_POSTCODE = "AB12 3EF";
    private static final LocalDate TEST_DATE = LocalDate.of(2025, JUNE, 15);

    @Mock
    private PostCodeCourtRepository postCodeCourtRepository;
    @Mock
    private PartialPostcodesGenerator partialPostcodesGenerator;
    @Mock(strictness = LENIENT)
    private Clock ukClock;

    private EligibilityService underTest;

    @BeforeEach
    void setUp() {
        when(ukClock.instant()).thenReturn(TEST_DATE.atTime(15, 21).atZone(UK_ZONE_ID).toInstant());
        when(ukClock.getZone()).thenReturn(UK_ZONE_ID);

        underTest = new EligibilityService(postCodeCourtRepository, partialPostcodesGenerator, ukClock);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void shouldThrowExceptionForNullOrBlankPostcode(String postcode) {
        Throwable throwable = catchThrowable(() -> underTest.checkEligibility(postcode, ENGLAND));

        assertThat(throwable)
            .isInstanceOf(InvalidPostCodeException.class)
            .hasMessage("Postcode can't be empty or null");
    }

    @Test
    void shouldIndicateWhenNoActiveMappings() {
        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(TEST_POSTCODE);

        List<PostcodeCourtMapping> mappings = List.of(
            PostcodeCourtMapping.builder()
                .postcode(TEST_POSTCODE)
                .legislativeCountry(ENGLAND.getLabel())
                .mappingEffectiveFrom(TEST_DATE.minusWeeks(2))
                .mappingEffectiveTo(TEST_DATE.minusDays(1))
                .build(),
            PostcodeCourtMapping.builder()
                .postcode(TEST_POSTCODE)
                .mappingEffectiveFrom(TEST_DATE.plusDays(1))
                .legislativeCountry(ENGLAND.getLabel())
                .build()
        );

        when(postCodeCourtRepository.findByPostCodeIn(expectedPartialPostcodes)).thenReturn(mappings);

        EligibilityResult eligibilityResult = underTest.checkEligibility(TEST_POSTCODE, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.NO_MATCH_FOUND);
    }

    @Test
    void shouldIndicateWhenMultipleActiveMappings() {
        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(TEST_POSTCODE);

        List<PostcodeCourtMapping> mappings = List.of(
            PostcodeCourtMapping.builder()
                .postcode(TEST_POSTCODE)
                .legislativeCountry(ENGLAND.getLabel())
                .mappingEffectiveFrom(TEST_DATE.minusWeeks(2))
                .mappingEffectiveTo(TEST_DATE.plusDays(1))
                .build(),
            PostcodeCourtMapping.builder()
                .postcode(TEST_POSTCODE)
                .mappingEffectiveFrom(TEST_DATE.minusDays(1))
                .legislativeCountry(ENGLAND.getLabel())
                .build()
        );

        when(postCodeCourtRepository.findByPostCodeIn(expectedPartialPostcodes)).thenReturn(mappings);

        EligibilityResult eligibilityResult = underTest.checkEligibility(TEST_POSTCODE, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.MULTIPLE_MATCHES_FOUND);
    }

    @Test
    void shouldOnlyCheckMappingsForMostSpecificPostcodeMatch() {
        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(TEST_POSTCODE);

        int expectedEpimsId = 1234;
        List<PostcodeCourtMapping> mappings = List.of(
            PostcodeCourtMapping.builder()
                .postcode(TEST_POSTCODE)
                .mappingEffectiveFrom(TEST_DATE)
                .legislativeCountry(ENGLAND.getLabel())
                .courtEligibleFrom(TEST_DATE)
                .epimsId(expectedEpimsId)
                .build(),
            PostcodeCourtMapping.builder()
                .postcode(removeLastCharacter(TEST_POSTCODE))
                .mappingEffectiveFrom(TEST_DATE)
                .legislativeCountry(ENGLAND.getLabel())
                .build()
        );

        when(postCodeCourtRepository.findByPostCodeIn(expectedPartialPostcodes)).thenReturn(mappings);

        EligibilityResult eligibilityResult = underTest.checkEligibility(TEST_POSTCODE, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(expectedEpimsId);
    }

    @Test
    void shouldIndicateWhenLegislativeCountryIsNeeded() {
        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(TEST_POSTCODE);

        List<PostcodeCourtMapping> mappings = List.of(
            PostcodeCourtMapping.builder()
                .postcode(TEST_POSTCODE)
                .legislativeCountry(WALES.getLabel())
                .courtEligibleFrom(TEST_DATE)
                .build(),
            PostcodeCourtMapping.builder()
                .postcode(TEST_POSTCODE)
                .legislativeCountry(ENGLAND.getLabel())
                .courtEligibleFrom(TEST_DATE)
                .build(),
            PostcodeCourtMapping.builder()
                .postcode(TEST_POSTCODE)
                .legislativeCountry(ENGLAND.getLabel())
                .courtEligibleFrom(TEST_DATE)
                .build()
        );

        when(postCodeCourtRepository.findByPostCodeIn(expectedPartialPostcodes)).thenReturn(mappings);

        EligibilityResult eligibilityResult = underTest.checkEligibility(TEST_POSTCODE, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED);
        assertThat(eligibilityResult.getLegislativeCountries()).containsExactly(ENGLAND, WALES);
    }

    @Test
    void shouldOnlyCheckLegislativeCountryForMostSpecificPostcodeMatch() {
        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(TEST_POSTCODE);

        int expectedEpimsId = 1234;
        List<PostcodeCourtMapping> mappings = List.of(
            PostcodeCourtMapping.builder()
                .postcode(TEST_POSTCODE)
                .mappingEffectiveFrom(TEST_DATE)
                .legislativeCountry(ENGLAND.getLabel())
                .courtEligibleFrom(TEST_DATE)
                .epimsId(expectedEpimsId)
                .build(),
            PostcodeCourtMapping.builder()
                .postcode(removeLastCharacter(TEST_POSTCODE))
                .mappingEffectiveFrom(TEST_DATE)
                .legislativeCountry(WALES.getLabel())
                .courtEligibleFrom(TEST_DATE)
                .build()
        );

        when(postCodeCourtRepository.findByPostCodeIn(expectedPartialPostcodes)).thenReturn(mappings);

        EligibilityResult eligibilityResult = underTest.checkEligibility(TEST_POSTCODE, null);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(expectedEpimsId);
    }

    @Test
    void shouldSearchWithLegislativeCountryWhenProvided() {
        List<String> expectedPartialPostcodes = stubPartialPostcodesGenerator(TEST_POSTCODE);

        int expectedEpimsId = 5678;
        List<PostcodeCourtMapping> mappings = List.of(
            PostcodeCourtMapping.builder()
                .postcode(TEST_POSTCODE)
                .mappingEffectiveFrom(TEST_DATE)
                .legislativeCountry(WALES.getLabel())
                .courtEligibleFrom(TEST_DATE)
                .epimsId(expectedEpimsId)
                .build()
        );

        when(postCodeCourtRepository.findByPostCodeIn(expectedPartialPostcodes, WALES)).thenReturn(mappings);

        EligibilityResult eligibilityResult = underTest.checkEligibility(TEST_POSTCODE, WALES);

        assertThat(eligibilityResult.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(eligibilityResult.getEpimsId()).isEqualTo(expectedEpimsId);
    }

    @SuppressWarnings("SameParameterValue")
    private List<String> stubPartialPostcodesGenerator(String postCode) {
        List<String> expectedPartialPostcodes = List.of("A", "B", "C");
        when(partialPostcodesGenerator.generateForPostcode(postCode)).thenReturn(expectedPartialPostcodes);
        return expectedPartialPostcodes;
    }

    @SuppressWarnings("SameParameterValue")
    private static String removeLastCharacter(String value) {
        return StringUtils.chop(value);
    }

}
