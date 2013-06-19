package utils.gson.adaptor;

import com.google.gson.*;
import models.IncidentType;

import java.lang.reflect.Type;

public class IncidentTypeGsonTypeAdaptor implements JsonSerializer<IncidentType>, JsonDeserializer<IncidentType> {

    public JsonElement serialize(IncidentType incidentType, Type type, JsonSerializationContext jsc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public IncidentType deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
        if (je.getAsString().equalsIgnoreCase("")) {
            return null;
        }
        return IncidentType.findByKey(je.getAsString());
    }
}
