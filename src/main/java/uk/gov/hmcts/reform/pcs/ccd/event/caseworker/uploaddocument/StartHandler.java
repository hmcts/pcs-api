package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.uploaddocument;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.COUNTERCLAIM_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.GEN_APP_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.NONE_PREFIX;

@Component("caseworkerUploadDocumentStartHandler")
@RequiredArgsConstructor
public class StartHandler implements Start<PCSCase, State> {

    private static final DateTimeFormatter RELATED_ENTITY_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        // Counterclaims aren't currently included in the response from PcsCaseView as they are
        // not exposed anywhere yet, so we fetch them from the DB
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        List<CounterClaimEntity> counterClaims = pcsCaseEntity.getCounterClaims();

        List<ListValue<GeneralApplication>> genApps = caseData.getGenApps();

        boolean showRelatedSubmissionsList = genApps.size() + counterClaims.size() > 0;

        DynamicStringList relatedSubmissionsList = showRelatedSubmissionsList
            ? buildRelatedSubmissionsList(pcsCaseEntity, genApps, counterClaims) : null;

        DynamicList relatedPartyList = buildRelatedPartyList(pcsCaseEntity);
        DynamicStringList documentTypeList = createDocumentTypeList(caseData.getLegislativeCountry());

        CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
            .relatedSubmission(relatedSubmissionsList)
            .showRelatedSubmissionsList(VerticalYesNo.from(showRelatedSubmissionsList))
            .relatedParty(relatedPartyList)
            .relatedSubmissionsDocumentType(documentTypeList)
            .standaloneDocumentType(documentTypeList)
            .build();

        caseData.setCaseworkerDocument(caseworkerDocument);

        return caseData;
    }

    private DynamicStringList buildRelatedSubmissionsList(PcsCaseEntity pcsCaseEntity,
                                                          List<ListValue<GeneralApplication>> genApps,
                                                          List<CounterClaimEntity> counterClaims) {

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

        return DynamicStringList.builder()
            .listItems(listElements)
            .build();
    }

    private static List<RelatedSubmission> getGenAppSubmissions(List<ListValue<GeneralApplication>> genApps) {
        return genApps.stream()
            .map(genAppListValue -> {
                GeneralApplication genApp = genAppListValue.getValue();

                String displayLabel = "General Application GA%d".formatted(genApp.getRank());

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

        return counterClaims.stream()
            .filter(counterClaimEntity -> counterClaimEntity.getStatus() != PENDING_COUNTER_CLAIM_ISSUED)
            .map(counterClaimEntity -> {
                UUID counterclaimPartyId = counterClaimEntity.getParty().getId();
                String partyLabel = partyService.getPartyLabel(pcsCaseEntity.getMainClaim(), counterclaimPartyId);

                String displayLabel = "Counterclaim - %s".formatted(partyLabel);

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

    private DynamicList buildRelatedPartyList(PcsCaseEntity pcsCaseEntity) {
        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();
        Map<PartyRole, List<ClaimPartyEntity>> partyRoleListMap = mainClaim.getClaimParties().stream()
            .collect(Collectors.groupingBy(ClaimPartyEntity::getRole));

        List<DynamicListElement> partyElementList = new ArrayList<>();

        partyRoleListMap.getOrDefault(PartyRole.CLAIMANT, List.of()).stream()
            .map(claimPartyEntity -> mapToPartyListElement(mainClaim, claimPartyEntity.getParty()))
            .forEach(partyElementList::add);

        partyRoleListMap.getOrDefault(PartyRole.DEFENDANT, List.of()).stream()
            .map(claimPartyEntity -> mapToPartyListElement(mainClaim, claimPartyEntity.getParty()))
            .forEach(partyElementList::add);

        return DynamicList.builder().listItems(partyElementList).build();
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

    private DynamicStringList createDocumentTypeList(LegislativeCountry legislativeCountry) {
        List<DynamicStringListElement> documentTypeList = Arrays.stream(CaseworkerDocumentType.values())
            .filter(documentType -> documentType.isApplicableFor(legislativeCountry))
            .map(documentType -> DynamicStringListElement.builder()
                .code(documentType.name())
                .label(documentType.getLabel())
                .build())
            .toList();

        return DynamicStringList.builder()
            .listItems(documentTypeList)
            .build();
    }

    @Builder
    @Data
    private static class RelatedSubmission {
        private final String compositeId;
        private final String displayLabel;
        private final LocalDateTime submittedDateTime;
    }

}
