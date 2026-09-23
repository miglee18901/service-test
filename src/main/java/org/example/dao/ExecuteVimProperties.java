package org.example.dao;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "execute-vim")
public class ExecuteVimProperties {
    private List<String> options = Collections.emptyList();
}