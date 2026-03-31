package myexpressionfriend_api.security.util;

public class CustomJWTException extends RuntimeException{

    public CustomJWTException(String message) {
        super(message);
    }
}
