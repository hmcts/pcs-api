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
        EnforcementChange<String> allocated =
            runAsCaller(authorization, () -> bailiffAllocationService.allocate(caseReference, bailiff));

        return allocated.refused()
            ? ResponseEntity.unprocessableEntity().body(allocated.errors())
            : ResponseEntity.noContent().build();
    }

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

    public record AllocateBailiffRequest(
        @NotBlank String userId,
        @NotBlank String name,
        @NotBlank String email) {
    }

    public record DefendantCaseRoleRequest(
        @NotBlank String userId,
        @NotBlank String caseRole) {
    }

    public record BackdateRequest(Period period, Duration duration, String enforcementNumber) {
    }

    public record SeedDocumentRequest(
        @NotNull BatchPrintDocumentKind kind,
        @NotBlank String title) {
    }

    public record SeededDocument(String fileName) {
    }
}
