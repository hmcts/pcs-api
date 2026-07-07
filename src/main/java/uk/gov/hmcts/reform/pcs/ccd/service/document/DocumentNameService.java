package uk.gov.hmcts.reform.pcs.ccd.service.document;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentNameService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final PartyService partyService;

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
        String partyLabel = partyService.getPartyLabel(mainClaim, applicantPartyId);
        String filename = "%s GA%d".formatted(baseName, genAppEntity.getRank());

        return buildFilename(filename, extension, partyLabel);
    }

    public String appendDefendantPostfix(String originalFilename,
                                         ClaimEntity mainClaim,
                                         UUID defendantPartyId) {

        if (originalFilename == null) {
            return null;
        }

        String baseName = FilenameUtils.getBaseName(originalFilename);
        String extension = FilenameUtils.getExtension(originalFilename);

        String partyLabel = partyService.getPartyLabel(mainClaim, defendantPartyId);

        return buildFilename(baseName, extension, partyLabel);
    }

    private static String buildFilename(String filename, String extension, String partyLabel) {
        if (partyLabel != null) {
            filename += " - " + partyLabel;
        }
        if (!extension.isBlank()) {
            filename += "." + extension;
        }
        return filename;
    }

    public String appendCounterClaimPostfix(String originalFilename, ClaimEntity claim, UUID partyId) {
        if (originalFilename == null) {
            return null;
        }

        String baseName = FilenameUtils.getBaseName(originalFilename);
        String extension = FilenameUtils.getExtension(originalFilename);
        String partyLabel = partyService.getPartyLabel(claim, partyId);
        String filename = partyLabel != null ? baseName + " - " + partyLabel : baseName;

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

        String partyLabel = partyService.getPartyLabel(mainClaim, applicantPartyId);
        String filename = (partyLabel != null) ? "%s - %s".formatted(baseName, partyLabel) : baseName;

        if (!extension.isBlank()) {
            filename += "." + extension;
        }

        return filename;
    }

    public String appendDate(String originalFilename, LocalDate localDate) {

        if (originalFilename == null) {
            return null;
        }

        if (localDate == null) {
            return originalFilename;
        }

        String formattedDate = DATE_FORMAT.format(localDate);
        String baseName = FilenameUtils.getBaseName(originalFilename);
        String extension = FilenameUtils.getExtension(originalFilename);

        String filename = "%s %s".formatted(baseName, formattedDate);

        if (!extension.isBlank()) {
            filename += "." + extension;
        }

        return filename;
    }

}
