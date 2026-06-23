ALTER TABLE claim
  ADD COLUMN energy_performance_certificate_provided YES_NO,
  ADD COLUMN gas_safety_report_provided YES_NO,
  ADD COLUMN electrical_installation_condition_provided YES_NO,
  ADD COLUMN no_energy_performance_certificate_reason VARCHAR(500),
  ADD COLUMN no_gas_safety_report_reason VARCHAR(500),
  ADD COLUMN no_electrical_installation_condition_reason VARCHAR(500);
