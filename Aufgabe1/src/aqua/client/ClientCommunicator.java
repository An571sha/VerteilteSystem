package aqua.client;

import java.net.InetSocketAddress;

import aqua.common.Direction;
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

		public void handOff(FishModel fish, TankModel model) {
			if(fish.getDirection() == Direction.LEFT) {
				endpoint.send(model.getLeftNeigbour(), new HandoffRequest(fish));
			}else{
				endpoint.send(model.getRightNeigbour(), new HandoffRequest(fish));
			}
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
				// spoiler alert, (msg.getPayload() instanceof NeigbourUpdate) is complete bullshit
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if(msg.getPayload() instanceof NeigbourUpdate) {
					tankModel.setLeftNeigbour(((NeigbourUpdate) msg.getPayload()).getLeft());
					tankModel.setRightNeigbour(((NeigbourUpdate) msg.getPayload()).getRight());
				}
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
