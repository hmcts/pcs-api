package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.UploadedDocumentsChecklistTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.UploadedDocumentChecklistType;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UploadedDocumentsChecklistTabDetailsBuilderTest {

    private UploadedDocumentsChecklistTabDetailsBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new UploadedDocumentsChecklistTabDetailsBuilder();
    }

    @Test
    void shouldReturnNullForEngland() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .documentsYouveUploaded(Set.of(UploadedDocumentChecklistType.OCCUPATION_LICENCE))
            .build();

        assertThat(builder.buildUploadedDocumentsChecklistTabDetails(pcsCase)).isNull();
    }

    @Test
    void shouldReturnNullWhenNoSelections() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .documentsYouveUploaded(Set.of())
            .build();

        assertThat(builder.buildUploadedDocumentsChecklistTabDetails(pcsCase)).isNull();
    }

    @Test
    void shouldBuildCommaSeparatedLabelsInEnumOrder() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .documentsYouveUploaded(Set.of(
                UploadedDocumentChecklistType.CURRENT_GAS_SAFETY_REPORT,
                UploadedDocumentChecklistType.OCCUPATION_LICENCE,
                UploadedDocumentChecklistType.ENERGY_PERFORMANCE_CERTIFICATE
            ))
            .build();

        UploadedDocumentsChecklistTabDetails details = builder.buildUploadedDocumentsChecklistTabDetails(pcsCase);

        assertThat(details.getDocumentsYouveUploaded()).isEqualTo(
            "Occupation contract or licence, Energy performance certificate, Current gas safety report"
        );
    }
}
