package com.realkinetic.app.gabby.util;

import java.util.Base64;
import java.util.Random;
import java.util.UUID;

import com.realkinetic.app.gabby.model.error.*;

public class IdUtil {
    public static String generateId() {
        String name = UUID.randomUUID().toString().replaceAll("[\\s\\-()]", "");
        // uuids can start with a number, sub names must start with a letter
        Random r = new Random();
        char c = (char) (r.nextInt(6) + 'a');
        return c + name.substring(1);
    }

    public static String generateAckId(String subscriptionId, String messageId) {
        String ackId = subscriptionId + ":" + messageId;
        return Base64.getUrlEncoder().encodeToString(ackId.getBytes());
    }

    public static String getSubscriptionIdFromAckId(String ackId) throws InvalidAckIdException {
        return splitAckId(ackId, 0);
    }

    public static String getMessageIdFromAckId(String ackId) throws InvalidAckIdException {
        return splitAckId(ackId, 1);
    }

    private static String splitAckId(String ackId, int part) throws InvalidAckIdException {
        try {
            String compound = new String(Base64.getUrlDecoder().decode(ackId));
            return compound.split(":")[0];
        } catch (Exception e) {
            throw new InvalidAckIdException(
                    "invalid ackid: " + ackId,
                    e
            );
        }
    }
}
