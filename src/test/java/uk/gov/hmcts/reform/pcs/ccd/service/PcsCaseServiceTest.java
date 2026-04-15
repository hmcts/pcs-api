package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;


@ExtendWith(MockitoExtension.class)
class PcsCaseServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private ClaimService claimService;
    @Mock
    private PartyService partyService;
    @Mock
    private DocumentService documentService;
    @Mock
    private TenancyLicenceService tenancyLicenceService;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private CaseFlagService caseFlagService;

    @Captor
    private ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor;

    private PcsCaseService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PcsCaseService(
            pcsCaseRepository,
            claimService,
            partyService,
            documentService,
            tenancyLicenceService,
            addressMapper,
            caseFlagService
        );
    }

    @Test
    void shouldCreateCaseWithAddressAndLegislativeCountry() {
        // Given
        AddressUK propertyAddress = mock(AddressUK.class);
        AddressEntity propertyAddressEntity = mock(AddressEntity.class);
        LegislativeCountry legislativeCountry = mock(LegislativeCountry.class);

        when(addressMapper.toAddressEntityAndNormalise(propertyAddress)).thenReturn(propertyAddressEntity);

        // When
        underTest.createCase(CASE_REFERENCE, propertyAddress, legislativeCountry);

        // Then
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());
        PcsCaseEntity savedEntity = pcsCaseEntityCaptor.getValue();
        assertThat(savedEntity.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(savedEntity.getPropertyAddress()).isEqualTo(propertyAddressEntity);
        assertThat(savedEntity.getLegislativeCountry()).isEqualTo(legislativeCountry);
    }

    @Test
    void shouldLoadCaseFromRepository() {
        // Given
        PcsCaseEntity expectedPcsCaseEntity = stubFindCase();

        // When
        PcsCaseEntity actualPcsCaseEntity = underTest.loadCase(CASE_REFERENCE);

        // Then
        verify(pcsCaseRepository).findByCaseReference(CASE_REFERENCE);
        assertThat(actualPcsCaseEntity).isSameAs(expectedPcsCaseEntity);
    }

    @Test
    void shouldThrowExceptionLoadingUnknownCaseReference() {
        // Given
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> underTest.loadCase(CASE_REFERENCE));

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessage("No case found with reference %s", CASE_REFERENCE);
    }

    @Test
    void shouldDelegateToClaimServiceToCreateMainClaim() {
        // Given
        PcsCaseEntity pcsCaseEntity = stubFindCase();
        ClaimEntity mainClaimEntity = stubClaimCreation();

        PCSCase caseData = PCSCase.builder().build();

        // When
        underTest.createMainClaimOnCase(CASE_REFERENCE, caseData);

        // Then
        verify(claimService).createMainClaimEntity(caseData);
        verify(pcsCaseEntity).addClaim(mainClaimEntity);
    }

    @Test
    void shouldCreatePartiesWithMainClaimOnCase() {
        // Given
        final PcsCaseEntity pcsCaseEntity = stubFindCase();
        final ClaimEntity mainClaimEntity = stubClaimCreation();

        PCSCase caseData = PCSCase.builder().build();

        // When
        underTest.createMainClaimOnCase(CASE_REFERENCE, caseData);

        // Then
        verify(partyService).createAllParties(caseData, pcsCaseEntity, mainClaimEntity);
    }

    @Test
    void shouldCreateDocumentsWithMainClaimOnCase() {
        // Given
        final PcsCaseEntity pcsCaseEntity = stubFindCase();
        final ClaimEntity mainClaimEntity = stubClaimCreation();

        PCSCase caseData = PCSCase.builder().build();

        List<DocumentEntity> documentEntities = List.of(mock(DocumentEntity.class), mock(DocumentEntity.class));
        when(documentService.createAllDocuments(caseData)).thenReturn(documentEntities);

        // When
        underTest.createMainClaimOnCase(CASE_REFERENCE, caseData);

        // Then
        verify(pcsCaseEntity).addDocuments(documentEntities);
        verify(mainClaimEntity).addClaimDocuments(documentEntities);
    }

    @Test
    void shouldCreateTenancyLicenceWithMainClaimOnCase() {
        // Given
        final PcsCaseEntity pcsCaseEntity = stubFindCase();
        stubClaimCreation();

        PCSCase caseData = PCSCase.builder().build();

        TenancyLicenceEntity tenancyLicenceEntity = mock(TenancyLicenceEntity.class);
        when(tenancyLicenceService.createTenancyLicenceEntity(caseData))
            .thenReturn(tenancyLicenceEntity);


        // When
        underTest.createMainClaimOnCase(CASE_REFERENCE, caseData);

        // Then
        verify(pcsCaseEntity).setTenancyLicence(tenancyLicenceEntity);
    }

    @Test
    void shouldThrowExceptionWhenCaseDataIsNull() {
        // Given
        PCSCase caseData = null;

        // When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> underTest.patchCaseFlags(CASE_REFERENCE, caseData));

        // Then
        assertEquals("PCSCase cannot be null", exception.getMessage());
    }

    @Test
    void shouldPatchCaseDataWithCaseFlags() {
        // Given
        PcsCaseEntity pcsCaseEntity =  PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .build();

        List<ListValue<FlagDetail>> details = List.of(createFlagDetails());

        Flags flags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(details)
            .build();

        PCSCase caseData = PCSCase.builder()
            .caseFlags(flags)
            .build();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        doNothing().when(caseFlagService).mergeCaseFlags(flags, pcsCaseEntity);

        // When
        underTest.patchCaseFlags(CASE_REFERENCE, caseData);

        // Then
        verify(caseFlagService).mergeCaseFlags(flags, pcsCaseEntity);
        verify(caseFlagService, times(1)).mergeCaseFlags(flags, pcsCaseEntity);
    }

    private ListValue<FlagDetail> createFlagDetails() {

        return ListValue.<FlagDetail>builder()
            .id(UUID.randomUUID().toString())
            .value(FlagDetail.builder()
                .flagCode("CF0007")
                .name("Urgent case")
                .flagComment("Needs to be handled ASAP")
                .build())
            .build();
    }

    private PcsCaseEntity stubFindCase() {
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        return pcsCaseEntity;
    }

    private ClaimEntity stubClaimCreation() {
        ClaimEntity claimEntity = mock(ClaimEntity.class);
        when(claimService.createMainClaimEntity(any(PCSCase.class))).thenReturn(claimEntity);
        return claimEntity;
    }

}
