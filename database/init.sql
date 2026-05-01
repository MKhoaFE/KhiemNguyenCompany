-- =============================
-- TABLES
-- =============================

CREATE TABLE IF NOT EXISTS card_types (
                                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                                          name TEXT NOT NULL,
                                          denomination INTEGER NOT NULL,
                                          default_price INTEGER NOT NULL,
                                          default_discount INTEGER NOT NULL,
                                          created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_entries (
                                                 id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                 date TEXT NOT NULL,
                                                 type TEXT NOT NULL,
                                                 card_type_id INTEGER NOT NULL,
                                                 quantity INTEGER NOT NULL,
                                                 price INTEGER NOT NULL,
                                                 discount_price INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
                                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                                      date TEXT NOT NULL,
                                      seller_name TEXT NOT NULL,
                                      created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
                                           id INTEGER PRIMARY KEY AUTOINCREMENT,
                                           order_id INTEGER NOT NULL,
                                           card_type_id INTEGER NOT NULL,
                                           quantity INTEGER NOT NULL,
                                           price INTEGER NOT NULL,
                                           discount_price INTEGER NOT NULL
);

-- =============================
-- SEED DATA
-- =============================

INSERT OR IGNORE INTO card_types (id, name, denomination, default_price, default_discount) VALUES
(1,'Viettel 10k',10000,10000,9000),
(2,'Viettel 20k',20000,20000,18000),
(3,'Viettel 50k',50000,50000,45000),
(4,'Viettel 100k',100000,100000,90000),
(5,'Viettel 200k',200000,200000,180000),
(6,'Viettel 500k',500000,500000,450000),
(7,'Mobi 10k',10000,10000,9000),
(8,'Mobi 20k',20000,20000,18000),
(9,'Mobi 50k',50000,50000,45000),
(10,'Mobi 100k',100000,100000,90000),
(11,'Mobi 200k',200000,200000,180000),
(12,'Mobi 500k',500000,500000,450000),
(13,'Vina 10k',10000,10000,9000),
(14,'Vina 20k',20000,20000,18000),
(15,'Vina 50k',50000,50000,45000),
(16,'Vina 100k',100000,100000,90000),
(17,'Vina 200k',200000,200000,180000),
(18,'Vina 500k',500000,500000,450000),
(19,'VietNamMobile 10k',10000,10000,9000),
(20,'VietNamMobile 20k',20000,20000,18000),
(21,'VietNamMobile 50k',50000,50000,45000),
(22,'VietNamMobile 100k',100000,100000,90000),
(23,'VietNamMobile 200k',200000,200000,180000),
(24,'VietNamMobile 500k',500000,500000,450000),
(25,'Arabica 0.5kg',250000,250000,250000),
(26,'Robusta 0.5kg',250000,250000,250000);