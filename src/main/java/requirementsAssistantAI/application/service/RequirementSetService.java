package requirementsAssistantAI.application.service;

import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.infrastructure.RequirementHistoryRepository;
import requirementsAssistantAI.infrastructure.RequirementRepository;
import requirementsAssistantAI.infrastructure.RequirementSetRepository;
import requirementsAssistantAI.dto.RequirementSetDTO;
import requirementsAssistantAI.dto.RequirementSummaryDTO;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RequirementSetService {

    private final RequirementSetRepository requirementSetRepository;
    private final RequirementRepository requirementRepository;
    private final RequirementHistoryRepository requirementHistoryRepository;
    private final ChatbotConfigRepository chatbotConfigRepository;

    public RequirementSetService(
            RequirementSetRepository requirementSetRepository,
            RequirementRepository requirementRepository,
            RequirementHistoryRepository requirementHistoryRepository,
            ChatbotConfigRepository chatbotConfigRepository) {
        this.requirementSetRepository = requirementSetRepository;
        this.requirementRepository = requirementRepository;
        this.requirementHistoryRepository = requirementHistoryRepository;
        this.chatbotConfigRepository = chatbotConfigRepository;
    }

    @Transactional
    public RequirementSetDTO createRequirementSet(String name,String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do RequirementSet não pode ser vazio");
        }

        RequirementSet requirementSet = new RequirementSet(name.trim(),description);
        requirementSet = requirementSetRepository.save(requirementSet);
        return convertToDTO(requirementSet);
    }

    @Transactional(readOnly = true)
    public List<RequirementSetDTO> getAllRequirementSets() {
        return requirementSetRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RequirementSetDTO getRequirementSetById(@NonNull UUID id) {
        RequirementSet requirementSet = requirementSetRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("RequirementSet (projeto)", id));
        return convertToDTO(requirementSet);
    }

    @Transactional
    public void deleteRequirementSet(@NonNull UUID id) {
        RequirementSet requirementSet = requirementSetRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("RequirementSet (projeto)", id));

        chatbotConfigRepository.findByRequirementSet_Id(id).forEach(chatbotConfigRepository::delete);

        List<Requirement> requirements = requirementRepository.findByRequirementSet_Id(id);
        for (Requirement requirement : requirements) {
            requirementHistoryRepository.deleteAll(
                requirementHistoryRepository.findByRequirement_UuidOrderByCreatedAtDesc(requirement.getUuid())
            );
        }
        
        requirementSetRepository.delete(requirementSet);
    }

    @Transactional(readOnly = true)
    public List<RequirementSummaryDTO> getRequirementsBySetId(@NonNull UUID requirementSetId) {
        requirementSetRepository.findById(Objects.requireNonNull(requirementSetId))
                .orElseThrow(() -> new ResourceNotFoundException("RequirementSet (projeto)", requirementSetId));

        return requirementRepository.findByRequirementSet_Id(requirementSetId).stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    private RequirementSetDTO convertToDTO(RequirementSet requirementSet) {
        int requirementsCount = requirementSet.getRequirements() != null
                ? requirementSet.getRequirements().size()
                : 0;
        return new RequirementSetDTO(
                requirementSet.getId(),
                requirementSet.getName(),
                requirementSet.getDescription(),
                requirementSet.getCreatedAt(),
                requirementSet.getUpdatedAt(),
                requirementsCount
        );
    }

    private RequirementSummaryDTO convertToSummaryDTO(Requirement requirement) {
        return new RequirementSummaryDTO(
                requirement.getUuid(),
                requirement.getRequirementId(),
                requirement.getRefinedRequirement(),
                requirement.getAnalise(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt()
        );
    }
}
