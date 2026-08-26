import { LoginAction, NavigateToUrlAction, CreateCaseAPIAction, CreateCaseAction } from "@utils/actions/custom-actions";
import { CaseManagementAction } from "@utils/actions/custom-actions/custom-actions-caseManagement";
import { ErrorValidationAction } from "@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementErrorValidation.action";
import { LinkSolicitorAPIAction } from "@utils/actions/custom-actions/linkSolicitorAPI.action";
import { CheckAction, ClickButtonAction, ClickRadioButtonAction, ClickTabAction, InputDateAction, InputTextAction, SelectAction, UploadFileAction } from "@utils/actions/element-actions";
import { ClickLinkAction } from "@utils/actions/element-actions/clickLink.action";
import { ClickSummaryAction } from "@utils/actions/element-actions/clickSummary.action";
import { RetryOnCallBackError } from "@utils/actions/element-actions/reTryOnCallBackError.action";
import { IAction } from "@utils/interfaces";


export class ActionCMRegistry {
  private static actions: Map<string, IAction> = new Map<string, IAction>([
    ['check', new CheckAction()],
    ['uncheck', new CheckAction()],
    ['clickButton', new ClickButtonAction()],
    ['clickSummary', new ClickSummaryAction()],
    ['clickLink', new ClickLinkAction()],
    ['clickLinkAndVerifyNewTabTitle', new ClickLinkAction()],
    ['clickLinkAndVerifySameTabTitle', new ClickLinkAction()],
    ['clickRadioButton', new ClickRadioButtonAction()],
    ['clickTab', new ClickTabAction()],
    ['inputText', new InputTextAction()],
    ['inputDate', new InputDateAction()],
    ['select', new SelectAction()],
    ['uploadFile', new UploadFileAction()],
    ['login', new LoginAction()],
    ['createUser', new LoginAction()],
    ['navigateToUrl', new NavigateToUrlAction()],
    ['createCaseAPI', new CreateCaseAPIAction()],
    ['submitCaseAPI', new CreateCaseAPIAction()],
    ['deleteCaseRole', new CreateCaseAPIAction()],
    ['getCaseAPI', new CreateCaseAPIAction()],
    ['updatePaymentAPI', new CreateCaseAPIAction()],
    ['linkSolicitorAPI', new LinkSolicitorAPIAction()],
    ['makeAnApplicationAPI', new CreateCaseAPIAction()],
    ['manageHearingAPI', new CreateCaseAPIAction()],
    ['validateCaseFileViewFolders', new CreateCaseAction()],
    ['validateCaseFileViewIndividualFolder', new CreateCaseAction()],
    ['validateDefendantDetails', new CaseManagementAction()],
    ['validateClaimantDetails', new CaseManagementAction()],
    ['validateCaseSummaryDetails', new CreateCaseAction()],
    ['navigateToSummaryPage', new CaseManagementAction()],
    ['selectAnEvent', new CaseManagementAction()],
    ['selectDocumentToAmend', new CaseManagementAction()],
    ['changeCaseState', new CaseManagementAction()],
    ['confirmCaseStateChange', new CaseManagementAction()],
    ['getAllPartyDetails', new CaseManagementAction()],
    ['enterApplicationDetails', new CaseManagementAction()],
    ['confirmIfCourtHearingInNext14Days', new CaseManagementAction()],
    ['enterApplicationFeeDetails', new CaseManagementAction()],
    ['selectDynamicAppAndPartyDocRelatedTo', new CaseManagementAction()],
    ['uploadADocument', new CaseManagementAction()],
    ['confirmUpload', new CaseManagementAction()],
    ['confirmAmend', new CaseManagementAction()],
    ['enterApplicationConsentAndNotice', new CaseManagementAction()],
    ['verifyReferToJudge', new CaseManagementAction()],
    ['selectManageHearing', new CaseManagementAction()],
    ['editHearing', new CaseManagementAction()],
    ['cancelHearing', new CaseManagementAction()],
    ['confirmHearingCancelled', new CaseManagementAction()],
    ['verifyGenAppConfirm', new CaseManagementAction()],
    ['confirmHearingEdited', new CaseManagementAction()],
    ['uploadRelativeEvidence', new CaseManagementAction()],
    ['errorValidationSelectDocumentPage', new ErrorValidationAction()],
    ['errorValidationChangeCaseStatePage', new ErrorValidationAction()],
    ['errorValidationSelectDocumentPage', new ErrorValidationAction()],
    ['addReviewDates', new CaseManagementAction()],
    ['confirmReviewDatesAdded', new CaseManagementAction()],
    ['selectManageParty', new CaseManagementAction()],
    ['addNewPartyAddress', new CaseManagementAction()],
    ['addNewParty', new CaseManagementAction()],
    ['confirmAddParty', new CaseManagementAction()],
    ['addAHearing', new CaseManagementAction()],
    ['confirmAddHearing', new CaseManagementAction()],
    ['errorValidationAddReviewDatesPage', new ErrorValidationAction()],
    ['errorValidationEnterGeneralAppPage', new ErrorValidationAction()],
    ['errorValidationHearingDatePage', new ErrorValidationAction()],
    ['errorValidationApplicationFeePage', new ErrorValidationAction()],
    ['errorValidationUploadADocumentPage', new ErrorValidationAction()],
    ['errorValidationApplicationConsentAndNotice', new ErrorValidationAction()],
    ['errorValidationManageHearing', new ErrorValidationAction()],
    ['errorValidationEnterAddAHearingPage', new ErrorValidationAction()],
    ['errorValidationCancelHearing', new ErrorValidationAction()],
    ['errorValidationUploadGenAppsFile', new ErrorValidationAction()],
    ['errorValidationEnterAddAHearingPage', new ErrorValidationAction()],
    ['inputErrorValidation', new CaseManagementAction()],
    ['reTryOnCallBackError', new RetryOnCallBackError()],
    ['getAddressInfo', new CaseManagementAction()],
    ['selectParty', new CaseManagementAction()],
    ['updatePartyDetails', new CaseManagementAction()],
    ['confirmPartyDetailsUpdated', new CaseManagementAction()],

  ]);

  static getAction(actionName: string): IAction {
    const action = this.actions.get(actionName);
    if (!action) {
      throw new Error(
        `Action '${actionName}' is not registered. Available actions: ${Array.from(this.actions.keys()).join(', ')}`
      );
    }
    return action;
  }

  static getAvailableActions(): string[] {
    return Array.from(this.actions.keys());
  }
}
