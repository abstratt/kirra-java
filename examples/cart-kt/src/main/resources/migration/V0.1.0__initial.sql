CREATE SCHEMA IF NOT EXISTS expenses;

SET search_path TO expenses, public;

CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 1;

CREATE TABLE user_profile (
    id bigint NOT NULL PRIMARY KEY,
    username varchar NOT NULL CONSTRAINT user_profile_username_uk UNIQUE,
    password varchar NOT NULL
);

CREATE TABLE person (
    id bigint NOT NULL PRIMARY KEY,
    name varchar NOT NULL CONSTRAINT person_name_uk UNIQUE,
    user_id bigint NOT NULL,
    person_kind varchar NOT NULL
);


CREATE TABLE employee (
    id bigint NOT NULL CONSTRAINT employee_person_uk UNIQUE
);

CREATE TABLE administrator (
    id bigint NOT NULL CONSTRAINT admin_person_uk UNIQUE
);

CREATE TABLE approver (
    id bigint NOT NULL CONSTRAINT approver_employee_uk UNIQUE
);


CREATE TABLE expense (
    id bigint NOT NULL PRIMARY KEY,
    description varchar NOT NULL,
    amount numeric NOT NULL,
    "date" date,
    processed date,
    status varchar NOT NULL,
    rejection_reason varchar,
    category_id bigint NOT NULL,
    employee_id bigint NOT NULL
);

CREATE TABLE category (
    id bigint NOT NULL PRIMARY KEY,
    name varchar NOT NULL CONSTRAINT category_name_uk UNIQUE,
    enabled boolean DEFAULT true
);

ALTER TABLE person
    ADD CONSTRAINT person_user_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);

ALTER TABLE employee
    ADD CONSTRAINT employee_person_fk FOREIGN KEY (id) REFERENCES person(id);

ALTER TABLE approver
    ADD CONSTRAINT approver_employeee_fk FOREIGN KEY (id) REFERENCES employee(id);

ALTER TABLE administrator
    ADD CONSTRAINT admin_person_fk FOREIGN KEY (id) REFERENCES person(id);

ALTER TABLE expense
    ADD CONSTRAINT expense_category_fk FOREIGN KEY (category_id) REFERENCES category(id);

ALTER TABLE expense
    ADD CONSTRAINT expense_employee_fk FOREIGN KEY (employee_id) REFERENCES employee(id);

