/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.websocket;

import io.micronaut.core.convert.value.ConvertibleMultiValues;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.http.MediaType;
import io.micronaut.websocket.exceptions.WebSocketSessionException;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.security.Principal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

/**
 * Represents an open WebSocket connection. Based largely on {@code javax.websocket} and likely to be able to implement the spec in the future.
 *
 * @author graemerocher
 * @since 1.0
 */
public interface WebSocketSession extends MutableConvertibleValues<Object>, AutoCloseable {

    /**
     * The ID of the session.
     *
     * @return The ID of the session
     */
    String getId();

    /**
     * @return Only the attributes of the session
     */
    MutableConvertibleValues<Object> getAttributes();

    /**
     * Whether the session is open.
     * @return True if it is
     */
    boolean isOpen();

    /**
     * Whether the connection is secure.
     *
     * @return True if it is secure
     */
    boolean isSecure();

    /**
     * The current open sessions.
     *
     * @return The open sessions
     */
    Set<? extends WebSocketSession> getOpenSessions();

    /**
     * The request URI this session was opened under.
     *
     * @return The request URI
     */
    URI getRequestURI();

    /**
     * The protocol version of the WebSocket protocol currently being used.
     *
     * @return The protocol version
     */
    String getProtocolVersion();

    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections that match the given filter. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param mediaType The media type of the message. Used to lookup an appropriate codec via the {@link io.micronaut.http.codec.MediaTypeCodecRegistry}.
     * @param filter The filter to apply
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    <T> Publisher<T> broadcast(T message, MediaType mediaType, Predicate<WebSocketSession> filter);

    /**
     * Send the given message to the remote peer.
     *
     * @param message The message
     * @param mediaType The media type of the message. Used to lookup an appropriate codec via the {@link io.micronaut.http.codec.MediaTypeCodecRegistry}.
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    <T> Publisher<T> send(T message, MediaType mediaType);

    /**
     * Send the given message to the remote peer asynchronously.
     *
     * @param message The message
     *  @param mediaType The media type of the message. Used to lookup an appropriate codec via the {@link io.micronaut.http.codec.MediaTypeCodecRegistry}.
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    <T> CompletableFuture<T> sendAsync(T message, MediaType mediaType);

    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param mediaType The media type of the message. Used to lookup an appropriate codec via the {@link io.micronaut.http.codec.MediaTypeCodecRegistry}.
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    default <T> Publisher<T> broadcast(T message, MediaType mediaType) {
        return broadcast(message, mediaType, (s) -> true);
    }

    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    default <T> Publisher<T> broadcast(T message) {
        return broadcast(message, MediaType.APPLICATION_JSON_TYPE, (s) -> true);
    }

    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections that match the given filter. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param filter The filter to apply
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    default <T> Publisher<T> broadcast(T message, Predicate<WebSocketSession> filter) {
        Objects.requireNonNull(filter, "The filter cannot be null");
        return broadcast(message, MediaType.APPLICATION_JSON_TYPE, filter);
    }

    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param mediaType The media type of the message. Used to lookup an appropriate codec via the {@link io.micronaut.http.codec.MediaTypeCodecRegistry}.
     * @param filter The filter
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    default <T> CompletableFuture<T> broadcastAsync(T message, MediaType mediaType, Predicate<WebSocketSession> filter) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Flowable.fromPublisher(broadcast(message, mediaType, filter)).subscribe(
                (o) -> { },
                future::completeExceptionally,
                () -> future.complete(message)
        );
        return future;
    }

    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    default <T> CompletableFuture<T> broadcastAsync(T message) {
        return broadcastAsync(message, MediaType.APPLICATION_JSON_TYPE, (o) -> true);
    }

    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections that match the given filter. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param filter The filter to apply
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    default <T> CompletableFuture<T> broadcastAsync(T message, Predicate<WebSocketSession> filter) {
        return broadcastAsync(message, MediaType.APPLICATION_JSON_TYPE, filter);
    }


    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param mediaType The media type of the message. Used to lookup an appropriate codec via the {@link io.micronaut.http.codec.MediaTypeCodecRegistry}.
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    default <T> CompletableFuture<T> broadcastAsync(T message, MediaType mediaType) {
        return broadcastAsync(message, mediaType, (o) -> true);
    }

    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param mediaType The media type of the message. Used to lookup an appropriate codec via the {@link io.micronaut.http.codec.MediaTypeCodecRegistry}.
     * @param filter The filter
     * @param <T> The message type
     */
    default <T> void broadcastSync(T message, MediaType mediaType, Predicate<WebSocketSession> filter) {
        try {
            broadcastAsync(message, mediaType, filter).get();
        } catch (InterruptedException e) {
            throw new WebSocketSessionException("Broadcast Interrupted");
        } catch (ExecutionException e) {
            throw new WebSocketSessionException("Broadcast Failure: " + e.getMessage(), e);
        }
    }

    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param <T> The message type
     */
    default <T> void broadcastSync(T message) {
        broadcastSync(message, MediaType.APPLICATION_JSON_TYPE, (o) -> true);
    }

    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections that match the given filter. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param filter The filter to apply
     * @param <T> The message type
     */
    default <T> void broadcastSync(T message, Predicate<WebSocketSession> filter) {
        broadcastSync(message, MediaType.APPLICATION_JSON_TYPE, filter);
    }


    /**
     * When used on the server this method will broadcast a message to all open WebSocket connections. When
     * used on the client this method behaves the same as {@link #send(Object)}.
     *
     * @param message The message
     * @param mediaType The media type of the message. Used to lookup an appropriate codec via the {@link io.micronaut.http.codec.MediaTypeCodecRegistry}.
     * @param <T> The message type
     */
    default <T> void broadcastSync(T message, MediaType mediaType) {
        broadcastSync(message, mediaType, (o) -> true);
    }

    /**
     * Send the given message to the remote peer synchronously.
     *
     * @param message The message
     * @param mediaType The media type of the message. Used to lookup an appropriate codec via the {@link io.micronaut.http.codec.MediaTypeCodecRegistry}.
     */
    default void sendSync(Object message, MediaType mediaType) {
        try {
            sendAsync(message, mediaType).get();
        } catch (InterruptedException e) {
            throw new WebSocketSessionException("Send Interrupted");
        } catch (ExecutionException e) {
            throw new WebSocketSessionException("Send Failure: " + e.getMessage(), e);
        }
    }

    /**
     * Send the given message to the remote peer.
     *
     * @param message The message
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    default <T> Publisher<T> send(T message) {
        return send(message, MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * Send the given message to the remote peer asynchronously.
     *
     * @param message The message
     * @param <T> The message type
     * @return A {@link Publisher} that either emits an error or emits the message once it has been published successfully.
     */
    default <T> CompletableFuture<T> sendAsync(T message) {
        return sendAsync(message, MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * Send the given message to the remote peer synchronously.
     *
     * @param message The message
     */
    default void sendSync(Object message) {
        sendSync(message, MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * The subprotocol if one is used.
     * @return The subprotocol
     */
    default Optional<String> getSubprotocol() {
        return Optional.empty();
    }

    /**
     * The request parameters used to create this session.
     *
     * @return The request parameters
     */
    default ConvertibleMultiValues<String> getRequestParameters() {
        return ConvertibleMultiValues.empty();
    }

    /**
     * Any matching URI path variables.
     *
     * @return The path variables
     */
    default ConvertibleValues<Object> getUriVariables() {
        return ConvertibleValues.empty();
    }

    /**
     * The user {@link Principal} used to create the session.
     *
     * @return The {@link Principal}
     */
    default Optional<Principal> getUserPrincipal() {
        return Optional.empty();
    }

    @Override
    void close();

    /**
     * Close the session with the given event.
     *
     * @param closeReason The close event
     */
    void close(CloseReason closeReason);
}