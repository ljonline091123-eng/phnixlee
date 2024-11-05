package com.zhaocai.business.manager.http.service;

import cn.hutool.core.util.StrUtil;
import com.zhaocai.business.common.enums.ProcessKeyEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.*;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.procurement.vo.res.MinProjectVO;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * 流程服务，具体业务都是调用这里的接口，也可以直接被前端直接调用
 */
@Slf4j
@Service
public class BpmService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private  UnderlingSystemService underlingSystemService;


    @Autowired
    private IMinProjectService minProjectService;

    /**
     * 初始化接口
     * @param requestDTO
     * @return
     */
    public  BpmInitializeResponseDTO initialize(BpmInitializeRequestDTO requestDTO) {
        BpmInitializeResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_INITIALIZE,
                BpmInitializeResponseDTO.class,requestDTO);
        return  responseDTO;

    }

    /**
     * 流程操作日志列表接口
     * @param requestDTO
     * @return
     */
    public List<BpmListProcessLogResponseDTO> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        List<BpmListProcessLogResponseDTO> responseDTO = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_LISTPROCESSLOG,
                BpmListProcessLogResponseDTO.class,requestDTO);
        return  responseDTO;
    }

    /**
     * 弃审接口
     * @param requestDTO
     * @return
     */
    public BpmDisCardResponseDTO disCard(BpmDisCardRequestDTO requestDTO) {
        BpmDisCardResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_DISCARD,
                BpmDisCardResponseDTO.class,requestDTO);
        return  responseDTO;
    }

    /**
     * 撤销
     * @param requestDTO
     * @return
     */
    public BpmRevokeResponseDTO revoke(BpmRevokeRequestDTO requestDTO) {
        BpmRevokeResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_REVOKE,
                BpmRevokeResponseDTO.class,requestDTO);
        return  responseDTO;
    }

    /**
     * 作废
     * @param requestDTO
     * @return
     */
    public BpmDeleteResponseDTO delete(BpmDeleteRequestDTO requestDTO) {
        BpmDeleteResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_DELETE,
                BpmDeleteResponseDTO.class,requestDTO);
        return  responseDTO;
    }


    /**
     * 审批接口
     * @param requestDTO
     * @return
     */
    public BpmAuditResponseDTO audit(BpmAuditRequestDTO requestDTO) {
        BpmAuditResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_AUDIT,
                BpmAuditResponseDTO.class,requestDTO);
        return  responseDTO;
    }

    /**
     * 提交接口
     * @param requestDTO
     * @return
     */
    public BpmSubmitResponseDTO submit(BpmSubmitRequestDTO requestDTO) {

//        String projectCode = "SG20012024000002-2";
//        if (StrUtil.isNotBlank(projectCode)) {
//            MinProjectVO minProjectVO = minProjectService.getMinProjectByMinAccountCode(projectCode);
//                if (null != minProjectVO) {
//                    List<PropertyListRequestDTO> propertyList = new ArrayList<>();
//                    PropertyListRequestDTO propertyListRequestDTO =
//                            new PropertyListRequestDTO("parentProjectCode", minProjectVO.getParentCode());
//                    propertyList.add(propertyListRequestDTO);
//                    PropertyListRequestDTO propertyListRequestDTO1 =
//                            new PropertyListRequestDTO("responsibilityDeptId", minProjectVO.getDutyUnit());
//                    propertyList.add(propertyListRequestDTO1);
//                    PropertyListRequestDTO propertyListRequestDTO2 =
//                            new PropertyListRequestDTO("companyId", "2013000000");
//                    propertyList.add(propertyListRequestDTO2);
//                    PropertyListRequestDTO propertyListRequestDTO3 =
//                            new PropertyListRequestDTO("groupId", "1000000000");
//                    propertyList.add(propertyListRequestDTO3);
//
//
//                requestDTO.setPropertyList(propertyList);
//            }
//        }

        String processKey = requestDTO.getProcessKey();
        if (processKey.contains("{org}")) {
            /* 获取二级单位 */
            String org = underlingSystemService.getL2OrgByOrgId(SecurityUtils.getThridOrgId());
            /* 获取三级单位 */
            String orgThree = underlingSystemService.getL3OrgByOrgId(SecurityUtils.getThridOrgId());
            processKey = processKey.replace("{org}", org);
            /* 获取所有流程 */
            List<ListCataLogDTO> listCataLogDTOS = underlingSystemService.listCatalog();
            if (listCataLogDTOS != null) {
                /* 判断二级单位流程是否存在 */
                ListCataLogDTO cataLogDTOTwo = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(org)).findFirst().orElse(null);
                if (cataLogDTOTwo != null) {
                    /* 赋值使用二级单位 */
                    processKey = processKey.replace("{org}", org);
                }
                if (orgThree != null) {
                    /* 判断三级单位流程是否存在 */
                    String finalOrgThree = orgThree;
                    ListCataLogDTO cataLogDTOThree = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(finalOrgThree)).findFirst().orElse(null);
                    if (cataLogDTOThree != null) {
                        /* 赋值使用三级单位 */
                        processKey = processKey.replace("{org}", orgThree);
                    }
                }
            }
        }
        requestDTO.setProcessKey(processKey);
        BpmSubmitResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_SUBMIT,
                BpmSubmitResponseDTO.class,requestDTO);
        return  responseDTO;
    }

    /**
     * 加载定义接口
     * @param requestDTO
     * @return
     */
    public List<BpmLoadTaskDefResponseDTO> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        List<BpmLoadTaskDefResponseDTO> responseDTO = UnderlingRestTemplateService.postForList(UnderlingPlatformUrlEnum.BPM_OPERATE_LOADTASKDEF,
                BpmLoadTaskDefResponseDTO.class,requestDTO);
        return  responseDTO;
    }

    /**
     *  发送无流程消息
     * @param requestDTO
     * @return
     */
    public BpmSendMsgNoProcessResponseDTO sendMsgNoProcess(BpmSendMsgNoProcessRequestDTO requestDTO) {
        BpmSendMsgNoProcessResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_SENDMSGNOPROCESS,
                BpmSendMsgNoProcessResponseDTO.class,requestDTO);
        return  responseDTO;
    }

    /**
     *  尝试对流程进行加锁
     * @param requestDTO
     * @return
     */
    public BpmTryLockResponseDTO tryLock(BpmTryLockRequestDTO requestDTO) {
        BpmTryLockResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_TRYLOCK,
                BpmTryLockResponseDTO.class,requestDTO);
        return  responseDTO;
    }

    /**
     * 解锁
     * @param requestDTO
     * @return
     */
    public BpmUnLockResponseDTO unLock(BpmUnLockRequestDTO requestDTO) {
        BpmUnLockResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.BPM_OPERATE_UNLOCK,
                BpmUnLockResponseDTO.class,requestDTO);
        return  responseDTO;
    }

}
