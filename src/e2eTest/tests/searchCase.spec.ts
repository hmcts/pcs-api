import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import configData from "@config/test.config";
import {initIdamAuthToken, initServiceAuthToken,getUser} from '../utils/helpers/idam-helpers/idam.helper';
import caseDataJson from 'data/case.data.json';
import {
    initializeExecutor,
    performAction,
    performValidation
} from '@utils/controller';
import {caseData} from '@data/case.data';
import {getCaseInfo} from '@utils/actions/custom-actions/createCase.action';

test.beforeEach(async ({page}, testInfo) => {
    initializeExecutor(page);
    await parentSuite('Case Creation');
    await performAction('navigateToUrl', configData.manageCasesBaseURL);
    await performAction('login', 'exuiUser');
    const userCreds = getUser('exuiUser');
    await initIdamAuthToken(userCreds?.email ?? '', userCreds?.password ?? '');
    await initServiceAuthToken();
    await testInfo.attach('Page URL', {
        body: page.url(),
        contentType: 'text/plain',
    });
    createCaseWithAddress()
});

async function createCaseWithAddress() {
    await performAction('createCase', {
        data: caseDataJson.data,
    });
}

async function searchCase(caseNumber: string) {
    await performAction('select', 'Jurisdiction', caseData.jurisdiction),
        await performAction('select', 'Case type', caseData.caseType),
        await performAction('fill', 'Case Number', caseNumber),
        await performAction('click', 'Apply')
}

test.describe('Search case by case number @PR @Master @nightly', () => {
    test('Search for case via caselist', async ({}) => {
        await performAction('click', 'Case list');
        await searchCase(getCaseInfo().id);
        await performValidation(
            'visibility',
            'caseNumber',
            { visible: getCaseInfo().fid}
        );
    });
    test('Search for case via find case', async ({}) => {
        await performAction('click', 'Find case');
        await searchCase(getCaseInfo().id);
        await performValidation(
            'visibility',
            'caseNumber',
            { visible: getCaseInfo().fid}
        );
    });
});


