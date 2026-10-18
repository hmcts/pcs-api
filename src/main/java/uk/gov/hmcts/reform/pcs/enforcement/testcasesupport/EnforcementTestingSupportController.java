package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;

import java.time.Duration;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Test-only shortcuts for enforcement, for the points where the end to end tests cannot drive the real
 * journey or cannot wait for it.
 *
 * <p>pcs-api cannot take an enforcement fee yet (HDPI-2877), so an application never reaches "Awaiting
 * review" on its own. And {@code allocateBailiff} cannot be driven without staff reference data, so without
 * a shortcut no warrant can get past "Awaiting bailiff allocation" and the schedule, reschedule,
 * notice-of-eviction and warrant-return journeys are all untestable. The 72 hour and 12 month rules are
 * measured from when things happened, so {@code backdate} moves those times into the past.
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
    private final IdamAuthenticator idamAuthenticator;
    private final EnforcementBackdateService enforcementBackdateService;

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
     * <p>The event itself cannot be driven on a CFTLib stack. Its one field is a StaffUser type-ahead
     * ({@code FindBailiffPage}) backed by staff reference data, which CFTLib does not run, so XUI degrades
     * the component to a raw {@code IdamId} input and CCD's "IdamId is required" fires before the page's own
     * "Choose a bailiff" mid-event. Whether a preview environment has staff reference data is unconfirmed.
     * Without this shortcut every status after "Awaiting bailiff allocation" is unreachable, which blocks
     * the schedule, reschedule, notice-of-eviction and warrant-return journeys.
     *
     * <p>Unlike {@code fee-paid} this needs a user bearer token as well as the S2S one, because
     * {@link uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService#allocate} records who made
     * the allocation and {@code SecurityContextService.getCurrentUserId()} throws when there is no user
     * principal. {@code IdamAuthenticationFilter} only populates the security context for {@code /ccd} and
     * {@code /callbacks}, so this resolves the caller itself and runs the allocation as them, in the same
     * way {@code SecurityContextSystemEventExecutor} runs system events as the system user.
     */
    @Operation(summary = "Allocate the case's current warrant to a bailiff, as the allocateBailiff event would")
    @PostMapping("/{caseReference}/allocate-bailiff")
    public ResponseEntity<List<String>> allocateBailiff(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorization,
        @PathVariable long caseReference,
        @Valid @RequestBody AllocateBailiffRequest request) {

        log.info("Testing support: allocating the warrant on case {} to bailiff {}", caseReference,
                 request.userId());
        BailiffUser bailiff = new BailiffUser(request.userId(), request.name(), request.email());
        EnforcementChange<Void> allocated =
            runAsCaller(authorization, () -> bailiffAllocationService.allocate(caseReference, bailiff));

        // The service refuses rather than throwing, so the refusal has to be turned into a failure here or
        // a caller would read "no enforcement" or "not an approved live warrant" as success.
        return allocated.refused()
            ? ResponseEntity.unprocessableEntity().body(allocated.errors())
            : ResponseEntity.noContent().build();
    }

    /**
     * Moves the timestamps of one of the case's enforcements into the past, for the rules measured from them:
     * the claimant's 72 hours to confirm an eviction and the free repeat warrant within 12 months of the first
     * being generated. See {@link EnforcementBackdateService}.
     */
    @Operation(summary = "Move an enforcement's eviction, generated, return and other timestamps into the past")
    @PostMapping("/{caseReference}/backdate")
    public ResponseEntity<Void> backdate(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @PathVariable long caseReference,
        @RequestBody BackdateRequest request) {

        if (request.period() == null && request.duration() == null) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Testing support: backdating enforcement {} on case {} by {} and {}",
                 request.enforcementNumber() == null ? "(current)" : request.enforcementNumber(), caseReference,
                 request.period(), request.duration());
        return enforcementBackdateService
            .backdate(caseReference, request.enforcementNumber(), request.period(), request.duration())
            .<ResponseEntity<Void>>map(id -> ResponseEntity.noContent().build())
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Runs {@code action} with the bearer token's user on the security context, restoring it afterwards.
     */
    private <T> T runAsCaller(String authorization, Supplier<T> action) {
        Authentication previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            User caller = idamAuthenticator.validateAuthToken(authorization);
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(caller, null, Collections.emptyList()));
            return action.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    /**
     * The bailiff to allocate. The caller supplies the IDAM id so that no staff reference data is needed.
     */
    public record AllocateBailiffRequest(
        @NotBlank String userId,
        @NotBlank String name,
        @NotBlank String email) {
    }

    /**
     * How far back to move the enforcement, as an ISO-8601 period ({@code P12M}), duration ({@code PT73H}) or
     * both, and which enforcement: its warrant or writ number, or the case's current enforcement when omitted.
     */
    public record BackdateRequest(Period period, Duration duration, String enforcementNumber) {
    }
}
