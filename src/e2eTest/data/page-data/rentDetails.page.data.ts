export const rentDetails = (
  rentAmount?: string,
  unpaidRentAmountPerDay?: string) => {
  return {
    title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
    mainHeader: 'Rent details',
    continue: 'Continue',
    rentFrequency: 'fortnightly',
    amountPerDayInputLabel: 'Enter the amount per day that unpaid rent should be charged at',
    HowMuchRentLabel: 'How much is the rent?',
    rentFrequencyLabel: 'Enter frequency',
    rentAmount: rentAmount,
    unpaidRentAmountPerDay: unpaidRentAmountPerDay
  };
};
