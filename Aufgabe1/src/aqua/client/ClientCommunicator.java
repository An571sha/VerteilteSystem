package aqua.client;

import java.net.InetSocketAddress;

import aqua.common.FishModel;
import aqua.common.Properties;
import aqua.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish) {
			endpoint.send(broker, new HandoffRequest(fish));
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				// could be complete bullshit need to debug and check
				Message msg = endpoint.blockingReceive();
				NeigbourUpdate neigbourUpdate = new NeigbourUpdate(((NeigbourUpdate) msg.getPayload()).getId(), ((NeigbourUpdate) msg.getPayload()).getLeft(), ((NeigbourUpdate) msg.getPayload()).getRight());

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if(msg.getPayload() instanceof NeigbourUpdate)
					tankModel.getLeftSocketAddressOfNeighbour(neigbourUpdate);
					tankModel.getRightSocketAddressOfNeighbour(neigbourUpdate);

			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
