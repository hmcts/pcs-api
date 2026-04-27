ALTER TABLE tenancy_licence
  ADD COLUMN has_copy_of_tenancy_licence YES_NO,
  ADD COLUMN reasons_for_no_tenancy_licence VARCHAR(500);
