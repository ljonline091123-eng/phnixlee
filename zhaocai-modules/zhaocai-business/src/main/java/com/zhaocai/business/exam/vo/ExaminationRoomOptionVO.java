package com.zhaocai.business.exam.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class ExaminationRoomOptionVO {
    private String value;
    private String label;

    public ExaminationRoomOptionVO(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
