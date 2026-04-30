package uk.gov.hmcts.reform.pcs.ccd.event.citizen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenCreateGenAppTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private GenAppService genAppService;
    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private GenAppDocumentGenerator genAppDocumentGenerator;
    @Mock
    private DocumentImportService documentImportService;

    @BeforeEach
    void setUp() {
        CitizenCreateGenApp underTest = new CitizenCreateGenApp(pcsCaseService, partyService, securityContextService,
                                                                genAppService, genAppRepository,
                                                                genAppDocumentGenerator, documentImportService);

        setEventUnderTest(underTest);
    }

    @Nested
    @DisplayName("Submit event tests")
    class SubmitTests {

        @Mock
        private PcsCaseEntity pcsCaseEntity;

        @BeforeEach
        void setUp() {
            given(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).willReturn(pcsCaseEntity);
        }

        @Test
        void shouldCreateGenAppWithCaseDataAndApplicantParty() {
            // Given
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SUSPEND)
                .clientReference("some reference")
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            PartyEntity applicantParty = stubCurrentUserParty();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(genAppService).createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);
        }

        @Test
        void shouldReturnErrorIfNoClientReference() {
            // Given
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SUSPEND)
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

            // Then
            assertThat(submitResponse.getErrors())
                .containsExactly("No client reference in request");

            verify(genAppService, never()).createGenAppEntity(any(), any(), any());
        }

        @Test
        void shouldReturnErrorIfDuplicateClientReference() {
            // Given
            String existingClientReference = "ref-1234";
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SUSPEND)
                .clientReference(existingClientReference)
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            when(genAppRepository.existsByClientReference(existingClientReference)).thenReturn(true);

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

            // Then
            assertThat(submitResponse.getErrors())
                .containsExactly("Application already exists for client reference");

            verify(genAppService, never()).createGenAppEntity(any(), any(), any());
        }

        @Test
        void shouldGenerateGenAppDocumentAndStoreMetadata() {
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SUSPEND)
                .clientReference("some reference")
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            PartyEntity applicantParty = stubCurrentUserParty();

            GenAppEntity genAppEntity = mock(GenAppEntity.class);
            when(genAppService.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty))
                .thenReturn(genAppEntity);

            String documentUrl = "some document URL";
            when(genAppDocumentGenerator.generateSubmissionDocument(TEST_CASE_REFERENCE, genAppRequest, genAppEntity))
                .thenReturn(documentUrl);

            // When
            callSubmitHandler(caseData);

            // Then
            verify(documentImportService).addDocumentToCase(TEST_CASE_REFERENCE, documentUrl,
                                                            CaseFileCategory.APPLICATIONS
            );
        }

        private PartyEntity stubCurrentUserParty() {
            PartyEntity currentUserParty = mock(PartyEntity.class);
            UUID currentUserId = UUID.randomUUID();
            given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(currentUserParty);
            return currentUserParty;
        }

    }
}
