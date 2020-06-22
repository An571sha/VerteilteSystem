package messaging;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.net.SocketAddress;
import java.net.DatagramPacket;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.DatagramSocket;

public class Endpoint
{
    private final DatagramSocket socket;

    public Endpoint() {
        try {
            this.socket = new DatagramSocket();
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public Endpoint(final int port) {
        try {
            this.socket = new DatagramSocket(port);
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(final InetSocketAddress receiver, final Serializable payload) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(payload);
            final byte[] bytes = baos.toByteArray();
            final DatagramPacket datagram = new DatagramPacket(bytes, bytes.length, receiver);
            this.socket.send(datagram);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Message readDatagram(final DatagramPacket datagram) {
        try {
            final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(datagram.getData()));
            return new Message((Serializable)ois.readObject(), (InetSocketAddress)datagram.getSocketAddress());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Message blockingReceive() {
        final DatagramPacket datagram = new DatagramPacket(new byte[1024], 1024);
        try {
            this.socket.receive(datagram);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this.readDatagram(datagram);
    }

    public Message nonBlockingReceive() {
        final DatagramPacket datagram = new DatagramPacket(new byte[1024], 1024);
        try {
            this.socket.setSoTimeout(1);
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
        boolean timeoutExpired;
        try {
            this.socket.receive(datagram);
            timeoutExpired = false;
        }
        catch (SocketTimeoutException e3) {
            timeoutExpired = true;
        }
        catch (IOException e2) {
            throw new RuntimeException(e2);
        }
        try {
            this.socket.setSoTimeout(0);
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
        if (timeoutExpired) {
            return null;
        }
        return this.readDatagram(datagram);
    }
}