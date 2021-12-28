package com.itau.fj1.backoffice;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class App implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final java.util.Random rand = new Random();

    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        Map<String, Object> response = new HashMap<>(event);

        if (response.get("id") == null) {
            response.put("id", UUID.randomUUID().toString());
        }
        response.put("isRejected", rand.nextBoolean());
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        return response;
    }
}
