package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Loads and caches the Housing Loss Prevention Advice Service (HLPAS) legal advice leaflet enclosed in the
 * defendant claim pack of a citizen claim. The leaflet's location is {@code bulk-print.hlpas-leaflet}; while it
 * is unset the pack goes out without the leaflet.
 */
@Service
@Slf4j
public class HlpasLeafletProvider {

    private final byte[] leaflet;

    public HlpasLeafletProvider(ResourceLoader resourceLoader,
                                @Value("${bulk-print.hlpas-leaflet:}") String leafletLocation) {
        if (StringUtils.isBlank(leafletLocation)) {
            log.warn("No HLPAS legal advice leaflet configured (bulk-print.hlpas-leaflet); citizen claim packs "
                + "will be posted without it");
            this.leaflet = null;
        } else {
            this.leaflet = loadBytes(resourceLoader.getResource(leafletLocation));
        }
    }

    public Optional<byte[]> getLeaflet() {
        return Optional.ofNullable(leaflet);
    }

    private byte[] loadBytes(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load the HLPAS legal advice leaflet from " + resource, e);
        }
    }
}
