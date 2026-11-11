package org.example.dao;

import lombok.Getter;
import lombok.Setter;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter @Setter
public class CommandRequest {
    private String path;
    private String method;
    private String body;
    private Map<String, String> header = new LinkedHashMap<>();
}
