SET search_path TO expenses, public;

INSERT INTO user_profile (id, username, password) VALUES (nextval('hibernate_sequence'), 'employee1@abstratt.com', 'pass');
INSERT INTO user_profile (id, username, password) VALUES (nextval('hibernate_sequence'), 'employee2@abstratt.com', 'pass');
INSERT INTO user_profile (id, username, password) VALUES (nextval('hibernate_sequence'), 'approver1@abstratt.com', 'pass');
INSERT INTO user_profile (id, username, password) VALUES (nextval('hibernate_sequence'), 'approver2@abstratt.com', 'pass');
INSERT INTO user_profile (id, username, password) VALUES (nextval('hibernate_sequence'), 'admin1@abstratt.com', 'pass');
INSERT INTO user_profile (id, username, password) VALUES (nextval('hibernate_sequence'), 'admin2@abstratt.com', 'pass');

INSERT INTO category (id, name) VALUES (nextval('hibernate_sequence'), 'Meal');
INSERT INTO category (id, name) VALUES (nextval('hibernate_sequence'), 'Lodging');
INSERT INTO category (id, name) VALUES (nextval('hibernate_sequence'), 'Transportation');

INSERT INTO person (id, name, person_kind, user_id) VALUES (nextval('hibernate_sequence'), 'John', 'Employee', (SELECT id FROM user_profile WHERE username = 'employee1@abstratt.com'));
INSERT INTO person (id, name, person_kind, user_id) VALUES (nextval('hibernate_sequence'), 'Mary', 'Employee', (SELECT id FROM user_profile WHERE username = 'employee2@abstratt.com'));
INSERT INTO person (id, name, person_kind, user_id) VALUES (nextval('hibernate_sequence'), 'Admin 1', 'Administrator', (SELECT id FROM user_profile WHERE username = 'admin1@abstratt.com'));
INSERT INTO person (id, name, person_kind, user_id) VALUES (nextval('hibernate_sequence'), 'Admin 2', 'Administrator', (SELECT id FROM user_profile WHERE username = 'admin2@abstratt.com'));
INSERT INTO person (id, name, person_kind, user_id) VALUES (nextval('hibernate_sequence'), 'Peter', 'Approver', (SELECT id FROM user_profile WHERE username = 'approver1@abstratt.com'));
INSERT INTO person (id, name, person_kind, user_id) VALUES (nextval('hibernate_sequence'), 'Sarah', 'Approver', (SELECT id FROM user_profile WHERE username = 'approver2@abstratt.com'));

INSERT INTO employee (id) VALUES ((SELECT id FROM person WHERE name = 'John'));
INSERT INTO employee (id) VALUES ((SELECT id FROM person WHERE name = 'Mary'));
INSERT INTO employee (id) VALUES ((SELECT id FROM person WHERE name = 'Peter'));
INSERT INTO employee (id) VALUES ((SELECT id FROM person WHERE name = 'Sarah'));

INSERT INTO expense (id, description, status, amount, "date", processed, rejection_reason, category_id, employee_id) VALUES (nextval('hibernate_sequence'), 'Cab airport->downtown', 'Approved', 40, DATE '2016-05-13', null, null, (SELECT id FROM category WHERE name = 'Transportation'), (SELECT id FROM person WHERE name = 'John'));
INSERT INTO expense (id, description, status, amount, "date", processed, rejection_reason, category_id, employee_id) VALUES (nextval('hibernate_sequence'), 'Lunch in LA', 'Submitted', 35.75, DATE '2016-06-14', null, null, (SELECT id FROM category WHERE name = 'Meal'), (SELECT id FROM person WHERE name = 'John'));
INSERT INTO expense (id, description, status, amount, "date", processed, rejection_reason, category_id, employee_id) VALUES (nextval('hibernate_sequence'), 'YVR/LAX', 'Draft', 190.5, DATE '2016-06-13', null, null, (SELECT id FROM category WHERE name = 'Transportation'), (SELECT id FROM person WHERE name = 'John'));
INSERT INTO expense (id, description, status, amount, "date", processed, rejection_reason, category_id, employee_id) VALUES (nextval('hibernate_sequence'), 'Lunch at subway', 'Rejected', 20.5, DATE '2016-06-13', null, null, (SELECT id FROM category WHERE name = 'Meal'), (SELECT id FROM person WHERE name = 'Mary'));
INSERT INTO expense (id, description, status, amount, "date", processed, rejection_reason, category_id, employee_id) VALUES (nextval('hibernate_sequence'), 'Hilton LA downtown', 'Approved', 304.5, DATE '2016-06-13', null, null, (SELECT id FROM category WHERE name = 'Lodging'), (SELECT id FROM person WHERE name = 'Sarah'));

INSERT INTO approver (id) VALUES ((SELECT id FROM person WHERE name = 'Peter'));
INSERT INTO approver (id) VALUES ((SELECT id FROM person WHERE name = 'Sarah'));

INSERT INTO administrator (id) VALUES ((SELECT id FROM person WHERE name = 'Admin 1'));
INSERT INTO administrator (id) VALUES ((SELECT id FROM person WHERE name = 'Admin 2'));
