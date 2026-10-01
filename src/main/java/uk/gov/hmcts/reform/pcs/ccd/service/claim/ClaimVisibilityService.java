package uk.gov.hmcts.reform.pcs.ccd.service.claim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseFlagService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ClaimVisibilityService {

    private final CaseFlagService caseFlagService;

    public List<VisibilityRestrictionReason> getVisibilityRestrictions(PcsCaseEntity pcsCaseEntity) {
        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();

        List<VisibilityRestrictionReason> visibilityRestrictionReasons = new ArrayList<>();

        boolean genAppExpected = mainClaim.getGenAppExpected() == VerticalYesNo.YES;
        boolean inClaimGenAppWaitingPeriod = genAppExpected
            && LocalDateTime.now().isBefore(mainClaim.getClaimSubmittedDate().plusHours(24));

        if (inClaimGenAppWaitingPeriod) {
            visibilityRestrictionReasons.add(VisibilityRestrictionReason.IN_GEN_APP_24H_WINDOW);
        }

        DocumentEntity claimForm = mainClaim.getClaimFormDocument();
        if (claimForm == null) {
            visibilityRestrictionReasons.add(VisibilityRestrictionReason.CLAIM_FORM_NOT_RENDERED);
        }

        boolean translationRequired =
            requiresTranslation(mainClaim.getLanguageUsed()) || hasActiveTranslationFlag(pcsCaseEntity);

        boolean translationCompleted = !mainClaim.getTranslatedDocuments().isEmpty();

        if (translationRequired && !translationCompleted) {
            visibilityRestrictionReasons.add(VisibilityRestrictionReason.CLAIM_FORM_NOT_TRANSLATED);
        }

        return visibilityRestrictionReasons;
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
