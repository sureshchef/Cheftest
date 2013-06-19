package utils.gson.adaptor;

import com.google.gson.*;
import models.enumerations.LocationTech;

import java.lang.reflect.Type;

public class LocationEnumGsonTypeAdaptor implements JsonSerializer<LocationTech>, JsonDeserializer<LocationTech> {

    public JsonElement serialize(LocationTech locationTech, Type type, JsonSerializationContext jsc) {
        return new JsonPrimitive(locationTech.toString());
    }

    public LocationTech deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
        if (je.getAsString().equalsIgnoreCase("")) {
            return null;
        }
        return LocationTech.valueOf(je.getAsString());
    }
}
