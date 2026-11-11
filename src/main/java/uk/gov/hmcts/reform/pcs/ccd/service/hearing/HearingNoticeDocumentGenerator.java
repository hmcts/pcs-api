package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.model.hearingnotice.HearingNoticePayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

/**
 * Thin Docmosis wrapper for the notice of hearing. The filename carries the notice number and recipient,
 * e.g. {@code Notice of hearing 1 - Defendant 1}.
 */
@Service
@AllArgsConstructor
public class HearingNoticeDocumentGenerator {

    static final String TEMPLATE_ID = "CV-PCS-HNO-ENG-Hearing-Notice.docx";

    private final DocAssemblyService docAssemblyService;

    public String generate(HearingNoticePayload payload, String filename) {
        return docAssemblyService.generateDocument(payload, TEMPLATE_ID, OutputType.PDF, filename);
    }
}
