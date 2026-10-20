package uk.gov.hmcts.reform.pcs.ccd.view;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.bulkprint.PackType;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.BulkPrintQueueEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseFlagService;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.BulkPrintQueueService;
import uk.gov.hmcts.reform.pcs.ccd.service.claim.ClaimVisibilityService;
import uk.gov.hmcts.reform.pcs.ccd.service.claim.VisibilityRestrictionReason;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class CaseStatusView {

    private final BulkPrintQueueService bulkPrintQueueService;
    private final CaseFlagService caseFlagService;
    private final PebbleEngine pebbleEngine;
    private final ClaimVisibilityService claimVisibilityService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {

        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();

        if (mainClaim == null) {
            return;
        }

        boolean genAppExpected = mainClaim.getGenAppExpected() == VerticalYesNo.YES;
        boolean inClaimGenAppWaitingPeriod = genAppExpected
            && LocalDateTime.now().isBefore(mainClaim.getClaimSubmittedDate().plusHours(24));
        List<BulkPrintQueueEntity> queuedBuildPrintItems = bulkPrintQueueService.getQueuedItemsForCase(pcsCaseEntity);

        boolean claimPackSent = queuedBuildPrintItems.stream()
            .filter(bulkPrintQueueEntity -> bulkPrintQueueEntity.getPackType() == PackType.CLAIM)
            .anyMatch(bulkPrintQueueEntity -> bulkPrintQueueEntity.getSentAt() != null);

        // TODO: Put in shared location?
        boolean translationRequired =
            requiresTranslation(mainClaim.getLanguageUsed()) || hasActiveTranslationFlag(pcsCaseEntity);

        boolean translationCompleted = !mainClaim.getTranslatedDocuments().isEmpty();

        List<VisibilityRestrictionReason> visibilityRestrictions
            = claimVisibilityService.getVisibilityRestrictions(pcsCaseEntity);

        ClaimStatus claimStatus = ClaimStatus.builder()
            .genAppExpected(genAppExpected)
            .inClaimGenAppWaitingPeriod(inClaimGenAppWaitingPeriod)
            .translationRequired(translationRequired)
            .translationCompleted(translationCompleted)
            .packHeld(!visibilityRestrictions.isEmpty())
            .packSent(claimPackSent)
            .build();

        Map<String, Object> context = Map.of(
            "claimStatus", claimStatus
        );

        pcsCase.setCaseStatusMarkdown(renderTemplate("case-status", context));

    }

    @Getter
    @Builder
    private static class ClaimStatus {
        private final boolean genAppExpected;
        private final boolean inClaimGenAppWaitingPeriod;
        private final boolean translationRequired;
        private final boolean translationCompleted;
        private final boolean packHeld;
        private final boolean packSent;
    }

    private String renderTemplate(String templateName, Map<String, Object> context) {
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate(templateName);
        Writer writer = new StringWriter();

        try {
            compiledTemplate.evaluate(writer, context);
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }

        return writer.toString();
    }

    private static boolean requiresTranslation(LanguageUsed languageUsed) {
        return languageUsed == LanguageUsed.WELSH || languageUsed == LanguageUsed.ENGLISH_AND_WELSH;
    }

    private boolean hasActiveTranslationFlag(PcsCaseEntity pcsCaseEntity) {
        if (hasActiveWelshCommunicationsFlag(pcsCaseEntity.getCaseFlags())) {
            return true;
        }
        return pcsCaseEntity.getParties().stream()
            .map(PartyEntity::getDefendantFlags)
            .anyMatch(this::hasActiveWelshCommunicationsFlag);
    }

    private boolean hasActiveWelshCommunicationsFlag(List<? extends BaseCaseFlag> flags) {
        return streamOf(flags).anyMatch(caseFlagService::isWelshCommunicationsPreference);
    }

    private static <T> Stream<T> streamOf(Collection<T> values) {
        return values == null ? Stream.empty() : values.stream();
    }

}
