package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;

/*compile an entire directory -  javac $(find . -name "*.java") */

public class Broker {

    private Endpoint endpoint;
    private ClientCollection<InetSocketAddress> clientCollection;
    private final int portNumber = 4711;
    private int counter = 0;


    private Broker() {
        this.endpoint = new Endpoint(portNumber);
        this.clientCollection = new ClientCollection<>();
    }

    private void broker(){

        do {

            Message message = endpoint.blockingReceive();
            if (message.getPayload() instanceof RegisterRequest) {
                register(message);

            } else if (message.getPayload() instanceof DeregisterRequest) {
                deregister(message);

            } else if (message.getPayload() instanceof HandoffRequest) {
                handOffFish(message);
            }

        } while (true);
    }

    private void register(Message message){
        InetSocketAddress sender = message.getSender();
        String clientName = "Tank" + counter;
        clientCollection.add(clientName,sender);
        System.out.println(clientName + ":" + sender.getHostString());
        RegisterResponse registerResponse =  new RegisterResponse(clientName);
        endpoint.send(sender,registerResponse);
        counter++;
    }


    private void deregister(Message message){
        DeregisterRequest payload = (DeregisterRequest) message.getPayload();
        String senderId = payload.getId();

        System.out.println("removing" + ":" + payload.getId());

        if (clientCollection.size() != 0) {
            clientCollection.remove(clientCollection.indexOf(senderId));
        }
    }

    private void handOffFish(Message message){
        InetSocketAddress receiver;
        HandoffRequest request = (HandoffRequest) message.getPayload();
        FishModel fish = request.getFish();
        Direction direction = fish.getDirection();

        int index = clientCollection.indexOf(message.getSender());

        if(direction == Direction.LEFT) {

            receiver = clientCollection.getLeftNeighorOf(index);

        } else {

            receiver = clientCollection.getRightNeighorOf(index);
        }

        endpoint.send(receiver, request);
    }

    public static void main(final String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

}
