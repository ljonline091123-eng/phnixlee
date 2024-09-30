package com.zhaocai.business.filez.vo.res;

import lombok.Data;

import java.io.Serializable;

/**
 * @className: 文档权修订类
 * @description: 文档修订的相关信息
 */
@Data
public class Extension implements Serializable {

    private boolean previewWithTrackChange = false;

    private boolean trackChangeForceOn = false;

}
