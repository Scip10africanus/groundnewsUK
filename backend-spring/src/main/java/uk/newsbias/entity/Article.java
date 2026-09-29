package uk.newsbias.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles",
       indexes = {
           @Index(columnList = "guid", unique = true),
           @Index(columnList = "outlet_id, published_at"),
       })
@Data
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id")
    private Story story;

    @Column(unique = true, nullable = false, length = 2048)
    private String guid;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(length = 2048)
    private String url;

    private LocalDateTime publishedAt;
    private LocalDateTime ingestedAt;

    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] embedding;   // float32 bytes, 384-dim
}
