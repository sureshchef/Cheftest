package utils;

import org.hibernate.type.StandardBasicTypes;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.contrib.hibernate.PersistentDateTime;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persist {@link org.joda.time.DateTime} via hibernate interpreting dates
 * as UTC (rather than local JVM TZ) when retrieving them from database.
 */
public class PersistentDateTimeUTC extends PersistentDateTime {

    public Object nullSafeGet(ResultSet resultSet, String string) throws SQLException {
        Object timestamp = StandardBasicTypes.TIMESTAMP.nullSafeGet(resultSet, string);
        if (timestamp == null) {
            return null;
        }

        return new DateTime(timestamp, DateTimeZone.UTC);
    }

}