package com.zhaocai.business.filez.vo.res;

import lombok.Data;

import java.io.Serializable;

/**
 * @className: 文档权限类
 * @description: 文档权限的相关信息
 */
@Data
public class DocPermission implements Serializable {

    private boolean write = true;

    private boolean read = true;

    private boolean download = true;

    private boolean comment = true;

    private boolean print = true;
}
