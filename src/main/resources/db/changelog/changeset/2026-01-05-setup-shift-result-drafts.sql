create table shift_result_drafts
(
    id         uuid primary key default gen_random_uuid(),
    session_id uuid not null unique references shift_sessions (id) on delete cascade,
    date       date not null
);

create table payment_drafts
(
    id                     uuid primary key default gen_random_uuid(),
    shift_result_draft_id  uuid not null references shift_result_drafts (id) on delete cascade,
    employee_id            uuid not null references employees (id) on delete cascade,
    percent_from_revenue   integer not null,
    tips                   integer not null,
    work_seconds           bigint not null,
    unique (shift_result_draft_id, employee_id)
);
