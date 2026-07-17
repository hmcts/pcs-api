package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.COUNTERCLAIM_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.GEN_APP_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.NONE_PREFIX;

@ExtendWith(MockitoExtension.class)
class DocumentAmendServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final UUID GEN_APP_ID = UUID.randomUUID();
    private static final UUID COUNTERCLAIM_ID = UUID.randomUUID();

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentService documentService;
    @Mock
    private DocumentNameService documentNameService;
    @Mock
    private GenAppService genAppService;
    @Mock
    private CounterClaimRepository counterClaimRepository;
    @Mock
    private PartyService partyService;
    @Captor
    private ArgumentCaptor<DocumentEntity> documentCaptor;

    private DocumentAmendService underTest;
    private PcsCaseEntity pcsCaseEntity;
    private ClaimEntity mainClaim;
    private PartyEntity partyEntity;
    private DocumentEntity documentEntity;

    @BeforeEach
    void setUp() {
        underTest = new DocumentAmendService(
            pcsCaseService,
            documentRepository,
            documentService,
            documentNameService,
            genAppService,
            counterClaimRepository,
            partyService
        );

        mainClaim = new ClaimEntity();
        pcsCaseEntity = PcsCaseEntity.builder().build();
        pcsCaseEntity.addClaim(mainClaim);
        partyEntity = PartyEntity.builder()
            .id(PARTY_ID)
            .firstName("Defendant")
            .lastName("One")
            .build();
        mainClaim.addParty(partyEntity, PartyRole.DEFENDANT);

        documentEntity = DocumentEntity.builder()
            .id(DOCUMENT_ID)
            .fileName("old file.pdf")
            .build();

        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(documentEntity));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(partyService.getPartyEntityById(PARTY_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyService.getPartyName(partyEntity)).thenReturn("Defendant One");
    }

    @Test
    void shouldUpdateDocumentDetailsAndRegenerateFilenameWhenNotRelatedToSubmission() {
        LocalDate issueDate = LocalDate.of(2021, 4, 16);
        DocumentAmendDetails amendDetails = baseDetails()
            .amendedFileName("rent statement.docx")
            .issueDate(issueDate)
            .showRelatedSubmissionsList(VerticalYesNo.YES)
            .relatedSubmission(dynamicStringList(NONE_PREFIX))
            .relatedSubmissionsDocumentType(dynamicStringList(CaseworkerDocumentType.RENT_STATEMENT.name()))
            .build();

        when(documentService.mapCaseworkerDocumentTypeToDocumentType(CaseworkerDocumentType.RENT_STATEMENT))
            .thenReturn(DocumentType.RENT_STATEMENT);
        when(documentService.categoryIdForDocumentType(DocumentType.RENT_STATEMENT))
            .thenReturn(CaseFileCategory.PROPERTY_DOCUMENTS.getId());
        when(documentNameService.appendDate("rent statement.pdf", issueDate))
            .thenReturn("rent statement 16042021.pdf");
        when(documentNameService.appendPartyPostfix("rent statement 16042021.pdf", mainClaim, PARTY_ID))
            .thenReturn("rent statement 16042021 - Defendant 1.pdf");

        DocumentAmendService.AmendedDocument amendedDocument =
            underTest.amendDocument(PCSCase.builder().documentAmendDetails(amendDetails).build(), CASE_REFERENCE);

        verify(documentRepository).save(documentCaptor.capture());
        DocumentEntity savedDocument = documentCaptor.getValue();

        assertThat(savedDocument.getFileName()).isEqualTo("rent statement 16042021 - Defendant 1.pdf");
        assertThat(savedDocument.getType()).isEqualTo(DocumentType.RENT_STATEMENT);
        assertThat(savedDocument.getCategoryId()).isEqualTo(CaseFileCategory.PROPERTY_DOCUMENTS.getId());
        assertThat(savedDocument.getIssueDate()).isEqualTo(issueDate);
        assertThat(savedDocument.getParty()).isSameAs(partyEntity);
        assertThat(savedDocument.getGeneralApplication()).isNull();
        assertThat(savedDocument.getCounterClaim()).isNull();
        assertThat(amendedDocument.fileName()).isEqualTo("rent statement 16042021");
        assertThat(amendedDocument.partyName()).isEqualTo("Defendant One");
    }

    @Test
    void shouldStoreGenAppDocumentInApplicationsWithoutShowingReferenceInConfirmationName() {
        GenAppEntity genAppEntity = GenAppEntity.builder().id(GEN_APP_ID).build();
        DocumentAmendDetails amendDetails = baseDetails()
            .amendedFileName("application evidence.pdf")
            .showRelatedSubmissionsList(VerticalYesNo.YES)
            .relatedSubmission(dynamicStringList(GEN_APP_ID_PREFIX + ":" + GEN_APP_ID))
            .build();

        when(genAppService.loadGenApp(GEN_APP_ID)).thenReturn(genAppEntity);
        when(documentNameService.appendGenAppPostfix("application evidence.pdf", genAppEntity, mainClaim, PARTY_ID))
            .thenReturn("application evidence GA1 - Defendant 1.pdf");

        DocumentAmendService.AmendedDocument amendedDocument =
            underTest.amendDocument(PCSCase.builder().documentAmendDetails(amendDetails).build(), CASE_REFERENCE);

        verify(documentRepository).save(documentCaptor.capture());
        DocumentEntity savedDocument = documentCaptor.getValue();

        assertThat(savedDocument.getFileName()).isEqualTo("application evidence GA1 - Defendant 1.pdf");
        assertThat(savedDocument.getCategoryId()).isEqualTo(CaseFileCategory.APPLICATIONS.getId());
        assertThat(savedDocument.getGeneralApplication()).isSameAs(genAppEntity);
        assertThat(savedDocument.getCounterClaim()).isNull();
        assertThat(savedDocument.getType()).isNull();
        assertThat(amendedDocument.fileName()).isEqualTo("application evidence");
    }

    @Test
    void shouldStoreCounterclaimDocumentInStatementsOfCase() {
        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder().id(COUNTERCLAIM_ID).build();
        DocumentAmendDetails amendDetails = baseDetails()
            .amendedFileName("counterclaim evidence.pdf")
            .showRelatedSubmissionsList(VerticalYesNo.YES)
            .relatedSubmission(dynamicStringList(COUNTERCLAIM_ID_PREFIX + ":" + COUNTERCLAIM_ID))
            .build();

        when(counterClaimRepository.getReferenceById(COUNTERCLAIM_ID)).thenReturn(counterClaimEntity);
        when(documentNameService.appendCounterClaimPostfix("counterclaim evidence.pdf", mainClaim, PARTY_ID))
            .thenReturn("counterclaim evidence - Defendant 1.pdf");

        underTest.amendDocument(PCSCase.builder().documentAmendDetails(amendDetails).build(), CASE_REFERENCE);

        verify(documentRepository).save(documentCaptor.capture());
        DocumentEntity savedDocument = documentCaptor.getValue();

        assertThat(savedDocument.getFileName()).isEqualTo("counterclaim evidence - Defendant 1.pdf");
        assertThat(savedDocument.getCategoryId()).isEqualTo(CaseFileCategory.STATEMENTS_OF_CASE.getId());
        assertThat(savedDocument.getCounterClaim()).isSameAs(counterClaimEntity);
        assertThat(savedDocument.getGeneralApplication()).isNull();
        assertThat(savedDocument.getType()).isNull();
    }

    private static DocumentAmendDetails.DocumentAmendDetailsBuilder baseDetails() {
        return DocumentAmendDetails.builder()
            .selectedDocumentId(DOCUMENT_ID.toString())
            .selectedDocumentFileName("old file.pdf")
            .relatedParty(DynamicList.builder()
                .value(DynamicListElement.builder().code(PARTY_ID).build())
                .build());
    }

    private static DynamicStringList dynamicStringList(String code) {
        return DynamicStringList.builder()
            .value(DynamicStringListElement.builder().code(code).build())
            .build();
    }
}
