-- Заполняем таблицу genres (если записи ещё отсутствуют)
INSERT INTO genres (name)
SELECT 'Комедия' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Комедия');
INSERT INTO genres (name)
SELECT 'Драма' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Драма');
INSERT INTO genres (name)
SELECT 'Мультфильм' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Мультфильм');
INSERT INTO genres (name)
SELECT 'Триллер' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Триллер');
INSERT INTO genres (name)
SELECT 'Документальный' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Документальный');
INSERT INTO genres (name)
SELECT 'Боевик' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Боевик');

-- Заполняем таблицу rating_mpaa (если записи ещё отсутствуют)
INSERT INTO rating_mpaa (name)
SELECT 'G' WHERE NOT EXISTS (SELECT 1 FROM rating_mpaa WHERE name = 'G');
INSERT INTO rating_mpaa (name)
SELECT 'PG' WHERE NOT EXISTS (SELECT 1 FROM rating_mpaa WHERE name = 'PG');
INSERT INTO rating_mpaa (name)
SELECT 'PG-13' WHERE NOT EXISTS (SELECT 1 FROM rating_mpaa WHERE name = 'PG-13');
INSERT INTO rating_mpaa (name)
SELECT 'R' WHERE NOT EXISTS (SELECT 1 FROM rating_mpaa WHERE name = 'R');
INSERT INTO rating_mpaa (name)
SELECT 'NC-17' WHERE NOT EXISTS (SELECT 1 FROM rating_mpaa WHERE name = 'NC-17');

-- Заполняем таблицу event_types (если записи ещё отсутствуют)
INSERT INTO event_types (id, name)
SELECT 1, 'LIKE' WHERE NOT EXISTS (SELECT 1 FROM event_types WHERE id = 1);
INSERT INTO event_types (id, name)
SELECT 2, 'REVIEW' WHERE NOT EXISTS (SELECT 1 FROM event_types WHERE id = 2);
INSERT INTO event_types (id, name)
SELECT 3, 'FRIEND' WHERE NOT EXISTS (SELECT 1 FROM event_types WHERE id = 3);

-- Заполняем таблицу operation_types (если записи ещё отсутствуют)
INSERT INTO operation_types (id, name)
SELECT 1, 'ADD' WHERE NOT EXISTS (SELECT 1 FROM operation_types WHERE id = 1);
INSERT INTO operation_types (id, name)
SELECT 2, 'UPDATE' WHERE NOT EXISTS (SELECT 1 FROM operation_types WHERE id = 2);
INSERT INTO operation_types (id, name)
SELECT 3, 'REMOVE' WHERE NOT EXISTS (SELECT 1 FROM operation_types WHERE id = 3);