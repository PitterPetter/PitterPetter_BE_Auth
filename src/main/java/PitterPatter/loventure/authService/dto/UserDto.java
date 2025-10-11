package PitterPatter.loventure.authService.dto;

import java.math.BigInteger;

import PitterPatter.loventure.authService.repository.User;

public record UserDto(
    BigInteger userId,
    String email,
    String name,
    String nickname
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getNickname()
        );
    }
}