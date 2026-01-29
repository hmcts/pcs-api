package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeGenerationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;
import uk.gov.hmcts.reform.pcs.testingsupport.model.CreateTestCaseRequest;
import uk.gov.hmcts.reform.pcs.testingsupport.model.CreateTestCaseResponse;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole.DEFENDANT;

@Slf4j
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
@Tag(name = "Testing Support")
public class TestingSupportController {

    private final SchedulerClient schedulerClient;
    private final Task<Void> helloWorldTask;
    private final DocAssemblyService docAssemblyService;
    private final EligibilityService eligibilityService;
    private final PcsCaseRepository pcsCaseRepository;
    private final PartyRepository partyRepository;
    private final PartyAccessCodeRepository partyAccessCodeRepository;
    private final PcsCaseService pcsCaseService;
    private final AccessCodeGenerationService accessCodeGenerationService;
    private final ModelMapper modelMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public TestingSupportController(
        SchedulerClient schedulerClient,
        @Qualifier("helloWorldTask") Task<Void> helloWorldTask,
        DocAssemblyService docAssemblyService,
        EligibilityService eligibilityService,
        PcsCaseRepository pcsCaseRepository,
        PartyRepository partyRepository,
        PartyAccessCodeRepository partyAccessCodeRepository,
        PcsCaseService pcsCaseService,
        AccessCodeGenerationService accessCodeGenerationService,
        ModelMapper modelMapper) {
        this.schedulerClient = schedulerClient;
        this.helloWorldTask = helloWorldTask;
        this.docAssemblyService = docAssemblyService;
        this.eligibilityService = eligibilityService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.partyRepository = partyRepository;
        this.partyAccessCodeRepository = partyAccessCodeRepository;
        this.pcsCaseService = pcsCaseService;
        this.accessCodeGenerationService = accessCodeGenerationService;
        this.modelMapper = modelMapper;
    }

    @Operation(
        summary = "Schedule a Hello World task",
        description = "Schedules a Hello World task to be executed after a specified delay. "
            + "This endpoint is used for testing the database scheduler functionality."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task scheduled successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authorization token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/db-scheduler-test")
    public ResponseEntity<String> scheduleHelloWorldTask(
        @Parameter(
            description = "Delay in seconds before task execution (default: 1)",
            example = "5"
        )
        @RequestParam(value = "delaySeconds", defaultValue = "1") int delaySeconds,
        @Parameter(
            description = "Bearer token for user authentication (default: DummyId)",
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = AUTHORIZATION, defaultValue = "DummyId") String authorisation,
        @Parameter(
            description = "Service-to-Service (S2S) authorization token",
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            required = true
        )
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization) {
        try {
            String taskId = "helloWorld-" + UUID.randomUUID();
            Instant executionTime = Instant.now().plusSeconds(delaySeconds);

            schedulerClient.scheduleIfNotExists(
                helloWorldTask.instance(taskId),
                executionTime
            );

            log.info("Scheduled Hello World task with ID: {} to execute at: {}", taskId, executionTime);
            return ResponseEntity.ok(String.format(
                "Hello World task scheduled successfully with ID: %s, execution time: %s",
                taskId, executionTime));
        } catch (Exception e) {
            log.error("Failed to schedule Hello World task", e);
            return ResponseEntity.internalServerError()
                .body("Failed to schedule Hello World task: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Generate a document using Doc Assembly API",
        description = "Generates a document by sending a request to the Doc Assembly service "
            + "with template ID and form payload. Returns the URL of the generated document."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Document generated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - formPayload is required"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authorization token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/generate-document", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> generateDocument(
        @Parameter(
            description = "Bearer token for user authentication",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = "Authorization") String authorization,
        @Parameter(
            description = "Service-to-Service (S2S) authorization token",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Parameter(
            description = "Document generation request containing template ID and form data",
            required = true
        )
        @RequestBody JsonNode formPayload) {
        try {
            if (formPayload == null) {
                return ResponseEntity.badRequest().body("FormPayload is required");
            }
            String documentUrl = docAssemblyService.generateDocument(
                formPayload,
                "CV-SPC-CLM-ENG-01356.docx",
                OutputType.PDF,
                "generated-document.pdf"
            );
            return ResponseEntity.created(URI.create(documentUrl)).body(documentUrl);
        } catch (DocAssemblyException e) {
            log.error("Doc Assembly service error: {}", e.getMessage(), e);
            return handleDocAssemblyException(e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate document", e);
            return ResponseEntity.internalServerError()
                .body("An error occurred while processing your request.");
        }
    }


    @Operation(
        summary = "Checks the eligibility for a given property postcode",
        description = "Checks the eligibility for a given property postcode, and returns a payload "
            + "with an eligibility status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Eligibilty check completed successfully",
            content = {
                @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "Eligible postcode",
                            description = "Result for a match with an eligible postcode",
                            value = """
                                 {
                                       "status": "ELIGIBLE",
                                       "epimsId": 12345,
                                       "legislativeCountry": "England"
                                 }
                            """
                            ),
                        @ExampleObject(
                            name = "Ineligible postcode",
                            description = "Result for a match with an ineligible postcode",
                            value = """
                                {
                                  "status": "NOT_ELIGIBLE",
                                  "epimsId": 45678,
                                  "legislativeCountry": "Wales"
                                }
                            """
                            ),
                        @ExampleObject(
                            name = "Postcode that is cross-border",
                            description = "Result for a match with a cross border postcode, that needs "
                                + "the legistalative country to be specified as well",
                            value = """
                                {
                                    "status": "LEGISLATIVE_COUNTRY_REQUIRED",
                                    "legislativeCountries" : [
                                        "England",
                                        "Wales"
                                    ]
                                }
                            """
                            ),
                        @ExampleObject(
                            name = "No match found for postcode",
                            description = "No match found in the DB for the provided postcode.",
                            value = """
                                {
                                    "status": "NO_MATCH_FOUND"
                                }
                            """
                            ),
                        @ExampleObject(
                            name = "Multiple matches found for postcode",
                            description = "Multiple matches found in the DB for the provided postcode.",
                            value = """
                                {
                                    "status": "MULTIPLE_MATCHES_FOUND"
                                }
                            """
                            ),
                    })
            }),
        @ApiResponse(responseCode = "400",
            description = "Missing or blank postcode query parameter",
            content = @Content()
            ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing authorization token",
            content = @Content()
            ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Invalid or missing service authorization token",
            content = @Content()
            ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content()
            )
    })
    @GetMapping(value = "/claim-eligibility", produces = MediaType.APPLICATION_JSON_VALUE)
    public EligibilityResult getPostcodeEligibility(
        @Parameter(
            description = "Service-to-Service (S2S) authorization token",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Parameter(
            description = "Property postcode to check eligibility for",
            required = true
        )
        @RequestParam("postcode") String postcode,
        @Parameter(description = "Legislative country for property, (for use with cross border postcodes)")
        @RequestParam(value = "legislativeCountry", required = false) LegislativeCountry legislativeCountry
    ) {
        return eligibilityService.checkEligibility(postcode, legislativeCountry);
    }

    @Operation(
        summary = "Create a test case with defendants",
        description = "Creates a test case with property address, legislative country, and at least 1 defendant. "
            + "Case reference and party IDs will be auto-generated if not provided."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Case created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid payload"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authorization token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(
        value = "/create-case",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CreateTestCaseResponse> createTestCase(
        @Parameter(
            description = "Bearer token for user authentication",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = AUTHORIZATION) String authorization,
        @Parameter(
            description = "Service-to-Service (S2S) authorization token",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Parameter(
            description = "Test case creation request",
            required = true
        )
        @RequestBody CreateTestCaseRequest request
    ) {
        try {
            // Generate case reference if not provided (expand to 16 digits)
            long caseReference = Optional.ofNullable(request.getCaseReference())
                .orElseGet(this::generateCaseReference);

            // Create case using PcsCaseService
            PcsCaseEntity caseEntity = pcsCaseService.createCase(
                caseReference,
                request.getPropertyAddress(),
                request.getLegislativeCountry()
            );

            ClaimEntity mainClaim = ClaimEntity.builder()
                .pcsCase(caseEntity)
                .claimCosts(VerticalYesNo.NO)
                .build();

            caseEntity.addClaim(mainClaim);

            // Create defendants
            List<PartyEntity> createdPartyEntities = new ArrayList<>();
            for (CreateTestCaseRequest.DefendantRequest defendantRequest : request.getDefendants()) {
                // Create PartyEntity
                PartyEntity partyEntity = PartyEntity.builder()
                    .pcsCase(caseEntity)
                    .firstName(defendantRequest.getFirstName())
                    .lastName(defendantRequest.getLastName())
                    .idamId(defendantRequest.getIdamUserId())
                    .build();

                createdPartyEntities.add(partyEntity);

                caseEntity.addParty(partyEntity);
                mainClaim.addParty(partyEntity, DEFENDANT);
            }

            // Save to DB to generate party IDs
            partyRepository.saveAllAndFlush(createdPartyEntities);

            // Save case with parties and defendants
            pcsCaseRepository.save(caseEntity);

            log.info("Created test case {} with {} defendants", caseReference, request.getDefendants().size());


            List<CreateTestCaseResponse.DefendantInfo> defendantInfos = mainClaim.getClaimParties().stream()
                .filter(claimParty -> claimParty.getRole() == DEFENDANT)
                .map(ClaimPartyEntity::getParty)
                .map(party ->
                    // Create response info (accessCode will be populated after generation)
                    new CreateTestCaseResponse.DefendantInfo(
                        party.getId(),
                        party.getIdamId(),
                        party.getFirstName(),
                        party.getLastName(),
                        null  // Will be populated after access code generation
                    )
                )
                .toList();


            // Generate access codes immediately (synchronous - better for testing)
            try {
                accessCodeGenerationService.createAccessCodesForParties(String.valueOf(caseReference));
                log.info("Generated access codes for case {}", caseReference);

                // Load access codes from database and populate in response
                List<PartyAccessCodeEntity> accessCodes = partyAccessCodeRepository
                    .findAllByPcsCase_Id(caseEntity.getId());

                // Create map of partyId -> accessCode
                Map<UUID, String> partyIdToCode = accessCodes.stream()
                    .collect(Collectors.toMap(
                        PartyAccessCodeEntity::getPartyId,
                        PartyAccessCodeEntity::getCode
                    ));

                // Update defendantInfos with access codes
                defendantInfos.forEach(info -> {
                    String code = partyIdToCode.get(info.getPartyId());
                    info.setAccessCode(code);
                });

            } catch (Exception e) {
                log.warn("Failed to generate access codes for case {}: {}", caseReference, e.getMessage());
                // Don't fail the request - codes can be generated later if needed
                // Set accessCode to null for all defendants if generation failed
                defendantInfos.forEach(info -> info.setAccessCode(null));
            }

            // Build response with caseId
            CreateTestCaseResponse response = new CreateTestCaseResponse(
                caseEntity.getId(),  // caseId (UUID)
                caseReference,
                defendantInfos
            );
            return ResponseEntity.status(201).body(response);

        } catch (Exception e) {
            log.error("Failed to create test case", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Delete a test case and related access codes",
        description = "Deletes a case created for testing purposes, along with any associated party access codes."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Case deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authorization token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "404", description = "Case not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/cases/{caseReference}")
    public ResponseEntity<Void> deleteCase(
        @Parameter(
            description = "Bearer token for user authentication",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = AUTHORIZATION) String authorization,
        @Parameter(
            description = "Service-to-Service (S2S) authorization token",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Parameter(description = "Case reference to delete", required = true)
        @PathVariable long caseReference
    ) {
        try {
            Optional<PcsCaseEntity> maybeCase = pcsCaseRepository.findByCaseReference(caseReference);
            if (maybeCase.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            PcsCaseEntity pcsCaseEntity = maybeCase.get();

            List<PartyAccessCodeEntity> accessCodes = partyAccessCodeRepository.findAllByPcsCase_Id(
                pcsCaseEntity.getId()
            );
            if (!accessCodes.isEmpty()) {
                partyAccessCodeRepository.deleteAll(accessCodes);
            }

            pcsCaseRepository.delete(pcsCaseEntity);
            log.info("Deleted test case {} and {} access codes", caseReference, accessCodes.size());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete test case {}", caseReference, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get all pins associated with a case"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pins Returned"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "404", description = "Case not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/pins/{caseReference}")
    public ResponseEntity<Map<String, Party>> getPins(
        @Parameter(
            description = "Service-to-Service (S2S) authorization token",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Parameter(description = "Case reference to find pins for", required = true)
        @PathVariable long caseReference
    ) {
        try {
            Optional<PcsCaseEntity> maybeCase = pcsCaseRepository.findByCaseReference(caseReference);
            if (maybeCase.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            PcsCaseEntity pcsCaseEntity = maybeCase.get();

            List<PartyAccessCodeEntity> accessCodes = partyAccessCodeRepository.findAllByPcsCase_Id(
                pcsCaseEntity.getId()
            );

            //map partyId to party
            Map<UUID, PartyEntity> partyByPartyId = pcsCaseEntity.getParties().stream()
                .collect(Collectors.toMap(
                    PartyEntity::getId,
                    Function.identity(),
                    (existing, incoming) -> {
                        throw new IllegalStateException("Duplicate partyId: " + existing.getId());
                    }
                ));

            Map<String, Party> minimalPartyMap = new HashMap<>();

            for (var accessCodeObject : accessCodes) {
                //for each access code return the matching defendant's name and address

                String accessCode = accessCodeObject.getCode();
                UUID partyId = accessCodeObject.getPartyId();

                PartyEntity matched = partyByPartyId.get(partyId);
                if (matched == null) {
                    throw new IllegalStateException("Party is not found on Case. PartyID = " + partyId);
                }

                AddressUK addressUK;

                if (!matched.getAddressKnown().toBoolean()) {
                    addressUK = null;
                } else if (matched.getAddressSameAsProperty().toBoolean()) {
                    addressUK = modelMapper.map(pcsCaseEntity.getPropertyAddress(), AddressUK.class);
                } else {
                    addressUK = modelMapper.map(matched.getAddress(), AddressUK.class);
                }

                Party minimalParty = Party.builder()
                    .firstName(matched.getFirstName())
                    .lastName(matched.getLastName())
                    .address(addressUK)
                    .build();

                minimalPartyMap.put(accessCode, minimalParty);
            }

            return ResponseEntity.ok(minimalPartyMap);
        } catch (Exception e) {
            log.error("Failed to get Access codes / Pins {}", caseReference, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private long generateCaseReference() {
        long timestamp = System.currentTimeMillis();
        int suffix = secureRandom.nextInt(1000);
        return Long.parseLong(String.format("%d%03d", timestamp, suffix));
    }

    private ResponseEntity<String> handleDocAssemblyException(DocAssemblyException e) {
        String message = e.getMessage();

        if (message.contains("Bad request")) {
            return ResponseEntity.badRequest().body("Bad request to Doc Assembly service: " + message);
        } else if (message.contains("Authorization failed")) {
            return ResponseEntity.status(401).body("Authorization failed: " + message);
        } else if (message.contains("endpoint not found")) {
            return ResponseEntity.status(404).body("Doc Assembly service endpoint not found: " + message);
        } else if (message.contains("temporarily unavailable") || message.contains("service error")) {
            return ResponseEntity.status(503).body("Doc Assembly service is temporarily unavailable: " + message);
        } else {
            return ResponseEntity.internalServerError().body("Doc Assembly service error: " + message);
        }
    }

}
