package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimKind;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimOrganisationException;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimantOrganisationResolver;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.MortgageBulkClaimPreparer;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.RentalBulkClaimPreparer;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimSubmissionService.ClaimInput;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimResponseBuilder;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimSchemaValidationException;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimXmlCodec;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.BulkClaimResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.BulkClaimStatusListResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.BulkClaimStatusRequest;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.BulkClaimStatusResponse;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimResponseType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimStatusType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.MortgageClaimSet;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.PaymentStatusType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentalClaimSet;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles bulk claim requests: XML in, PCOL-style XML out.
 *
 * <p>Response mapping:
 * <ul>
 *   <li>schema errors: 400, overall SCHEMA_INVALID, with the errors on a single claim response that carries no
 *       customer unique identifier (path is the line and column, code {@value #SCHEMA_ERROR_CODE});</li>
 *   <li>organisation unusable: 403, overall ORGANISATION_NOT_USABLE;</li>
 *   <li>invalid set-level payment account: 200, overall PBA_NOT_VALID and no claims processed;</li>
 *   <li>otherwise 200 with a response per claim, overall success only when every claim succeeded.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultBulkClaimV2Handler implements BulkClaimV2Handler {

    static final String SCHEMA_ERROR_CODE = "SCHEMA_INVALID";
    static final Set<String> SCHEMA_FILES = Set.of(
        "Common.xsd", "RentalClaim.xsd", "MortgageClaim.xsd", "BulkClaimResponse.xsd", "BulkClaimStatusResponse.xsd",
        "BulkClaimStatusRequest.xsd", "BulkClaimStatusListResponse.xsd");

    private static final String SCHEMA_DIR = "xsd/bulkclaim/v2/";
    private static final int MAX_SCHEMA_ERRORS = 100;
    private static final Pattern LOCATED_ERROR = Pattern.compile("^(\\d+):(\\d+) (.*)$", Pattern.DOTALL);
    private static final MediaType XML = MediaType.APPLICATION_XML;

    private final BulkClaimXmlCodec codec;
    private final BulkClaimResponseBuilder responseBuilder;
    private final BulkClaimProperties properties;
    private final BulkClaimantOrganisationResolver organisationResolver;
    private final BulkClaimSubmissionService submissionService;
    private final BulkClaimSubmissionRepository submissionRepository;
    private final BulkClaimCaseStateReader caseStateReader;
    private final RentalBulkClaimPreparer rentalPreparer;
    private final MortgageBulkClaimPreparer mortgagePreparer;

    @Override
    public ResponseEntity<byte[]> submitRental(byte[] xml, String organisationId, boolean validateOnly) {
        RentalClaimSet claimSet;
        try {
            claimSet = codec.parseRental(xml);
        } catch (BulkClaimSchemaValidationException e) {
            return schemaInvalid(e);
        }
        if (claimSet.getRentalClaim().size() > properties.getMaxClaimsPerRequest()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        return process(organisationId, claimSet.getPaymentAccountNumber(), validateOnly,
            organisation -> claimSet.getRentalClaim().stream()
                .map(claim -> new ClaimInput(
                    claim.getCustomerUniqueIdentifier(), codec.claimHash(claim), BulkClaimKind.RENTAL,
                    claimSet.getVersion(),
                    () -> rentalPreparer.prepare(claimSet, claim, organisation)))
                .toList());
    }

    @Override
    public ResponseEntity<byte[]> submitMortgage(byte[] xml, String organisationId, boolean validateOnly) {
        MortgageClaimSet claimSet;
        try {
            claimSet = codec.parseMortgage(xml);
        } catch (BulkClaimSchemaValidationException e) {
            return schemaInvalid(e);
        }
        if (claimSet.getMortgageClaim().size() > properties.getMaxClaimsPerRequest()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        return process(organisationId, claimSet.getPaymentAccountNumber(), validateOnly,
            organisation -> claimSet.getMortgageClaim().stream()
                .map(claim -> new ClaimInput(
                    claim.getCustomerUniqueIdentifier(), codec.claimHash(claim), BulkClaimKind.MORTGAGE,
                    claimSet.getVersion(),
                    () -> mortgagePreparer.prepare(claimSet, claim, organisation)))
                .toList());
    }

    @Override
    public ResponseEntity<byte[]> status(String organisationId, String customerUniqueIdentifier) {
        return submissionRepository
            .findByOrganisationIdAndCustomerUniqueIdentifier(organisationId, customerUniqueIdentifier)
            .map(submission -> xml(codec.write(singleStatusOf(submission))))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<byte[]> statusBatch(byte[] xml, String organisationId) {
        BulkClaimStatusRequest request;
        try {
            request = codec.parseStatusRequest(xml);
        } catch (BulkClaimSchemaValidationException e) {
            return schemaInvalid(e);
        }
        List<String> identifiers = request.getCustomerUniqueIdentifier();
        if (identifiers.size() > properties.getMaxClaimsPerRequest()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        Map<String, BulkClaimSubmissionEntity> submissions = new HashMap<>();
        submissionRepository
            .findByOrganisationIdAndCustomerUniqueIdentifierIn(organisationId, new HashSet<>(identifiers))
            .forEach(submission -> submissions.put(submission.getCustomerUniqueIdentifier(), submission));
        Map<Long, String> caseStates = caseStateReader.statesOf(
            submissions.values().stream().map(BulkClaimSubmissionEntity::getCaseReference)
                .filter(Objects::nonNull).collect(Collectors.toSet()));

        BulkClaimStatusListResponse response = new BulkClaimStatusListResponse();
        for (String identifier : identifiers) {
            BulkClaimSubmissionEntity submission = submissions.get(identifier);
            response.getClaim().add(submission == null
                ? notFoundStatus(identifier)
                : statusOf(submission, caseState(caseStates, submission)));
        }
        return xml(codec.write(response));
    }

    @Override
    public ResponseEntity<byte[]> schema(String fileName) {
        if (!SCHEMA_FILES.contains(fileName)) {
            return ResponseEntity.notFound().build();
        }
        try (InputStream stream = new ClassPathResource(SCHEMA_DIR + fileName).getInputStream()) {
            return xml(stream.readAllBytes());
        } catch (IOException e) {
            log.error("Unable to read bulk claim schema {}", fileName, e);
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<byte[]> process(String organisationId,
                                           String setPbaAccountNumber,
                                           boolean validateOnly,
                                           Function<OrganisationDetailsResponse, List<ClaimInput>> claimInputs) {
        OrganisationDetailsResponse organisation;
        try {
            organisation = organisationResolver.resolve(organisationId);
        } catch (BulkClaimOrganisationException e) {
            log.info("Organisation {} cannot submit bulk claims: {}", organisationId, e.getReason());
            return xml(HttpStatus.FORBIDDEN,
                       codec.write(responseBuilder.overallFailure(BulkFailReason.ORGANISATION_NOT_USABLE, null)));
        }

        if (!organisationResolver.isPbaAccountValid(organisation, setPbaAccountNumber)) {
            return xml(codec.write(responseBuilder.overallFailure(BulkFailReason.PBA_NOT_VALID, null)));
        }

        List<ClaimResponseType> responses = submissionService.submit(
            organisationId, organisation, claimInputs.apply(organisation), validateOnly);

        boolean allSucceeded = responses.stream()
            .allMatch(response -> response.getClaimStatus().getStatusCode() == BulkFailReason.CLAIM_SUCCESS
                .getStatusCode());
        BulkClaimResponse response = allSucceeded
            ? responseBuilder.overallSuccess(responses)
            : responseBuilder.overallFailure(BulkFailReason.CLAIMS_FAILED, responses);
        return xml(codec.write(response));
    }

    private ResponseEntity<byte[]> schemaInvalid(BulkClaimSchemaValidationException e) {
        List<BulkValidationError> errors = e.getErrors().stream()
            .limit(MAX_SCHEMA_ERRORS)
            .map(DefaultBulkClaimV2Handler::toValidationError)
            .toList();
        BulkClaimResponse response = responseBuilder.overallFailure(
            BulkFailReason.SCHEMA_INVALID,
            List.of(responseBuilder.claimFailure(null, BulkFailReason.SCHEMA_INVALID, errors)));
        return xml(HttpStatus.BAD_REQUEST, codec.write(response));
    }

    private static BulkValidationError toValidationError(String error) {
        Matcher located = LOCATED_ERROR.matcher(error);
        if (located.matches()) {
            return new BulkValidationError(
                "line " + located.group(1) + ", column " + located.group(2), SCHEMA_ERROR_CODE, located.group(3));
        }
        return new BulkValidationError("/", SCHEMA_ERROR_CODE, error);
    }

    private BulkClaimStatusResponse singleStatusOf(BulkClaimSubmissionEntity submission) {
        BulkClaimStatusResponse status = new BulkClaimStatusResponse();
        populate(status, submission, submission.getCaseReference() == null
            ? Optional.empty()
            : caseStateReader.stateOf(submission.getCaseReference()));
        return status;
    }

    private ClaimStatusType statusOf(BulkClaimSubmissionEntity submission, Optional<String> caseState) {
        ClaimStatusType status = new ClaimStatusType();
        populate(status, submission, caseState);
        return status;
    }

    private static Optional<String> caseState(Map<Long, String> caseStates, BulkClaimSubmissionEntity submission) {
        return submission.getCaseReference() == null
            ? Optional.empty()
            : Optional.ofNullable(caseStates.get(submission.getCaseReference()));
    }

    private ClaimStatusType notFoundStatus(String customerUniqueIdentifier) {
        ClaimStatusType status = new ClaimStatusType();
        status.setCustomerUniqueIdentifier(customerUniqueIdentifier);
        status.setClaimStatus(responseBuilder.status(BulkFailReason.CLAIM_NOT_FOUND));
        return status;
    }

    private void populate(ClaimStatusType status, BulkClaimSubmissionEntity submission, Optional<String> caseState) {
        status.setCustomerUniqueIdentifier(submission.getCustomerUniqueIdentifier());
        status.setClaimStatus(responseBuilder.status(claimReason(submission)));
        if (submission.getCaseReference() != null) {
            status.setCaseReference(String.valueOf(submission.getCaseReference()));
            caseState.ifPresent(status::setCaseState);
        }
        status.setServiceRequestReference(submission.getServiceRequestReference());
        if (submission.getPaymentStatus() != null) {
            status.setPaymentStatus(PaymentStatusType.fromValue(submission.getPaymentStatus().name()));
        }
        status.setPaymentFailureReason(submission.getPaymentFailureReason());
    }

    private static BulkFailReason claimReason(BulkClaimSubmissionEntity submission) {
        return switch (submission.getStatus()) {
            case CASE_CREATED -> BulkFailReason.CLAIM_SUCCESS;
            case PENDING -> submission.getCaseReference() != null
                ? BulkFailReason.CLAIM_SUCCESS
                : BulkFailReason.CLAIM_PENDING;
            case FAILED_VALIDATION -> BulkFailReasonCodes.forFailReasonCode(submission.getFailReasonCode())
                .orElse(BulkFailReason.VALIDATION_FAILED);
            case FAILED -> BulkFailReasonCodes.forFailReasonCode(submission.getFailReasonCode())
                .orElse(BulkFailReason.SAVE_FAILED);
        };
    }

    private static ResponseEntity<byte[]> xml(byte[] body) {
        return xml(HttpStatus.OK, body);
    }

    private static ResponseEntity<byte[]> xml(HttpStatus status, byte[] body) {
        return ResponseEntity.status(status).contentType(XML).body(body);
    }
}
