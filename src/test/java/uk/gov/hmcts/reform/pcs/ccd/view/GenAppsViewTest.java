package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAppsViewTest {

    private static final UUID CURRENT_USER_IDAM_ID = UUID.randomUUID();
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

    private PCSCase pcsCase;

    private GenAppsView underTest;

    @BeforeEach
    void setUp() {
        when(securityContextService.getCurrentUserId()).thenReturn(CURRENT_USER_IDAM_ID);

        pcsCase = PCSCase.builder().build();

        underTest = new GenAppsView(modelMapper, securityContextService);
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
    void shouldHideWithoutNoticeGenAppsFromOtherParties() {
        // Given
        final PartyEntity currentParty = createPartyEntityWithIdamId(CURRENT_USER_IDAM_ID);
        final PartyEntity otherPartyWithIdamId = createPartyEntityWithIdamId(UUID.randomUUID());
        final PartyEntity otherPartyWithoutIdamId = createPartyEntityWithIdamId(null);

        UUID genApp1Id = UUID.randomUUID();
        LocalDateTime genApp1SubmittedDate = LocalDateTime.parse("2026-05-02T15:00:00");
        GenAppEntity genAppEntity1 = createGenAppEntity(genApp1Id, genApp1SubmittedDate);
        genAppEntity1.setParty(currentParty);
        genAppEntity1.setWithoutNotice(VerticalYesNo.YES);

        UUID genApp2Id = UUID.randomUUID();
        LocalDateTime genApp2SubmittedDate = LocalDateTime.parse("2026-05-04T10:00:00");
        GenAppEntity genAppEntity2 = createGenAppEntity(genApp2Id, genApp2SubmittedDate);
        genAppEntity2.setParty(otherPartyWithIdamId);
        genAppEntity2.setWithoutNotice(VerticalYesNo.YES);

        UUID genApp3Id = UUID.randomUUID();
        LocalDateTime genApp3SubmittedDate = LocalDateTime.parse("2026-05-04T09:00:00");
        GenAppEntity genAppEntity3 = createGenAppEntity(genApp3Id, genApp3SubmittedDate);
        genAppEntity3.setParty(otherPartyWithIdamId);
        genAppEntity3.setWithoutNotice(VerticalYesNo.NO);

        UUID genApp4Id = UUID.randomUUID();
        LocalDateTime genApp4SubmittedDate = LocalDateTime.parse("2026-05-04T09:10:00");
        GenAppEntity genAppEntity4 = createGenAppEntity(genApp4Id, genApp4SubmittedDate);
        genAppEntity4.setParty(otherPartyWithoutIdamId);
        genAppEntity4.setWithoutNotice(VerticalYesNo.YES);

        when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(genAppEntity1, genAppEntity2, genAppEntity3, genAppEntity4));

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
        PartyEntity currentParty = createPartyEntityWithIdamId(CURRENT_USER_IDAM_ID);
        currentParty.setId(UUID.randomUUID());
        currentParty.setIdamId(UUID.randomUUID());
        currentParty.setFirstName("Current party first name");
        currentParty.setLastName("Current party last name");

        PartyEntity otherParty = createPartyEntityWithIdamId(UUID.randomUUID());
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
        LocalDateTime genApp1SubmittedDate = LocalDateTime.parse("2026-05-02T15:00:00");
        GenAppEntity genAppEntity1 = createGenAppEntity(UUID.randomUUID(), genApp1SubmittedDate);
        DocumentEntity submissionDocumentEntity = mock(DocumentEntity.class);
        genAppEntity1.setSubmissionDocument(submissionDocumentEntity);

        Document expectedSubmissionDocument = mock(Document.class);
        when(modelMapper.map(submissionDocumentEntity, Document.class)).thenReturn(expectedSubmissionDocument);
        when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(genAppEntity1));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<GeneralApplication>> genApps = pcsCase.getGenApps();

        assertThat(genApps).hasSize(1);

        Document actualSubmission = genApps.getFirst().getValue().getSubmissionDocument();

        assertThat(actualSubmission).isEqualTo(expectedSubmissionDocument);
    }

    private static PartyEntity createPartyEntityWithIdamId(UUID currentUserIdamId) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(currentUserIdamId)
            .build();
    }

    private static GenAppEntity createGenAppEntity(UUID id, LocalDateTime submittedDate) {
        return GenAppEntity.builder()
            .id(id)
            .applicationSubmittedDate(submittedDate)
            .build();
    }
}
