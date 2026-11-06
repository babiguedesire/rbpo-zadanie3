-- ============================================================
-- Задание 3: схема БД "Ремонт бытовой техники" (Вариант 4)
-- Скрипт выполняется от имени repair_admin при первом запуске контейнера.
-- ============================================================

-- Клиенты (владельцы устройств)
CREATE TABLE users (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE
);

-- Устройства
CREATE TABLE devices (
    id            BIGSERIAL PRIMARY KEY,
    owner_id      BIGINT NOT NULL REFERENCES users (id),
    type          VARCHAR(100) NOT NULL,
    model         VARCHAR(150) NOT NULL,
    serial_number VARCHAR(100) NOT NULL UNIQUE,
    CONSTRAINT fk_device_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_devices_owner ON devices (owner_id);

-- Запчасти на складе
CREATE TABLE spare_parts (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    price            NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    stock_quantity   INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0 AND reserved_quantity <= stock_quantity)
);

-- Наряды на ремонт
CREATE TABLE repair_orders (
    id         BIGSERIAL PRIMARY KEY,
    device_id  BIGINT NOT NULL REFERENCES devices (id),
    fault      TEXT NOT NULL,
    status     VARCHAR(30) NOT NULL DEFAULT 'NEW'
               CHECK (status IN ('NEW', 'IN_PROGRESS', 'WAITING_FOR_PARTS', 'COMPLETED', 'CANCELLED')),
    cost       NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (cost >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_order_device FOREIGN KEY (device_id) REFERENCES devices (id)
);

CREATE INDEX idx_orders_device ON repair_orders (device_id);

-- Позиции наряда (запчасть + количество + зафиксированная цена)
CREATE TABLE repair_items (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL REFERENCES repair_orders (id),
    spare_part_id BIGINT NOT NULL REFERENCES spare_parts (id),
    quantity      INT NOT NULL CHECK (quantity > 0),
    fixed_price   NUMERIC(12, 2) NOT NULL CHECK (fixed_price >= 0),
    usage_status  VARCHAR(20) NOT NULL DEFAULT 'RESERVED'
                  CHECK (usage_status IN ('RESERVED', 'USED', 'CANCELLED')),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES repair_orders (id),
    CONSTRAINT fk_item_part FOREIGN KEY (spare_part_id) REFERENCES spare_parts (id)
);

CREATE INDEX idx_items_order ON repair_items (order_id);
CREATE INDEX idx_items_part ON repair_items (spare_part_id);
