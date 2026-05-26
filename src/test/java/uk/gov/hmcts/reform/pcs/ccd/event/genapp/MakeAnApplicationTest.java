package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
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
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativeService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mock.Strictness.LENIENT;
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
    @Mock(strictness = LENIENT)
    private GenAppService genAppService;
    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private GenAppDocumentGenerator genAppDocumentGenerator;
    @Mock
    private DocumentImportService documentImportService;
    @Mock
    private LegalRepresentativeService legalRepresentativeService;
    @Mock
    private FeeApplier feeApplier;
    @Mock
    private GenAppEntity genAppEntity;
    @Captor
    private ArgumentCaptor<BiConsumer<PCSCase, String>> feeSetterCaptor;

    @BeforeEach
    void setUp() {
        when(genAppService.createGenAppEntity(any(CitizenGenAppRequest.class),
                                              any(PcsCaseEntity.class),
                                              any(PartyEntity.class))).thenReturn(genAppEntity);

        MakeAnApplication underTest = new MakeAnApplication(pcsCaseService, partyService,
                                                            securityContextService, genAppService,
                                                            genAppRepository, genAppDocumentGenerator,
                                                            documentImportService, legalRepresentativeService,
                                                            feeApplier);

        setEventUnderTest(underTest);
    }

    @Nested
    @DisplayName("Start event tests")
    class StartEventTests {
        @Test
        void shouldSetRepresentedPartiesFieldWhenUserRepresentsOne() {
            // Given
            DynamicList expectedPartyNameList = DynamicList.builder()
                .listItems(List.of(DynamicListElement.builder().code(UUID.randomUUID()).build()))
                .build();

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
            List<DynamicListElement> listItems = IntStream.range(0, numRepresentedParties)
                .boxed()
                .map(i -> DynamicListElement.builder().code(UUID.randomUUID()).build())
                .toList();

            DynamicList expectedPartyNameList = DynamicList.builder()
                .listItems(listItems)
                .build();

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

        @Test
        void shouldSetTheApplicationFees() {
            // Given
            final String formattedStandardFee = "10.99";
            final String formattedMaxFee = "20.99";

            PCSCase caseData = PCSCase.builder()
                .xuiGenAppRequest(XuiGenAppRequest.builder().build())
                .build();

            // When
            callStartHandler(caseData);

            // Then
            verify(feeApplier)
                .applyFeeAmount(eq(caseData), eq(FeeType.GEN_APP_STANDARD_FEE), feeSetterCaptor.capture());
            feeSetterCaptor.getValue().accept(caseData, formattedStandardFee);
            assertThat(caseData.getXuiGenAppRequest().getStandardFee()).isEqualTo(formattedStandardFee);

            verify(feeApplier)
                .applyFeeAmount(eq(caseData), eq(FeeType.GEN_APP_MAX_FEE), feeSetterCaptor.capture());
            feeSetterCaptor.getValue().accept(caseData, formattedMaxFee);
            assertThat(caseData.getXuiGenAppRequest().getMaxFee()).isEqualTo(formattedMaxFee);
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

            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SET_ASIDE)
                .build();

            PCSCase caseData = PCSCase.builder()
                .currentRepresentedPartyId(representedPartyUuid.toString())
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

            String documentUrl = "some document URL";
            when(genAppDocumentGenerator
                     .generateSubmissionDocument(TEST_CASE_REFERENCE, genAppRequest, genAppEntity, applicantParty))
                .thenReturn(documentUrl);

            DocumentEntity documentEntity = mock(DocumentEntity.class);
            when(documentImportService.addDocumentToCase(eq(TEST_CASE_REFERENCE), anyString(), any()))
                .thenReturn(documentEntity);

            // When
            callSubmitHandler(caseData);

            // Then
            verify(documentImportService).addDocumentToCase(TEST_CASE_REFERENCE, documentUrl,
                                                            CaseFileCategory.APPLICATIONS
            );

            verify(genAppEntity).setSubmissionDocument(documentEntity);
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
