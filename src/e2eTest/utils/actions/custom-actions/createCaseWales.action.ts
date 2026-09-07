import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {addressInfo, caseNumber, CreateCaseAction} from "@utils/actions/custom-actions/createCase.action";
import {
  // migration (page-data → page-data-figma)
  contactPreferences,
  documentsYouVeUploadedCheckListWales,
  exemptLandlord,
  occupationLicenceDetailsWales,
  prohibitedConductWales,
} from '@data/page-data-figma';
import {asbQuestionsWales} from '@data/page-data/asbQuestionsWales.page.data';
import {readPageHeading} from '@utils/common/locator.utils';

export class CreateCaseWalesAction extends CreateCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectClaimantDetails', () => this.selectClaimantDetails(fieldName as actionRecord)],
      ['selectProhibitedConductStandardContract', () => this.selectProhibitedConductStandardContract(fieldName as actionRecord)],
      ['selectOccupationContractOrLicenceDetails', () => this.selectOccupationContractOrLicenceDetails(page, fieldName as actionRecord)],
      ['selectAsb', () => this.selectAsb(fieldName as actionRecord)],
      ['requiredDocumentsUpload', () => this.requiredDocumentsUpload(fieldName as actionRecord)],
      ['selectDocumentsYouVeUploadedCheckList', () => this.selectDocumentsYouVeUploadedCheckList(fieldName as actionRecord)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectClaimantDetails(claimant: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: claimant.question1, option: claimant.option1});
    await performAction('clickButtonAndVerifyPageNavigation', exemptLandlord.continueButton, contactPreferences.mainHeader);    
  }

  private async selectOccupationContractOrLicenceDetails(page: Page, occupationContractData: actionRecord) {
    // Gate on this page's own heading before touching anything on it.
    //
    // The two validations below look like page checks but are not: 'Case number:' and
    // 'Property address:' are a shared header rendered on every page of the journey — the
    // same pair is asserted in 78 places across the createCase actions — so they pass just as
    // happily on the *previous* page. Nothing here established that the occupation-contract
    // page had actually arrived.
    //
    // That is the failure on createCaseWales:604 after the radio-pattern fix let the test get
    // this far: clickRadioButton reported all four patterns at 0, and pattern3 is
    // question-independent (`label >> text=<option>`), so a count of 0 there means no label
    // with that option text existed anywhere on the page — not a selector problem, a
    // wrong-or-unrendered page. Only 1 of this action's 7 call sites validated the heading
    // first, and the failing one at spec:639 was not it, so the gate belongs here rather than
    // in each caller.
    //
    // Reports the heading actually on screen, plus any error summary, before asserting.
    //
    // performValidation('mainHeader', x) alone is not enough to diagnose this: it goes through
    // pageHeading(page, x), which *filters by* the expected text, so on a mismatch the locator
    // resolves to nothing and the failure reads 'element(s) not found' with no indication of
    // where the journey actually is. That is what the first run of this gate produced.
    //
    // Narrowing so far: of this action's 7 call sites only spec:639 fails, and it is the only
    // one passing addAdditionalDefendantsOption: yes with numberOfDefendants: 2 — the other six
    // pass 'no'. The England equivalent (createCase.spec.ts:96) has the identical structure with
    // 2 extra defendants and passes, so this is specific to the Wales multi-defendant path.
    // The likely mechanism is that addDefendantDetails' final Continue does not advance —
    // clickButton does not verify navigation, so a validation error on the defendant page would
    // leave the journey there while the test carries on. This logging confirms or kills that.
    const actualHeading = await readPageHeading(page);
    if (actualHeading !== occupationLicenceDetailsWales.mainHeader) {
      const errorSummary = await page.locator('.error-summary, #error-summary-title, .govuk-error-summary')
        .first()
        .innerText()
        .catch(() => '<none>');
      console.warn(
        `[occupationContract] expected heading "${occupationLicenceDetailsWales.mainHeader}" `
        + `but page shows "${actualHeading}"; error summary: ${errorSummary.replace(/\s+/g, ' ').trim()}`
      );
    }
    await performValidation('mainHeader', occupationLicenceDetailsWales.mainHeader);
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: occupationContractData.occupationContractQuestion,
      option: occupationContractData.occupationContractType
    });
    if (occupationContractData.occupationContractType === occupationLicenceDetailsWales.otherRadioOption) {
      await performAction('inputText', occupationLicenceDetailsWales.GiveDetailsAboutHiddenTextLabel, occupationLicenceDetailsWales.GiveDetailsAboutHiddenTextInput);
    }
    if (occupationContractData.day && occupationContractData.month && occupationContractData.year) {
      await performActions(
        'Enter Date',
        ['inputText', occupationLicenceDetailsWales.dayTextLabel, occupationContractData.day],
        ['inputText', occupationLicenceDetailsWales.monthTextLabel, occupationContractData.month],
        ['inputText', occupationLicenceDetailsWales.yearTextLabel, occupationContractData.year]);
    }
    if (occupationContractData.files) {
      await performAction('uploadFile', occupationContractData.files);
    }
    await performAction('clickButton', occupationLicenceDetailsWales.continueButton);
  }

  private async selectProhibitedConductStandardContract(prohibitedConduct: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: prohibitedConduct.question1, option: prohibitedConduct.option1});
    if (prohibitedConduct.option1 == prohibitedConductWales.yesRadioOption) {
      await performAction('inputText', prohibitedConduct.label1, prohibitedConduct.input1);
      await performAction('clickRadioButton', {question: prohibitedConduct.question2, option: prohibitedConduct.option2});
      if (prohibitedConduct.option2 == prohibitedConductWales.yesRadioOption) {
        await performAction('inputText', prohibitedConduct.label2, prohibitedConduct.input2);
      }
    }
    await performAction('clickButton', prohibitedConductWales.continueButton);
  }

  private async selectAsb(asbQuestions: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: asbQuestionsWales.isThereActualOrThreatenedAsbQuestion,
      option: asbQuestions.asbChoice
    });
    if (asbQuestions.asbChoice == asbQuestionsWales.yesRadioOption) {
      await performAction('inputText', asbQuestionsWales.giveDetailsOfAsbHiddenTextLabel, asbQuestions.giveDetailsOfAsb);
    }
    await performAction('clickRadioButton', {
      question: asbQuestionsWales.isThereActualIllegalPurposesQuestion,
      option: asbQuestions.illegalPurposesChoice
    });
    if (asbQuestions.illegalPurposesChoice === asbQuestionsWales.yesRadioOption) {
      await performAction('inputText', asbQuestionsWales.giveDetailsOfIllegalHiddenTextLabel, asbQuestions.giveDetailsOfIllegal);
    }
    await performAction('clickRadioButton', {
      question: asbQuestionsWales.hasThereBeenOtherProhibitedQuestion,
      option: asbQuestions.prohibitedConductChoice
    });
    if (asbQuestions.prohibitedConductChoice === asbQuestionsWales.yesRadioOption) {
      await performAction('inputText', asbQuestionsWales.giveDetailsOfTheOtherHiddenTextLabel, asbQuestions.giveDetailsOfTheOther);
    }
    await performAction('clickButton', asbQuestionsWales.continueButton);
  }

  private async requiredDocumentsUpload(reqDocs: actionRecord){
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseNumber });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: reqDocs.question,
      option: reqDocs.option
    });
    if (reqDocs.option === 'Yes') {
      await performAction('uploadFile', reqDocs.file);
    } else {
      await performAction('inputText', reqDocs.label, reqDocs.input);
    }   

  }

  private async selectDocumentsYouVeUploadedCheckList(documents: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseNumber });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });

    if (Array.isArray(documents.uploadedDocuments)) {
      for (const document of documents.uploadedDocuments) {
        await performAction('check', { label: document });
      }
    } else {
      throw new Error('uploadedDocuments must be an array');
    }

    await performAction('clickButton', documentsYouVeUploadedCheckListWales.continueButton);
  }
}
