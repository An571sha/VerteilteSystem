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
            System.out.println("size before" + clientCollection.size());
            lock.readLock().lock();
            if (clientCollection.size() == 1) {

                NeigbourUpdate neigbourUpdate = new NeigbourUpdate(clientName,
                        sender, sender);
                System.out.println("Register first neighbour update" + clientName +
                        clientCollection.indexOf(neigbourUpdate.getLeft().toString()) + " " +
                        clientCollection.indexOf(neigbourUpdate.getRight().toString()));
                endpoint.send(sender, neigbourUpdate);

            } else {

                InetSocketAddress leftNeighbour = clientCollection.getLeftNeighorOf(clientCollection.indexOf(sender));
                InetSocketAddress rightNeighbour = clientCollection.getClient(0);

                NeigbourUpdate neigbourUpdate = new NeigbourUpdate(clientName,
                        leftNeighbour,
                        rightNeighbour);

                NeigbourUpdate leftNeigbourUpdate = new NeigbourUpdate("left",
                        clientCollection.getLeftNeighorOf(clientCollection.indexOf(leftNeighbour)),
                        sender);

                NeigbourUpdate rightNeigbourUpdate = new NeigbourUpdate("right",
                       sender,
                       clientCollection.getRightNeighorOf(clientCollection.indexOf(rightNeighbour)));

                System.out.println("Register next neighbour update" + clientName +
                        clientCollection.indexOf(neigbourUpdate.getLeft().toString()) + " " +
                        clientCollection.indexOf(neigbourUpdate.getRight().toString()));

                endpoint.send(sender, neigbourUpdate);
                endpoint.send(leftNeighbour, leftNeigbourUpdate);
                endpoint.send(rightNeighbour, rightNeigbourUpdate);


            }
            lock.readLock().unlock();
            System.out.println("size after" + clientCollection.size());
            System.out.println("collection -" + clientCollection.toString());
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

                lock.writeLock().lock();
                clientCollection.remove(clientCollection.indexOf(senderId));
                lock.writeLock().unlock();

                lock.readLock().lock();
                InetSocketAddress leftNeighbour = clientCollection.getLeftNeighorOf(clientCollection.indexOf(senderId));
                InetSocketAddress rightNeighbour = clientCollection.getRightNeighorOf(clientCollection.indexOf(senderId));
                NeigbourUpdate leftNeigbourUpdate = new NeigbourUpdate("left",
                        clientCollection.getLeftNeighorOf(clientCollection.indexOf(leftNeighbour)),
                        rightNeighbour);

                NeigbourUpdate rightNeigbourUpdate = new NeigbourUpdate("right",
                        leftNeighbour,
                        clientCollection.getRightNeighorOf(clientCollection.indexOf(rightNeighbour)));

                System.out.println("Dereigster next neighbour update" + senderId + leftNeigbourUpdate.getLeft().getAddress().toString() + leftNeigbourUpdate.getRight().getAddress().toString());

                lock.readLock().unlock();
                endpoint.send(message.getSender(), leftNeigbourUpdate);
                endpoint.send(message.getSender(), rightNeigbourUpdate);

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
