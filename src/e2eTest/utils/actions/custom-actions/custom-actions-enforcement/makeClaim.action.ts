import { addressDetails } from "@data/page-data/addressDetails.page.data";
import { home } from "@data/page-data/home.page.data";
import { Page } from "@playwright/test";
import { performAction, performValidation } from "@utils/controller-enforcement";
import { actionData, actionRecord, IAction } from "@utils/interfaces/action.interface";
import {initializeExecutor} from "@utils/controller";

export class MakeClaimAction implements IAction {
    async execute(page: Page, action: string, fieldName?: actionData | actionRecord, value?: actionData | actionRecord): Promise<void> {
          
        initializeExecutor(page);
        const actionsMap = new Map<string, () => Promise<void>>([

        ]);
        const actionToPerform = actionsMap.get(action);
        if (!actionToPerform) throw new Error(`No action found for '${action}'`);
        await actionToPerform();
    }

    private async makeClaim() {
        await performAction('clickTab', home.createCaseTab);
        await performAction('selectJurisdictionCaseTypeEvent');
        await performAction('housingPossessionClaim');
        await performAction('selectAddress', {
            postcode: addressDetails.englandCourtAssignedPostcode,
            addressIndex: addressDetails.addressIndex
        });
        await performValidation('bannerAlert', 'Case #.* has been created.');
    }

}