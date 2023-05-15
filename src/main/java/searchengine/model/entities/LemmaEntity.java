package searchengine.model.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "lemma", schema = "search_engine")
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lemma_id")
    private int lemmaID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (referencedColumnName = "site_id", name = "site_id")
    private SiteEntity siteID;

    @Column(name = "lemma", nullable = false, columnDefinition = "varchar(255)")
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;
}
