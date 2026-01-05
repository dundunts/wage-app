create table shift_results
(
    id                  uuid primary key default gen_random_uuid(),
    company_id          uuid not null references companies (id) on delete cascade,
    date                date not null,
    session_id          uuid references shift_sessions (id) on delete set null,
    calculation_source  varchar(50) not null,
    unique (company_id, date)
);

create table payments
(
    id                   uuid primary key default gen_random_uuid(),
    shift_result_id      uuid not null references shift_results (id) on delete cascade,
    employee_id          uuid not null references employees (id) on delete cascade,
    percent_from_revenue integer not null,
    tips                 integer not null,
    work_seconds         bigint not null,
    unique (shift_result_id, employee_id)
);
