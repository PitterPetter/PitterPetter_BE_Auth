package PitterPatter.loventure.authService.exception;

/**
 * 티켓이 부족할 때 발생하는 예외
 */
public class InsufficientTicketException extends RuntimeException {
    public InsufficientTicketException(String message) {
        super(message);
    }
}
