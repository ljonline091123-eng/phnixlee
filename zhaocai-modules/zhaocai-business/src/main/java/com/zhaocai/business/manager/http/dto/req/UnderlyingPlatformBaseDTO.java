package com.zhaocai.business.manager.http.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import lombok.Data;

/**
 *  底层逻辑基础 DTO
 *
 * @author chenming
 * @date 2024-07-09
 */
@Data
public class UnderlyingPlatformBaseDTO {

    /**
     * 校验码
     */
    @JsonIgnore
    protected String authCode;

    /**
     * 用户token
     */
    @JsonIgnore
    protected String authorization;

    @JsonIgnore
    protected String xClientToken = "jiantou-zhaocai";

    /**
     * 账套Id
     */
    @JsonIgnore
    protected String corpIdHeader;

    /**
     * 公众号AppID
     */
    @JsonIgnore
    protected String mpAppid;

    /**
     * 小程序AppID
     */
    @JsonIgnore
    protected String miniAppId;

    /**
     * 为了防止返回数据量过大，通过该值控制
     */
    protected Boolean isLogResponseData;

    /**
     * 易料AppId
     */
    @JsonIgnore
    protected String appId;

    public String getAppId() {
        return "0c42a181e696";
    }

    public String getAuthorization() {
        if (StringUtils.isNotEmpty(authorization)) {
            return authorization;
        }
       return SecurityUtils.getMasterControlToken();
    }

    public Boolean getLogResponseData() {
        return true;
    }
}
