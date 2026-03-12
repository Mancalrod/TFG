package com.tfg.gestionentregables;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class CheckDBTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testQueryMaterial18() {
        System.out.println("=========================================");
        System.out.println("QUERYING MATERIAL 18");
        List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT id, nombre, ruta FROM materiales WHERE id = 18");
        for (Map<String, Object> row : result) {
            System.out.println("Row: " + row);
        }
        System.out.println("=========================================");
    }
}
