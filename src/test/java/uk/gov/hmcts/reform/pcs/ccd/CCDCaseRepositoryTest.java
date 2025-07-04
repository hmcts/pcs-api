package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimantInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.GeneralApplicationRenderer;
import uk.gov.hmcts.reform.pcs.ccd.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CCDCaseRepositoryTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PCSCaseRepository pcsCaseRepository;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private PCSCaseService pcsCaseService;
    @Mock
    private GeneralApplicationService genAppService;
    @Mock
    private GeneralApplicationRenderer genAppRenderer;
    @Mock
    private GeneralApplicationRepository genAppnRepo;

    private CCDCaseRepository underTest;

    @BeforeEach
    void setUp() {
        underTest = new CCDCaseRepository(
            pcsCaseRepository,
            genAppnRepo,
            genAppService,
            pcsCaseService,
            genAppRenderer
        );
    }


    @Test
    void shouldReturnCaseWithNoPropertyAddress() {
        // Given
        final String expectedForename = "Test forename";
        final String expectedSurname = "Test surname";
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setGeneralApplications(new ArrayList<>());

        ClaimantInfo claimantInfo = new ClaimantInfo();
        claimantInfo.setForename(expectedForename);
        claimantInfo.setSurname(expectedSurname);
        pcsCaseEntity.setClaimantInfo(claimantInfo);

        PCSCase pcsCaseReturned = PCSCase.builder()
            .applicantForename(expectedForename)
            .applicantSurname(expectedSurname)
            .propertyAddress(null)
            .generalApplications(new ArrayList<>())
            .build();

        when(pcsCaseService.convertToPCSCase(pcsCaseEntity)).thenReturn(pcsCaseReturned);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // When
        PCSCase pcsCase = (PCSCase) underTest.getCase(CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getApplicantForename()).isEqualTo(expectedForename);
        assertThat(pcsCase.getApplicantSurname()).isEqualTo(expectedSurname);
        assertThat(pcsCase.getPropertyAddress()).isNull();
    }

    @Test
    void shouldMapPropertyAddress() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        AddressUK addressUK = new AddressUK();
        PCSCase pcsCaseReturned = PCSCase.builder()
            .propertyAddress(addressUK)
            .generalApplications(new ArrayList<>())
            .build();

        when(pcsCaseService.convertToPCSCase(pcsCaseEntity)).thenReturn(pcsCaseReturned);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // When
        PCSCase pcsCase = (PCSCase) underTest.getCase(CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getPropertyAddress()).isEqualTo(addressUK);
    }


}
