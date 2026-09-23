package com.zhaocai.business.filez.service.dto;

import lombok.Data;

/**
 * 加水印请求DTO
 */
@Data
public class WatermarkRequestOps extends FileZRequestBaseOps {

    private String actId;

    private WatermarkOptions options;

    public WatermarkRequestOps(WatermarkOptions options) {
        this.actId = "ApplyWatermark";
        this.options = options;
    }

    @Data
    public static class WatermarkOptions {
        /**
         * 水印文本
         */
        private String text;

        /**
         * 水印颜色
         */
        private String fontcolor;

        /**
         * 水印文本字体大小，取值范围[36, 144]，值越大水印越大
         */
        private String fontsize;

        /**
         * 水印文本字体
         */
        private String font;

        /**
         * 水印倾斜度，取值范围[0, 360]，值为0时水平，值增大时水印顺时针旋转
         */
        private String rotation;

        /**
         * 水印透明度，取值范围[0, 1]，值越大越清晰
         */
        private String opacity;

        /**
         * 水印位置，可选值及含义：
         * TOP_LEFT : 顶部靠左
         * TOP_CENTER : 顶部中间
         * TOP_RIGHT : 顶部靠右
         * CENTER_LEFT : 中间靠左
         * CENTER : 正中
         * CENTER_RIGHT : 中间靠右
         * BOTTOM_LEFT : 底部靠左
         * BOTTOM_CENTER : 底部中间
         * BOTTOM_RIGHT : 底部靠右
         */
        private String position;

        public WatermarkOptions(String text) {
            this.text = text;
            this.fontcolor = "#DC134C";
            this.fontsize = "30";
            this.font = "黑体";
            this.rotation = "315";
            this.opacity = "0.2";
            this.position = "BOTTOM_RIGHT";
        }
    }
}
