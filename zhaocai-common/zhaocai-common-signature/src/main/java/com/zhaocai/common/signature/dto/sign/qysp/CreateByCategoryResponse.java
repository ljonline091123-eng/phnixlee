package com.zhaocai.common.signature.dto.sign.qysp;


import lombok.Data;

/**
 * 创建电子签约响应
 *
 * @author chenming
 * @date 2024-09-18
 */
@Data
public class CreateByCategoryResponse extends QiYueSuoResponse{

    /**
     * 电子签约文件id
     */
    private String contractId;
}
