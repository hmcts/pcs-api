package uk.gov.hmcts.reform.pcs.ccd.service.riskassessment;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.RiskProfileEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.riskassessment.RiskAssessmentPayload;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

@Service
@AllArgsConstructor
public class RiskAssessmentPersistenceService {

    static final String APPLICATION_TYPE = "Warrant of Possession";

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final DocumentImportService documentImportService;
    private final ClaimActivityLogService claimActivityLogService;
    private final PartyService partyService;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;

    /**
     * Builds the data needed to render the risk assessment, or nothing when the order isn't a warrant, has no risk
     * profile, or its risk assessment has already been generated (so a retried task doesn't attach a second copy).
     */
    @Transactional(readOnly = true)
    public Optional<RiskAssessmentRenderContext> buildRenderContext(UUID enforcementOrderId) {
        EnforcementOrderEntity enforcementOrder = loadEnforcementOrder(enforcementOrderId);
        if (enforcementOrder.getWarrantDetails() == null
            || enforcementOrder.getRiskProfileDetails() == null
            || hasRiskAssessmentDocument(enforcementOrder)) {
            return Optional.empty();
        }
        return Optional.of(new RiskAssessmentRenderContext(toPayload(enforcementOrder), enforcementOrder.getRank()));
    }

    @Transactional
    public void attachDocument(UUID enforcementOrderId, String documentUrl) {
        EnforcementOrderEntity enforcementOrder = loadEnforcementOrder(enforcementOrderId);
        PcsCaseEntity pcsCase = enforcementOrder.getPcsCase();

        DocumentEntity document = documentImportService.addDocumentToCase(
            pcsCase, documentUrl, CaseFileCategory.ENFORCEMENT);
        document.setType(DocumentType.RISK_ASSESSMENT);
        enforcementOrder.addDocuments(List.of(document));

        claimActivityLogService.logGenerationSuccess(pcsCase, partyService.getPrimaryClaimantPartyEntity(pcsCase));
    }

    private RiskAssessmentPayload toPayload(EnforcementOrderEntity enforcementOrder) {
        PcsCaseEntity pcsCase = enforcementOrder.getPcsCase();
        WarrantEntity warrant = enforcementOrder.getWarrantDetails();
        RiskProfileEntity risk = enforcementOrder.getRiskProfileDetails();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);

        return RiskAssessmentPayload.builder()
            // Title block
            .referenceNumber(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()))
            .caseName(caseNameFormatter.formatCaseName(pcsCase))
            // Application and property details
            .applicationType(APPLICATION_TYPE)
            .claimantName(partyService.getPartyName(claimant))
            // TODO: confirm the source of the claimant / applicant reference
            .claimantReference(null)
            .defendantNames(defendantsToEvict(enforcementOrder).stream()
                                .map(partyService::getPartyName)
                                .collect(Collectors.joining(", ")))
            .evictionAddress(formatAddress(pcsCase.getPropertyAddress()))
            // Defendants and occupants
            .defendantDatesOfBirthKnown(warrant.getDefendantsDOBKnown() == null
                ? null : warrant.getDefendantsDOBKnown().getLabel())
            .defendantDatesOfBirth(warrant.getDefendantsDOBDetails())
            .propertyRiskResponse(risk.getAnyRiskToBailiff() == null
                ? null : risk.getAnyRiskToBailiff().getLabel())
            .selectedPropertyRisks(selectedRisks(risk))
            // Risk details
            .violentAggressiveDetails(risk.getViolentDetails())
            .firearmHistoryDetails(risk.getFirearmsDetails())
            .criminalAntisocialDetails(risk.getCriminalDetails())
            .threatDetails(risk.getVerbalThreatsDetails())
            .evictionProtestGroupDetails(risk.getProtestGroupDetails())
            .policeVisitDetails(risk.getPoliceSocialServicesDetails())
            .animalDetails(risk.getAnimalsDetails())
            // Vulnerable adults and children
            .vulnerabilityResponse(risk.getVulnerablePeoplePresent() == null
                ? null : risk.getVulnerablePeoplePresent().getLabel())
            .vulnerablePeopleType(risk.getVulnerableCategory() == null
                ? null : risk.getVulnerableCategory().getLabel())
            .vulnerabilityDetails(risk.getVulnerableReasonText())
            // Access and other information
            .accessDifficultyResponse(warrant.getIsDifficultToAccessProperty() == null
                ? null : warrant.getIsDifficultToAccessProperty().getLabel())
            .accessDifficultyDetails(warrant.getClarificationOnAccessDifficultyText())
            .otherEvictionInformationResponse(warrant.getAdditionalInformationSelect() == null
                ? null : warrant.getAdditionalInformationSelect().getLabel())
            .otherEvictionInformation(warrant.getAdditionalInformationDetails())
            .build();
    }

    /**
     * Every defendant on the claim when the claimant chose to evict everyone, otherwise the defendants they selected.
     */
    private List<PartyEntity> defendantsToEvict(EnforcementOrderEntity enforcementOrder) {
        if (enforcementOrder.getWarrantDetails().getEvictEveryone() == VerticalYesNo.YES) {
            return partiesByRole(enforcementOrder.getPcsCase().getMainClaim(), PartyRole.DEFENDANT);
        }
        return enforcementOrder.getSelectedDefendants().stream()
            .map(SelectedDefendantEntity::getParty)
            .toList();
    }

    /**
     * The risk categories the claimant ticked. The journey only asks for details of the categories selected, so a
     * category counts as selected when its details were given.
     */
    private static String selectedRisks(RiskProfileEntity risk) {
        Map<RiskCategory, String> detailsByCategory = new LinkedHashMap<>();
        detailsByCategory.put(RiskCategory.VIOLENT_OR_AGGRESSIVE, risk.getViolentDetails());
        detailsByCategory.put(RiskCategory.FIREARMS_POSSESSION, risk.getFirearmsDetails());
        detailsByCategory.put(RiskCategory.CRIMINAL_OR_ANTISOCIAL, risk.getCriminalDetails());
        detailsByCategory.put(RiskCategory.VERBAL_OR_WRITTEN_THREATS, risk.getVerbalThreatsDetails());
        detailsByCategory.put(RiskCategory.PROTEST_GROUP_MEMBER, risk.getProtestGroupDetails());
        detailsByCategory.put(RiskCategory.AGENCY_VISITS, risk.getPoliceSocialServicesDetails());
        detailsByCategory.put(RiskCategory.AGGRESSIVE_ANIMALS, risk.getAnimalsDetails());

        String selected = detailsByCategory.entrySet().stream()
            .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
            .map(entry -> entry.getKey().getLabel())
            .collect(Collectors.joining(AddressFormatter.NEWLINE_DELIMITER));
        return selected.isEmpty() ? null : selected;
    }

    private String formatAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }
        return addressFormatter.formatFullAddress(addressMapper.toAddressUK(address),
                                                  AddressFormatter.NEWLINE_DELIMITER);
    }

    private boolean hasRiskAssessmentDocument(EnforcementOrderEntity enforcementOrder) {
        return enforcementOrder.getDocuments().stream()
            .anyMatch(document -> document.getType() == DocumentType.RISK_ASSESSMENT);
    }

    private EnforcementOrderEntity loadEnforcementOrder(UUID enforcementOrderId) {
        return enforcementOrderRepository.findById(enforcementOrderId)
            .orElseThrow(() -> new EnforcementApplicationNotFoundException(
                "Unable to find Enforcement app with ID " + enforcementOrderId));
    }
}
