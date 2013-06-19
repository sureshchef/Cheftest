package utils.gson.adaptor;

import com.google.gson.*;
import models.enumerations.Position;

import java.lang.reflect.Type;

public class PositionEnumGsonTypeAdaptor implements JsonSerializer<Position>, JsonDeserializer<Position> {

    public JsonElement serialize(Position position, Type type, JsonSerializationContext jsc) {
        return new JsonPrimitive(position.toString());
    }

    public Position deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
        if (je.getAsString().equalsIgnoreCase("")) {
            return null;
        }
        return Position.valueOf(je.getAsString());
    }
}
