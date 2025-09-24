import axios from "axios";
import * as readline from 'node:readline/promises';

const idamAccessToken = 'eyJ6aXAiOiJOT05FIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYiLCJraWQiOiJNVFE0TURBeE1qYzFPRFl6TnpFPSJ9.eyJzdWIiOiJwY3Mtc3lzdGVtLXVzZXJAbG9jYWxob3N0IiwiYXV0aF9sZXZlbCI6MCwiYXVkaXRUcmFja2luZ0lkIjoiYzc0YmFjMGUtYTAwOS00YzFkLTgyODEtYTE4OWY2NTE1ZTg0IiwiaXNzIjoiaHR0cDpcL1wvbG9jYWxob3N0OjUwNjIiLCJ0b2tlbk5hbWUiOiJhY2Nlc3NfdG9rZW4iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXV0aEdyYW50SWQiOiI1ZDUxOTI2NC04OTRmLTRkNmYtODU4OC04OGUxYjg3NjdmMzYiLCJhdWQiOiJzb21ldGVzdHNlcnZpY2UiLCJuYmYiOjE3NTg3MTMwMDEsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsImF1dGhfdGltZSI6MTc1ODcxMzAwMTYzMSwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSIsInJvbGVzIl0sInJlYWxtIjoiXC9obWN0cyIsImV4cCI6MTc1ODc0MTgwMSwiaWF0IjoxNzU4NzEzMDAxLCJleHBpcmVzX2luIjoyODgwMCwianRpIjoiMzdjZWQ5MGQtMGViYy00YTJhLTg4ZjUtN2U2NjFmMWMwZWU0In0.F2hLqmO45bqvBYJezznI_YW2D68ddHyUm5wAkeS46rBkAkFw8U8fiFaZ5kmcmhghS_sfEGg9c_L5_3KgFzPG6Mlc7asnYYP_h5An-vlcM-4_VWGzCEMQClqEdI5LSfYG7wWvqdS9QWZq0j-Ex8tsnVzK18M-Mz8jo5HXsyNfXMqgkMa5V8h73c4Ms_3YjUsdEZm5qTNrWp_09XX0AywVVv1KvQXCgYp2CtH3rTpPn-MvOhVcX9QzC71Ljqt-WmSqsdDjV8GkzHVELTW0ZxptK4WpIpbR030v2IMx4CF1SkhS64oonGJR357y9aScGPkc9iXWPeaN7xPmyjWJu7hUqQ';
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

