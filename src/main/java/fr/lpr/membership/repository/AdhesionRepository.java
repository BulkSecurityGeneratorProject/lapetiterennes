package fr.lpr.membership.repository;

import fr.lpr.membership.domain.Adhesion;
import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * Spring Data JPA repository for the Adhesion entity.
 */
public interface AdhesionRepository extends JpaRepository<Adhesion,Long> {

}
