package org.example.constants;

public final class ServiceConstants {
    private ServiceConstants() { }
    // Constant values are not visible in the screenshots: local mock values.
    public static final class ExecuteStatus {
        public static final Integer FINISH_SUCCESS_STATUS = 0;
        public static final Integer FINISH_FAIL_STATUS = -1;
        public static final Integer NOT_AUTHEN = -2;
        public static final Integer NOT_FOUND_SERVER = -3;
        private ExecuteStatus() { }
    }
    public static final class ExecuteDescription {
        public static final String GET_TOKEN_FAILED = "GET_TOKEN_FAILED";
        public static final String NOT_FOUND_SERVER = "NOT_FOUND_SERVER";
        public static final String EXCEPTION_IN_EXECUTE_BATCH = "EXCEPTION_IN_EXECUTE_BATCH";
        private ExecuteDescription() { }
    }
    public static final class BatchFinishStatus {
        public static final String RUNNING = "RUNNING";
        public static final String FINISH = "FINISH";
        private BatchFinishStatus() { }
    }
}
