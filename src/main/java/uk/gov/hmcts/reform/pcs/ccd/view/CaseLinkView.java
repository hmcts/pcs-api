package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.ccd.sdk.type.LinkReason;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkReasonEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseLinkReasonRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
public class CaseLinkView {

    private final CaseLinkReasonRepository caseLinkReasonRepository;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        setCaseLinks(pcsCase, pcsCaseEntity);

    }

    private void setCaseLinks(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<ListValue<CaseLink>> caseLinks = new ArrayList<>();

        if (!pcsCaseEntity.getCaseLinks().isEmpty()) {
            List<CaseLinkEntity> caseLinkEntities = pcsCaseEntity.getCaseLinks();
            Map<UUID, List<CaseLinkReasonEntity>> reasonsByCaseLinkId = getReasonsByCaseLinkId(caseLinkEntities);

            caseLinks = caseLinkEntities.stream()
                .map(linkEntity -> ListValue.<CaseLink>builder()
                    .id(linkEntity.getLinkedCaseReference().toString())
                    .value(
                        CaseLink.builder()
                            .caseReference(linkEntity.getLinkedCaseReference().toString())
                            .caseType(linkEntity.getCcdListId())
                            .reasonForLink(
                                mapReasons(reasonsByCaseLinkId.getOrDefault(linkEntity.getId(), List.of()))
                            )
                            .build()
                    )
                    .build()
                )
                .toList();
        }
        pcsCase.setCaseLinks(caseLinks);
    }

    private Map<UUID, List<CaseLinkReasonEntity>> getReasonsByCaseLinkId(List<CaseLinkEntity> caseLinkEntities) {
        Set<UUID> caseLinkIds = caseLinkEntities.stream()
            .map(CaseLinkEntity::getId)
            .collect(toSet());

        if (caseLinkIds.isEmpty()) {
            return Map.of();
        }

        return caseLinkReasonRepository.findAllByCaseLinkIds(caseLinkIds).stream()
            .collect(groupingBy(reason -> reason.getCaseLink().getId()));
    }

    private List<ListValue<LinkReason>> mapReasons(List<CaseLinkReasonEntity> reasonEntities) {
        return reasonEntities.stream()
            .map(reasonEntity -> ListValue.<LinkReason>builder()
                .id(reasonEntity.getId().toString())
                .value(
                    LinkReason.builder()
                        .reason(reasonEntity.getReasonCode())
                        .build()
                )
                .build()
            )
            .toList();
    }
}
