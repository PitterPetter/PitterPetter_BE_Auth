package PitterPatter.loventure.authService.exception;

/**
 * 커플을 찾을 수 없을 때 발생하는 예외
 */
public class CoupleNotFoundException extends RuntimeException {
    public CoupleNotFoundException(String message) {
        super(message);
    }
}
