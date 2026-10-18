package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementDocumentVisibilityTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String ENFORCEMENT = FeatureCaseFileCategory.ENFORCEMENT.getId();
    private static final UUID CLAIM_FORM_ID = UUID.randomUUID();
    private static final UUID EVIDENCE_ID = UUID.randomUUID();
    private static final UUID WARRANT_ID = UUID.randomUUID();
    private static final UUID NOTICE_OF_APPOINTMENT_ID = UUID.randomUUID();

    @Mock
    private UserRoleService userRoleService;

    @InjectMocks
    private EnforcementDocumentVisibility underTest;

    private final PcsCaseEntity pcsCaseEntity = caseWithDocuments(
        entity(CLAIM_FORM_ID, CaseFileCategory.STATEMENTS_OF_CASE.getId(), DocumentType.CLAIM),
        entity(EVIDENCE_ID, ENFORCEMENT, DocumentType.PHOTOGRAPHIC_EVIDENCE),
        entity(WARRANT_ID, ENFORCEMENT, DocumentType.WARRANT_OF_POSSESSION),
        entity(NOTICE_OF_APPOINTMENT_ID, ENFORCEMENT, DocumentType.NOTICE_OF_APPOINTMENT)
    );

    private final ListValue<Document> claimForm =
        document(CLAIM_FORM_ID, "claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE.getId());
    private final ListValue<Document> evidence = document(EVIDENCE_ID, "evidence.pdf", ENFORCEMENT);
    private final ListValue<Document> warrant = document(WARRANT_ID, "warrant.pdf", ENFORCEMENT);
    private final ListValue<Document> noticeOfAppointment =
        document(NOTICE_OF_APPOINTMENT_ID, "appointment.pdf", ENFORCEMENT);

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"DEFENDANT", "DEFENDANT_SOLICITOR", "GA_DEFENDANT_SOLICITOR"})
    void shouldHideEnforcementDocumentsFromDefendants(UserRole defendantRole) {
        // Given
        PCSCase pcsCase = caseWith(claimForm, evidence, warrant, noticeOfAppointment);
        givenCurrentUserHasRoles("citizen", defendantRole.getRole());

        // When
        underTest.restrict(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CREATOR", "CLAIMANT_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "CLAIMANT"})
    void shouldHideOnlyTheInternalEnforcementDocumentsFromTheClaimantSide(UserRole claimantRole) {
        // Given
        PCSCase pcsCase = caseWith(claimForm, evidence, warrant, noticeOfAppointment);
        givenCurrentUserHasRoles(claimantRole.getRole());

        // When
        underTest.restrict(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm, evidence, noticeOfAppointment);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "JUDGE", "CTSC_ADMIN", "HEARING_CENTRE_ADMIN", "PCS_CASE_WORKER", "SYSTEM_USER", "BAILIFF"
    })
    void shouldShowEveryEnforcementDocumentToInternalUsers(UserRole role) {
        // Given
        PCSCase pcsCase = caseWith(claimForm, evidence, warrant, noticeOfAppointment);
        givenCurrentUserHasRoles(role.getRole());

        // When
        underTest.restrict(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm, evidence, warrant, noticeOfAppointment);
    }

    @Test
    void shouldNotLookUpRolesWhenThereAreNoEnforcementDocuments() {
        // Given
        PCSCase pcsCase = caseWith(claimForm);

        // When
        underTest.restrict(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm);
        verifyNoInteractions(userRoleService);
    }

    @Test
    void shouldLeaveACaseWithNoDocumentsAlone() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.restrict(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).isNull();
        verifyNoInteractions(userRoleService);
    }

    private void givenCurrentUserHasRoles(String... roles) {
        when(userRoleService.getCurrentUserCaseRoles(CASE_REFERENCE))
            .thenReturn(new UserRoles("user-id", List.of(roles)));
    }

    @SafeVarargs
    private static PCSCase caseWith(ListValue<Document>... documents) {
        return PCSCase.builder().allDocuments(List.of(documents)).build();
    }

    private static PcsCaseEntity caseWithDocuments(DocumentEntity... documents) {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setDocuments(new ArrayList<>(List.of(documents)));
        return pcsCaseEntity;
    }

    private static DocumentEntity entity(UUID id, String categoryId, DocumentType type) {
        return DocumentEntity.builder().id(id).categoryId(categoryId).type(type).build();
    }

    private static ListValue<Document> document(UUID id, String filename, String categoryId) {
        return ListValue.<Document>builder()
            .id(id.toString())
            .value(Document.builder()
                       .filename(filename)
                       .url("http://dm-store/documents/" + id)
                       .categoryId(categoryId)
                       .build())
            .build();
    }
}
