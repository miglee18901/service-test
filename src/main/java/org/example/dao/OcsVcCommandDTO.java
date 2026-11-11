package org.example.dao;

import lombok.Getter;
import lombok.Setter;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class OcsVcCommandDTO {
    @NotNull @Positive
    private Long id;
    @NotBlank
    private String name;
    @NotBlank
    private String code = "<request><path>/mock-command</path><method>POST</method><body>mock</body></request>";
    private String successCode = "SUCCESS";
    @NotNull @Valid
    private List<@NotNull OcsVcParameterDTO> listParameter = new ArrayList<>();
}
