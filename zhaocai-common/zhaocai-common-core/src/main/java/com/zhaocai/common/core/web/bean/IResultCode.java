package com.zhaocai.common.core.web.bean;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/5/24 17:12
 */
public interface IResultCode extends Serializable {
    String getMessage();

    int getCode();
}
