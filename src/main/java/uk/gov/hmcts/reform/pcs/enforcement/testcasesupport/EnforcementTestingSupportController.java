package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;

/**
 * Test-only shortcuts for enforcement. pcs-api cannot take an enforcement fee yet (HDPI-2877), so an application
 * never reaches "Awaiting review" on its own; the end to end tests use this to put one there.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/testing-support/enforcement")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
@Tag(name = "Testing Support")
public class EnforcementTestingSupportController {

    private final EnforcementOrderService enforcementOrderService;
    private final BailiffAllocationService bailiffAllocationService;

    @Operation(summary = "Record the fee as paid for the case's enforcement application awaiting payment")
    @PostMapping("/{caseReference}/fee-paid")
    public ResponseEntity<Void> recordFeePaid(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @PathVariable long caseReference) {

        log.info("Testing support: recording the enforcement fee as paid on case {}", caseReference);
        return enforcementOrderService.recordFeePaid(caseReference)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    /**
     * Allocates the case's current warrant to a bailiff, with the same outcome as the {@code allocateBailiff}
     * event: an {@code enf_bailiff_allocation} row and, from "Awaiting bailiff allocation", a move to
     * "Awaiting scheduling".
     *
     * <p>The event itself cannot be driven outside AAT. Its one field is a StaffUser type-ahead
     * ({@code FindBailiffPage}) backed by staff reference data, and neither CFTLib nor a preview environment
     * runs {@code rd-caseworker-ref-api}; XUI degrades the component to a raw {@code IdamId} input. Without
     * this shortcut every status after "Awaiting bailiff allocation" is unreachable, which blocks the
     * schedule, reschedule, notice-of-eviction and warrant-return journeys.
     *
     * <p>Unlike {@code fee-paid} this needs a user bearer token as well as the S2S one:
     * {@link uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService#allocate} records who made
     * the allocation, and {@code SecurityContextService.getCurrentUserId()} throws when there is no user
     * principal.
     */
    @Operation(summary = "Allocate the case's current warrant to a bailiff, as the allocateBailiff event would")
    @PostMapping("/{caseReference}/allocate-bailiff")
    public ResponseEntity<Void> allocateBailiff(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @PathVariable long caseReference,
        @Valid @RequestBody AllocateBailiffRequest request) {

        log.info("Testing support: allocating the warrant on case {} to bailiff {}", caseReference,
                 request.userId());
        bailiffAllocationService.allocate(
            caseReference, new BailiffUser(request.userId(), request.name(), request.email()));
        return ResponseEntity.noContent().build();
    }

    /** The bailiff to allocate. The caller supplies the IDAM id so that no staff reference data is needed. */
    public record AllocateBailiffRequest(
        @NotBlank String userId,
        @NotBlank String name,
        @NotBlank String email) {
    }
}
