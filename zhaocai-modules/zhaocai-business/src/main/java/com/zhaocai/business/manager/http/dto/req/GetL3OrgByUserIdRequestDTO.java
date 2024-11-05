package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

@Data
public class GetL3OrgByUserIdRequestDTO extends UnderlyingPlatformBaseDTO{
    private String userId;
}
