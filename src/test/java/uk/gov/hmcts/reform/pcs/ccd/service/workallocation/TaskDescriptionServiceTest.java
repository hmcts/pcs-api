package uk.gov.hmcts.reform.pcs.ccd.service.workallocation;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
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
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class TaskDescriptionServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PartyService partyService;
    @Mock
    private PebbleEngine pebbleEngine;
    @Mock(strictness = LENIENT)
    private ClaimRepository claimRepository;
    @Mock
    private ClaimEntity mainClaim;
    @Captor
    private ArgumentCaptor<Map<String, Object>> contextMapCaptor;

    private TaskDescriptionService underTest;

    @BeforeEach
    void setUp() {
        when(claimRepository.findClaimByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(mainClaim));

        underTest = new TaskDescriptionService(partyService, pebbleEngine, claimRepository);
    }

    @Nested
    @DisplayName("Get description for Review Gen App task")
    class ReviewAdjournGenAppTests {

        @Mock
        private PartyEntity partyEntity;
        @Mock
        private GenAppEntity genAppEntity;

        @ParameterizedTest
        @MethodSource("genAppTypeToTemplateNameScenarios")
        void shouldRenderTaskDescription(GenAppType genAppType, String expectedTemplateName) throws IOException {
            // Given
            UUID partyId = UUID.randomUUID();
            when(partyEntity.getId()).thenReturn(partyId);

            when(genAppEntity.getType()).thenReturn(genAppType);
            when(genAppEntity.getParty()).thenReturn(partyEntity);
            when(genAppEntity.getSubmissionDocument()).thenReturn(
                DocumentEntity.builder().fileName("submission-document.pdf").build()
            );

            String expectedPartyLabel = "some party label";
            when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn(expectedPartyLabel);

            when(genAppEntity.getDocuments()).thenReturn(List.of(
                DocumentEntity.builder().fileName("filename1.pdf").build(),
                DocumentEntity.builder().fileName("filename2.csv").build()
            ));

            String expectedRenderedContent = "some rendered content";
            PebbleTemplate pebbleTemplate = stubPebbleTemplate(
                expectedTemplateName,
                expectedRenderedContent
            );

            // When
            String description = underTest.createReviewGenAppDescription(CASE_REFERENCE, genAppEntity);

            // Then
            assertThat(description).isEqualTo(expectedRenderedContent);

            verify(pebbleTemplate).evaluate(isA(StringWriter.class), contextMapCaptor.capture());
            Map<String, Object> contextMap = contextMapCaptor.getValue();
            assertThat(contextMap)
                .containsEntry("caseReference", CASE_REFERENCE)
                .containsEntry("partyLabel", expectedPartyLabel)
                .containsEntry("filenames", List.of("submission-document.pdf", "filename1.pdf", "filename2.csv"));
        }

        private static Stream<Arguments> genAppTypeToTemplateNameScenarios() {
            return Stream.of(
                Arguments.arguments(GenAppType.ADJOURN, "review-adjourn-gen-app"),
                Arguments.arguments(GenAppType.SET_ASIDE, "review-set-aside-gen-app"),
                Arguments.arguments(GenAppType.SOMETHING_ELSE, "review-gen-app")
            );
        }

        @Test
        void shouldThrowExceptionWhenUnableToRenderTemplate() throws IOException {
            // Given
            UUID partyId = UUID.randomUUID();
            when(partyEntity.getId()).thenReturn(partyId);

            when(genAppEntity.getParty()).thenReturn(partyEntity);
            when(genAppEntity.getType()).thenReturn(GenAppType.ADJOURN);
            when(genAppEntity.getSubmissionDocument()).thenReturn(
                DocumentEntity.builder().fileName("submission-document.pdf").build()
            );

            PebbleTemplate pebbleTemplate = stubPebbleTemplate(
                "review-adjourn-gen-app",
                "some content"
            );

            when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn("some party label");

            IOException pebbleException = mock(IOException.class);
            doThrow(pebbleException).when(pebbleTemplate).evaluate(any(StringWriter.class), anyMap());

            // When
            Throwable throwable = catchThrowable(
                () -> underTest.createReviewGenAppDescription(CASE_REFERENCE, genAppEntity)
            );

            // Then
            assertThat(throwable)
                .isInstanceOf(TemplateRenderingException.class)
                .hasMessage("Failed to render template")
                .hasCause(pebbleException);
        }
    }

    @Nested
    @DisplayName("Get description for Gen App Additional Documents task")
    class GenAppAdditionalDocumentsTests {

        @Mock
        private PartyEntity partyEntity;
        @Mock
        private GenAppEntity genAppEntity;

        @Test
        void shouldRenderTaskDescription() throws IOException {
            // Given
            UUID partyId = UUID.randomUUID();
            when(partyEntity.getId()).thenReturn(partyId);

            int expectedGenAppRank = 4;
            when(genAppEntity.getRank()).thenReturn(expectedGenAppRank);

            List<DocumentEntity> documentEntityList = List.of(
                DocumentEntity.builder().fileName("filename1.pdf").build(),
                DocumentEntity.builder().fileName("filename2.csv").build()
            );

            String expectedPartyLabel = "some party label";
            when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn(expectedPartyLabel);

            String expectedRenderedContent = "some rendered content";
            PebbleTemplate pebbleTemplate = stubPebbleTemplate(
                "gen-app-review-additional-docs",
                expectedRenderedContent
            );

            // When
            String description = underTest
                .createGenAppAdditionalDocumentsDescription(
                    CASE_REFERENCE,
                    mainClaim,
                    partyEntity,
                    genAppEntity,
                    documentEntityList
                );

            // Then
            assertThat(description).isEqualTo(expectedRenderedContent);

            verify(pebbleTemplate).evaluate(isA(StringWriter.class), contextMapCaptor.capture());
            Map<String, Object> contextMap = contextMapCaptor.getValue();
            assertThat(contextMap)
                .containsEntry("caseReference", CASE_REFERENCE)
                .containsEntry("partyLabel", expectedPartyLabel)
                .containsEntry("genAppRank", expectedGenAppRank)
                .containsEntry("filenames", List.of("filename1.pdf", "filename2.csv"));
        }

        @Test
        void shouldThrowExceptionWhenUnableToRenderTemplate() throws IOException {
            // Given
            PebbleTemplate pebbleTemplate = stubPebbleTemplate(
                "gen-app-review-additional-docs",
                "some content"
            );

            IOException pebbleException = mock(IOException.class);
            doThrow(pebbleException).when(pebbleTemplate).evaluate(any(StringWriter.class), anyMap());

            UUID partyId = UUID.randomUUID();
            when(partyEntity.getId()).thenReturn(partyId);
            when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn("some party label");

            // When
            Throwable throwable = catchThrowable(
                () -> underTest.createGenAppAdditionalDocumentsDescription(
                    CASE_REFERENCE,
                    mainClaim,
                    partyEntity,
                    genAppEntity,
                    List.of()
                ));

            // Then
            assertThat(throwable)
                .isInstanceOf(TemplateRenderingException.class)
                .hasMessage("Failed to render template")
                .hasCause(pebbleException);
        }

    }

    @Nested
    @DisplayName("Get description for Translate Claimant Document task")
    class TranslateClaimantDocumentTests {

        @Test
        void shouldRenderTaskDescription() throws IOException {
            // Given
            List<DocumentEntity> documentEntityList = List.of(
                DocumentEntity.builder().fileName("filename1.pdf").build(),
                DocumentEntity.builder().fileName("filename2.csv").build()
            );

            String expectedRenderedContent = "some rendered content";
            PebbleTemplate pebbleTemplate = stubPebbleTemplate(
                "translate-claimant-submitted-document",
                expectedRenderedContent
            );

            // When
            String description = underTest.createTranslateClaimantDocumentDescription(
                CASE_REFERENCE, documentEntityList);

            // Then
            assertThat(description).isEqualTo(expectedRenderedContent);

            verify(pebbleTemplate).evaluate(isA(StringWriter.class), contextMapCaptor.capture());
            Map<String, Object> contextMap = contextMapCaptor.getValue();
            assertThat(contextMap)
                .containsEntry("caseReference", CASE_REFERENCE)
                .containsEntry("filenames", List.of("filename1.pdf", "filename2.csv"));
        }

        @Test
        void shouldThrowExceptionWhenUnableToRenderTemplate() throws IOException {
            // Given
            PebbleTemplate pebbleTemplate = stubPebbleTemplate(
                "translate-claimant-submitted-document",
                "some content"
            );

            IOException pebbleException = mock(IOException.class);
            doThrow(pebbleException).when(pebbleTemplate).evaluate(any(StringWriter.class), anyMap());

            // When
            Throwable throwable = catchThrowable(
                () -> underTest.createTranslateClaimantDocumentDescription(
                    CASE_REFERENCE, List.of()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TemplateRenderingException.class)
                .hasMessage("Failed to render template")
                .hasCause(pebbleException);
        }

    }

    @Nested
    @DisplayName("Get description for Claim Additional Documents task")
    class ClaimAdditionalDocumentsTests {
        @Mock
        private ClaimEntity mainClaim;
        @Mock
        private PartyEntity partyEntity;

        @Test
        void shouldRenderTaskDescription() throws IOException {
            // Given
            UUID partyId = UUID.randomUUID();
            when(partyEntity.getId()).thenReturn(partyId);

            List<DocumentEntity> documentEntityList = List.of(
                DocumentEntity.builder().fileName("filename1.pdf").build(),
                DocumentEntity.builder().fileName("filename2.csv").build()
            );

            String expectedPartyLabel = "some party label";
            when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn(expectedPartyLabel);

            String expectedRenderedContent = "some rendered content";
            PebbleTemplate pebbleTemplate = stubPebbleTemplate(
                "claim-review-additional-docs",
                expectedRenderedContent
            );

            // When
            String description = underTest
                .createClaimAdditionalDocumentsDescription(
                    CASE_REFERENCE,
                    mainClaim,
                    partyEntity,
                    documentEntityList
                );

            // Then
            assertThat(description).isEqualTo(expectedRenderedContent);

            verify(pebbleTemplate).evaluate(isA(StringWriter.class), contextMapCaptor.capture());
            Map<String, Object> contextMap = contextMapCaptor.getValue();
            assertThat(contextMap)
                .containsEntry("caseReference", CASE_REFERENCE)
                .containsEntry("partyLabel", expectedPartyLabel)
                .containsEntry("filenames", List.of("filename1.pdf", "filename2.csv"));
        }

        @Test
        void shouldThrowExceptionWhenUnableToRenderTemplate() throws IOException {
            // Given
            PebbleTemplate pebbleTemplate = stubPebbleTemplate(
                "claim-review-additional-docs",
                "some content"
            );

            IOException pebbleException = mock(IOException.class);
            doThrow(pebbleException).when(pebbleTemplate).evaluate(any(StringWriter.class), anyMap());

            UUID partyId = UUID.randomUUID();
            when(partyEntity.getId()).thenReturn(partyId);
            when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn("some party label");

            // When
            Throwable throwable = catchThrowable(
                () -> underTest.createClaimAdditionalDocumentsDescription(
                    CASE_REFERENCE,
                    mainClaim,
                    partyEntity,
                    List.of()
                ));

            // Then
            assertThat(throwable)
                .isInstanceOf(TemplateRenderingException.class)
                .hasMessage("Failed to render template")
                .hasCause(pebbleException);
        }
    }

    @Nested
    @DisplayName("Get description for Counter Claim Additional Documents task")
    class CounterClaimAdditionalDocumentsTests {
        @Mock
        private ClaimEntity mainClaim;
        @Mock
        private PartyEntity partyEntity;

        @Test
        void shouldRenderTaskDescription() throws IOException {
            // Given
            UUID partyId = UUID.randomUUID();
            when(partyEntity.getId()).thenReturn(partyId);

            List<DocumentEntity> documentEntityList = List.of(
                DocumentEntity.builder().fileName("filename1.pdf").build(),
                DocumentEntity.builder().fileName("filename2.csv").build()
            );

            String expectedPartyLabel = "some party label";
            when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn(expectedPartyLabel);

            String expectedRenderedContent = "some rendered content";
            PebbleTemplate pebbleTemplate = stubPebbleTemplate(
                "counterclaim-review-additional-docs",
                expectedRenderedContent
            );

            // When
            String description = underTest
                .createCounterClaimAdditionalDocumentsDescription(
                    CASE_REFERENCE,
                    mainClaim,
                    partyEntity,
                    documentEntityList
                );

            // Then
            assertThat(description).isEqualTo(expectedRenderedContent);

            verify(pebbleTemplate).evaluate(isA(StringWriter.class), contextMapCaptor.capture());
            Map<String, Object> contextMap = contextMapCaptor.getValue();
            assertThat(contextMap)
                .containsEntry("caseReference", CASE_REFERENCE)
                .containsEntry("partyLabel", expectedPartyLabel)
                .containsEntry("filenames", List.of("filename1.pdf", "filename2.csv"));
        }

        @Test
        void shouldThrowExceptionWhenUnableToRenderTemplate() throws IOException {
            // Given
            PebbleTemplate pebbleTemplate = stubPebbleTemplate(
                "counterclaim-review-additional-docs",
                "some content"
            );

            IOException pebbleException = mock(IOException.class);
            doThrow(pebbleException).when(pebbleTemplate).evaluate(any(StringWriter.class), anyMap());

            UUID partyId = UUID.randomUUID();
            when(partyEntity.getId()).thenReturn(partyId);
            when(partyService.getPartyLabel(mainClaim, partyId)).thenReturn("some party label");

            // When
            Throwable throwable = catchThrowable(
                () -> underTest.createCounterClaimAdditionalDocumentsDescription(
                    CASE_REFERENCE,
                    mainClaim,
                    partyEntity,
                    List.of()
                ));

            // Then
            assertThat(throwable)
                .isInstanceOf(TemplateRenderingException.class)
                .hasMessage("Failed to render template")
                .hasCause(pebbleException);
        }
    }

    private PebbleTemplate stubPebbleTemplate(String templateName, String renderedContent) throws IOException {
        PebbleTemplate pebbleTemplate = mock(PebbleTemplate.class, withSettings().strictness(Strictness.LENIENT));
        when(pebbleEngine.getTemplate("workallocation/" + templateName)).thenReturn(pebbleTemplate);
        doAnswer(invocationOnMock -> {
            StringWriter stringWriter = invocationOnMock.getArgument(0);
            stringWriter.write(renderedContent);
            return null;
        }).when(pebbleTemplate).evaluate(isA(StringWriter.class), anyMap());

        return pebbleTemplate;
    }
}
