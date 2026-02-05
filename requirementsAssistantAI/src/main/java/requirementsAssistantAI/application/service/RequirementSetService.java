package requirementsAssistantAI.application.service;

import requirementsAssistantAI.domain.Requirement;
import requirementsAssistantAI.domain.RequirementSet;
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

    public RequirementSetService(
            RequirementSetRepository requirementSetRepository,
            RequirementRepository requirementRepository,
            RequirementHistoryRepository requirementHistoryRepository) {
        this.requirementSetRepository = requirementSetRepository;
        this.requirementRepository = requirementRepository;
        this.requirementHistoryRepository = requirementHistoryRepository;
    }

    @Transactional
    public RequirementSetDTO createRequirementSet(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do RequirementSet não pode ser vazio");
        }

        RequirementSet requirementSet = new RequirementSet(name.trim());
        requirementSet = requirementSetRepository.save(requirementSet);
        return convertToDTO(requirementSet);
    }

    public List<RequirementSetDTO> getAllRequirementSets() {
        return requirementSetRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public RequirementSetDTO getRequirementSetById(@NonNull UUID id) {
        RequirementSet requirementSet = requirementSetRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("RequirementSet não encontrado com ID: " + id));
        return convertToDTO(requirementSet);
    }

    @Transactional
    public void deleteRequirementSet(@NonNull UUID id) {
        RequirementSet requirementSet = requirementSetRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("RequirementSet não encontrado com ID: " + id));
        
        // Deleta os RequirementHistory antes de deletar os Requirements
        List<Requirement> requirements = requirementRepository.findByRequirementSet_Id(id);
        for (Requirement requirement : requirements) {
            requirementHistoryRepository.deleteAll(
                requirementHistoryRepository.findByRequirement_UuidOrderByCreatedAtDesc(requirement.getUuid())
            );
        }
        
        requirementSetRepository.delete(requirementSet);
    }

    public List<RequirementSummaryDTO> getRequirementsBySetId(@NonNull UUID requirementSetId) {
        requirementSetRepository.findById(Objects.requireNonNull(requirementSetId))
                .orElseThrow(() -> new RuntimeException("RequirementSet não encontrado com ID: " + requirementSetId));

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
                null,
                requirement.getStatus(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt()
        );
    }
}
