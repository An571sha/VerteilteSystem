package aqua.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NeigbourUpdate implements Serializable {
    private final String id;
    private final InetSocketAddress left;
    private final InetSocketAddress Right;

    public NeigbourUpdate(String id, InetSocketAddress left, InetSocketAddress right) {
        this.id = id;
        this.left = left;
        Right = right;
    }


    public String getId() {
        return id;
    }

    public InetSocketAddress getLeft() {
        return left;
    }

    public InetSocketAddress getRight() {
        return Right;
    }

}
