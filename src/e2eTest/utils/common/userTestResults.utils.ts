export type UserTestResult = {
  email: string;
  status: 'PASS' | 'FAIL';
  error?: string;
};

export function recordUserTestFailure(
  results: UserTestResult[],
  email: string,
  error: unknown
): void {
  results.push({
    email,
    status: 'FAIL',
    error: error instanceof Error ? error.message : String(error)
  });
  console.error(`❌ ${email} failed`, error);
}

export function logUserTestResults(summaryTitle: string, results: UserTestResult[]): void {
  console.log(`\n===== ${summaryTitle} =====`);
  results.forEach((result) => {
    console.log(`${result.status === 'PASS' ? '✅' : '❌'} ${result.email} - ${result.status}`);
  });
}

export function assertAllUsersPassed(results: UserTestResult[]): void {
  const failedUsers = results.filter((result) => result.status === 'FAIL');
  if (failedUsers.length === 0) {
    return;
  }

  console.log('\nFailed Users:');
  failedUsers.forEach((user) => console.log(`- ${user.email}: ${user.error}`));
  throw new Error(`${failedUsers.length} user(s) failed. See summary above.`);
}

export function logUserTestResultsAndAssert(summaryTitle: string, results: UserTestResult[]): void {
  logUserTestResults(summaryTitle, results);
  assertAllUsersPassed(results);
}
