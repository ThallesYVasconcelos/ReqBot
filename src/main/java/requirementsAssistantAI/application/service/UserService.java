package requirementsAssistantAI.application.service;

import requirementsAssistantAI.domain.AppUser;
import requirementsAssistantAI.infrastructure.AppUserRepository;
import requirementsAssistantAI.dto.AppUserDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public List<AppUserDTO> getAllUsers() {
        return appUserRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AppUserDTO convertToDTO(AppUser user) {
        return new AppUserDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPictureUrl(),
                user.getRole() != null ? user.getRole().name() : "USER",
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
