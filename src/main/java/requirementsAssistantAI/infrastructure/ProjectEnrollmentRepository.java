package requirementsAssistantAI.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import requirementsAssistantAI.domain.ProjectEnrollment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectEnrollmentRepository extends JpaRepository<ProjectEnrollment, UUID> {

    boolean existsByProject_IdAndUser_Id(UUID projectId, UUID userId);

    Optional<ProjectEnrollment> findByProject_IdAndUser_Id(UUID projectId, UUID userId);

    @Query("SELECT e FROM ProjectEnrollment e " +
            "JOIN FETCH e.user " +
            "LEFT JOIN FETCH e.joinedViaChatbot " +
            "WHERE e.project.id = :projectId " +
            "ORDER BY e.joinedAt DESC")
    List<ProjectEnrollment> findAllByProjectIdWithUser(@Param("projectId") UUID projectId);
}
