package controllers.error;

public class Error {

    private int code;
    private String message;
    private String heading;

    public Error(ErrorTypes errorType) {
        this.code = errorType.code;
        this.heading = errorType.heading;
        this.message = errorType.message;
    }

    public int getCode() {
        return code;
    }

    public String getHeading() {
        return heading;
    }

    public String getMessage() {
        return message;
    }
}
