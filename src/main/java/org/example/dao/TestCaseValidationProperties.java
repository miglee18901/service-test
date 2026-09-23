package org.example.dao;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "test-case.validation")
public class TestCaseValidationProperties {
    private Map<String, List<String>> blockedValueCharacteristics = new LinkedHashMap<>();
}
