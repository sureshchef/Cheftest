package models;

import play.db.jpa.Model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Groups a set of Regions. It is identified by a unique name and is used
 * during region-level statistical reporting.
 */
@Entity
@Table(name = "rpt_region_scheme")
public class RegionScheme extends Model {
    @Column(unique = true, length = 32, nullable = false)
    public String name;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "scheme_id", nullable = false)
    @OrderBy("name ASC")
    public Set<Region> regions = new HashSet<Region>();

    public RegionScheme(String name) {
        this.name = name;
    }
}
