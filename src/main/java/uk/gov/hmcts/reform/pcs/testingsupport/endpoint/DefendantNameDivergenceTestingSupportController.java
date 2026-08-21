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
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.PackRecipientResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PartyAttributeAssertationService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.isPopulated;

/**
 * Test-support only (HDPI-7686): reports who each defence pack was addressed to and what name its coversheet
 * carried, so the names on the letters described in the ticket can be checked without downloading PDFs.
 *
 * <p>A defence pack is posted to <em>every</em> party on the claim, not only the defendant who responded
 * ({@code DefencePackSelector} adds each defence form to every claimant and defendant). Each recipient's
 * coversheet is addressed with {@link RecipientAddressResolver#resolveDisplayName} applied to that
 * <em>recipient</em>, while the enclosed defence form describes the defendant who filed it. So a coversheet name
 * and an enclosed form name are only expected to match on the responder's own copy - the copy where the
 * recorded {@code PackDocumentRef.self} is true.
 *
 * <p>{@code parties} therefore reports, per party, the name a coversheet addressed to them would carry and the
 * name a defence form about them would carry, and {@code defencePacks} reports the letters actually dispatched
 * (from the {@code claim_activity_log} {@code PACK_SENT} / {@code PACK_FAILED} rows, the same rows the ticket
 * reads letter ids from) with the recipient each was addressed to.
 *
 * <p>Remove with the rest of this branch.
 */
@Slf4j
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
@Tag(name = "Testing Support")
public class DefendantNameDivergenceTestingSupportController {

    private final PcsCaseRepository pcsCaseRepository;
    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final RecipientAddressResolver recipientAddressResolver;
    private final PartyAttributeAssertationService partyAttributeAssertationService;
    private final PackRecipientResolver packRecipientResolver;
    private final ObjectMapper objectMapper;

    public DefendantNameDivergenceTestingSupportController(
        PcsCaseRepository pcsCaseRepository,
        ClaimActivityLogRepository claimActivityLogRepository,
        RecipientAddressResolver recipientAddressResolver,
        PartyAttributeAssertationService partyAttributeAssertationService,
        PackRecipientResolver packRecipientResolver,
        ObjectMapper objectMapper
    ) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.recipientAddressResolver = recipientAddressResolver;
        this.partyAttributeAssertationService = partyAttributeAssertationService;
        this.packRecipientResolver = packRecipientResolver;
        this.objectMapper = objectMapper;
    }

    @Operation(
        summary = "Report the defence packs dispatched for a case and the name on each coversheet",
        description = "HDPI-7686 diagnostic. Returns every party with the name a coversheet addressed to them "
            + "would carry, plus each dispatched defence pack with its recipient and letter id."
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

        return ResponseEntity.ok(new CaseNameReport(
            caseReference,
            pcsCase.getId(),
            parties(pcsCase),
            defencePacks(pcsCase),
            pendingDefencePackRecipients(pcsCase.getId())
        ));
    }

    private List<PartyNames> parties(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return List.of();
        }
        return pcsCase.getClaims().getFirst().getClaimParties().stream()
            .sorted(Comparator.comparing(ClaimPartyEntity::getRole)
                .thenComparing(ClaimPartyEntity::getRank, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(this::describeParty)
            .toList();
    }

    private PartyNames describeParty(ClaimPartyEntity claimParty) {
        PartyEntity party = claimParty.getParty();

        // Exactly what a coversheet addressed to this party prints - the production resolver, unmodified.
        String coversheetName = recipientAddressResolver.resolveDisplayName(party);

        String assertedName = assertedName(party.getId());
        String assertedNameJoined = joinAssertedName(assertedName);

        // Mirrors DefenceFormPayloadBuilder.resolveDefendantName: the assertion wins when populated, otherwise
        // its own displayName fallback (org name, else joined party name - note no nameKnown gate, so a party
        // with no name yields blank here where the coversheet yields "Persons unknown").
        String ownFormName = isPopulated(assertedNameJoined) ? assertedNameJoined : formFallbackName(party);

        return new PartyNames(
            party.getId(),
            claimParty.getRole().name(),
            claimParty.getRank(),
            party.getFirstName(),
            party.getLastName(),
            party.getOrgName(),
            party.getNameKnown(),
            coversheetName,
            ownFormName,
            assertedName
        );
    }

    /**
     * The defence packs actually dispatched, newest last. {@code recipientCoversheetName} is resolved now rather
     * than read back from the letter, so a name changed after dispatch shows the current value - which is the
     * point when checking whether a later name change reached an already-posted letter.
     */
    private List<SentPack> defencePacks(PcsCaseEntity pcsCase) {
        return claimActivityLogRepository.findAllByPcsCase_Id(pcsCase.getId()).stream()
            .filter(row -> row.getActivityType() == ClaimActivityType.PACK_SENT
                || row.getActivityType() == ClaimActivityType.PACK_FAILED)
            .sorted(Comparator.comparing(ClaimActivityLogEntity::getCreatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())))
            .map(this::describePack)
            .filter(pack -> pack != null && LetterType.DEFENCE_PACK.name().equals(pack.packType()))
            .toList();
    }

    private SentPack describePack(ClaimActivityLogEntity row) {
        PackDetails details = readPackDetails(row);
        if (details == null) {
            return null;
        }
        PartyEntity recipient = row.getParty();

        return new SentPack(
            details.letterId(),
            details.packType() == null ? null : details.packType().name(),
            row.getStatus() == null ? null : row.getStatus().name(),
            recipient == null ? null : recipient.getId(),
            recipient == null ? null : recipientAddressResolver.resolveDisplayName(recipient),
            details.documents() == null ? List.of() : details.documents().stream()
                .map(document -> new PackDoc(
                    document.id(),
                    document.type() == null ? null : document.type().name(),
                    document.defendantNumber(),
                    document.self()
                ))
                .toList(),
            details.failureReason() == null ? null : details.failureReason().name(),
            row.getCreatedAt()
        );
    }

    private PackDetails readPackDetails(ClaimActivityLogEntity row) {
        if (row.getDetails() == null) {
            return null;
        }
        try {
            return objectMapper.readValue(row.getDetails(), PackDetails.class);
        } catch (Exception e) {
            log.warn("Unreadable pack details on activity row {}", row.getId(), e);
            return null;
        }
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
     * Recipients of a defence pack that is due but not yet posted. Empty once the sweep has sent everything.
     */
    private List<PendingRecipient> pendingDefencePackRecipients(UUID caseId) {
        try {
            return packRecipientResolver.resolveDefenceRecipients(caseId).stream()
                .map(recipient -> new PendingRecipient(
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

    public record CaseNameReport(long caseReference,
                                 UUID caseId,
                                 List<PartyNames> parties,
                                 List<SentPack> defencePacks,
                                 List<PendingRecipient> pendingDefencePacks) {
    }

    /**
     * {@code coversheetName} is what a coversheet addressed to this party says; {@code ownFormName} is what a
     * defence form about this party says. They are only expected to agree on this party's own copy of their own
     * defence pack.
     */
    public record PartyNames(UUID partyId,
                             String role,
                             Integer rank,
                             String firstName,
                             String lastName,
                             String orgName,
                             VerticalYesNo nameKnown,
                             String coversheetName,
                             String ownFormName,
                             String defendantNameAssertion) {
    }

    /**
     * One dispatched pack: {@code self} on a document marks the recipient's own filing.
     */
    public record SentPack(UUID letterId,
                           String packType,
                           String status,
                           UUID recipientPartyId,
                           String recipientCoversheetName,
                           List<PackDoc> documents,
                           String failureReason,
                           LocalDateTime createdAt) {
    }

    public record PackDoc(UUID documentId, String documentType, Integer defendantNumber, boolean self) {
    }

    public record PendingRecipient(UUID partyId, String letterType, String recipientName) {
    }
}
