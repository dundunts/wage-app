DELETE FROM checkpoint_metric_records
WHERE id LIKE '33000000-%';

DELETE FROM checkpoints_employees
WHERE id LIKE '32000000-%';

DELETE FROM checkpoints
WHERE id LIKE '31000000-%';

DELETE FROM shift_sessions
WHERE id LIKE '30000000-%';
