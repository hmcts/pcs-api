package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.UploadedDocumentsChecklistTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.UploadedDocumentChecklistType;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UploadedDocumentsChecklistTabDetailsBuilder {

    public UploadedDocumentsChecklistTabDetails buildUploadedDocumentsChecklistTabDetails(PCSCase pcsCase) {
        Set<UploadedDocumentChecklistType> documentsYouveUploaded = pcsCase.getDocumentsYouveUploaded();
        if (pcsCase.getLegislativeCountry() != LegislativeCountry.WALES
            || CollectionUtils.isEmpty(documentsYouveUploaded)) {
            return null;
        }

        String labels = documentsYouveUploaded.stream()
            .sorted(Comparator.comparingInt(UploadedDocumentChecklistType::ordinal))
            .map(UploadedDocumentChecklistType::getLabel)
            .collect(Collectors.joining(", "));

        return UploadedDocumentsChecklistTabDetails.builder()
            .documentsYouveUploaded(labels)
            .build();
    }
}
