package utils.gson.adaptor;

import com.google.gson.*;
import org.joda.time.DateTime;

import java.lang.reflect.Type;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class JodaFilterDateTimeGsonAdapter implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {
    private static final int YEAR_POSITION = 2;
    private static final int DAY_POSITION = 0;
    private static final int MONTH_POSITION = 1;

    public JsonElement serialize(DateTime srcDateTime, Type srcType, JsonSerializationContext context) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy");
        return new JsonPrimitive(srcDateTime.toString(fmt));
    }

    public DateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        
        String dateString = json.getAsString();
        if ("".equalsIgnoreCase(dateString)) {
            return null;
        }
        String[] dateParts = dateString.split("/");
        return new DateTime(Integer.parseInt(dateParts[YEAR_POSITION]), Integer.parseInt(dateParts[MONTH_POSITION]), Integer.parseInt(dateParts[DAY_POSITION]), 0, 0);
    }
}
