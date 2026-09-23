package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

@Data
public class GetL2OrgByUserIdRequestDTO extends UnderlyingPlatformBaseDTO{
    private String userId;
}
