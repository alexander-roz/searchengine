package searchengine.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Data
@EqualsAndHashCode
@Table(name = "page", schema = "search_engine",
        indexes =
        @Index(name = "path_index",
                columnList = "page_path",
                unique = true))

public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "page_id")
    private int pageID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (referencedColumnName = "site_id", name = "site_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SiteEntity siteID;

    @Column(name = "page_path", nullable = false,
            columnDefinition = "varchar(255)", unique = true)
    private String pagePath;

    @Column(name = "page_code", nullable = false)
    private int pageCode;

    @Column(name = "page_content", nullable = false, columnDefinition = "mediumtext")
    private String pageContent;
}
