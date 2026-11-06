package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads and caches the static blank forms enclosed in defendant claim packs:
 * ONC 002R (Defence form) and ONC 003 (Counterclaim form), plus their Welsh versions, ONC 002RW and the
 * Welsh ONC 003, for packs to a property in Wales. All forms are read from classpath resources at startup
 * and held in memory so bulk print dispatches avoid repeated disk I/O.
 */
@Service
public class BlankClaimPackFormsProvider {

    private final byte[] defenceForm;
    private final byte[] counterclaimForm;
    private final byte[] defenceFormWelsh;
    private final byte[] counterclaimFormWelsh;

    public BlankClaimPackFormsProvider(
            @Value("${bulk-print.blank-forms.defence-form}") Resource defenceFormResource,
            @Value("${bulk-print.blank-forms.counterclaim-form}") Resource counterclaimFormResource,
            @Value("${bulk-print.blank-forms.defence-form-welsh}") Resource defenceFormWelshResource,
            @Value("${bulk-print.blank-forms.counterclaim-form-welsh}") Resource counterclaimFormWelshResource) {
        this.defenceForm = loadBytes(defenceFormResource, "defence form (ONC 002R)");
        this.counterclaimForm = loadBytes(counterclaimFormResource, "counterclaim form (ONC 003)");
        this.defenceFormWelsh = loadBytes(defenceFormWelshResource, "Welsh defence form (ONC 002RW)");
        this.counterclaimFormWelsh = loadBytes(counterclaimFormWelshResource, "Welsh counterclaim form (ONC 003)");
    }

    public byte[] getBlankResponseForm() {
        return defenceForm;
    }

    public byte[] getBlankCounterclaimForm() {
        return counterclaimForm;
    }

    public byte[] getBlankResponseFormWelsh() {
        return defenceFormWelsh;
    }

    public byte[] getBlankCounterclaimFormWelsh() {
        return counterclaimFormWelsh;
    }

    private byte[] loadBytes(Resource resource, String description) {
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load blank " + description + " from " + resource, e);
        }
    }
}
