package com.realkinetic.app.gabby.util;

import java.util.Random;
import java.util.UUID;

public class IdUtil {
    public static String generateId() {
        String name = UUID.randomUUID().toString().replaceAll("[\\s\\-()]", "");
        // uuids can start with a number, sub names must start with a letter
        Random r = new Random();
        char c = (char) (r.nextInt(6) + 'a');
        return c + name.substring(1);
    }
}
