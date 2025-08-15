package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PcsCaseServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private ModelMapper modelMapper;
    @Captor
    private ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor;

    private PcsCaseService underTest;

    @BeforeEach
    void setUp() {
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

    // Test for tenancy_licence JSON creation, temporary until Data Model is finalised
    @Test
    void shouldSetTenancyLicenceJson() {
        // Test notice_served field updates (converts YesOrNo enum to boolean)
        assertTenancyLicenceField(pcsCase -> when(pcsCase.getNoticeServed()).thenReturn(YesOrNo.YES),
                "notice_served", true);
        assertTenancyLicenceField(pcsCase -> when(pcsCase.getNoticeServed()).thenReturn(YesOrNo.NO),
                "notice_served", false);
        assertTenancyLicenceField(pcsCase -> when(pcsCase.getNoticeServed()).thenReturn(null),
                null, null);

        // TODO: Future developers can add ANY field type like:
        // assertTenancyLicenceField(
        //     pcsCase -> when(pcsCase.getRentAmount()).thenReturn(1200), 
        //     "rent_amount", 1200);

    }

    private void assertTenancyLicenceField(java.util.function.Consumer<PCSCase> setupMock,
            String jsonKey, Object expectedValue) {
        PCSCase pcsCase = mock(PCSCase.class);
        setupMock.accept(pcsCase);
        when(pcsCaseRepository.save(pcsCaseEntityCaptor.capture())).thenReturn(null);

        underTest.createCase(CASE_REFERENCE, pcsCase);

        String actualJson = pcsCaseEntityCaptor.getValue().getTenancyLicence();
        if (jsonKey == null) {
            assertThat(actualJson).isNull();
        } else {
            assertThat(actualJson).contains("\"" + jsonKey + "\":" + formatJsonValue(expectedValue));
        }
    }

    private String formatJsonValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        return String.valueOf(value); // for boolean, int, etc.
    }

    private AddressEntity stubAddressUKModelMapper(AddressUK addressUK) {
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(modelMapper.map(addressUK, AddressEntity.class)).thenReturn(addressEntity);
        return addressEntity;
    }

}
