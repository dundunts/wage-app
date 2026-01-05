create table shift_sessions
(
    id               uuid primary key default gen_random_uuid(),
    company_id       uuid not null references companies (id) on delete cascade,
    status           varchar(50) not null,
    start_work_time  time not null,
    date             date not null
);

create table checkpoints
(
    id               uuid primary key default gen_random_uuid(),
    shift_session_id uuid not null references shift_sessions (id) on delete cascade,
    tips             integer not null,
    revenue          integer not null,
    date_time        timestamp not null,
    type             varchar(50) not null
);

create table checkpoints_employees
(
    id             uuid primary key default gen_random_uuid(),
    checkpoint_id  uuid not null references checkpoints (id) on delete cascade,
    employee_id    uuid not null references employees (id) on delete cascade,
    unique (checkpoint_id, employee_id)
);

create table checkpoint_metric_records
(
    id             uuid primary key default gen_random_uuid(),
    checkpoint_id  uuid not null references checkpoints (id) on delete cascade,
    label          varchar(255) not null,
    destination    varchar(50) not null,
    value          integer not null
);
