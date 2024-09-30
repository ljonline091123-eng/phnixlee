package com.zhaocai.business.filez.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateBookmarkRefRequestDTO extends DocumentUpdateRequestDTO{

    /**
     * 书签内容
     */
    private List<UpdateBookmarkRefListRequestDTO> bookmarkRefList;

    @Data
    public static class UpdateBookmarkRefListRequestDTO {
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
