package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import org.springframework.core.io.AbstractResource;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A finished batch print in a temp file, which is deleted once it has been read, so the file is streamed to the
 * bailiff without being held in memory. It can be read only once.
 */
public class BatchPrintFile extends AbstractResource {

    private final Path path;

    public BatchPrintFile(Path path) {
        this.path = path;
    }

    @Override
    public String getDescription() {
        return "batch print file [" + path + "]";
    }

    @Override
    public boolean exists() {
        return Files.exists(path);
    }

    @Override
    public long contentLength() throws IOException {
        return Files.size(path);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FilterInputStream(Files.newInputStream(path)) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Files.deleteIfExists(path);
                }
            }
        };
    }
}
