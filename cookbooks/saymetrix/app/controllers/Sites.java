package controllers;

import com.google.gson.*;
import controllers.deadbolt.Deadbolt;
import controllers.deadbolt.RoleHolderPresent;
import java.util.List;
import models.*;
import org.hibernate.Session;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With(Deadbolt.class)
@RoleHolderPresent
public class Sites extends Controller {

    @Before
    static void setConnecteduser() {
        if (Security.isConnected()) {
            WebUser user = WebUser.find("byEmail", Security.connected()).first();
            renderArgs.put("webuser", user.firstname);

            // Account Managers should only be able to see their own accounts
            if("kam".equals(user.role.name)) {
                ((Session) JPA.em().getDelegate()).enableFilter("manager").setParameter("manager_id", user.id);
            }
        }
    }

    public static void findAll() {
        renderJSON(new GsonBuilder().create().toJson(Site.findAll()));
    }
    
    public static void getForEvent(Long eventId) {
        NetworkEvent ne = NetworkEvent.findById(eventId);
        List<Site> sites = ne.sites;
        renderJSON(new GsonBuilder().create().toJson(sites));
    }
    
    public static void findMapSites(String bounds) {
        Filter filter = Filters.parseFilterFromRequestBody();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        
        List<Site> requestedSites = Site.findAllFor(filter.incidentPeriod, bounds);
        renderJSON(gson.toJson(requestedSites));
    }

}
