package com.zhaocai.business.manager.http.dto.res;


import lombok.Data;

import java.util.List;


/**
 * 流程服务-提交返回
 */
@Data
public class BpmSubmitResponseDTO {

    /**
     * 流程状态中文描述
     */
    private String processStatusDesc;

    /**
     * 流程状态
     */
    private String processStatus;

    /**
     * 运行时用户对象
     */
    private List<CurCandidateListDTO> curCandidateList;

    /**
     * 流程实例ID
     */
    private String processId;
}
