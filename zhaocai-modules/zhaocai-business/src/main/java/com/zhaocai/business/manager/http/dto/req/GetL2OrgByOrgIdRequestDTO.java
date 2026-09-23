package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

@Data
public class GetL2OrgByOrgIdRequestDTO extends UnderlyingPlatformBaseDTO{
    private String orgId;
}
