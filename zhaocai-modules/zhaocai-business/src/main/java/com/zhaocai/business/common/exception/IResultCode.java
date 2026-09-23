package com.zhaocai.business.common.exception;

import java.io.Serializable;

public interface IResultCode extends Serializable {

    String getMessage();

    int getCode();
}
