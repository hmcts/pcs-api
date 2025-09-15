import axios from "axios";
import * as readline from 'node:readline/promises';

const idamAccessToken = 'eyJ6aXAiOiJOT05FIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYiLCJraWQiOiJPRFF3TnpFMk16QTNNVEExTURjPSJ9.eyJzdWIiOiJwY3Mtc3lzdGVtLXVzZXJAbG9jYWxob3N0IiwiYXV0aF9sZXZlbCI6MCwiYXVkaXRUcmFja2luZ0lkIjoiNmY1OGY2ZGMtMzZlMS00OTBiLTliZDQtMDQzMGUzY2U2NjkwIiwiaXNzIjoiaHR0cDpcL1wvbG9jYWxob3N0OjUwNjIiLCJ0b2tlbk5hbWUiOiJhY2Nlc3NfdG9rZW4iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXV0aEdyYW50SWQiOiJkZDQ3NDUzOC01YTM1LTQwM2QtYTRkZS01MjhiZWZhMGZkNWYiLCJhdWQiOiJzb21ldGVzdHNlcnZpY2UiLCJuYmYiOjE3NTc2OTQwMzksImdyYW50X3R5cGUiOiJwYXNzd29yZCIsImF1dGhfdGltZSI6MTc1NzY5NDAzOTEzMSwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSIsInJvbGVzIl0sInJlYWxtIjoiXC9obWN0cyIsImV4cCI6MTc1NzcyMjgzOSwiaWF0IjoxNzU3Njk0MDM5LCJleHBpcmVzX2luIjoyODgwMCwianRpIjoiZWM5NTU4OTktNmE2Zi00YzcyLWJiYTQtZDFkNGI3YmEwMTcxIn0.PtNeE4y98KI6RqezqNyozBVYCyHf3eeyHukL8ZMq-YvkDVcZmpSPWa-gfsSh4lupWCvpfNI6Fz_PI9taDpLd0hKSM3oVvCicFY5a-raiYGiBWgRtNmIPpC5waGgHxYJGefvnQlLJsFhNSab3SBM-tArSCQxEzCF1s5_UZ1soPmMUrBlXjnOM78mi4QFjcg0HiLNsxfymgJObfqJ3mecE_ECTwLWkcfT3c8OiTDNvtHMpg8tK4Z4G28AQ_lIKARAd-OXd3Om6rWP7OFoN99AZmmb7zAOmc2-D43Bd6iAp6J-PHy4x7a0-f6MkD99DTg3IFh8n7omucqro5Ta5oeGiUQ';
const s2sToken = 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZGF0YSIsImV4cCI6MTc1NzE3Mzk1MH0.Oo5mdmJBpQjyAImuMwyDUKzsq2If1ir4dnLHwgLUxFo2UE-BaNlGgtIW-jPpxCxEYBXDK_kP-xmK-fmxkhtBuQ';

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
});

async function getLinkEventToken(caseReference) {
  return axios.get(`http://localhost:4452/cases/${caseReference}/event-triggers/linkDefendant`, {
    headers: {
      'Accept': 'application/vnd.uk.gov.hmcts.ccd-data-store-api.start-event-trigger.v2+json;charset=UTF-8',
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${idamAccessToken}`,
      'ServiceAuthorization': `Bearer ${s2sToken}`
    }
  })
  .then(response => response.data.token)
  .catch(error => {
    throw new Error(`Request failed with status ${error.status}: ${error.response.data.message}`, error);
  })
}

async function fireLinkEvent(eventToken, caseReference, linkCode, linkUserId) {
  return axios.post(`http://localhost:4452/cases/${caseReference}/events`, {
    "data": {
      "linkCode": linkCode,
      'linkUserId': linkUserId
    },
    "event": {
      "description": "Link defendant to a claim",
      "id": "linkDefendant",
      "summary": "Link defendant to a claim"
    },
    "event_token": eventToken,
    "ignore_warning": false
  },{
    headers: {
      'Accept': 'application/vnd.uk.gov.hmcts.ccd-data-store-api.create-event.v2+json;charset=UTF-8',
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${idamAccessToken}`,
        'ServiceAuthorization': `Bearer ${s2sToken}`,
        'experimental': 'experimental'
    }
  })
    .then(response => response.status)
    .catch(error => {
      throw new Error(`Request failed with status ${error.status}: ${error.response.data.message}`, error);
    })
}


// const caseReference = 1757845582285988;
// const accessCode = '2TLAXK3SDHN6';
const userId = 'ee42bcc5-e35e-3542-8894-a8742b175376';
// const userId = '924b6cb4-1ca6-3b53-9e1e-a274c6ff750a';

console.clear();
console.log('\nGet access to a possessions case\n=================================\n\n');

const claimNumber = await rl.question('What is the claim number? : ');
const caseReference = claimNumber.replaceAll('-', '');

const accessCode = await rl.question('What is the security code? : ');


let eventToken;
try {
  eventToken = await getLinkEventToken(caseReference);
} catch (error) {
  console.log(error.message)
  process.exit(1);
}

await fireLinkEvent(eventToken, caseReference, accessCode, userId)


rl.close();

console.log('\nYou have been granted access to the claim details\n')

console.log(`View the claim at http://localhost:3000/cases/case-details/${caseReference}\n`);

