package aqua.common.msgtypes;

import java.io.Serializable;

public class Token implements Serializable {

    String token;

    public Token(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
