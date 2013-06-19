package models.attributes;

public enum MobileSubscriberModelAttributes {

    MSISDN("msisdn"), ACCOUNT("account");
    public String asNamedInClass;

    private MobileSubscriberModelAttributes(String asNamedInClass) {
        this.asNamedInClass = asNamedInClass;
    }
}
