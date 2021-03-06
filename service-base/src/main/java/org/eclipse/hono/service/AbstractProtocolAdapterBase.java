/**
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bosch Software Innovations GmbH - initial creation
 */
package org.eclipse.hono.service;

import java.net.HttpURLConnection;
import java.util.Objects;

import org.eclipse.hono.client.HonoClient;
import org.eclipse.hono.client.MessageSender;
import org.eclipse.hono.client.RegistrationClient;
import org.eclipse.hono.config.ServiceConfigProperties;
import org.eclipse.hono.util.Constants;
import org.eclipse.hono.util.RegistrationConstants;
import org.eclipse.hono.util.RegistrationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonClientOptions;

/**
 * A base class for implementing protocol adapters.
 * <p>
 * Provides connections to device registration and telemetry and event endpoints.
 * 
 * @param <T> The type of configuration properties used by this service.
 */
public abstract class AbstractProtocolAdapterBase<T extends ServiceConfigProperties> extends AbstractServiceBase<T> {

    private HonoClient hono;
    private HonoClient registration;

    /**
     * Sets the client to use for connecting to the Hono server.
     * 
     * @param honoClient The client.
     * @throws NullPointerException if hono client is {@code null}.
     */
    @Autowired
    public final void setHonoClient(final HonoClient honoClient) {
        this.hono = Objects.requireNonNull(honoClient);
    }

    /**
     * Gets the client used for connecting to the Hono server.
     * 
     * @return The client.
     */
    public final HonoClient getHonoClient() {
        return hono;
    }

    /**
     * Sets the client to use for connecting to the Device Registration service.
     * <p>
     * If this property is not set then the Device Registration endpoint is assumed
     * to be exposed by the Hono server as well.
     * 
     * @param registrationServiceClient The client.
     * @throws NullPointerException if the client is {@code null}.
     */
    @Qualifier(RegistrationConstants.REGISTRATION_ENDPOINT)
    @Autowired(required = false)
    public final void setRegistrationServiceClient(final HonoClient registrationServiceClient) {
        this.registration = Objects.requireNonNull(registrationServiceClient);
    }

    /**
     * Gets the client used for connecting to the Device Registration service.
     * 
     * @return The client.
     */
    public final HonoClient getRegistrationServiceClient() {
        if (registration == null) {
            return hono;
        }
        return registration;
    }

    @Override
    public final void start(final Future<Void> startFuture) {
        if (hono == null) {
            startFuture.fail("Hono client must be set");
        } else {
            doStart(startFuture);
        }
    }

    /**
     * Subclasses should override this method to perform any work required on start-up of this protocol adapter.
     * <p>
     * This method is invoked by {@link #start()} as part of the startup process.
     *
     * @param startFuture The future to complete once start up is complete.
     */
    protected void doStart(final Future<Void> startFuture) {
        // should be overridden by subclasses
        startFuture.complete();
    }

    @Override
    public final void stop(Future<Void> stopFuture) {
        doStop(stopFuture);
    }

    /**
     * Subclasses should override this method to perform any work required before shutting down this protocol adapter.
     * <p>
     * This method is invoked by {@link #stop()} as part of the shutdown process.
     *
     * @param stopFuture The future to complete once shutdown is complete.
     */
    protected void doStop(final Future<Void> stopFuture) {
        // to be overridden by subclasses
        stopFuture.complete();
    }

    /**
     * Connects to the Hono server using the configured client.
     * 
     * @param connectHandler The handler to invoke with the outcome of the connection attempt.
     *                       If {@code null} and the connection attempt failed, this method
     *                       tries to re-connect until a connection is established.
     */
    protected final void connectToHono(final Handler<AsyncResult<HonoClient>> connectHandler) {

        if (hono == null) {
            if (connectHandler != null) {
                connectHandler.handle(Future.failedFuture("Hono client not set"));
            }
        } else if (hono.isConnected()) {
            LOG.debug("already connected to Hono server");
            if (connectHandler != null) {
                connectHandler.handle(Future.succeededFuture(hono));
            }
        } else {
            hono.connect(createClientOptions(), connectAttempt -> {
                if (connectHandler != null) {
                    connectHandler.handle(connectAttempt);
                } else {
                    LOG.debug("connected to Hono");
                }
            });
        }
    }

    /**
     * Connects to the Registration Service using the configured client.
     * <p>
     * If the <em>registrationServiceClient</em> is not set, this method connects to the
     * Hono server instead, assuming that the Registration Service is implemented by the Hono server.
     * 
     * @param connectHandler The handler to invoke with the outcome of the connection attempt.
     *                       If {@code null} and the connection attempt failed, this method
     *                       tries to re-connect until a connection is established.
     */
    protected final void connectToRegistration(final Handler<AsyncResult<HonoClient>> connectHandler) {

        if (registration == null) {
            if (hono != null) {
                // no need to open an additional connection to Hono server
                LOG.info("using Hono client for accessing Device Registration endpoint");
            } else if (connectHandler != null) {
                connectHandler.handle(Future.failedFuture("Device Registration client not set"));
            }
        } else if (registration.isConnected()) {
            LOG.debug("already connected to Registration Service");
            if (connectHandler != null) {
                connectHandler.handle(Future.succeededFuture(registration));
            }
        } else {
            registration.connect(createClientOptions(), connectAttempt -> {
                if (connectHandler != null) {
                    connectHandler.handle(connectAttempt);
                } else {
                    LOG.debug("connected to Registration Service");
                }
            });
        }
    }

    private ProtonClientOptions createClientOptions() {
        return new ProtonClientOptions()
                .setConnectTimeout(200)
                .setReconnectAttempts(1)
                .setReconnectInterval(Constants.DEFAULT_RECONNECT_INTERVAL_MILLIS);
    }

    /**
     * Checks if this adapter is connected to both the Hono server and the Device Registration service.
     * 
     * @return {@code true} if this adapter is connected.
     */
    protected final boolean isConnected() {
        boolean result = hono != null && hono.isConnected();
        if (registration != null) {
            result &= registration.isConnected();
        }
        return result;
    }

    protected final void closeClients(final Handler<AsyncResult<Void>> closeHandler) {

        Future<Void> honoTracker = Future.future();
        Future<Void> registrationTracker = Future.future();

        if (hono == null) {
            honoTracker.complete();
        } else {
            hono.shutdown(honoTracker.completer());
        }

        if (registration == null) {
            registrationTracker.complete();
        } else {
            registration.shutdown(registrationTracker.completer());
        }

        CompositeFuture.all(honoTracker, registrationTracker).setHandler(s -> {
            if (closeHandler != null) {
                if (s.succeeded()) {
                    closeHandler.handle(Future.succeededFuture());
                } else {
                    closeHandler.handle(Future.failedFuture(s.cause()));
                }
            }
        });
    }

    protected final Future<MessageSender> getTelemetrySender(final String tenantId) {
        Future<MessageSender> result = Future.future();
        hono.getOrCreateTelemetrySender(tenantId, result.completer());
        return result;
    }

    protected final Future<MessageSender> getEventSender(final String tenantId) {
        Future<MessageSender> result = Future.future();
        hono.getOrCreateEventSender(tenantId, result.completer());
        return result;
    }

    protected final Future<RegistrationClient> getRegistrationClient(final String tenantId) {
        Future<RegistrationClient> result = Future.future();
        getRegistrationServiceClient().getOrCreateRegistrationClient(tenantId, result.completer());
        return result;
    }

    protected final Future<String> getRegistrationAssertion(final String tenantId, final String deviceId) {
        Future<String> result = Future.future();
        getRegistrationClient(tenantId).compose(client -> {
            Future<RegistrationResult> tokenTracker = Future.future();
            client.assertRegistration(deviceId, tokenTracker.completer());
            return tokenTracker;
        }).compose(regResult -> {
            if (regResult.getStatus() == HttpURLConnection.HTTP_OK) {
                result.complete(regResult.getPayload().getString(RegistrationConstants.FIELD_ASSERTION));
            } else {
                result.fail("cannot assert device registration status");
            }
        }, result);
        return result;
    }

}
