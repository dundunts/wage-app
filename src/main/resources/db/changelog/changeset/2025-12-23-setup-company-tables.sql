create table companies
(
    id                       uuid primary key default gen_random_uuid(),
    title                    varchar(255) not null unique,
    k_from_revenue           integer      not null,
    default_shift_start_time varchar(5)   not null
);

create table user_companies
(
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null,
    company_id uuid not null references companies (id) on delete cascade
);
