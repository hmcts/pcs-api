package uk.gov.hmcts.reform.pcs.testingsupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementDMTest {

    private DomainDataModelSupportHelper domainDataModelSupportHelper;

    @BeforeEach
    void beforeEach() throws IOException {
        domainDataModelSupportHelper = new DomainDataModelSupportHelper(EnforcementOrder.class);
        File tempFile = File.createTempFile("ccd_fields_report_", ".txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            domainDataModelSupportHelper.printCCDFields(writer);
            System.out.println("Created : " + tempFile.getAbsolutePath());
        }
    }

    @Test
    void shouldCaptureAllCCDAnnotatedFields() throws IOException {
        domainDataModelSupportHelper.addIgnoredClassesToMissingList(EnforcementOrder.class, ClaimEntity.class);
        File tempFile = File.createTempFile("missing_entity_representation_for_ccd_fields_report_",
                                            ".txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            domainDataModelSupportHelper.printMissingCCDFields(writer, EnforcementOrderEntity.class);
            System.out.println("Created Missing field report : " + tempFile.getAbsolutePath());
        }
        assertThat(domainDataModelSupportHelper.findMissingCCDFields(EnforcementOrderEntity.class)).isEmpty();
    }

}
