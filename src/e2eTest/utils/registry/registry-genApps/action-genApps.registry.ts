import { LoginAction, NavigateToUrlAction, CreateCaseAPIAction } from "@utils/actions/custom-actions";
import { GenAppsAction } from "@utils/actions/custom-actions/custom-actions-genApps/genApps.action";
import { RecordAnswers } from "@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action";
import { CheckAction, ClickButtonAction, ClickRadioButtonAction, ClickTabAction, InputTextAction, SelectAction, UploadFileAction } from "@utils/actions/element-actions";
import { ClickLinkAction } from "@utils/actions/element-actions/clickLink.action";
import { ClickSummaryAction } from "@utils/actions/element-actions/clickSummary.action";
import { IAction } from "@utils/interfaces";


export class ActionGenAppsRegistry {
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
    // ['fetchPINsAPI', new FetchPINsAndValidateAccessCodeAPIAction()],
    // ['validateAccessCodeAPI', new FetchPINsAndValidateAccessCodeAPIAction()],
    
    ['chooseAnApplication', new GenAppsAction()],
    ['confirmIfCourtHearingInNext14Days', new GenAppsAction()],
    ['doYouNeedHelpPayingFee', new GenAppsAction()],
    ['confirmYouHaveAppliedForFeeHelp', new GenAppsAction()],
    ['confirmOtherPartiesAgreed', new GenAppsAction()],
    ['reasonsApplicationShouldNotBeShared', new GenAppsAction()],
    ['inputErrorValidationGenApp', new GenAppsAction()],
    ['selectLanguageUsedToComplete', new GenAppsAction()],
    ['retrieveCYATableData', new GenAppsAction()],
    ['validateCYA', new GenAppsAction()],
    ['recordUserEntry', new RecordAnswers()],
    ['confirmOrderDoYouWant', new GenAppsAction()],
    
    ['confirmOrderDoYouWant', new GenAppsAction()],
    ['reviewCYA', new GenAppsAction()],
    ['selectStatementOfTruth', new GenAppsAction()],
    ['reviewAndUpdateCYA', new GenAppsAction()],
   
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
