package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.MapperConfig;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PcsCaseServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Autowired
    private ModelMapper modelMapper;

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private SecurityContextService securityContextService;
    @Captor
    private ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor;

    private PcsCaseService underTest;

    @BeforeEach
    void setUp() {
        MapperConfig config = new MapperConfig();
        modelMapper = spy(config.modelMapper());
        underTest = new PcsCaseService(pcsCaseRepository, securityContextService, modelMapper);
    }

    @Test
    void shouldCreateCaseWithNoData() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        // When
        underTest.createCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getCaseManagementLocation()).isNull();
        assertThat(savedEntity.getPropertyAddress()).isNull();
        assertThat(savedEntity.getPreActionProtocolCompleted()).isNull();
    }

    @Test
    void shouldCreateCaseWithData() {
        // Given
        VerticalYesNo preActionProtocolCompleted = VerticalYesNo.YES;

        PCSCase pcsCase = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);
        final AddressEntity propertyAddressEntity = stubAddressUKModelMapper(propertyAddress);

        when(pcsCase.getPropertyAddress()).thenReturn(propertyAddress);
        when(pcsCase.getPreActionProtocolCompleted()).thenReturn(preActionProtocolCompleted);

        // When
        underTest.createCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getPropertyAddress()).isEqualTo(propertyAddressEntity);
        assertThat(savedEntity.getPreActionProtocolCompleted()).isEqualTo(preActionProtocolCompleted.toBoolean());
    }

    @Test
    void shouldThrowExceptionPatchingUnknownCaseReference() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> underTest.patchCase(CASE_REFERENCE, pcsCase));

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessage("No case found with reference %s", CASE_REFERENCE);
    }

    @Test
    void shouldLeaveFieldsUnchangedWhenPatchingCaseWithNoData() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity existingPcsCaseEntity = mock(PcsCaseEntity.class);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));
        when(pcsCase.getCaseManagementLocation()).thenReturn(null);

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity).isSameAs(existingPcsCaseEntity);
        verifyNoInteractions(existingPcsCaseEntity);
    }

    @Test
    void shouldChangeFieldsWhenPatchingCase() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);

        AddressUK updatedPropertyAddress = mock(AddressUK.class);
        final AddressEntity updatedAddressEntity = stubAddressUKModelMapper(updatedPropertyAddress);

        PcsCaseEntity existingPcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCase.getPropertyAddress()).thenReturn(updatedPropertyAddress);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity).isSameAs(existingPcsCaseEntity);
        verify(existingPcsCaseEntity).setPropertyAddress(updatedAddressEntity);
    }

    @Test
    void shouldCreatePartyWithPcqIdForCurrentUser() {
        UUID userId = UUID.randomUUID();
        UUID expectedPcqId = UUID.randomUUID();
        String expectedForename = "some forename";
        String expectedSurname = "some surname";

        PCSCase pcsCase = mock(PCSCase.class);
        when(pcsCase.getUserPcqId()).thenReturn(expectedPcqId.toString());

        UserInfo userDetails = mock(UserInfo.class);
        when(userDetails.getUid()).thenReturn(userId.toString());
        when(userDetails.getGivenName()).thenReturn(expectedForename);
        when(userDetails.getFamilyName()).thenReturn(expectedSurname);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);

        PcsCaseEntity existingPcsCaseEntity = new PcsCaseEntity();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getParties())
            .hasSize(1)
            .allSatisfy(party -> {
                assertThat(party.getIdamId()).isEqualTo(userId);
                assertThat(party.getPcqId()).isEqualTo(expectedPcqId);
                assertThat(party.getForename()).isEqualTo(expectedForename);
                assertThat(party.getSurname()).isEqualTo(expectedSurname);
            });

    }

    @Test
    void shouldUpdateExistingPartyWithPcqIdForCurrentUser() {
        final UUID userId = UUID.randomUUID();
        final UUID expectedPcqId = UUID.randomUUID();
        final UUID existingPartyId = UUID.randomUUID();

        PCSCase pcsCase = mock(PCSCase.class);
        when(pcsCase.getUserPcqId()).thenReturn(expectedPcqId.toString());

        UserInfo userDetails = mock(UserInfo.class);
        when(userDetails.getUid()).thenReturn(userId.toString());
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);

        PcsCaseEntity existingPcsCaseEntity = new PcsCaseEntity();
        PartyEntity existingPartyEntity = new PartyEntity();
        existingPartyEntity.setId(existingPartyId);
        existingPartyEntity.setIdamId(userId);
        existingPcsCaseEntity.addParty(existingPartyEntity);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getParties())
            .hasSize(1)
            .allSatisfy(
                party -> {
                    assertThat(party.getId()).isEqualTo(existingPartyId);
                    assertThat(party.getPcqId()).isEqualTo(expectedPcqId);
                }
            );

    }

    @Test
    void shouldUpdatePaymentStatusWhenNotNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity existingPcsCaseEntity = mock(PcsCaseEntity.class);
        PaymentStatus paymentStatus = PaymentStatus.PAID;

        when(pcsCase.getPaymentStatus()).thenReturn(paymentStatus);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        verify(existingPcsCaseEntity).setPaymentStatus(paymentStatus);
        assertThat(pcsCaseEntityCaptor.getValue()).isSameAs(existingPcsCaseEntity);
    }

    @Test
    void shouldUpdateCaseManagementLocationWhenNotNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity existingPcsCaseEntity = mock(PcsCaseEntity.class);
        Integer location = 13685;

        when(pcsCase.getCaseManagementLocation()).thenReturn(location);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        verify(existingPcsCaseEntity).setCaseManagementLocation(location);
        assertThat(pcsCaseEntityCaptor.getValue()).isSameAs(existingPcsCaseEntity);
    }

    @Test
    void shouldUpdatePreActionProtocolCompletedWhenNotNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        PcsCaseEntity existingPcsCaseEntity = mock(PcsCaseEntity.class);
        VerticalYesNo preActionProtocolCompleted = VerticalYesNo.YES;

        when(pcsCase.getPreActionProtocolCompleted()).thenReturn(preActionProtocolCompleted);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(existingPcsCaseEntity));

        // When
        underTest.patchCase(CASE_REFERENCE, pcsCase);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        verify(existingPcsCaseEntity).setPreActionProtocolCompleted(preActionProtocolCompleted.toBoolean());
        assertThat(pcsCaseEntityCaptor.getValue()).isSameAs(existingPcsCaseEntity);
    }

    @Test
    void shouldMapFromDefendantDetailsToDefendantPojo() {
        // Given
        AddressUK correspondenceAddress = AddressUK.builder()
            .addressLine1("125 Broadway")
            .postCode("W5 8DG")
            .build();
        DefendantDetails details =  DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("John")
            .lastName("Doe")
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsPossession(VerticalYesNo.YES)
            .correspondenceAddress(correspondenceAddress)
            .emailKnown(VerticalYesNo.NO)
            .build();

        ListValue<DefendantDetails> listValue = new ListValue<>("123", details);

        // When
        List<Defendant> result = underTest.mapFromDefendantDetails(List.of(listValue));

        // Then
        assertThat(result).hasSize(1);
        Defendant mappedDefendant = result.get(0);

        assertThat(mappedDefendant.getId()).isEqualTo("123");
        assertThat(mappedDefendant.getNameKnown()).isTrue();
        assertThat(mappedDefendant.getFirstName()).isEqualTo("John");
        assertThat(mappedDefendant.getLastName()).isEqualTo("Doe");
        assertThat(mappedDefendant.getAddressKnown()).isTrue();
        assertThat(mappedDefendant.getAddressSameAsPossession()).isTrue();
        assertThat(mappedDefendant.getCorrespondenceAddress().getAddressLine1())
            .isEqualTo(correspondenceAddress.getAddressLine1());
        assertThat(mappedDefendant.getEmailKnown()).isFalse();
    }

    @Test
    void shouldMapDefendantPojoToDefendantDetails() {
        // Given
        Defendant defendant = Defendant.builder()
            .id("456")
            .nameKnown(true)
            .firstName("Jane")
            .lastName("Smith")
            .addressKnown(false)
            .addressSameAsPossession(false)
            .emailKnown(true)
            .email("jane.smith@email.com")
            .build();

        // When
        List<ListValue<DefendantDetails>> result = underTest.mapToDefendantDetails(List.of(defendant));

        // Then
        ListValue<DefendantDetails> listValue = result.get(0);
        DefendantDetails mappedDefendantDetails = listValue.getValue();

        assertThat(result).hasSize(1);
        assertThat(listValue.getId()).isEqualTo("456");
        assertThat(mappedDefendantDetails.getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(mappedDefendantDetails.getFirstName()).isEqualTo("Jane");
        assertThat(mappedDefendantDetails.getLastName()).isEqualTo("Smith");
        assertThat(mappedDefendantDetails.getAddressKnown()).isEqualTo(VerticalYesNo.NO);
        assertThat(mappedDefendantDetails.getAddressSameAsPossession()).isEqualTo(VerticalYesNo.NO);
        assertThat(mappedDefendantDetails.getEmailKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(mappedDefendantDetails.getEmail()).isEqualTo("jane.smith@email.com");
    }

    @Test
    void shouldClearHiddenDefendantDetailsFields() {
        // Given
        DefendantDetails defendantWithHiddenFields = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.NO)
            .firstName("John")
            .lastName("Doe")
            .addressKnown(VerticalYesNo.NO)
            .correspondenceAddress(AddressUK.builder()
                                       .addressLine1("123 Test Street")
                                       .postTown("Test Town")
                                       .postCode("TE1 1ST")
                                       .build())
            .addressSameAsPossession(VerticalYesNo.NO)
            .emailKnown(VerticalYesNo.NO)
            .email("test@example.com")
            .build();

        List<ListValue<DefendantDetails>> defendantsList = List.of(
            new ListValue<>("1", defendantWithHiddenFields));

        // When
        underTest.clearHiddenDefendantDetailsFields(defendantsList);

        // Then
        DefendantDetails clearedDefendant = defendantsList.get(0).getValue();
        assertThat(clearedDefendant.getFirstName()).isNull();
        assertThat(clearedDefendant.getLastName()).isNull();
        assertThat(clearedDefendant.getCorrespondenceAddress()).isNull();
        assertThat(clearedDefendant.getAddressSameAsPossession()).isNull();
        assertThat(clearedDefendant.getEmail()).isNull();
    }

    // Test for tenancy_licence JSON creation, temporary until Data Model is finalised
    @Test
    void shouldSetTenancyLicence() {
        // Test notice_served field updates
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getNoticeServed()).thenReturn(YesOrNo.YES),
                expected -> assertThat(expected.getNoticeServed()).isTrue());
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getNoticeServed()).thenReturn(YesOrNo.NO),
                expected -> assertThat(expected.getNoticeServed()).isFalse());

        // Test rent amount field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getCurrentRent()).thenReturn("1200.50"),
                expected -> assertThat(expected.getRentAmount()).isEqualTo(new BigDecimal("1200.50")));

        // Test rent payment frequency field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getRentFrequency()).thenReturn(RentPaymentFrequency.MONTHLY),
                expected -> assertThat(expected.getRentPaymentFrequency()).isEqualTo(RentPaymentFrequency.MONTHLY));

        // Test other rent frequency field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getOtherRentFrequency()).thenReturn("Bi-weekly"),
                expected -> assertThat(expected.getOtherRentFrequency()).isEqualTo("Bi-weekly"));

        // Test daily rent charge amount field
        assertTenancyLicenceField(
                pcsCase -> when(pcsCase.getDailyRentChargeAmount()).thenReturn("40.00"),
                expected -> assertThat(expected.getDailyRentChargeAmount()).isEqualTo(new BigDecimal("40.00")));
    }

    private void assertTenancyLicenceField(java.util.function.Consumer<PCSCase> setupMock,
            java.util.function.Consumer<TenancyLicence> assertions) {
        PCSCase pcsCase = mock(PCSCase.class);
        setupMock.accept(pcsCase);
        when(pcsCaseRepository.save(pcsCaseEntityCaptor.capture())).thenReturn(null);

        underTest.createCase(CASE_REFERENCE, pcsCase);

        TenancyLicence actual = pcsCaseEntityCaptor.getValue().getTenancyLicence();
        assertions.accept(actual);

    }

    private AddressEntity stubAddressUKModelMapper(AddressUK addressUK) {
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(modelMapper.map(addressUK, AddressEntity.class)).thenReturn(addressEntity);
        return addressEntity;
    }

}
