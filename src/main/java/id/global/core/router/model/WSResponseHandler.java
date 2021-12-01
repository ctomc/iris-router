package id.global.core.router.model;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import id.global.core.router.service.WebsocketRegistry;

/**
 * @author Tomaz Cerar
 */
public class WSResponseHandler extends DefaultResponseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WSResponseHandler.class);

    private final WebsocketRegistry websocketRegistry;
    protected final ObjectMapper objectMapper;

    public WSResponseHandler(WebsocketRegistry websocketRegistry, ObjectMapper objectMapper) {
        this.websocketRegistry = websocketRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    protected void onSuccess(ResponseMessageType responseMessageType, AmpqMessage message) {
        if (responseMessageType == ResponseMessageType.RPC) {
            sendRPCMessage(message);
        } else if (responseMessageType == ResponseMessageType.SESSION) {
            sendToSession(message);
        } else if (responseMessageType == ResponseMessageType.BROADCAST) {
            sendBroadcastMessage(message);
        } else {
            throw new RuntimeException("Don't know how to handle: " + responseMessageType);
        }
    }

    private void sendToSession(AmpqMessage message) {
        String sessionId = message.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            LOGGER.warn("Could not send session message with sessionId: {}, requestId: {}, userId: {}, message: {}", sessionId,
                    message.correlationId(), message.userId(), BackendRequest.sanitizeBody(message.body()));
            return;
        }
        UserSession session = websocketRegistry.getSession(sessionId);
        //assert session != null;
        if (session == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.warn("Could not find session with sessionId: {} on this router, requestId: {}, userId: {}, message: {}",
                        sessionId, message.correlationId(), message.userId(), BackendRequest.sanitizeBody(message.body()));
            }
            return;
        }
        session.sendMessage(message);

    }

    private void sendRPCMessage(AmpqMessage message) {
        String userId = message.userId();
        Set<? extends UserSession> allSocketOfTheUser = websocketRegistry.getAllUserSessions(userId);
        if (allSocketOfTheUser == null || allSocketOfTheUser.isEmpty()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("There are no sockets for user: {}, request: {}! dropping message, event: {},  body{}", userId,
                        message.correlationId(), message.eventType(), BackendRequest.sanitizeBody(message.body()));
            }
            return;
        }
        for (UserSession session : allSocketOfTheUser) {
            session.sendMessage(message);
        }
    }

    private void sendBroadcastMessage(AmpqMessage message) {
        for (UserSession userSession : websocketRegistry.getAllSessions()) {
            userSession.sendMessage(message);
        }
    }

}
