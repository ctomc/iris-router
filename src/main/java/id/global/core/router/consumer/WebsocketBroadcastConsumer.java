package id.global.core.router.consumer;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import id.global.core.router.model.ResponseMessageType;
import id.global.iris.common.constants.Exchanges;

@ApplicationScoped
public class WebsocketBroadcastConsumer extends AbstractWebSocketConsumer {

    @Override
    protected String getQueueName() {
        return Exchanges.BROADCAST.getValue();
    }

    @Override
    protected List<String> getQueueRoles() {
        return List.of();
    }

    @Override
    protected ResponseMessageType getSocketMessageType() {
        return ResponseMessageType.BROADCAST;
    }

}
