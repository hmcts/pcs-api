package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.ThirdPartyPaymentSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(pcsCase.getTenancyLicenceDetails()).thenReturn(TenancyLicenceDetails.builder().build());

        underTest = new TenancyLicenceService();
    }

    @Test
    void shouldSetTenancyLicenceDetails() {
        // Given
        TenancyLicenceType tenancyLicenceType = TenancyLicenceType.ASSURED_TENANCY;
        LocalDate tenancyLicenceDate = mock(LocalDate.class);

        TenancyLicenceDetails tenancyLicenceDetails = TenancyLicenceDetails.builder()
            .typeOfTenancyLicence(tenancyLicenceType)
            .tenancyLicenceDate(tenancyLicenceDate)
            .detailsOfOtherTypeOfTenancyLicence("should be ignored")
            .build();

        when(pcsCase.getTenancyLicenceDetails()).thenReturn(tenancyLicenceDetails);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.buildTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity.getType()).isEqualTo(tenancyLicenceType);
        assertThat(tenancyLicenceEntity.getOtherTypeDetails()).isNull();
        assertThat(tenancyLicenceEntity.getStartDate()).isEqualTo(tenancyLicenceDate);
    }

    @Test
    void shouldSetTenancyLicenceDetailsForOtherType() {
        // Given
        TenancyLicenceType tenancyLicenceType = TenancyLicenceType.OTHER;
        LocalDate tenancyLicenceDate = mock(LocalDate.class);
        String otherTenancyType = "other tenancy type";

        TenancyLicenceDetails tenancyLicenceDetails = TenancyLicenceDetails.builder()
            .typeOfTenancyLicence(tenancyLicenceType)
            .tenancyLicenceDate(tenancyLicenceDate)
            .detailsOfOtherTypeOfTenancyLicence(otherTenancyType)
            .build();

        when(pcsCase.getTenancyLicenceDetails()).thenReturn(tenancyLicenceDetails);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.buildTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity.getType()).isEqualTo(tenancyLicenceType);
        assertThat(tenancyLicenceEntity.getOtherTypeDetails()).isEqualTo(otherTenancyType);
        assertThat(tenancyLicenceEntity.getStartDate()).isEqualTo(tenancyLicenceDate);
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
            .build();

        when(pcsCase.getRentDetails()).thenReturn(rentDetails);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.buildTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity.getRentAmount()).isEqualTo(rentAmount);
        assertThat(tenancyLicenceEntity.getRentPerDay()).isEqualTo(dailyCharge);
        assertThat(tenancyLicenceEntity.getRentFrequency()).isEqualTo(rentFrequency);
        assertThat(tenancyLicenceEntity.getOtherTypeDetails()).isNull();
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
        TenancyLicenceEntity tenancyLicenceEntity = underTest.buildTenancyLicenceEntity(pcsCase);

        // Then
        assertThat(tenancyLicenceEntity.getRentAmount()).isEqualTo(rentAmount);
        assertThat(tenancyLicenceEntity.getRentPerDay()).isEqualTo(dailyCharge);
        assertThat(tenancyLicenceEntity.getRentFrequency()).isEqualTo(rentFrequency);
        assertThat(tenancyLicenceEntity.getOtherRentFrequency()).isEqualTo(otherRentFrequency);
    }

    @Test
    void shouldSetLegacyTenancyLicence() {
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);

        List<ListValue<Document>> uploadedDocs = Arrays.asList(
            ListValue.<Document>builder().id("1")
                .value(Document.builder()
                           .filename("tenancy_agreement.pdf")
                           .build())
                .build(),
            ListValue.<Document>builder().id("2")
                .value(Document.builder()
                           .filename("proof_of_id.png")
                           .build())
                .build()
        );

        //Test supporting documents field
        TenancyLicenceDetails tenancyDetailsWithDocs =
            TenancyLicenceDetails.builder()
                .tenancyLicenceDocuments(uploadedDocs)
                .build();
        assertTenancyLicenceField(
            pcsCase -> when(pcsCase.getTenancyLicenceDetails()).thenReturn(tenancyDetailsWithDocs),
            expected -> {
                assertThat(expected.getSupportingDocuments()).hasSize(2);
                assertThat(expected.getSupportingDocuments())
                    .extracting(Document::getFilename)
                    .containsExactlyInAnyOrder("tenancy_agreement.pdf", "proof_of_id.png");
            }
        );

        // Test rent statement documents field
        List<ListValue<Document>> rentStatementDocs = Arrays.asList(
            ListValue.<Document>builder().id("10")
                .value(Document.builder().filename("rent_statement_jan.pdf").build()).build(),
            ListValue.<Document>builder().id("11")
                .value(Document.builder().filename("rent_statement_feb.pdf").build()).build()
        );
        assertTenancyLicenceField(
            pcsCase -> when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                    .statementDocuments(rentStatementDocs)
                    .build()),
            expected -> {
                assertThat(expected.getRentStatementDocuments()).hasSize(2);
                assertThat(expected.getRentStatementDocuments())
                    .extracting(Document::getFilename)
                    .containsExactlyInAnyOrder("rent_statement_jan.pdf", "rent_statement_feb.pdf");
            }
        );

        // Test notice documents field
        List<ListValue<Document>> noticeDocs = Arrays.asList(
            ListValue.<Document>builder().id("20")
                .value(Document.builder().filename("notice_served.pdf").build()).build(),
            ListValue.<Document>builder().id("21")
                .value(Document.builder().filename("certificate_service.pdf").build()).build()
        );
        assertTenancyLicenceField(
            pcsCase -> {
                when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
                when(noticeServedDetails.getNoticeDocuments()).thenReturn(noticeDocs);
            },
            expected -> {
                assertThat(expected.getNoticeDocuments()).hasSize(2);
                assertThat(expected.getNoticeDocuments())
                    .extracting(Document::getFilename)
                    .containsExactlyInAnyOrder("notice_served.pdf", "certificate_service.pdf");
            }
        );

        // Test notice_served field updates
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getNoticeServed()).thenReturn(YesOrNo.YES),
                expected -> assertThat(expected.getNoticeServed()).isTrue());
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getNoticeServed()).thenReturn(YesOrNo.NO),
                expected -> assertThat(expected.getNoticeServed()).isFalse());

        // Test total rent arrears field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                        .total(new BigDecimal("1500.00"))
                        .build()),
                expected -> assertThat(expected.getTotalRentArrears())
                        .isEqualTo(new BigDecimal("1500.00")));

        // Test third party payment sources field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                        .thirdPartyPaymentSources(Arrays.asList(ThirdPartyPaymentSource.UNIVERSAL_CREDIT,
                                ThirdPartyPaymentSource.HOUSING_BENEFIT))
                        .build()),
                expected -> {
                    assertThat(expected.getThirdPartyPaymentSources()).hasSize(2);
                    assertThat(expected.getThirdPartyPaymentSources())
                            .containsExactlyInAnyOrder(ThirdPartyPaymentSource.UNIVERSAL_CREDIT,
                                    ThirdPartyPaymentSource.HOUSING_BENEFIT);
                });

        // Test third party payment source other field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                        .paymentSourceOther("Custom payment method")
                        .build()),
                expected -> assertThat(expected.getThirdPartyPaymentSourceOther()).isEqualTo("Custom payment method"));

        // Test arrearsJudgmentWanted field updates
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getArrearsJudgmentWanted()).thenReturn(VerticalYesNo.YES),
                expected -> assertThat(expected.getArrearsJudgmentWanted()).isTrue());
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getArrearsJudgmentWanted()).thenReturn(VerticalYesNo.NO),
                expected -> assertThat(expected.getArrearsJudgmentWanted()).isFalse());
    }

    private void assertTenancyLicenceField(Consumer<PCSCase> setupMock,
            Consumer<TenancyLicence> assertions) {
        setupMock.accept(pcsCase);
        TenancyLicence actual = underTest.buildTenancyLicence(pcsCase);
        assertions.accept(actual);
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

        TenancyLicenceEntity result = underTest.buildTenancyLicenceEntity(pcsCase);
        assertThat(result.getRentPerDay()).isEqualTo(expectedAmount);
    }

    @Test
    void shouldHandleNullTotalRentArrears() {
        // Given
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                .total(null)
                .build());
        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getTotalRentArrears()).isNull();
    }

    @Test
    void shouldHandleEmptyThirdPartyPaymentSources() {
        // Given
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                .thirdPartyPaymentSources(Collections.emptyList())
                .build());
        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getThirdPartyPaymentSources()).isEmpty();
    }

    @Test
    void shouldHandleNullThirdPartyPaymentSources() {
        // Given
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                .thirdPartyPaymentSources(null)
                .build());
        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getThirdPartyPaymentSources()).isNull();
    }

    @Test
    void shouldHandleNullThirdPartyPaymentSourceOther() {
        // Given
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                .paymentSourceOther(null)
                .build());
        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getThirdPartyPaymentSourceOther()).isNull();
    }

    @Test
    void shouldHandleEmptyThirdPartyPaymentSourceOther() {
        // Given
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                .paymentSourceOther("")
                .build());
        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getThirdPartyPaymentSourceOther()).isEqualTo("");
    }

    @Test
    void shouldHandleNullRentStatementDocuments() {
        // Given
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                .statementDocuments(null)
                .build());
        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getRentStatementDocuments()).isEmpty();
    }

    @Test
    void shouldHandleEmptyRentStatementDocuments() {
        // Given
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCase.getRentArrears()).thenReturn(RentArrearsSection.builder()
                .statementDocuments(Collections.emptyList())
                .build());
        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getRentStatementDocuments()).isEmpty();
    }

    @Test
    void shouldHandleNullNoticeDocuments() {
        // Given
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(noticeServedDetails.getNoticeDocuments()).thenReturn(null);
        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getNoticeDocuments()).isEmpty();
    }

    @Test
    void shouldHandleEmptyNoticeDocuments() {
        // Given
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(noticeServedDetails.getNoticeDocuments()).thenReturn(Collections.emptyList());
        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);
        // Then
        assertThat(result.getNoticeDocuments()).isEmpty();
    }

    @Test
    void shouldMapWalesNoticeFieldsWhenPresent() {
        // Given
        String typeOfNoticeServed = "Some notice type";

        WalesNoticeDetails walesNoticeDetails = WalesNoticeDetails.builder()
            .noticeServed(YesOrNo.YES)
            .typeOfNoticeServed(typeOfNoticeServed)
            .build();
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCase.getWalesNoticeDetails()).thenReturn(walesNoticeDetails);

        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);

        // Then
        assertThat(result.getWalesNoticeServed()).isTrue();
        assertThat(result.getWalesTypeOfNoticeServed()).isEqualTo(typeOfNoticeServed);
    }

    @Test
    void shouldMapWalesOccupationContractDetailsToTenancyLicence() {
        // Given - Wales case with occupation contract details
        LocalDate licenseStartDate = LocalDate.of(2024, 1, 15);
        List<ListValue<Document>> walesDocuments = Arrays.asList(
            ListValue.<Document>builder().id("1")
                .value(Document.builder()
                    .filename("occupation_contract.pdf")
                    .url("http://doc.com/occupation_contract.pdf")
                    .build())
                .build(),
            ListValue.<Document>builder().id("2")
                .value(Document.builder()
                    .filename("additional_doc.pdf")
                    .url("http://doc.com/additional_doc.pdf")
                    .build())
                .build()
        );

        OccupationLicenceDetailsWales walesDetails = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.SECURE_CONTRACT)
            .otherLicenceTypeDetails("Custom contract details")
            .licenceStartDate(licenseStartDate)
            .licenceDocuments(walesDocuments)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(walesDetails)
            .noticeServedDetails(noticeServedDetails)
            .build();

        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);

        // Then - Wales fields populated
        assertThat(result.getOccupationLicenceTypeWales()).isEqualTo(OccupationLicenceTypeWales.SECURE_CONTRACT);
        assertThat(result.getWalesOtherLicenceTypeDetails()).isEqualTo("Custom contract details");
        assertThat(result.getWalesLicenceStartDate()).isEqualTo(licenseStartDate);
        assertThat(result.getWalesLicenceDocuments()).hasSize(2);
        assertThat(result.getWalesLicenceDocuments().get(0).getFilename()).isEqualTo("occupation_contract.pdf");
        assertThat(result.getWalesLicenceDocuments().get(1).getFilename()).isEqualTo("additional_doc.pdf");

        // England fields should be null or empty
        assertThat(result.getSupportingDocuments()).isEmpty();
    }

    @Test
    void shouldMapWalesOccupationContractDetailsWithStandardContract() {
        // Given - Wales case with standard contract
        LocalDate licenseStartDate = LocalDate.of(2023, 6, 1);

        OccupationLicenceDetailsWales walesDetails = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.STANDARD_CONTRACT)
            .licenceStartDate(licenseStartDate)
            .licenceDocuments(null)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(walesDetails)
            .noticeServedDetails(noticeServedDetails)
            .build();

        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);

        // Then
        assertThat(result.getOccupationLicenceTypeWales()).isEqualTo(OccupationLicenceTypeWales.STANDARD_CONTRACT);
        assertThat(result.getWalesLicenceStartDate()).isEqualTo(licenseStartDate);
        assertThat(result.getWalesOtherLicenceTypeDetails()).isNull();
        assertThat(result.getWalesLicenceDocuments()).isEmpty();
    }

    @Test
    void shouldHandleNullOccupationLicenceDetailsWales() {
        // Given - Case with no Wales occupation contract details
        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(null)
            .noticeServedDetails(noticeServedDetails)
            .build();

        // When
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);

        // Then - Wales fields should be null
        assertThat(result.getOccupationLicenceTypeWales()).isNull();
        assertThat(result.getWalesOtherLicenceTypeDetails()).isNull();
        assertThat(result.getWalesLicenceStartDate()).isNull();
        assertThat(result.getWalesLicenceDocuments()).isNull();
    }

    @Test
    void shouldMapEnglandTenancyDetailsWithoutWalesData() {
        // Given - England case (no Wales data)
        LocalDate tenancyDate = LocalDate.of(2024, 2, 20);
        List<ListValue<Document>> englandDocs = List.of(
            ListValue.<Document>builder().id("1")
                .value(Document.builder()
                    .filename("tenancy_agreement.pdf")
                    .url("http://doc.com/tenancy_agreement.pdf")
                    .build())
                .build()
        );

        TenancyLicenceDetails tenancyDetails =
            TenancyLicenceDetails.builder()
                .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                .tenancyLicenceDate(tenancyDate)
                .tenancyLicenceDocuments(englandDocs)
                .build();

        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(pcsCase.getTenancyLicenceDetails()).thenReturn(tenancyDetails);
        when(pcsCase.getOccupationLicenceDetailsWales()).thenReturn(null);

        // When
        TenancyLicenceEntity tenancyLicenceEntity = underTest.buildTenancyLicenceEntity(pcsCase);
        TenancyLicence result = underTest.buildTenancyLicence(pcsCase);

        // Then - England fields populated
        assertThat(tenancyLicenceEntity.getType()).isEqualTo(TenancyLicenceType.ASSURED_TENANCY);
        assertThat(tenancyLicenceEntity.getStartDate()).isEqualTo(tenancyDate);
        assertThat(result.getSupportingDocuments()).hasSize(1);

        // Wales occupation contract fields should be null
        assertThat(result.getOccupationLicenceTypeWales()).isNull();
        assertThat(result.getWalesLicenceStartDate()).isNull();
        assertThat(result.getWalesLicenceDocuments()).isNull();
    }

    static Stream<Arguments> dailyRentChargeScenarios() {
        return Stream.of(
                Arguments.of("50.00", "40.00", "35.00", "50.00"),
                Arguments.of(null, "40.00", "35.00", "40.00"),
                Arguments.of(null, null, "35.00", "35.00")
        );
    }
}
