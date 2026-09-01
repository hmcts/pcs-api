package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseFlagService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.isPopulated;

/**
 * Selector-time skip rules for bulk-print packs. When {@link FeatureFlag#RELEASE_1_DOT_3} is on, packs that
 * need translation, a follow-on gen app, or an issued counterclaim HWF reference are held back silently —
 * no {@code PACK_SENT} or {@code PACK_FAILED} row is written. Flag off keeps current send behaviour.
 */
@Service
@Slf4j
public class PackSkipRules {

    private static final String TRANSLATION_REQUIRED = "translation required";

    private final FeatureToggleService featureToggleService;
    private final CaseFlagService caseFlagService;

    public PackSkipRules(FeatureToggleService featureToggleService, CaseFlagService caseFlagService) {
        this.featureToggleService = featureToggleService;
        this.caseFlagService = caseFlagService;
    }

    public boolean shouldSkipClaimPack(PcsCaseEntity pcsCase, ClaimEntity claim) {
        if (!release13Enabled()) {
            return false;
        }
        return skipIfPresent(pcsCase, "claim", claimPackSkipReason(pcsCase, claim));
    }

    public boolean shouldSkipDefencePack(PcsCaseEntity pcsCase) {
        if (!release13Enabled()) {
            return false;
        }
        return skipIfPresent(pcsCase, "defence", defencePackSkipReason(pcsCase));
    }

    public boolean shouldSkipGenAppPack(PcsCaseEntity pcsCase, GenAppEntity genApp) {
        if (!release13Enabled()) {
            return false;
        }
        return skipIfPresent(pcsCase, "gen-app", genAppPackSkipReason(pcsCase, genApp));
    }

    private Optional<String> claimPackSkipReason(PcsCaseEntity pcsCase, ClaimEntity claim) {
        boolean translation = requiresTranslation(claim.getLanguageUsed()) || hasActiveTranslationFlag(pcsCase);
        boolean genAppExpected = claim.getGenAppExpected() == VerticalYesNo.YES;
        if (translation && genAppExpected) {
            return Optional.of(TRANSLATION_REQUIRED + " and gen app expected");
        }
        if (translation) {
            return Optional.of(TRANSLATION_REQUIRED);
        }
        if (genAppExpected) {
            return Optional.of("gen app expected");
        }
        return Optional.empty();
    }

    private Optional<String> defencePackSkipReason(PcsCaseEntity pcsCase) {
        boolean translation = hasActiveTranslationFlag(pcsCase)
            || hasSubmittedDefenceNeedingTranslation(pcsCase)
            || hasIssuedCounterClaimNeedingTranslation(pcsCase);
        boolean hwf = hasIssuedCounterClaimWithHwf(pcsCase);
        if (translation && hwf) {
            return Optional.of(TRANSLATION_REQUIRED + " and issued counterclaim has help with fees");
        }
        if (translation) {
            return Optional.of(TRANSLATION_REQUIRED);
        }
        if (hwf) {
            return Optional.of("issued counterclaim has help with fees");
        }
        return Optional.empty();
    }

    private Optional<String> genAppPackSkipReason(PcsCaseEntity pcsCase, GenAppEntity genApp) {
        if (requiresTranslation(genApp.getLanguageUsed()) || hasActiveTranslationFlag(pcsCase)) {
            return Optional.of(TRANSLATION_REQUIRED);
        }
        return Optional.empty();
    }

    private boolean skipIfPresent(PcsCaseEntity pcsCase, String packType, Optional<String> reason) {
        if (reason.isEmpty()) {
            return false;
        }
        log.debug("Skipping {} pack for case {} - {}", packType, pcsCase.getId(), reason.get());
        return true;
    }

    private boolean release13Enabled() {
        return featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3);
    }

    private boolean hasSubmittedDefenceNeedingTranslation(PcsCaseEntity pcsCase) {
        return streamOf(pcsCase.getDefendantResponses())
            .filter(response -> response.getStatus() == DefendantResponseStatus.SUBMITTED)
            .anyMatch(response -> requiresTranslation(response.getLanguageUsed()));
    }

    private boolean hasIssuedCounterClaimNeedingTranslation(PcsCaseEntity pcsCase) {
        return issuedCounterClaims(pcsCase)
            .anyMatch(counterClaim -> requiresTranslation(counterClaim.getLanguageUsed()));
    }

    private boolean hasIssuedCounterClaimWithHwf(PcsCaseEntity pcsCase) {
        return hwfBlockingCounterClaims(pcsCase)
            .anyMatch(counterClaim -> isPopulated(counterClaim.getHwfReferenceNumber()));
    }

    private Stream<CounterClaimEntity> issuedCounterClaims(PcsCaseEntity pcsCase) {
        return streamOf(pcsCase.getCounterClaims())
            .filter(counterClaim -> counterClaim.getStatus() == CounterClaimState.COUNTER_CLAIM_ISSUED);
    }

    private Stream<CounterClaimEntity> hwfBlockingCounterClaims(PcsCaseEntity pcsCase) {
        return streamOf(pcsCase.getCounterClaims())
            .filter(counterClaim -> counterClaim.getStatus() == CounterClaimState.COUNTER_CLAIM_ISSUED
                || counterClaim.getStatus() == CounterClaimState.PENDING_REVIEW);
    }

    private boolean hasActiveTranslationFlag(PcsCaseEntity pcsCase) {
        if (hasActiveWelshCommunicationsFlag(pcsCase.getCaseFlags())) {
            return true;
        }
        return partiesOnCase(pcsCase)
            .map(PartyEntity::getDefendantFlags)
            .anyMatch(this::hasActiveWelshCommunicationsFlag);
    }

    private Stream<PartyEntity> partiesOnCase(PcsCaseEntity pcsCase) {
        Stream<PartyEntity> caseParties = streamOf(pcsCase.getParties());
        Stream<PartyEntity> claimParties = streamOf(pcsCase.getClaims())
            .map(ClaimEntity::getClaimParties)
            .flatMap(PackSkipRules::streamOf)
            .map(ClaimPartyEntity::getParty);
        return Stream.concat(caseParties, claimParties).filter(Objects::nonNull).distinct();
    }

    private boolean hasActiveWelshCommunicationsFlag(List<? extends BaseCaseFlag> flags) {
        return streamOf(flags).anyMatch(caseFlagService::isWelshCommunicationsPreference);
    }

    private static boolean requiresTranslation(LanguageUsed languageUsed) {
        return languageUsed == LanguageUsed.WELSH || languageUsed == LanguageUsed.ENGLISH_AND_WELSH;
    }

    private static <T> Stream<T> streamOf(Collection<T> values) {
        return values == null ? Stream.empty() : values.stream();
    }
}
