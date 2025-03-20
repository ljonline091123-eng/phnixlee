package com.zhaocai.business.manager.http.service;

import cn.hutool.json.JSONUtil;
import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.*;
import com.zhaocai.business.process.domain.BpmLog;
import com.zhaocai.business.process.service.IBpmLogService;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.system.api.flowable.RemoteFlowableService;
import com.zhaocai.system.api.system.RemoteSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 流程服务，具体业务都是调用这里的接口，也可以直接被前端直接调用
 */
@Slf4j
@Service
public class BpmService {

    @Autowired
    private RemoteSystemService remoteSystemService;
    @Autowired
    private IBpmLogService bpmLogService;

    @Autowired
    private UnderlingSystemService underlingSystemService;


    @Autowired
    private IMinProjectService minProjectService;


    @Resource
    private RemoteFlowableService remoteFlowableService;

    /**
     * 初始化接口
     *
     * @param requestDTO
     * @return
     */
    public BpmInitializeResponseDTO initialize(BpmInitializeRequestDTO requestDTO) {
        requestDTO.setOrgPenetrate(true);
        AjaxResult initialize = remoteFlowableService.initialize(JSONUtil.toBean(JSONUtil.toJsonStr(requestDTO), Map.class));
        BpmInitializeResponseDTO responseDTO = JSONUtil.toBean(JSONUtil.toJsonStr(initialize.get("data")), BpmInitializeResponseDTO.class);
//        BpmInitializeResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_INITIALIZE,
//                BpmInitializeResponseDTO.class, requestDTO);
        try {
            /* 流程操作记录 */
            bpmLogService.save(BpmLog.builder()
                    .bpmType("初始化接口")
                    .businessId(requestDTO.getBusinessId())
                    .wfProcessId(requestDTO.getProcessId())
                    .bpmUrl(UnderlingPlatformUrlEnum.BPM_OPERATE_INITIALIZE.getUrl())
                    .bpmParam(JSONUtil.parse(requestDTO).toString())
                    .bpmResponse(JSONUtil.parse(responseDTO).toString())
                    .build());
        } catch (Exception e) {
            log.error("[  bpmLogService报错  ]{}", e.getMessage());
        }
        return responseDTO;
    }

    /**
     * 流程操作日志列表接口
     *
     * @param requestDTO
     * @return
     */
    public List<BpmListProcessLogResponseDTO> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
//        List<BpmListProcessLogResponseDTO> responseDTO = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_LISTPROCESSLOG,
//                BpmListProcessLogResponseDTO.class,requestDTO);
//        try{
//            /* 流程操作记录 */
//            bpmLogService.save(BpmLog.builder()
//                    .bpmType("流程操作日志列表接口")
//                    .businessId(requestDTO.getBusinessId())
//                    .wfProcessId(requestDTO.getProcessId())
//                    .bpmUrl(UnderlingPlatformUrlEnum.BPM_OPERATE_LISTPROCESSLOG.getUrl())
//                    .bpmParam(JSONUtil.parse(requestDTO).toString())
//                    .bpmResponse(JSONUtil.parse(responseDTO).toString())
//                    .build());
//        }catch (Exception e){log.error("[  bpmLogService报错  ]{}",e.getMessage());}
//        return  responseDTO;
        return null;
    }

    /**
     * 弃审接口
     *
     * @param requestDTO
     * @return
     */
    public BpmDisCardResponseDTO disCard(BpmDisCardRequestDTO requestDTO) {
//        BpmDisCardResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_DISCARD,
//                BpmDisCardResponseDTO.class,requestDTO);
//        try{
//            /* 流程操作记录 */
//            bpmLogService.save(BpmLog.builder()
//                    .bpmType("弃审接口")
//                    .businessId(requestDTO.getBusinessId())
//                    .wfProcessId(requestDTO.getProcessId())
//                    .bpmUrl(UnderlingPlatformUrlEnum.BPM_OPERATE_DISCARD.getUrl())
//                    .bpmParam(JSONUtil.parse(requestDTO).toString())
//                    .bpmResponse(JSONUtil.parse(responseDTO).toString())
//                    .build());
//        }catch (Exception e){log.error("[  bpmLogService报错  ]{}",e.getMessage());}
//        return  responseDTO;
        return null;
    }

    /**
     * 撤销
     *
     * @param requestDTO
     * @return
     */
    public BpmRevokeResponseDTO revoke(BpmRevokeRequestDTO requestDTO) {
//        BpmRevokeResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_REVOKE,
//                BpmRevokeResponseDTO.class,requestDTO);
//        try{
//            /* 流程操作记录 */
//            bpmLogService.save(BpmLog.builder()
//                    .bpmType("撤销接口")
//                    .businessId(requestDTO.getBusinessId())
//                    .wfProcessId(requestDTO.getProcessId())
//                    .bpmUrl(UnderlingPlatformUrlEnum.BPM_OPERATE_REVOKE.getUrl())
//                    .bpmParam(JSONUtil.parse(requestDTO).toString())
//                    .bpmResponse(JSONUtil.parse(responseDTO).toString())
//                    .build());
//        }catch (Exception e){log.error("[  bpmLogService报错  ]{}",e.getMessage());}
//        return  responseDTO;
        return null;
    }

    /**
     * 作废
     *
     * @param requestDTO
     * @return
     */
    public BpmDeleteResponseDTO delete(BpmDeleteRequestDTO requestDTO) {
//        BpmDeleteResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_DELETE,
//                BpmDeleteResponseDTO.class,requestDTO);
//        try{
//            /* 流程操作记录 */
//            bpmLogService.save(BpmLog.builder()
//                    .bpmType("作废接口")
//                    .businessId(requestDTO.getBusinessId())
//                    .wfProcessId(requestDTO.getProcessId())
//                    .bpmUrl(UnderlingPlatformUrlEnum.BPM_OPERATE_DELETE.getUrl())
//                    .bpmParam(JSONUtil.parse(requestDTO).toString())
//                    .bpmResponse(JSONUtil.parse(responseDTO).toString())
//                    .build());
//        }catch (Exception e){log.error("[  bpmLogService报错  ]{}",e.getMessage());}
//        return  responseDTO;
        return null;
    }


    /**
     * 审批接口
     *
     * @param requestDTO
     * @return
     */
    public BpmAuditResponseDTO audit(BpmAuditRequestDTO requestDTO) {
        requestDTO.setOrgPenetrate(true);
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", requestDTO.getCurTaskId());
        map.put("comment", requestDTO.getOperateComment());
        map.put("instanceId", requestDTO.getProcessId());
        map.put("variables", JSONUtil.toBean(JSONUtil.toJsonStr(requestDTO), Map.class));
        AjaxResult complete;
        if (requestDTO.isPass()) {
            complete = remoteFlowableService.completeCopy(map);
        } else {
            complete = remoteFlowableService.rejectCopy(map);
        }

        BpmAuditResponseDTO responseDTO = JSONUtil.toBean(JSONUtil.toJsonStr(complete.get("data")), BpmAuditResponseDTO.class);
//        BpmAuditResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_AUDIT,
//                BpmAuditResponseDTO.class,requestDTO);
        try {
            /* 流程操作记录 */
            bpmLogService.save(BpmLog.builder()
                    .bpmType("审批接口")
                    .businessId(requestDTO.getBusinessId())
                    .wfProcessId(requestDTO.getProcessId())
                    .bpmUrl(UnderlingPlatformUrlEnum.BPM_OPERATE_AUDIT.getUrl())
                    .bpmParam(JSONUtil.parse(requestDTO).toString())
                    .bpmResponse(JSONUtil.parse(responseDTO).toString())
                    .build());
        } catch (Exception e) {
            log.error("[  bpmLogService报错  ]{}", e.getMessage());
        }
        return responseDTO;
    }

    /**
     * 提交接口
     *
     * @param requestDTO
     * @return
     */
    public BpmSubmitResponseDTO submit(BpmSubmitRequestDTO requestDTO) {
//        requestDTO.setOrgPenetrate(true);
//        log.info("[流程提交参数对象]{}",requestDTO);
//
//
//        String processKeyReplace = requestDTO.getProcessKey();
//        String processKey = requestDTO.getProcessKey();
//        if (processKey.contains("{org}")) {
//            /* 获取二级单位 */
//            String org = underlingSystemService.getL2OrgByOrgId(SecurityUtils.getThridOrgId());
//            /* 获取三级单位 */
//            String orgThree = underlingSystemService.getL3OrgByOrgId(SecurityUtils.getThridOrgId());
//            processKey = processKeyReplace.replace("{org}", org);
//            /* 获取所有流程 */
//            List<ListCataLogDTO> listCataLogDTOS = underlingSystemService.listCatalog();
//            if (listCataLogDTOS != null) {
//                /* 判断二级单位流程是否存在 */
//                ListCataLogDTO cataLogDTOTwo = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(org)).findFirst().orElse(null);
//                if (cataLogDTOTwo != null) {
//                    /* 赋值使用二级单位 */
//                    processKey = processKeyReplace.replace("{org}", org);
//                }
//                if (orgThree != null) {
//                    /* 判断三级单位流程是否存在 */
//                    String finalOrgThree = orgThree;
//                    ListCataLogDTO cataLogDTOThree = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(finalOrgThree)).findFirst().orElse(null);
//                    if (cataLogDTOThree != null) {
//                        /* 赋值使用三级单位 */
//                        processKey = processKeyReplace.replace("{org}", orgThree);
//                    }
//                }
//            }
//        }
//        requestDTO.setProcessKey(processKey);
//        BpmSubmitResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_SUBMIT,
//                BpmSubmitResponseDTO.class,requestDTO);
//        try{
//            /* 流程操作记录 */
//            bpmLogService.save(BpmLog.builder()
//                    .bpmType("提交接口")
//                    .businessId(requestDTO.getBusinessId())
//                    .wfProcessId(requestDTO.getProcessId())
//                    .bpmKey(requestDTO.getProcessKey())
//                    .bpmUrl(UnderlingPlatformUrlEnum.BPM_OPERATE_SUBMIT.getUrl())
//                    .bpmParam(JSONUtil.parse(requestDTO).toString())
//                    .bpmResponse(JSONUtil.parse(responseDTO).toString())
//                    .build());
//        }catch (Exception e){log.error("[  bpmLogService报错  ]{}",e.getMessage());}
//        return  responseDTO;
        return null;
    }

    /**
     * 加载定义接口
     *
     * @param requestDTO
     * @return
     */
    public List<BpmLoadTaskDefResponseDTO> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
//        requestDTO.setOrgPenetrate(true);
//        List<BpmLoadTaskDefResponseDTO> responseDTO = UnderlingRestTemplateService.postForList(UnderlingPlatformUrlEnum.BPM_OPERATE_LOADTASKDEF,
//                BpmLoadTaskDefResponseDTO.class,requestDTO);
//        try{
//            /* 流程操作记录 */
//            bpmLogService.save(BpmLog.builder()
//                    .bpmType("加载定义接口")
//                    .businessId(requestDTO.getBusinessId())
//                    .wfProcessId(requestDTO.getProcessId())
//                    .bpmKey(requestDTO.getProcessKey())
//                    .bpmUrl(UnderlingPlatformUrlEnum.BPM_OPERATE_LOADTASKDEF.getUrl())
//                    .bpmParam(JSONUtil.parse(requestDTO).toString())
//                    .bpmResponse(JSONUtil.parse(responseDTO).toString())
//                    .build());
//        }catch (Exception e){log.error("[  bpmLogService报错  ]{}",e.getMessage());}
//        return  responseDTO;
        return null;
    }

    /**
     * 发送无流程消息
     *
     * @param requestDTO
     * @return
     */
    public BpmSendMsgNoProcessResponseDTO sendMsgNoProcess(BpmSendMsgNoProcessRequestDTO requestDTO) {
//        BpmSendMsgNoProcessResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_SENDMSGNOPROCESS,
//                BpmSendMsgNoProcessResponseDTO.class,requestDTO);
//        return  responseDTO;
        return null;
    }

    /**
     * 尝试对流程进行加锁
     *
     * @param requestDTO
     * @return
     */
    public BpmTryLockResponseDTO tryLock(BpmTryLockRequestDTO requestDTO) {
//        BpmTryLockResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_TRYLOCK,
//                BpmTryLockResponseDTO.class,requestDTO);
//        return  responseDTO;
        return null;
    }

    /**
     * 解锁
     *
     * @param requestDTO
     * @return
     */
    public BpmUnLockResponseDTO unLock(BpmUnLockRequestDTO requestDTO) {
//        BpmUnLockResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_UNLOCK,
//                BpmUnLockResponseDTO.class,requestDTO);
//        return  responseDTO;
        return null;
    }

}
