package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.isPopulated;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Selects gen-app envelopes per recipient. Issued with-notice defendant CUI GAs with a submission PDF go to
 * every claim party whose contact preferences include post (or are unset). Without-notice GAs,
 * non-defendant-applicant GAs, ExUI/caseworker GAs produce no candidates.
 */
@Service
public class GenAppPackSelector {

    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final SentPackDocuments sentPackDocuments;
    private final PackSkipRules packSkipRules;

    public GenAppPackSelector(ClaimActivityLogRepository claimActivityLogRepository,
                              SentPackDocuments sentPackDocuments,
                              PackSkipRules packSkipRules) {
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.sentPackDocuments = sentPackDocuments;
        this.packSkipRules = packSkipRules;
    }

    public List<GenAppPackCandidate> findGenAppPackCandidates(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return List.of();
        }
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        List<PartyEntity> defendants = partiesByRole(claim, PartyRole.DEFENDANT);
        List<PartyEntity> allParties = new ArrayList<>(claimants);
        allParties.addAll(defendants);
        if (allParties.isEmpty()) {
            return List.of();
        }

        Set<UUID> defendantIds = defendants.stream().map(PartyEntity::getId).collect(Collectors.toSet());

        Map<UUID, PartyRole> roleByPartyId = Stream.concat(
                claimants.stream().map(party -> Map.entry(party.getId(), PartyRole.CLAIMANT)),
                defendants.stream().map(party -> Map.entry(party.getId(), PartyRole.DEFENDANT)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));

        Set<String> sent =
            sentPackDocuments.sentDocumentKeys(claimActivityLogRepository.findAllByPcsCase_Id(pcsCase.getId()));

        List<GenAppPackCandidate> candidates = new ArrayList<>();
        for (GenAppEntity genApp : defendantCuiWithNoticeGenApps(pcsCase, defendantIds)) {
            if (packSkipRules.shouldSkipGenAppPack(pcsCase, genApp)) {
                continue;
            }
            DocumentEntity document = genApp.getSubmissionDocument();
            for (PartyEntity party : allParties) {
                if (!wantsPost(party) || sent.contains(key(party, document))) {
                    continue;
                }
                candidates.add(new GenAppPackCandidate(
                    roleByPartyId.getOrDefault(party.getId(), PartyRole.DEFENDANT), party, List.of(document)));
            }
        }
        return candidates;
    }

    private List<GenAppEntity> defendantCuiWithNoticeGenApps(PcsCaseEntity pcsCase, Set<UUID> defendantIds) {
        if (pcsCase.getGenApps() == null || pcsCase.getGenApps().isEmpty()) {
            return List.of();
        }
        return pcsCase.getGenApps().stream()
            .filter(genApp -> isDefendantCuiWithNoticeAndDocument(genApp, defendantIds))
            .sorted(Comparator.comparing(GenAppEntity::getRank, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }

    private boolean isDefendantCuiWithNoticeAndDocument(GenAppEntity genApp, Set<UUID> defendantIds) {
        return isIssuedWithNoticeAndDocument(genApp) && isDefendantCuiGenApp(genApp, defendantIds);
    }

    private boolean isDefendantCuiGenApp(GenAppEntity genApp, Set<UUID> defendantIds) {
        PartyEntity applicant = genApp.getParty();
        if (applicant == null || !defendantIds.contains(applicant.getId())) {
            return false;
        }
        if (genApp.getApplicationReceivedDate() != null) {
            return false;
        }
        StatementOfTruthEntity statementOfTruth = genApp.getStatementOfTruth();
        return statementOfTruth == null || !isPopulated(statementOfTruth.getFirmName());
    }

    private boolean isIssuedWithNoticeAndDocument(GenAppEntity genApp) {
        return genApp.getState() == GenAppState.GEN_APP_ISSUED
            && genApp.getSubmissionDocument() != null
            && genApp.getWithoutNotice() != VerticalYesNo.YES;
    }

    private boolean wantsPost(PartyEntity party) {
        ContactPreferencesEntity preferences = party.getContactPreferences();
        if (preferences == null || preferences.getContactByPost() == null) {
            return true;
        }
        return preferences.getContactByPost() != VerticalYesNo.NO;
    }

    private String key(PartyEntity party, DocumentEntity document) {
        return SentPackDocuments.key(party.getId(), document.getId());
    }
}
