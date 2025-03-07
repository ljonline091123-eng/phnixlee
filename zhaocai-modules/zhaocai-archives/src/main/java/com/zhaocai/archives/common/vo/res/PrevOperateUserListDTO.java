package com.zhaocai.archives.common.vo.res;

import lombok.Data;

@Data
public class PrevOperateUserListDTO {
    private boolean appoint;

    private boolean completed;

    private String userId;

    private String userName;
}
