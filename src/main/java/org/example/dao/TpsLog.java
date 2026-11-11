package org.example.dao;

import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor @ToString
public class TpsLog {
    private long countSent;
    private double tps;
    private long totalSent;
}
