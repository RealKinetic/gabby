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
package com.realkinetic.app.gabby.repository.downstream.google.pubsub;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.Sleeper;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * RetryHttpInitializerWrapper will automatically retry upon RPC
 * failures, preserving the auto-refresh behavior of the Google
 * Credentials.
 */
public class RetryHttpInitializerWrapper implements HttpRequestInitializer {

    /**
     * A private logger.
     */
    private static final Logger LOG =
            Logger.getLogger(RetryHttpInitializerWrapper.class.getName());

    /**
     * Intercepts the request for filling in the "Authorization"
     * header field, as well as recovering from certain unsuccessful
     * error codes wherein the Credential must refresh its token for a
     * retry.
     */
    private final Credential wrappedCredential;

    /**
     * A sleeper; you can replace it with a mock in your test.
     */
    private final Sleeper sleeper;

    /**
     * Defines the readTimeout of an http request in milliseconds.
     */
    private final long readTimeout;

    /**
     * A constructor.
     *
     * @param wrappedCredential Credential which will be wrapped and
     * used for providing auth header.
     */
    public RetryHttpInitializerWrapper(final Credential wrappedCredential, final long readTimeout) {
        this(wrappedCredential, readTimeout, Sleeper.DEFAULT);
    }

    /**
     * A protected constructor only for testing.
     *
     * @param wrappedCredential Credential which will be wrapped and
     * used for providing auth header.
     * @param sleeper Sleeper for easy testing.
     */
    RetryHttpInitializerWrapper(
            final Credential wrappedCredential, final long readTimeout, final Sleeper sleeper) {
        this.wrappedCredential = Preconditions.checkNotNull(wrappedCredential);
        this.sleeper = sleeper;
        this.readTimeout = readTimeout * 1000;
    }

    /**
     * Initializes the given request.
     */
    @Override
    public final void initialize(final HttpRequest request) {
        request.setReadTimeout((int) this.readTimeout); // 2 minutes read timeout
        final HttpUnsuccessfulResponseHandler backoffHandler =
                new HttpBackOffUnsuccessfulResponseHandler(
                        new ExponentialBackOff())
                        .setSleeper(sleeper);
        request.setInterceptor(wrappedCredential);
        request.setUnsuccessfulResponseHandler(
                new HttpUnsuccessfulResponseHandler() {
                    @Override
                    public boolean handleResponse(
                            final HttpRequest request,
                            final HttpResponse response,
                            final boolean supportsRetry) throws IOException {
                        if (wrappedCredential.handleResponse(
                                request, response, supportsRetry)) {
                            // If credential decides it can handle it,
                            // the return code or message indicated
                            // something specific to authentication,
                            // and no backoff is desired.
                            return true;
                        } else if (backoffHandler.handleResponse(
                                request, response, supportsRetry)) {
                            // Otherwise, we defer to the judgement of
                            // our internal backoff handler.
                            LOG.info("Retrying "
                                    + request.getUrl().toString());
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
        request.setIOExceptionHandler(
                new HttpBackOffIOExceptionHandler(new ExponentialBackOff())
                        .setSleeper(sleeper));
    }
}
