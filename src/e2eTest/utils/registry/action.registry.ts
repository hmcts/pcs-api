import {IAction} from '../interfaces/action.interface';
import {ClickTabAction} from '../actions/element-actions/clickTab.action';
import {InputTextAction} from '../actions/element-actions/inputText.action';
import {CheckAction} from '../actions/element-actions/check.action';
import {SelectAction} from '../actions/element-actions/select.action';
import {LoginAction} from "../actions/custom-actions/login.action";
import {NavigateToUrl} from "@utils/actions/custom-actions/navigateToUrl.action";
import {CreateCaseAction} from "@utils/actions/custom-actions/createCase.action";
import {ClickButtonAction} from "../actions/element-actions/clickButton.action";
import {ClickRadioButton} from "../actions/element-actions/clickRadioButton.action";
import {UploadFileAction} from "@utils/actions/element-actions/uploadFile.action";

export class ActionRegistry {
  private static actions: Map<string, IAction> = new Map([
    ['clickButton', new ClickButtonAction()],
    ['clickTab', new ClickTabAction()],
    ['inputText', new InputTextAction()],
    ['check', new CheckAction()],
    ['select', new SelectAction()],
    ['createUserAndLogin', new LoginAction()],
    ['login', new LoginAction()],
    ['navigateToUrl', new NavigateToUrl()],
    ['clickRadioButton', new ClickRadioButton()],
    ['uploadFile', new UploadFileAction()],
    ['selectAddress', new CreateCaseAction()],
    ['extractCaseIdFromAlert', new CreateCaseAction()],
    ['selectResumeClaimOption', new CreateCaseAction()],
    ['selectClaimantType', new CreateCaseAction()],
    ['defendantDetails', new CreateCaseAction()],
    ['selectRentArrearsPossessionGround', new CreateCaseAction()],
    ['selectJurisdictionCaseTypeEvent', new CreateCaseAction()],
    ['enterTestAddressManually', new CreateCaseAction()],
    ['createCase', new CreateCaseAction()],
    ['selectClaimType', new CreateCaseAction()],
    ['selectClaimantName', new CreateCaseAction()],
    ['selectContactPreferences', new CreateCaseAction()],
    ['housingPossessionClaim', new CreateCaseAction()],
    ['selectGroundsForPossession', new CreateCaseAction()],
    ['selectYourPossessionGrounds', new CreateCaseAction()],
    ['enterReasonForPossession', new CreateCaseAction()],
    ['selectPreActionProtocol', new CreateCaseAction()],
    ['selectMediationAndSettlement', new CreateCaseAction()],
    ['selectNoticeOfYourIntention', new CreateCaseAction()],
    ['selectCountryRadioButton', new CreateCaseAction()],
    ['selectOtherGrounds', new CreateCaseAction()],
    ['selectTenancyOrLicenceDetails', new CreateCaseAction()],
    ['reloginAndFindTheCase', new CreateCaseAction()],
    ['provideRentDetails', new CreateCaseAction()],
    ['selectDailyRentAmount', new CreateCaseAction()]
  ]);

  static getAction(actionName: string): IAction {
    const action = this.actions.get(actionName);
    if (!action) {
      throw new Error(`Action '${actionName}' is not registered. Available actions: ${Array.from(this.actions.keys()).join(', ')}`);
    }
    return action;
  }

  static getAvailableActions(): string[] {
    return Array.from(this.actions.keys());
  }
}
