package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
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
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativeService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakeAnApplicationTest extends BaseEventTest {

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
    @Mock
    private LegalRepresentativeService legalRepresentativeService;

    @BeforeEach
    void setUp() {
        MakeAnApplication underTest = new MakeAnApplication(pcsCaseService, partyService,
                                                            securityContextService, genAppService,
                                                            genAppRepository, genAppDocumentGenerator,
                                                            documentImportService, legalRepresentativeService);

        setEventUnderTest(underTest);
    }

    @Nested
    @DisplayName("Start event tests")
    class StartEventTests {
        @Test
        void shouldSetRepresentedPartiesFieldWhenUserRepresentsOne() {
            // Given
            DynamicList expectedPartyNameList = mock(DynamicList.class);
            DynamicListElement party1Element = mock(DynamicListElement.class);
            when(expectedPartyNameList.getListItems()).thenReturn(List.of(party1Element));

            UUID currentUserId = UUID.randomUUID();
            when(securityContextService.getCurrentUserId()).thenReturn(currentUserId);
            when(legalRepresentativeService.getRepresentedPartiesDynamicList(currentUserId, TEST_CASE_REFERENCE))
                .thenReturn(Optional.of(expectedPartyNameList));

            PCSCase caseData = PCSCase.builder()
                .build();

            // When
            callStartHandler(caseData);

            // Then
            assertThat(caseData.getRepresentedPartyNames()).isEqualTo(expectedPartyNameList);
        }

        @ParameterizedTest
        @MethodSource("multipleRepresentedPartiesScenarios")
        void shouldSetMultipleRepresentedPartiesFlag(int numRepresentedParties, VerticalYesNo expectedFlag) {
            // Given
            DynamicList expectedPartyNameList = mock(DynamicList.class);
            @SuppressWarnings("unchecked") List<DynamicListElement> listItems = mock(List.class);
            when(expectedPartyNameList.getListItems()).thenReturn(listItems);
            when(listItems.size()).thenReturn(numRepresentedParties);

            UUID currentUserId = UUID.randomUUID();
            when(securityContextService.getCurrentUserId()).thenReturn(currentUserId);
            when(legalRepresentativeService.getRepresentedPartiesDynamicList(currentUserId, TEST_CASE_REFERENCE))
                .thenReturn(Optional.of(expectedPartyNameList));

            PCSCase caseData = PCSCase.builder()
                .build();

            // When
            callStartHandler(caseData);

            // Then
            assertThat(caseData.getMultipleRepresentedParties()).isEqualTo(expectedFlag);
        }

        private static Stream<Arguments> multipleRepresentedPartiesScenarios() {
            return Stream.of(
                // Represented party count, expected flag
                arguments(0, VerticalYesNo.NO),
                arguments(1, VerticalYesNo.NO),
                arguments(2, VerticalYesNo.YES)
            );
        }

        @Test
        void shouldNotSetRepresentedPartiesFieldWhenUserDoesNotRepresentAny() {
            // Given
            UUID currentUserId = UUID.randomUUID();
            when(securityContextService.getCurrentUserId()).thenReturn(currentUserId);
            when(legalRepresentativeService.getRepresentedPartiesDynamicList(currentUserId, TEST_CASE_REFERENCE))
                .thenReturn(Optional.empty());

            PCSCase caseData = PCSCase.builder()
                .build();

            // When
            callStartHandler(caseData);

            // Then
            assertThat(caseData.getRepresentedPartyNames()).isNull();
        }
    }

    @Nested
    @DisplayName("XUI submit event tests")
    class XuiSubmitEventTests {

        @Mock
        private PcsCaseEntity pcsCaseEntity;

        @BeforeEach
        void setUp() {
            given(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).willReturn(pcsCaseEntity);
        }

        @Test
        void shouldCreateGenAppWithCaseDataAndRepresentedParty() {
            // Given
            UUID representedPartyUuid = UUID.randomUUID();
            PartyEntity representedParty = mock(PartyEntity.class);

            DynamicList representedParties = DynamicList.builder()
                .value(DynamicListElement.builder().code(representedPartyUuid).build())
                .build();

            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .build();

            PCSCase caseData = PCSCase.builder()
                .representedPartyNames(representedParties)
                .citizenGenAppRequest(genAppRequest)
                .build();

            given(partyService.getPartyEntityByEntityId(representedPartyUuid, TEST_CASE_REFERENCE))
                .willReturn(representedParty);

            // When
            callSubmitHandler(caseData);

            // Then
            verify(genAppService).createGenAppEntity(genAppRequest, pcsCaseEntity, representedParty);
        }
    }

    @Nested
    @DisplayName("Citizen submit event tests")
    class CitizenSubmitEventTests {

        @Mock
        private PcsCaseEntity pcsCaseEntity;

        @BeforeEach
        void setUp() {
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

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            UUID currentUserId = UUID.randomUUID();
            given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(applicantParty);

            // When
            callSubmitHandler(caseData);

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
            SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

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
