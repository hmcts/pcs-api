package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

public class UploadAdditionalDocumentsInformationPage implements CcdPageConfiguration, CcdPage {

    private static final String SUPPORTING_EVIDENCE_UPLOAD_CONTENT  = """
                    <p class="govuk-body">
                        You can upload a document to support an application, or to show proof that you have done
                        something.
                    </p>

                    <div class="govuk-details__text govuk-!-margin-bottom-5">
                        <p class="govuk-body">
                            We usually share anything you upload with the other party, for example your landlord,
                            housing association or mortgage provider.
                        </p>
                        <p class="govuk-body">
                            If your application is 'without notice' (where you have asked us to) consider your
                            application without testing the other party) we will not share anything with them.
                        </p>
                    </div>
                    <p class="govuk-body govuk-!-font-weight-bold govuk-!-font-size-24">What you can upload
                    </p>

                    <p class="govuk-body">
                        You can upload any documents that you think are relevant.
                    </p>

                    <p class="govuk-body govuk-!-margin-bottom-0">For example, you can share:</p>
                    <ul class="govuk-list govuk-list--bullet">
                        <li class="govuk-!-font-size-19">evidence that a judge has asked for, like a bank statements
                        showing your rent payments
                        </li>
                        <li class="govuk-!-font-size-19">photographs of the property in a state of disrepair, like a
                        damp in the bathroom</li>
                       <li class="govuk-!-font-size-19">a report from an electrician or a plumber, showing evidence of
                       a repair that needs to be done</li>
                       <li class="govuk-!-font-size-19">emails or letters from the claimant (your landlord, housing
                       association, or mortgage lender)</li>
                    </ul>

                    <p class="govuk-body govuk-!-margin-bottom-0">You can upload the following file types:</p>
                    <ul class="govuk-list govuk-list--bullet">
                        <li class="govuk-!-font-size-19">DOCS/DOCX(Word)</li>
                       <li class="govuk-!-font-size-19">XLS/XLSX(Excel)</li>
                       <li class="govuk-!-font-size-19">PPT/PPTX(PowerPoint)</li>
                       <li class="govuk-!-font-size-19">PDF</li>
                       <li class="govuk-!-font-size-19">RTF</li>
                       <li class="govuk-!-font-size-19">TXT</li>
                       <li class="govuk-!-font-size-19">CSV</li>
                       <li class="govuk-!-font-size-19">JPG/JPEG</li>
                       <li class="govuk-!-font-size-19">PNG</li>
                       <li class="govuk-!-font-size-19">BMP</li>
                       <li class="govuk-!-font-size-19">TIF/TIFF</li>
                    </ul>
                    <p class="govuk-body">
                        You cannot upload video or audio files.
                    </p>
                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("Upload Additional Documents")
            .label(pageKey + "-line-separator", "---")
            .label(pageKey + "-content", SUPPORTING_EVIDENCE_UPLOAD_CONTENT);
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
