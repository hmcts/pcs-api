package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentsViewTest {

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private GenAppVisibilityService genAppVisibilityService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

    private PCSCase pcsCase;

    private DocumentsView underTest;

    @BeforeEach
    void setUp() {
        pcsCase = PCSCase.builder().build();

        underTest = new DocumentsView(securityContextService, genAppVisibilityService);
    }

    @Test
    void shouldMapDocuments() {
        // Given
        Instant submittedDate = Instant.parse("2026-05-14T09:30:00Z");
        UUID document1Id = UUID.randomUUID();
        DocumentEntity entity1 = DocumentEntity.builder()
            .id(document1Id)
            .fileName("doc1.pdf")
            .url("url1")
            .binaryUrl("binary url1")
            .categoryId("category 1")
            .submittedDate(submittedDate)
            .build();

        UUID document2Id = UUID.randomUUID();
        DocumentEntity entity2 = DocumentEntity.builder()
            .id(document2Id)
            .fileName("doc2.pdf")
            .url("url2")
            .binaryUrl("binary url2")
            .categoryId("category 2")
            .build();

        when(pcsCaseEntity.getDocuments()).thenReturn(List.of(entity1, entity2));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<Document>> allDocuments = pcsCase.getAllDocuments();
        assertThat(allDocuments).hasSize(2);

        ListValue<Document> document1ListValue = allDocuments.get(0);
        ListValue<Document> document2ListValue = allDocuments.get(1);

        assertThat(document1ListValue.getId()).isEqualTo(document1Id.toString());
        assertThat(document1ListValue.getValue())
            .satisfies(
                document -> {
                    assertThat(document.getFilename()).isEqualTo("doc1.pdf");
                    assertThat(document.getUrl()).isEqualTo("url1");
                    assertThat(document.getBinaryUrl()).isEqualTo("binary url1");
                    assertThat(document.getCategoryId()).isEqualTo("category 1");
                    assertThat(document.getUploadTimestamp())
                        .isEqualTo(LocalDateTime.of(2026, 5, 14, 9, 30));
                }
            );

        assertThat(document2ListValue.getId()).isEqualTo(document2Id.toString());
        assertThat(document2ListValue.getValue())
            .satisfies(
                document -> {
                    assertThat(document.getFilename()).isEqualTo("doc2.pdf");
                    assertThat(document.getUrl()).isEqualTo("url2");
                    assertThat(document.getBinaryUrl()).isEqualTo("binary url2");
                    assertThat(document.getCategoryId()).isEqualTo("category 2");
                    assertThat(document.getUploadTimestamp()).isNull();
                }
            );

    }

    @Test
    void shouldReturnEmptyListWhenNoDocumentsExist() {
        // Given
        when(pcsCaseEntity.getDocuments()).thenReturn(List.of());

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getAllDocuments()).isEmpty();
    }

    @Test
    void shoulFilterGenAppDocumentsBasedOnVisibilitty() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(CURRENT_USER_ID);

        GenAppEntity genAppEntity1 = mock(GenAppEntity.class);
        when(genAppVisibilityService.isGenAppVisibleToUser(genAppEntity1, CURRENT_USER_ID))
            .thenReturn(true);

        GenAppEntity genAppEntity2 = mock(GenAppEntity.class);
        when(genAppVisibilityService.isGenAppVisibleToUser(genAppEntity2, CURRENT_USER_ID))
            .thenReturn(false);

        UUID document1Id = UUID.randomUUID();
        DocumentEntity documentEntity1 = DocumentEntity.builder()
            .id(document1Id)
            .url("url1")
            .generalApplication(genAppEntity1)
            .build();

        UUID document2Id = UUID.randomUUID();
        DocumentEntity documentEntity2 = DocumentEntity.builder()
            .id(document2Id)
            .url("url2")
            .generalApplication(genAppEntity2)
            .build();

        UUID document3Id = UUID.randomUUID();
        DocumentEntity documentEntity3 = DocumentEntity.builder()
            .id(document3Id)
            .url("url3")
            .generalApplication(genAppEntity1)
            .build();

        UUID document4Id = UUID.randomUUID();
        DocumentEntity documentEntity4 = DocumentEntity.builder()
            .id(document4Id)
            .url("url4")
            .generalApplication(null)
            .build();

        when(pcsCaseEntity.getDocuments()).thenReturn(
            List.of(documentEntity1, documentEntity2, documentEntity3, documentEntity4));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<Document>> allDocuments = pcsCase.getAllDocuments();
        assertThat(allDocuments)
            .extracting(ListValue::getValue)
            .extracting(Document::getUrl)
            .containsExactly("url1", "url3", "url4");
    }

    @ParameterizedTest(name = "[{index}] description={0} => isEmpty={1}")
    @MethodSource("descriptionProvider")
    void shouldCheckIfDescriptionIsEmpty(String description, boolean expectedEmpty) {
        // Given
        DocumentEntity documentEntity = DocumentEntity.builder()
                .description(description)
                .build();

        // When
        boolean result = DocumentsView.isDescriptionEmpty(documentEntity);

        // Then
        assertThat(result).isEqualTo(expectedEmpty);
    }

    private static Stream<Arguments> descriptionProvider() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of("", true),
                Arguments.of("   ", true),
                Arguments.of("Valid description text", false)
        );
    }
}
