package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.enforcement.batchprint.BatchPrintDocumentKind;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;

import java.time.Duration;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Test-only shortcuts for enforcement, for the points where the end to end tests cannot drive the real
 * journey or cannot wait for it.
 *
 * <p>A local stack has no Payments service, so an application's fee is never paid and it never reaches
 * "Awaiting review" on its own; {@code fee-paid} pays it through the real payment callback handler (see
 * {@link EnforcementFeePaidSimulator}). And {@code allocateBailiff} cannot be driven without staff reference
 * data, so without a shortcut no warrant can get past "Awaiting bailiff allocation" and the schedule, reschedule,
 * notice-of-eviction and warrant-return journeys are all untestable. The 72 hour and 12 month rules are
 * measured from when things happened, so {@code backdate} moves those times into the past. Viewing a case as
 * a defendant needs a defendant case role, which locally only this can give. Batch print takes the latest of
 * each document, and a second notice of eviction needs a reschedule the suite cannot drive yet, so
 * {@code documents} files placeholders.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/testing-support/enforcement")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
@Tag(name = "Testing Support")
public class EnforcementTestingSupportController {

    private static final List<UserRole> DEFENDANT_CASE_ROLES =
        List.of(UserRole.DEFENDANT, UserRole.DEFENDANT_SOLICITOR);

    private final EnforcementFeePaidSimulator enforcementFeePaidSimulator;
    private final BailiffAllocationService bailiffAllocationService;
    private final IdamAuthenticator idamAuthenticator;
    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final EnforcementBackdateService enforcementBackdateService;
    private final EnforcementTestDocumentSeeder enforcementTestDocumentSeeder;

    @Operation(summary = "Record the fee as paid for the case's enforcement application awaiting payment")
    @PostMapping("/{caseReference}/fee-paid")
    public ResponseEntity<Void> recordFeePaid(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @PathVariable long caseReference) {

        log.info("Testing support: recording the enforcement fee as paid on case {}", caseReference);
        return enforcementFeePaidSimulator.recordFeePaid(caseReference)
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
     * Gives a user the {@code [DEFENDANT]} or {@code [DEFENDANTSOLICITOR]} case role through CCD's case-users
     * API, so that the end to end tests can view a case as a defendant (14-defendant-visibility.feature).
     *
     * <p>It stands in for the access code a citizen defendant enters and for Notice of Change. The testing-support
     * {@code link-defendant-solicitor-to-party} endpoint does not work on a CFTLib stack, and an access code
     * needs pcs-frontend.
     */
    @Operation(summary = "Give a user a defendant case role on the case")
    @PostMapping("/{caseReference}/defendant-case-role")
    public ResponseEntity<Void> assignDefendantCaseRole(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @PathVariable long caseReference,
        @Valid @RequestBody DefendantCaseRoleRequest request) {

        Optional<UserRole> role = DEFENDANT_CASE_ROLES.stream()
            .filter(candidate -> candidate.getRole().equals(request.caseRole()))
            .findFirst();
        if (role.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Testing support: giving user {} the {} case role on case {}", request.userId(),
                 request.caseRole(), caseReference);
        caseRoleAssignmentService.assignRasRole(caseReference, request.userId(), role.get());
        return ResponseEntity.noContent().build();
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
     * Files a placeholder warrant, risk assessment or notice of eviction in the case's Enforcement folder, against
     * its current enforcement, as a later copy of one pcs-api generated. Batch print (HDPI-8012) prints only the
     * latest of each, and the suite cannot reschedule an eviction to generate a second notice of eviction yet.
     * See {@link EnforcementTestDocumentSeeder}.
     */
    @Operation(summary = "File a placeholder enforcement document in the case's Enforcement folder")
    @PostMapping("/{caseReference}/documents")
    public ResponseEntity<SeededDocument> seedDocument(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @PathVariable long caseReference,
        @Valid @RequestBody SeedDocumentRequest request) {

        log.info("Testing support: filing a placeholder {} on case {}", request.kind(), caseReference);
        return enforcementTestDocumentSeeder.seed(caseReference, request.kind(), request.title())
            .map(fileName -> ResponseEntity.status(HttpStatus.CREATED).body(new SeededDocument(fileName)))
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
     * The user to give a defendant case role, and the role: {@code [DEFENDANT]} or {@code [DEFENDANTSOLICITOR]}.
     */
    public record DefendantCaseRoleRequest(
        @NotBlank String userId,
        @NotBlank String caseRole) {
    }

    /**
     * How far back to move the enforcement, as an ISO-8601 period ({@code P12M}), duration ({@code PT73H}) or
     * both, and which enforcement: its warrant or writ number, or the case's current enforcement when omitted.
     */
    public record BackdateRequest(Period period, Duration duration, String enforcementNumber) {
    }

    /**
     * The placeholder to file: which document it stands in for, and the title printed on it.
     */
    public record SeedDocumentRequest(
        @NotNull BatchPrintDocumentKind kind,
        @NotBlank String title) {
    }

    public record SeededDocument(String fileName) {
    }
}
