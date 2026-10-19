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
 * The directory one batch print is built in: its own, inside the system temp directory, and readable only by
 * the user pcs-api runs as.
 *
 * <p>The system temp directory itself is writable by every user on the host, so a file created straight into
 * it can be read by anything else running there, and a bailiff's batch print holds several cases' warrants,
 * risk assessments and notices of eviction. The permissions are given at creation, not set afterwards, so
 * there is no moment at which the directory exists and is readable.</p>
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

    /**
     * A new directory for this batch print, which the caller owns and has to delete.
     */
    static Path create() throws IOException {
        return Files.createTempDirectory(PREFIX, OWNER_ONLY_DIRECTORY);
    }

    /**
     * Deletes the directory and anything left in it. Called on every way out of a batch print, so a failure to
     * delete is logged rather than thrown: it must not replace the answer the bailiff is getting.
     */
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
