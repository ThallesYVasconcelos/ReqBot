package requirementsAssistantAI.application.service;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.domain.WorkspaceMember;
import requirementsAssistantAI.domain.WorkspaceRole;
import requirementsAssistantAI.infrastructure.RequirementRepository;
import requirementsAssistantAI.infrastructure.RequirementSetRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;
import requirementsAssistantAI.infrastructure.WorkspaceRepository;

import java.util.Objects;
import java.util.UUID;

@Service
public class WorkspaceAuthorizationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final RequirementSetRepository requirementSetRepository;
    private final RequirementRepository requirementRepository;

    public WorkspaceAuthorizationService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            RequirementSetRepository requirementSetRepository,
            RequirementRepository requirementRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.requirementSetRepository = requirementSetRepository;
        this.requirementRepository = requirementRepository;
    }

    @Transactional(readOnly = true)
    public Workspace requireOwnerOrAdmin(@NonNull UUID workspaceId, @NonNull UUID userId) {
        Workspace workspace = findWorkspace(workspaceId);
        WorkspaceMember membership = membership(workspaceId, userId);
        if (membership.getRole() != WorkspaceRole.OWNER && membership.getRole() != WorkspaceRole.ADMIN) {
            throw new ForbiddenException("Acesso administrativo negado para este workspace.");
        }
        return workspace;
    }

    @Transactional(readOnly = true)
    public Workspace requireOwner(@NonNull UUID workspaceId, @NonNull UUID userId) {
        Workspace workspace = findWorkspace(workspaceId);
        WorkspaceMember membership = membership(workspaceId, userId);
        if (membership.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Apenas o owner pode executar esta ação.");
        }
        return workspace;
    }

    @Transactional(readOnly = true)
    public RequirementSet requireOwnerOrAdminForProject(@NonNull UUID projectId, @NonNull UUID userId) {
        RequirementSet project = requirementSetRepository.findById(Objects.requireNonNull(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", projectId));
        if (project.getWorkspace() == null) {
            throw new ForbiddenException("O projeto não está associado a um workspace.");
        }
        requireOwnerOrAdmin(project.getWorkspace().getId(), userId);
        return project;
    }

    @Transactional(readOnly = true)
    public Requirement requireOwnerOrAdminForRequirement(@NonNull UUID requirementId, @NonNull UUID userId) {
        Requirement requirement = requirementRepository.findById(Objects.requireNonNull(requirementId))
                .orElseThrow(() -> new ResourceNotFoundException("Requisito", requirementId));
        RequirementSet project = requirement.getRequirementSet();
        if (project == null || project.getWorkspace() == null) {
            throw new ForbiddenException("O requisito não está associado a um workspace.");
        }
        requireOwnerOrAdmin(project.getWorkspace().getId(), userId);
        return requirement;
    }

    private Workspace findWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(Objects.requireNonNull(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
    }

    private WorkspaceMember membership(UUID workspaceId, UUID userId) {
        return memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, Objects.requireNonNull(userId))
                .orElseThrow(() -> new ForbiddenException("Você não possui acesso a este workspace."));
    }
}
