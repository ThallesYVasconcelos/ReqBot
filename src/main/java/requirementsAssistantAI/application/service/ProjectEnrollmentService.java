package requirementsAssistantAI.application.service;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.domain.AppUser;
import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.domain.ProjectEnrollment;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.dto.ProjectUserDTO;
import requirementsAssistantAI.infrastructure.ProjectEnrollmentRepository;
import requirementsAssistantAI.infrastructure.RequirementSetRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProjectEnrollmentService {

    private final ProjectEnrollmentRepository enrollmentRepository;
    private final RequirementSetRepository projectRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public ProjectEnrollmentService(
            ProjectEnrollmentRepository enrollmentRepository,
            RequirementSetRepository projectRepository,
            WorkspaceAuthorizationService authorizationService) {
        this.enrollmentRepository = enrollmentRepository;
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public ProjectEnrollment ensureEnrollment(
            @NonNull RequirementSet project,
            @NonNull AppUser user,
            ChatbotConfig joinedViaChatbot) {
        return enrollmentRepository.findByProject_IdAndUser_Id(project.getId(), user.getId())
                .orElseGet(() -> enrollmentRepository.save(
                        new ProjectEnrollment(project, user, joinedViaChatbot)));
    }

    @Transactional(readOnly = true)
    public List<ProjectUserDTO> listUsers(
            @NonNull UUID workspaceId,
            @NonNull UUID projectId,
            @NonNull UUID requesterUserId) {
        RequirementSet project = projectRepository.findById(Objects.requireNonNull(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", projectId));
        if (project.getWorkspace() == null || !project.getWorkspace().getId().equals(workspaceId)) {
            throw new ForbiddenException("O projeto nÃ£o pertence ao workspace informado.");
        }
        authorizationService.requireOwnerOrAdmin(workspaceId, requesterUserId);
        return enrollmentRepository.findAllByProjectIdWithUser(projectId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private ProjectUserDTO toDTO(ProjectEnrollment enrollment) {
        AppUser user = enrollment.getUser();
        ChatbotConfig chatbot = enrollment.getJoinedViaChatbot();
        return new ProjectUserDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                chatbot != null ? chatbot.getId() : null,
                chatbot != null ? chatbot.getName() : null,
                enrollment.getJoinedAt());
    }
}
