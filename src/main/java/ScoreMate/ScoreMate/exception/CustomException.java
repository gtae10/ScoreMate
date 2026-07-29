package ScoreMate.ScoreMate.exception;

/**
 * 비즈니스 로직에서 발생하는 예외를 표현하는 공통 예외 클래스.
 * 필요해지면 ErrorCode enum을 도입해 code/status를 분리해도 좋다.
 */
public class CustomException extends RuntimeException {

    public CustomException(String message) {
        super(message);
    }
}
