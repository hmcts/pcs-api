package uk.gov.hmcts.reform.pcs.ccd.service.workallocation;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskDescriptionService {

    private final PartyService partyService;
    private final PebbleEngine pebbleEngine;

    public String createGenAppAdditionalDocumentsDescription(long caseReference,
                                                             ClaimEntity mainClaim,
                                                             PartyEntity partyEntity,
                                                             GenAppEntity genAppEntity,
                                                             List<DocumentEntity> documentEntities) {


        String partyLabel = partyService.getPartyLabel(mainClaim, partyEntity.getId());

        List<String> filenames = documentEntities.stream()
            .map(DocumentEntity::getFileName)
            .toList();

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "partyLabel", partyLabel,
            "genAppRank", genAppEntity.getRank(),
            "filenames", filenames
        );

        String templateName = "gen-app-review-additional-docs";
        return renderTemplate(templateName, context);
    }

    public String createReviewCaseFlagDescription(long caseReference, List<ListValue<FlagDetail>> flagDetails) {
        List<String> flags = flagDetails.stream()
            .map(ListValue::getValue)
            .map(FlagDetail::getName)
            .toList();

        Map<String, Object> context = Map.of(
            "caseReference", caseReference,
            "flags", flags
        );

        String templateName = "review-case-flag";
        return renderTemplate(templateName, context);
    }

    private String renderTemplate(String templateName, Map<String, Object> context) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate("workallocation/" + templateName);
        Writer writer = new StringWriter();

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }

}
