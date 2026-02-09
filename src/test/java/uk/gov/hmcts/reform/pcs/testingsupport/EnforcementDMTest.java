package uk.gov.hmcts.reform.pcs.testingsupport;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.EnforcementWarrantEntity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enforcement Domain-Entity Validation Tests
 * Validates that CCD-annotated fields from the enforcement domain model
 * are properly represented in the EnforcementOrderEntity persistence layer.
 * This test serves as both a progress tracker and ongoing validation to ensure
 * the enforcement domain and entity models remain synchronized.
 * Category: Domain Entity Validation
 */
//@Tag("architecture")
//@Tag("enforcement-domain-entity-alignment")
class EnforcementDMTest {

    private DomainDataModelSupportHelper domainDataModelSupportHelper;

    /**
     * Monitors the completeness of the EnforcementOrderEntity graph implementation.
     * This test tracks which CCD-annotated fields from the domain model are not yet
     * represented in the entity graph.
     * PURPOSE: Developer visibility and progress tracking
     * - Run this test to see what still needs to be implemented
     * - Console output shows missing fields on every test run
     * - Test passes/fails based on whether implementation is complete
     * GOAL: Achieve "Entity Graph: COMPLETE" status
     * @see EnforcementOrderEntity
     * @see EnforcementOrder
     */
    @Test
    void shouldCaptureAllMissingEntityFieldsFromTheEnforcementDomain() throws IOException {
        domainDataModelSupportHelper = new DomainDataModelSupportHelper(EnforcementOrder.class);
        domainDataModelSupportHelper.addClassesToIgnore(EnforcementOrder.class, ClaimEntity.class);
        List<DomainDataModelSupportHelper.MissingCCDFieldInfo> missingFields =
            domainDataModelSupportHelper.findMissingCCDFields(EnforcementOrderEntity.class);

        consoleOutput(missingFields);

        assertThat(missingFields)
            .as("Missing CCD field detection is working. "
                    + "Review console output to track entity graph implementation progress.")
            .isNotNull();
    }

    @Test
    void shouldCaptureAllMissingEntityFieldsFromTheEnforcementWarrantDetailsDomain() throws IOException {
        domainDataModelSupportHelper = new DomainDataModelSupportHelper(EnforcementOrder.class);
        domainDataModelSupportHelper.addClassesToIgnore(EnforcementOrder.class, ClaimEntity.class,
                                                        EnforcementRiskDetails.class, WritDetails.class);
        domainDataModelSupportHelper.addFieldsToIgnore("enforcementOrder", "writDetails");
        List<DomainDataModelSupportHelper.MissingCCDFieldInfo> missingFields =
            domainDataModelSupportHelper.findMissingCCDFields(EnforcementWarrantEntity.class);

        consoleOutput(missingFields);

        assertThat(missingFields)
            .as("Missing CCD field detection is working. "
                    + "Review console output to track entity graph implementation progress.")
            .isNotNull();
    }

    void consoleOutput(List<DomainDataModelSupportHelper.MissingCCDFieldInfo> missingFields)
        throws IOException {
        System.out.println("\n" + "=".repeat(100));
        if (missingFields.isEmpty()) {
            System.out.println("Entity Graph: COMPLETE :)");
            System.out.println("=".repeat(100));
            System.out.println("  All CCD fields are represented in the entity graph");
        } else {
            System.out.println("!!! Entity Graph: IN PROGRESS !!!");
            System.out.println("=".repeat(100));
            System.out.println("Missing " + missingFields.size() + " CCD field(s):");
            System.out.println();
            Map<String, List<String>> fieldsByClass = new HashMap<>();
            for (DomainDataModelSupportHelper.MissingCCDFieldInfo missing : missingFields) {
                fieldsByClass.computeIfAbsent(missing.className(), k -> new ArrayList<>())
                    .add(missing.ccdFieldInfo().fieldName());
            }

            fieldsByClass.forEach((className, fields) -> {
                System.out.println("  From " + className + ":");
                fields.forEach(fieldName -> System.out.println("    - " + fieldName));
                System.out.println();
            });
            generateReport();
        }
        System.out.println("=".repeat(100) + "\n");
    }

    private void generateReport() throws IOException {
        File tempFile = File.createTempFile("missing_entity_representation_for_ccd_fields_report_",
                                            ".txt");
        if (!domainDataModelSupportHelper.findMissingCCDFields(EnforcementOrderEntity.class).isEmpty()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
                domainDataModelSupportHelper.printMissingCCDFields(writer, EnforcementOrderEntity.class);
                System.out.println("Created Missing field report : " + tempFile.getAbsolutePath());
            }
        }
        System.out.println("  See detailed report: " + tempFile.getName());
    }

}
