/*
Copyright 2017 Real Kinetic LLC

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
*/
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
            return compound.split(":")[part];
        } catch (Exception e) {
            throw new InvalidAckIdException(
                    "invalid ackid: " + ackId,
                    e
            );
        }
    }
}
