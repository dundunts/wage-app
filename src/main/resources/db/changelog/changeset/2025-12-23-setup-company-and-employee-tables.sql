create table companies
(
    id                       uuid primary key default gen_random_uuid(),
    title                    varchar(255) not null unique,
    k_from_revenue           integer      not null,
    default_shift_start_time varchar(5)   not null
);

create table employees
(
    id          uuid primary key default gen_random_uuid(),
    user_id     varchar(255) unique,
    first_name  varchar(255) not null,
    last_name   varchar(255) not null,
    patronymic  varchar(255) not null,
    simple_name varchar(255),
    position    varchar(255) not null
);

create table employees_companies
(
    id          uuid primary key default gen_random_uuid(),
    employee_id uuid not null references employees (id) on delete cascade,
    company_id  uuid not null references companies (id) on delete cascade
);
