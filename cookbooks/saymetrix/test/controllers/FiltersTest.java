package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import controllers.Filters;
import models.Filter;
import models.valueobject.FilterValueObject;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Http.Response;
import play.test.Fixtures;
import util.BaseFunctionalTest;

import java.lang.reflect.Type;
import java.util.Collection;

public class FiltersTest extends BaseFunctionalTest {

	@Test
	public void testPersonalFilterIsSaved() {
		Object json = Fixtures.loadYaml("controllers/filter.json");
		Gson gson = new GsonBuilder().create();
		JsonElement jsonElement = gson.toJsonTree(json);
		Http.Response response = POST("/api/filters/personal",
				"application/json", jsonElement.toString());
		assertStatus(Http.StatusCode.CREATED, response);
	}

	@Test
	public void testFilterCreationWithDifferentDateOptions() {
		String filterWithNoDates = "{\"name\":\"Filter1\",\"incidentPeriodStart\":\"\",\"incidentPeriodEnd\":\"\",\"blackOutPeriod\":[\"\",\"\"],\"incidentTypes\":[\"voice_poor_sound\",\"data_slow_connection\"],\"locationTech\":[\"GPS\",\"NETWORK\"],\"position\":[\"INDOOR\",\"OUTDOOR\"],\"cellid\":\"\",\"msisdn\":\"\"}";
		String filterWithStartDateOnly = "{\"name\":\"Filter2\",\"incidentPeriodStart\":\"01/03/2012\",\"incidentPeriodEnd\":\"11/03/2012\",\"blackOutPeriod\":[\"\",\"\"],\"incidentTypes\":[\"voice_poor_sound\",\"data_slow_connection\"],\"locationTech\":[\"GPS\",\"NETWORK\"],\"position\":[\"INDOOR\",\"OUTDOOR\"],\"cellid\":\"\",\"msisdn\":\"\"}";
		String filterWithBothDates = "{\"name\":\"Filter2\",\"incidentPeriodStart\":\"01/03/2012\",\"incidentPeriodEnd\":\"11/03/2012\",\"blackOutPeriod\":[\"\",\"\"],\"incidentTypes\":[\"voice_poor_sound\",\"data_slow_connection\"],\"locationTech\":[\"GPS\",\"NETWORK\"],\"position\":[\"INDOOR\",\"OUTDOOR\"],\"cellid\":\"\",\"msisdn\":\"\"}";
		
        Http.Response response = POST("/api/filters/personal", "application/json", filterWithNoDates);
		assertStatus(Http.StatusCode.CREATED, response);
		response = POST("/api/filters/personal", "application/json", filterWithStartDateOnly);
		assertStatus(Http.StatusCode.CREATED, response);
		response = POST("/api/filters/personal", "application/json", filterWithBothDates);
		assertStatus(Http.StatusCode.CREATED, response);
	}

	@Test
	public void testFilterIsEdited() {
		Object json = Fixtures.loadYaml("controllers/filter.json");
		Gson gson = new GsonBuilder().create();
		JsonElement jsonElement = gson.toJsonTree(json);
		// Create Filter in DB
		Http.Response response = POST("/api/filters/personal",
				"application/json", "{'name':'testFilter'}");
		long createdFilterID = parseFilterMetaFromResponse(response).getId();
		// Edit filter
		response = PUT("/api/filters/" + createdFilterID, "application/json",
				jsonElement.toString());
		assertIsOk(response);
		// Retrive Filter and make sure it has changed
		response = GET("/api/filters/" + createdFilterID);
		assertIsOk(response);
		FilterValueObject filterMeta = parseFilterMetaFromResponse(response);
		assertTrue(filterMeta.getName().equalsIgnoreCase("fileTestFilter"));

	}

	@Test
	public void testSpecificFiltersIsReturned() {
		String createdFilterName = "testFilter";
		// Create Filter in DB
		Http.Response response = POST("/api/filters/personal",
				"application/json", "{'name':'" + createdFilterName + "'}");
		assertStatus(Http.StatusCode.CREATED, response);
		long createdFilterID = parseFilterMetaFromResponse(response).getId();
		// Make sure filter exists
		response = GET("/api/filters/" + createdFilterID);
		assertIsOk(response);
		Filter filter = parseFilterFromResponse(response);
		assertTrue("Filter names are not equal.Expected " + createdFilterName
				+ " and recieved " + filter.name,
				filter.name.equalsIgnoreCase(createdFilterName));
	}

	@Test
	public void testAllUsersFitlersReturned() {
		Response response = GET("/api/filters.json");
		// Only 1 fitler in
		assertTrue("Expected user to be returned 1 filter actually retruned "
				+ parseFilterMetaListFromResponse(response).size(),
				parseFilterMetaListFromResponse(response).size() == 1);
		assertIsOk(response);
	}

	@Test
	public void testNotFoundReturnedWhenFilterNoMatchingID() {
		Response response = GET("/api/filters/" + 9999);
		assertIsNotFound(response);
	}

	@Test
	public void testFilterIsDeleted() {
		// Create Filter in DB
		Http.Response response = POST(
				"/api/filters/personal",
				"application/json",
				"{'name':'testFilter','incidentTypes': ['voice_poor_sound','data_slow_connection']}");
		assertStatus(Http.StatusCode.CREATED, response);
		FilterValueObject filter = parseFilterMetaFromResponse(response);
		long createdFilterID = filter.getId();
		// Delete filter
		response = DELETE("/api/filters/" + createdFilterID);
		assertIsOk(response);
		// Make sure filter no longer in db
		response = GET("/api/filters/" + createdFilterID);
		assertIsNotFound(response);
	}

	private FilterValueObject parseFilterMetaFromResponse(Http.Response response) {
		Gson gson = new GsonBuilder().create();
		String content = getContent(response);
		JsonElement jsonElement = new JsonParser().parse(content);
		return gson.fromJson(jsonElement, FilterValueObject.class);
	}

	private Collection<FilterValueObject> parseFilterMetaListFromResponse(
			Http.Response response) {
		Type filterMetaList = new TypeToken<Collection<FilterValueObject>>() {
		}.getType();
		Gson gson = new GsonBuilder().create();
		String content = getContent(response);
		JsonElement jsonElement = new JsonParser().parse(content);
		return gson.fromJson(jsonElement, filterMetaList);

	}

	private Filter parseFilterFromResponse(Http.Response response) {
		return Filters.parseFilterFromJson(getContent(response));
	}
}
