package searchengine.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@EqualsAndHashCode
@Table(name = "site", schema = "search_engine")
public class SiteEntity {
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "site_id")
    private int siteID;

    @Enumerated(EnumType.STRING)
    @Column(name = "site_status", nullable = false)
    private Status status;

    @Column(name = "status_time", nullable = false, columnDefinition = "datetime")
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "site_url", nullable = false, columnDefinition = "varchar(255)")
    private String url;

    @Column(name = "site_name", updatable = false, columnDefinition = "varchar(255)")
    private String name;
}
