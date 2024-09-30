package com.zhaocai.business.filez.service.dto;


import lombok.Data;

import java.math.BigDecimal;

/**
 * 平铺水印
 *
 * @author chenming
 * @date 2024-07-30
 */
@Data
public class TiledWatermark {

    /**
     * 水印内容
     */
    private String line1;

    /**
     * 水印内容2
     */
    private String line2;

    /**
     * 水印内容3
     */
    private String line3;

    /**
     * 水印内容4
     */
    private String line4;

    /**
     * 水印是否带日期。默认值true。
     */
    private Boolean withDate;

    /**
     * 字体。默认黑体。
     */
    private String font;

    /**
     * 字体颜色。颜色值为6位十六进制颜色值
     */
    private String fontcolor;

    /**
     * 字号。默认值16。
     */
    private String fontsize;

    /**
     * 水印字体是否加粗。仅在操作图片时支持。
     */
    private Boolean isFontBold;

    /**
     * 水印字体是否为斜体。仅在操作图片时支持
     */
    private Boolean isFontItalic;

    /**
     * 水印透明度。默认值70。
     */
    private BigDecimal transparent;

    /**
     * 水印倾斜角度。默认值315。
     */
    private BigDecimal rotation;

    /**
     * 水印间距。默认值50。
     */
    private BigDecimal spacing;

    public TiledWatermark(String line) {
        this.line1 = line;
        this.withDate = false;
        this.font = "黑体";
        this.fontcolor = "#DC134C";
        this.fontsize = "12";

        this.transparent = new BigDecimal("90");
        this.rotation = new BigDecimal("45");
        this.spacing = new BigDecimal("60");
    }

}
