INSERT INTO enterprise_client (client_id, display_name) VALUES
    ('ACME-001', 'Acme Corporation'),
    ('GLOBEX-002', 'Globex Industries'),
    ('NOTERMS-003', 'Initech (No Terms on File)'),
    ('EXPIRED-004', 'Umbrella Logistics (Terms Expired)'),
    ('AMBIGUOUS-005', 'Hooli Manufacturing (Ambiguous Terms)');

INSERT INTO operator (operator_id, display_name) VALUES
    ('OPS-1', 'Fulfillment Operations');

-- ACME-001: valid, currently-effective 12.5% discount, open-ended.
INSERT INTO contract_discount_terms (id, client_id, discount_percentage, effective_from, effective_until) VALUES
    ('11111111-1111-1111-1111-111111111111', 'ACME-001', 12.50, now() - INTERVAL '365 days', NULL);

-- GLOBEX-002: valid, currently-effective 8% discount, open-ended (differs from ACME-001).
INSERT INTO contract_discount_terms (id, client_id, discount_percentage, effective_from, effective_until) VALUES
    ('22222222-2222-2222-2222-222222222222', 'GLOBEX-002', 8.00, now() - INTERVAL '365 days', NULL);

-- NOTERMS-003: intentionally no contract_discount_terms row at all (missing terms).

-- EXPIRED-004: one row whose effective_until is in the past.
INSERT INTO contract_discount_terms (id, client_id, discount_percentage, effective_from, effective_until) VALUES
    ('33333333-3333-3333-3333-333333333333', 'EXPIRED-004', 10.00, now() - INTERVAL '365 days', now() - INTERVAL '30 days');

-- AMBIGUOUS-005: two overlapping rows both effective right now (ambiguous match).
INSERT INTO contract_discount_terms (id, client_id, discount_percentage, effective_from, effective_until) VALUES
    ('44444444-4444-4444-4444-444444444444', 'AMBIGUOUS-005', 5.00, now() - INTERVAL '365 days', NULL),
    ('55555555-5555-5555-5555-555555555555', 'AMBIGUOUS-005', 15.00, now() - INTERVAL '180 days', NULL);
