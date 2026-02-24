package ai.planmate.shared.exception;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

    private final String errorCode;

    public ConflictException(String message) {
        super(message);
        this.errorCode = "CONFLICT";
    }

    public ConflictException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
