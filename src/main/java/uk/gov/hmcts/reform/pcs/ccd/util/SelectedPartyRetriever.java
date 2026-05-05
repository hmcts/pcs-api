package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@AllArgsConstructor
public class SelectedPartyRetriever {
    private final ClientContextRetriever clientContextRetriever;

    public Optional<UUID> getSelectedPartyId(PCSCase caseData) {
        return caseData.getAllDefendants().size() == 1
            ? Optional.of(UUID.fromString(caseData.getAllDefendants().getFirst().getId())) : getRequiredPartyId();
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
}
