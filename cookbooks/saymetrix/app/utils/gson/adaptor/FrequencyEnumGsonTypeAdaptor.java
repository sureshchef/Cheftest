package utils.gson.adaptor;

import com.google.gson.*;
import models.enumerations.Frequency;

import java.lang.reflect.Type;

public class FrequencyEnumGsonTypeAdaptor implements JsonSerializer<Frequency>, JsonDeserializer<Frequency> {

    public JsonElement serialize(Frequency frequency, Type type, JsonSerializationContext jsc) {
        return new JsonPrimitive(frequency.toString());
    }

    public Frequency deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
        if (je.getAsString().equalsIgnoreCase("")) {
            return null;
        }
        return Frequency.valueOf(je.getAsString());
    }
}