
import { expect, Page } from '@playwright/test';
import { IAction, actionData, actionRecord } from '@utils/interfaces';
import { createCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { performAction, performValidation } from '@utils/controller-caseManagement';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary, home } from '@data/page-data';
import {generateRandomString} from "@utils/common/string.utils";
import {performActions} from "@utils/controller";
import {
  CaseManagementCommonUtils
} from "@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action";
import {
  addReviewDates,
  confirmReviewDatesAdded,
  selectDocument,
  changeCaseState,
  confirmCaseStateChange
} from '@data/page-data-figma/page-data-caseManagement-figma';
import { caseInfo } from '../createCaseAPI.action';


export const addressInfo = {
  buildingStreet: createCaseApiData.createCasePayload.propertyAddress.AddressLine1,
  addressLine2: createCaseApiData.createCasePayload.propertyAddress.AddressLine2,
  townCity: createCaseApiData.createCasePayload.propertyAddress.PostTown,
  engOrWalPostcode: createCaseApiData.createCasePayload.propertyAddress.PostCode
};
const cyaMap = new Map<string, string>();
export let defendantDetails: string[] = [];

export class CaseManagementAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['navigateToSummaryPage', () => this.navigateToSummaryPage(page)],
      ['selectAnEvent', () => this.selectAnEvent(fieldName as actionRecord)],
      ['selectDocumentToAmend', () => this.selectDocumentToAmend(fieldName as actionRecord)],
      ['addReviewDates', () => this.addReviewDates(fieldName as actionRecord)],
      ['confirmReviewDatesAdded', () => this.confirmReviewDatesAdded()],
      ['changeCaseState', () => this.changeCaseState(fieldName as actionRecord)],
      ['confirmCaseStateChange', () => this.confirmCaseStateChange()],
      ['inputErrorValidation', () => this.inputErrorValidation(page, fieldName as actionRecord)],

    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }
    await actionToPerform();
  }

  private async navigateToSummaryPage(page: Page) {
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    await expect(async () => {
      await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`, { waitUntil: 'domcontentloaded' });
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });
    await page.waitForLoadState();
    await page.locator('.spinner-container').waitFor({ state: 'detached' });
    await performValidation('mainHeader', home.caseSummary);
  }

  private async selectAnEvent(event: actionRecord) {
    await performAction('select', caseSummary.nextStepEventList, event.eventType);
    await performAction('clickButton', caseSummary.go);
  }

  private async selectDocumentToAmend(selectDoc: actionRecord){
    await performAction('select', selectDoc.question, selectDoc.option);
    await performAction('clickRadioButton', { question: selectDoc.question1, option: selectDoc.option1 });
    await performAction('reTryOnCallBackError', selectDocument.continueButton, selectDoc.nextPage as string);
  }

  private async changeCaseState(caseState: actionRecord){
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('select', caseState.question, caseState.option);
    await performAction('reTryOnCallBackError', changeCaseState.continueButton, caseState.nextPage as string);
  }

  private async confirmCaseStateChange(): Promise<void> {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', { elementType: 'inlineText', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'inlineText',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('mainHeader', confirmCaseStateChange.mainHeader);
    await performAction('clickButton', confirmCaseStateChange.closeAndReturnToCaseOverviewButton);
  }

  private async addReviewDates(reviewDateData: actionRecord){
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    const userInput =
      typeof reviewDateData.input === 'number'
        ? generateRandomString(reviewDateData.input)
        : (reviewDateData.input as string);
    let date = CaseManagementCommonUtils.getRandomDate(reviewDateData.dateType as string);
    await performActions('Date of Review',
      ['inputText', reviewDateData.day, date.split('/')[0]],
      ['inputText', reviewDateData.month, date.split('/')[1]],
      ['inputText', reviewDateData.year, date.split('/')[2]]);
    await performAction('clickRadioButton', { question: reviewDateData.question, option: reviewDateData.option });
    await performAction('inputText', reviewDateData.label, reviewDateData.userInput);
    await performAction('reTryOnCallBackError', addReviewDates.continueButton, reviewDateData.nextPage as string);
  }

  private async confirmReviewDatesAdded(): Promise<void> {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', { elementType: 'inlineText', text: confirmReviewDatesAdded.reviewDatesAdded});
    await performValidation('text', { elementType: 'inlineText', text: 'Case number #' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'inlineText',
      text: `${addressInfo.buildingStreet}, ${addressInfo.addressLine2}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('mainHeader', confirmReviewDatesAdded.mainHeader);
    await performAction('clickButton', confirmReviewDatesAdded.closeAndReturnToCaseOverviewButton);
  }


  private async inputErrorValidation(page: Page, validationArr: actionRecord) {
        if (Array.isArray(validationArr.inputArray)) {
          for (const item of validationArr.inputArray) {

            switch (validationArr.validationType) {

                case 'radioOptions':
                await performAction('clickButton', validationArr.button);
                await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errInlineMessage);
                await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
                await performAction('clickRadioButton', {question: validationArr.question, option: validationArr.option});
                break;

              case 'checkBox':
                await performAction('clickButton', validationArr.button);
                await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errMessage);
                await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
                await performAction('check', validationArr.checkBox);
                break;

              case 'checkBoxPageLevel':
                await performAction('clickButton', validationArr.button);
                await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
                await performAction('check', validationArr.checkBox);
                break;

              case 'dropDown':
                await performAction('clickButton', validationArr.button);
                await expect(async () => {
                  await performAction('clickButton', validationArr.button);
                  await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
                }).toPass({
                  timeout: VERY_LONG_TIMEOUT,
                });
                await performAction('select', validationArr.dropQn, validationArr.option);
                break;

              case 'textField':
                await performAction('inputText', validationArr.label, CaseManagementCommonUtils.generateRandomString(item.input));
                await expect(async () => {
                  await performAction('clickButton', validationArr.button);
                  if (item.type === 'moreThanMax') {
                    await performValidation('errorMessage', { header: validationArr.header, message: item.errMessage });
                  } else {
                    await performValidation('inputError', validationArr.label, item.errMessage);
                    await performValidation('errorMessage', validationArr.label, item.errMessage);
                  }
                }).toPass({
                  timeout: VERY_LONG_TIMEOUT,
                });
                break;

              case 'dateField':
                let date: string = CaseManagementCommonUtils.getRandomDate(item.type as string);
                const enterDate = () =>
                  performActions(
                    'Enter Date',
                    ['inputText', validationArr.label1, date.split('/')[0]],
                    ['inputText', validationArr.label2, date.split('/')[1]],
                    ['inputText', validationArr.label3, date.split('/')[2]]
                  );

                if (item.type === 'empty') {
                  await performAction('clickButton', validationArr.button);
                  await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errInlineMessage);
                  await performValidation('errorMessage', validationArr.header1, item.errMessage);
                } else if (item.type === 'past') {
                  await enterDate();
                } else if (item.type === 'invalid') {
                  await enterDate();
                  await performAction('clickButton', validationArr.button);
                  await performValidation('errorMessage', validationArr.header1, item.errMessage);
                }
                else {
                  await enterDate();
                  await performAction('clickButton', validationArr.button);
                  await performValidation('errorMessage', { header: validationArr.header, message: item.errMessage });
                }
                break;

              case 'dateRadioOption' :
                let dateOfReview: string = CaseManagementCommonUtils.getRandomDate(item.type as string);
                const enterDateOfReview = () =>
                  performActions(
                    'Enter Date',
                    ['inputText', validationArr.label1, dateOfReview.split('/')[0]],
                    ['inputText', validationArr.label2, dateOfReview.split('/')[1]],
                    ['inputText', validationArr.label3, dateOfReview.split('/')[2]]
                  );
                await enterDateOfReview();
                await performAction('clickRadioButton', {
                  question: validationArr.question,
                  option: validationArr.option
                });
                await performAction('inputText', validationArr.label, generateRandomString(Number(item.input)));
                await performAction('clickButton', validationArr.button);
                await performValidation('errorMessage', { header: validationArr.header, message: item.errMessage });
                break;

              default:
                throw new Error(`Validation type :"${validationArr.validationType}" is not valid`);
            }
          }
        }
      if (validationArr.buttonRemove) {
        await performAction('removeFile');
        await page.waitForTimeout(6000);
      }
    }
}
