-- ============================================================
-- Тестовые данные (выполняются от имени администратора)
-- ============================================================

INSERT INTO users (name, email) VALUES
    ('Иван Петров', 'ivan.petrov@mail.ru'),
    ('Анна Смирнова', 'anna.smirnova@mail.ru'),
    ('Сергей Кузнецов', 'sergey.kuznetsov@mail.ru');

INSERT INTO devices (owner_id, type, model, serial_number) VALUES
    (1, 'Стиральная машина', 'LG F-1096ND', 'SN-LG-1001'),
    (1, 'Холодильник', 'Samsung RL-44HE', 'SN-SS-2002'),
    (2, 'Телевизор', 'Philips 50PUS', 'SN-PH-3003'),
    (2, 'Пылесос', 'Dyson V8', 'SN-DY-4004'),
    (3, 'Посудомоечная машина', 'Bosch SMS25AW', 'SN-BS-5005');

INSERT INTO spare_parts (name, price, stock_quantity, reserved_quantity) VALUES
    ('Подшипник барабана', 1500.00, 10, 2),
    ('ТЭН стиральной машины', 2500.00, 8, 0),
    ('Компрессор холодильника', 12000.00, 3, 1),
    ('Матрица телевизора', 8000.00, 5, 3),
    ('Мотор пылесоса', 3000.00, 4, 0);

-- Резервы в spare_parts соответствуют RESERVED-позициям ниже:
-- подшипник 2, компрессор 1, матрица 3.
INSERT INTO repair_orders (device_id, fault, status, cost, created_at) VALUES
    (1, 'Не крутит барабан, гудит подшипник', 'IN_PROGRESS', 0, now()),
    (2, 'Не морозит, компрессор не запускается', 'WAITING_FOR_PARTS', 0, now()),
    (3, 'Нет изображения, есть звук', 'NEW', 0, now()),
    (5, 'Не сливает воду', 'COMPLETED', 2500.00, now());

INSERT INTO repair_items (order_id, spare_part_id, quantity, fixed_price, usage_status) VALUES
    (1, 1, 2, 1500.00, 'RESERVED'),
    (2, 3, 1, 12000.00, 'RESERVED'),
    (2, 4, 3, 8000.00, 'RESERVED'),
    (4, 2, 1, 2500.00, 'USED');
