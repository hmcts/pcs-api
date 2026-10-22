package uk.gov.hmcts.reform.pcs.ccd.service.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.TenancyLicenceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentCategoryUpdateServiceTest {

    private static final long CASE_REFERENCE = 12345L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private DocumentRepository documentRepository;

    private DocumentCategoryUpdateService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentCategoryUpdateService(pcsCaseService, documentRepository,
                                                      new JacksonConfiguration().getMapper());
    }

    @Test
    void shouldPersistSuppliedCategoriesAndPreserveDocumentDetails() {
        DocumentEntity first = document("propertyDocuments");
        DocumentEntity second = document("evidence");
        existingCase(first, second);

        underTest.updateCategories(
            CASE_REFERENCE, caseData(submitted(first, "correspondence"), submitted(second, null)));

        assertThat(first.getCategoryId()).isEqualTo("correspondence");
        assertThat(second.getCategoryId()).isNull();
        assertThat(first.getFileName()).isEqualTo("original.pdf");
        assertThat(first.getUrl()).isEqualTo("http://documents/" + first.getId());
        verify(documentRepository).save(first);
        verify(documentRepository).save(second);
        verify(pcsCaseService).loadCase(CASE_REFERENCE);
        verifyNoMoreInteractions(pcsCaseService, documentRepository);
    }

    @Test
    void shouldLeaveUnchangedAndOmittedDocuments() {
        DocumentEntity unchanged = document("evidence");
        DocumentEntity omitted = document("propertyDocuments");
        existingCase(unchanged, omitted);

        underTest.updateCategories(CASE_REFERENCE, caseData(submitted(unchanged, "evidence")));

        assertThat(omitted.getCategoryId()).isEqualTo("propertyDocuments");
        verifyNoInteractions(documentRepository);
    }

    @Test
    void shouldOnlyUpdateDocumentsOnCase() {
        DocumentEntity document = document("evidence");
        existingCase(document);

        underTest.updateCategories(CASE_REFERENCE, caseData(submitted(document("evidence"), "correspondence")));

        assertThat(document.getCategoryId()).isEqualTo("evidence");
        verifyNoInteractions(documentRepository);
    }

    @Test
    void shouldAcceptAnEmptyDocumentList() {
        existingCase(document("evidence"));

        underTest.updateCategories(CASE_REFERENCE, caseData());

        verifyNoInteractions(documentRepository);
    }

    @ParameterizedTest
    @MethodSource("dedicatedDocumentFields")
    void shouldMoveOutsideAllDocuments(Function<List<ListValue<Document>>, PCSCase> caseFactory) throws Exception {
        DocumentEntity entity = document("propertyDocuments");
        existingCase(entity);
        PCSCase caseData = caseFactory.apply(List.of(submitted(entity, "correspondence")));
        ObjectMapper mapper = new JacksonConfiguration().getMapper();
        PCSCase submitted = mapper.readValue(mapper.writeValueAsString(caseData), PCSCase.class);
        if (caseData.getCaseDetailsTab() != null) {
            assertThat(submitted.getCaseDetailsTab()).isEqualTo(caseData.getCaseDetailsTab());
        }

        underTest.updateCategories(CASE_REFERENCE, submitted);

        assertThat(entity.getCategoryId()).isEqualTo("correspondence");
        assertThat(entity.getFileName()).isEqualTo("original.pdf");
        verify(documentRepository).save(entity);
    }

    @Test
    void shouldRejectConflictingChangesBeforeSaving() {
        DocumentEntity entity = document("propertyDocuments");
        existingCase(entity);

        PCSCase caseData = PCSCase.builder()
            .allDocuments(List.of(submitted(entity, "correspondence")))
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .tenancyLicenceDocuments(List.of(submitted(entity, "evidence"))).build())
            .build();

        assertThatThrownBy(() -> underTest.updateCategories(CASE_REFERENCE, caseData))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(entity.getCategoryId()).isEqualTo("propertyDocuments");
        verifyNoInteractions(documentRepository);
    }

    private static Stream<Function<List<ListValue<Document>>, PCSCase>> dedicatedDocumentFields() {
        return Stream.of(
            docs -> PCSCase.builder().tenancyLicenceDetails(
                TenancyLicenceDetails.builder().tenancyLicenceDocuments(docs).build()).build(),
            docs -> PCSCase.builder().noticeServedDetails(
                NoticeServedDetails.builder().documents(docs).build()).build(),
            docs -> PCSCase.builder().rentArrears(
                RentArrearsSection.builder().statementDocuments(docs).build()).build(),
            docs -> PCSCase.builder().caseDetailsTab(
                CaseDetailsTab.builder().tenancyLicenceDetails(
                    TenancyLicenceTabDetails.builder().tenancyLicenceDocuments(docs)
                        .tenancyLicenceDate("1 January 2026").build()).build()).build(),
            docs -> PCSCase.builder().caseDetailsTab(
                CaseDetailsTab.builder().noticeDetails(
                    NoticeTabDetails.builder().noticeDocuments(docs).noticeServed("Yes").build()).build()).build(),
            docs -> PCSCase.builder().caseDetailsTab(
                CaseDetailsTab.builder().rentArrearsDetails(
                    RentArrearsTabDetails.builder().rentStatement(docs).rentAmount("£100").build()).build()).build()
        );
    }

    @SafeVarargs
    private PCSCase caseData(ListValue<Document>... documents) {
        return PCSCase.builder().allDocuments(List.of(documents)).build();
    }

    private void existingCase(DocumentEntity... documents) {
        PcsCaseEntity entity = new PcsCaseEntity();
        entity.getDocuments().addAll(List.of(documents));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(entity);
    }

    private DocumentEntity document(String category) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(UUID.randomUUID());
        entity.setCategoryId(category);
        entity.setFileName("original.pdf");
        entity.setUrl("http://documents/" + entity.getId());
        return entity;
    }

    private ListValue<Document> submitted(DocumentEntity entity, String category) {
        return ListValue.<Document>builder()
            .id(entity.getId().toString())
            .value(Document.builder()
                       .url(entity.getUrl())
                       .categoryId(category)
                       .filename("ignored.pdf")
                       .build())
            .build();
    }
}
