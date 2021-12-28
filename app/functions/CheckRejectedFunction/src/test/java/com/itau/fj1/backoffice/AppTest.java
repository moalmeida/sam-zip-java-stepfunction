package com.itau.fj1.backoffice;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class AppTest {

    @Test
    public void successfulResponse() {
        App app = new App();

        int stockPrice = 30;

        Map<String, Object> event = new HashMap<>();
        event.put("stockPrice", stockPrice);

        Map<String, Object> result = app.handleRequest(event, null);

        assertNotNull(result.get("id"));
        assertNotNull(result.get("stockPrice"));
        assertNotNull(result.get("timestamp"));
    }
}
