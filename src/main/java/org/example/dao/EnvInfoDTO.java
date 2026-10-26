package org.example.dao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvInfoDTO {
    private List<MsisdnInfoDTO> msisdn;
    private List<MsisdnInfoDTO> msisdnMember;
    private List<MsisdnInfoDTO> msisdnMaster;
    private String sourceMsisdn;
    private String executeVim;
}
