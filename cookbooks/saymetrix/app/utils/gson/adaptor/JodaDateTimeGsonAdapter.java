package utils.gson.adaptor;

import com.google.gson.*;
import org.joda.time.DateTime;

import java.lang.reflect.Type;

public class JodaDateTimeGsonAdapter implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {

    public JsonElement serialize(DateTime srcDateTime, Type srcType, JsonSerializationContext context) {
        return new JsonPrimitive(srcDateTime.toString());
    }

    public DateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        return new DateTime(json.getAsString());
    }
}
