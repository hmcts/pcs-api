package uk.gov.hmcts.reform.pcs.enforcement.view;

import jakarta.persistence.EntityManager;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.ccd.view.DocumentsView;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffNoteRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.WarrantReturnRepository;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * The enforcement part of a case view runs one statement per kind of row it reads, however many enforcements
 * the claim has, rather than loading the claim's enforcements once per view that needs them and each
 * enforcement's details and warrant return one query at a time.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Statements run by the enforcement case view")
class EnforcementCaseViewQueryIT extends AbstractPostgresContainerIT {

    private static final LocalDateTime EVICTION = LocalDateTime.parse("2099-06-25T10:00");
    private static final LocalDateTime SERVED = LocalDateTime.parse("2026-03-15T08:00");
    private static final String ENFORCEMENT_CATEGORY = FeatureCaseFileCategory.ENFORCEMENT.getId();

    /**
     * Every statement run while {@link #capturing}, in order, shared with the Hibernate session factory the
     * Spring context builds.
     */
    private static final List<String> CAPTURED = new ArrayList<>();
    private static volatile boolean capturing;

    private static final Pattern FROM_TABLE = Pattern.compile("\\bfrom ([\\w.]+)");

    @TestConfiguration
    static class StatementCaptureConfiguration {

        @Bean
        HibernatePropertiesCustomizer captureStatements() {
            return properties -> properties.put(AvailableSettings.STATEMENT_INSPECTOR, (StatementInspector) sql -> {
                if (capturing) {
                    synchronized (CAPTURED) {
                        CAPTURED.add(sql);
                    }
                }
                return sql;
            });
        }
    }

    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;
    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private BailiffNoteRepository bailiffNoteRepository;
    @Autowired
    private WarrantReturnRepository warrantReturnRepository;
    @Autowired
    private DocumentsView documentsView;
    @Autowired
    private EnforcementCaseViewContributor enforcementCaseViewContributor;
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private UserRoleService userRoleService;
    @MockitoBean
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void enableEnforcement() {
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            new User("testing", UserInfo.builder().uid("user-id").build()), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("reads each table once, however many enforcements the claim has")
    void readsEachTableOnce() {
        List<String> oneEnforcement = tablesReadByCaseView(1717000000000001L, 1, UserRole.BAILIFF);
        List<String> threeEnforcements = tablesReadByCaseView(1717000000000002L, 3, UserRole.BAILIFF);

        assertThat(oneEnforcement).containsExactlyInAnyOrder(
            "claim", "enf_case", "enf_bailiff_note", "enf_warrant_return", "draft.draft_case_data");
        assertThat(threeEnforcements).containsExactlyInAnyOrderElementsOf(oneEnforcement);
    }

    /**
     * A defendant's view reads the enforcement documents as well, to keep from them every one but the notice
     * of eviction of a warrant served on them.
     */
    @Test
    @DisplayName("reads each table once for a defendant, who sees the warrants served on them")
    void readsEachTableOnceForADefendant() {
        List<String> tables = tablesReadByCaseView(1717000000000003L, 3, UserRole.DEFENDANT);

        assertThat(tables).containsExactlyInAnyOrder(
            "claim", "enf_case", "enf_bailiff_note", "enf_warrant_return", "draft.draft_case_data", "document");
    }

    /**
     * The table each statement the enforcement case view contributor runs reads, for a claim with
     * {@code enforcements} warrants, each with a bailiff note, a warrant return, a notice of eviction served
     * on the defendants and some claimant evidence.
     */
    private List<String> tablesReadByCaseView(long caseReference, int enforcements, UserRole role) {
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(caseReference).build();
        pcsCaseEntity.addClaim(new ClaimEntity());
        pcsCaseEntity = pcsCaseRepository.save(pcsCaseEntity);
        ClaimEntity claim = pcsCaseEntity.getClaims().getFirst();
        pcsCaseEntity.addDocuments(List.of(document("claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE.getId(),
                                                    DocumentType.CLAIM)));
        for (int i = 0; i < enforcements; i++) {
            // Only the newest is live, so that the claim always has at most one live enforcement.
            boolean live = i == enforcements - 1;
            EnforcementOrderEntity enforcement = savedEnforcement(
                claim, live ? EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION : EnforcementStatus.CLOSED, i);
            DocumentEntity notice = document("notice-of-eviction-" + i + ".pdf", ENFORCEMENT_CATEGORY,
                                             DocumentType.NOTICE_OF_EVICTION);
            DocumentEntity evidence = document("evidence-" + i + ".pdf", ENFORCEMENT_CATEGORY,
                                               DocumentType.PHOTOGRAPHIC_EVIDENCE);
            pcsCaseEntity.addDocuments(List.of(notice, evidence));
            enforcement.addDocuments(List.of(notice, evidence));
            entityManager.flush();
        }
        entityManager.flush();
        entityManager.clear();

        when(userRoleService.getCurrentUserCaseRoles(anyLong(), anyCollection()))
            .thenReturn(new UserRoles("user-id", List.of(role.getRole())));
        when(userRoleService.getCurrentUserOrganisationalRoles())
            .thenReturn(new UserRoles("user-id", List.of(role.getRole())));

        PcsCaseEntity loaded = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();
        PCSCase pcsCase = PCSCase.builder().build();
        documentsView.setCaseFields(pcsCase, loaded, null);

        synchronized (CAPTURED) {
            CAPTURED.clear();
        }
        capturing = true;
        try {
            enforcementCaseViewContributor.contribute(loaded, pcsCase, caseReference, State.CASE_ISSUED);
        } finally {
            capturing = false;
        }
        assertThat(pcsCase.getEnforcementOrder()).isNotNull();
        synchronized (CAPTURED) {
            return CAPTURED.stream().map(EnforcementCaseViewQueryIT::firstTable).toList();
        }
    }

    /**
     * The table a statement reads from, as the one readable name for it: its whole SQL runs to thousands of
     * characters.
     */
    private static String firstTable(String sql) {
        Matcher matcher = FROM_TABLE.matcher(sql);
        return matcher.find() ? matcher.group(1) : sql;
    }

    private EnforcementOrderEntity savedEnforcement(ClaimEntity claim, EnforcementStatus status, int index) {
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(
            EnforcementOrder.builder().selectEnforcementType(SelectEnforcementType.WARRANT).build());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(Instant.parse("2026-03-01T09:00:00Z").plusSeconds(index));
        enforcement.setApprovedAt(Instant.parse("2026-02-01T09:00:00Z").plusSeconds(index));
        enforcement.setBailiffDate(EVICTION);
        enforcement.setRiskLevel(RiskLevel.MEDIUM);
        enforcement.setNoticeServedAt(SERVED);
        enforcement.setDefendantsCanSeeEnforcement(true);
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);

        bailiffNoteRepository.save(BailiffNoteEntity.builder()
                                      .enforcementOrder(saved)
                                      .note("A bailiff note")
                                      .createdAt(Instant.parse("2026-03-02T09:00:00Z"))
                                      .build());
        warrantReturnRepository.save(WarrantReturnEntity.builder()
                                        .enforcementOrder(saved)
                                        .returnType(WarrantReturnType.INTERIM)
                                        .returnName(WarrantReturnName.WARRANT_SUSPENDED_BY_ORDER)
                                        .recordedAt(Instant.parse("2026-03-03T09:00:00Z"))
                                        .build());
        return saved;
    }

    private static DocumentEntity document(String fileName, String categoryId, DocumentType type) {
        return DocumentEntity.builder()
            .fileName(fileName)
            .url("http://dm-store/documents/" + fileName)
            .binaryUrl("http://dm-store/documents/" + fileName + "/binary")
            .categoryId(categoryId)
            .type(type)
            .build();
    }
}
