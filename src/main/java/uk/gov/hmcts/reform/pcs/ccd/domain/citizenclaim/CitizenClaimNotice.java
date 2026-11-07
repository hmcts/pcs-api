package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimNotice {

    private VerticalYesNo noticeServed;

    @CCD(typeOverride = TextArea)
    private String noNoticeReason;

    private CitizenClaimNoticeType noticeType;

    private String noticeTypeOther;

    private LocalDate noticeDate;

    @CCD(typeOverride = MultiSelectList, typeParameterOverride = "CitizenClaimNoticeMethod")
    private Set<CitizenClaimNoticeMethod> noticeMethods;

    private LocalDate postedDate;

    private LocalDate deliveredDate;

    private String handedToName;

    private LocalDate handedDate;

    private String emailedTo;

    private LocalDate emailedDate;

    @CCD(typeOverride = TextArea)
    private String otherElectronicDetails;

    private LocalDate otherElectronicDate;

    @CCD(typeOverride = TextArea)
    private String otherMethodDetails;

    private LocalDate otherMethodDate;

    private VerticalYesNo noticeAvailable;

    @CCD(typeOverride = TextArea)
    private String noticeUnavailableReason;

    private List<ListValue<Document>> noticeDocuments;

}
