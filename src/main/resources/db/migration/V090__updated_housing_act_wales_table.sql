ALTER TABLE housing_act_wales
  ADD COLUMN is_exempt_landlord YES_NO,
  DROP COLUMN registered,
  DROP COLUMN registration_number,
  DROP COLUMN licensed,
  DROP COLUMN licence_number,
  DROP COLUMN agent_appointed,
  DROP COLUMN agent_first_name,
  DROP COLUMN agent_last_name,
  DROP COLUMN agent_licence_number,
  DROP COLUMN agent_appointment_date;
