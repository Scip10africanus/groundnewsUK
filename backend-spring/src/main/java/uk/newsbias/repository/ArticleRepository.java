package uk.newsbias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.newsbias.entity.Article;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByGuid(String guid);

    boolean existsByGuid(String guid);

    @Query("SELECT a FROM Article a JOIN FETCH a.outlet WHERE a.story IS NULL " +
           "AND a.ingestedAt >= :cutoff AND a.embedding IS NOT NULL " +
           "ORDER BY a.publishedAt ASC NULLS LAST")
    List<Article> findUnassignedSince(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT a FROM Article a JOIN FETCH a.outlet WHERE a.story.id = :storyId " +
           "ORDER BY a.publishedAt ASC NULLS LAST")
    List<Article> findByStoryId(@Param("storyId") Long storyId);

    @Query("SELECT DISTINCT a.outlet.slug FROM Article a WHERE a.story.id = :storyId")
    List<String> findOutletSlugsByStoryId(@Param("storyId") Long storyId);
}
