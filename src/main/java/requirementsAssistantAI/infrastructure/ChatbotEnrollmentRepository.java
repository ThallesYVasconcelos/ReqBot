package requirementsAssistantAI.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import requirementsAssistantAI.domain.ChatbotEnrollment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatbotEnrollmentRepository extends JpaRepository<ChatbotEnrollment, UUID> {

    boolean existsByChatbot_IdAndUser_Id(UUID chatbotId, UUID userId);

    Optional<ChatbotEnrollment> findByChatbot_IdAndUser_Id(UUID chatbotId, UUID userId);

    @Query("SELECT e FROM ChatbotEnrollment e " +
            "JOIN FETCH e.chatbot c " +
            "JOIN FETCH c.requirementSet " +
            "WHERE e.user.id = :userId ORDER BY e.joinedAt DESC")
    List<ChatbotEnrollment> findAllByUserIdWithChatbot(@Param("userId") UUID userId);
}
