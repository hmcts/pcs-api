package uk.gov.hmcts.reform.pcs.enforcement.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffAllocationRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The transactional reads and writes of enforcement document generation, separated from the orchestration in
 * {@link EnforcementDocumentGenerator} so the Docmosis render runs outside any transaction. The payload is
 * built in a read-only transaction, so it can read the enforcement's lazy relations, and the rendered document
 * is filed in a short write transaction. Both phases check whether the document has already been generated, so
 * a retry never files a second copy.
 */
@Slf4j
@Service
public class EnforcementDocumentPersistenceService {

    static final String ENFORCEMENT_CATEGORY_ID = FeatureCaseFileCategory.ENFORCEMENT.getId();

    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a",
                                                                                      Locale.UK);

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final DocumentImportService documentImportService;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final ModelMapper modelMapper;
    private final Clock ukClock;
    private final LocationReferenceService locationReferenceService;
    private final BailiffAllocationRepository bailiffAllocationRepository;

    public EnforcementDocumentPersistenceService(EnforcementOrderRepository enforcementOrderRepository,
                                                 DocumentImportService documentImportService,
                                                 CaseReferenceFormatter caseReferenceFormatter,
                                                 CaseNameFormatter caseNameFormatter,
                                                 AddressMapper addressMapper,
                                                 AddressFormatter addressFormatter,
                                                 ModelMapper modelMapper,
                                                 @Qualifier("ukClock") Clock ukClock,
                                                 LocationReferenceService locationReferenceService,
                                                 BailiffAllocationRepository bailiffAllocationRepository) {
        this.enforcementOrderRepository = enforcementOrderRepository;
        this.documentImportService = documentImportService;
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.caseNameFormatter = caseNameFormatter;
        this.addressMapper = addressMapper;
        this.addressFormatter = addressFormatter;
        this.modelMapper = modelMapper;
        this.ukClock = ukClock;
        this.locationReferenceService = locationReferenceService;
        this.bailiffAllocationRepository = bailiffAllocationRepository;
    }

    /**
     * The payload for the document, unless the enforcement already has the one document of this kind it can
     * have.
     */
    @Transactional(readOnly = true)
    public Optional<EnforcementDocumentPayload> buildPayloadIfNotGenerated(UUID enforcementId,
                                                                          EnforcementOutput output) {
        EnforcementOrderEntity enforcement = loadEnforcement(enforcementId);
        if (alreadyGenerated(enforcement, output)) {
            log.info("Enforcement {} already has a {}; not generating another", enforcementId, output.getTitle());
            return Optional.empty();
        }
        return Optional.of(buildPayload(enforcement, output, enforcement.getClaim().getPcsCase()));
    }

    /**
     * Files the rendered document in the Enforcement folder of Case File View, on both the case and the
     * enforcement.
     *
     * @return the filed document, or empty if another attempt filed it first
     */
    @Transactional
    public Optional<DocumentEntity> attach(UUID enforcementId, EnforcementOutput output, String documentUrl) {
        EnforcementOrderEntity enforcement = loadEnforcement(enforcementId);
        if (alreadyGenerated(enforcement, output)) {
            log.info("Enforcement {} already has a {}; not filing another", enforcementId, output.getTitle());
            return Optional.empty();
        }

        PcsCaseEntity pcsCase = enforcement.getClaim().getPcsCase();
        DocumentEntity document = documentImportService.addDocumentToCase(pcsCase, documentUrl,
                                                                          ENFORCEMENT_CATEGORY_ID);
        document.setType(output.getDocumentType());
        enforcement.addDocuments(List.of(document));
        enforcementOrderRepository.save(enforcement);
        log.info("Generated the {} for enforcement {} on case {}", output.getTitle(), enforcementId,
                 pcsCase.getCaseReference());
        return Optional.of(document);
    }

    String outputFilename(EnforcementOutput output, EnforcementDocumentPayload payload) {
        String reference = payload.getEnforcementNumber() != null
            ? payload.getEnforcementNumber() : payload.getCaseReference();
        return output.getTitle() + " - " + reference;
    }

    EnforcementDocumentPayload buildPayload(EnforcementOrderEntity enforcement, EnforcementOutput output,
                                            PcsCaseEntity pcsCase) {
        SelectEnforcementType enforcementType = SelectEnforcementType.from(enforcement.getEnforcementOrder());
        Optional<CourtVenue> court = caseManagementCourt(pcsCase);
        return EnforcementDocumentPayload.builder()
            .documentTitle(output.getTitle())
            .caseReference(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()))
            .caseName(caseName(enforcement.getClaim()))
            .propertyAddress(pcsCase.getPropertyAddress() == null ? null : addressFormatter.formatFullAddress(
                addressMapper.toAddressUK(pcsCase.getPropertyAddress()), AddressFormatter.NEWLINE_DELIMITER))
            .enforcementType(enforcementType == null ? null : enforcementType.getLabel())
            .enforcementNumber(enforcement.getEnforcementNumber())
            .seal(output.getSeal().getLabel())
            .generatedAt(GENERATED_AT.format(LocalDateTime.now(ukClock)).toUpperCase(Locale.UK))
            .courtName(court.map(CourtVenue::courtName).orElse(null))
            .courtAddress(court.map(EnforcementDocumentPersistenceService::courtAddress).orElse(null))
            .bailiffName(bailiffName(enforcement))
            .language(language(enforcement.getEnforcementOrder(), enforcementType))
            .evictionDateTime(enforcement.getBailiffDate())
            .dispenseWithService(enforcement.getDispenseWithService())
            .enforcementOrder(enforcement.getEnforcementOrder())
            .build();
    }

    private EnforcementOrderEntity loadEnforcement(UUID enforcementId) {
        return enforcementOrderRepository.findById(enforcementId)
            .orElseThrow(() -> new IllegalStateException("No enforcement with id " + enforcementId));
    }

    /**
     * The court the case is managed by, as the access code form names it. A Location Reference failure is let
     * through so the generation task retries.
     */
    private Optional<CourtVenue> caseManagementCourt(PcsCaseEntity pcsCase) {
        Integer epimsId = pcsCase.getCaseManagementLocation();
        if (epimsId == null) {
            log.warn("Case {} has no case management location, so its enforcement document names no court",
                     pcsCase.getCaseReference());
            return Optional.empty();
        }
        List<CourtVenue> venues = locationReferenceService.getCourtVenues(List.of(epimsId));
        if (venues == null || venues.isEmpty()) {
            log.warn("No court venue found for epimsId {} on case {}, so its enforcement document names no court",
                     epimsId, pcsCase.getCaseReference());
            return Optional.empty();
        }
        return Optional.of(venues.getFirst());
    }

    private static String courtAddress(CourtVenue court) {
        return Stream.of(court.courtAddress(), court.postcode())
            .filter(StringUtils::isNotBlank)
            .flatMap(part -> Stream.of(part.split(",")))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .reduce((a, b) -> a + AddressFormatter.NEWLINE_DELIMITER + b)
            .orElse(null);
    }

    private String bailiffName(EnforcementOrderEntity enforcement) {
        return bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(enforcement.getId()).stream()
            .findFirst()
            .map(EnforcementDocumentPersistenceService::displayName)
            .orElse(null);
    }

    private static String displayName(BailiffAllocationEntity allocation) {
        return StringUtils.isNotBlank(allocation.getBailiffName())
            ? allocation.getBailiffName() : allocation.getBailiffEmail();
    }

    /**
     * The warrant and writ journeys ask which language the claimant used; the restitution journeys do not yet.
     */
    private static String language(EnforcementOrder enforcementOrder, SelectEnforcementType enforcementType) {
        if (enforcementOrder == null || enforcementType == null) {
            return null;
        }
        Optional<LanguageUsed> languageUsed = switch (enforcementType) {
            case WARRANT -> Optional.ofNullable(enforcementOrder.getWarrantDetails())
                .map(WarrantDetails::getLanguageUsed);
            case WRIT -> Optional.ofNullable(enforcementOrder.getWritDetails()).map(WritDetails::getLanguageUsed);
            case WARRANT_OF_RESTITUTION, WRIT_OF_RESTITUTION -> Optional.empty();
        };
        return languageUsed.map(LanguageUsed::getLabel).orElse(null);
    }

    /**
     * Whether the enforcement already has the one document of this kind it can have. A notice of eviction is
     * generated again for a rescheduled eviction, so it is never already generated.
     */
    private static boolean alreadyGenerated(EnforcementOrderEntity enforcement, EnforcementOutput output) {
        return !output.isRepeatable()
            && enforcement.getDocuments().stream()
            .anyMatch(document -> document.getType() == output.getDocumentType());
    }

    private String caseName(ClaimEntity claim) {
        List<Party> claimants = parties(claim, PartyRole.CLAIMANT);
        List<Party> defendants = parties(claim, PartyRole.DEFENDANT);
        return caseNameFormatter.formatCaseName(claimants, defendants);
    }

    private List<Party> parties(ClaimEntity claim, PartyRole role) {
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .map(ClaimPartyEntity::getParty)
            .filter(party -> !party.isRemoved())
            .map(party -> modelMapper.map(party, Party.class))
            .toList();
    }
}
