package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.PackRecipientResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PartyAttributeAssertationService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.isPopulated;

/**
 * Test-support only (HDPI-7686): exposes, per defendant, the name the bulk-print coversheet would carry
 * alongside the name the enclosed defence form would carry, so the divergence reported in HDPI-7686 can be
 * observed without access to the send-letter service.
 *
 * <p>The coversheet name comes from the production resolver itself
 * ({@link RecipientAddressResolver#resolveDisplayName}, reached from
 * {@code PackRecipientResolver.resolveDefenceRecipient}), which reads only {@code PartyEntity}. The defence form
 * instead prefers the defendant's {@code DEFENDANT_NAME} party-attribute assertion
 * ({@code DefenceFormPayloadBuilder.resolveDefendantName}), which is what this endpoint reports as the form name.
 * When a defendant has disputed or supplied their own name, the two sources can disagree — that disagreement is
 * the defect.
 *
 * <p>Remove once HDPI-7686 is fixed and the two names come from one shared resolver.
 */
@Slf4j
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
@Tag(name = "Testing Support")
public class DefendantNameDivergenceTestingSupportController {

    private final PcsCaseRepository pcsCaseRepository;
    private final RecipientAddressResolver recipientAddressResolver;
    private final PartyAttributeAssertationService partyAttributeAssertationService;
    private final PackRecipientResolver packRecipientResolver;
    private final ObjectMapper objectMapper;

    public DefendantNameDivergenceTestingSupportController(
        PcsCaseRepository pcsCaseRepository,
        RecipientAddressResolver recipientAddressResolver,
        PartyAttributeAssertationService partyAttributeAssertationService,
        PackRecipientResolver packRecipientResolver,
        ObjectMapper objectMapper
    ) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.recipientAddressResolver = recipientAddressResolver;
        this.partyAttributeAssertationService = partyAttributeAssertationService;
        this.packRecipientResolver = packRecipientResolver;
        this.objectMapper = objectMapper;
    }

    @Operation(
        summary = "Compare the coversheet name against the defence form name for every defendant on a case",
        description = "HDPI-7686 diagnostic. Returns both resolved names per defendant plus the underlying party "
            + "fields and name assertion, so a test can assert whether the envelope matches the enclosed form."
    )
    @GetMapping(value = "/defendant-name-divergence/{caseReference}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<CaseNameReport> getDefendantNameDivergence(
        @Parameter(description = "Service-to-Service (S2S) authorization token", required = true)
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Parameter(description = "The 12-digit case reference", required = true)
        @PathVariable long caseReference
    ) {
        PcsCaseEntity pcsCase = pcsCaseRepository.findByCaseReference(caseReference).orElse(null);
        if (pcsCase == null) {
            return ResponseEntity.notFound().build();
        }

        List<DefendantNames> defendants = defendantClaimParties(pcsCase).stream()
            .map(this::describeDefendant)
            .toList();

        return ResponseEntity.ok(new CaseNameReport(
            caseReference,
            pcsCase.getId(),
            defendants,
            defendants.stream().anyMatch(DefendantNames::diverges),
            resolvedDefencePackRecipients(pcsCase.getId())
        ));
    }

    private List<ClaimPartyEntity> defendantClaimParties(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return List.of();
        }
        return pcsCase.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .sorted(Comparator.comparing(ClaimPartyEntity::getRank, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    private DefendantNames describeDefendant(ClaimPartyEntity claimParty) {
        PartyEntity party = claimParty.getParty();

        // Exactly what the bulk-print coversheet prints - the production resolver, unmodified.
        String coversheetName = recipientAddressResolver.resolveDisplayName(party);

        String assertedName = assertedName(party.getId());
        String assertedNameJoined = joinAssertedName(assertedName);

        // Mirrors DefenceFormPayloadBuilder.resolveDefendantName: the assertion wins when populated,
        // otherwise its own displayName fallback (org name, else joined party name - note no nameKnown gate).
        String formName = isPopulated(assertedNameJoined) ? assertedNameJoined : formFallbackName(party);

        return new DefendantNames(
            party.getId(),
            claimParty.getRank(),
            party.getFirstName(),
            party.getLastName(),
            party.getOrgName(),
            party.getNameKnown(),
            coversheetName,
            formName,
            assertedName,
            !Objects.equals(coversheetName, formName)
        );
    }

    /**
     * Mirrors the private {@code DefenceFormPayloadBuilder.displayName} fallback.
     */
    private static String formFallbackName(PartyEntity party) {
        if (isPopulated(party.getOrgName())) {
            return party.getOrgName();
        }
        return PartyDisplayMapper.joinName(party.getFirstName(), party.getLastName());
    }

    private String assertedName(UUID partyId) {
        return partyAttributeAssertationService.getSubmittedAssertionsForParty(partyId).stream()
            .filter(assertion -> assertion.getAttributesName() == PartyAttributeType.DEFENDANT_NAME)
            .map(PartyAttributeAssertationEntity::getAssertedValue)
            .reduce((first, second) -> second)
            .orElse(null);
    }

    private String joinAssertedName(String assertedName) {
        if (!isPopulated(assertedName)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(assertedName);
            return PartyDisplayMapper.joinName(text(node, "firstName"), text(node, "lastName"));
        } catch (Exception e) {
            log.error("Failed to parse defendant name assertion", e);
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * The live defence-pack recipients, when a pack is currently due. Empty is normal - the selector only
     * returns candidates while an unsent defence pack exists - so tests should rely on the per-defendant
     * comparison above and treat this as corroboration when present.
     */
    private List<PackRecipientName> resolvedDefencePackRecipients(UUID caseId) {
        try {
            return packRecipientResolver.resolveDefenceRecipients(caseId).stream()
                .map(recipient -> new PackRecipientName(
                    recipient.recipient().getId(),
                    recipient.letterType().name(),
                    recipient.recipientName()
                ))
                .toList();
        } catch (Exception e) {
            log.warn("Could not resolve defence pack recipients for case {}", caseId, e);
            return List.of();
        }
    }

    /**
     * Per-case report. {@code anyDivergence} is the single assertion a test needs.
     */
    public record CaseNameReport(long caseReference,
                                 UUID caseId,
                                 List<DefendantNames> defendants,
                                 boolean anyDivergence,
                                 List<PackRecipientName> resolvedDefencePackRecipients) {
    }

    /**
     * {@code coversheetName} is what the envelope says, {@code formName} what the enclosed defence form says.
     * They should always be equal.
     */
    public record DefendantNames(UUID partyId,
                                 Integer rank,
                                 String partyFirstName,
                                 String partyLastName,
                                 String partyOrgName,
                                 VerticalYesNo partyNameKnown,
                                 String coversheetName,
                                 String formName,
                                 String defendantNameAssertion,
                                 boolean diverges) {
    }

    public record PackRecipientName(UUID partyId, String letterType, String recipientName) {
    }
}
