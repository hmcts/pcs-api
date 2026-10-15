package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads and caches the static blank forms enclosed in defendant claim packs:
 * ONC 002R (Defence form) and ONC 003 (Counterclaim form). Both forms are read from
 * classpath resources at startup and held in memory so bulk print dispatches avoid repeated disk I/O.
 */
@Service
public class BlankClaimPackFormsProvider {

    private final byte[] defenceForm;
    private final byte[] counterclaimForm;

    public BlankClaimPackFormsProvider(
            @Value("${bulk-print.blank-forms.defence-form}") Resource defenceFormResource,
            @Value("${bulk-print.blank-forms.counterclaim-form}") Resource counterclaimFormResource) {
        this.defenceForm = loadBytes(defenceFormResource, "defence form (ONC 002R)");
        this.counterclaimForm = loadBytes(counterclaimFormResource, "counterclaim form (ONC 003)");
    }

    public byte[] getBlankResponseForm() {
        return defenceForm;
    }

    public byte[] getBlankCounterclaimForm() {
        return counterclaimForm;
    }

    private byte[] loadBytes(Resource resource, String description) {
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load blank " + description + " from " + resource, e);
        }
    }
}
