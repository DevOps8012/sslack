package jp.webpay.sslack;

import java.util.Date;

public class Message {
    public String token;
    public Double timestamp;
    public String channel_name;
    public String user_name;
    public String text;

    public Date timestampAsDate() {
        return new Date((long)(timestamp * 1000L));
    }
}
