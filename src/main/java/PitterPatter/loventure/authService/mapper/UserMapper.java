package PitterPatter.loventure.authService.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import PitterPatter.loventure.authService.dto.request.ProfileUpdateRequest;
import PitterPatter.loventure.authService.repository.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * ProfileUpdateRequest의 null이 아닌 필드만 User 엔티티에 업데이트
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromProfileRequest(ProfileUpdateRequest request, @MappingTarget User user);
}