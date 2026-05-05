flowchart TB
    A0["Sign in or create an account"] -- Enter credentials --> A1["Case list"]
    A1 --> A2["Create Case"]
    A2 -- Start --> A3["Make a claim"]
    A3 -- Continue --> A4@{ label: "What is the address of the property you're claiming possession" }
    A4 -- Case number created --> A5["Provide more details about your claim - h2"]
    A5 -- Continue --> A7{"Claimant type"}
    A7 -- Community lanlord --> A9["Claim type"]
    A7 -- Private landlord --> A8@{ label: "You're not eligible for this online service" }
    A7 -- Mortgage lender --> A8
    A7 -- Other --> A8
    A9 -- "Yes - Claimant Trespassed" --> A8
    A9 -- No --> A10["Claimant name"]
    A10 -- Yes or No --> A11["Claimant details"]
    A11 -- Yes or No or Not applicable --> A12["Contact preferences"]
    A12 -- Yes or No --> A13["Defendant details"]
    A13 -- Yes or No & Add single or multiple defendants --> A14["Occupation contract or licence details"]
    A14 -- Secure --> A15{"What are your grounds for possession?"}
    A14 -- Standard --> A15
    A14 -- Other --> A15
    A15 -- "1 - Select : Only Rent arrears" - 
    Rent arrears (breach of contract)
(section 157)
Contract-holder under a periodic
standard contract seriously in arrears
with rent (section 181)
Contract-holder under a fixed term
standard contract seriously in arrears
with rent (section 187)--> A16["“Pre-Action protocol”"]
    A15 -- "2 - Select : Only ASB" - Antisocial behaviour (breach of contract)
(section 157) --> A18["Antisocial behaviour and illegal or prohibited conduct"]
    A15 -- "3 - Select : Rent arrears  + Other grounds, Do not select ASB" --> A17["Reasons for possession"]
    A15 -- "4 - Select : Rent arrears + Antisocial behaviour (ASB) + Other grounds  / ASB with other grounds" --> A17
    A15 -- "5 - Select : Only Other grounds" --> A17
    A15 -- "6 - Select ASB with Rent Arrears" --> A18
    A18 -- 6 (Yes or No) --> A16
    A17 -- 4 --> A18
    A18 -- 2 (Yes or No) --> A16
    A18 -- 4 (Yes or No) --> A16
    A17 -- 3 (Continue) --> A16
    A17 -- 5 (Continue) --> A16
    A16 -- Yes or No --> A19["Mediation and settlement"]
    A19 -- Yes or No --> A20["Notice of your intention to begin possession proceedings"]
    A20 -- 2 - No --> A26
    A20 -- 2,5 - Yes--> A21["Notice details"]
    A21 -- 2 --> A26
    A21 -- 5 --> A26  
    A20 -- 5 - No --> A26
    A20 -- 1, 3, 4 , 6 - Yes --> A22["Rent details"] 
    A22 -- 1, 3, 4, 6 - Enter Rent details & select frequency --> A23["Daily rent amount"]
    A23 -- 1, 3, 4 , 6- Yes or No --> A24["Details of rent arrears"]
    A24 -- 1, 3, 4 , 6 - Yes or No --> A25["Money judgment"]
    A25 -- Yes or No --> A26["Claimant circumstances"]
    A26 -- Yes or No --> A27@{ label: "Defendants' circumstances" }
    A27 -- Yes or No --> A28["Prohibited conduct standard contract"]
    A28 -- Yes or No --> A29["Claiming costs"]
    A29 -- Yes or No --> A30["Additional reasons for possession"]
    A30 -- Yes or No --> A31{"Underlessee or mortgagee entitled to claim relief against forfeiture"}
    A31 -- Yes --> A32["Underlessee or mortgagee details"]
    A31 -- No --> A33["Upload additional documents"]
    A32 -- Yes or No --> A33    
    A33 -- Yes or No --> A34["Language used"]
    A34 -- Select: English or Welsh or English and Welsh --> A35{"Completing your claim"}
    A35 -- Submit and pay for my claim now --> A36["Statement of truth"]
    A36 -- Completed either by selecting Claimant or Claimant's legal representative --> A37["Make a Claim"]
    A35 -- Save it for later --> A37
    A37 -- Save and continue --> A38["Claim successfully submitted"]
