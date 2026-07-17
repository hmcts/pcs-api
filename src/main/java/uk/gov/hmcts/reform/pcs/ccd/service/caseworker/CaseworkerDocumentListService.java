package uk.gov.hmcts.reform.pcs.ccd.service.caseworker;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.COUNTERCLAIM_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.GEN_APP_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.NONE_PREFIX;

@Service
@RequiredArgsConstructor
public class CaseworkerDocumentListService {

    private static final DateTimeFormatter RELATED_ENTITY_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final PartyService partyService;

    public boolean hasRelatedSubmissions(List<ListValue<GeneralApplication>> genApps,
                                         List<CounterClaimEntity> counterClaims) {
        return !CollectionUtils.isEmpty(genApps)
            || !getOpenCounterClaims(counterClaims).isEmpty();
    }

    public DynamicStringList buildRelatedSubmissionsList(PcsCaseEntity pcsCaseEntity,
                                                         List<ListValue<GeneralApplication>> genApps,
                                                         List<CounterClaimEntity> counterClaims) {
        return buildRelatedSubmissionsList(pcsCaseEntity, genApps, counterClaims, null);
    }

    public DynamicStringList buildRelatedSubmissionsList(PcsCaseEntity pcsCaseEntity,
                                                         List<ListValue<GeneralApplication>> genApps,
                                                         List<CounterClaimEntity> counterClaims,
                                                         DynamicStringList existingList) {
        List<RelatedSubmission> genAppSubmissions = getGenAppSubmissions(genApps);
        List<RelatedSubmission> counterClaimSubmissions = getCounterClaimSubmissions(counterClaims, pcsCaseEntity);

        List<RelatedSubmission> relatedSubmissions = new ArrayList<>(genAppSubmissions);
        relatedSubmissions.addAll(counterClaimSubmissions);
        relatedSubmissions.sort(comparing(RelatedSubmission::getSubmittedDateTime).reversed());

        List<DynamicStringListElement> listElements = relatedSubmissions.stream()
            .map(relatedEntity -> DynamicStringListElement.builder()
                .code(relatedEntity.getCompositeId())
                .label(relatedEntity.getDisplayLabel())
                .build())
            .collect(Collectors.toCollection(ArrayList::new));

        listElements.add(DynamicStringListElement.builder()
            .code(NONE_PREFIX)
            .label("Not related to an application or counterclaim")
            .build());

        DynamicStringListElement selected = existingList == null ? null : existingList.getValue();
        return DynamicStringList.builder()
            .value(retainSelectedValue(selected, listElements))
            .listItems(listElements)
            .build();
    }

    public DynamicList buildRelatedPartyList(PcsCaseEntity pcsCaseEntity) {
        return buildRelatedPartyList(pcsCaseEntity, null);
    }

    public DynamicList buildRelatedPartyList(PcsCaseEntity pcsCaseEntity, DynamicList existingList) {
        final DynamicListElement selected = existingList == null ? null : existingList.getValue();
        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();
        if (mainClaim == null || CollectionUtils.isEmpty(mainClaim.getClaimParties())) {
            return DynamicList.builder()
                .value(null)
                .listItems(List.of())
                .build();
        }

        Map<PartyRole, List<ClaimPartyEntity>> partiesByRole = mainClaim.getClaimParties().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(ClaimPartyEntity::getRole));

        List<DynamicListElement> partyOptions = new ArrayList<>();
        partiesByRole.getOrDefault(PartyRole.CLAIMANT, List.of()).stream()
            .map(ClaimPartyEntity::getParty)
            .filter(Objects::nonNull)
            .map(party -> mapToPartyListElement(mainClaim, party))
            .forEach(partyOptions::add);

        partiesByRole.getOrDefault(PartyRole.DEFENDANT, List.of()).stream()
            .map(ClaimPartyEntity::getParty)
            .filter(Objects::nonNull)
            .map(party -> mapToPartyListElement(mainClaim, party))
            .forEach(partyOptions::add);

        return DynamicList.builder()
            .value(retainSelectedValue(selected, partyOptions))
            .listItems(partyOptions)
            .build();
    }

    public DynamicStringList buildDocumentTypeList(LegislativeCountry legislativeCountry) {
        return buildDocumentTypeList(legislativeCountry, null);
    }

    public DynamicStringList buildDocumentTypeList(LegislativeCountry legislativeCountry,
                                                  DynamicStringList existingList) {
        if (legislativeCountry == null) {
            return DynamicStringList.builder()
                .value(null)
                .listItems(List.of())
                .build();
        }

        List<DynamicStringListElement> documentTypeList = Arrays.stream(CaseworkerDocumentType.values())
            .filter(documentType -> documentType.isApplicableFor(legislativeCountry))
            .map(documentType -> DynamicStringListElement.builder()
                .code(documentType.name())
                .label(documentType.getLabel())
                .build())
            .toList();

        DynamicStringListElement selected = existingList == null ? null : existingList.getValue();
        return DynamicStringList.builder()
            .value(retainSelectedValue(selected, documentTypeList))
            .listItems(documentTypeList)
            .build();
    }

    private static List<RelatedSubmission> getGenAppSubmissions(List<ListValue<GeneralApplication>> genApps) {
        if (CollectionUtils.isEmpty(genApps)) {
            return List.of();
        }

        return genApps.stream()
            .map(genAppListValue -> {
                GeneralApplication genApp = genAppListValue.getValue();
                String displayLabel = "Gen app GA%d".formatted(genApp.getRank());

                LocalDateTime submittedDate = genApp.getSubmittedOn();
                displayLabel += " - submitted %s".formatted(RELATED_ENTITY_DATE_FORMATTER.format(submittedDate));

                return RelatedSubmission.builder()
                    .compositeId(GEN_APP_ID_PREFIX + ":" + genAppListValue.getId())
                    .submittedDateTime(submittedDate)
                    .displayLabel(displayLabel)
                    .build();
            })
            .toList();
    }

    private List<RelatedSubmission> getCounterClaimSubmissions(List<CounterClaimEntity> counterClaims,
                                                               PcsCaseEntity pcsCaseEntity) {
        return getOpenCounterClaims(counterClaims).stream()
            .map(counterClaimEntity -> {
                UUID counterclaimPartyId = counterClaimEntity.getParty().getId();
                String partyLabel = partyService.getPartyLabel(pcsCaseEntity.getMainClaim(), counterclaimPartyId);

                String displayLabel = "Counter claim %s".formatted(counterClaimReference(partyLabel));

                LocalDateTime submittedDate = counterClaimEntity.getClaimSubmittedDate();
                displayLabel += " - submitted %s".formatted(RELATED_ENTITY_DATE_FORMATTER.format(submittedDate));

                return RelatedSubmission.builder()
                    .compositeId(COUNTERCLAIM_ID_PREFIX + ":" + counterClaimEntity.getId())
                    .submittedDateTime(submittedDate)
                    .displayLabel(displayLabel)
                    .build();
            })
            .toList();
    }

    private static List<CounterClaimEntity> getOpenCounterClaims(List<CounterClaimEntity> counterClaims) {
        if (CollectionUtils.isEmpty(counterClaims)) {
            return List.of();
        }

        return counterClaims.stream()
            .filter(counterClaimEntity -> counterClaimEntity.getStatus() != PENDING_COUNTER_CLAIM_ISSUED)
            .toList();
    }

    private DynamicListElement mapToPartyListElement(ClaimEntity mainClaim, PartyEntity partyEntity) {
        String partyName = partyService.getPartyName(partyEntity);
        String partyLabel = partyService.getPartyLabel(mainClaim, partyEntity.getId());
        String label = ("%s - %s").formatted(partyName, partyLabel);
        return DynamicListElement.builder()
            .code(partyEntity.getId())
            .label(label)
            .build();
    }

    private static String counterClaimReference(String partyLabel) {
        if (partyLabel == null) {
            return "CC";
        }

        String rank = partyLabel.replaceAll("\\D+", "");
        return rank.isBlank() ? "CC" : "CC" + rank;
    }

    private DynamicListElement retainSelectedValue(DynamicListElement selected,
                                                   List<DynamicListElement> options) {
        if (selected == null || selected.getCode() == null) {
            if (selected == null || selected.getLabel() == null) {
                return null;
            }
            return options.stream()
                .filter(option -> selected.getLabel().equals(option.getLabel()))
                .findFirst()
                .orElse(null);
        }

        return options.stream()
            .filter(option -> selected.getCode().toString().equals(option.getCode().toString())
                || selected.getLabel() != null && selected.getLabel().equals(option.getLabel()))
            .findFirst()
            .orElse(null);
    }

    private DynamicStringListElement retainSelectedValue(DynamicStringListElement selected,
                                                         List<DynamicStringListElement> options) {
        if (selected == null || selected.getCode() == null) {
            if (selected == null || selected.getLabel() == null) {
                return null;
            }
            return options.stream()
                .filter(option -> selected.getLabel().equals(option.getLabel()))
                .findFirst()
                .orElse(null);
        }

        return options.stream()
            .filter(option -> selected.getCode().equals(option.getCode())
                || selected.getLabel() != null && selected.getLabel().equals(option.getLabel()))
            .findFirst()
            .orElse(null);
    }

    @Builder
    @Data
    private static class RelatedSubmission {
        private final String compositeId;
        private final String displayLabel;
        private final LocalDateTime submittedDateTime;
    }
}
