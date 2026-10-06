package common;

/**
 * Hằng số giao thức truyền thông giữa Server và Client.
 * Tất cả command/response strings tập trung tại đây.
 */
public final class Protocol {
    private Protocol() {}

    /* ── Commands (Client → Server) ────────────────────── */
    public static final String PING        = "PING_CHECK";
    public static final String REGISTER    = "REGISTER";
    public static final String LOGIN       = "LOGIN";
    public static final String LIST_MAIL   = "LIST_MAIL";
    public static final String SEND_MAIL   = "SEND_MAIL";
    public static final String READ_MAIL   = "READ_MAIL";
    public static final String QUIT        = "QUIT";

    /* ── Responses (Server → Client) ───────────────────── */
    public static final String PONG        = "PONG";
    public static final String SUCCESS     = "SUCCESS";
    public static final String FAIL        = "FAIL";
    public static final String MAIL_START  = "MAIL_START";
    public static final String MAIL_END    = "MAIL_END";

    /* ── Delimiters ────────────────────────────────────── */
    public static final String END_CONTENT = "<<END_CONTENT>>";
    public static final String DELIM       = "|";
}
