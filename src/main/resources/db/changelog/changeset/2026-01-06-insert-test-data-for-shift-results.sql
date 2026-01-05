-- Shift result for CLOSED session

INSERT INTO shift_results (id, company_id, date, session_id, calculation_source)
VALUES ('34000000-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        '2026-01-01',
        '30000000-0000-0000-0000-000000000002',
        'CHECKPOINTS');

-- Payments per employee

INSERT INTO payments (id, shift_result_id, employee_id, percent_from_revenue, tips, work_seconds)
VALUES ('35000000-0000-0000-0000-000000000001',
        '34000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        15,
        500,
        28800),
       ('35000000-0000-0000-0000-000000000002',
        '34000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000002',
        15,
        400,
        25200);
