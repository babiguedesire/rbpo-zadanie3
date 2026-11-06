package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверка соответствия JPA-сущностей SQL-схеме из db/init/01-schema.sql:
 * Hibernate работает в режиме ddl-auto=validate на схеме, созданной SQL-скриптами,
 * затем API читает тестовые данные из db/init/03-seed.sql.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:repairvalidate;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:sql/schema.sql",
        "spring.sql.init.data-locations=classpath:sql/seed.sql"
})
@AutoConfigureMockMvc
@Transactional
class SchemaValidationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void entitiesMatchSqlSchema() {
        // Схема прошла валидацию Hibernate при старте контекста — тест уже успешен.
        // Дополнительно проверяем, что тестовые данные читаются через JPA.
    }

    @Test
    void seedDataIsReadable() throws Exception {
        // 3 пользователя, 5 устройств, 5 запчастей, 4 наряда из 03-seed.sql
        mvc.perform(get("/api/users")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/devices")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/api/spare-parts")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/api/repair-orders")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        // Резерв подшипника (id=1) = 2 из RESERVED-позиции наряда 1
        mvc.perform(get("/api/spare-parts/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(2));
        // Завершённый наряд 4 со стоимостью 2500
        mvc.perform(get("/api/repair-orders/4")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.cost").value(2500.0));
    }
}
