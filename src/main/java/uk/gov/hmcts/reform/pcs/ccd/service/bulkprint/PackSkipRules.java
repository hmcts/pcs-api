package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseFlagService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Selector-time skip rules for bulk-print packs. When {@link FeatureFlag#RELEASE_1_DOT_3} is on, the claim pack is
 * held back silently when it needs translation or has a follow-on gen app (until its hold period after claim issue
 * ends), and the defence pack is held back only when it needs translation (HDPI-9007: for RTAC Pro an HWF reference
 * does not hold the defence pack) — no {@code PACK_SENT} or {@code PACK_FAILED} row is written. Flag off keeps current
 * send behaviour. When {@link FeatureFlag#RELEASE_1_DOT_4} is on, claimants are excluded from every pack type.
 */
@Service
@Slf4j
public class PackSkipRules {

    private static final String TRANSLATION_REQUIRED = "translation required";

    private final FeatureToggleService featureToggleService;
    private final CaseFlagService caseFlagService;
    private final Clock utcClock;
    private final Duration genAppExpectedHold;

    public PackSkipRules(FeatureToggleService featureToggleService,
                         CaseFlagService caseFlagService,
                         @Qualifier("utcClock") Clock utcClock,
                         @Value("${bulk-print.claim-pack-gen-app-expected-hold}") Duration genAppExpectedHold) {
        this.featureToggleService = featureToggleService;
        this.caseFlagService = caseFlagService;
        this.utcClock = utcClock;
        this.genAppExpectedHold = genAppExpectedHold;
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

    public boolean shouldExcludeClaimant() {
        return featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4);
    }

    private Optional<String> claimPackSkipReason(PcsCaseEntity pcsCase, ClaimEntity claim) {
        boolean translation = requiresTranslation(claim.getLanguageUsed()) || hasActiveTranslationFlag(pcsCase);
        boolean holdForGenApp = claim.getGenAppExpected() == VerticalYesNo.YES && genAppHoldActive(pcsCase, claim);
        if (translation && holdForGenApp) {
            return Optional.of(TRANSLATION_REQUIRED + " and gen app expected");
        }
        if (translation) {
            return Optional.of(TRANSLATION_REQUIRED);
        }
        if (holdForGenApp) {
            return Optional.of("gen app expected");
        }
        return Optional.empty();
    }

    private boolean genAppHoldActive(PcsCaseEntity pcsCase, ClaimEntity claim) {
        if (!release14Enabled()) {
            return true;
        }
        // If the claimant has not issued a gen app within the 24-hour hold, the claim pack is released
        return LocalDateTime.now(utcClock).isBefore(claim.getClaimIssuedDate().plus(genAppExpectedHold))
            || hasIssuedClaimantGenApp(pcsCase, claim);
    }

    private boolean hasIssuedClaimantGenApp(PcsCaseEntity pcsCase, ClaimEntity claim) {
        List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        if (claimants.isEmpty()) {
            throw new IllegalStateException("No claimants found for claim " + claim.getId());
        }
        UUID claimantId = claimants.getFirst().getId();
        LocalDateTime holdDeadline = claim.getClaimIssuedDate().plus(genAppExpectedHold);
        return pcsCase.getGenApps().stream()
            .filter(genApp -> genApp.getState() == GenAppState.GEN_APP_ISSUED)
            .filter(genApp -> genApp.getParty().getId().equals(claimantId))
            .anyMatch(genApp -> genApp.getApplicationSubmittedDate().isBefore(holdDeadline));
    }

    private Optional<String> defencePackSkipReason(PcsCaseEntity pcsCase) {
        boolean translation = hasActiveTranslationFlag(pcsCase)
            || hasSubmittedDefenceNeedingTranslation(pcsCase)
            || hasCounterClaimNeedingTranslation(pcsCase);
        if (translation) {
            return Optional.of(TRANSLATION_REQUIRED);
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

    private boolean release14Enabled() {
        return featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4);
    }

    private boolean hasSubmittedDefenceNeedingTranslation(PcsCaseEntity pcsCase) {
        return streamOf(pcsCase.getDefendantResponses())
            .filter(response -> response.getStatus() == DefendantResponseStatus.SUBMITTED)
            .anyMatch(response -> requiresTranslation(response.getLanguageUsed()));
    }

    private boolean hasCounterClaimNeedingTranslation(PcsCaseEntity pcsCase) {
        return includedCounterClaims(pcsCase)
            .anyMatch(counterClaim -> requiresTranslation(counterClaim.getLanguageUsed()));
    }

    // A counterclaim is "included" once it exists on the case, in any state. A counterclaim awaiting its
    // help-with-fees decision stays PENDING_COUNTER_CLAIM_ISSUED (it is only ISSUED after the fee is paid),
    // so a counterclaim needing translation holds the defence pack from that state too - not only once
    // COUNTER_CLAIM_ISSUED.
    private Stream<CounterClaimEntity> includedCounterClaims(PcsCaseEntity pcsCase) {
        return streamOf(pcsCase.getCounterClaims());
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
