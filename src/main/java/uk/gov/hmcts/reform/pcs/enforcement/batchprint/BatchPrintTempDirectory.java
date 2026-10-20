package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Permissions are given at creation, not set afterwards, so the directory is never world readable.
 */
@Slf4j
final class BatchPrintTempDirectory {

    private static final String PREFIX = "batch-print-";

    private static final FileAttribute<Set<PosixFilePermission>> OWNER_ONLY_DIRECTORY =
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));

    static final FileAttribute<Set<PosixFilePermission>> OWNER_ONLY_FILE =
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    private BatchPrintTempDirectory() {
    }

    static Path create() throws IOException {
        return Files.createTempDirectory(PREFIX, OWNER_ONLY_DIRECTORY);
    }

    static void delete(Path directory) {
        if (directory == null) {
            return;
        }
        try {
            FileSystemUtils.deleteRecursively(directory);
        } catch (IOException e) {
            log.warn("Could not delete the batch print temp directory {}", directory, e);
        }
    }
}
