package controllers.error;

public enum ErrorTypes {

    DELETING_YOURSELF(10, "Unable to Delete",
    "You cannot delete yourself while you are logged in."),
    ERROR_DELETING_WEBUSER(11, "Unable to Delete",
    "An error occurred while attempting to delete the webuser"),
    USER_HAS_ACTIVE_ACCOUNTS(12, "Unable to Delete",
    "Cannot delete user as he/she has an active account."),
    ERROR_DELETING_SUBSCRIBER(13, "Unable to Delete ",
    "There was a problem deleting the subscriber.  This maybe due to there being incidents associated with him/her."),
    ERROR_GENERATING_INCIDENT_REPORT(14, "An error occurred",
    "There was a problem generating the report"),
    ACCOUNT_NOT_FOUND(20, "Account Not Found",
    "Unable to delete the account as the account was not found"),
    ACCOUNT_CONTAINS_SUBSCRIBERS(21, "Unable to Delete",
    "Unable to delete as this account contains subscribers."),
    ERRROR_DELETING_ACCOUNT(22, "Unable to Delete", "An error occurred while trying to delete the account"),
    NETWORK_EVENT_NOT_FOUND(20, "Network Event Not Found",
    "Unable to delete the network event as it was not found"),
    ERROR_DELETING_NETWORK_EVENT(22, "Unable to Delete", "An error occurred while trying to delete the network event"),
    INTERNAL_SERVER_ERROR(23, "Internal Server Error", "An internal server error occurred during processing of your request.");
    public int code;
    public String message;
    public String heading;

    private ErrorTypes(int errorNumber, String heading, String message) {
        this.code = errorNumber;
        this.heading = heading;
        this.message = message;
    }
}
