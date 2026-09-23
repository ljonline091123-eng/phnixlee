package com.zhaocai.business.filez.vo.res;

import lombok.Data;

import java.io.Serializable;

/**
 * @className: 文档水印类
 * @description: 文档水印的相关信息
 */
@Data
public class WaterMark implements Serializable {

    private String line1;

    private String line2;

    private String line3;

    private String line4;

    private boolean withDate = true;

    private String fontcolor = "#FD4147";

    private int transparent = 30;

    private int rotation = 315;

    private String fontsize = "20";

    private String font = "黑体";

    private int spacing = 50;

}
