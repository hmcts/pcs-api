package uk.gov.hmcts.reform.pcs.ccd.entity.bundle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * A case's judges' bundle, as the doc toolkit handed it over. Bundles are only ever added; the one with the highest
 * case revision is the bundle as it stands.
 */
@Entity
@Immutable
@Table(name = "case_bundles")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseBundleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID caseId;

    /** The case revision the bundle shows the case as at; it stays accurate until an event changes the bundle. */
    @Column(nullable = false)
    private long caseRevision;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String fileName;

    /** Set by the database when the bundle is saved. */
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    /** The document's id in CDAM, the last part of its URL. */
    public UUID getDocumentId() {
        return UUID.fromString(url.substring(url.lastIndexOf('/') + 1));
    }

    public String getBinaryUrl() {
        return url + "/binary";
    }
}
