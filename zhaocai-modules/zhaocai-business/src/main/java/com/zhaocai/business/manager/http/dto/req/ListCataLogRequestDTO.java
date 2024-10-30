package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

@Data
public class ListCataLogRequestDTO extends UnderlyingPlatformBaseDTO{

    /** 系统key */
    private String systemKey = "jiantou-zhaocai";
}
