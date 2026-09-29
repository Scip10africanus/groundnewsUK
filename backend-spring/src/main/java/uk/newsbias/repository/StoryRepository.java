package uk.newsbias.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.newsbias.entity.Story;

import java.time.LocalDateTime;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {

    @Query("SELECT s FROM Story s WHERE s.lastSeenAt >= :cutoff AND s.articleCount > 0")
    List<Story> findActiveSince(@Param("cutoff") LocalDateTime cutoff);

    // Feed queries
    @Query("SELECT s FROM Story s WHERE s.articleCount > 0 ORDER BY s.articleCount DESC, s.lastSeenAt DESC")
    List<Story> findAllByOrderByArticleCountDesc(Pageable pageable);

    @Query("SELECT s FROM Story s WHERE s.articleCount > 0 AND s.leftCount > 0 ORDER BY s.articleCount DESC, s.lastSeenAt DESC")
    List<Story> findByLeftCoverage(Pageable pageable);

    @Query("SELECT s FROM Story s WHERE s.articleCount > 0 AND s.centreCount > 0 ORDER BY s.articleCount DESC, s.lastSeenAt DESC")
    List<Story> findByCentreCoverage(Pageable pageable);

    @Query("SELECT s FROM Story s WHERE s.articleCount > 0 AND s.rightCount > 0 ORDER BY s.articleCount DESC, s.lastSeenAt DESC")
    List<Story> findByRightCoverage(Pageable pageable);

    @Query("SELECT s FROM Story s WHERE s.articleCount > 0 AND s.blindSpot = true ORDER BY s.articleCount DESC, s.lastSeenAt DESC")
    List<Story> findBlindSpots(Pageable pageable);

    // Trending: top 10 by article count in last 24 hrs
    @Query("SELECT s FROM Story s WHERE s.lastSeenAt >= :since AND s.articleCount > 0 ORDER BY s.articleCount DESC")
    List<Story> findTrending(@Param("since") LocalDateTime since, Pageable pageable);
}
