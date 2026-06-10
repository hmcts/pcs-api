package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@AllArgsConstructor
public class SelectedPartyRetriever {
    private final ClientContextRetriever clientContextRetriever;
    private final LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;
    private final PcsCaseService pcsCaseService;

    public Optional<UUID> getSelectedPartyId(long caseReference, String organisationId) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        List<PartyEntity> partyEntities = legalRepForDefendantAccessValidator.validateAndGetDefendants(
            caseEntity,
            organisationId
        );
        return partyEntities.size() == 1
            ? Optional.of(UUID.fromString(partyEntities.getFirst().getId().toString())) : getRequiredPartyId();
    }

    public Optional<UUID> getSelectedPartyId(PCSCase caseData) {
        return caseData.getAllDefendants().size() == 1
            ? Optional.of(UUID.fromString(caseData.getAllDefendants().getFirst().getId())) : getRequiredPartyId();
    }

    public Optional<UUID> getCurrentRepresentedPartyId(PCSCase caseData) {
        return caseData.getAllDefendants().size() == 1
            ? Optional.of(UUID.fromString(caseData.getAllDefendants().getFirst().getId())) :
            extractCurrentRepresentedPartyId(caseData.getCurrentRepresentedPartyId());
    }

    private Optional<UUID> getRequiredPartyId() {
        ClientContext clientContext = clientContextRetriever.getClientContext();
        if (clientContext == null) {
            return Optional.empty();
        }
        String selectedPartyId = clientContext.getSelectedPartyId();
        if (isBlank(selectedPartyId)) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(selectedPartyId));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid selected responding party id for respond to claim", ex);
        }
    }

    private Optional<UUID> extractCurrentRepresentedPartyId(String currentRepresentedPartyId) {
        return currentRepresentedPartyId != null ? Optional.of(UUID.fromString(currentRepresentedPartyId)) :
            Optional.empty();
    }
}
