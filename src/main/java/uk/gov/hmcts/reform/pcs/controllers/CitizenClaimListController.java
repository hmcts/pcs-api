package uk.gov.hmcts.reform.pcs.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CitizenClaimListService;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
@Tag(name = "Citizen Claim List", description = "Retrieve claims associated with the authenticated citizen")
public class CitizenClaimListController {

    private final IdamAuthenticator idamAuthenticator;
    private final CitizenClaimListService citizenClaimListService;

    @GetMapping(value = "/citizen-claims", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get all claims against the authenticated citizen",
        description = "Returns all possession claims where the authenticated user is linked as a defendant",
        security = {
            @SecurityRequirement(name = "AuthorizationToken"),
            @SecurityRequirement(name = "ServiceAuthorization")
        }
    )
    @ApiResponse(responseCode = "200", description = "Claims retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Invalid access token")
    @ApiResponse(responseCode = "403", description = "Invalid Service Authorization")
    public ResponseEntity<List<ClaimSummary>> getDefendantClaims(
        @Parameter(description = "Bearer token for user authentication", required = true)
        @RequestHeader("Authorization") String authorization,
        @Parameter(description = "Service-to-Service (S2S) authorization token", required = true)
        @RequestHeader("ServiceAuthorization") String s2sToken
    ) {
        UUID idamId = UUID.fromString(idamAuthenticator.validateAuthToken(authorization).getUserDetails().getUid());
        return ResponseEntity.ok(citizenClaimListService.getClaimsAgainst(idamId));
    }
}
