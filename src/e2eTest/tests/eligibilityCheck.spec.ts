import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {initializeExecutor, performAction, performActions, performValidation} from '@utils/controller';
import {borderPostcodePageData} from "../data/page-data/borderPostcode.page.data";
import configData from "@config/test.config";
import {caseOption} from "@data/page-data/caseOptions.data";
import {addressDetails} from "@data/page-data/addressDetails.data";

test.beforeEach(async ({page}, testInfo) => {
    initializeExecutor(page);
    await parentSuite('Case Creation');
    await performAction('navigateToUrl', configData.manageCasesBaseURL);
    await performAction('login', 'exuiUser');
    await performAction('clickButton', 'Create case');
    await selectJurisdictionCaseTypeEvent()
});

async function selectJurisdictionCaseTypeEvent() {
    await performActions('Case option selection'
        , ['select', 'Jurisdiction', caseOption.jurisdiction.posessions]
        , ['select', 'Case type', caseOption.caseType.civilPosessions]
        , ['select', 'Event', caseOption.event.makeAPosessionClaim]);
    await performAction('clickButton', 'Start');
}

async function inputAddressDetails(postcode: string) {
    await performActions('Find Address based on postcode'
        , ['inputText', 'Enter a UK postcode', postcode]
        , ['clickButton', 'Find address']
        , ['select', 'Select an address', addressDetails.addressIndex]
        , ['inputText', 'Address Line 2', addressDetails.addressLine2]
        , ['inputText', 'Address Line 3', addressDetails.addressLine3]
        , ['inputText', 'County', addressDetails.englandCounty]);
    await performAction('clickButton', 'Continue');
}

test.describe('[verify cross border postcode page for england and wales]  @Master @nightly', async () => {
    test('verify cross border postcode page redirection for england and wales', async ({page}) => {
            await inputAddressDetails(borderPostcodePageData.enlandWalesPostcode)
            await performValidation('text', {
                "text": borderPostcodePageData.borderPostcodeHeader,
                "elementType": "heading"
            });
        }
    );

    test('verify cross border postcode page for england and wales content', async () => {
            await inputAddressDetails(borderPostcodePageData.enlandWalesPostcode)
            await performValidation('text', {
                "text": borderPostcodePageData.borderPostcodeHeader,
                "elementType": "heading"
            });
            await performValidation('text', {
                "text": borderPostcodePageData.englandWalesParagraphContent,
                "elementType": "paragraph"
            });
            await performValidation('text', {
                "text": borderPostcodePageData.englandWalesInlineContent,
                "elementType": "inlineText"
            });
            await performValidation('text', {"text": borderPostcodePageData.continueBtn, "elementType": "button"})
            await performValidation('text', {"text": borderPostcodePageData.cancelLink, "elementType": "link"})
        }
    );
});

test.describe('[verify cross border postcode page for england and scotland]  @Master @nightly', async () => {
    test('verify cross border postcode page redirection for england and scotland', async () => {
            await inputAddressDetails(borderPostcodePageData.enlandScotlandPostcode)
            await performValidation('text', {
                "text": borderPostcodePageData.borderPostcodeHeader,
                "elementType": "heading"
            });
        }
    );

    test('verify cross border postcode page for england and scotland content', async () => {
            await inputAddressDetails(borderPostcodePageData.enlandScotlandPostcode)
            await performValidation('text', {
                "text": borderPostcodePageData.borderPostcodeHeader,
                "elementType": "heading"
            });
            await performValidation('text', {
                "text": borderPostcodePageData.englandScotlandParagraphContent,
                "elementType": "paragraph"
            });
            await performValidation('text', {
                "text": borderPostcodePageData.englandScotlandInlineContent,
                "elementType": "inlineText"
            });
            await performValidation('text', {"text": borderPostcodePageData.continueBtn, "elementType": "button"})
            await performValidation('text', {"text": borderPostcodePageData.cancelLink, "elementType": "link"})
        }
    );
});

