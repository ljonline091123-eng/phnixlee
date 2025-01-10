package com.zhaocai.business.vendor.service.impl;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.*;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.vo.res.MinProjectVO;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorChange;
import com.zhaocai.business.vendor.mapper.VendorChangeMapper;
import com.zhaocai.business.vendor.service.IVendorChangeBlackService;
import com.zhaocai.business.vendor.service.IVendorChangeService;
import com.zhaocai.business.vendor.service.IVendorOperateLogService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorBlackRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorChangeRequestVO;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商变更黑名单Service业务层处理
 *
 * @author lsn
 * @date 2024-08-05
 */
@Slf4j
@Service
public class VendorChangeBlackServiceImpl extends ServiceImpl<VendorChangeMapper,VendorChange> implements IVendorChangeBlackService {

    @Autowired
    private IVendorService vendorService;

    @Autowired
    private IBPMProcessService processService;

    @Autowired
    private IVendorChangeService vendorChangeService;

    @Autowired
    private IVendorOperateLogService vendorOperateLogService;

    @Autowired
    private IAttachmentService attachmentService;


    @Autowired
    private UnderlingSystemService underlingSystemService;

    /**
     * 保存供应商黑名单信息
     * @param requestVO
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void saveVendorBlack(VendorBlackRequestVO requestVO) {
        Vendor vendor = vendorService.getById(requestVO.getId());
        ValidateUtils.isNullException(vendor,"该供应商不存在");

        if (requestVO.getBlackState() == 1) {
            // 移入黑名单
            addVendorToBlack(requestVO);
        } else {
            // 移除黑名单
            addVendorFromBlack(requestVO);
        }
    }

    /**
     * 移除黑名单
     * @param requestVO
     */
    private void addVendorFromBlack(VendorBlackRequestVO requestVO) {
        // 通过调用供应商变更表，获取最新版变更记录
        VendorChangeRequestVO result = vendorChangeService.getVendorUpdateDetail(requestVO.getId());
        VendorChange vendorChange = result.getVendorChange();
        super.update(new LambdaUpdateWrapper<VendorChange>()
                .set(VendorChange::getIsBlack,requestVO.getBlackState())
                .set(VendorChange::getBlackBeginDate,requestVO.getBlackBeginDate())
                .set(VendorChange::getBlackEndDate,requestVO.getBlackEndDate())
                .eq(VendorChange::getId,vendorChange.getId()));
        // 添加批注信息
        vendorChange.setOperateComment(requestVO.getOperateComment());
        // 添加操作记录
        Long logId = vendorOperateLogService.addVendorOperateLog(requestVO.getId(), VendorOperateLogCodeEnum.REMOVE_FROM_BLACK);
        // 添加附件
        attachmentService.addAttachment(requestVO.getAttachmentList(), AttachmentTypeEnum.REMOVE_VENDOR_BLACK, logId);
        // 提交流程
        this.handleSubmit(vendorChange);
    }

    /**
     * 提交流程
     * @param vendorChange
     */
    private void handleSubmit(VendorChange vendorChange) {
        //接入底层逻辑平台流程
        Vendor vendor = vendorService.getById(vendorChange.getVendorId());
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("businessId", vendorChange.getId());
        paramMap.put("businessTitle", "招标采购/供应商管理/供应商基本信息 供应商移入移出黑名单");
        paramMap.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.VENDOR_BLACKLIST_APPROVE.getDesc(), vendor.getEnterpriseName()));
        paramMap.put("detailUrl", "/vendor/vendor-detail/"+ Base64.encodeStr(("\""+vendorChange.getVendorId().toString()+"\"").getBytes(),true,true));
        String org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
        String customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_MOVE_INOROUT_BLACK.getIdentifying().replace("{org}",org);
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        /* 获取所有流程 */
        List<ListCataLogDTO> listCataLogDTOS = underlingSystemService.listCatalog();
        if (listCataLogDTOS != null) {
            /* 判断二级单位流程是否存在 */
            ListCataLogDTO cataLogDTOTwo = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(org)).findFirst().orElse(null);
            if (cataLogDTOTwo != null) {
                /* 赋值使用二级单位 */
                customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_MOVE_INOROUT_BLACK.getIdentifying().replace("{org}",org);
            }
            if (orgThree != null) {
                /* 判断三级单位流程是否存在 */
                String finalOrgThree = orgThree;
                ListCataLogDTO cataLogDTOThree = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(finalOrgThree)).findFirst().orElse(null);
                if (cataLogDTOThree != null) {
                    /* 赋值使用三级单位 */
                    customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_MOVE_INOROUT_BLACK.getIdentifying().replace("{org}",orgThree);
                }
            }
        }
        UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_VENDOR_MOVE_INOROUT_BLACK.name()).
                businessId(vendor.getId().toString())
                .toDoType(ToDoTypeEnum.EXAMINE.name()).build();
        paramMap.put("userObj", JSON.toJSONString(userObj));
        paramMap.put("customProcessKey", customProcessKey);
        paramMap.put("operateComment", vendorChange.getOperateComment());

        /* 获取三级单位 */
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        paramMap.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
        paramMap.put("companyId", org);/* 公司 二级单位 */
        paramMap.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        paramMap.put("parentProjectCode", org);/* 父项目编码(项目部) */

        processService.startProcessInstance(
                ProcessKeyEnum.ZHAOCAI_VENDOR_MOVE_INOROUT_BLACK.getIdentifying(),paramMap);
    }

    /**
     * 移入黑名单
     * @param requestVO
     */
    private void addVendorToBlack(VendorBlackRequestVO requestVO) {
        VendorChangeRequestVO result = vendorChangeService.getVendorUpdateDetail(requestVO.getId());
        VendorChange vendorChange = result.getVendorChange();
        super.update(new LambdaUpdateWrapper<VendorChange>()
                .set(VendorChange::getIsBlack,requestVO.getBlackState())
                // 移入黑名单，设置限制时间
                .set(VendorChange::getBlackBeginDate,requestVO.getBlackBeginDate())
                .set(VendorChange::getBlackEndDate,requestVO.getBlackEndDate())
                .eq(VendorChange::getId,vendorChange.getId()));

        // 添加批注信息
        vendorChange.setOperateComment(requestVO.getOperateComment());

        // 添加操作记录
        Long logId = vendorOperateLogService.addVendorOperateLog(requestVO.getId(), VendorOperateLogCodeEnum.ADD_TO_BLACK);
        // 添加附件
        attachmentService.addAttachment(requestVO.getAttachmentList(),AttachmentTypeEnum.ADD_VENDOR_BLACK,logId);
        // 提交流程
        this.handleSubmit(vendorChange);
    }

    /**
     * 发起流程回调
     * @param variables
     */
    @Override
    public void processStart(Map<String, Object> variables) {
        String processId = variables.get("processId").toString();
        String businessId = variables.get("businessId").toString();
        Object flagObj = variables.get("completedFlag");
        Integer vendorState =  VendorStateEnum.IN_APPROVAL.getState();
        if (!ObjectUtils.isEmpty(flagObj) && ProcessStateEnum.COMPLETED.getDesc().equals(flagObj.toString())) {
            vendorState = VendorStateEnum.APPROVE.getState();
        }
        super.update(new LambdaUpdateWrapper<VendorChange>()
                .set(VendorChange::getWfProcessId,processId)
                .set(VendorChange::getChangeStatus,vendorState)
                .eq(VendorChange::getId,businessId));
        VendorChange vendorChange = super.getById(businessId);
        if(vendorState.equals(VendorStateEnum.APPROVE.getState())){
            // 审批通过
            this.handleApprove(vendorChange);
        }else {
            // 审批中
            Vendor vendor = new Vendor();
            vendor.setState(VendorStateEnum.IN_APPROVAL.getState());
            vendor.setId(vendorChange.getVendorId());
            vendor.setProcessType(VendorProcessTypeEnum.VENDOR_MOVE_INOROUT_BLACK.getState());
            vendorService.updateById(vendor);
        }
    }

    /**
     * 审批通过后将黑名单信息更新
     * @param vendorChange
     */
    private void handleApprove(VendorChange vendorChange) {
        // 更新企业信息
        vendorService.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getIsBlack,vendorChange.getIsBlack())
                .set(Vendor::getBlackBeginDate,vendorChange.getBlackBeginDate())
                .set(Vendor::getBlackEndDate,vendorChange.getBlackEndDate())
                .set(Vendor::getState,VendorStateEnum.APPROVE.getState())
                .set(Vendor::getWfProcessId,vendorChange.getWfProcessId())
                .eq(Vendor::getId,vendorChange.getVendorId()));
        //推送中台接口
        vendorService.pushVendor(vendorChange.getVendorId(),Vendor.LOG_TYPE_MODIFY,vendorChange.getIsBlack());
    }

    /**
     * 审批通过
     * @param variables
     */
    @Override
    public void processAuditPass(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<VendorChange>()
                .set(VendorChange::getChangeStatus,VendorStateEnum.APPROVE.getState())
                .eq(VendorChange::getId, businessId));
        // 审批通过
        VendorChange vendorChange = super.getById(businessId);
        this.handleApprove(vendorChange);
    }

    /**
     * 审批驳回
     * @param variables
     */
    @Override
    public void processAuditReject(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        // 修改变更状态为驳回
        super.update(new LambdaUpdateWrapper<VendorChange>()
                .set(VendorChange::getChangeStatus,VendorStateEnum.REJECT.getState())
                .eq(VendorChange::getId, businessId));
        // 审批驳回将供应商状态改回审批通过
        VendorChange vendorChange = super.getById(businessId);
        vendorService.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getState,VendorStateEnum.APPROVE.getState())
                .eq(Vendor::getId, vendorChange.getVendorId()));
    }

    /**
     * 审批驳回到提交人状态
     * @param variables
     */
    @Override
    public void processAuditFreedom(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        // 修改变更状态为驳回
        super.update(new LambdaUpdateWrapper<VendorChange>()
                .set(VendorChange::getChangeStatus,VendorStateEnum.REJECT.getState())
                .eq(VendorChange::getId, businessId));
        // 审批驳回将供应商状态改回审批通过
        VendorChange vendorChange = super.getById(businessId);
        vendorService.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getState,VendorStateEnum.APPROVE.getState())
                .eq(Vendor::getId, vendorChange.getVendorId()));
    }

    @Override
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        VendorChange vendorChange = vendorChangeService.getById(requestDTO.getBusinessId());
        String org = underlingSystemService.getL2OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();
        PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);/* 1000000000 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", org);/* 公司 二级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", org);/* 父项目编码(项目部) */
        requestDTO.setPropertyList(propertyList);
        return processService.initialize(requestDTO);
    }

    @Override
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        VendorChange vendorChange = vendorChangeService.getById(requestDTO.getBusinessId());
        String org = underlingSystemService.getL2OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();
        PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);/* 1000000000 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", org);/* 公司 二级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", org);/* 父项目编码(项目部) */
        requestDTO.setPropertyList(propertyList);
        return processService.listProcessLog(requestDTO);
    }

    @Override
    public String audit(String processKey, Map<String, Object> variables) {
        VendorChange vendorChange = vendorChangeService.getById((Serializable) variables.get("businessId"));
        String org = underlingSystemService.getL2OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        variables.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
        variables.put("companyId", org);/* 公司 二级单位 */
        variables.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        variables.put("parentProjectCode", org);/* 父项目编码(项目部) */
        return processService.auditProcessInstance(ProcessKeyEnum.ZHAOCAI_VENDOR_MOVE_INOROUT_BLACK.getIdentifying(),variables);
    }

    @Override
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        VendorChange vendorChange = vendorChangeService.getById(requestDTO.getBusinessId());
        String org = underlingSystemService.getL2OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();
        PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);/* 1000000000 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", org);/* 公司 二级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", org);/* 父项目编码(项目部) */
        requestDTO.setPropertyList(propertyList);
        return processService.loadTaskDef(requestDTO);
    }
}
