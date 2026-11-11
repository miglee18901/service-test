package org.example.constants;

public final class HttpRequestConstants {
    private HttpRequestConstants() { }
    public static final class ErrorCode {
        public static final String Success = "0";
        public static final String Fail = "-1";
        private ErrorCode() { }
    }
    public static final class Description {
        public static final String Success = "Success";
        public static final String BATCH_EXECUTE_FAILED = "BATCH_EXECUTE_FAILED";
        public static final String BATCH_EXECUTE_CANNOT_SENT_REQUEST = "BATCH_EXECUTE_CANNOT_SENT_REQUEST";
        public static final String BATCH_EXECUTE_NOT_SUCCESS_ALL = "BATCH_EXECUTE_NOT_SUCCESS_ALL";
        private Description() { }
    }
}
