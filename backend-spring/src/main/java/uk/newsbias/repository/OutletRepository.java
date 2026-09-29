package uk.newsbias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.newsbias.entity.Outlet;

import java.util.Optional;

public interface OutletRepository extends JpaRepository<Outlet, Long> {
    Optional<Outlet> findBySlug(String slug);
}
