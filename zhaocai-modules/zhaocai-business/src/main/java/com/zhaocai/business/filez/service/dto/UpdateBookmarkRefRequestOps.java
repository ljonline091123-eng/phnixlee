package com.zhaocai.business.filez.service.dto;

import lombok.Data;

import java.util.List;

/**
 * 书签内容替换
 *
 * @author chenming
 * @date 2024-07-26
 */
@Data
public class UpdateBookmarkRefRequestOps extends FileZRequestBaseOps{

    private String actId;

    private UpdateBookmarkRefOps options;

    public UpdateBookmarkRefRequestOps() {
        this.actId = "UpdateBookmarkRef";
    }

    @Data
    public static  class UpdateBookmarkRefOps {
        private List<UpdateBookmarkRefArgs> args;

        public UpdateBookmarkRefOps(List<UpdateBookmarkRefArgs> args) {
            this.args = args;
        }
    }

    @Data
    public static class UpdateBookmarkRefArgs {

        /**
         * 书签名
         */
        private String bookname;

        /**
         * 书签引用类型
         */
        private String dataType;

        /**
         * 书签引用内容
         */
        private String dataRef;

        /**
         * 书签引用内容名称
         */
        private String refName;
    }
}
