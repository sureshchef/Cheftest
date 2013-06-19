package utils.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import models.Account;
import org.joda.time.DateTime;
import play.Play;
import utils.gson.adaptor.AccountsGsonTypeAdaptor;
import utils.gson.adaptor.JodaDateTimeGsonAdapter;

public enum GSONBuilderTemp {
    INSTANCE;

    public Gson fromReportCount() {
        GsonBuilder builder = new GsonBuilder().registerTypeAdapter(DateTime.class,
                new JodaDateTimeGsonAdapter());
        if (Play.mode == Play.Mode.DEV) {
            builder.setPrettyPrinting();
        }
        return builder.create();
    }

    public Gson standardWithExpose() {
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();
        if (Play.mode == Play.Mode.DEV) {
            builder.setPrettyPrinting();
        }
        return builder.create();
    }

    public Gson fromSubscriber() {
        GsonBuilder builder = new GsonBuilder().
                excludeFieldsWithoutExposeAnnotation().registerTypeAdapter(
                Account.class, new AccountsGsonTypeAdaptor());

        if (Play.mode == Play.Mode.DEV) {
            builder.setPrettyPrinting();
        }
        return builder.create();
    }
}
