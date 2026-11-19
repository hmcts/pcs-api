package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimKind;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.PreparedBulkClaim;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimantOrganisationResolver;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimSubmissionService.ClaimInput;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimResponseBuilder;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimResponseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkClaimSubmissionServiceTest {

    private static final String ORG = "ORG1";
    private static final String CUI = "CLAIM-1";
    private static final String HASH = "hash-1";
    private static final long CASE_REFERENCE = 1781000000000001L;

    @Mock
    private BulkClaimSubmissionRepository repository;
    @Mock
    private BulkClaimCcdClient ccdClient;
    @Mock
    private BulkClaimantOrganisationResolver organisationResolver;

    private final Map<UUID, BulkClaimSubmissionEntity> rows = new HashMap<>();
    private final BulkClaimResponseBuilder responseBuilder = new BulkClaimResponseBuilder();
    private final OrganisationDetailsResponse organisation = OrganisationDetailsResponse.builder()
        .organisationIdentifier(ORG).name("Org One").build();

    private BulkClaimProperties properties;
    private BulkClaimSubmissionService underTest;

    @BeforeEach
    void setUp() {
        properties = new BulkClaimProperties();
        properties.setParallelism(2);
        JacksonConfiguration jackson = new JacksonConfiguration();
        BulkClaimStagingSerializer serializer = new BulkClaimStagingSerializer(
            jackson.draftCaseDataObjectMapper(), jackson.getMapper());
        underTest = new BulkClaimSubmissionService(repository, ccdClient, organisationResolver, serializer,
                                                   responseBuilder, properties,
                                                   mock(PlatformTransactionManager.class));

        lenient().when(repository.findByOrganisationIdAndCustomerUniqueIdentifier(any(), any()))
            .thenAnswer(invocation -> rows.values().stream()
                .filter(row -> row.getCustomerUniqueIdentifier().equals(invocation.getArgument(1)))
                .findFirst());
        lenient().when(repository.save(any())).thenAnswer(invocation -> {
            BulkClaimSubmissionEntity row = invocation.getArgument(0);
            if (row.getId() == null) {
                row.setId(UUID.randomUUID());
            }
            rows.put(row.getId(), row);
            return row;
        });
        lenient().when(repository.findById(any()))
            .thenAnswer(invocation -> Optional.ofNullable(rows.get(invocation.<UUID>getArgument(0))));
        lenient().when(organisationResolver.isPbaAccountValid(any(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        underTest.shutdown();
    }

    @Test
    void shouldStageThenCreateCaseAndReturnSuccess() {
        when(ccdClient.createCase(any())).thenAnswer(invocation -> {
            BulkClaimSubmissionEntity staged = rows.get(invocation.<UUID>getArgument(0));
            assertThat(staged.getStatus()).isEqualTo(BulkClaimSubmissionStatus.PENDING);
            assertThat(staged.getStagedCaseData()).isNotBlank();
            assertThat(staged.getStagedExtras()).isNotBlank();
            assertThat(staged.getOrganisationSnapshot()).contains(ORG);
            assertThat(staged.getRequestHash()).isEqualTo(HASH);
            assertThat(staged.getPbaAccountNumber()).isEqualTo("PBA1");
            assertThat(staged.getPaymentCustomerReference()).isEqualTo("REF1");
            return BulkClaimCcdResult.created(CASE_REFERENCE);
        });

        ClaimResponseType response = submitOne(prepared(CUI, HASH));

        assertThat(response.getClaimStatus().getStatusCode()).isEqualTo(1);
        assertThat(response.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(rows).hasSize(1);
    }

    @Test
    void shouldReplayStoredSuccessForSameClaimAlreadyCreated() {
        existing(BulkClaimSubmissionStatus.CASE_CREATED, CASE_REFERENCE, Instant.now());

        ClaimResponseType response = submitOne(unexpectedPreparation());

        assertThat(response.getClaimStatus().getStatusCode()).isEqualTo(1);
        assertThat(response.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE));
        verifyNoInteractions(ccdClient);
    }

    @Test
    void shouldRejectDifferentClaimWithReusedIdentifierWhenAlreadyCreated() {
        existing(BulkClaimSubmissionStatus.CASE_CREATED, CASE_REFERENCE, Instant.now());

        ClaimResponseType response = submitOne(input(CUI, "other-hash", unexpectedPreparation().preparation()));

        assertThat(response.getClaimStatus().getFailReasonCode())
            .isEqualTo(BulkFailReason.IDENTIFIER_REUSED.getFailReasonCode());
        verifyNoInteractions(ccdClient);
    }

    @Test
    void shouldReplaySuccessForPendingClaimThatAlreadyHasACase() {
        existing(BulkClaimSubmissionStatus.PENDING, CASE_REFERENCE, Instant.now());

        ClaimResponseType response = submitOne(unexpectedPreparation());

        assertThat(response.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE));
        verifyNoInteractions(ccdClient);
    }

    @Test
    void shouldReportPendingForRecentPendingClaimWithoutACase() {
        existing(BulkClaimSubmissionStatus.PENDING, null, Instant.now());

        ClaimResponseType response = submitOne(unexpectedPreparation());

        assertThat(response.getClaimStatus().getStatusCode()).isEqualTo(BulkFailReason.CLAIM_PENDING.getStatusCode());
        verifyNoInteractions(ccdClient);
    }

    @Test
    void shouldRejectDifferentClaimWhilePendingWithReusedIdentifier() {
        existing(BulkClaimSubmissionStatus.PENDING, null, Instant.now());

        ClaimResponseType response = submitOne(input(CUI, "other-hash", unexpectedPreparation().preparation()));

        assertThat(response.getClaimStatus().getFailReasonCode())
            .isEqualTo(BulkFailReason.IDENTIFIER_REUSED.getFailReasonCode());
    }

    @Test
    void shouldReprocessStalePendingClaimOverwritingTheSameRow() {
        BulkClaimSubmissionEntity stale = existing(
            BulkClaimSubmissionStatus.PENDING, null,
            Instant.now().minus(properties.getPendingRetryAfter()).minusSeconds(60));
        when(ccdClient.createCase(stale.getId())).thenReturn(BulkClaimCcdResult.created(CASE_REFERENCE));

        ClaimResponseType response = submitOne(prepared(CUI, "new-hash"));

        assertThat(response.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(rows).hasSize(1);
        assertThat(stale.getRequestHash()).isEqualTo("new-hash");
    }

    @Test
    void shouldReprocessPreviouslyFailedValidationOverwritingTheSameRow() {
        BulkClaimSubmissionEntity failed = existing(BulkClaimSubmissionStatus.FAILED_VALIDATION, null, Instant.now());
        failed.setFailReasonCode(BulkFailReason.VALIDATION_FAILED.getFailReasonCode());
        failed.setValidationErrors("[{\"path\":\"a\",\"code\":\"b\",\"message\":\"c\"}]");
        when(ccdClient.createCase(failed.getId())).thenReturn(BulkClaimCcdResult.created(CASE_REFERENCE));

        ClaimResponseType response = submitOne(prepared(CUI, HASH));

        assertThat(response.getClaimStatus().getStatusCode()).isEqualTo(1);
        assertThat(rows).hasSize(1);
        assertThat(failed.getStatus()).isEqualTo(BulkClaimSubmissionStatus.PENDING);
        assertThat(failed.getFailReasonCode()).isNull();
        assertThat(failed.getValidationErrors()).isNull();
    }

    @Test
    void shouldStoreRejectionAsFailedValidationWithCodesAndErrors() {
        BulkValidationError error = new BulkValidationError("Defendant/Name", "REQUIRED", "A name is required");
        ClaimInput input = input(CUI, HASH, () -> new BulkClaimPreparationResult.Rejected(
            CUI, BulkFailReason.VALIDATION_FAILED, List.of(error)));

        ClaimResponseType response = submitOne(input);

        assertThat(response.getClaimStatus().getFailReasonCode())
            .isEqualTo(BulkFailReason.VALIDATION_FAILED.getFailReasonCode());
        assertThat(response.getValidationErrors().getValidationError()).hasSize(1);
        BulkClaimSubmissionEntity row = rows.values().iterator().next();
        assertThat(row.getStatus()).isEqualTo(BulkClaimSubmissionStatus.FAILED_VALIDATION);
        assertThat(row.getFailReasonCode()).isEqualTo(BulkFailReason.VALIDATION_FAILED.getFailReasonCode());
        assertThat(row.getValidationErrors()).contains("Defendant/Name", "REQUIRED", "A name is required");
        assertThat(row.getStagedCaseData()).isNull();
        verifyNoInteractions(ccdClient);
    }

    @Test
    void shouldMarkRowFailedWhenCcdRejectsTheClaim() {
        BulkValidationError error = new BulkValidationError("Property", "CCD_REJECTED", "Bad address");
        when(ccdClient.createCase(any()))
            .thenReturn(BulkClaimCcdResult.rejected(BulkFailReason.VALIDATION_FAILED, List.of(error)));

        ClaimResponseType response = submitOne(prepared(CUI, HASH));

        assertThat(response.getClaimStatus().getFailReasonCode())
            .isEqualTo(BulkFailReason.VALIDATION_FAILED.getFailReasonCode());
        BulkClaimSubmissionEntity row = rows.values().iterator().next();
        assertThat(row.getStatus()).isEqualTo(BulkClaimSubmissionStatus.FAILED);
        assertThat(row.getFailReasonCode()).isEqualTo(BulkFailReason.VALIDATION_FAILED.getFailReasonCode());
        assertThat(row.getValidationErrors()).contains("Bad address");
    }

    @Test
    void shouldReportPendingAndLeaveRowUntouchedWhenCcdIsUnavailableAndNoCaseExists() {
        when(ccdClient.createCase(any())).thenReturn(BulkClaimCcdResult.unavailable());

        ClaimResponseType response = submitOne(prepared(CUI, HASH));

        assertThat(response.getClaimStatus().getStatusCode()).isEqualTo(BulkFailReason.CLAIM_PENDING.getStatusCode());
        BulkClaimSubmissionEntity row = rows.values().iterator().next();
        assertThat(row.getStatus()).isEqualTo(BulkClaimSubmissionStatus.PENDING);
        assertThat(row.getCaseReference()).isNull();
        verify(repository, times(1)).save(any());
    }

    @Test
    void shouldReportSuccessWhenCcdIsUnavailableButTheCaseWasCommitted() {
        when(ccdClient.createCase(any())).thenAnswer(invocation -> {
            BulkClaimSubmissionEntity row = rows.get(invocation.<UUID>getArgument(0));
            row.setStatus(BulkClaimSubmissionStatus.CASE_CREATED);
            row.setCaseReference(CASE_REFERENCE);
            return BulkClaimCcdResult.unavailable();
        });

        ClaimResponseType response = submitOne(prepared(CUI, HASH));

        assertThat(response.getClaimStatus().getStatusCode()).isEqualTo(1);
        assertThat(response.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE));
    }

    @Test
    void shouldReplayStoredSuccessWhenValidatingOnlyAndSameClaimAlreadyCreated() {
        existing(BulkClaimSubmissionStatus.CASE_CREATED, CASE_REFERENCE, Instant.now());

        ClaimResponseType response = submit(true, unexpectedPreparation()).getFirst();

        assertThat(response.getClaimStatus().getStatusCode()).isEqualTo(1);
        assertThat(response.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE));
        verifyNoWrites();
    }

    @Test
    void shouldReplayStoredSuccessWhenValidatingOnlyAndPendingClaimHasACase() {
        existing(BulkClaimSubmissionStatus.PENDING, CASE_REFERENCE, Instant.now());

        ClaimResponseType response = submit(true, unexpectedPreparation()).getFirst();

        assertThat(response.getCaseReference()).isEqualTo(String.valueOf(CASE_REFERENCE));
        verifyNoWrites();
    }

    @Test
    void shouldReportIdentifierReusedWhenValidatingOnlyWithDifferentClaimAlreadyCreated() {
        existing(BulkClaimSubmissionStatus.CASE_CREATED, CASE_REFERENCE, Instant.now());

        ClaimResponseType response =
            submit(true, input(CUI, "other-hash", unexpectedPreparation().preparation())).getFirst();

        assertThat(response.getClaimStatus().getFailReasonCode())
            .isEqualTo(BulkFailReason.IDENTIFIER_REUSED.getFailReasonCode());
        verifyNoWrites();
    }

    @Test
    void shouldReportIdentifierReusedWhenValidatingOnlyWithDifferentClaimRecentlyPending() {
        existing(BulkClaimSubmissionStatus.PENDING, null, Instant.now());

        ClaimResponseType response =
            submit(true, input(CUI, "other-hash", unexpectedPreparation().preparation())).getFirst();

        assertThat(response.getClaimStatus().getFailReasonCode())
            .isEqualTo(BulkFailReason.IDENTIFIER_REUSED.getFailReasonCode());
        verifyNoWrites();
    }

    @Test
    void shouldPrepareWhenValidatingOnlyAndNoRowExists() {
        ClaimResponseType response = submit(true, prepared(CUI, HASH)).getFirst();

        assertThat(response.getClaimStatus().getStatusCode()).isEqualTo(1);
        assertThat(response.getCaseReference()).isNull();
        verifyNoWrites();
    }

    @Test
    void shouldNeitherStageNorCallCcdWhenValidatingOnly() {
        ClaimResponseType valid = submit(true, prepared(CUI, HASH)).getFirst();
        ClaimResponseType invalid = submit(true, input("CLAIM-2", "h2", () ->
            new BulkClaimPreparationResult.Rejected("CLAIM-2", BulkFailReason.VALIDATION_FAILED, List.of())))
            .getFirst();

        assertThat(valid.getClaimStatus().getStatusCode()).isEqualTo(1);
        assertThat(valid.getCaseReference()).isNull();
        assertThat(invalid.getClaimStatus().getFailReasonCode())
            .isEqualTo(BulkFailReason.VALIDATION_FAILED.getFailReasonCode());
        verify(repository, never()).save(any());
        verifyNoInteractions(ccdClient);
    }

    @Test
    void shouldRejectClaimWhoseOwnPbaIsNotValidForTheOrganisation() {
        when(organisationResolver.isPbaAccountValid(organisation, "PBA1")).thenReturn(false);

        ClaimResponseType response = underTest.submit(ORG, organisation, "PBA9", List.of(prepared(CUI, HASH)), false)
            .getFirst();

        assertThat(response.getClaimStatus().getFailReasonCode())
            .isEqualTo(BulkFailReason.PBA_OVERRIDE_NOT_VALID.getFailReasonCode());
        verifyNoInteractions(ccdClient);
    }

    @Test
    void shouldNotRevalidateClaimPbaThatMatchesTheSetLevelAccount() {
        when(ccdClient.createCase(any())).thenReturn(BulkClaimCcdResult.created(CASE_REFERENCE));

        ClaimResponseType response = underTest.submit(ORG, organisation, "pba1", List.of(prepared(CUI, HASH)), false)
            .getFirst();

        assertThat(response.getClaimStatus().getStatusCode()).isEqualTo(1);
        verify(organisationResolver, never()).isPbaAccountValid(any(), any());
    }

    @Test
    void shouldReportIdentifierReusedForDuplicateIdentifiersWithinTheRequest() {
        when(ccdClient.createCase(any())).thenReturn(BulkClaimCcdResult.created(CASE_REFERENCE));

        List<ClaimResponseType> responses = submit(false, prepared(CUI, HASH), prepared(CUI, HASH));

        assertThat(responses.get(0).getClaimStatus().getStatusCode()).isEqualTo(1);
        assertThat(responses.get(1).getClaimStatus().getFailReasonCode())
            .isEqualTo(BulkFailReason.IDENTIFIER_REUSED.getFailReasonCode());
        assertThat(rows).hasSize(1);
    }

    @Test
    void shouldReportPendingWhenAConcurrentRequestWinsTheInsert() {
        doThrow(new DataIntegrityViolationException("uq_bulk_claim_submission")).when(repository).save(any());

        ClaimResponseType response = submitOne(prepared(CUI, HASH));

        assertThat(response.getClaimStatus().getStatusCode()).isEqualTo(BulkFailReason.CLAIM_PENDING.getStatusCode());
        verifyNoInteractions(ccdClient);
    }

    @Test
    void shouldReportSaveFailedWhenPreparationThrows() {
        ClaimInput input = input(CUI, HASH, () -> {
            throw new IllegalStateException("boom");
        });

        ClaimResponseType response = submitOne(input);

        assertThat(response.getClaimStatus().getFailReasonCode())
            .isEqualTo(BulkFailReason.SAVE_FAILED.getFailReasonCode());
    }

    @Test
    void shouldProcessClaimsInParallelUpToTheConfiguredLimitAndKeepRequestOrder() throws Exception {
        when(ccdClient.createCase(any())).thenReturn(BulkClaimCcdResult.created(CASE_REFERENCE));

        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();
        CountDownLatch twoRunning = new CountDownLatch(2);
        List<ClaimInput> inputs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String cui = "CLAIM-" + i;
            inputs.add(input(cui, "hash-" + i, () -> {
                int now = running.incrementAndGet();
                maxRunning.accumulateAndGet(now, Math::max);
                twoRunning.countDown();
                try {
                    twoRunning.await(5, TimeUnit.SECONDS);
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    running.decrementAndGet();
                }
                return preparedResult(cui, "hash-" + cui);
            }));
        }

        List<ClaimResponseType> responses = underTest.submit(ORG, organisation, "PBA1", inputs, false);

        assertThat(responses).extracting(ClaimResponseType::getCustomerUniqueIdentifier)
            .containsExactly("CLAIM-0", "CLAIM-1", "CLAIM-2", "CLAIM-3", "CLAIM-4");
        assertThat(maxRunning.get()).isEqualTo(2);
    }

    private BulkClaimSubmissionEntity existing(BulkClaimSubmissionStatus status, Long caseReference,
                                               Instant lastModified) {
        BulkClaimSubmissionEntity row = BulkClaimSubmissionEntity.builder()
            .id(UUID.randomUUID())
            .organisationId(ORG)
            .customerUniqueIdentifier(CUI)
            .claimKind(BulkClaimKind.RENTAL)
            .requestHash(HASH)
            .status(status)
            .caseReference(caseReference)
            .lastModified(lastModified)
            .createdAt(lastModified)
            .build();
        rows.put(row.getId(), row);
        return row;
    }

    private void verifyNoWrites() {
        verify(repository, never()).save(any());
        verify(repository, never()).saveAll(any());
        verify(repository, never()).delete(any());
        verifyNoInteractions(ccdClient);
    }

    private ClaimResponseType submitOne(ClaimInput input) {
        return submit(false, input).getFirst();
    }

    private List<ClaimResponseType> submit(boolean validateOnly, ClaimInput... inputs) {
        return underTest.submit(ORG, organisation, "PBA1", List.of(inputs), validateOnly);
    }

    private static ClaimInput prepared(String cui, String hash) {
        return input(cui, hash, () -> preparedResult(cui, hash));
    }

    private static BulkClaimPreparationResult preparedResult(String cui, String hash) {
        return new BulkClaimPreparationResult.Prepared(new PreparedBulkClaim(
            cui, BulkClaimKind.RENTAL, hash, PCSCase.builder().build(),
            new BulkClaimExtras(null, null, null, null, null, null, null, null, null, null, null, null),
            "PBA1", "REF1"));
    }

    private static ClaimInput unexpectedPreparation() {
        return input(CUI, HASH, () -> {
            throw new AssertionError("The claim should not be prepared");
        });
    }

    private static ClaimInput input(String cui, String hash,
                                    Supplier<BulkClaimPreparationResult> preparation) {
        return new ClaimInput(cui, hash, BulkClaimKind.RENTAL, "2.0", preparation);
    }
}
