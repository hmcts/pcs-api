package uk.gov.hmcts.reform.pcs.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;

import java.util.regex.Pattern;

public class ArchIgnoreTestSources implements ImportOption {

    private static final Pattern TEST_SOURCE_SETS = Pattern.compile(
        ".*/build/classes/java/(functional|integration|smoke|contract|pact)Test/.*");

    @Override
    public boolean includes(Location location) {
        return !location.matches(TEST_SOURCE_SETS);
    }

}
