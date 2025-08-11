import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {initializeExecutor, performAction, performActions, performValidation} from '@utils/controller';
import {borderPostcode} from "../data/page-data/borderPostcode.page.data";
import configData from "@config/test.config";
import {caseOptions} from "@data/page-data/createCase.page.data";
import {addressDetails} from "@data/page-data/addressDetails.page.data";

test.beforeEach(async ({page}, testInfo) => {
    initializeExecutor(page);
    await parentSuite('Case Creation');
    await performAction('navigateToUrl', configData.manageCasesBaseURL);
    await performAction('createUserAndLogin', ['caseworker-pcs', 'caseworker']);
    await performAction('clickButton', 'Create case');
    await selectJurisdictionCaseTypeEvent()
});

async function selectJurisdictionCaseTypeEvent() {
    await performActions('Case option selection'
        , ['select', 'Jurisdiction', caseOptions.jurisdiction.posessions]
        , ['select', 'Case type', caseOptions.caseType.civilPosessions]
        , ['select', 'Event', caseOptions.event.makeAPosessionClaim]);
    await performAction('clickButton', 'Start');
}

async function inputAddressDetails(postcode: string) {
    await performActions('Enter Address Manually'
        , ['inputText', 'Enter a UK postcode', postcode]
        , ['clickButton', 'Find address']
        , ['select', 'Select an address', addressDetails.propertyAddressSection.addressIndex]
        , ['inputText', 'Address Line 2', addressDetails.propertyAddressSection.addressLine2]
        , ['inputText', 'Address Line 3', addressDetails.propertyAddressSection.addressLine3]
        , ['inputText', 'County', addressDetails.propertyAddressSection.englandCounty]);
    await performAction('clickButton', 'Continue');
}

test.describe.skip('[verify cross border postcode page]  @Master @nightly', async () => {
    test('verify cross border postcode page redirection for england and wales', async ({page}) => {
            await inputAddressDetails(borderPostcode.enlandWalesPostcode)
            await performValidation('text', {
                "text": borderPostcode.borderPostcodeHeader,
                "elementType": "heading"
            });
        }
    );

    test('verify cross border postcode page for england and wales content', async () => {
            await inputAddressDetails(borderPostcode.enlandWalesPostcode)
            await performValidation('text', {
                "text": borderPostcode.borderPostcodeHeader,
                "elementType": "heading"
            });
            await performValidation('text', {
                "text": borderPostcode.englandWalesParagraphContent,
                "elementType": "paragraph"
            });
            await performValidation('text', {
                "text": borderPostcode.englandWalesInlineContent,
                "elementType": "inlineText"
            });
            await performValidation('text', {"text": borderPostcode.continueBtn, "elementType": "button"})
            await performValidation('text', {"text": borderPostcode.cancelLink, "elementType": "link"})
        }
    );


    test('verify cross border postcode page redirection for england and scotland', async () => {
            await inputAddressDetails(borderPostcode.enlandScotlandPostcode)
            await performValidation('text', {
                "text": borderPostcode.borderPostcodeHeader,
                "elementType": "heading"
            });
        }
    );

    test('verify cross border postcode page for england and scotland content', async () => {
        await inputAddressDetails(borderPostcode.enlandScotlandPostcode)
        await performValidation('text', {
            "text": borderPostcode.borderPostcodeHeader,
            "elementType": "heading"
        });
        await performValidation('text', {
            "text": borderPostcode.englandScotlandParagraphContent,
            "elementType": "paragraph"
        });
        await performValidation('text', {
            "text": borderPostcode.englandScotlandInlineContent,
            "elementType": "inlineText"
        });
        await performValidation('text', {"text": borderPostcode.continueBtn, "elementType": "button"})
        await performValidation('text', {"text": borderPostcode.cancelLink, "elementType": "link"})
    });
})

