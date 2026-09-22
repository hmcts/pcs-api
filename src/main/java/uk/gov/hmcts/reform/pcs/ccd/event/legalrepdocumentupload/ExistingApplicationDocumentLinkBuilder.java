package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExistingApplicationDocumentLinkBuilder {

    private final PartyService partyService;

    public String build(PcsCaseEntity pcsCaseEntity, List<GenAppEntity> genApps) {
        List<String> links = genApps.stream()
            .map(genApp -> buildLink(pcsCaseEntity, genApp))
            .filter(link -> !link.isBlank())
            .toList();

        if (links.isEmpty()) {
            return "";
        }

        String listItems = links.stream()
            .map(link -> "<li class=\"govuk-!-margin-bottom-1\">" + link + "</li>")
            .collect(Collectors.joining());

        return """
            <div class="govuk-inset-text">
                <ul class="govuk-list">
                    %s
                </ul>
            </div>
            """.formatted(listItems);
    }

    public String applicationLabel(PcsCaseEntity pcsCaseEntity, GenAppEntity genApp) {
        return "General app (%s)%s".formatted(genAppReference(genApp), partyLabelText(pcsCaseEntity, genApp));
    }

    private String buildLink(PcsCaseEntity pcsCaseEntity, GenAppEntity genApp) {
        String documentLink = documentLink(genApp.getSubmissionDocument());
        if (documentLink == null) {
            return "";
        }

        String linkText = "%s (opens in new tab)".formatted(applicationLabel(pcsCaseEntity, genApp));

        return """
            <a href="%s" target="_blank" rel="noopener noreferrer" class="govuk-link">%s</a>
            """.formatted(
                HtmlUtils.htmlEscape(documentLink),
                HtmlUtils.htmlEscape(linkText)
            );
    }

    private String documentLink(DocumentEntity documentEntity) {
        if (documentEntity == null) {
            return null;
        }
        if (documentEntity.getDocumentId() != null) {
            return "/documents/%s/binary".formatted(documentEntity.getDocumentId());
        }
        return documentEntity.getBinaryUrl() == null ? documentEntity.getUrl() : documentEntity.getBinaryUrl();
    }

    private String genAppReference(GenAppEntity genApp) {
        return genApp.getRank() == null ? "GA" : "GA%d".formatted(genApp.getRank());
    }

    private String partyLabelText(PcsCaseEntity pcsCaseEntity, GenAppEntity genApp) {
        if (genApp.getParty() == null || genApp.getParty().getId() == null) {
            return "";
        }

        String partyLabel = partyService.getPartyLabel(pcsCaseEntity.getMainClaim(), genApp.getParty().getId());
        return partyLabel == null ? "" : " - " + partyLabel;
    }
}
