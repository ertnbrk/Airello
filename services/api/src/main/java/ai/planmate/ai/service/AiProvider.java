package ai.planmate.ai.service;

import java.util.Map;

public interface AiProvider {

    String name();

    boolean supportsTools();

    AiResult invoke(String prompt, Map<String, Object> tools, Map<String, Object> context);

    record AiResult(
            String content,
            Map<String, Object> toolOutput,
            int tokensUsed,
            boolean success,
            String error) {

        public static AiResult success(
                String content, Map<String, Object> toolOutput, int tokensUsed) {
            return new AiResult(content, toolOutput, tokensUsed, true, null);
        }

        public static AiResult failure(String error) {
            return new AiResult(null, null, 0, false, error);
        }
    }
}
