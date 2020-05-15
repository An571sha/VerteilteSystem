package aqua.broker;

import aqua.common.Direction;
import aqua.common.FishModel;
import aqua.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/* compile an entire directory -  javac $(find . -name "*.java") */

public class Broker {

    private Endpoint endpoint;
    private ClientCollection<InetSocketAddress> clientCollection;
    private final int portNumber = 4711;
    private final int nThreads = 5;
    private int counter = 0;
    private boolean stopRequested;


    private Broker() {
        this.endpoint = new Endpoint(portNumber);
        this.clientCollection = new ClientCollection<>();
        this.stopRequested =  false;
    }

    private void broker(){
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        Thread dialogBoxThread =  new Thread(() -> {
            int res = JOptionPane.showOptionDialog(null,
                    "Press Ok button to stop server","",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    null,
                    null);
            if (res == 0) {
                stopRequested = true;
                System.exit(0);
            }

        });

        dialogBoxThread.start();

        do {
            Message message = endpoint.blockingReceive();
            executorService.execute(() -> {
                BrokerTask brokerTask = new BrokerTask();
                if (message.getPayload() instanceof RegisterRequest) {
                    brokerTask.register(message);

                } else if (message.getPayload() instanceof DeregisterRequest) {
                    brokerTask.deregister(message);

                } else if (message.getPayload() instanceof HandoffRequest) {
                    brokerTask.handOffFish(message);

                } else if(message.getPayload() instanceof PoisonPill){
                    System.exit(0);
                }
            });

        } while (!stopRequested);
    }

    public static void main(final String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }


    public class BrokerTask {
        private ReadWriteLock lock;

        public BrokerTask() {
            this.lock = new ReentrantReadWriteLock();
        }

        public void register(Message message){
            InetSocketAddress sender = message.getSender();
            String clientName = "Tank" + counter;

            lock.writeLock().lock();
            clientCollection.add(clientName,sender);
            lock.writeLock().unlock();

            lock.readLock().lock();

            if (clientCollection.size() != 0) {
               NeigbourUpdate neigbourUpdate = new NeigbourUpdate(clientName,
                       clientCollection.getLeftNeighorOf(clientCollection.indexOf(clientName)),
                       clientCollection.getRightNeighorOf(clientCollection.indexOf(clientName)));

               endpoint.send(sender, neigbourUpdate);
            } else {
                NeigbourUpdate neigbourUpdate = new NeigbourUpdate(clientName,
                        clientCollection.getClient(clientCollection.indexOf(clientName)),
                        clientCollection.getClient(clientCollection.indexOf(clientName)));
                endpoint.send(sender, neigbourUpdate);
            }

            lock.readLock().unlock();

            System.out.println(clientName + ":" + sender.getHostString());
            RegisterResponse registerResponse =  new RegisterResponse(clientName);
            endpoint.send(sender,registerResponse);
            counter++;
        }

        public void deregister(Message message){
            DeregisterRequest payload = (DeregisterRequest) message.getPayload();
            String senderId = payload.getId();

            System.out.println("removing" + ":" + payload.getId());

            if (clientCollection.size() != 0) {
                lock.readLock().lock();
                clientCollection.remove(clientCollection.indexOf(senderId));
                NeigbourUpdate neigbourUpdate = new NeigbourUpdate(senderId,
                        clientCollection.getLeftNeighorOf(clientCollection.indexOf(senderId)),
                        clientCollection.getRightNeighorOf(clientCollection.indexOf(senderId)));

                endpoint.send(message.getSender(), neigbourUpdate);
                lock.readLock().unlock();
            }
        }

        public void handOffFish(Message message){
            InetSocketAddress receiver;
            HandoffRequest request = (HandoffRequest) message.getPayload();
            FishModel fish = request.getFish();
            Direction direction = fish.getDirection();

            lock.readLock().lock();
            int index = clientCollection.indexOf(message.getSender());
            lock.readLock().unlock();

            if(direction == Direction.LEFT) {

                receiver = clientCollection.getLeftNeighorOf(index);

            } else {

                receiver = clientCollection.getRightNeighorOf(index);
            }

            endpoint.send(receiver, request);
        }
    }

}
