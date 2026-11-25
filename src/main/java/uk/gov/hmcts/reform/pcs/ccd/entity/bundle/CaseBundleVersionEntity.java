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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One version of a case's judges' bundle. Versions are only ever added; the latest is the bundle as it stands. */
@Entity
@Immutable
@Table(name = "case_bundle_versions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseBundleVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID caseId;

    /** The case revision the bundle was built at. */
    @Column(nullable = false)
    private long revision;

    @Column(nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String binaryUrl;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private int pageCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<MissingDocument> missingDocuments;

    // Set here rather than by the database: Hibernate cannot read a generated value back into an immutable entity.
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** A document the bundle could not include: its id, title, folder, page in the bundle and why. */
    public record MissingDocument(String id, String title, String folder, Integer page, String reason) {
    }
}
