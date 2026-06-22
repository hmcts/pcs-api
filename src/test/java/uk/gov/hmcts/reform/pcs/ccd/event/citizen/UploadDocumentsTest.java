package uk.gov.hmcts.reform.pcs.ccd.event.citizen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.DocumentUploadCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.DocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadDocumentsTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private DocumentService documentService;
    @Mock
    private GenAppVisibilityService genAppVisibilityService;

    @BeforeEach
    void setUp() {
        UploadDocuments underTest = new UploadDocuments(pcsCaseService, partyService,
                                                        securityContextService, documentService,
                                                        genAppVisibilityService);
        setEventUnderTest(underTest);

        // Default: visibility service is identity (returns input unchanged). Individual tests
        // override to simulate hiding without-notice gen apps or to control ordering.
        lenient().when(genAppVisibilityService.getVisibleGenAppsToUser(any(), any()))
            .thenAnswer(invocation -> {
                Collection<GenAppEntity> input = invocation.getArgument(0);
                return input == null ? List.<GenAppEntity>of() : new ArrayList<>(input);
            });
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
        void shouldPersistUploadedDocumentsForCurrentPartyWithNoGenAppWhenNoneSelected() {
            UploadedDocument uploaded = UploadedDocument.builder()
                .document(Document.builder()
                    .url("url-1").filename("file-1.pdf").binaryUrl("bin-1").build())
                .build();

            List<ListValue<UploadedDocument>> uploadedDocs = List.of(
                ListValue.<UploadedDocument>builder().id("1").value(uploaded).build()
            );

            PCSCase caseData = PCSCase.builder()
                .uploadedAdditionalDocuments(uploadedDocs)
                .build();

            PartyEntity currentParty = stubCurrentUserParty();

            callSubmitHandler(caseData);

            verify(documentService).linkAdditionalDocumentsToCase(uploadedDocs, pcsCaseEntity, currentParty, null);
        }

        @Test
        void shouldResolveSelectedGenAppAndPassToDocumentService() {
            UUID selectedId = UUID.randomUUID();
            GenAppEntity selectedGenApp = mock(GenAppEntity.class);
            when(selectedGenApp.getId()).thenReturn(selectedId);
            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(selectedGenApp));

            UploadedDocument uploaded = UploadedDocument.builder()
                .document(Document.builder().url("url-1").filename("f.pdf").binaryUrl("bin").build())
                .build();
            List<ListValue<UploadedDocument>> uploadedDocs = List.of(
                ListValue.<UploadedDocument>builder().id("1").value(uploaded).build()
            );

            PCSCase caseData = PCSCase.builder()
                .uploadedAdditionalDocuments(uploadedDocs)
                .documentUploadDetails(DocumentUploadDetails.builder()
                    .selectedRelatedApplicationId(selectedId.toString())
                    .build())
                .build();

            PartyEntity currentParty = stubCurrentUserParty();

            callSubmitHandler(caseData);

            verify(documentService).linkAdditionalDocumentsToCase(
                uploadedDocs, pcsCaseEntity, currentParty, selectedGenApp);
        }

        @Test
        void shouldPassNullGenAppWhenSelectedIdDoesNotMatchAnyVisibleApp() {
            UUID strayId = UUID.randomUUID();
            GenAppEntity otherGenApp = mock(GenAppEntity.class);
            when(otherGenApp.getId()).thenReturn(UUID.randomUUID());
            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(otherGenApp));

            PCSCase caseData = PCSCase.builder()
                .documentUploadDetails(DocumentUploadDetails.builder()
                    .selectedRelatedApplicationId(strayId.toString())
                    .build())
                .build();

            PartyEntity currentParty = stubCurrentUserParty();

            callSubmitHandler(caseData);

            verify(documentService).linkAdditionalDocumentsToCase(null, pcsCaseEntity, currentParty, null);
        }

        @Test
        void shouldPassNullGenAppWhenSelectedIdIsHiddenByVisibilityService() {
            // Tampering guard: a selectedRelatedApplicationId pointing at a gen app the visibility
            // service hides (e.g. a without-notice app the current user is not party to) must not
            // resolve to a real entity at submit time.
            UUID hiddenId = UUID.randomUUID();
            GenAppEntity hidden = mock(GenAppEntity.class);
            lenient().when(hidden.getId()).thenReturn(hiddenId);
            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(hidden));
            when(genAppVisibilityService.getVisibleGenAppsToUser(any(), any())).thenReturn(List.of());

            PCSCase caseData = PCSCase.builder()
                .documentUploadDetails(DocumentUploadDetails.builder()
                    .selectedRelatedApplicationId(hiddenId.toString())
                    .build())
                .build();

            PartyEntity currentParty = stubCurrentUserParty();

            callSubmitHandler(caseData);

            verify(documentService).linkAdditionalDocumentsToCase(null, pcsCaseEntity, currentParty, null);
        }

        @Test
        void shouldPassNullGenAppWhenSelectedIdIsNotAValidUuid() {
            PCSCase caseData = PCSCase.builder()
                .documentUploadDetails(DocumentUploadDetails.builder()
                    .selectedRelatedApplicationId("not-a-uuid")
                    .build())
                .build();

            PartyEntity currentParty = stubCurrentUserParty();

            callSubmitHandler(caseData);

            verify(documentService).linkAdditionalDocumentsToCase(null, pcsCaseEntity, currentParty, null);
        }

        @Test
        void shouldPassNullGenAppWhenDocumentUploadDetailsHasNoSelection() {
            PCSCase caseData = PCSCase.builder()
                .documentUploadDetails(DocumentUploadDetails.builder().build())
                .build();

            PartyEntity currentParty = stubCurrentUserParty();

            callSubmitHandler(caseData);

            verify(documentService).linkAdditionalDocumentsToCase(null, pcsCaseEntity, currentParty, null);
        }

        @Test
        void shouldDelegateEvenWhenNoDocumentsSent() {
            PCSCase caseData = PCSCase.builder().build();
            PartyEntity currentParty = stubCurrentUserParty();

            callSubmitHandler(caseData);

            verify(documentService).linkAdditionalDocumentsToCase(null, pcsCaseEntity, currentParty, null);
        }

        private PartyEntity stubCurrentUserParty() {
            PartyEntity currentUserParty = mock(PartyEntity.class);
            UUID currentUserId = UUID.randomUUID();
            given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE))
                .willReturn(currentUserParty);
            return currentUserParty;
        }
    }

    @Nested
    @DisplayName("Start event tests")
    class StartTests {

        @Mock
        private PcsCaseEntity pcsCaseEntity;

        private UUID currentUserId;

        @BeforeEach
        void setUp() {
            given(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).willReturn(pcsCaseEntity);
            currentUserId = UUID.randomUUID();
            lenient().when(securityContextService.getCurrentUserId()).thenReturn(currentUserId);
        }

        @Test
        void shouldReturnEmptyOptionsWhenNoGenAppsExist() {
            when(pcsCaseEntity.getGenApps()).thenReturn(new HashSet<>());
            PCSCase caseData = PCSCase.builder().build();

            PCSCase result = callStartHandler(caseData);

            DocumentUploadDetails details = result.getDocumentUploadDetails();
            assertThat(details).isNotNull();
            assertThat(details.getRelatedApplicationOptions()).isEmpty();
            assertThat(details.getShowRelatedApplicationsPage()).isEqualTo(YesOrNo.NO);
        }

        @Test
        void shouldIncludeAdjournCategoryWhenAdjournGenAppExists() {
            LocalDateTime submittedDate = LocalDateTime.now();
            GenAppEntity adjourn = stubGenApp(GenAppType.ADJOURN, submittedDate);

            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(adjourn));

            PCSCase result = callStartHandler(PCSCase.builder().build());

            DocumentUploadDetails details = result.getDocumentUploadDetails();
            assertThat(details.getRelatedApplicationOptions())
                .extracting(option -> option.getValue().getCategory())
                .containsExactly(DocumentUploadCategory.ADJOURN_HEARING_APPLICATION);
            assertThat(details.getRelatedApplicationOptions().getFirst().getValue().getSubmittedDate())
                .isEqualTo(submittedDate);
            assertThat(details.getShowRelatedApplicationsPage()).isEqualTo(YesOrNo.YES);
        }

        @Test
        void shouldExcludeGenAppsWithNoType() {
            // A genApp with a null type means mapGenAppTypeToCategory returns null,
            // toOption returns null, and the option is filtered out.
            GenAppEntity typeless = stubGenApp(null, LocalDateTime.now());

            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(typeless));

            PCSCase result = callStartHandler(PCSCase.builder().build());

            assertThat(result.getDocumentUploadDetails().getRelatedApplicationOptions()).isEmpty();
            assertThat(result.getDocumentUploadDetails().getShowRelatedApplicationsPage())
                .isEqualTo(YesOrNo.NO);
        }

        @Test
        void shouldPreserveOrderingProvidedByVisibilityService() {
            // GenAppVisibilityService is responsible for ordering (submitted date desc).
            // Our code must not re-sort or otherwise reshuffle the result.
            LocalDateTime now = LocalDateTime.now();
            GenAppEntity newest = stubGenApp(GenAppType.SOMETHING_ELSE, now);
            GenAppEntity middle = stubGenApp(GenAppType.SET_ASIDE, now.minusDays(3));
            GenAppEntity oldest = stubGenApp(GenAppType.ADJOURN, now.minusDays(10));

            // Bypass the identity stub: return entities in the order the service would.
            when(genAppVisibilityService.getVisibleGenAppsToUser(any(), any()))
                .thenReturn(List.of(newest, middle, oldest));
            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(newest, middle, oldest));

            PCSCase result = callStartHandler(PCSCase.builder().build());

            assertThat(result.getDocumentUploadDetails().getRelatedApplicationOptions())
                .extracting(option -> option.getValue().getCategory())
                .containsExactly(
                    DocumentUploadCategory.GENERAL_APPLICATION,
                    DocumentUploadCategory.SET_ASIDE_ORDER_APPLICATION,
                    DocumentUploadCategory.ADJOURN_HEARING_APPLICATION);
        }

        @Test
        void shouldEmitOneOptionPerVisibleGenAppEvenWithinTheSameCategory() {
            LocalDateTime older = LocalDateTime.now().minusDays(5);
            LocalDateTime newer = LocalDateTime.now();
            GenAppEntity olderAdjourn = stubGenApp(GenAppType.ADJOURN, older);
            GenAppEntity newerAdjourn = stubGenApp(GenAppType.ADJOURN, newer);

            when(genAppVisibilityService.getVisibleGenAppsToUser(any(), any()))
                .thenReturn(List.of(newerAdjourn, olderAdjourn));
            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(newerAdjourn, olderAdjourn));

            PCSCase result = callStartHandler(PCSCase.builder().build());

            assertThat(result.getDocumentUploadDetails().getRelatedApplicationOptions())
                .hasSize(2)
                .extracting(option -> option.getValue().getSubmittedDate())
                .containsExactly(newer, older);
        }

        @Test
        void shouldStampOptionWithGenAppIdAndUseItAsListValueId() {
            LocalDateTime submittedDate = LocalDateTime.now();
            GenAppEntity adjourn = stubGenApp(GenAppType.ADJOURN, submittedDate);
            UUID genAppId = UUID.randomUUID();
            when(adjourn.getId()).thenReturn(genAppId);

            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(adjourn));

            PCSCase result = callStartHandler(PCSCase.builder().build());

            assertThat(result.getDocumentUploadDetails().getRelatedApplicationOptions())
                .hasSize(1)
                .first()
                .satisfies(option -> {
                    assertThat(option.getId()).isEqualTo(genAppId.toString());
                    assertThat(option.getValue().getGenAppId()).isEqualTo(genAppId.toString());
                });
        }

        @Test
        void shouldDelegateWithNoticeVisibilityToGenAppVisibilityService() {
            // The without-notice / legal-rep / applicant rules live in GenAppVisibilityService.
            // When the service hides everything for this user, no options surface and the
            // frontend skips the confirm page.
            GenAppEntity hidden = stubGenApp(GenAppType.SOMETHING_ELSE, LocalDateTime.now());
            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(hidden));
            when(genAppVisibilityService.getVisibleGenAppsToUser(any(), any())).thenReturn(List.of());

            PCSCase result = callStartHandler(PCSCase.builder().build());

            DocumentUploadDetails details = result.getDocumentUploadDetails();
            assertThat(details.getRelatedApplicationOptions()).isEmpty();
            assertThat(details.getShowRelatedApplicationsPage()).isEqualTo(YesOrNo.NO);
        }

        @Test
        void shouldOnlyRenderGenAppsTheVisibilityServiceReturns() {
            // Mixed set: service returns only the visible one. Our code passes it through.
            LocalDateTime now = LocalDateTime.now();
            GenAppEntity visibleWithNotice = stubGenApp(GenAppType.ADJOURN, now);
            GenAppEntity hiddenWithoutNotice = stubGenApp(GenAppType.SOMETHING_ELSE, now.minusDays(1));
            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(visibleWithNotice, hiddenWithoutNotice));
            when(genAppVisibilityService.getVisibleGenAppsToUser(any(), any()))
                .thenReturn(List.of(visibleWithNotice));

            PCSCase result = callStartHandler(PCSCase.builder().build());

            DocumentUploadDetails details = result.getDocumentUploadDetails();
            assertThat(details.getRelatedApplicationOptions())
                .extracting(option -> option.getValue().getCategory())
                .containsExactly(DocumentUploadCategory.ADJOURN_HEARING_APPLICATION);
            assertThat(details.getShowRelatedApplicationsPage()).isEqualTo(YesOrNo.YES);
        }

        @Test
        void shouldNotSurfaceSuspendCategoryWhileGenAppTypeSuspendIsAbsent() {
            // SUSPEND was removed from GenAppType by PR #1804. Until it is restored, the
            // SUSPEND_EVICTION_APPLICATION category must be filtered out so we don't render
            // a radio backed by no data.
            GenAppEntity adjourn = stubGenApp(GenAppType.ADJOURN, LocalDateTime.now());
            when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(adjourn));

            PCSCase result = callStartHandler(PCSCase.builder().build());

            assertThat(result.getDocumentUploadDetails().getRelatedApplicationOptions())
                .isNotEmpty()
                .extracting(option -> option.getValue().getCategory())
                .doesNotContain(DocumentUploadCategory.SUSPEND_EVICTION_APPLICATION);
        }

        private GenAppEntity stubGenApp(GenAppType type, LocalDateTime submittedDate) {
            GenAppEntity entity = mock(GenAppEntity.class);
            lenient().when(entity.getId()).thenReturn(UUID.randomUUID());
            lenient().when(entity.getType()).thenReturn(type);
            lenient().when(entity.getApplicationSubmittedDate()).thenReturn(submittedDate);
            return entity;
        }

        @Test
        void shouldHandleNullGenAppsCollection() {
            when(pcsCaseEntity.getGenApps()).thenReturn(null);
            PCSCase caseData = PCSCase.builder().build();

            PCSCase result = callStartHandler(caseData);

            assertThat(result.getDocumentUploadDetails().getRelatedApplicationOptions()).isEmpty();
            assertThat(result.getDocumentUploadDetails().getShowRelatedApplicationsPage())
                .isEqualTo(YesOrNo.NO);
        }

        @Test
        void shouldReuseExistingDetailsObjectIfAlreadySet() {
            when(pcsCaseEntity.getGenApps()).thenReturn(new HashSet<>());

            DocumentUploadDetails existing = new DocumentUploadDetails();
            PCSCase caseData = PCSCase.builder()
                .documentUploadDetails(existing)
                .build();

            PCSCase result = callStartHandler(caseData);

            assertThat(result.getDocumentUploadDetails()).isSameAs(existing);
        }
    }
}
