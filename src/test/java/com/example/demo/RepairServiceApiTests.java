package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты REST API и бизнес-операций (H2 в режиме PostgreSQL).
 * Каждый тест выполняется в транзакции и откатывается, поэтому тесты независимы.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepairServiceApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode body(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private long createUser() throws Exception {
        var result = mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Тест Клиент\",\"email\":\"client" + System.nanoTime() + "@mail.ru\"}"))
                .andExpect(status().isCreated());
        return body(result).get("id").asLong();
    }

    private long createDevice(long ownerId) throws Exception {
        var result = mvc.perform(post("/api/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":" + ownerId + ",\"type\":\"Стиральная машина\","
                        + "\"model\":\"LG F-1096ND\",\"serialNumber\":\"SN-" + System.nanoTime() + "\"}"))
                .andExpect(status().isCreated());
        return body(result).get("id").asLong();
    }

    private long createPart(int stock) throws Exception {
        var result = mvc.perform(post("/api/spare-parts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Подшипник барабана\",\"price\":1500.00,"
                        + "\"stockQuantity\":" + stock + "}"))
                .andExpect(status().isCreated());
        return body(result).get("id").asLong();
    }

    private long createOrder(long deviceId) throws Exception {
        var result = mvc.perform(post("/api/repair-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deviceId\":" + deviceId + ",\"fault\":\"Не крутит барабан\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.cost").value(0));
        return body(result).get("id").asLong();
    }

    private long addItem(long orderId, long partId, int quantity) throws Exception {
        var result = mvc.perform(post("/api/repair-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + orderId + ",\"sparePartId\":" + partId
                        + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isCreated());
        return body(result).get("id").asLong();
    }

    @Test
    void fullBusinessScenario() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long part = createPart(10);
        long order = createOrder(device);

        // 1. Добавление запчасти в наряд с резервированием
        long item = addItem(order, part, 3);
        mvc.perform(get("/api/spare-parts/" + part))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(3));

        // 2. Приёмка в работу
        mvc.perform(post("/api/repair-orders/" + order + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 3. Списание запчасти со склада
        mvc.perform(post("/api/repair-items/" + item + "/use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageStatus").value("USED"));
        mvc.perform(get("/api/spare-parts/" + part))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(7))
                .andExpect(jsonPath("$.reservedQuantity").value(0));

        // 4. Завершение: стоимость = 1500 * 3 = 4500
        mvc.perform(post("/api/repair-orders/" + order + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.cost").value(4500.0));
    }

    @Test
    void addItem_insufficientStock_conflict() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long part = createPart(2);
        long order = createOrder(device);

        mvc.perform(post("/api/repair-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + order + ",\"sparePartId\":" + part + ",\"quantity\":5}"))
                .andExpect(status().isConflict());
    }

    @Test
    void addItem_toCompletedOrder_conflict() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long part = createPart(10);
        long order = createOrder(device);

        mvc.perform(post("/api/repair-orders/" + order + "/start")).andExpect(status().isOk());
        mvc.perform(post("/api/repair-orders/" + order + "/complete")).andExpect(status().isOk());

        mvc.perform(post("/api/repair-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + order + ",\"sparePartId\":" + part + ",\"quantity\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void startOrder_wrongStatus_conflict() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long order = createOrder(device);

        mvc.perform(post("/api/repair-orders/" + order + "/start")).andExpect(status().isOk());
        // Повторный старт из IN_PROGRESS невозможен
        mvc.perform(post("/api/repair-orders/" + order + "/start"))
                .andExpect(status().isConflict());
    }

    @Test
    void completeOrder_withReservedItems_conflict() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long part = createPart(10);
        long order = createOrder(device);

        addItem(order, part, 1);
        mvc.perform(post("/api/repair-orders/" + order + "/start")).andExpect(status().isOk());

        // Незакрытый резерв запрещает завершение
        mvc.perform(post("/api/repair-orders/" + order + "/complete"))
                .andExpect(status().isConflict());
    }

    @Test
    void waitForParts_requiresReservedItems_conflict() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long order = createOrder(device);

        mvc.perform(post("/api/repair-orders/" + order + "/start")).andExpect(status().isOk());
        // Нет зарезервированных позиций -> перевод в WAITING_FOR_PARTS невозможен
        mvc.perform(post("/api/repair-orders/" + order + "/wait-for-parts"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelOrder_releasesReservations() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long part = createPart(10);
        long order = createOrder(device);

        addItem(order, part, 4);
        mvc.perform(post("/api/repair-orders/" + order + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mvc.perform(get("/api/spare-parts/" + part))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.stockQuantity").value(10));

        // Новая позиция в отменённый наряд не добавляется
        mvc.perform(post("/api/repair-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + order + ",\"sparePartId\":" + part + ",\"quantity\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void useItem_twice_conflict() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long part = createPart(10);
        long order = createOrder(device);

        long item = addItem(order, part, 1);
        mvc.perform(post("/api/repair-items/" + item + "/use")).andExpect(status().isOk());
        // Повторное списание невозможно
        mvc.perform(post("/api/repair-items/" + item + "/use"))
                .andExpect(status().isConflict());
    }

    @Test
    void patchItem_changeStatus_rejected() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long part = createPart(10);
        long order = createOrder(device);

        long item = addItem(order, part, 1);
        mvc.perform(patch("/api/repair-items/" + item)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usageStatus\":\"USED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchOrder_changeStatus_rejected() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long order = createOrder(device);

        mvc.perform(patch("/api/repair-orders/" + order)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchSparePart_changeReserved_rejected() throws Exception {
        long part = createPart(10);
        mvc.perform(patch("/api/spare-parts/" + part)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservedQuantity\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_cascadesToDevicesAndOrders() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long part = createPart(10);
        long order = createOrder(device);
        addItem(order, part, 2);

        mvc.perform(delete("/api/users/" + user)).andExpect(status().isNoContent());

        mvc.perform(get("/api/devices/" + device)).andExpect(status().isNotFound());
        mvc.perform(get("/api/repair-orders/" + order)).andExpect(status().isNotFound());
        // Резерв освобождён каскадным удалением
        mvc.perform(get("/api/spare-parts/" + part))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(0));
    }

    @Test
    void duplicateSerialNumber_conflict() throws Exception {
        long user = createUser();
        String serial = "SN-DUP-" + System.nanoTime();
        String json = "{\"ownerId\":" + user + ",\"type\":\"Телевизор\",\"model\":\"LG 55\","
                + "\"serialNumber\":\"" + serial + "\"}";
        mvc.perform(post("/api/devices").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/devices").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void itemPriceFixedAtCreation() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long part = createPart(10);
        long order = createOrder(device);

        long item = addItem(order, part, 2);
        // Меняем цену запчасти после добавления позиции
        mvc.perform(patch("/api/spare-parts/" + part)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":9999.00}")).andExpect(status().isOk());
        mvc.perform(get("/api/repair-items/" + item))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fixedPrice").value(1500.0));
    }

    @Test
    void cancelItem_returnsReservation() throws Exception {
        long user = createUser();
        long device = createDevice(user);
        long part = createPart(10);
        long order = createOrder(device);

        long item = addItem(order, part, 3);
        mvc.perform(post("/api/repair-items/" + item + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageStatus").value("CANCELLED"));

        mvc.perform(get("/api/spare-parts/" + part))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(0));
    }
}
