package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.RoleHolderPresent;
import controllers.error.ErrorTypes;
import models.Account;
import models.Filter;
import models.IncidentType;
import models.WebUser;
import models.enumerations.Frequency;
import models.enumerations.LocationTech;
import models.enumerations.Position;
import models.valueobject.FilterValueObject;
import org.joda.time.Interval;
import play.Logger;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;
import utils.gson.adaptor.*;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

@With(Deadbolt.class)
@RoleHolderPresent
public class Filters extends Controller {

    public static void getAll() {
        List<Filter> filters = Filter.findAll();
        if (filters == null) {
            response.status = Http.StatusCode.NOT_FOUND;
            return;
        }
        List<FilterValueObject> filterMetaList = convertToFilterValueObjectList(filters);
        renderJSON(new GsonBuilder().create().toJson(filterMetaList));
    }

    public static void get(long id) {
        Filter dbFilter = Filter.findById(id);
        if (dbFilter == null) {
            response.status = Http.StatusCode.NOT_FOUND;
            return;
        }
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().registerTypeAdapter(Interval.class, new JodaIntervalGsonAdapter()).registerTypeAdapter(DateTime.class, new JodaFilterDateTimeGsonAdapter()).create();
        renderJSON(gson.toJson(dbFilter));
    }

    public static void createPersonal() {
        try {
            Filter filter = parseFilterFromRequestBody();
            filter.validateAndCreate();
            WebUser user = Security.getCurrentlyLoggedInUser();
            user.personalFilters.add(filter);
            user.save();
            response.status = Http.StatusCode.CREATED;
            Gson gson = new GsonBuilder().create();
            renderJSON(gson.toJson(filter.getValueObject()));
        } catch (Exception e) {
            Logger.error(e, "Error validating and creating a filter.");
            response.status = Http.StatusCode.INTERNAL_ERROR;
            renderJSON(new GsonBuilder().create().toJson(new controllers.error.Error(ErrorTypes.INTERNAL_SERVER_ERROR)));
        }
    }

    public static void edit(long id) {
        Filter dbFilter = Filter.findById(id);
        dbFilter.overWriteValues(parseFilterFromRequestBody());
        validation.valid(dbFilter);
        if (Validation.hasErrors()) {
            //TODO deal with validaiton errors,return somethign that front end can understand for validaiton,at the moment we will just set the return as not modified
            response.status = Http.StatusCode.BAD_REQUEST;
        } else {
            dbFilter.save();
        }
    }

    public static void delete(long id) {
        Filter dbFilter = Filter.findById(id);
        dbFilter.delete();
    }

    public static Filter parseFilterFromRequestBody() {
        return parseFilterFromJson(params.get("body"));
    }

    public static Filter parseFilterFromJson(String body) {
        JsonElement jsonElement = new JsonParser().parse(body);
        Gson gson = new GsonBuilder().registerTypeAdapter(LocationTech.class, new LocationEnumGsonTypeAdaptor()).registerTypeAdapter(Position.class, new PositionEnumGsonTypeAdaptor()).registerTypeAdapter(Frequency.class, new FrequencyEnumGsonTypeAdaptor()).registerTypeAdapter(IncidentType.class, new IncidentTypeGsonTypeAdaptor()).registerTypeAdapter(Account.class, new AccountsGsonTypeAdaptor()).registerTypeAdapter(Interval.class, new JodaIntervalGsonAdapter()).registerTypeAdapter(DateTime.class, new JodaFilterDateTimeGsonAdapter()).create();
        return gson.fromJson(jsonElement, Filter.class);
    }

    private static List<FilterValueObject> convertToFilterValueObjectList(List<Filter> filters) {
        List<FilterValueObject> returnList = new ArrayList<FilterValueObject>();
        for (Filter filter : filters) {
            returnList.add(filter.getValueObject());
        }
        return returnList;
    }
}
