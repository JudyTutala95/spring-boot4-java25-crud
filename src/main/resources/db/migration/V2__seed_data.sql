-- Flyway-migration V2: exempeldata så att API:t har något att visa direkt.

INSERT INTO author (name, birth_year, created_at, updated_at) VALUES
    ('Astrid Lindgren', 1907, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Joshua Bloch',    1961, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Ursula K. Le Guin', 1929, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO book (title, isbn, price, published_year, genre, author_id, created_at, updated_at) VALUES
    ('Pippi Långstrump',          '9789129697711', 129.00, 1945, 'CHILDREN',   1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Bröderna Lejonhjärta',      '9789129688313', 149.00, 1973, 'CHILDREN',   1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Effective Java',            '9780134685991', 499.00, 2018, 'TECHNOLOGY', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('A Wizard of Earthsea',      '9780547773742', 189.00, 1968, 'FANTASY',    3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('The Left Hand of Darkness', '9780441478125', 179.00, 1969, 'SCIENCE_FICTION', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
