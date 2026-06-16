package uk.gov.hmcts.reform.pcs.ccd.generator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static org.assertj.core.api.Assertions.assertThat;

class JudicialHistoryAuthorisationGeneratorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    private Path outputFolder;

    private final JudicialHistoryAuthorisationGenerator underTest = new JudicialHistoryAuthorisationGenerator();

    @Test
    void shouldGenerateCaseHistoryFieldAuthorisationForJudicialRoles() throws Exception {
        underTest.write(outputFolder.toFile(), config());

        assertCaseHistoryAuthorisation(AccessProfile.CIRCUIT_JUDGE);
        assertCaseHistoryAuthorisation(AccessProfile.FEE_PAID_JUDGE);
        assertCaseHistoryAuthorisation(AccessProfile.JUDGE);
        assertCaseHistoryAuthorisation(AccessProfile.LEADERSHIP_JUDGE);
    }

    private void assertCaseHistoryAuthorisation(AccessProfile role) throws Exception {
        Path authorisationFile = outputFolder.resolve("AuthorisationCaseField").resolve(role.getRole() + ".json");
        List<Map<String, Object>> authorisations = OBJECT_MAPPER.readValue(
            authorisationFile.toFile(),
            new TypeReference<>() {
            }
        );

        assertThat(authorisations)
            .anySatisfy(authorisation -> {
                assertThat(authorisation).containsEntry("CaseFieldID", "caseHistory");
                assertThat(authorisation).containsEntry("CaseTypeID", "PCS");
                assertThat(authorisation).containsEntry("CRUD", "R");
                assertThat(authorisation).containsEntry("UserRole", role.getRole());
            });
    }

    private ResolvedCCDConfig<PCSCase, State, AccessProfile> config() {
        ResolvedCCDConfig<PCSCase, State, AccessProfile> initialConfig =
            new ResolvedCCDConfig<>(PCSCase.class, State.class, AccessProfile.class, Map.of(), ImmutableSet.of());
        ConfigBuilderImpl<PCSCase, State, AccessProfile> builder = new ConfigBuilderImpl<>(initialConfig);
        builder.caseType("PCS", "Possession", "Possession Case Type");
        return builder.build();
    }
}
