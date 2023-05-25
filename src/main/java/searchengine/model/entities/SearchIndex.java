package searchengine.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table
@Data
@EqualsAndHashCode
public class SearchIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_index_id")
    private int searchIndexID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "page_id", name = "page_id")
    private PageEntity pageID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "lemma_id", name = "lemma_id")
    private LemmaEntity lemmaID;

    @Column(name = "search_rank", nullable = false)
    private float searchRank;
}
