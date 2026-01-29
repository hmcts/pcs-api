package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimantProvidedInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantProvided;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PossessionClaimResponseMapperTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private AddressMapper addressMapper;

    private PossessionClaimResponseMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new PossessionClaimResponseMapper(addressMapper);
    }

    @Test
    void shouldMapCompleteResponseWithAllFields() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .postcode("SW1A 1AA")
            .build();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .emailAddress("john@example.com")
            .phoneNumber("07700900000")
            .address(addressEntity)
            .phoneNumberProvided(VerticalYesNo.YES)
            .build();

        PartyEntity claimantEntity = PartyEntity.builder()
            .orgName("Housing Association Ltd")
            .build();

        ClaimEntity claimEntity = createClaimWithClaimantAndDefendant(claimantEntity, defendantEntity);

        TenancyLicence tenancy = TenancyLicence.builder()
            .tenancyLicenceType("Assured tenancy")
            .tenancyLicenceDate(LocalDate.of(2020, 1, 1))
            .dailyRentChargeAmount(new BigDecimal("50.00"))
            .totalRentArrears(new BigDecimal("1000.00"))
            .noticeServed(true)
            .noticePostedDate(LocalDate.of(2024, 6, 1))
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .tenancyLicence(tenancy)
            .build();
        caseEntity.getClaims().add(claimEntity);

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postCode("SW1A 1AA")
            .build();

        when(addressMapper.toAddressUK(addressEntity)).thenReturn(expectedAddress);

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, defendantEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getClaimantProvided()).isNotNull();
        assertThat(result.getDefendantProvided()).isNotNull();

        // Verify claimant provided data
        ClaimantProvidedInfo claimantProvided = result.getClaimantProvided();
        assertThat(claimantProvided.getParty().getFirstName()).isEqualTo("John");
        assertThat(claimantProvided.getParty().getLastName()).isEqualTo("Doe");
        assertThat(claimantProvided.getParty().getOrgName()).isEqualTo("Housing Association Ltd");
        assertThat(claimantProvided.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
        assertThat(claimantProvided.getTenancyType()).isEqualTo("Assured tenancy");
        assertThat(claimantProvided.getTenancyStartDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(claimantProvided.getDailyRentAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(claimantProvided.getRentArrearsOwed()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(claimantProvided.getNoticeServed()).isEqualTo(YesOrNo.YES);
        assertThat(claimantProvided.getNoticeDate()).isEqualTo(LocalDate.of(2024, 6, 1).atStartOfDay());

        // Verify defendant provided data
        DefendantProvided defendantProvided = result.getDefendantProvided();
        assertThat(defendantProvided.getContactDetails().getParty().getFirstName()).isEqualTo("John");
        assertThat(defendantProvided.getContactDetails().getParty().getLastName()).isEqualTo("Doe");
        assertThat(defendantProvided.getContactDetails().getContactByPhone()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldUsePropertyAddressWhenAddressSameAsPropertyIsYes() {
        // Given
        PartyEntity defendantEntity = PartyEntity.builder()
            .addressSameAsProperty(VerticalYesNo.YES)
            .address(null)
            .build();

        AddressEntity propertyAddress = AddressEntity.builder()
            .addressLine1("Property Street")
            .postcode("M1 1AA")
            .build();

        PcsCaseEntity caseEntity = createMinimalCaseEntity();
        caseEntity.setPropertyAddress(propertyAddress);

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("Property Street")
            .postCode("M1 1AA")
            .build();

        when(addressMapper.toAddressUK(propertyAddress)).thenReturn(expectedAddress);

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, defendantEntity);

        // Then
        verify(addressMapper).toAddressUK(propertyAddress);
        assertThat(result.getClaimantProvided().getParty().getAddress()).isEqualTo(expectedAddress);
    }

    @Test
    void shouldUseDefendantAddressWhenAddressSameAsPropertyIsNo() {
        // Given
        AddressEntity defendantAddress = AddressEntity.builder()
            .addressLine1("Defendant Street")
            .postcode("B1 1AA")
            .build();

        PartyEntity defendantEntity = PartyEntity.builder()
            .addressSameAsProperty(VerticalYesNo.NO)
            .address(defendantAddress)
            .build();

        PcsCaseEntity caseEntity = createMinimalCaseEntity();

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("Defendant Street")
            .postCode("B1 1AA")
            .build();

        when(addressMapper.toAddressUK(defendantAddress)).thenReturn(expectedAddress);

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, defendantEntity);

        // Then
        verify(addressMapper).toAddressUK(defendantAddress);
        assertThat(result.getClaimantProvided().getParty().getAddress()).isEqualTo(expectedAddress);
    }

    @Test
    void shouldCreatePartyWithNullFieldsWhenDefendantDataIsNull() {
        // Given
        PartyEntity defendantEntity = PartyEntity.builder()
            .firstName(null)
            .lastName(null)
            .emailAddress(null)
            .phoneNumber(null)
            .address(null)
            .build();

        PcsCaseEntity caseEntity = createMinimalCaseEntity();

        when(addressMapper.toAddressUK(null)).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, defendantEntity);

        // Then
        assertThat(result.getClaimantProvided().getParty()).isNotNull();
        assertThat(result.getDefendantProvided().getContactDetails().getParty()).isNotNull();
    }

    @Test
    void shouldMapTenancyTypeForEnglandCase() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .tenancyLicenceType("Assured tenancy")
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .tenancyLicence(tenancy)
            .build();
        caseEntity.getClaims().add(createMinimalClaim());

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getTenancyType()).isEqualTo("Assured tenancy");
    }

    @Test
    void shouldMapTenancyTypeForWalesCase() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.SECURE_CONTRACT)
            .tenancyLicenceType("Should not use this")
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.WALES)
            .tenancyLicence(tenancy)
            .build();
        caseEntity.getClaims().add(createMinimalClaim());

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getTenancyType()).isEqualTo("Secure contract");
    }

    @Test
    void shouldMapTenancyStartDateForEnglandCase() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .tenancyLicenceDate(LocalDate.of(2020, 1, 1))
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .tenancyLicence(tenancy)
            .build();
        caseEntity.getClaims().add(createMinimalClaim());

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getTenancyStartDate()).isEqualTo(LocalDate.of(2020, 1, 1));
    }

    @Test
    void shouldMapTenancyStartDateForWalesCase() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .walesLicenceStartDate(LocalDate.of(2024, 3, 1))
            .tenancyLicenceDate(LocalDate.of(2020, 1, 1))
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.WALES)
            .tenancyLicence(tenancy)
            .build();
        caseEntity.getClaims().add(createMinimalClaim());

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getTenancyStartDate()).isEqualTo(LocalDate.of(2024, 3, 1));
    }

    @Test
    void shouldMapNoticeServedForEnglandCase() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .noticeServed(true)
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .tenancyLicence(tenancy)
            .build();
        caseEntity.getClaims().add(createMinimalClaim());

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getNoticeServed()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldMapNoticeServedForWalesCase() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .walesNoticeServed(true)
            .noticeServed(false)
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.WALES)
            .tenancyLicence(tenancy)
            .build();
        caseEntity.getClaims().add(createMinimalClaim());

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getNoticeServed()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldMapNoticeDateWithPostedDateAsHighestPriority() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .noticePostedDate(LocalDate.of(2024, 6, 1))
            .noticeDeliveredDate(LocalDate.of(2024, 6, 2))
            .build();

        PcsCaseEntity caseEntity = createCaseWithTenancy(tenancy);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getNoticeDate())
            .isEqualTo(LocalDate.of(2024, 6, 1).atStartOfDay());
    }

    @Test
    void shouldMapNoticeDateWithDeliveredDateWhenPostedDateIsNull() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .noticePostedDate(null)
            .noticeDeliveredDate(LocalDate.of(2024, 6, 2))
            .build();

        PcsCaseEntity caseEntity = createCaseWithTenancy(tenancy);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getNoticeDate())
            .isEqualTo(LocalDate.of(2024, 6, 2).atStartOfDay());
    }

    @Test
    void shouldMapNoticeDateWithHandedOverDateTimeWhenHigherPrioritiesNull() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .noticePostedDate(null)
            .noticeDeliveredDate(null)
            .noticeHandedOverDateTime(LocalDateTime.of(2024, 6, 3, 10, 30))
            .build();

        PcsCaseEntity caseEntity = createCaseWithTenancy(tenancy);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getNoticeDate())
            .isEqualTo(LocalDateTime.of(2024, 6, 3, 10, 30));
    }

    @Test
    void shouldReturnNullNoticeDateWhenAllDatesAreNull() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .noticePostedDate(null)
            .noticeDeliveredDate(null)
            .noticeHandedOverDateTime(null)
            .noticeEmailSentDateTime(null)
            .noticeOtherElectronicDateTime(null)
            .noticeOtherDateTime(null)
            .build();

        PcsCaseEntity caseEntity = createCaseWithTenancy(tenancy);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getNoticeDate()).isNull();
    }

    @Test
    void shouldMapContactByPhoneWhenPhoneNumberProvidedIsYes() {
        // Given
        PartyEntity defendantEntity = PartyEntity.builder()
            .phoneNumberProvided(VerticalYesNo.YES)
            .phoneNumber("07700900000")
            .build();

        PcsCaseEntity caseEntity = createMinimalCaseEntity();

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, defendantEntity);

        // Then
        assertThat(result.getDefendantProvided().getContactDetails().getContactByPhone())
            .isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldMapContactByPhoneWhenPhoneNumberProvidedIsNo() {
        // Given
        PartyEntity defendantEntity = PartyEntity.builder()
            .phoneNumberProvided(VerticalYesNo.NO)
            .build();

        PcsCaseEntity caseEntity = createMinimalCaseEntity();

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, defendantEntity);

        // Then
        assertThat(result.getDefendantProvided().getContactDetails().getContactByPhone())
            .isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldMapContactByPhoneAsNullWhenPhoneNumberProvidedIsNull() {
        // Given
        PartyEntity defendantEntity = PartyEntity.builder()
            .phoneNumberProvided(null)
            .build();

        PcsCaseEntity caseEntity = createMinimalCaseEntity();

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, defendantEntity);

        // Then
        assertThat(result.getDefendantProvided().getContactDetails().getContactByPhone()).isNull();
    }

    @Test
    void shouldMapDailyRentAmount() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .dailyRentChargeAmount(new BigDecimal("75.50"))
            .build();

        PcsCaseEntity caseEntity = createCaseWithTenancy(tenancy);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getDailyRentAmount())
            .isEqualByComparingTo(new BigDecimal("75.50"));
    }

    @Test
    void shouldMapRentArrearsOwed() {
        // Given
        TenancyLicence tenancy = TenancyLicence.builder()
            .totalRentArrears(new BigDecimal("5000.00"))
            .build();

        PcsCaseEntity caseEntity = createCaseWithTenancy(tenancy);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getRentArrearsOwed())
            .isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void shouldExtractClaimantOrgName() {
        // Given
        PartyEntity claimantEntity = PartyEntity.builder()
            .orgName("Test Landlord Ltd")
            .build();

        ClaimEntity claimEntity = ClaimEntity.builder().build();
        ClaimPartyEntity claimantParty = ClaimPartyEntity.builder()
            .party(claimantEntity)
            .role(PartyRole.CLAIMANT)
            .build();
        claimEntity.getClaimParties().add(claimantParty);

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();
        caseEntity.getClaims().add(claimEntity);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getParty().getOrgName()).isEqualTo("Test Landlord Ltd");
    }

    private ClaimEntity createMinimalClaim() {
        PartyEntity claimant = PartyEntity.builder()
            .orgName("Test Org")
            .build();
        ClaimEntity claimEntity = ClaimEntity.builder().build();
        ClaimPartyEntity claimantParty = ClaimPartyEntity.builder()
            .party(claimant)
            .role(PartyRole.CLAIMANT)
            .build();
        claimEntity.getClaimParties().add(claimantParty);
        return claimEntity;
    }

    private PcsCaseEntity createMinimalCaseEntity() {
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();
        caseEntity.getClaims().add(createMinimalClaim());
        return caseEntity;
    }

    private PcsCaseEntity createCaseWithTenancy(TenancyLicence tenancy) {
        PcsCaseEntity caseEntity = createMinimalCaseEntity();
        caseEntity.setTenancyLicence(tenancy);
        return caseEntity;
    }

    private ClaimEntity createClaimWithClaimantAndDefendant(PartyEntity claimant, PartyEntity defendant) {
        ClaimEntity claimEntity = ClaimEntity.builder().build();

        ClaimPartyEntity claimantParty = ClaimPartyEntity.builder()
            .party(claimant)
            .role(PartyRole.CLAIMANT)
            .build();

        ClaimPartyEntity defendantParty = ClaimPartyEntity.builder()
            .party(defendant)
            .role(PartyRole.DEFENDANT)
            .build();

        claimEntity.getClaimParties().add(claimantParty);
        claimEntity.getClaimParties().add(defendantParty);
        return claimEntity;
    }
}
