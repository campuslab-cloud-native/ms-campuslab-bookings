package booking.config;

public class MessagingNames {

    private MessagingNames() {
    }

    public static final String EXCHANGE = "cmd.direct";

    public static final String EMAIL_ROUTING_KEY = "email.send";
    public static final String PREP_ROUTING_KEY = "prep.ticket";
    public static final String VOUCHER_ROUTING_KEY = "voucher.gen";

}
