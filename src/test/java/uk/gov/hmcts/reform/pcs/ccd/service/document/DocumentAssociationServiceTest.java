package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.COUNTERCLAIM_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.GEN_APP_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.NONE_PREFIX;

@ExtendWith(MockitoExtension.class)
class DocumentAssociationServiceTest {

    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final UUID GEN_APP_ID = UUID.randomUUID();
    private static final UUID COUNTERCLAIM_ID = UUID.randomUUID();

    @Mock
    private DocumentService documentService;
    @Mock
    private DocumentNameService documentNameService;
    @Mock
    private GenAppService genAppService;
    @Mock
    private CounterClaimRepository counterClaimRepository;

    private DocumentAssociationService underTest;
    private DocumentEntity documentEntity;
    private ClaimEntity claimEntity;

    @BeforeEach
    void setUp() {
        underTest = new DocumentAssociationService(
            documentService,
            documentNameService,
            genAppService,
            counterClaimRepository
        );
        documentEntity = DocumentEntity.builder().build();
        claimEntity = new ClaimEntity();
    }

    @Test
    void shouldAssociateDocumentWithGeneralApplication() {
        GenAppEntity genApp = GenAppEntity.builder().id(GEN_APP_ID).build();
        when(genAppService.loadGenApp(GEN_APP_ID)).thenReturn(genApp);
        when(documentNameService.appendGenAppPostfix("evidence.pdf", genApp, claimEntity, PARTY_ID))
            .thenReturn("evidence GA1 - Defendant 1.pdf");

        String fileName = underTest.applyAssociation(
            documentEntity,
            claimEntity,
            PARTY_ID,
            null,
            "evidence.pdf",
            VerticalYesNo.YES,
            dynamicStringList(GEN_APP_ID_PREFIX + ":" + GEN_APP_ID)
        );

        assertThat(fileName).isEqualTo("evidence GA1 - Defendant 1.pdf");
        assertThat(documentEntity.getGeneralApplication()).isSameAs(genApp);
        assertThat(documentEntity.getCounterClaim()).isNull();
        assertThat(documentEntity.getCategoryId()).isEqualTo(CaseFileCategory.APPLICATIONS.getId());
    }

    @Test
    void shouldAssociateDocumentWithCounterclaim() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder().id(COUNTERCLAIM_ID).build();
        when(counterClaimRepository.getReferenceById(COUNTERCLAIM_ID)).thenReturn(counterClaim);
        when(documentNameService.appendCounterClaimPostfix("evidence.pdf", claimEntity, PARTY_ID))
            .thenReturn("evidence - Defendant 1.pdf");

        String fileName = underTest.applyAssociation(
            documentEntity,
            claimEntity,
            PARTY_ID,
            null,
            "evidence.pdf",
            VerticalYesNo.YES,
            dynamicStringList(COUNTERCLAIM_ID_PREFIX + ":" + COUNTERCLAIM_ID)
        );

        assertThat(fileName).isEqualTo("evidence - Defendant 1.pdf");
        assertThat(documentEntity.getCounterClaim()).isSameAs(counterClaim);
        assertThat(documentEntity.getGeneralApplication()).isNull();
        assertThat(documentEntity.getCategoryId()).isEqualTo(CaseFileCategory.STATEMENTS_OF_CASE.getId());
    }

    @Test
    void shouldApplyDocumentTypeAssociationWhenNotRelatedToSubmission() {
        documentEntity.setGeneralApplication(GenAppEntity.builder().build());
        documentEntity.setCounterClaim(CounterClaimEntity.builder().build());
        when(documentService.categoryIdForDocumentType(DocumentType.RENT_STATEMENT))
            .thenReturn(CaseFileCategory.PROPERTY_DOCUMENTS.getId());
        when(documentNameService.appendPartyPostfix("rent statement.pdf", claimEntity, PARTY_ID))
            .thenReturn("rent statement - Defendant 1.pdf");

        String fileName = underTest.applyAssociation(
            documentEntity,
            claimEntity,
            PARTY_ID,
            DocumentType.RENT_STATEMENT,
            "rent statement.pdf",
            VerticalYesNo.YES,
            dynamicStringList(NONE_PREFIX)
        );

        assertThat(fileName).isEqualTo("rent statement - Defendant 1.pdf");
        assertThat(documentEntity.getGeneralApplication()).isNull();
        assertThat(documentEntity.getCounterClaim()).isNull();
        assertThat(documentEntity.getCategoryId()).isEqualTo(CaseFileCategory.PROPERTY_DOCUMENTS.getId());
    }

    @Test
    void shouldApplyDocumentTypeAssociationWhenRelatedSubmissionListIsNotShown() {
        when(documentService.categoryIdForDocumentType(DocumentType.WITNESS_STATEMENT))
            .thenReturn(CaseFileCategory.EVIDENCE.getId());
        when(documentNameService.appendPartyPostfix("statement.pdf", claimEntity, PARTY_ID))
            .thenReturn("statement - Claimant 1.pdf");

        String fileName = underTest.applyAssociation(
            documentEntity,
            claimEntity,
            PARTY_ID,
            DocumentType.WITNESS_STATEMENT,
            "statement.pdf",
            VerticalYesNo.NO,
            null
        );

        assertThat(fileName).isEqualTo("statement - Claimant 1.pdf");
        assertThat(documentEntity.getCategoryId()).isEqualTo(CaseFileCategory.EVIDENCE.getId());
    }

    @Test
    void shouldRejectMissingRelatedSubmissionWhenRelatedSubmissionListIsShown() {
        assertThatThrownBy(() -> underTest.applyAssociation(
            documentEntity,
            claimEntity,
            PARTY_ID,
            DocumentType.WITNESS_STATEMENT,
            "statement.pdf",
            VerticalYesNo.YES,
            null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Related submission must be selected");
    }

    @Test
    void shouldRejectInvalidRelatedSubmissionCode() {
        assertThatThrownBy(() -> underTest.applyAssociation(
            documentEntity,
            claimEntity,
            PARTY_ID,
            DocumentType.WITNESS_STATEMENT,
            "statement.pdf",
            VerticalYesNo.YES,
            dynamicStringList(GEN_APP_ID_PREFIX)
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid related submission: " + GEN_APP_ID_PREFIX);
    }

    @Test
    void shouldRejectUnexpectedRelatedSubmissionPrefix() {
        String unexpectedPrefix = "UNKNOWN";

        assertThatThrownBy(() -> underTest.applyAssociation(
            documentEntity,
            claimEntity,
            PARTY_ID,
            DocumentType.WITNESS_STATEMENT,
            "statement.pdf",
            VerticalYesNo.YES,
            dynamicStringList(unexpectedPrefix + ":" + UUID.randomUUID())
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unexpected related submission: " + unexpectedPrefix);
    }

    private static DynamicStringList dynamicStringList(String code) {
        return DynamicStringList.builder()
            .value(DynamicStringListElement.builder()
                .code(code)
                .build())
            .build();
    }
}
