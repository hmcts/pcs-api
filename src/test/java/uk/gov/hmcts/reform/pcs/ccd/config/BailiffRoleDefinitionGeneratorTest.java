package uk.gov.hmcts.reform.pcs.ccd.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.generator.ConfigGenerator;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BailiffRoleDefinitionGeneratorTest {

    @TempDir
    private Path output;

    @ParameterizedTest(name = "bailiff management enabled={0}")
    @ValueSource(booleans = {false, true})
    void gatesOnlyBailiffCaseTypePermissionsAndLeavesOtherFilesUntouched(boolean enabled) throws IOException {
        FeatureToggleService features = mock(FeatureToggleService.class);
        when(features.isEnabled(FeatureFlag.BAILIFF_MANAGEMENT)).thenReturn(enabled);
        @SuppressWarnings("unchecked")
        ResolvedCCDConfig<PCSCase, State, AccessProfile> config = mock(ResolvedCCDConfig.class);
        when(config.getCaseType()).thenReturn("PCS");
        when(config.getCaseName()).thenReturn("Possession");
        when(config.getCaseDesc()).thenReturn("Possession");
        when(config.getJurId()).thenReturn("PCS");
        when(config.getJurName()).thenReturn("Possession");
        when(config.getJurDesc()).thenReturn("Possession");
        when(config.getPrintableDocumentsUrl()).thenReturn("");

        List<Map<String, String>> permissions = List.of(
            Map.of("UserRole", "bailiff", "CRUD", "CRU"),
            Map.of("UserRole", "bailiff-admin", "CRUD", "CRU"),
            Map.of("UserRole", "bailiff-manager", "CRUD", "CRU"),
            Map.of("UserRole", "hearing-centre-admin", "CRUD", "CRU"),
            Map.of("UserRole", "GS_profile", "CRUD", "R")
        );
        List<Map<String, String>> mappings = List.of(
            Map.of("RoleName", "bailiff", "AccessProfiles", "bailiff"),
            Map.of("RoleName", "bailiff-admin", "AccessProfiles", "bailiff-admin"),
            Map.of("RoleName", "bailiff-manager", "AccessProfiles", "bailiff-manager"),
            Map.of("RoleName", "hmcts-enforcement", "AccessProfiles", "GS_profile"),
            Map.of("RoleName", "hmcts-admin", "AccessProfiles", "GS_profile")
        );
        String mappingsJson = new ObjectMapper().writeValueAsString(mappings);
        ConfigGenerator<PCSCase, State, AccessProfile> writer = (folder, ignored) -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(folder.toPath().resolve("AuthorisationCaseType.json").toFile(), permissions);
                Files.writeString(folder.toPath().resolve("RoleToAccessProfiles.json"), mappingsJson);
                Files.createDirectories(folder.toPath().resolve("CaseEventToFields"));
                Files.writeString(folder.toPath().resolve("CaseEventToFields/question.json"),
                                  "[{\"Label\":\"Does anyone pose a risk to the bailiff?\"}]");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        new BailiffRoleDefinitionGenerator<>(List.of(writer), features).writeConfig(output.toFile(), config);

        ObjectMapper mapper = new ObjectMapper();
        assertThat(mapper.readTree(output.resolve("AuthorisationCaseType.json").toFile()))
            .isEqualTo(mapper.valueToTree(enabled ? permissions : permissions.subList(3, 5)));
        assertThat(Files.readString(output.resolve("RoleToAccessProfiles.json"))).isEqualTo(mappingsJson);
        assertThat(Files.readString(output.resolve("CaseEventToFields/question.json")))
            .isEqualTo("[{\"Label\":\"Does anyone pose a risk to the bailiff?\"}]");
    }
}
