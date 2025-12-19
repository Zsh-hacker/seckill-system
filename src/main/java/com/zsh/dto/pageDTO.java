package com.zsh.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class pageDTO {
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum;

    @Min(value = 1, message = "每页大小不能小于1")
    private Integer pageSize = 10;

    private String orderBy;
    private Boolean asc = true;

    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
