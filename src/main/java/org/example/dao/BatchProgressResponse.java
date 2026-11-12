package org.example.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchProgressResponse<T> {
    private double percent;
    private String status;
    private T result;

    public BatchProgressResponse(double percent, String status) {
        this(percent, status, null);
    }
}
