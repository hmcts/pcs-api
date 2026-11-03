package uk.gov.hmcts.reform.pcs.data.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceKeyBackfillScriptTest {

    private static final String BACKFILL_SCRIPT = "db/data-migration/HDPI-8828/02_backfill_reference_keys.sql";
    private static final String VALIDATION_SCRIPT = "db/data-migration/HDPI-8828/01_validation.sql";

    private static final String CLAIMANT_TYPE = "claim_party.claimant_type_key";
    private static final String ENTITY_ROLE_CODE = "claim_party.entity_role_code_key";
    private static final String TENANCY_TYPE = "tenancy_licence.tenancy_type_key";
    private static final String HEARING_TYPE = "hearing.hearing_type_key";

    private static final Pattern BACKFILL_BLOCK =
        Pattern.compile("WITH updated AS \\((.*?)\\)\\s*SELECT '([^']+)' AS target", Pattern.DOTALL);
    private static final Pattern PAIR = Pattern.compile("\\('([A-Z_]+)', '([A-Za-z0-9-]+)'\\)");
    private static final Pattern TRIPLE = Pattern.compile("\\('([a-z_.]+)', '([A-Z_]+)', '([A-Za-z0-9-]+)'\\)");

    @Test
    void shouldBackfillExactlyTheImplementedMappings() throws IOException {
        assertThat(backfillCrosswalks().keySet())
            .containsExactlyInAnyOrder(CLAIMANT_TYPE, ENTITY_ROLE_CODE, TENANCY_TYPE, HEARING_TYPE);
    }

    @Test
    void claimantTypeCrosswalkShouldCoverEveryClaimantType() throws IOException {
        assertThat(legacyValues(CLAIMANT_TYPE)).isEqualTo(enumNames(ClaimantType.values()));
    }

    @Test
    void tenancyTypeCrosswalkShouldCoverEveryCombinedLicenceType() throws IOException {
        assertThat(legacyValues(TENANCY_TYPE)).isEqualTo(enumNames(CombinedLicenceType.values()));
    }

    @Test
    void entityRoleCrosswalkShouldCoverEveryPartyRoleExceptUnderlesseeOrMortgagee() throws IOException {
        Set<String> expected = enumNames(PartyRole.values());
        expected.remove(PartyRole.UNDERLESSEE_OR_MORTGAGEE.name());

        assertThat(legacyValues(ENTITY_ROLE_CODE)).isEqualTo(expected);
    }

    @Test
    void hearingTypeCrosswalkShouldCoverEveryHearingTypeExceptOther() throws IOException {
        Set<String> expected = enumNames(HearingType.values());
        expected.remove(HearingType.OTHER.name());

        assertThat(legacyValues(HEARING_TYPE)).isEqualTo(expected);
    }

    @Test
    void validationCrosswalkShouldMatchBackfillCrosswalk() throws IOException {
        Map<String, Map<String, String>> validation = new LinkedHashMap<>();
        Matcher matcher = TRIPLE.matcher(read(VALIDATION_SCRIPT));
        while (matcher.find()) {
            validation.computeIfAbsent(matcher.group(1), target -> new LinkedHashMap<>())
                .put(matcher.group(2), matcher.group(3));
        }

        assertThat(validation).isEqualTo(backfillCrosswalks());
    }

    @Test
    void backfillShouldOnlyUpdateRowsWithoutAKeyAndWithAReferenceRow() throws IOException {
        Matcher matcher = BACKFILL_BLOCK.matcher(read(BACKFILL_SCRIPT));
        int blocks = 0;
        while (matcher.find()) {
            blocks++;
            String block = matcher.group(1);
            assertThat(block).containsPattern("_key IS NULL");
            assertThat(block).containsPattern("JOIN public\\.\\w+ AS ref ON ref\\.key = crosswalk\\.ref_key");
        }

        assertThat(blocks).isEqualTo(4);
    }

    private Set<String> legacyValues(String target) throws IOException {
        return backfillCrosswalks().get(target).keySet();
    }

    private Map<String, Map<String, String>> backfillCrosswalks() throws IOException {
        Map<String, Map<String, String>> crosswalks = new LinkedHashMap<>();
        Matcher block = BACKFILL_BLOCK.matcher(read(BACKFILL_SCRIPT));
        while (block.find()) {
            Map<String, String> crosswalk = new LinkedHashMap<>();
            Matcher pair = PAIR.matcher(block.group(1));
            while (pair.find()) {
                assertThat(crosswalk.put(pair.group(1), pair.group(2)))
                    .as("duplicate legacy value %s for %s", pair.group(1), block.group(2))
                    .isNull();
            }
            crosswalks.put(block.group(2), crosswalk);
        }
        return crosswalks;
    }

    private static Set<String> enumNames(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String read(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
