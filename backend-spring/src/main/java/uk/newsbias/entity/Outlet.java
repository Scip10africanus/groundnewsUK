package uk.newsbias.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "outlets")
@Data
@NoArgsConstructor
public class Outlet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    private String bias;

    @Column(nullable = false)
    private String rssUrl;

    @Column(nullable = false)
    private String logoFilename;

    @OneToMany(mappedBy = "outlet", fetch = FetchType.LAZY)
    private List<Article> articles = new ArrayList<>();
}
