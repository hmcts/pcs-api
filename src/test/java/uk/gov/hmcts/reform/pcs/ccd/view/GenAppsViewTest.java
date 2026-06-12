package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentWithId;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAppsViewTest {

    private static final String ORG_ID = "org";
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private OrganisationService organisationService;
    @Mock
    private GenAppVisibilityService genAppVisibilityService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

    private PCSCase pcsCase;

    private GenAppsView underTest;

    @BeforeEach
    void setUp() {
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);
        when(genAppVisibilityService.isGenAppVisibleToUser(isA(GenAppEntity.class), eq(ORG_ID)))
            .thenReturn(true);

        pcsCase = PCSCase.builder().build();

        underTest = new GenAppsView(modelMapper, organisationService, genAppVisibilityService);
    }

    @Test
    void shouldOrderGenAppsBySubmissionDateDescending() {
        // Given
        UUID genApp1Id = UUID.randomUUID();
        LocalDateTime genApp1SubmittedDate = LocalDateTime.parse("2026-05-02T15:00:00");
        GenAppEntity genAppEntity1 = createGenAppEntity(genApp1Id, genApp1SubmittedDate);

        UUID genApp2Id = UUID.randomUUID();
        LocalDateTime genApp2SubmittedDate = LocalDateTime.parse("2026-05-04T10:00:00");
        GenAppEntity genAppEntity2 = createGenAppEntity(genApp2Id, genApp2SubmittedDate);

        UUID genApp3Id = UUID.randomUUID();
        LocalDateTime genApp3SubmittedDate = LocalDateTime.parse("2026-05-04T09:00:00");
        GenAppEntity genAppEntity3 = createGenAppEntity(genApp3Id, genApp3SubmittedDate);

        when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(genAppEntity1, genAppEntity2, genAppEntity3));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<GeneralApplication>> genApps = pcsCase.getGenApps();

        assertThat(genApps)
            .extracting(ListValue::getId)
            .containsExactly(genApp2Id.toString(), genApp3Id.toString(), genApp1Id.toString());

        assertThat(genApps)
            .extracting(ListValue::getValue)
            .extracting(GeneralApplication::getSubmittedOn)
            .containsExactly(genApp2SubmittedDate, genApp3SubmittedDate, genApp1SubmittedDate);
    }

    @Test
    void shouldHideGenAppsFromOtherPartiesIfTheyShouldNotSeeThem() {
        // Given
        UUID genApp1Id = UUID.randomUUID();
        LocalDateTime genApp1SubmittedDate = LocalDateTime.parse("2026-05-02T15:00:00");
        GenAppEntity genAppEntity1 = createGenAppEntity(genApp1Id, genApp1SubmittedDate);
        when(genAppVisibilityService.isGenAppVisibleToUser(genAppEntity1, ORG_ID)).thenReturn(true);

        UUID genApp2Id = UUID.randomUUID();
        LocalDateTime genApp2SubmittedDate = LocalDateTime.parse("2026-05-04T10:00:00");
        GenAppEntity genAppEntity2 = createGenAppEntity(genApp2Id, genApp2SubmittedDate);
        when(genAppVisibilityService.isGenAppVisibleToUser(genAppEntity2, ORG_ID)).thenReturn(false);

        UUID genApp3Id = UUID.randomUUID();
        LocalDateTime genApp3SubmittedDate = LocalDateTime.parse("2026-05-04T09:00:00");
        GenAppEntity genAppEntity3 = createGenAppEntity(genApp3Id, genApp3SubmittedDate);
        when(genAppVisibilityService.isGenAppVisibleToUser(genAppEntity3, ORG_ID)).thenReturn(true);

        when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(genAppEntity1, genAppEntity2, genAppEntity3));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<GeneralApplication>> genApps = pcsCase.getGenApps();

        assertThat(genApps)
            .extracting(ListValue::getId)
            .containsExactlyInAnyOrder(genApp1Id.toString(), genApp3Id.toString());

    }

    @Test
    void shouldMapPartyDetails() {
        // Given
        PartyEntity currentParty = createPartyEntity();
        currentParty.setId(UUID.randomUUID());
        currentParty.setIdamId(UUID.randomUUID());
        currentParty.setFirstName("Current party first name");
        currentParty.setLastName("Current party last name");

        PartyEntity otherParty = createPartyEntity();
        otherParty.setId(UUID.randomUUID());
        otherParty.setIdamId(null);
        otherParty.setFirstName("Other party first name");
        otherParty.setLastName("Other party last name");

        UUID genApp1Id = UUID.randomUUID();
        LocalDateTime genApp1SubmittedDate = LocalDateTime.parse("2026-05-02T15:00:00");
        GenAppEntity genAppEntity1 = createGenAppEntity(genApp1Id, genApp1SubmittedDate);
        genAppEntity1.setParty(currentParty);

        UUID genApp2Id = UUID.randomUUID();
        LocalDateTime genApp2SubmittedDate = LocalDateTime.parse("2026-05-01T10:00:00");
        GenAppEntity genAppEntity2 = createGenAppEntity(genApp2Id, genApp2SubmittedDate);
        genAppEntity2.setParty(otherParty);

        when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(genAppEntity1, genAppEntity2));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<GeneralApplication>> genApps = pcsCase.getGenApps();

        assertThat(genApps).hasSize(2);

        Party genApp1Party = genApps.get(0).getValue().getParty();
        Party genApp2Party = genApps.get(1).getValue().getParty();

        assertThat(genApp1Party.getId()).isEqualTo(currentParty.getId().toString());
        assertThat(genApp1Party.getIdamId()).isEqualTo(currentParty.getIdamId().toString());
        assertThat(genApp1Party.getFirstName()).isEqualTo("Current party first name");
        assertThat(genApp1Party.getLastName()).isEqualTo("Current party last name");

        assertThat(genApp2Party.getId()).isEqualTo(otherParty.getId().toString());
        assertThat(genApp2Party.getIdamId()).isNull();
        assertThat(genApp2Party.getFirstName()).isEqualTo("Other party first name");
        assertThat(genApp2Party.getLastName()).isEqualTo("Other party last name");
    }

    @Test
    void shouldSetSubmissionDocument() {
        // Given
        UUID pcsDocumentId = UUID.randomUUID();

        LocalDateTime genApp1SubmittedDate = LocalDateTime.parse("2026-05-02T15:00:00");
        GenAppEntity genAppEntity1 = createGenAppEntity(UUID.randomUUID(), genApp1SubmittedDate);
        DocumentEntity submissionDocumentEntity = mock(DocumentEntity.class);
        genAppEntity1.setSubmissionDocument(submissionDocumentEntity);

        final Document expectedSubmissionDocument = stubDocument(submissionDocumentEntity, pcsDocumentId);

        when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(genAppEntity1));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<GeneralApplication>> genApps = pcsCase.getGenApps();

        assertThat(genApps).hasSize(1);

        DocumentWithId actualSubmissionDocument = genApps.getFirst().getValue().getSubmissionDocument();

        assertThat(actualSubmissionDocument.getId()).isEqualTo(pcsDocumentId.toString());
        assertThat(actualSubmissionDocument.getDocument()).isEqualTo(expectedSubmissionDocument);
    }

    @Test
    void shouldSetSupporingDocsDocument() {
        // Given
        UUID pcsDocumentId1 = UUID.randomUUID();
        UUID pcsDocumentId2 = UUID.randomUUID();

        LocalDateTime genAppSubmittedDate = LocalDateTime.parse("2026-05-02T15:00:00");
        GenAppEntity genAppEntity = createGenAppEntity(UUID.randomUUID(), genAppSubmittedDate);
        when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(genAppEntity));
        DocumentEntity documentEntity1 = mock(DocumentEntity.class);
        DocumentEntity documentEntity2 = mock(DocumentEntity.class);

        final Document expectedSupportingDocument1 = stubDocument(documentEntity1, pcsDocumentId1);
        final Document expectedSupportingDocument2 = stubDocument(documentEntity2, pcsDocumentId2);

        genAppEntity.setDocuments(List.of(documentEntity1, documentEntity2));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<GeneralApplication>> genApps = pcsCase.getGenApps();

        assertThat(genApps).hasSize(1);

        List<ListValue<Document>> actualSupportingDocuments = genApps.getFirst().getValue().getSupportingDocuments();

        assertThat(actualSupportingDocuments).hasSize(2);
        assertThat(actualSupportingDocuments.get(0).getId()).isEqualTo(pcsDocumentId1.toString());
        assertThat(actualSupportingDocuments.get(0).getValue()).isEqualTo(expectedSupportingDocument1);

        assertThat(actualSupportingDocuments.get(1).getId()).isEqualTo(pcsDocumentId2.toString());
        assertThat(actualSupportingDocuments.get(1).getValue()).isEqualTo(expectedSupportingDocument2);

    }

    private Document stubDocument(DocumentEntity documentEntity, UUID pcsDocumentId) {
        when(documentEntity.getId()).thenReturn(pcsDocumentId);

        Document document = mock(Document.class);
        when(modelMapper.map(documentEntity, Document.class)).thenReturn(document);
        return document;
    }

    private static PartyEntity createPartyEntity() {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .build();
    }

    private static GenAppEntity createGenAppEntity(UUID id, LocalDateTime submittedDate) {
        return GenAppEntity.builder()
            .id(id)
            .applicationSubmittedDate(submittedDate)
            .build();
    }
}
