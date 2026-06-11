package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class SubmitEventHandlerTest {

    private static final long TEST_CASE_REFERENCE = 1234L;

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
    @Mock(strictness = LENIENT)
    private DocumentImportService documentImportService;
    @Mock
    private LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;
    @Mock
    private ConfirmationScreenFactory confirmationScreenFactory;
    @Mock
    private OrganisationService organisationService;
    private SubmitEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitEventHandler(pcsCaseService, partyService, securityContextService, genAppService,
                                           genAppRepository, genAppDocumentGenerator, documentImportService,
                                           legalRepresentativeOrganisationRepository, confirmationScreenFactory,
                                           organisationService);
    }

    @Nested
    @DisplayName("XUI submit event tests")
    class XuiSubmitEventTests {

        @Mock
        private PcsCaseEntity pcsCaseEntity;

        @BeforeEach
        void setUp() {
            stubDocumentImport();
            given(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).willReturn(pcsCaseEntity);
        }

        @Test
        void shouldCreateGenAppWithCaseDataAndRepresentedParty() {
            // Given
            UUID representedPartyUuid = UUID.randomUUID();
            PartyEntity representedParty = mock(PartyEntity.class);

            XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .build();

            when(partyService.getPartyEntityByEntityId(representedPartyUuid, TEST_CASE_REFERENCE))
                .thenReturn(representedParty);

            stubLegalRepForParty(representedPartyUuid);

            stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, representedParty);

            PCSCase caseData = PCSCase.builder()
                .currentRepresentedPartyId(representedPartyUuid.toString())
                .xuiGenAppRequest(genAppRequest)
                .build();

            // When
            underTest.submit(eventPayload(caseData));

            // Then
            verify(genAppService).createGenAppEntity(genAppRequest, pcsCaseEntity, representedParty);
        }

        @Test
        void shouldBuildConfirmationScreenResponse() {
            // Given
            UUID representedPartyUuid = UUID.randomUUID();
            PartyEntity representedParty = mock(PartyEntity.class);

            XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .build();

            when(partyService.getPartyEntityByEntityId(representedPartyUuid, TEST_CASE_REFERENCE))
                .thenReturn(representedParty);

            stubLegalRepForParty(representedPartyUuid);

            stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, representedParty);

            SubmitResponse<State> expectedSubmitResponse = createMockSubmitResponse();
            when(confirmationScreenFactory.buildConfirmationScreenResponse(genAppRequest, TEST_CASE_REFERENCE))
                .thenReturn(expectedSubmitResponse);

            PCSCase caseData = PCSCase.builder()
                .currentRepresentedPartyId(representedPartyUuid.toString())
                .xuiGenAppRequest(genAppRequest)
                .build();

            // When
            SubmitResponse<State> actualSubmitResponse = underTest.submit(eventPayload(caseData));

            // Then
            assertThat(actualSubmitResponse).isEqualTo(expectedSubmitResponse);
        }

        @Test
        void shouldThrowErrorIfApplicantIsNotRepresentedByCurrentUser() {
            // Given
            UUID representedPartyUuid = UUID.randomUUID();
            XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .build();

            PCSCase caseData = PCSCase.builder()
                .currentRepresentedPartyId(representedPartyUuid.toString())
                .xuiGenAppRequest(genAppRequest)
                .build();

            UUID currentUserId = UUID.randomUUID();
            String orgId = "org";
            when(securityContextService.getCurrentUserId()).thenReturn(currentUserId);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(orgId);
            when(legalRepresentativeOrganisationRepository
                     .isRepresentativeOrganisationLinkedToPartyAndActive(orgId, representedPartyUuid))
                .thenReturn(false);

            // When
            Throwable throwable = catchThrowable(() -> underTest.submit(eventPayload(caseData)));

            // Then
            assertThat(throwable).isInstanceOf(PartyNotFoundException.class);
        }

        private void stubLegalRepForParty(UUID representedPartyUuid) {
            UUID currentUserId = UUID.randomUUID();
            String orgId = "org";
            when(securityContextService.getCurrentUserId()).thenReturn(currentUserId);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(orgId);
            when(legalRepresentativeOrganisationRepository
                     .isRepresentativeOrganisationLinkedToPartyAndActive(orgId, representedPartyUuid))
                .thenReturn(true);
        }

    }

    @Nested
    @DisplayName("Citizen submit event tests")
    class CitizenSubmitEventTests {

        @Mock
        private PcsCaseEntity pcsCaseEntity;

        @BeforeEach
        void setUp() {
            stubDocumentImport();
            given(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).willReturn(pcsCaseEntity);
        }

        @Test
        void shouldCreateGenAppWithCaseDataAndApplicantParty() {
            // Given
            final PartyEntity applicantParty = mock(PartyEntity.class);

            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .clientReference("some reference")
                .build();

            stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

            UUID currentUserId = UUID.randomUUID();
            given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(applicantParty);

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            // When
            underTest.submit(eventPayload(caseData));

            // Then
            verify(genAppService).createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);
        }

        @Test
        void shouldReturnErrorIfDuplicateClientReference() {
            // Given
            String existingClientReference = "ref-1234";
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.ADJOURN)
                .clientReference(existingClientReference)
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            when(genAppRepository.existsByPcsCaseAndClientReference(pcsCaseEntity, existingClientReference))
                .thenReturn(true);

            // When
            SubmitResponse<State> submitResponse = underTest.submit(eventPayload(caseData));

            // Then
            assertThat(submitResponse.getErrors())
                .containsExactly("Application already exists for client reference");

            verify(genAppService, never()).createGenAppEntity(any(), any(), any());
        }

        @Test
        void shouldGenerateGenAppDocumentAndStoreMetadata() {
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.ADJOURN)
                .clientReference("some reference")
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            PartyEntity applicantParty = stubCurrentUserParty();

            GenAppEntity genAppEntity = stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

            String documentUrl = "some document URL";
            when(genAppDocumentGenerator
                     .generateSubmissionDocument(TEST_CASE_REFERENCE, genAppRequest, genAppEntity, applicantParty))
                .thenReturn(documentUrl);

            // When
            underTest.submit(eventPayload(caseData));

            // Then
            verify(documentImportService).addDocumentToCase(TEST_CASE_REFERENCE, documentUrl,
                                                            CaseFileCategory.APPLICATIONS
            );
        }

        @Test
        void shouldSetGenAppReferenceOnImportedDocumentEntity() {
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.ADJOURN)
                .clientReference("some reference")
                .build();

            final PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            PartyEntity applicantParty = stubCurrentUserParty();

            GenAppEntity genAppEntity = stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

            String documentUrl = "some document URL";
            when(genAppDocumentGenerator
                     .generateSubmissionDocument(TEST_CASE_REFERENCE, genAppRequest, genAppEntity, applicantParty))
                .thenReturn(documentUrl);

            DocumentEntity importedDocumentEntity = stubDocumentImport();

            // When
            underTest.submit(eventPayload(caseData));

            // Then
            verify(importedDocumentEntity).setGeneralApplication(genAppEntity);
            verify(genAppEntity).setSubmissionDocument(importedDocumentEntity);
        }

        @Test
        void shouldBuildConfirmationScreenResponse() {
            // Given
            final PartyEntity applicantParty = mock(PartyEntity.class);

            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .clientReference("some reference")
                .build();

            stubCreateGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty);

            UUID currentUserId = UUID.randomUUID();
            given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(applicantParty);

            SubmitResponse<State> expectedSubmitResponse = createMockSubmitResponse();
            when(confirmationScreenFactory.buildConfirmationScreenResponse(genAppRequest, TEST_CASE_REFERENCE))
                .thenReturn(expectedSubmitResponse);

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            // When
            SubmitResponse<State> actualSubmitResponse = underTest.submit(eventPayload(caseData));

            // Then
            assertThat(actualSubmitResponse).isEqualTo(expectedSubmitResponse);
        }

    }

    private static EventPayload<PCSCase, State> eventPayload(PCSCase caseData) {
        return new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);
    }

    private PartyEntity stubCurrentUserParty() {
        PartyEntity currentUserParty = mock(PartyEntity.class);
        UUID currentUserId = UUID.randomUUID();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(currentUserParty);
        return currentUserParty;
    }

    private DocumentEntity stubDocumentImport() {
        DocumentEntity importedDocumentEntity = mock(DocumentEntity.class);
        when(documentImportService
                 .addDocumentToCase(eq(TEST_CASE_REFERENCE), nullable(String.class), any(CaseFileCategory.class)))
            .thenReturn(importedDocumentEntity);
        return importedDocumentEntity;
    }

    private GenAppEntity stubCreateGenAppEntity(GenAppRequest genAppRequest,
                                                PcsCaseEntity pcsCaseEntity,
                                                PartyEntity applicantParty) {
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppService.createGenAppEntity(genAppRequest, pcsCaseEntity, applicantParty)).thenReturn(genAppEntity);
        return genAppEntity;
    }

    @SuppressWarnings("unchecked")
    private static SubmitResponse<State> createMockSubmitResponse() {
        return mock(SubmitResponse.class);
    }

}
