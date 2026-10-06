package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.bundling.CaseBundleFolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A generated case bundle. The stitched PDF is a {@link DocumentEntity} of type CASE_BUNDLE; this
 * row records what the bundle was built from and how, modelled on em-stitching-api's BundleDTO.
 * Exactly one bundle per case is {@code latest}: the one whose documents were selected most
 * recently, whatever order the renders finished in.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "case_bundle")
public class CaseBundleEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private PcsCaseEntity pcsCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private DocumentEntity document;

    private UUID jobId;

    private String bundleTitle;

    private String description;

    private String fileName;

    private String stitchStatus;

    private int pageCount;

    private String sha256;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<CaseBundleFolder> folders = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    private boolean hasTableOfContents;

    private boolean hasCoversheets;

    private boolean hasFolderCoversheets;

    private String pageNumberFormat;

    private Instant documentsSelectedAt;

    private boolean latest;

    @CreationTimestamp
    private Instant createdAt;
}
