package com.zhaocai.common.signature.dto.callback.qysp;


import com.qiyuesuo.sdk.v2.bean.SignatureInfo;
import lombok.Data;

/**
 * 电子签章契约锁
 *
 * @author chenming
 * @date 2024-09-19
 */
@Data
public class SignCallBackData extends QiYueSuoCallBackData {

    /**
     * 文件id
     */
    private Long contractId;

    /**
     * 文件编号
     */
    private String sn;

    /**
     * 文件业务id
     */
    private String bizId;

    /**
     * 文件状态
     */
    private String status;

    /**
     * 文件发起主体id
     */
    private Long tenantId;

    /**
     * 文件发起主体名称
     */
    private String tenantName;

    /**
     * 文件业务数据
     */
    private String businessData;

    /**
     * 签署方信息
     */
    private SignatoryCallBackInfo signatoryInfo;
}
