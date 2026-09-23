package com.zhaocai.common.signature.dto;

import lombok.Data;

/**
 * 签章发起人
 *
 * @author chenming
 * @date 2024-09-18
 */
@Data
public class SignatureCreator {

    /**
     * 发起主体 id
     */
    private String tenantId;

    /**
     * 发起主体名称
     * 即电子签约文件的发起单位，仅能传入用印流程设置的发起主体范围
     */
    private String tenantName;

    /**
     * 发起人姓名
     * 当传入的发起人未在系统中时，将使用该姓名作为发起人账号的姓名。如发起人已存在，则传入的姓名无效
     */
    private String creatorName;

    /**
     * 发起人联系方式
     */
    private String creatorContact;
}
