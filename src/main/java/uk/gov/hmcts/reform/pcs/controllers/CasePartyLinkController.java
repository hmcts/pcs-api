package uk.gov.hmcts.reform.pcs.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.controllers.RestExceptionHandler.Error;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeRequest;
import uk.gov.hmcts.reform.pcs.service.PartyAccessCodeLinkService;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Access Code Validation",
        description = "Validate access code and link user ID to party")
public class CasePartyLinkController {

    private final IdamAuthenticator idamAuthenticator;
    private final PartyAccessCodeLinkService partyAccessCodeLinkService;

    @PostMapping(
            value = "/{caseReference}/validate-access-code",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Validate access code and link user to a case",
            description = "Stores the user's IDAM user ID for the matching party",
            security = {
                @SecurityRequirement(name = "AuthorizationToken"),
                @SecurityRequirement(name = "ServiceAuthorization")
            }
    )
    @ApiResponse(responseCode = "200", description = "Successful validation and linking",
            content = @Content())
    @ApiResponse(responseCode = "400", description = "Invalid access code, or case not found",
            content = @Content())
    @ApiResponse(responseCode = "401", description = "Invalid access token",
            content = @Content())
    @ApiResponse(responseCode = "403", description = "Invalid Service Authorization",
            content = @Content())
    @ApiResponse(responseCode = "404", description = "Party does not belong to this case",
            content = @Content())
    @ApiResponse(responseCode = "409", description = "Access code already used by another user",
            content = @Content())
    public ResponseEntity<Void> validateAccessCode(
            @Parameter(description = "The 12-digit case reference number", required = true)
            @PathVariable long caseReference,
            @Valid @RequestBody ValidateAccessCodeRequest request,
            @Parameter(description = "Bearer token for user authentication", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "Service-to-Service (S2S) authorization token", required = true)
            @RequestHeader("ServiceAuthorization") String s2sToken
    ) {
        var user = idamAuthenticator.validateAuthToken(authorization).getUserDetails();

        partyAccessCodeLinkService.linkPartyByAccessCode(caseReference, request.getAccessCode(), user);

        return ResponseEntity.ok().build();
    }

    // Same response as a wrong access code, so callers cannot tell which case numbers exist.
    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<Error> handleCaseNotFound(CaseNotFoundException ex) {
        log.warn("Access code validation attempted for an unknown case", ex);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new Error("Invalid access code"));
    }

}
