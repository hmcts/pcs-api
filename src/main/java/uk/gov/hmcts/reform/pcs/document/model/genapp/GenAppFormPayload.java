package uk.gov.hmcts.reform.pcs.document.model.genapp;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.document.model.Document;
import uk.gov.hmcts.reform.pcs.document.model.Party;
import uk.gov.hmcts.reform.pcs.document.model.StatementOfTruth;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class GenAppFormPayload implements FormPayload {

    private String caseReference;
    private String caseName;
    private String propertyAddress;
    private LocalDate issuedOn;
    private LocalDate submittedOn;
    private Party applicant;
    private GenAppType applicationType;
    private VerticalYesNo within14Days;
    private String whatOrderWanted;
    private VerticalYesNo otherPartiesAgreed;
    private VerticalYesNo withoutNotice;
    private String withoutNoticeReason;
    private VerticalYesNo documentUploadWanted;
    private List<Document> uploadedDocuments;
    private StatementOfTruth statementOfTruth;

}
