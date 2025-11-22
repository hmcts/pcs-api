---
config:
  layout: elk
---
flowchart LR
    A0["Sign in or create an account"] -- Enter credentials --> A1["Case list"]
    A1 --> A2["Create Case"]
    A2 -- Start --> A3["Make a claim"]
    A3 -- Continue --> A4@{ label: "What is the address of the property you're claiming possession" }
    A4 -- Case number created --> A5["Provide more details about your claim - h2"]
    A5 -- Continue --> A7{"Claimant type"}
    A7 -- Registered social provider --> A9["Claim type"]
    A7 -- Non registered social provider --> A8@{ label: "You're not eligible for this online service" }
    A9 -- "Yes - Claimant Trespassed" --> A8
    A9 -- No --> A10["Claimant name"]
    A10 -- Yes --> A11["Contact preferences"]
    A10 -- "No-Enter correct claimant name" --> A11
    A11 -- Notifications & Correspondence address : Select 'Yes' or 'No' options on the mandatory sections without selecting Contact phone number (optional) --> A12["Defendant 1 details"]
    A11 -- Contact phone number : Select 'Yes' & enter phone number --> A12
    A11 -- Contact phone number : Select 'No' --> A12
    A12 -- "Defendant's name - Yes: Enter Fn & Ln" --> A13["Tenancy or licence details"]
    A12 -- "Defendant's name - No" --> A13
    A12 -- "Defendant's correspondence address: The defendant's correspondence address? - Yes: Is the defendant's correspondence address same as the address of the property you're claiming possession of? - No: Enter address details" --> A13
    A12 -- "Defendant's correspondence address  :The defendant's correspondence address? -Yes. Is the defendant's correspondence address same as the address of the property you're claiming possession of? -Yes" --> A13
    A12 -- "Defendant's correspondence address :Do you know the defendant's correspondence address?-No" --> A13
    A12 -- "Defendant's email address - Yes : Enter email address" --> A13
    A12 -- "Defendant's email address - No" --> A13
    A12 --> A13
    A13 -- Tenancy or license type --> A14{"Assured tenancy"}
    A14 -- Continue --> A15["Grounds for possession"]
    A15 -- Yes --> A16["Grounds for possession-1"]
    A16 --> A17{"Serious rent arrears (ground 8)"} & A18{"Rent arrears (ground 10)"} & A19{"Persistent delay in paying rent(ground 11)"}
    A17 -- Yes --> A20["What are your grounds for possession?"]
    A20 -- "Auto selected - Serious rent arrears (ground 8), continue" --> A21["Pre-action protocol"]
    A17 -- No --> A21
    A18 -- Yes --> A20
    A18 -- No --> A21
    A20 -- "Auto selected - Rent arrears (ground 10), continue" --> A21
    A19 -- Yes --> A20
    A19 -- No --> A21
    A20 -- "Auto selected - Persistent delay in paying rent (ground 11), continue" --> A21
    A20 -- "Select grounds - Ground 8, Ground 10, Ground 11" --> A21
    A15 -- No --> A20
    A20 -- Select grounds (ground 1, ground 4, ground 9) other than ground 8, 10, 11 --> A22["Reasons for possession"]
    A20 -- Select 'ground 3' along with grounds 8,10, 11 --> A22
    A22 -- Enter the Reasons for ground 1, 3, 4 & 9 ), continue --> A21
    A21 -- Yes --> A23["Mediation and settlement"] & A23 & A23
    A21 -- No --> A23
    A22 -- Textboxes displayed only for grounds other than 8, 10, 11 --> A21
    A23 -- "Have you attempted mediation with the defendants?- Yes" --> A24["Notice of your intention to begin possession proceedings"]
    A23 -- "Have you tried to reach a settlement with the defendants? - Yes" --> A24
    A23 -- "Have you attempted mediation with the defendants?- No" --> A24 & A24 & A24
    A23 -- "Have you tried to reach a settlement with the defendants? - No" --> A24 & A24 & A24
    A24 -- Yes --> A25["Notice details"]
    A24 -- No --> A26["Rent details"] & A26 & A26
    A25 -- "How did you serve the notice? - Any option" --> A26
    A26 -- "How much is the rent? - Enter rent amount = For e.g £100" --> A27["Daily rent amount"] & A27 & A27
    A26 -- "How frequently should rent be paid? - Weekly/Fortnightly/Monthly/Other-select monthly" --> A27 & A27 & A27
    A27 -- Yes --> A28["Money judgment"] & A28 & A28
    A27 -- No : 'Enter amount per day that unpaid rent should be charged at' --> A28
    A28 -- Yes --> A29["Claimant circumstances"]
    A28 -- No --> A29 & A29 & A29
    A29 -- Yes: Give details about test's circumstances --> A30@{ label: "Defendants' circumstances" }
    A29 -- No --> A30 & A30 & A30
    A30 -- Yes:Give details about the defendants' circumstances --> A31["Alternatives to possession"]
    A30 -- No --> A31 & A31 & A31
    A31 -- Suspension of right to buy --> A32["Housing Act"]
    A31 -- Demotion of tenancy --> A32 & A32 & A32
    A32 -- "Which section of the Housing Act is the suspension of right to buy claim made under?-select any section" --> A33["Statement of express terms"]
    A32 -- "Which section of the Housing Act is the claim for demotion of tenancy made under?-select any section" --> A33 & A33 & A33
    A33 -- Yes:Give details of the terms --> A34["Reasons for requesting a suspension order and a demotion order"]
    A33 -- No --> A34 & A34 & A34
    A34 -- "Why are you requesting a suspension order? is required-Enter text" --> A35["Claiming costs"]
    A34 -- "Why are you requesting a demotion order? is required-Enter text" --> A35 & A35 & A35
    A34 --> A35 & A35 & A35 & A35
    A35 -- Yes --> A36["Additional reasons for possession"] & A36
    A35 -- No --> A36 & A36
    A36 -- Yes: Additional reasons for possession: Enter reason --> A37["Underlessee or mortgagee entitled to claim relief against forfeiture"]
    A36 -- No --> A37 & A37
    A37 --> A38["Underlessee or mortgagee details"]
    A38 -- "Yes - add documents" --> A39["Upload additional documents"]
    A38 -- No --> A39
    A39 -- Yes --> A40["Applications"] & A41["Language used"]
    A39 -- No --> A40 & A40 & A41
    A40 -- English --> A41
    A40 -- Welsh --> A41
    A40 -- English and Welsh --> A41
    A41 -- Submit and pay for my claim now --> A42["Completing your claim"] & A43["Make a claim-Check your answers"]
    A42 -- Continue --> A43
    A41 -- Save it for later --> A43 & A43 & A43
    A43 -- "Change link - Which language did you use to complete this service?, modify the language" --> A40 & A41
    A40 -- Select English and Welsh --> A41
    A43 -- "Change link - What type of tenancy or licence is in place?" --> A13 & A13
    A13 --> A45{"Secure tenancy"} & A45
    A45 -- Continue --> A20 & A20
    A20 -- Select: Rent arrears or breach of the tenancy ground 1 --> A46["Rent arrears or breach of the tenancy (ground 1)"] & A44["Claim successfully created"]
    A46 -- Select Rent arrears --> A21
    A36 -- Yes: Additional reasons for possession : Enter reason --> A37
    A37 -- Yes --> A38
    A38 -- "Underlessee or mortgagee name - Yes, Underlessee or mortgagee address - Yes,
Additional underlessee or mortgagee? -Yes" --> A39
    A38 -- "Underlessee or mortgagee name - No, Underlessee or mortgagee address - No,
Additional underlessee or mortgagee? - No" --> A39
    A37 -- No --> A39
    A39 -- "Yes - add documents" --> A40
    A41 -- English --> A42
    A41 -- Welsh --> A42
    A41 -- English and Welsh --> A42
    A43 -- Continue --> A43
    A41 -- Select English and Welsh --> A42
    A42 -- Save it for later --> A43 & A43
    A44 -- Select Rent arrears --> A21
    A35 -- Continue --> A36
    A36 -- Continue --> A37
    A37 -- Continue --> A38
    A38 -- Continue --> A39
    A39 -- Continue --> A40
    A40 -- Continue --> A41
    A41 -- Continue --> A42
    A43 -- Save and Continue --> A44
    A43 -- Cancel --> A5
    A4@{ shape: rect}
    A8@{ shape: rect}
    A30@{ shape: rect}
