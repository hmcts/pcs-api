SELECT $work$
crosswalk (target, scope, legacy_value, ref_key, ref_value_en, subtype_key, subtype_value_en) AS (
  VALUES
    ('claim_party.claimant_type_key', '', 'PRIVATE_LANDLORD', 'AAA3-PLD', 'Private Landlord', NULL, NULL),
    ('claim_party.claimant_type_key', '', 'PROVIDER_OF_SOCIAL_HOUSING', 'AAA3-SHS', 'Social Housing', NULL, NULL),
    ('claim_party.claimant_type_key', '', 'COMMUNITY_LANDLORD', 'AAA3-RCL', 'Registered Community Landlord', NULL, NULL),
    ('claim_party.claimant_type_key', '', 'MORTGAGE_LENDER', 'AAA3-MPL', 'Mortgage Lender', NULL, NULL),
    ('claim_party.claimant_type_key', '', 'OTHER', 'AAA3-OTH', 'Other', NULL, NULL),
    ('claim_party.entity_role_code_key', '', 'CLAIMANT', 'CLAI', 'Claimant', NULL, NULL),
    ('claim_party.entity_role_code_key', '', 'DEFENDANT', 'DEFE', 'Defendant', NULL, NULL),
    ('claim_party.entity_role_code_key', '', 'LITIGATION_FRIEND', 'LIFR', 'Litigation Friend', NULL, NULL),
    ('claim_party.entity_role_code_key', '', 'UNDERLESSEE_OR_MORTGAGEE', 'OTPA', 'Other Party', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'ASSURED_TENANCY', 'AAA3-ATY', 'Assured tenancy', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'SECURE_TENANCY', 'AAA3-STY', 'Secure tenancy', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'INTRODUCTORY_TENANCY', 'AAA3-ITY', 'Introductory tenancy', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'FLEXIBLE_TENANCY', 'AAA3-FTY', 'Flexible tenancy', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'DEMOTED_TENANCY', 'AAA3-DTY', 'Demoted tenancy', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'SECURE_CONTRACT', 'AAA3-SEC', 'Secure contract', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'STANDARD_CONTRACT', 'AAA3-STN', 'Standard contract', NULL, NULL),
    ('tenancy_licence.tenancy_type_key', '', 'OTHER', 'AAA3-OTH', 'Other', NULL, NULL),
    ('hearing.hearing_type_key', '', 'POSSESSION', 'AAA3-FPH', 'First Possession Hearing', NULL, NULL),
    ('hearing.hearing_type_key', '', 'APPLICATION', 'AAA3-APP', 'Application Hearing', NULL, NULL),
    ('hearing.hearing_type_key', '', 'ADJOURNED', 'AAA3-AFP', 'Adjourned First Possession Hearing', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'ALTERNATIVE_ACCOMMODATION_GROUND9', 'AAA3-grounds-037', 'Suitable alternative accommodation (ground 9)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'DETERIORATION_FURNITURE_GROUND15', 'AAA3-grounds-045', 'Deterioration of furniture (ground 15)', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_ANTISOCIAL', 'S84A_CONDITION_1', 'AAA3-grounds-036', 'Absolute ground for possession for anti-social behaviour', 'AAA3-sub-grounds-003', 'Condition 1 of section 84A of the housing act 1985'),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_ANTISOCIAL', 'S84A_CONDITION_2', 'AAA3-grounds-036', 'Absolute ground for possession for anti-social behaviour', 'AAA3-sub-grounds-004', 'Condition 2 of section 84A of the housing act 1985'),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_ANTISOCIAL', 'S84A_CONDITION_3', 'AAA3-grounds-036', 'Absolute ground for possession for anti-social behaviour', 'AAA3-sub-grounds-005', 'Condition 3 of section 84A of the housing act 1985'),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_ANTISOCIAL', 'S84A_CONDITION_4', 'AAA3-grounds-036', 'Absolute ground for possession for anti-social behaviour', 'AAA3-sub-grounds-006', 'Condition 4 of section 84A of the housing act 1985'),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_ANTISOCIAL', 'S84A_CONDITION_5', 'AAA3-grounds-036', 'Absolute ground for possession for anti-social behaviour', 'AAA3-sub-grounds-007', 'Condition 5 of section 84A of the housing act 1985'),
    ('claim_ground', 'WALES_STANDARD_OTHER_MANDATORY', 'FAILURE_TO_GIVE_UP_POSSESSION_S170', 'AAA3-grounds-052', 'Failure to give up possession on date specified in contract-holder''s notice (section 170)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_MANDATORY', 'SERIOUS_ARREARS_PERIODIC_S181', 'AAA3-grounds-054', 'Contract-holder under a periodic standard contract seriously in arrears with rent (section 181)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_MANDATORY', 'SERIOUS_ARREARS_FIXED_TERM_S187', 'AAA3-grounds-056', 'Contract-holder under a fixed term standard contract seriously in arrears with rent (section 187)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_MANDATORY', 'FAIL_TO_GIVE_UP_BREAK_NOTICE_S191', 'AAA3-grounds-057', 'Failure to give up possession on date specified in contract-holder''s break clause notice (section 191)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_MANDATORY', 'LANDLORD_BREAK_CLAUSE_S199', 'AAA3-grounds-058', 'Notice given under a landlord''s break clause (section 199)', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_MANDATORY', 'FAILURE_TO_GIVE_UP_POSSESSION_S170', 'AAA3-grounds-052', 'Failure to give up possession on date specified in contract-holder''s notice (section 170)', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_MANDATORY', 'FAILURE_TO_GIVE_UP_POSSESSION_S191', 'AAA3-grounds-057', 'Failure to give up possession on date specified in contract-holder''s break clause notice (section 191)', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_MANDATORY', 'LANDLORD_NOTICE_S199', 'AAA3-grounds-058', 'Notice given under a landlord''s break clause (section 199)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_DISCRETIONARY', 'ESTATE_MANAGEMENT_GROUNDS_S160', 'AAA3-grounds-050', 'Estate management grounds (section 160)', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_DISCRETIONARY', 'ESTATE_MANAGEMENT_GROUNDS_S160', 'AAA3-grounds-050', 'Estate management grounds (section 160)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_ESTATE_MANAGEMENT', 'BUILDING_WORKS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-008', 'Ground A: Building works'),
    ('claim_ground', 'WALES_STANDARD_OTHER_ESTATE_MANAGEMENT', 'REDEVELOPMENT_SCHEMES', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-009', 'Ground B: Redevelopment schemes'),
    ('claim_ground', 'WALES_STANDARD_OTHER_ESTATE_MANAGEMENT', 'CHARITIES', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-010', 'Ground C: Charities'),
    ('claim_ground', 'WALES_STANDARD_OTHER_ESTATE_MANAGEMENT', 'DISABLED_SUITABLE_DWELLING', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-011', 'Ground D: Dwelling suitable for disabled people'),
    ('claim_ground', 'WALES_STANDARD_OTHER_ESTATE_MANAGEMENT', 'HOUSING_ASSOCIATIONS_AND_TRUSTS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-012', 'Ground E: Housing associations and housing trusts - people difficult to house'),
    ('claim_ground', 'WALES_STANDARD_OTHER_ESTATE_MANAGEMENT', 'SPECIAL_NEEDS_DWELLINGS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-013', 'Ground F: Groups of dwellings for people with special needs'),
    ('claim_ground', 'WALES_STANDARD_OTHER_ESTATE_MANAGEMENT', 'RESERVE_SUCCESSORS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-014', 'Ground G: Reserve successors'),
    ('claim_ground', 'WALES_STANDARD_OTHER_ESTATE_MANAGEMENT', 'JOINT_CONTRACT_HOLDERS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-015', 'Ground H: Joint contract-holders'),
    ('claim_ground', 'WALES_STANDARD_OTHER_ESTATE_MANAGEMENT', 'OTHER_ESTATE_MANAGEMENT_REASONS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-016', 'Ground I: Other estate management reasons'),
    ('claim_ground', 'WALES_SECURE_ESTATE_MANAGEMENT', 'BUILDING_WORKS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-008', 'Ground A: Building works'),
    ('claim_ground', 'WALES_SECURE_ESTATE_MANAGEMENT', 'REDEVELOPMENT_SCHEMES', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-009', 'Ground B: Redevelopment schemes'),
    ('claim_ground', 'WALES_SECURE_ESTATE_MANAGEMENT', 'CHARITIES', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-010', 'Ground C: Charities'),
    ('claim_ground', 'WALES_SECURE_ESTATE_MANAGEMENT', 'DISABLED_SUITABLE_DWELLING', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-011', 'Ground D: Dwelling suitable for disabled people'),
    ('claim_ground', 'WALES_SECURE_ESTATE_MANAGEMENT', 'HOUSING_ASSOCIATIONS_AND_TRUSTS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-012', 'Ground E: Housing associations and housing trusts - people difficult to house'),
    ('claim_ground', 'WALES_SECURE_ESTATE_MANAGEMENT', 'SPECIAL_NEEDS_DWELLINGS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-013', 'Ground F: Groups of dwellings for people with special needs'),
    ('claim_ground', 'WALES_SECURE_ESTATE_MANAGEMENT', 'RESERVE_SUCCESSORS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-014', 'Ground G: Reserve successors'),
    ('claim_ground', 'WALES_SECURE_ESTATE_MANAGEMENT', 'JOINT_CONTRACT_HOLDERS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-015', 'Ground H: Joint contract-holders'),
    ('claim_ground', 'WALES_SECURE_ESTATE_MANAGEMENT', 'OTHER_ESTATE_MANAGEMENT_REASONS', 'AAA3-grounds-050', 'Estate management grounds (section 160)', 'AAA3-sub-grounds-016', 'Ground I: Other estate management reasons'),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY|RENT_ARREARS', 'RENT_ARREARS_OR_BREACH_OF_TENANCY', 'AAA3-grounds-017', 'Ground 1: Rent arrears or breach of the tenancy', 'AAA3-sub-grounds-001', 'Rent arrears'),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY|BREACH', 'RENT_ARREARS_OR_BREACH_OF_TENANCY', 'AAA3-grounds-017', 'Ground 1: Rent arrears or breach of the tenancy', 'AAA3-sub-grounds-002', 'Breach of tenancy'),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY|BOTH', 'RENT_ARREARS_OR_BREACH_OF_TENANCY', 'AAA3-grounds-017', 'Ground 1: Rent arrears or breach of the tenancy', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY|NEITHER', 'RENT_ARREARS_OR_BREACH_OF_TENANCY', 'AAA3-grounds-017', 'Ground 1: Rent arrears or breach of the tenancy', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY', 'NUISANCE_OR_IMMORAL_USE', 'AAA3-grounds-018', 'Ground 2: Nuisance, annoyance, illegal or immoral use of the property', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY', 'DOMESTIC_VIOLENCE', 'AAA3-grounds-019', 'Ground 2A: Domestic violence', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY', 'RIOT_OFFENCE', 'AAA3-grounds-020', 'Ground 2ZA: Offence during a riot', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY', 'PROPERTY_DETERIORATION', 'AAA3-grounds-021', 'Ground 3: Deterioration in the condition of the property', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY', 'FURNITURE_DETERIORATION', 'AAA3-grounds-022', 'Ground 4: Deterioration of furniture', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY', 'TENANCY_OBTAINED_BY_FALSE_STATEMENT', 'AAA3-grounds-023', 'Ground 5: Tenancy obtained by false statement', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY', 'PREMIUM_PAID_MUTUAL_EXCHANGE', 'AAA3-grounds-024', 'Ground 6: Premium paid in connection with mutual exchange', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY', 'UNREASONABLE_CONDUCT_TIED_ACCOMMODATION', 'AAA3-grounds-025', 'Ground 7: Unreasonable conduct in tied accommodation', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY', 'REFUSAL_TO_MOVE_BACK', 'AAA3-grounds-026', 'Ground 8: Refusal to move back to main home after works completed', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_MANDATORY_ALT', 'OVERCROWDING', 'AAA3-grounds-027', 'Ground 9: Overcrowding', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_MANDATORY_ALT', 'LANDLORD_WORKS', 'AAA3-grounds-028', 'Ground 10: Landlord''s works', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_MANDATORY_ALT', 'PROPERTY_SOLD', 'AAA3-grounds-029', 'Ground 10A: Property sold for redevelopment', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_MANDATORY_ALT', 'CHARITABLE_LANDLORD', 'AAA3-grounds-030', 'Ground 11: Charitable landlords', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT', 'TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE', 'AAA3-grounds-031', 'Ground 12: Tied accommodation needed for another employee', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT', 'ADAPTED_ACCOMMODATION', 'AAA3-grounds-032', 'Ground 13: Adapted accommodation', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT', 'HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES', 'AAA3-grounds-033', 'Ground 14: Housing special circumstances accommodation', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT', 'SPECIAL_NEEDS_ACCOMMODATION', 'AAA3-grounds-034', 'Ground 15: Special needs accommodation', NULL, NULL),
    ('claim_ground', 'SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT', 'UNDER_OCCUPYING_AFTER_SUCCESSION', 'AAA3-grounds-035', 'Ground 15A: Underoccupying after succession', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'OWNER_OCCUPIER_GROUND1', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'HOLIDAY_LET_GROUND3', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'SERIOUS_RENT_ARREARS_GROUND8', 'AAA3-grounds-001', 'Serious rent arrears (ground 8)', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'REPOSSESSION_GROUND2', 'AAA3-grounds-006', 'Sale by mortgagee (ground 2)', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'STUDENT_LET_GROUND4', 'AAA3-grounds-009', 'Student accommodation (ground 4)', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'MINISTER_RELIGION_GROUND5', 'AAA3-grounds-012', 'Ministers of religion (ground 5)', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'REDEVELOPMENT_GROUND6', 'AAA3-grounds-013', 'Redevelopment (ground 6)', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'DEATH_OF_TENANT_GROUND7', 'AAA3-grounds-014', 'Death of tenant (ground 7)', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'ANTISOCIAL_BEHAVIOUR_GROUND7A', 'AAA3-grounds-015', 'Severe anti-social behaviour or criminal behaviour (ground 7A)', NULL, NULL),
    ('claim_ground', 'ASSURED_MANDATORY', 'NO_RIGHT_TO_RENT_GROUND7B', 'AAA3-grounds-016', 'No right to rent (ground 7B)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'RENT_ARREARS_GROUND10', 'AAA3-grounds-038', 'Any rent arrears (ground 10)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'PERSISTENT_DELAY_GROUND11', 'AAA3-grounds-039', 'Persistent arrears (ground 11)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'BREACH_TENANCY_GROUND12', 'AAA3-grounds-040', 'Breach of tenancy (ground 12)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'DETERIORATION_PROPERTY_GROUND13', 'AAA3-grounds-041', 'Deterioration of property (ground 13)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'NUISANCE_ANNOYANCE_GROUND14', 'AAA3-grounds-042', 'Anti-social behaviour (ground 14)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'DOMESTIC_VIOLENCE_GROUND14A', 'AAA3-grounds-043', 'Domestic abuse (ground 14A)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'OFFENCE_RIOT_GROUND14ZA', 'AAA3-grounds-044', 'Rioting (ground 14ZA)', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'EMPLOYEE_LANDLORD_GROUND16', 'AAA3-grounds-047', 'Property 16', NULL, NULL),
    ('claim_ground', 'ASSURED_DISCRETIONARY', 'FALSE_STATEMENT_GROUND17', 'AAA3-grounds-046', 'False statement (ground 17)', NULL, NULL),
    ('claim_ground', 'ASSURED_OTHER', 'OTHER', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|DEMOTED_TENANCY', 'RENT_ARREARS', 'AAA3-grounds-048', 'Demoted tenancy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|INTRODUCTORY_TENANCY', 'RENT_ARREARS', 'AAA3-grounds-049', 'Introductory tenacy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|DEMOTED_TENANCY', 'BREACH_OF_THE_TENANCY', 'AAA3-grounds-048', 'Demoted tenancy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|INTRODUCTORY_TENANCY', 'BREACH_OF_THE_TENANCY', 'AAA3-grounds-049', 'Introductory tenacy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|DEMOTED_TENANCY', 'ANTI_SOCIAL', 'AAA3-grounds-048', 'Demoted tenancy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|INTRODUCTORY_TENANCY', 'ANTI_SOCIAL', 'AAA3-grounds-049', 'Introductory tenacy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|DEMOTED_TENANCY', 'ABSOLUTE_GROUNDS', 'AAA3-grounds-048', 'Demoted tenancy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|INTRODUCTORY_TENANCY', 'ABSOLUTE_GROUNDS', 'AAA3-grounds-049', 'Introductory tenacy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|DEMOTED_TENANCY', 'OTHER', 'AAA3-grounds-048', 'Demoted tenancy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|INTRODUCTORY_TENANCY', 'OTHER', 'AAA3-grounds-049', 'Introductory tenacy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS|DEMOTED_TENANCY', 'NO_GROUNDS', 'AAA3-grounds-048', 'Demoted tenancy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS|INTRODUCTORY_TENANCY', 'NO_GROUNDS', 'AAA3-grounds-049', 'Introductory tenacy', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|OTHER', 'RENT_ARREARS', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|OTHER', 'BREACH_OF_THE_TENANCY', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|OTHER', 'ANTI_SOCIAL', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|OTHER', 'ABSOLUTE_GROUNDS', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER|OTHER', 'OTHER', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS|OTHER', 'NO_GROUNDS', 'AAA3-grounds-010', 'Other', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_DISCRETIONARY', 'RENT_ARREARS_S157', 'AAA3-grounds-051', 'Breach of contract (section 157)', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_DISCRETIONARY', 'ANTISOCIAL_BEHAVIOUR_S157', 'AAA3-grounds-051', 'Breach of contract (section 157)', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_DISCRETIONARY', 'OTHER_BREACH_OF_CONTRACT_S157', 'AAA3-grounds-051', 'Breach of contract (section 157)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_DISCRETIONARY', 'RENT_ARREARS_S157', 'AAA3-grounds-051', 'Breach of contract (section 157)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_DISCRETIONARY', 'ANTISOCIAL_BEHAVIOUR_S157', 'AAA3-grounds-051', 'Breach of contract (section 157)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_DISCRETIONARY', 'OTHER_BREACH_OF_CONTRACT_S157', 'AAA3-grounds-051', 'Breach of contract (section 157)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_MANDATORY', 'LANDLORD_NOTICE_PERIODIC_S178', 'AAA3-grounds-053', 'Landlord''s notice given in relation to periodic standard contract AKA "Non-Fault"(section 178)', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_MANDATORY', 'LANDLORD_NOTICE_FT_END_S186', 'AAA3-grounds-055', 'Landlord''s notice in connection with end of fixed term given (section 186). Applies to contracts under schedule 9B.', NULL, NULL),
    ('claim_ground', 'WALES_SECURE_MANDATORY', 'LANDLORD_NOTICE_S186', 'AAA3-grounds-055', 'Landlord''s notice in connection with end of fixed term given (section 186). Applies to contracts under schedule 9B.', NULL, NULL),
    ('claim_ground', 'WALES_STANDARD_OTHER_MANDATORY', 'CONVERTED_FIXED_TERM_SCH12_25B2', 'AAA3-grounds-059', 'Notice given in relation to end of converted fixed term standard contract (para 25B(2) of schedule 12)', NULL, NULL),
    ('claim.claim_type_key', '', 'TRESPASS', 'AAA3-TCM', 'Trespass Claim', NULL, NULL),
    ('claim.claim_type_key', '', 'PROVIDER_OF_SOCIAL_HOUSING|ENGLAND', 'AAA3-SLD', 'Standard Possession Claim (Social Landlord - England)', NULL, NULL),
    ('claim.claim_type_key', '', 'COMMUNITY_LANDLORD|WALES', 'AAA3-WSL', 'Standard Possession Claim (Social Landlord - Wales)', NULL, NULL),
    ('claim.claim_type_key', '', 'MORTGAGE_LENDER', 'AAA3-MOR', 'Standard Possession Claim (Mortgage)', NULL, NULL),
    ('claim.claim_type_key', '', 'PRIVATE_LANDLORD|WALES', 'AAA3-WPL', 'Standard Possession Claim (Private Landlord - Wales)', NULL, NULL),
    ('claim.claim_type_key', '', 'PRIVATE_LANDLORD|ENGLAND', 'AAA3-PLD', 'Standard Possession Claim (Private Landlord - England)', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'appealabandoned', 'abandoned', 'Appeal abandoned', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'bailconditionsmet', 'conditions-met', 'Bail Conditions met', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'courtdisruptionclosure', 'court-closure', 'Court disruption/closure', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'dutyadvisornotavailable', 'dadvunav', 'Duty Advisor not available', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'appellanttransferredtoadifferentdetentionfacility', 'detention-facility-transfer', 'Appellant transferred to a different detention facility', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'expertevidenceincontention', 'evidence-contention', 'Expert evidence in contention', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'expertevidencerequired', 'evidence-required', 'Expert evidence required', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'exclusion', 'exclusio', 'Exclusion', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'incompletetribunal', 'incompl', 'Incomplete Tribunal', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'judicialdirection', 'jodir', 'Judicial direction', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'nojudgeavailablenonattendance', 'joh-not-attend', 'No judge available/non attendance', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'lapsed', 'lapsed', 'Lapsed', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'listedinerror', 'listerr', 'Listed In error', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'newissueraised', 'new-issue', 'New issue raised', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'novalidappeal', 'no-valid-appeal', 'No valid appeal', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'feenotpaid', 'notpaid', 'Fee not paid', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'nolongerreadyforhearing', 'notready', 'No longer ready for hearing', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'other', 'other', 'Other', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'applicationforpostponementgranted', 'postponement-granted', 'Application for postponement granted', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'publichealthemergency', 'public-health', 'Public health emergency', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'reasonableadjustmentnotmet', 'ra-not-met', 'Reasonable adjustment not met', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'reclassified', 'reclassified', 'Reclassified', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'settled', 'settled', 'Settled', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'struckout', 'struck', 'Struck Out', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'partyunabletoattend', 'unable', 'Party Unable To Attend', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'vacated', 'vacated', 'Vacated', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'withdrawn', 'withdraw', 'Withdrawn', NULL, NULL),
    ('hearing.case_management_cancellation_reason_key', '', 'UNRECOGNISED_FREE_TEXT', 'other', 'Other', NULL, NULL)
),
tenancy_of_claim AS (
  SELECT c.id AS claim_id,
         CASE
           WHEN count(tl.id) = 0 THEN 'NO_TENANCY'
           WHEN count(DISTINCT tl.type) = 1 THEN min(tl.type)
           ELSE 'MIXED_TENANCY_TYPES'
         END AS tenancy_type
  FROM public.claim c
  LEFT JOIN public.tenancy_licence tl ON tl.case_id = c.case_id
  GROUP BY c.id
),
source_rows AS (
  SELECT 'claim_party.claimant_type_key' AS target,
         cp.claim_id::text || '/' || cp.party_id::text AS row_id,
         '' AS scope,
         nullif(btrim(c.claimant_type), '') AS legacy_value,
         cp.claimant_type_key::text AS current_key,
         NULL::text AS current_subkey
  FROM public.claim_party cp
  JOIN public.claim c ON c.id = cp.claim_id
  WHERE cp.role = 'CLAIMANT'
  UNION ALL
  SELECT 'claim_party.entity_role_code_key', claim_id::text || '/' || party_id::text, '',
         nullif(btrim(role), ''), entity_role_code_key::text, NULL::text
  FROM public.claim_party
  UNION ALL
  SELECT 'tenancy_licence.tenancy_type_key', id::text, '',
         nullif(btrim(type), ''), tenancy_type_key::text, NULL::text
  FROM public.tenancy_licence
  UNION ALL
  SELECT 'hearing.hearing_type_key', id::text, '',
         nullif(btrim(type), ''), hearing_type_key::text, NULL::text
  FROM public.hearing
  UNION ALL
  SELECT 'claim_ground', g.id::text,
         CASE
           WHEN btrim(g.code) = 'RENT_ARREARS_OR_BREACH_OF_TENANCY'
             THEN btrim(g.category) || '|' || CASE
               WHEN coalesce(g.is_rent_arrears, false) AND nullif(btrim(g.reason), '') IS NULL THEN 'RENT_ARREARS'
               WHEN NOT coalesce(g.is_rent_arrears, false) AND nullif(btrim(g.reason), '') IS NOT NULL THEN 'BREACH'
               WHEN coalesce(g.is_rent_arrears, false) THEN 'BOTH'
               ELSE 'NEITHER'
             END
           WHEN btrim(g.category) LIKE 'INTRODUCTORY_DEMOTED_OTHER%'
             THEN btrim(g.category) || '|' || coalesce(t.tenancy_type, 'NO_TENANCY')
           ELSE btrim(g.category)
         END,
         nullif(btrim(g.code), ''), g.grounds_type_key::text, g.grounds_subtype_key::text
  FROM public.claim_ground g
  LEFT JOIN tenancy_of_claim t ON t.claim_id = g.claim_id
  UNION ALL
  SELECT 'claim.claim_type_key', c.id::text, '',
         CASE
           WHEN c.against_trespassers::text = 'YES' THEN 'TRESPASS'
           WHEN btrim(c.claimant_type) = 'MORTGAGE_LENDER' THEN 'MORTGAGE_LENDER'
           WHEN btrim(c.claimant_type) IN ('PROVIDER_OF_SOCIAL_HOUSING', 'COMMUNITY_LANDLORD', 'PRIVATE_LANDLORD')
                AND pc.legislative_country IN ('ENGLAND', 'WALES')
             THEN btrim(c.claimant_type) || '|' || pc.legislative_country
           ELSE nullif(btrim(c.claimant_type), '')
         END,
         c.claim_type_key::text, NULL::text
  FROM public.claim c
  LEFT JOIN public.pcs_case pc ON pc.id = c.case_id
  UNION ALL
  SELECT 'hearing.case_management_cancellation_reason_key', h.id::text, '',
         CASE
           WHEN n.norm IS NULL THEN NULL
           WHEN n.norm IN (SELECT legacy_value FROM crosswalk WHERE target = 'hearing.case_management_cancellation_reason_key')
             THEN n.norm
           ELSE 'UNRECOGNISED_FREE_TEXT'
         END,
         h.case_management_cancellation_reason_key::text, NULL::text
  FROM public.hearing h
  CROSS JOIN LATERAL (SELECT nullif(lower(regexp_replace(h.cancellation_reason, '[^[:alnum:]]+', '', 'g')), '') AS norm) n
),
reference_rows AS (
  SELECT 'claim_party.claimant_type_key' AS target, 'key' AS slot, key, value_en, active, NULL::text AS parent_key
  FROM public.claimant_type
  UNION ALL SELECT 'claim_party.entity_role_code_key', 'key', key, value_en, active, NULL FROM public.entity_role_code
  UNION ALL SELECT 'tenancy_licence.tenancy_type_key', 'key', key, value_en, active, NULL FROM public.tenancy_type
  UNION ALL SELECT 'hearing.hearing_type_key', 'key', key, value_en, active, NULL FROM public.hearing_type
  UNION ALL SELECT 'claim_ground', 'key', key, value_en, active, NULL FROM public.ground_type
  UNION ALL SELECT 'claim_ground', 'subkey', key, value_en, active, parent_key FROM public.ground_subtype
  UNION ALL SELECT 'claim.claim_type_key', 'key', key, value_en, active, NULL FROM public.claim_type
  UNION ALL SELECT 'hearing.case_management_cancellation_reason_key', 'key', key, value_en, active, NULL FROM public.case_management_cancellation_reason
),
classified AS (
  SELECT s.target, s.row_id, s.scope, s.legacy_value, s.current_key, s.current_subkey,
         cw.ref_key, cw.subtype_key,
         CASE
           WHEN s.legacy_value IS NULL THEN 'NO_LEGACY_VALUE'
           WHEN cw.ref_key IS NULL THEN 'UNMAPPED_LEGACY_VALUE'
           WHEN (s.current_key IS NOT NULL AND s.current_key <> cw.ref_key)
             OR (s.current_subkey IS NOT NULL AND s.current_subkey IS DISTINCT FROM cw.subtype_key)
             THEN 'CONFLICTING_EXISTING_KEY'
           WHEN s.current_key = cw.ref_key AND s.current_subkey IS NOT DISTINCT FROM cw.subtype_key
             THEN 'ALREADY_SET'
           WHEN rk.key IS NULL OR (cw.subtype_key IS NOT NULL AND rs.key IS NULL) THEN 'REFERENCE_MISSING'
           WHEN rk.active IS DISTINCT FROM 'YES' OR (cw.subtype_key IS NOT NULL AND rs.active IS DISTINCT FROM 'YES') THEN 'REFERENCE_INACTIVE'
           WHEN lower(regexp_replace(rk.value_en, '[^[:alnum:]]+', '', 'g')) <> lower(regexp_replace(cw.ref_value_en, '[^[:alnum:]]+', '', 'g'))
             OR (cw.subtype_key IS NOT NULL AND (lower(regexp_replace(rs.value_en, '[^[:alnum:]]+', '', 'g')) <> lower(regexp_replace(cw.subtype_value_en, '[^[:alnum:]]+', '', 'g')) OR rs.parent_key IS DISTINCT FROM cw.ref_key))
             THEN 'REFERENCE_MISMATCH'
           ELSE 'UPDATE'
         END AS status
  FROM source_rows s
  LEFT JOIN crosswalk cw
    ON cw.target = s.target AND cw.scope = s.scope AND cw.legacy_value = s.legacy_value
  LEFT JOIN reference_rows rk
    ON rk.target = s.target AND rk.slot = 'key' AND rk.key = cw.ref_key
  LEFT JOIN reference_rows rs
    ON rs.target = s.target AND rs.slot = 'subkey' AND rs.key = cw.subtype_key
)
$work$ AS work
\gset
