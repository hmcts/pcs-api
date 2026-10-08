-- Seeded from MRD List of Values V76: PCS (AAA3) and shared rows with Active = 'Y'. Deleted ('D') rows are left out.

CREATE TABLE public.judge_type
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  lov_order INTEGER,
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.judge_type (key, value_en, value_cy, lov_order, active)
VALUES
  ('90155', 'Acting Designated Civil Judge', NULL, 9, 'YES'),
  ('19', 'Circuit Judge', NULL, 3, 'YES'),
  ('30', 'Deputy Circuit Judge', NULL, 6, 'YES'),
  ('24', 'Deputy District Judge- Fee-Paid', NULL, 2, 'YES'),
  ('90001', 'Designated Civil Judge', NULL, 8, 'YES'),
  ('45', 'District Judge', NULL, 1, 'YES'),
  ('57', 'Master', NULL, 7, 'YES'),
  ('67', 'Recorder', NULL, 4, 'YES'),
  ('75', 'Senior Circuit Judge', NULL, 5, 'YES');

CREATE TABLE public.hearing_channel
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.hearing_channel (key, value_en, value_cy, active)
VALUES
  ('INTER', 'In Person', 'Yn Bersonol', 'YES'),
  ('NA', 'Not in Attendance', 'Ddim yn Bresennol', 'YES'),
  ('ONPPRS', 'On the Papers', 'Ar sail y Papurau', 'YES'),
  ('TEL', 'Telephone', 'Ffôn', 'YES'),
  ('VID', 'Video', 'Fideo', 'YES');

CREATE TABLE public.hearing_priority
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.hearing_priority (key, value_en, value_cy, active)
VALUES
  ('Standard', 'Standard', 'Safonol', 'YES'),
  ('Urgent', 'Urgent', 'Brys', 'YES');

CREATE TABLE public.hearing_case_subtype
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  parent_category VARCHAR(64),
  parent_key VARCHAR(64),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.hearing_case_subtype (key, value_en, parent_category, parent_key, active)
VALUES
  ('AAA3-CYAP-XX', 'Accelerated Possession (Wales Only)', 'caseType', 'AAA3-CYAP-XX', 'YES'),
  ('AAA3-DSP-XX', 'Demotion/Suspension of Right to buy/PSCO', 'caseType', 'AAA3-DSP-XX', 'YES'),
  ('AAA3-FAST_CLAIM-XX', 'Fast track (Possession)', 'caseType', 'AAA3-FAST_CLAIM-XX', 'YES'),
  ('AAA3-FR-XX', 'Forfeiture', 'caseType', 'AAA3-FR-XX', 'YES'),
  ('AAA3-INTERMEDIATE_CLAIM-XX', 'Intermediate (Possession)', 'caseType', 'AAA3-INTERMEDIATE_CLAIM-XX', 'YES'),
  ('AAA3-MA-XX', 'Mortgage Arrears', 'caseType', 'AAA3-MA-XX', 'YES'),
  ('AAA3-MNA-XX', 'Mortgage Non-Arrears', 'caseType', 'AAA3-MNA-XX', 'YES'),
  ('AAA3-MULTI_CLAIM-XX', 'Multi-track (Possession)', 'caseType', 'AAA3-MULTI_CLAIM-XX', 'YES'),
  ('AAA3-RA-PL', 'Private Landlord Rent Arrears', 'caseType', 'AAA3-RA-PL', 'YES'),
  ('AAA3-RNA-PL', 'Private landlord Rented Non-Arrears', 'caseType', 'AAA3-RNA-PL', 'YES'),
  ('AAA3-RF-XX', 'Relief from Forfeiture', 'caseType', 'AAA3-RF-XX', 'YES'),
  ('AAA3-SMALL_CLAIM-XX', 'Small Claims (Possession)', 'caseType', 'AAA3-SMALL_CLAIM-XX', 'YES'),
  ('AAA3-RA-SL', 'Social Landlord Rent Arrears', 'caseType', 'AAA3-RA-SL', 'YES'),
  ('AAA3-RNA-SL', 'Social Landlord Rented Non-Arrears', 'caseType', 'AAA3-RNA-SL', 'YES'),
  ('AAA3-TR-XX', 'Trespass', 'caseType', 'AAA3-TR-XX', 'YES');

CREATE TABLE public.facilities
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.facilities (key, value_en, value_cy, active)
VALUES
  ('14', 'Children''s Room', 'Ystafell i Blant', 'YES'),
  ('5', 'Common Area Restriction', 'Cyfyngiad Ardal Gyffredin', 'YES'),
  ('1', 'Custody Cell', 'Cell yn y Ddalfa', 'YES'),
  ('27', 'DVD/CD Player', 'Chwaraewr DVD/CD', 'YES'),
  ('23', 'Evidence Camera', 'Camera Tystiolaeth', 'YES'),
  ('33', 'IAC Type-C Conference Equipment', NULL, 'YES'),
  ('26', 'Laptop', 'Gliniadur', 'YES'),
  ('7', 'Linked Rooms', 'Ystafelloedd Cyswllt', 'YES'),
  ('6', 'Linked to Cells', 'Cyswllt i’r Celloedd', 'YES'),
  ('12', 'Private Interior Room', 'Ystafell Fewnol Breifat', 'YES'),
  ('22', 'Projector', 'Taflunydd', 'YES'),
  ('11', 'Secure Dock', 'Doc Diogel', 'YES'),
  ('25', 'Video Conference Trolley', 'Troli Cynhadledd Fideo', 'YES'),
  ('17', 'Wireless Internet Access', 'Mynediad Di-wifr i’r Rhyngrwyd', 'YES'),
  ('9', 'Witness Room', 'Ystafell i Dystion', 'YES'),
  ('10', 'Witness Screen', 'Sgrin i Dystion', 'YES');

CREATE TABLE public.auto_list_change_reasons
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  active YES_NO NOT NULL
);

INSERT INTO public.auto_list_change_reasons (key, value_en, active)
VALUES
  ('no-mapping-available', 'Some provided values had no auto listable mapping available', 'YES'),
  ('user-added-comments', 'User has provided some free text', 'YES');

CREATE TABLE public.additional_roles
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  active YES_NO NOT NULL
);

-- additional_roles: no active AdditionalRoles values in List of Values V76 for PCS or shared services

CREATE TABLE public.actual_part_heard_reason
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.actual_part_heard_reason (key, value_en, active)
VALUES
  ('AAA3-111', 'Appellant to attend - oral requested - did not attend - no reason', 'YES'),
  ('AAA3-110', 'Appellant to attend - oral requested - did not attend - reason given', 'YES'),
  ('AAA3-112', 'Appellant to attend - paper requested or no Enquiry Form returned', 'YES'),
  ('AAA3-118', 'Documents supplied but not before the Tribunal at the hearing', 'YES'),
  ('AAA3-114', 'Evidence (other than medical evidence) or further Response from Respondent required', 'YES'),
  ('AAA3-115', 'Evidence (other than medical evidence) or submission from Appellant required', 'YES'),
  ('AAA3-113', 'Further evidence essential', 'YES'),
  ('AAA3-117', 'Insufficient time to deal with case', 'YES'),
  ('AAA3-116', 'No interpreter', 'YES'),
  ('AAA3-121', 'Other administrative error', 'YES'),
  ('AAA3-122', 'Other reason for adjourning', 'YES'),
  ('AAA3-320', 'Party failed to comply with directions', 'YES'),
  ('AAA3-12', 'Postponed, due to Exclusions', 'YES'),
  ('AAA3-13', 'Postponed, due to Other Reasons', 'YES'),
  ('AAA3-14', 'Postponed, Incomplete Tribunal', 'YES'),
  ('AAA3-15', 'Postponed, No Reason Given', 'YES'),
  ('AAA3-16', 'Postponed, Other Party unable to attend', 'YES'),
  ('AAA3-119', 'Presenting Officer to attend', 'YES');

CREATE TABLE public.change_reasons
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.change_reasons (key, value_en, active)
VALUES
  ('adminerr', 'Admin error', 'YES'),
  ('adminreq', 'Admin requested change', 'YES'),
  ('postponement-granted', 'Application for postponement granted', 'YES'),
  ('expedite-granted', 'Application to expedite granted', 'YES'),
  ('requirements-change', 'Change to hearing request/requirements', 'YES'),
  ('interpreter-change', 'Change to interpreter requirements', 'YES'),
  ('claimreq', 'claimant requested change', 'YES'),
  ('court-closure', 'Court disruption/closure', 'YES'),
  ('hearing-type-change', 'Different hearing mode required', 'YES'),
  ('davdunav', 'Duty Solicitor not available', 'YES'),
  ('hearing-centre-transfer', 'Hearing Centre transfer directed/granted', 'YES'),
  ('judgereq', 'Judge requested change', 'YES'),
  ('no-interpreter-available', 'No interpreter available in required language', 'YES'),
  ('other', 'Other', 'YES'),
  ('partyreq', 'Party requested change', 'YES'),
  ('resporeq', 'respondent requested change', 'YES');

CREATE TABLE public.listing_status
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  active YES_NO NOT NULL
);

INSERT INTO public.listing_status (key, value_en, active)
VALUES
  ('CNCL', 'Cancelled', 'YES'),
  ('DRAFT', 'Draft - Parties yet to be Notified', 'YES'),
  ('FIXED', 'Fixed - Unlikely to be moved', 'YES'),
  ('PROV', 'Provisional - Will be firmed near time', 'YES');

CREATE TABLE public.actual_cancellation_reason
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.actual_cancellation_reason (key, value_en, active)
VALUES
  ('dadvunav', 'Duty Advisor not available', 'YES'),
  ('exclusio', 'Exclusion', 'YES'),
  ('incompl', 'Incomplete Tribunal', 'YES'),
  ('lapsed', 'Lapsed', 'YES'),
  ('listerr', 'Listed in error', 'YES'),
  ('notready', 'No longer ready for hearing', 'YES'),
  ('other', 'Other', 'YES'),
  ('papernot', 'Paper case not heard due to lack of time', 'YES'),
  ('notatt', 'Party did not attend', 'YES'),
  ('unable', 'Party unable to attend', 'YES'),
  ('settled', 'Settled', 'YES'),
  ('struck', 'Struck Out', 'YES'),
  ('vacated', 'Vacated', 'YES'),
  ('withdraw', 'Withdrawn', 'YES');

CREATE TABLE public.case_management_cancellation_reason
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.case_management_cancellation_reason (key, value_en, active)
VALUES
  ('postponement-granted', 'Application for postponement granted', 'YES'),
  ('conditions-met', 'Bail Conditions met', 'YES'),
  ('abandoned', 'Appeal abandoned', 'YES'),
  ('court-closure', 'Court disruption/closure', 'YES'),
  ('detention-facility-transfer', 'Appellant transferred to a different detention facility', 'YES'),
  ('dadvunav', 'Duty Advisor not available', 'YES'),
  ('exclusio', 'Exclusion', 'YES'),
  ('evidence-contention', 'Expert evidence in contention', 'YES'),
  ('evidence-required', 'Expert evidence required', 'YES'),
  ('notpaid', 'Fee not paid', 'YES'),
  ('incompl', 'Incomplete Tribunal', 'YES'),
  ('jodir', 'Judicial direction', 'YES'),
  ('lapsed', 'Lapsed', 'YES'),
  ('listerr', 'Listed In error', 'YES'),
  ('new-issue', 'New issue raised', 'YES'),
  ('joh-not-attend', 'No judge available/non attendance', 'YES'),
  ('notready', 'No longer ready for hearing', 'YES'),
  ('no-valid-appeal', 'No valid appeal', 'YES'),
  ('other', 'Other', 'YES'),
  ('unable', 'Party Unable To Attend', 'YES'),
  ('public-health', 'Public health emergency', 'YES'),
  ('ra-not-met', 'Reasonable adjustment not met', 'YES'),
  ('reclassified', 'Reclassified', 'YES'),
  ('settled', 'Settled', 'YES'),
  ('struck', 'Struck Out', 'YES'),
  ('vacated', 'Vacated', 'YES'),
  ('withdraw', 'Withdrawn', 'YES');

CREATE TABLE public.hearing_type
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  lov_order INTEGER,
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.hearing_type (key, value_en, value_cy, lov_order, active)
VALUES
  ('AAA3-AFP', 'Adjourned First Possession Hearing', NULL, 2, 'YES'),
  ('AAA3-ALL', 'Allocation Hearing', NULL, 7, 'YES'),
  ('AAA3-APP', 'Application Hearing', NULL, 4, 'YES'),
  ('AAA3-CMC', 'Case Management Conference', NULL, 8, 'YES'),
  ('AAA3-CCM', 'Costs and Case Management Conference', NULL, 9, 'YES'),
  ('AAA3-DIR', 'Directions Hearing', NULL, 3, 'YES'),
  ('AAA3-DIS', 'Disposal Hearing', NULL, 10, 'YES'),
  ('AAA3-DRH', 'Dispute Resolution Hearing', NULL, 11, 'YES'),
  ('AAA3-FPH', 'First Possession Hearing', NULL, 1, 'YES'),
  ('AAA3-LTA', 'Leave To Appeal Hearing', NULL, 6, 'YES'),
  ('AAA3-PTR', 'Pre-trial Review', NULL, 12, 'YES'),
  ('AAA3-TRI', 'Trial', NULL, 5, 'YES');

CREATE TABLE public.sign_language
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  active YES_NO NOT NULL
);

INSERT INTO public.sign_language (key, value_en, value_cy, active)
VALUES
  ('ase', 'American Sign Language (ASL)', 'Iaith Arwyddion America (ASL)', 'YES'),
  ('bfi', 'British Sign Language (BSL)', 'Iaith Arwyddion Prydain (BSL)', 'YES'),
  ('sign-dfr', 'Deaf Relay', 'Gwasanaeth Cyfnewid i Bobl Fyddar', 'YES'),
  ('sign-dma', 'Deafblind manual alphabet', 'Wyddor Pobl Ddall a Byddar', 'YES'),
  ('sign-hos', 'Hands on signing', 'Arwyddo trwy gyffyrddiad', 'YES'),
  ('ils', 'International Sign (IS)', 'Arwyddion Rhyngwladol (IS)', 'YES'),
  ('sign-lps', 'Lipspeaker', 'Gwefuslefarydd', 'YES'),
  ('sign-mkn', 'Makaton', 'Makaton', 'YES'),
  ('sign-ntr', 'Notetaker', 'Ysgrifennwr Nodiadau', 'YES'),
  ('sign-pst', 'Palantypist / Speech to text', 'Palanteipydd / Llais-i-destun', 'YES'),
  ('sign-sse', 'Speech Supported English (SSE)', 'Cefnogaeth Lleferydd Saesneg (SSE)', 'YES'),
  ('sign-vfs', 'Visual frame signing', 'Arwyddo Gweledol', 'YES');

CREATE TABLE public.unavailable_type
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  active YES_NO NOT NULL
);

INSERT INTO public.unavailable_type (key, value_en, value_cy, active)
VALUES
  ('All Day', 'Unavailable - All Day', 'Ddim ar gael - Drwy’r dydd', 'YES'),
  ('AM', 'Unavailable AM', 'Ddim ar gael - AM', 'YES'),
  ('PM', 'Unavailable PM', 'Ddim ar gael - PM', 'YES');

CREATE TABLE public.hearing_case_type
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.hearing_case_type (key, value_en, active)
VALUES
  ('AAA3-CYAP-XX', 'Accelerated Possession (Wales Only)', 'YES'),
  ('AAA3-DSP-XX', 'Demotion/Suspension of Right to buy/PSCO', 'YES'),
  ('AAA3-FAST_CLAIM-XX', 'Fast track (Possession)', 'YES'),
  ('AAA3-FR-XX', 'Forfeiture', 'YES'),
  ('AAA3-INTERMEDIATE_CLAIM-XX', 'Intermediate (Possession)', 'YES'),
  ('AAA3-MA-XX', 'Mortgage Arrears', 'YES'),
  ('AAA3-MNA-XX', 'Mortgage Non-Arrears', 'YES'),
  ('AAA3-MULTI_CLAIM-XX', 'Multi-track (Possession)', 'YES'),
  ('AAA3-RA-PL', 'Private Landlord Rent Arrears', 'YES'),
  ('AAA3-RNA-PL', 'Private landlord Rented Non-Arrears', 'YES'),
  ('AAA3-RF-XX', 'Relief from Forfeiture', 'YES'),
  ('AAA3-SMALL_CLAIM-XX', 'Small Claims (Possession)', 'YES'),
  ('AAA3-RA-SL', 'Social Landlord Rent Arrears', 'YES'),
  ('AAA3-RNA-SL', 'Social Landlord Rented Non-Arrears', 'YES'),
  ('AAA3-TR-XX', 'Trespass', 'YES');

CREATE TABLE public.interpreter_language
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  active YES_NO NOT NULL
);

INSERT INTO public.interpreter_language (key, value_en, value_cy, active)
VALUES
  ('ach', 'Acholi', 'Acholi', 'YES'),
  ('afr', 'Afrikaans', 'Afrikaans', 'YES'),
  ('aka', 'Akan', 'Akan', 'YES'),
  ('sqi', 'Albanian', 'Albaniaidd', 'YES'),
  ('arq', 'Algerian', 'Algeraidd', 'YES'),
  ('amh', 'Amharic', 'Amhareg', 'YES'),
  ('ara', 'Arabic', 'Arabeg', 'YES'),
  ('ara-ame', 'Arabic Middle Eastern', 'Arabeg Y Dwyrain Canol', 'YES'),
  ('ara-ana', 'Arabic North African', 'Arabeg Gogledd Affrica', 'YES'),
  ('hye', 'Armenian', 'Armeneg', 'YES'),
  ('aii', 'Assyrian', 'Assyriaidd', 'YES'),
  ('teo', 'Ateso', 'Ateso', 'YES'),
  ('aze', 'Azerbaijani (also North Azerbaijani/Azari)', 'Azerbaijani (a elwir hefyd yn Azerbaijani/Azari y Gogledd)', 'YES'),
  ('bjs', 'Bajan (West Indian)', 'Bajan (India''r Gorllewin)', 'YES'),
  ('bal', 'Baluchi', 'Baluchi', 'YES'),
  ('bam', 'Bambara', 'Bambara', 'YES'),
  ('bas', 'Bassa', 'Bassa', 'YES'),
  ('bel', 'Belarusian', 'Belarwseg', 'YES'),
  ('bem', 'Bemba (Zambia)', 'Bembeg (Sambia)', 'YES'),
  ('ben', 'Bengali', 'Bengali', 'YES'),
  ('ben-bsy', 'Bengali Sylheti', 'Bengali Sylheti', 'YES'),
  ('bin', 'Benin/Edo', 'Benin/Edo', 'YES'),
  ('ber', 'Berber', 'Berber', 'YES'),
  ('btn', 'Bhutanese', 'Bhutaneg', 'YES'),
  ('bih', 'Bihari', 'Bihari', 'YES'),
  ('byn', 'Bilin', 'Bilin', 'YES'),
  ('bos', 'Bosnian', 'Bosnieg', 'YES'),
  ('abr', 'Brong', 'Brong', 'YES'),
  ('bul', 'Bulgarian', 'Bwlgareg', 'YES'),
  ('mya', 'Burmese', 'Byrmaneg', 'YES'),
  ('yue', 'Cantonese', 'Cantoneg', 'YES'),
  ('ceb', 'Cebuano', 'Cebuano', 'YES'),
  ('cld', 'Chaldean Neo-Aramaic', 'Caldëeg Neo-Aramaeg', 'YES'),
  ('che', 'Chechen', 'Chechen', 'YES'),
  ('nya', 'Chichewa', 'Chichewa', 'YES'),
  ('ctg', 'Chittagonian', 'Chittagoneg', 'YES'),
  ('cpe', 'Creole (English)', 'Creole (Saesneg)', 'YES'),
  ('cpf', 'Creole (French)', 'Creole (Ffrangeg)', 'YES'),
  ('cpp', 'Creole (Portuguese)', 'Creole (Portiwgeeg)', 'YES'),
  ('crp', 'Creole (Spanish)', 'Creole (Sbaeneg)', 'YES'),
  ('hrv', 'Croatian', 'Croateg', 'YES'),
  ('ces', 'Czech', 'Tsieceg', 'YES'),
  ('dan', 'Danish', 'Daneg', 'YES'),
  ('prs', 'Dari', 'Dari', 'YES'),
  ('div', 'Dhivehi', 'Maldifeg', 'YES'),
  ('din', 'Dinka', 'Dinka', 'YES'),
  ('dyu', 'Dioula', 'Dioula', 'YES'),
  ('dua', 'Duala', 'Duala', 'YES'),
  ('nld', 'Dutch', 'Iseldireg', 'YES'),
  ('efi', 'Efik', 'Efik', 'YES'),
  ('eng', 'English', 'Saesneg', 'YES'),
  ('ish', 'Esan', 'Esan', 'YES'),
  ('est', 'Estonian', 'Estoneg', 'YES'),
  ('ewe', 'Ewe', 'Ewe', 'YES'),
  ('ewo', 'Ewondo', 'Ewondo', 'YES'),
  ('fat', 'Fanti', 'Fanti', 'YES'),
  ('fas', 'Farsi', 'Farsi', 'YES'),
  ('kur-fey', 'Feyli', 'Feyli', 'YES'),
  ('fij', 'Fijian', 'Fijieg', 'YES'),
  ('nld-nfl', 'Flemish', 'Fflemeg', 'YES'),
  ('fra', 'French', 'Ffrangeg', 'YES'),
  ('fra-can', 'French (Canadian)', 'Ffrangeg (Canada)', 'YES'),
  ('fra-faf', 'French African', 'Arabeg Affricanaidd', 'YES'),
  ('fra-far', 'French Arabic', 'Arabeg Ffrengig', 'YES'),
  ('ful', 'Fula', 'Fula', 'YES'),
  ('gaa', 'Ga', 'Ga', 'YES'),
  ('glg', 'Galician', 'Galiseg', 'YES'),
  ('kat', 'Georgian', 'Georgeg', 'YES'),
  ('deu', 'German', 'Almaeneg', 'YES'),
  ('hac', 'Gorani', 'Gorani', 'YES'),
  ('ell', 'Greek', 'Groegaidd', 'YES'),
  ('guj', 'Gujarati', 'Gujarati', 'YES'),
  ('sgw', 'Gurage', 'Gurage', 'YES'),
  ('hak', 'Hakka', 'Hakka', 'YES'),
  ('hau', 'Hausa', 'Hausa', 'YES'),
  ('heb', 'Hebrew', 'Hebraeg', 'YES'),
  ('her', 'Herero', 'Herero', 'YES'),
  ('hin', 'Hindi', 'Hindi', 'YES'),
  ('hnd', 'Hindko', 'Hindko', 'YES'),
  ('zho-hok', 'Hokkien', 'Hokkien', 'YES'),
  ('hun', 'Hungarian', 'Hwngareg', 'YES'),
  ('ibb', 'Ibibio', 'Ibibio', 'YES'),
  ('ibo', 'Igbo (Also Known As Ibo)', 'Igbo (a elwir hefyd yn Ibo)', 'YES'),
  ('ilo', 'Ilocano', 'Ilocano', 'YES'),
  ('ind', 'Indonesian', 'Indoneseg', 'YES'),
  ('iso', 'Isoko', 'Georgeg', 'YES'),
  ('ita', 'Italian', 'Eidaleg', 'YES'),
  ('jam', 'Jamaican Patois (Jamaican Creole)', 'Tafodiaith Jamaicaidd (Creoliaith Jamaicaidd)', 'YES'),
  ('jpn', 'Japanese', 'Japaneeg', 'YES'),
  ('jav', 'Javanese', 'Javaneeg', 'YES'),
  ('gjk', 'Kachi Koli', 'Kachi Koli', 'YES'),
  ('krx', 'Karon', 'Karon', 'YES'),
  ('kas', 'Kashmiri', 'Kashmiri', 'YES'),
  ('kck', 'Khalanga', 'Khalanga', 'YES'),
  ('khm', 'Khmer', 'Khmer', 'YES'),
  ('bnt-kic', 'Kichagga', 'Kichagga', 'YES'),
  ('kon', 'Kikongo', 'Kikongo', 'YES'),
  ('kik', 'Kikuyu', 'Kikuyu', 'YES'),
  ('kin', 'Kinyarwanda', 'Kinyarwandeg', 'YES'),
  ('run', 'Kirundi', 'Kirundi', 'YES'),
  ('swh', 'Kiswahili', 'Kiswahili', 'YES'),
  ('knn', 'Konkani', 'Konkani', 'YES'),
  ('kor', 'Korean', 'Coreeg', 'YES'),
  ('spv', 'Kosli, Sambalpuri', 'Kosli, Sambalpuri', 'YES'),
  ('kri', 'Krio (Sierra Leone)', 'Krio (Sierra Leone)', 'YES'),
  ('kru', 'Kru', 'Kru', 'YES'),
  ('kur-kbr', 'Kurdish Badini (Bahdini)', 'Cwrdeg Badini', 'YES'),
  ('kur-kkr', 'Kurdish kurmanji', 'Cwrdeg kurmanji', 'YES'),
  ('kur-ksr', 'Kurdish Sorani', 'Cwrdeg Sorani', 'YES'),
  ('kfr', 'Kutchi', 'Kutchi', 'YES'),
  ('kir', 'Kyrgyz', 'Kyrgyz', 'YES'),
  ('laj', 'Lango', 'Lango', 'YES'),
  ('lav', 'Latvian', 'Latfeg', 'YES'),
  ('lin', 'Lingala', 'Lingala', 'YES'),
  ('lit', 'Lithuanian', 'Lithwaneg', 'YES'),
  ('lub', 'Luba (Tshiluba)', 'Luba (Tshiluba)', 'YES'),
  ('lug', 'Luganda', 'Lugandeg', 'YES'),
  ('luo', 'Luo', 'Luo', 'YES'),
  ('luo-lah', 'Luo Acholi', 'Luo Acholi', 'YES'),
  ('luo-lky', 'Luo Kenyan', 'Luo Kenya', 'YES'),
  ('luo-llg', 'Luo Lango', 'Luo Lango', 'YES'),
  ('xog', 'Lusoga', 'Lusoga', 'YES'),
  ('mkd', 'Macedonian', 'Macedoneg', 'YES'),
  ('ara-mag', 'Maghreb', 'Maghreb', 'YES'),
  ('msa', 'Malay', 'Malay', 'YES'),
  ('mal', 'Malayalam', 'Malayalam', 'YES'),
  ('mku', 'Malinke', 'Malinke', 'YES'),
  ('mlt', 'Maltese', 'Maltaeg', 'YES'),
  ('cmn', 'Mandarin', 'Mandarin', 'YES'),
  ('mnk', 'Mandinka', 'Mandinka', 'YES'),
  ('mar', 'Marathi', 'Marathi', 'YES'),
  ('myx', 'Masaaba', 'Masaaba', 'YES'),
  ('men', 'Mende', 'Mende', 'YES'),
  ('min', 'Mina', 'Mina', 'YES'),
  ('ron-fmo', 'Moldovan', 'Moldofeg', 'YES'),
  ('mon', 'Mongolian', 'Molgoleg', 'YES'),
  ('mkw', 'Monokutuba', 'Monokutuba', 'YES'),
  ('cnr', 'Montenegrin', 'Montenegreg', 'YES'),
  ('nde', 'Ndebele', 'Ndebele', 'YES'),
  ('nep', 'Nepali', 'Nepali', 'YES'),
  ('pcm', 'Nigerian Pidgin', 'Pidgin (Nigeria)', 'YES'),
  ('hno', 'Northern Hindko', 'Hindko Gogleddol', 'YES'),
  ('nor', 'Norwegian', 'Norwyeg', 'YES'),
  ('nyn', 'Nyankole', 'Nyankole', 'YES'),
  ('nzi', 'Nzima', 'Nzima', 'YES'),
  ('orm', 'Oromo', 'Oromo', 'YES'),
  ('bfz', 'Pahari', 'Pahari', 'YES'),
  ('phr', 'Pahari-Potwari', 'Pahari-Potwari', 'YES'),
  ('pam', 'Pampangan', 'Pampangan', 'YES'),
  ('pag', 'Pangasinan', 'Pangasinan', 'YES'),
  ('pat', 'Patois', 'Patois', 'YES'),
  ('pol', 'Polish', 'Pwyleg', 'YES'),
  ('por', 'Portuguese', 'Portiwgeeg', 'YES'),
  ('por-bra', 'Portuguese (Brazil)', 'Portiwgaleg (Brasil)', 'YES'),
  ('pan', 'Punjabi', 'Punjabi', 'YES'),
  ('pan-pji', 'Punjabi Indian', 'Punjabi Indiaidd', 'YES'),
  ('pan-pjp', 'Punjabi Pakistani', 'Punjabi Pacistanaidd', 'YES'),
  ('pus', 'Pushtu (Also Known As Pashto)', 'Pushtu (a elwir hefyd yn Pashto)', 'YES'),
  ('rmm', 'Roma', 'Roma', 'YES'),
  ('ron', 'Romanian', 'Romaneg', 'YES'),
  ('rom', 'Romany', 'Romani', 'YES'),
  ('cgg', 'Rukiga', 'Rukiga', 'YES'),
  ('nyo', 'Runyoro', 'Runyoro', 'YES'),
  ('rus', 'Russian', 'Rwsieg', 'YES'),
  ('skt', 'Sakata', 'Sakata', 'YES'),
  ('skr', 'Saraiki (Seraiki)', 'Saraiki (Seraiki)', 'YES'),
  ('krn', 'Sarpo', 'Sarpo', 'YES'),
  ('srp', 'Serbian', 'Serbeg', 'YES'),
  ('hbs', 'Serbo-Croatian', 'Serbo-Croateg', 'YES'),
  ('tsn', 'Setswana', 'Setswana', 'YES'),
  ('scl', 'Shina', 'Shina', 'YES'),
  ('sna', 'Shona', 'Shona', 'YES'),
  ('snd', 'Sindhi', 'Sindhi', 'YES'),
  ('sin', 'Sinhalese', 'Sinhaleg', 'YES'),
  ('slk', 'Slovak', 'Slovak', 'YES'),
  ('slv', 'Slovenian', 'Slofeneg', 'YES'),
  ('som', 'Somali', 'Somali', 'YES'),
  ('snk', 'Soninke', 'Soninke', 'YES'),
  ('spa', 'Spanish', 'Sbaeneg', 'YES'),
  ('spa-lat', 'Spanish (Latin America)', 'Sbaeneg (America Ladin)', 'YES'),
  ('sus', 'Susu', 'Susu', 'YES'),
  ('swa', 'Swahili', 'Swahili', 'YES'),
  ('swa-sbv', 'Swahili Bravanese', 'Swahili Bravanese', 'YES'),
  ('swa-skb', 'Swahili Kibajuni', 'Swahili Kibajuni', 'YES'),
  ('swe', 'Swedish', 'Swedeg', 'YES'),
  ('syl', 'Sylheti', 'Sylheti', 'YES'),
  ('tgl', 'Tagalog', 'Tagalog', 'YES'),
  ('tai', 'Taiwanese', 'Taiwaneg', 'YES'),
  ('tgk', 'Tajik', 'Tajiceg', 'YES'),
  ('tam', 'Tamil', 'Tamil', 'YES'),
  ('tel', 'Telugu', 'Telugu', 'YES'),
  ('tem', 'Temne', 'Temne', 'YES'),
  ('tet', 'Tetum', 'Tetwm', 'YES'),
  ('tha', 'Thai', 'Thai', 'YES'),
  ('bod', 'Tibetan', 'Tibeteg', 'YES'),
  ('tig', 'Tigre', 'Tigre', 'YES'),
  ('tir', 'Tigrinya', 'Tigrinya', 'YES'),
  ('ttj', 'Tooro', 'Tooro', 'YES'),
  ('don', 'Toura', 'Toura', 'YES'),
  ('tur', 'Turkish', 'Tyrceg', 'YES'),
  ('tuk', 'Turkmen', 'Turkmen', 'YES'),
  ('twi', 'Twi', 'Twi', 'YES'),
  ('uig', 'Uighur', 'Uighur', 'YES'),
  ('ukr', 'Ukrainian', 'Wcreineg', 'YES'),
  ('urd', 'Urdu', 'Urdu', 'YES'),
  ('urh', 'Urhobo', 'Urhobo', 'YES'),
  ('uzb', 'Uzbek', 'Uzbek', 'YES'),
  ('vie', 'Vietnamese', 'Fietnameg', 'YES'),
  ('vsa', 'Visayan', 'Visayan', 'YES'),
  ('cym', 'Welsh', 'Cymraeg', 'YES'),
  ('nld-nwf', 'West Flemish', 'Fflemeg Gorllewinol', 'YES'),
  ('wol', 'Wolof', 'Wolof', 'YES'),
  ('xho', 'Xhosa', 'Xhosa', 'YES'),
  ('yid', 'Yiddish', 'Iddew-Almaeneg', 'YES'),
  ('yor', 'Yoruba', 'Yoruba', 'YES'),
  ('zag', 'Zaghawa', 'Zaghawa', 'YES'),
  ('zza', 'Zaza', 'Zaza', 'YES'),
  ('zul', 'Zulu', 'Zulu', 'YES');

CREATE TABLE public.hearing_subchannel
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  parent_category VARCHAR(64),
  parent_key VARCHAR(64),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.hearing_subchannel (key, value_en, value_cy, parent_category, parent_key, active)
VALUES
  ('INTER', 'In Person', 'Yn Bersonol', 'HearingChannel', 'INTER', 'YES'),
  ('NA', 'Not in Attendance', 'Ddim yn Bresennol', 'HearingChannel', 'NA', 'YES'),
  ('VIDPVL', 'Prison Video', 'Cyswllt Fideo â Charchar', 'HearingChannel', 'VID', 'YES'),
  ('TELCVP', 'Telephone - CVP', 'Dros y Ffôn - CVP', 'HearingChannel', 'TEL', 'YES'),
  ('TELOTHER', 'Telephone - Other', 'Dros y Ffôn - Arall', 'HearingChannel', 'TEL', 'YES'),
  ('TELSKYP', 'Telephone - Skype', 'Dros y Ffôn - Skype', 'HearingChannel', 'TEL', 'YES'),
  ('VIDCVP', 'Video - CVP', 'Drwy Fideo - CVP', 'HearingChannel', 'VID', 'YES'),
  ('VIDOTHER', 'Video - Other', 'Drwy Fideo - Arall', 'HearingChannel', 'VID', 'YES'),
  ('VIDSKYPE', 'Video - Skype', 'Drwy Fideo - Skype', 'HearingChannel', 'VID', 'YES'),
  ('VIDTEAMS', 'Video - Teams', 'Drwy Fideo - Teams', 'HearingChannel', 'VID', 'YES'),
  ('VIDVHS', 'Video - Video Hearing Service', 'Drwy Fideo - Gwasanaeth Gwrandawiadau Fideo', 'HearingChannel', 'VID', 'YES');

CREATE TABLE public.custody_status
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.custody_status (key, value_en, active)
VALUES
  ('C', 'Custody', 'YES'),
  ('D', 'Detention', 'YES'),
  ('A', 'Not applicable', 'YES');

CREATE TABLE public.party_relationship_type
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO public.party_relationship_type (key, value_en, value_cy, active)
VALUES
  ('12', 'Interpreter', 'Cyfieithydd ar y Pryd', 'YES'),
  ('11', 'Solicitor', 'Cyfreithiwr', 'YES');
