package booking.config;

public class MessagingNames {

    private MessagingNames() {
    }

    public static final String NOTIFY_EXCHANGE = "x.cmd.notify";

    public static final String EMAIL_ROUTING_KEY = "cmd.email";
    public static final String PREP_ROUTING_KEY = "cmd.prep";
    public static final String VOUCHER_ROUTING_KEY = "cmd.voucher";

}
