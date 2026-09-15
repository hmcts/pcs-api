package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.uploaddocument;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentListService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import java.util.List;

@Component("caseworkerUploadDocumentStartHandler")
@RequiredArgsConstructor
public class StartHandler implements Start<PCSCase, State> {

    private final PcsCaseService pcsCaseService;
    private final CaseworkerDocumentListService caseworkerDocumentListService;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        // Counterclaims aren't currently included in the response from PcsCaseView as they are
        // not exposed anywhere yet, so we fetch them from the DB
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        List<CounterClaimEntity> counterClaims = pcsCaseEntity.getCounterClaims();

        List<ListValue<GeneralApplication>> genApps = caseData.getGenApps();

        boolean showRelatedSubmissionsList = caseworkerDocumentListService.hasRelatedSubmissions(
            genApps,
            counterClaims
        );

        DynamicStringList relatedSubmissionsList = showRelatedSubmissionsList
            ? caseworkerDocumentListService.buildRelatedSubmissionsList(pcsCaseEntity, genApps, counterClaims) : null;

        DynamicStringList documentTypeList = caseworkerDocumentListService
            .buildDocumentTypeList(caseData.getLegislativeCountry());

        CaseworkerDocument caseworkerDocument = CaseworkerDocument.builder()
            .relatedSubmission(relatedSubmissionsList)
            .showRelatedSubmissionsList(VerticalYesNo.from(showRelatedSubmissionsList))
            .relatedParty(caseworkerDocumentListService.buildRelatedPartyList(pcsCaseEntity))
            .relatedSubmissionsDocumentType(documentTypeList)
            .standaloneDocumentType(documentTypeList)
            .build();

        caseData.setCaseworkerDocument(caseworkerDocument);

        return caseData;
    }

}
