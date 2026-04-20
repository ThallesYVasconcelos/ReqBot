package requirementsAssistantAI.application.service;

import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.domain.WorkspaceMember;
import requirementsAssistantAI.domain.WorkspaceRole;
import requirementsAssistantAI.dto.AddMemberRequest;
import requirementsAssistantAI.dto.ChatMessageDTO;
import requirementsAssistantAI.dto.CreateWorkspaceRequest;
import requirementsAssistantAI.dto.WorkspaceDTO;
import requirementsAssistantAI.dto.WorkspaceMemberDTO;
import requirementsAssistantAI.infrastructure.ChatMessageRepository;
import requirementsAssistantAI.infrastructure.WorkspaceMemberRepository;
import requirementsAssistantAI.infrastructure.WorkspaceRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository memberRepository,
                            ChatMessageRepository chatMessageRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public WorkspaceDTO createWorkspace(@NonNull CreateWorkspaceRequest request, @NonNull String ownerEmail) {
        Workspace workspace = new Workspace(
                request.name(),
                request.description(),
                request.type(),
                ownerEmail
        );
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = new WorkspaceMember(workspace, ownerEmail, WorkspaceRole.OWNER);
        memberRepository.save(ownerMember);

        return toDTO(workspace, List.of(ownerMember));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDTO> getMyWorkspaces(@NonNull String userEmail) {
        return workspaceRepository.findAllAccessibleByEmail(userEmail)
                .stream()
                .map(w -> toDTO(w, memberRepository.findByWorkspace_Id(w.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkspaceDTO getById(@NonNull UUID id, @NonNull String userEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
        assertAccess(workspace, userEmail);
        return toDTO(workspace, workspace.getMembers());
    }

    @Transactional
    public WorkspaceDTO updateWorkspace(@NonNull UUID id, @NonNull CreateWorkspaceRequest request, @NonNull String userEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
        assertAdminOrOwner(workspace, userEmail);

        workspace.setName(request.name());
        if (request.description() != null) workspace.setDescription(request.description());
        workspace.setType(request.type());
        workspace = workspaceRepository.save(workspace);
        return toDTO(workspace, workspace.getMembers());
    }

    @Transactional
    public void deleteWorkspace(@NonNull UUID id, @NonNull String userEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", id));
        assertOwner(workspace, userEmail);
        workspaceRepository.delete(workspace);
    }

    @Transactional
    public WorkspaceMemberDTO addMember(@NonNull UUID workspaceId, @NonNull AddMemberRequest request, @NonNull String requesterEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        assertAdminOrOwner(workspace, requesterEmail);

        if (memberRepository.existsByWorkspace_IdAndUserEmail(workspaceId, request.userEmail())) {
            throw new IllegalStateException("Usuário " + request.userEmail() + " já é membro deste workspace.");
        }

        WorkspaceMember member = new WorkspaceMember(workspace, request.userEmail(), request.role());
        member = memberRepository.save(member);
        return toMemberDTO(member);
    }

    @Transactional
    public void removeMember(@NonNull UUID workspaceId, @NonNull String memberEmail, @NonNull String requesterEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        assertAdminOrOwner(workspace, requesterEmail);

        if (workspace.getOwnerEmail().equals(memberEmail)) {
            throw new IllegalStateException("Não é possível remover o dono do workspace.");
        }
        memberRepository.deleteByWorkspace_IdAndUserEmail(workspaceId, memberEmail);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistory(@NonNull UUID workspaceId, @NonNull String requesterEmail) {
        Workspace workspace = workspaceRepository.findByIdWithMembers(Objects.requireNonNull(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        assertAccess(workspace, requesterEmail);

        return chatMessageRepository.findByWorkspaceIdOrderByAskedAtDesc(workspaceId)
                .stream()
                .map(this::toChatMessageDTO)
                .collect(Collectors.toList());
    }

    private void assertAccess(Workspace workspace, String email) {
        boolean isMember = workspace.getOwnerEmail().equals(email) ||
                workspace.getMembers().stream().anyMatch(m -> m.getUserEmail().equals(email));
        if (!isMember) throw new ForbiddenException("Acesso negado a este workspace.");
    }

    private void assertAdminOrOwner(Workspace workspace, String email) {
        boolean allowed = workspace.getOwnerEmail().equals(email) ||
                workspace.getMembers().stream().anyMatch(m ->
                        m.getUserEmail().equals(email) &&
                        (m.getRole() == WorkspaceRole.ADMIN || m.getRole() == WorkspaceRole.OWNER));
        if (!allowed) throw new ForbiddenException("Apenas admins ou o dono podem executar esta ação.");
    }

    private void assertOwner(Workspace workspace, String email) {
        if (!workspace.getOwnerEmail().equals(email))
            throw new ForbiddenException("Apenas o dono pode excluir o workspace.");
    }

    private WorkspaceDTO toDTO(Workspace workspace, List<WorkspaceMember> members) {
        List<WorkspaceMemberDTO> memberDTOs = members.stream()
                .map(this::toMemberDTO)
                .collect(Collectors.toList());
        return new WorkspaceDTO(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getType(),
                workspace.getOwnerEmail(),
                memberDTOs,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    private WorkspaceMemberDTO toMemberDTO(WorkspaceMember m) {
        return new WorkspaceMemberDTO(m.getId(), m.getUserEmail(), m.getRole(), m.getCreatedAt());
    }

    private ChatMessageDTO toChatMessageDTO(requirementsAssistantAI.domain.ChatMessage cm) {
        return new ChatMessageDTO(
                cm.getId(),
                cm.getUserEmail(),
                cm.getQuestion(),
                cm.getAnswer(),
                cm.getAnsweredFromCache(),
                cm.getChatbotAvailable(),
                cm.getAskedAt(),
                cm.getRequirementSet() != null ? cm.getRequirementSet().getId() : null,
                cm.getRequirementSet() != null ? cm.getRequirementSet().getName() : null,
                cm.getWorkspace() != null ? cm.getWorkspace().getId() : null,
                cm.getWorkspace() != null ? cm.getWorkspace().getName() : null
        );
    }
}
