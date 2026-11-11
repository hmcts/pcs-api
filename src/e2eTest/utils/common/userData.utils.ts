export function getDefendantAddressByName(submitPayload: any, createPayload: any, fullName: string) {
  const defendant = [
    submitPayload.defendant1,
    ...(submitPayload.additionalDefendants ?? []).map((d: any) => d.value),
  ].find(d => `${d.firstName} ${d.lastName}` === fullName);

  const address =
    defendant?.addressKnown === 'YES'
      ? defendant.addressSameAsPossession === 'YES'
        ? createPayload.propertyAddress
        : defendant.correspondenceAddress
      : null;

  if (!address?.AddressLine1) {
    return 'Address unknown';
  }

  return [
    address.AddressLine1,
    address.AddressLine2,
    address.AddressLine3,
    address.PostTown,
    address.County,
    address.PostCode,
    address.Country,
  ]
    .filter(Boolean)
    .join('\n');
}

export function getDefendantAddress(
  submitPayload: any,
  createPayload: any,
  fullName: string,
) {
  const defendant = [
    submitPayload.defendant1,
    ...(submitPayload.additionalDefendants ?? []).map((d: any) => d.value),
  ].find(d => `${d.firstName} ${d.lastName}` === fullName);

  if (!defendant) {
    return createPayload.propertyAddress;
  }

  const address =
    defendant?.addressKnown === 'YES'
      ? defendant.addressSameAsPossession === 'YES'
        ? createPayload.propertyAddress
        : defendant.correspondenceAddress
      : null;

  return address?.AddressLine1 ? address : 'Address unknown';
}

export function generateRandomFirstAndLastNames(options: {
  countOfFirstNamesToGenerate?: number;
  countOfLastNamesToGenerate?: number;
  maxFirstNameLength?: number;
  maxLastNameLength?: number;
}): {
  firstNames: string[];
  lastNames: string[];
} {
  const firstNamePool = [
    'John', 'Peter', 'James', 'Michael', 'David',
    'Sarah', 'Emma', 'Olivia', 'Sophia', 'Grace'
  ];

  const lastNamePool = [
    'Smith', 'Jones', 'Brown', 'Taylor', 'Wilson',
    'Parker', 'Johnson', 'Evans', 'Thomas', 'White'
  ];

  const uniqueFirstNames = new Set<string>();
  const uniqueLastNames = new Set<string>();

  while (
    uniqueFirstNames.size < (options.countOfFirstNamesToGenerate ?? 0)
  ) {
    const name =
      firstNamePool[Math.floor(Math.random() * firstNamePool.length)];

    uniqueFirstNames.add(
      options.maxFirstNameLength
        ? name.slice(0, options.maxFirstNameLength)
        : name
    );
  }

  while (
    uniqueLastNames.size < (options.countOfLastNamesToGenerate ?? 0)
  ) {
    const name =
      lastNamePool[Math.floor(Math.random() * lastNamePool.length)];

    uniqueLastNames.add(
      options.maxLastNameLength
        ? name.slice(0, options.maxLastNameLength)
        : name
    );
  }

  return {
    firstNames: [...uniqueFirstNames],
    lastNames: [...uniqueLastNames],
  };
}