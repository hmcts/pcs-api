import { CollectedQAPair } from './cya-types';

/**
 * Data structure to store collected Q&A pairs for Address CYA validation
 */
export interface CYAAddressData {
  collectedQAPairs?: CollectedQAPair[];
}

export let cyaAddressData: CYAAddressData = {};

export function resetCYAAddressData(): void {
  cyaAddressData = {};
}
