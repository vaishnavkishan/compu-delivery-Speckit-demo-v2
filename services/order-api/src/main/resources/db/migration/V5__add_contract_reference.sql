ALTER TABLE enterprise_client ADD COLUMN contract_reference VARCHAR(64);

UPDATE enterprise_client SET contract_reference = 'EC-8942' WHERE client_id = 'ACME-001';
UPDATE enterprise_client SET contract_reference = 'EC-3317' WHERE client_id = 'GLOBEX-002';
UPDATE enterprise_client SET contract_reference = 'EC-0000' WHERE client_id = 'NOTERMS-003';
UPDATE enterprise_client SET contract_reference = 'EC-1204' WHERE client_id = 'EXPIRED-004';
UPDATE enterprise_client SET contract_reference = 'EC-7765' WHERE client_id = 'AMBIGUOUS-005';
