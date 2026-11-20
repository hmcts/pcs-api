package uk.gov.hmcts.reform.pcs.ccd.entity.claim;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notice_of_possession")
public class NoticeOfPossessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JsonBackReference
    private ClaimEntity claim;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo noticeServed;

    private String noticeType;

    @Enumerated(EnumType.STRING)
    private NoticeServiceMethod servingMethod;

    private String noticeDetails;

    private LocalDate noticeDate;

    private LocalDateTime noticeDateTime;

    private String noticeStatement;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo isAbleToUploadDocument;

    private String unableToUploadReason;

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "noticeOfPossession", orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    @JsonManagedReference
    private List<NoticeServiceMethodEntity> serviceMethods = new ArrayList<>();

    @UpdateTimestamp
    private LocalDateTime lastModified;

    public void addServiceMethod(NoticeServiceMethodEntity serviceMethod) {
        serviceMethod.setNoticeOfPossession(this);
        serviceMethods.add(serviceMethod);
    }

}
