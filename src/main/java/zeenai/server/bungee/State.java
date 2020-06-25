package zeenai.server.bungee;


public enum State
{
    JOIN,
    PRE_AUTH,
    POST_AUTH,
    POST_AUTH_NEW, // <-- This is used when the account is new and needs /email
    AUTH_GET_EMAIL,
    LOGGED_IN,
    ENQUEUED,
    FINAL_STATE
}