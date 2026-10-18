package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PAYMENT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;

class DefendantEnforcementRulesTest {

    @Test
    void shouldShowAWarrantOnceItsNoticeOfEvictionIsServed() {
        EnforcementOrderEntity served = warrant(AWAITING_CLAIMANT_CONFIRMATION, true);

        assertThat(DefendantEnforcementRules.visibleToDefendants(List.of(served))).containsExactly(served);
    }

    @Test
    void shouldNotShowAWarrantWhoseNoticeIsNotServedOrWasDispensedWith() {
        EnforcementOrderEntity notServed = warrant(AWAITING_CLAIMANT_CONFIRMATION, false);

        assertThat(DefendantEnforcementRules.visibleToDefendants(List.of(notServed))).isEmpty();
    }

    @Test
    void shouldKeepShowingAServedWarrantOnceItIsClosed() {
        EnforcementOrderEntity closed = warrant(CLOSED, true);

        assertThat(DefendantEnforcementRules.visibleToDefendants(List.of(closed))).containsExactly(closed);
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WRIT", "WRIT_OF_RESTITUTION"})
    void shouldNeverShowAWrit(SelectEnforcementType writType) {
        EnforcementOrderEntity writ = enforcement(writType, CLOSED, true);

        assertThat(DefendantEnforcementRules.visibleToDefendants(List.of(writ))).isEmpty();
    }

    @Test
    void shouldShowAWarrantRecordedByItsTypeAlone() {
        EnforcementOrderEntity warrant = enforcement(SelectEnforcementType.WARRANT, CLOSED, true);

        assertThat(DefendantEnforcementRules.visibleToDefendants(List.of(warrant))).containsExactly(warrant);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"CLOSED", "REJECTED", "TRANSFERRED_TO_HIGH_COURT"},
        mode = EnumSource.Mode.EXCLUDE)
    void shouldHideEverythingWhileAWarrantOfRestitutionIsLive(EnforcementStatus liveStatus) {
        EnforcementOrderEntity earlierWarrant = warrant(CLOSED, true);
        EnforcementOrderEntity restitution = warrantOfRestitution(liveStatus, true);

        assertThat(DefendantEnforcementRules.visibleToDefendants(List.of(restitution, earlierWarrant))).isEmpty();
    }

    @Test
    void shouldHideEverythingWhileAWritOfRestitutionIsLive() {
        EnforcementOrderEntity earlierWarrant = warrant(CLOSED, true);
        EnforcementOrderEntity restitution = enforcement(SelectEnforcementType.WRIT_OF_RESTITUTION,
                                                         AWAITING_PAYMENT, false);

        assertThat(DefendantEnforcementRules.visibleToDefendants(List.of(restitution, earlierWarrant))).isEmpty();
    }

    @Test
    void shouldShowServedWarrantsAgainOnceTheWarrantOfRestitutionIsNoLongerLive() {
        EnforcementOrderEntity earlierWarrant = warrant(CLOSED, true);
        EnforcementOrderEntity servedRestitution = warrantOfRestitution(CLOSED, true);
        EnforcementOrderEntity unservedRestitution = warrantOfRestitution(CLOSED, false);

        assertThat(DefendantEnforcementRules.visibleToDefendants(
            List.of(servedRestitution, unservedRestitution, earlierWarrant)))
            .containsExactly(servedRestitution, earlierWarrant);
    }

    @Test
    void shouldShowNothingForNoEnforcements() {
        assertThat(DefendantEnforcementRules.visibleToDefendants(null)).isEmpty();
        assertThat(DefendantEnforcementRules.visibleToDefendants(List.of())).isEmpty();
    }

    @Test
    void shouldOnlyShowTheNoticeOfEvictionOfAVisibleWarrant() {
        DocumentEntity notice = document(DocumentType.NOTICE_OF_EVICTION);
        DocumentEntity evidence = document(DocumentType.PHOTOGRAPHIC_EVIDENCE);
        DocumentEntity other = document(DocumentType.OTHER);
        EnforcementOrderEntity served = warrant(CLOSED, true);
        served.getDocuments().addAll(List.of(notice, evidence, other));
        DocumentEntity unservedNotice = document(DocumentType.NOTICE_OF_EVICTION);
        EnforcementOrderEntity notServed = warrant(AWAITING_CLAIMANT_CONFIRMATION, false);
        notServed.getDocuments().add(unservedNotice);

        assertThat(DefendantEnforcementRules.documentIdsVisibleToDefendants(List.of(notServed, served)))
            .containsExactly(notice.getId());
    }

    @Test
    void shouldShowNoDocumentWhileAWarrantOfRestitutionIsLive() {
        EnforcementOrderEntity served = warrant(CLOSED, true);
        served.getDocuments().add(document(DocumentType.NOTICE_OF_EVICTION));

        assertThat(DefendantEnforcementRules.documentIdsVisibleToDefendants(
            List.of(warrantOfRestitution(AWAITING_PAYMENT, false), served))).isEmpty();
    }

    private static EnforcementOrderEntity warrant(EnforcementStatus status, boolean defendantsCanSee) {
        EnforcementOrderEntity warrant = enforcement(null, status, defendantsCanSee);
        warrant.setWarrantDetails(new WarrantEntity());
        return warrant;
    }

    private static EnforcementOrderEntity warrantOfRestitution(EnforcementStatus status, boolean defendantsCanSee) {
        EnforcementOrderEntity restitution = enforcement(null, status, defendantsCanSee);
        restitution.setWarrantOfRestitutionDetails(new WarrantOfRestitutionEntity());
        return restitution;
    }

    private static EnforcementOrderEntity enforcement(SelectEnforcementType type, EnforcementStatus status,
                                                      boolean defendantsCanSee) {
        return EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .enforcementOrder(EnforcementOrder.builder().selectEnforcementType(type).build())
            .status(status)
            .defendantsCanSeeEnforcement(defendantsCanSee)
            .documents(new ArrayList<>())
            .build();
    }

    private static DocumentEntity document(DocumentType type) {
        return DocumentEntity.builder().id(UUID.randomUUID()).type(type).build();
    }
}
