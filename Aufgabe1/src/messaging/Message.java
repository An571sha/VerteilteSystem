package messaging;

import java.net.InetSocketAddress;
import java.io.Serializable;

public class Message
{
    private final Serializable payload;
    private final InetSocketAddress sender;

    public Message(final Serializable payload, final InetSocketAddress sender) {
        this.payload = payload;
        this.sender = sender;
    }

    public Serializable getPayload() {
        return this.payload;
    }

    public InetSocketAddress getSender() {
        return this.sender;
    }
}