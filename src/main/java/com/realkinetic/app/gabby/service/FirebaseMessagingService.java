package com.realkinetic.app.gabby.service;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Key;
import com.realkinetic.app.gabby.model.dto.CreateSubscriptionRequest;
import io.reactivex.Observable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class FirebaseMessagingService implements MessagingService {
    private static final Logger log = Logger.getLogger(FirebaseMessagingService.class.getName());
    private static String SERVER_KEY = "AAAAXbExgaY:APA91bE_kqbVngqsLi7sdT3M36oypgdBz4ob7n5yLxTTX8D89rPMK0Rhyez97Q7YjNGd_yMXg3FcXN6ggEOsOfTKawiuuSX0LtnXwZ_nmXnWLOYhvBliYkDZQb2t6pts7hrAm3qEPLxT";

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new GsonFactory();

    public static class Message extends GenericJson {
         @Key
         private String to;

         public String getTo() {
            return this.to;
         }

         @Key
         private Map<String, String> data;

         public Map<String, String> getData() {
            return this.data;
         }
    }

    public static class FirebaseUrl extends GenericUrl {
        private static String BASE_URL = "https://fcm.googleapis.com/fcm/send";

        public FirebaseUrl(String encodedUrl) {
            super(encodedUrl);
        }

        public static FirebaseUrl sendMessage() {
            return new FirebaseUrl(
                BASE_URL
            );
        }
    }

    private final HttpRequestFactory requestFactory;

    public FirebaseMessagingService() {
        this.requestFactory =
            HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) {
                    request.setParser(new JsonObjectParser(JSON_FACTORY));
                }
            });
    }

    @Override
    public String createSubscription(CreateSubscriptionRequest request) throws IOException {
        GenericUrl url
                = new GenericUrl(
                        new URL("https://iid.googleapis.com/iid/v1/"+request.getToken()+"/rel/topics/"+ request.getName())
        );
        HttpRequest hr = this.requestFactory.buildPostRequest(url,  new EmptyContent());
        hr.setHeaders(this.getHeaders());
        HttpResponse response = hr.execute();
        return response.getStatusMessage();
    }

    @Override
    public void deleteSubscription(String subscriptionName) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public Observable<Iterable<com.realkinetic.app.gabby.model.dto.Message>> pull(String subscriptionName) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void acknowledge(String subscriptionName, Iterable<String> ackIds) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void send(String topic, String message) throws IOException {
        FirebaseUrl url = FirebaseUrl.sendMessage();
        Map<String, String> data = new HashMap<>();
        data.put("message", message);
        Message json = new Message();
        json.to = "/topics/" + topic;
        json.data = data;
        final HttpContent content = new JsonHttpContent(JSON_FACTORY, json);
        HttpRequest request = this.requestFactory.buildPostRequest(url, content);
        request.setHeaders(this.getHeaders());
        request.execute(); // TODO: need to check response here
    }

    private HttpHeaders getHeaders() {
        return new HttpHeaders()
                .setContentType("application/json")
                .setAuthorization("key=" + SERVER_KEY);
    }
}
