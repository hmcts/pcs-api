import { CollectedQAPair } from './cya-types';

/**
 * Data structure to store collected Q&A pairs for Final CYA validation
 */
export interface CYAData {
  collectedQAPairs?: CollectedQAPair[];
}

// Global instance to store CYA data during test execution
export let cyaData: CYAData = {};

// Function to reset CYA data (useful for test cleanup)
export function resetCYAData(): void {
  cyaData = {};
}

