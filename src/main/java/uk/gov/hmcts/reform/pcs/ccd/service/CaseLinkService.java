package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.ccd.sdk.type.LinkReason;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkReasonEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CaseLinkService {

    public void mergeCaseLinks(List<ListValue<CaseLink>> incomingLinkedCases, PcsCaseEntity pcsCaseEntity) {

        Map<Long, CaseLinkEntity> existingLinkedCases =
            pcsCaseEntity.getCaseLinks().stream()
                .collect(Collectors.toMap(CaseLinkEntity::getLinkedCaseReference,
                                          Function.identity()
                ));

        List<CaseLinkEntity> mergedCaseLinkEntities = new ArrayList<>();

        for (ListValue<CaseLink> caseLinkListValue : incomingLinkedCases) {
            CaseLink dto = caseLinkListValue.getValue();
            Long incomingCaseRef = Long.valueOf(dto.getCaseReference());

            CaseLinkEntity caseLinkEntity = existingLinkedCases.remove(incomingCaseRef);

            if (caseLinkEntity == null) {
                caseLinkEntity = new CaseLinkEntity();
                caseLinkEntity.setPcsCase(pcsCaseEntity);
                caseLinkEntity.setLinkedCaseReference(incomingCaseRef);
            }

            caseLinkEntity.setCcdListId(dto.getCaseType());

            caseLinkEntity.getReasons().clear();

            if (dto.getReasonForLink() != null) {
                for (ListValue<LinkReason> incomingLinkReason : dto.getReasonForLink()) {
                    CaseLinkReasonEntity caseLinkReasonEntity = CaseLinkReasonEntity.builder()
                        .caseLink(caseLinkEntity)
                        .reasonCode(incomingLinkReason.getValue().getReason())
                        .build();
                    caseLinkEntity.getReasons().add(caseLinkReasonEntity);
                }
            }

            mergedCaseLinkEntities.add(caseLinkEntity);
        }

        pcsCaseEntity.getCaseLinks().clear();
        pcsCaseEntity.getCaseLinks().addAll(mergedCaseLinkEntities);

    }
}

