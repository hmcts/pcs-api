package uk.gov.hmcts.reform.pcs.noc;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class NoCService {

    private static final String ANSWERS_NOT_MATCHED_ANY_LITIGANT = "answers-not-matched-any-litigant";
    private static final String ANSWERS_EMPTY = "answers-empty";
    private static final String CASE_TYPE_ID = "PCS";
    private static final String CASE_ROLE = "[CLAIMANTSOLICITOR]";
    private static final String CHALLENGE_ID = "NoCChallenge";
    private static final String QUESTION_ID = "pcsClaimantName";
    private static final String QUESTION_TEXT = "Enter the claimant name";
    private static final String ANSWER_FIELD = "${nocClaimantName}:" + CASE_ROLE;
    private static final String APPROVED = "APPROVED";

    private final PcsCaseRepository pcsCaseRepository;

    public Map<String, Object> questions(String caseId) {
        loadClaimantParty(caseId);

        Map<String, Object> answerFieldType = map();
        answerFieldType.put("id", "Text");
        answerFieldType.put("type", "Text");
        answerFieldType.put("min", null);
        answerFieldType.put("max", null);
        answerFieldType.put("regular_expression", null);
        answerFieldType.put("fixed_list_items", List.of());
        answerFieldType.put("complex_fields", List.of());
        answerFieldType.put("collection_field_type", null);

        Map<String, Object> question = map();
        question.put("case_type_id", CASE_TYPE_ID);
        question.put("order", 1);
        question.put("question_text", QUESTION_TEXT);
        question.put("answer_field_type", answerFieldType);
        question.put("display_context_parameter", null);
        question.put("challenge_question_id", CHALLENGE_ID);
        question.put("answer_field", ANSWER_FIELD);
        question.put("question_id", QUESTION_ID);
        question.put("ignore_null_fields", false);

        Map<String, Object> response = map();
        response.put("questions", List.of(question));
        return response;
    }

    public NoCResponse verifyAnswers(NoCRequest request) {
        validateAnswers(request);
        return NoCResponse.builder()
            .code("answers-validated")
            .statusMessage("The answers matched a PCS litigant")
            .build();
    }

    public NoCResponse submitNoticeOfChange(NoCRequest request) {
        validateAnswers(request);
        return NoCResponse.builder()
            .approvalStatus("APPROVED")
            .caseRole("[CLAIMANTSOLICITOR]")
            .code("noc-request-approved")
            .statusMessage("Notice of change request approved")
            .build();
    }

    public Map<String, Object> providerVerify(NoCRequest request) {
        PartyEntity party = validateAnswers(request);

        Map<String, Object> response = providerDecisionPayload(request.getCaseId(), party);
        response.put("status", "VERIFIED");
        response.put("statusMessage", "Notice of change answers verified successfully");
        return response;
    }

    public Map<String, Object> providerPrepareRequest(NoCRequest request) {
        PartyEntity party = validateAnswers(request);

        Map<String, Object> response = providerDecisionPayload(request.getCaseId(), party);
        response.put("approvalStatus", APPROVED);
        return response;
    }

    public Map<String, Object> providerApplyDecision(Map<String, Object> request) {
        Map<String, Object> response = map();
        response.put("status", "APPLIED");
        response.put("accessDelta", accessDelta());
        response.put("notifications", notifications());
        return response;
    }

    public Map<String, Object> providerAccessDeltaApplied(Map<String, Object> request) {
        Map<String, Object> response = map();
        response.put("status", "ACKNOWLEDGED");
        return response;
    }

    private PartyEntity validateAnswers(NoCRequest request) {
        if (request == null || request.getCaseId() == null || request.getAnswers() == null
            || request.getAnswers().isEmpty()) {
            throw badRequest(ANSWERS_EMPTY, "Challenge question answers can not be empty");
        }

        PartyEntity party = loadClaimantParty(request.getCaseId());
        String expectedClaimantName = party.getOrgName();
        boolean matched = request.getAnswers().stream()
            .map(NoCAnswer::getValue)
            .anyMatch(answer -> answer != null && answer.trim().equalsIgnoreCase(expectedClaimantName));

        if (!matched) {
            throw badRequest(ANSWERS_NOT_MATCHED_ANY_LITIGANT, "The answers did not match those for any litigant");
        }

        return party;
    }

    private PartyEntity loadClaimantParty(String caseId) {
        long caseReference = parseCaseReference(caseId);
        PcsCaseEntity pcsCase = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> badRequest(ANSWERS_NOT_MATCHED_ANY_LITIGANT,
                "The answers did not match those for any litigant"));

        return pcsCase.getParties().stream()
            .filter(party -> party.getOrgName() != null && !party.getOrgName().isBlank())
            .findFirst()
            .orElseThrow(() -> badRequest(ANSWERS_NOT_MATCHED_ANY_LITIGANT,
                "The answers did not match those for any litigant"));
    }

    private long parseCaseReference(String caseId) {
        try {
            return Long.parseLong(caseId);
        } catch (NumberFormatException exception) {
            throw badRequest(ANSWERS_NOT_MATCHED_ANY_LITIGANT,
                "The answers did not match those for any litigant");
        }
    }

    private ResponseStatusException badRequest(String code, String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message + " [" + code + "]");
    }

    private Map<String, Object> providerDecisionPayload(String caseId, PartyEntity party) {
        Map<String, Object> response = map();
        response.put("caseRole", CASE_ROLE);
        response.put("party", party(party));
        response.put("incomingOrganisation", organisation("incoming-pcs-organisation", "Incoming PCS Organisation"));
        response.put("incumbentOrganisation", organisation("incumbent-pcs-organisation", party.getOrgName()));
        response.put("providerContext", providerContext(caseId));
        return response;
    }

    private Map<String, Object> party(PartyEntity party) {
        Map<String, Object> response = map();
        response.put("partyId", party.getId().toString());
        response.put("partyRole", "claimant");
        response.put("displayName", party.getOrgName());
        return response;
    }

    private Map<String, Object> organisation(String organisationId, String organisationName) {
        Map<String, Object> response = map();
        response.put("organisationId", organisationId);
        response.put("organisationName", organisationName);
        return response;
    }

    private Map<String, Object> providerContext(String caseId) {
        Map<String, Object> response = map();
        response.put("contextId", UUID.nameUUIDFromBytes(caseId.getBytes()).toString());
        response.put("expiresAt", OffsetDateTime.now().plusHours(1).toString());
        return response;
    }

    private Map<String, Object> accessDelta() {
        Map<String, Object> response = map();
        response.put("grants", List.of());
        response.put("revokes", List.of());
        response.put("retained", List.of());
        return response;
    }

    private Map<String, Object> notifications() {
        Map<String, Object> response = map();
        response.put("caseAccessRemoved", List.of());
        return response;
    }

    private Map<String, Object> map() {
        return new LinkedHashMap<>();
    }
}
