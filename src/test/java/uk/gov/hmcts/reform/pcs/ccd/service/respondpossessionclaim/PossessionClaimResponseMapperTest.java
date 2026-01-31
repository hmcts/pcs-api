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
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
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

        TenancyLicenceEntity tenancy = TenancyLicenceEntity.builder()
            .type(CombinedLicenceType.ASSURED_TENANCY)
            .startDate(LocalDate.of(2020, 1, 1))
            .rentPerDay(new BigDecimal("50.00"))
            .build();

        RentArrearsEntity rentArrears = RentArrearsEntity.builder()
            .totalRentArrears(new BigDecimal("1000.00"))
            .build();

        NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
            .noticeServed(YesOrNo.YES)
            .noticeDateTime(LocalDate.of(2024, 6, 1).atStartOfDay())
            .build();

        claimEntity.setRentArrears(rentArrears);
        claimEntity.setNoticeOfPossession(notice);

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
        assertThat(claimantProvided.getClaimantOrg()).isEqualTo("Housing Association Ltd");
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
    void shouldMapTenancyTypeWhenCaseIsInEngland() {
        // Given
        TenancyLicenceEntity tenancy = TenancyLicenceEntity.builder()
            .type(CombinedLicenceType.ASSURED_TENANCY)
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
    void shouldMapTenancyTypeWhenCaseIsInWales() {
        // Given
        TenancyLicenceEntity tenancy = TenancyLicenceEntity.builder()
            .type(CombinedLicenceType.SECURE_CONTRACT)
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
    void shouldMapTenancyStartDateWhenCaseIsInEngland() {
        // Given
        TenancyLicenceEntity tenancy = TenancyLicenceEntity.builder()
            .startDate(LocalDate.of(2020, 1, 1))
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
    void shouldMapTenancyStartDateWhenCaseIsInWales() {
        // Given
        TenancyLicenceEntity tenancy = TenancyLicenceEntity.builder()
            .startDate(LocalDate.of(2024, 3, 1))
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
    void shouldMapNoticeServedWhenCaseIsInEngland() {
        // Given
        NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
            .noticeServed(YesOrNo.YES)
            .build();

        ClaimEntity claim = createMinimalClaim();
        claim.setNoticeOfPossession(notice);

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();
        caseEntity.getClaims().add(claim);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getNoticeServed()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldMapNoticeServedWhenCaseIsInWales() {
        // Given
        NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
            .noticeServed(YesOrNo.YES)
            .build();

        ClaimEntity claim = createMinimalClaim();
        claim.setNoticeOfPossession(notice);

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.WALES)
            .build();
        caseEntity.getClaims().add(claim);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getNoticeServed()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldMapNoticeDateWhenPostedDateHasHighestPriority() {
        // Given
        NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
            .noticeDateTime(LocalDate.of(2024, 6, 1).atStartOfDay())
            .build();

        ClaimEntity claim = createMinimalClaim();
        claim.setNoticeOfPossession(notice);

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();
        caseEntity.getClaims().add(claim);

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
        NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
            .noticeDate(LocalDate.of(2024, 6, 2))
            .build();

        ClaimEntity claim = createMinimalClaim();
        claim.setNoticeOfPossession(notice);

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();
        caseEntity.getClaims().add(claim);

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
        NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
            .noticeDateTime(LocalDateTime.of(2024, 6, 3, 10, 30))
            .build();

        ClaimEntity claim = createMinimalClaim();
        claim.setNoticeOfPossession(notice);

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();
        caseEntity.getClaims().add(claim);

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
        NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
            .noticeDate(null)
            .noticeDateTime(null)
            .build();

        ClaimEntity claim = createMinimalClaim();
        claim.setNoticeOfPossession(notice);

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();
        caseEntity.getClaims().add(claim);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getNoticeDate()).isNull();
    }

    @Test
    void shouldMapDailyRentAmountWhenTenancyHasRentPerDay() {
        // Given
        TenancyLicenceEntity tenancy = TenancyLicenceEntity.builder()
            .rentPerDay(new BigDecimal("75.50"))
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
    void shouldMapRentArrearsOwedWhenClaimHasRentArrears() {
        // Given
        RentArrearsEntity rentArrears = RentArrearsEntity.builder()
            .totalRentArrears(new BigDecimal("5000.00"))
            .build();

        ClaimEntity claim = createMinimalClaim();
        claim.setRentArrears(rentArrears);

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();
        caseEntity.getClaims().add(claim);

        when(addressMapper.toAddressUK(any())).thenReturn(AddressUK.builder().build());

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, PartyEntity.builder().build());

        // Then
        assertThat(result.getClaimantProvided().getRentArrearsOwed())
            .isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void shouldExtractClaimantOrgNameWhenClaimantIsOrganisation() {
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
        assertThat(result.getClaimantProvided().getClaimantOrg()).isEqualTo("Test Landlord Ltd");
    }

    @Test
    void shouldPopulateBothPartiesWithSameValuesOnFirstLoad() {
        // Given - Defendant data in database
        UUID defendantUserId = UUID.randomUUID();

        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .addressLine2("Apartment 4B")
            .postcode("SW1A 1AA")
            .build();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .nameKnown(VerticalYesNo.YES)
            .emailAddress("john.doe@example.com")
            .phoneNumber("07700900123")
            .phoneNumberProvided(VerticalYesNo.YES)
            .address(addressEntity)
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsProperty(VerticalYesNo.NO)
            .build();

        PartyEntity claimantEntity = PartyEntity.builder()
            .orgName("Test Housing Association Ltd")
            .build();

        ClaimEntity claimEntity = createClaimWithClaimantAndDefendant(claimantEntity, defendantEntity);

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();
        caseEntity.getClaims().add(claimEntity);

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .addressLine2("Apartment 4B")
            .postCode("SW1A 1AA")
            .build();

        when(addressMapper.toAddressUK(addressEntity)).thenReturn(expectedAddress);

        // When
        PossessionClaimResponse result = underTest.mapFrom(caseEntity, defendantEntity);

        // Then
        uk.gov.hmcts.reform.pcs.ccd.domain.Party claimantParty = result.getClaimantProvided().getParty();
        uk.gov.hmcts.reform.pcs.ccd.domain.Party defendantParty = result.getDefendantProvided()
            .getContactDetails().getParty();

        // Verify the editable fields match between both parties
        assertThat(defendantParty.getFirstName())
            .as("firstName should match on first load")
            .isEqualTo(claimantParty.getFirstName())
            .isEqualTo("John");

        assertThat(defendantParty.getLastName())
            .as("lastName should match on first load")
            .isEqualTo(claimantParty.getLastName())
            .isEqualTo("Doe");

        assertThat(defendantParty.getEmailAddress())
            .as("emailAddress should match on first load")
            .isEqualTo(claimantParty.getEmailAddress())
            .isEqualTo("john.doe@example.com");

        assertThat(defendantParty.getPhoneNumber())
            .as("phoneNumber should match on first load")
            .isEqualTo(claimantParty.getPhoneNumber())
            .isEqualTo("07700900123");

        assertThat(defendantParty.getPhoneNumberProvided())
            .as("phoneNumberProvided should match on first load")
            .isEqualTo(claimantParty.getPhoneNumberProvided())
            .isEqualTo(VerticalYesNo.YES);

        assertThat(defendantParty.getAddress())
            .as("address should match on first load")
            .isEqualTo(claimantParty.getAddress())
            .isEqualTo(expectedAddress);

        // Additional verification: claimantProvided has routing fields and claimant org
        assertThat(result.getClaimantProvided().getClaimantOrg()).isEqualTo("Test Housing Association Ltd");
        assertThat(claimantParty.getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(claimantParty.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(claimantParty.getAddressSameAsProperty()).isEqualTo(VerticalYesNo.NO);
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

    private PcsCaseEntity createCaseWithTenancy(TenancyLicenceEntity tenancy) {
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
