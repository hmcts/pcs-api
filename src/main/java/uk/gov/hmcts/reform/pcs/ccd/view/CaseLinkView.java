package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.ccd.sdk.type.LinkReason;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.ArrayList;
import java.util.List;

@Component
public class CaseLinkView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        setCaseLinks(pcsCase, pcsCaseEntity);

    }

    private void setCaseLinks(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<ListValue<CaseLink>> caseLinks = new ArrayList<>();

        if (!pcsCaseEntity.getCaseLinks().isEmpty()) {
            caseLinks = pcsCaseEntity.getCaseLinks().stream()
                .map(linkEntity -> ListValue.<CaseLink>builder()
                    .id(linkEntity.getLinkedCaseReference().toString())
                    .value(
                        CaseLink.builder()
                            .caseReference(linkEntity.getLinkedCaseReference().toString())
                            .caseType(linkEntity.getCcdListId())
                            .reasonForLink(
                                linkEntity.getReasons().stream()
                                    .map(reasonEntity -> ListValue.<LinkReason>builder()
                                        .id(reasonEntity.getId().toString())
                                        .value(
                                            LinkReason.builder()
                                                .reason(reasonEntity.getReasonCode())
                                                .build()
                                        )
                                        .build()
                                    )
                                    .toList()
                            )
                            .build()
                    )
                    .build()
                )
                .toList();
        }
        pcsCase.setCaseLinks(caseLinks);
    }
}
