import { exec } from 'child_process';

const createUserApi = '/Users/gautham/Documents/Repos/pcs-api/src/createAuthToken.sh';

exec(`chmod +x ${createUserApi} && ${createUserApi}`, (error, stdout, stderr) => {
  if (error) {
    console.error(`Error: ${error.message}`);
    return;
  }
  if (stderr) {
    console.error(`Stderr: ${stderr}`);
    return;
  }
  console.log(`Output: ${stdout}`);
});
