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