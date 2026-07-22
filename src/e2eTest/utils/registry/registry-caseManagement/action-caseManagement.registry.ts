import { LoginAction, NavigateToUrlAction, CreateCaseAPIAction } from "@utils/actions/custom-actions";
import { CaseManagementAction } from "@utils/actions/custom-actions/custom-actions-caseManagement";
import { ErrorValidationAction } from "@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementErrorValidation.action";
import { LinkSolicitorAPIAction } from "@utils/actions/custom-actions/linkSolicitorAPI.action";
import { CheckAction, ClickButtonAction, ClickRadioButtonAction, ClickTabAction, InputTextAction, SelectAction, UploadFileAction } from "@utils/actions/element-actions";
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
    ['navigateToSummaryPage', new CaseManagementAction()],
    ['selectAnEvent', new CaseManagementAction()],
    ['selectDocumentToAmend', new CaseManagementAction()],
    ['changeCaseState', new CaseManagementAction()],
    ['confirmCaseStateChange', new CaseManagementAction()],
    ['getAllPartyDetails', new CaseManagementAction()],
    ['enterApplicationDetails', new CaseManagementAction()],
    ['confirmIfCourtHearingInNext14Days', new CaseManagementAction()],
    ['errorValidationSelectDocumentPage', new ErrorValidationAction()],
    ['errorValidationChangeCaseStatePage', new ErrorValidationAction()],
    ['errorValidationEnterGeneralAppPage', new ErrorValidationAction()],
    ['errorValidationHearingDatePage', new ErrorValidationAction()],
    ['inputErrorValidation', new CaseManagementAction()],
    ['reTryOnCallBackError', new RetryOnCallBackError()],
    
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
