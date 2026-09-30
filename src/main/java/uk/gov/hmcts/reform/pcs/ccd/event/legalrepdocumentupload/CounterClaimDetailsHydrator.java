package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class CounterClaimDetailsHydrator {

    private static final DateTimeFormatter CC_LABEL_DATE_FORMAT =
        DateTimeFormatter.ofPattern("EEEE d MMMM uuuu", Locale.UK);

    public void hydrate(
        PcsCaseEntity pcsCaseEntity,
        LegalRepDocumentUploadDetails details,
        String currentUserOrganisationId
    ) {
        List<CounterClaimEntity> counterClaims = pcsCaseEntity.getCounterClaims();
        if (counterClaims == null || counterClaims.isEmpty()) {
            details.setShowCounterclaimPage(VerticalYesNo.NO);
            return;
        }

        details.setShowCounterclaimPage(VerticalYesNo.YES);

        StringBuilder linksHtml = new StringBuilder("<div class=\"govuk-inset-text\">%n".formatted());
        List<DynamicStringListElement> ccRadioItems = new ArrayList<>();

        for (int i = 0; i < counterClaims.size(); i++) {
            CounterClaimEntity cc = counterClaims.get(i);
            int ccIndex = i + 1;
            String defName = getPartyDisplayName(cc.getParty(), ccIndex);
            String escapedDefName = HtmlUtils.htmlEscape(defName);

            String fileName = String.format("Counterclaim CC%d - %s.pdf", ccIndex, escapedDefName);
            String docUrl = findCounterclaimDocumentUrl(pcsCaseEntity, cc);

            linksHtml.append(String.format(
                "  <p class=\"govuk-body\"><a href=\"%s\" target=\"_blank\" rel=\"noopener noreferrer\">%s (Open in a new tab)</a></p>%n",
                docUrl, fileName
            ));

            String formattedDate = cc.getClaimSubmittedDate() != null
                ? " on " + cc.getClaimSubmittedDate().format(CC_LABEL_DATE_FORMAT)
                : "";

            boolean isCurrentUsersCounterclaim = cc.getParty() != null
                && currentUserOrganisationId != null
                && currentUserOrganisationId.equals(cc.getParty().getOrganisationId());

            String radioLabel = isCurrentUsersCounterclaim
                ? String.format("Yes, the documents I'm uploading relate to the counterclaim I made%s", formattedDate)
                : String.format("Yes, the documents I'm uploading relate to the counterclaim made by %s%s",
                                escapedDefName, formattedDate);

            ccRadioItems.add(
                DynamicStringListElement.builder()
                    .code(cc.getId() != null ? cc.getId().toString() : "CC_" + ccIndex)
                    .label(radioLabel)
                    .build()
            );
        }

        ccRadioItems.add(
            DynamicStringListElement.builder()
                .code("MAIN_CLAIM")
                .label("No, the documents I'm uploading do not relate to a counterclaim")
                .build()
        );

        linksHtml.append("</div>");
        details.setCounterclaimDocumentLinks(linksHtml.toString());
        details.setValidCounterclaims(
            DynamicStringList.builder()
                .listItems(ccRadioItems)
                .build()
        );
    }

    private String getPartyDisplayName(PartyEntity party, int fallbackIndex) {
        if (party != null) {
            if (isNotBlank(party.getOrgName())) {
                return party.getOrgName().trim();
            }
            String fullName = Stream.of(party.getFirstName(), party.getLastName())
                .filter(StringUtils::isNotBlank)
                .collect(joining(" "));
            if (isNotBlank(fullName)) {
                return fullName;
            }
        }
        return "Defendant " + fallbackIndex;
    }

    private String findCounterclaimDocumentUrl(PcsCaseEntity pcsCaseEntity, CounterClaimEntity cc) {
        if (pcsCaseEntity.getDocuments() == null || cc.getId() == null) {
            return "#";
        }

        List<DocumentEntity> ccDocs = pcsCaseEntity.getDocuments().stream()
            .filter(doc -> doc.getCounterClaim() != null && cc.getId().equals(doc.getCounterClaim().getId()))
            .toList();

        if (ccDocs.isEmpty()) {
            return "#";
        }

        Optional<DocumentEntity> primaryCcDoc = ccDocs.stream()
            .filter(doc -> doc.getType() == DocumentType.COUNTERCLAIM)
            .findFirst();

        DocumentEntity targetDoc = primaryCcDoc.orElseGet(ccDocs::getFirst);

        String rawUrl = targetDoc.getBinaryUrl() != null ? targetDoc.getBinaryUrl() : targetDoc.getUrl();
        return formatDocumentUrl(rawUrl);
    }

    private String formatDocumentUrl(String url) {
        if (url != null && url.contains("/documents/")) {
            return url.substring(url.indexOf("/documents/"));
        }
        return url;
    }
}
