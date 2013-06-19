package utils.gson.adaptor;

import com.google.gson.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.lang.reflect.Type;

public class JodaIntervalGsonAdapter implements JsonSerializer<Interval>, JsonDeserializer<Interval> {
    private static final int YEAR_POSITION = 2;
    private static final int DAY_POSITION = 0;
    private static final int MONTH_POSITION = 1;

    public JsonElement serialize(Interval srcInterval, Type srcType, JsonSerializationContext context) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy");
        return new JsonPrimitive(fmt.print(srcInterval.getStart()) + "," + fmt.print(srcInterval.getEnd()));
    }

    public Interval deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        JsonArray jsonArray = json.getAsJsonArray();
        String startDate = jsonArray.get(0).getAsString();
        String endDate = jsonArray.get(1).getAsString();
        if (startDate.equalsIgnoreCase("") && endDate.equalsIgnoreCase("")) {
            /**
             * this is a valid state Not Set.we will leave if only 1 is set to
             * be caught at a higher level as a JsonParseException
             */
           return null;
        }
        DateTime startDateTime = createDateTime(startDate, 0, 0);
        DateTime endDateTime = createDateTime(endDate, 23, 59);
        if(startDateTime.isAfter(endDateTime)){
            throw new JsonParseException("Start date not before end date in period field.");
        }
        return new Interval(startDateTime, endDateTime);
    }

    private DateTime createDateTime(String date, int hourOfDay, int minuteOfHour) {
        String[] dateParts = date.split("/");
        return new DateTime(Integer.parseInt(dateParts[YEAR_POSITION]), Integer.parseInt(dateParts[MONTH_POSITION]), Integer.parseInt(dateParts[DAY_POSITION]), hourOfDay, minuteOfHour);
    }
}