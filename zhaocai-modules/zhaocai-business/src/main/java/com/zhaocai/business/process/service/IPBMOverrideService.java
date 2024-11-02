package com.zhaocai.business.process.service;

import com.zhaocai.business.manager.http.dto.req.BpmAuditRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmInitializeRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmListProcessLogRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmLoadTaskDefRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmAuditResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.common.core.web.bean.ResultData;

import java.util.List;


/**
 *  当前这个接口是细分每个流程业务的请求参数。
 *  实际就是更改前端用的公共请求
 *  {@link com.zhaocai.business.manager.controller.BpmController}
 *  实现类实现该方法最后都是同样的去调用下面的方法去执行
 *  {@link com.zhaocai.business.manager.http.service.BpmService}
 *
 *  相比这个接口是流程执行后的回调接口
 *  {@link com.zhaocai.business.process.service.IProcessBusinessBaseService}
 *  使用这个方法实现的回调 审批通过/驳回/..
 *  {@link com.zhaocai.business.process.service.impl.BPMProcessService#getProcessBusinessService(String)}
 */
public interface IPBMOverrideService {

    /**
     * 初始化接口
     */
    ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO);
    /**
     * 流程操作日志列表接口
     */
    ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO);
    /**
     * 审批
     */
    ResultData<BpmAuditResponseDTO> audit(BpmAuditRequestDTO requestDTO);
    /**
     * 加载定义接口
     */
    ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO);

}
