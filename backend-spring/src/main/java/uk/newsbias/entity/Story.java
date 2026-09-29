package uk.newsbias.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stories")
@Data
@NoArgsConstructor
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String title;

    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;

    private int articleCount;
    private int leftCount;
    private int centreCount;
    private int rightCount;

    private boolean blindSpot;
    private String blindSpotSide;   // "left" | "centre" | "right" | null

    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] centroid;        // float32 bytes, 384-dim

    @OneToMany(mappedBy = "story", fetch = FetchType.LAZY)
    private List<Article> articles = new ArrayList<>();
}
