package com.zhaocai.business.filez.vo.res;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lijie@lenovocloud.com
 * @className: 用户类
 */
@Data
public class User implements Serializable {
    private String id;

    private String email;

    private String name;

    @JSONField(name = "display_name")
    private String displayName;

    @JSONField(name = "photo_url")
    private String photoUrl;

    @JSONField(name = "org_id")
    private String orgId;

}
