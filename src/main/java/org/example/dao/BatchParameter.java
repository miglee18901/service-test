package org.example.dao;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter @Setter
public class BatchParameter {
    @JsonAlias("values")
    private Map<String, String> paramMap = new LinkedHashMap<>();
    private boolean mockSuccess = true;
    @Min(100) @Max(599)
    private int mockHttpStatus = 200;
    private boolean mockException;
}
