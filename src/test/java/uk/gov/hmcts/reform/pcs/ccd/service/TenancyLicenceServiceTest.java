package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenancyLicenceServiceTest {

    @Mock(strictness = LENIENT)
    private PCSCase pcsCase;
    @Mock
    private NoticeServedDetails noticeServedDetails;

    private TenancyLicenceService underTest;

    @BeforeEach
    void setUp() {
        TenancyLicenceDetails tenancyLicenceDetails = TenancyLicenceDetails.builder()
            .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
            .build();

        when(pcsCase.getTenancyLicenceDetails()).thenReturn(tenancyLicenceDetails);

        underTest = new TenancyLicenceService();
    }

    @ParameterizedTest
    @MethodSource("noTenancyTypeScenarios")
    void shouldReturnNullWhenNoTenancyType(LegislativeCountry legislativeCountry,
                                           TenancyLicenceDetails tenancyLicenceDetails,
                                           OccupationLicenceDetailsWales occupationLicenceDetailsWales) {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(legislativeCountry);
        when(pcsCase.getTenancyLicenceDetails()).thenReturn(tenancyLicenceDetails);
        when(pcsCase.getOccupationLicenceDetailsWales()).thenReturn(occupationLicenceDetailsWales);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.createTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity).isNull();
    }


    @Test
    void shouldSetTenancyLicenceDetailsForNonWales() {
        // Given
        LocalDate tenancyLicenceDate = mock(LocalDate.class);

        TenancyLicenceDetails tenancyLicenceDetails = TenancyLicenceDetails.builder()
            .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
            .tenancyLicenceDate(tenancyLicenceDate)
            .detailsOfOtherTypeOfTenancyLicence("should be ignored")
            .build();

        when(pcsCase.getTenancyLicenceDetails()).thenReturn(tenancyLicenceDetails);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.createTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity.getType()).isEqualTo(CombinedLicenceType.ASSURED_TENANCY);
        assertThat(tenancyLicenceEntity.getOtherTypeDetails()).isNull();
        assertThat(tenancyLicenceEntity.getStartDate()).isEqualTo(tenancyLicenceDate);
    }

    @Test
    void shouldSetTenancyLicenceDetailsForOtherTypeNonWales() {
        // Given
        LocalDate tenancyLicenceDate = mock(LocalDate.class);
        String otherTenancyType = "other tenancy type";

        TenancyLicenceDetails tenancyLicenceDetails = TenancyLicenceDetails.builder()
            .typeOfTenancyLicence(TenancyLicenceType.OTHER)
            .tenancyLicenceDate(tenancyLicenceDate)
            .detailsOfOtherTypeOfTenancyLicence(otherTenancyType)
            .build();

        when(pcsCase.getTenancyLicenceDetails()).thenReturn(tenancyLicenceDetails);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.createTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity.getType()).isEqualTo(CombinedLicenceType.OTHER);
        assertThat(tenancyLicenceEntity.getOtherTypeDetails()).isEqualTo(otherTenancyType);
        assertThat(tenancyLicenceEntity.getStartDate()).isEqualTo(tenancyLicenceDate);
    }

    @Test
    void shouldSetTenancyLicenceDetailsForWales() {
        // Given
        LocalDate licenceStartDate = mock(LocalDate.class);

        OccupationLicenceDetailsWales occupationLicenceDetails = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.SECURE_CONTRACT)
            .licenceStartDate(licenceStartDate)
            .otherLicenceTypeDetails("should be ignored")
            .build();

        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.WALES);
        when(pcsCase.getOccupationLicenceDetailsWales()).thenReturn(occupationLicenceDetails);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.createTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity.getType()).isEqualTo(CombinedLicenceType.SECURE_CONTRACT);
        assertThat(tenancyLicenceEntity.getOtherTypeDetails()).isNull();
        assertThat(tenancyLicenceEntity.getStartDate()).isEqualTo(licenceStartDate);
    }

    @Test
    void shouldSetTenancyLicenceDetailsForOtherTypeWales() {
        // Given
        LocalDate licenceStartDate = mock(LocalDate.class);
        String otherTenancyType = "other tenancy type";

        OccupationLicenceDetailsWales occupationLicenceDetails = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.OTHER)
            .licenceStartDate(licenceStartDate)
            .otherLicenceTypeDetails(otherTenancyType)
            .build();

        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.WALES);
        when(pcsCase.getOccupationLicenceDetailsWales()).thenReturn(occupationLicenceDetails);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.createTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity.getType()).isEqualTo(CombinedLicenceType.OTHER);
        assertThat(tenancyLicenceEntity.getOtherTypeDetails()).isEqualTo(otherTenancyType);
        assertThat(tenancyLicenceEntity.getStartDate()).isEqualTo(licenceStartDate);
    }

    @Test
    void shouldSetRentDetailsInTenancyLicenceEntity() {
        // Given
        BigDecimal rentAmount = new BigDecimal("1.23");
        BigDecimal dailyCharge = new BigDecimal("4.56");
        RentPaymentFrequency rentFrequency = RentPaymentFrequency.FORTNIGHTLY;

        RentDetails rentDetails = RentDetails.builder()
            .currentRent(rentAmount)
            .dailyCharge(dailyCharge)
            .frequency(rentFrequency)
            .otherFrequency("should be ignored")
            .perDayCorrect(VerticalYesNo.YES)
            .build();

        when(pcsCase.getRentDetails()).thenReturn(rentDetails);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.createTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity.getRentAmount()).isEqualTo(rentAmount);
        assertThat(tenancyLicenceEntity.getRentPerDay()).isEqualTo(dailyCharge);
        assertThat(tenancyLicenceEntity.getRentFrequency()).isEqualTo(rentFrequency);
        assertThat(tenancyLicenceEntity.getOtherTypeDetails()).isNull();
        assertThat(tenancyLicenceEntity.getCalculatedDailyRentCorrect()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldSetRentFrequencyDetailsForOtherFrequency() {
        // Given
        BigDecimal rentAmount = new BigDecimal("1.23");
        BigDecimal dailyCharge = new BigDecimal("4.56");
        RentPaymentFrequency rentFrequency = RentPaymentFrequency.OTHER;
        String otherRentFrequency = "other rent frequency";

        RentDetails rentDetails = RentDetails.builder()
            .currentRent(rentAmount)
            .dailyCharge(dailyCharge)
            .frequency(rentFrequency)
            .otherFrequency(otherRentFrequency)
            .build();

        when(pcsCase.getRentDetails()).thenReturn(rentDetails);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.createTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity.getRentAmount()).isEqualTo(rentAmount);
        assertThat(tenancyLicenceEntity.getRentPerDay()).isEqualTo(dailyCharge);
        assertThat(tenancyLicenceEntity.getRentFrequency()).isEqualTo(rentFrequency);
        assertThat(tenancyLicenceEntity.getOtherRentFrequency()).isEqualTo(otherRentFrequency);
    }

    @ParameterizedTest(name = "amended={0}, calculated={1}, daily={2} -> expected={3}")
    @MethodSource("dailyRentChargeScenarios")
    void shouldPreferDailyRentCharge(BigDecimal amendedDailyRent, BigDecimal calculatedDailyRent, BigDecimal dailyRent,
                                     BigDecimal expectedAmount) {
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCase.getRentDetails()).thenReturn(RentDetails.builder()
                .amendedDailyCharge(amendedDailyRent)
                .calculatedDailyCharge(calculatedDailyRent)
                .dailyCharge(dailyRent)
                .currentRent(new BigDecimal("1200.00"))
                .frequency(RentPaymentFrequency.MONTHLY)
                .build());

        TenancyLicenceEntity result = underTest.createTenancyLicenceEntity(pcsCase);
        assertThat(result.getRentPerDay()).isEqualTo(expectedAmount);
    }

    private static Stream<Arguments> noTenancyTypeScenarios() {
        return Stream.of(
            argumentSet(
                "Non-Wales: No tenancy licence or occupation licence instances",
                LegislativeCountry.ENGLAND,
                null, // Tenancy licence
                null  // occupation licece
            ),
            argumentSet(
                "Non-Wales: No tenancy licence type or occupation licence instance",
                LegislativeCountry.ENGLAND,
                TenancyLicenceDetails.builder().build(),
                null
            ),
            argumentSet(
                "Non-Wales: No tenancy licence type or occupation licence type",
                LegislativeCountry.ENGLAND,
                TenancyLicenceDetails.builder().build(),
                OccupationLicenceDetailsWales.builder().build()
            ),
            argumentSet(
                "Wales: No tenancy licence or occupation licence instances",
                LegislativeCountry.WALES,
                null,
                null
            ),
            argumentSet(
                "Wales: No tenancy licence instance or occupation licence type",
                LegislativeCountry.WALES,
                null,
                OccupationLicenceDetailsWales.builder().build()
            ),
            argumentSet(
                "Wales: No tenancy licence type or occupation licence type",
                LegislativeCountry.WALES,
                TenancyLicenceDetails.builder().build(),
                OccupationLicenceDetailsWales.builder().build()
            )
        );
    }


    static Stream<Arguments> dailyRentChargeScenarios() {
        return Stream.of(
                Arguments.of("50.00", "40.00", "35.00", "50.00"),
                Arguments.of(null, "40.00", "35.00", "40.00"),
                Arguments.of(null, null, "35.00", "35.00")
        );
    }
}
