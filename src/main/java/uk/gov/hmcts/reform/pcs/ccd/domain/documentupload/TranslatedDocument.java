package uk.gov.hmcts.reform.pcs.ccd.domain.documentupload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.TranslatedLanguage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslatedDocument {

    @CCD(label = "Translated document")
    private Document document;

    @CCD(label = "What language is this document in?")
    private TranslatedLanguage language;

}
