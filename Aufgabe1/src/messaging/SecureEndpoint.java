package messaging;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecureEndpoint extends Endpoint {

    private SecretKeySpec secretKeySpec;
    private final String SEQUENCE = "CAFEBABECAFEBABE";
    private final String ENCODING = "AES";
    private byte[] encodedPayload;
    private byte[] decodedPayload;

    public SecureEndpoint() {
        super();
    }

    public SecureEndpoint(int port) {
        super(port);
        this.secretKeySpec =  new SecretKeySpec(SEQUENCE.getBytes(), ENCODING);
    }

    @Override
    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
            encodedPayload = encode(convertToBytes(payload));
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.send(receiver, encodedPayload);
    }

    @Override
    public Message readDatagram(DatagramPacket datagram) {
        return super.readDatagram(datagram);
    }

    @Override
    public Message blockingReceive() {
        return super.blockingReceive();
    }

    @Override
    public Message nonBlockingReceive() {
        return super.nonBlockingReceive();
    }

    public byte[] encode(byte[] message) {

        Cipher cipher = null;

        try {

            cipher = Cipher.getInstance(ENCODING);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return cipher.doFinal(message);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public byte[] decode(byte[] message){

        Cipher cipher = null;

        try {
            cipher = Cipher.getInstance(ENCODING);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return cipher.doFinal(message);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] convertToBytes(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(object);
        return bos.toByteArray();
        }

    private Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        }
    }
}

