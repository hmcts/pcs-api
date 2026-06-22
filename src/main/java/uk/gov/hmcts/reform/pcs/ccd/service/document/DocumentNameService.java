package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;

import java.util.UUID;

@Service
public class DocumentNameService {

    public String appendGenAppPostfix(String originalFilename,
                                      GenAppEntity genAppEntity,
                                      ClaimEntity mainClaim,
                                      UUID applicantPartyId) {

        if (originalFilename == null) {
            return null;
        }

        String baseName = FilenameUtils.getBaseName(originalFilename);
        String extension = FilenameUtils.getExtension(originalFilename);

        // Example label: General Application (GA2) - Defendant 1.pdf
        String partyLabel = getPartyLabel(mainClaim, applicantPartyId);
        String filename = "%s GA%d".formatted(baseName, genAppEntity.getRank());
        if (partyLabel != null) {
            filename += " - " + partyLabel;
        }

        if (!extension.isBlank()) {
            filename += "." + extension;
        }

        return filename;
    }

    public String appendPartyPostfix(String originalFilename,
                                     ClaimEntity mainClaim,
                                     UUID applicantPartyId) {

        if (originalFilename == null) {
            return null;
        }

        String baseName = FilenameUtils.getBaseName(originalFilename);
        String extension = FilenameUtils.getExtension(originalFilename);

        String partyLabel = getPartyLabel(mainClaim, applicantPartyId);
        String filename = (partyLabel != null) ? "%s - %s".formatted(baseName, partyLabel) : baseName;

        if (!extension.isBlank()) {
            filename += "." + extension;
        }

        return filename;
    }

    private static String getPartyLabel(ClaimEntity mainClaim, UUID partyId) {
        ClaimPartyEntity applicantClaimParty = getClaimParty(mainClaim, partyId);

        if (applicantClaimParty.getRole() == PartyRole.CLAIMANT) {
            return "Claimant %d".formatted(applicantClaimParty.getRank());
        } else if (applicantClaimParty.getRole() == PartyRole.DEFENDANT) {
            return "Defendant %d".formatted(applicantClaimParty.getRank());
        } else {
            return null;
        }
    }

    private static ClaimPartyEntity getClaimParty(ClaimEntity claim, UUID partyId) {
        return claim.getClaimParties().stream()
            .filter(claimPartyEntity -> partyId.equals(claimPartyEntity.getParty().getId()))
            .findFirst()
            .orElseThrow(() -> new PartyNotFoundException("Party not found"));
    }

}
