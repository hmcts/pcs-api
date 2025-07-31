export const pageIdentifier =
    {
        title: 'Case Options',
        mainHeader: 'some header'
    };

export const caseOption = {
    jurisdiction:
        {
            posessions: 'Possessions'
        },
    caseType:
        {
            civilPosessions: 'Civil Possessions 394'
        },
    // caseType:
    //     {
    //         civilPosessions: process.env.CHANGE_ID
    //             ? `Civil Possessions ${process.env.CHANGE_ID}`
    //             : 'Civil Possessions'
    //     },
    event:
        {
            makeAPosessionClaim: 'Make a claim'
        },

}

export const errorMessages = {
    header: 'There is a problem',
    errorMessage: 'An address is required',
}
