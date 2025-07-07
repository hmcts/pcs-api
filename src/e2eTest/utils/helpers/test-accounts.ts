
import * as fs from 'fs';
import * as path from 'path';
import {permanentUsers} from "../data/permanent-users";

export interface UserCredentials {
  email: string;
  password: string;
  temp?: boolean;
  roles: string[];
}

const storePath = path.resolve(__dirname, '../.temp-users.json');

// Holds temp users in memory
let tempUsers: Record<string, UserCredentials> = {};

// Load temp users at startup
if (fs.existsSync(storePath)) {
  const data = fs.readFileSync(storePath, 'utf-8');
  tempUsers = JSON.parse(data);
}

function saveTempUsers() {
  fs.writeFileSync(storePath, JSON.stringify(tempUsers, null, 2));
}

export function setTempUser(key: string, creds: UserCredentials) {
  tempUsers[key] = creds;
  saveTempUsers();
}

export function deleteTempUser(key: string) {
  delete tempUsers[key];
  saveTempUsers();
}

export function getUser(key: string): UserCredentials | undefined {
  return tempUsers[key] || permanentUsers[key];
}

export function getAllUsers(): Record<string, UserCredentials> {
  return { ...permanentUsers, ...tempUsers };
}
