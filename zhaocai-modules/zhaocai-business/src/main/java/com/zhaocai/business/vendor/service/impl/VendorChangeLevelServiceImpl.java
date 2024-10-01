package com.zhaocai.business.vendor.service.impl;

import cn.hutool.core.codec.Base64;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorChange;
import com.zhaocai.business.vendor.mapper.VendorChangeMapper;
import com.zhaocai.business.vendor.service.IVendorChangeLevelService;
import com.zhaocai.business.vendor.service.IVendorChangeService;
import com.zhaocai.business.vendor.service.IVendorOperateLogService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorChangeRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorLevelRequestVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 供应商变更-修改等级Service业务层处理
 *
 * @author lsn
 * @date 2024-08-15
 */
@Slf4j
@Service
public class VendorChangeLevelServiceImpl extends ServiceImpl<VendorChangeMapper,VendorChange> implements IVendorChangeLevelService {

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
     * 保存供应商等级信息
     * @param requestVO
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void saveVendorLevel(VendorLevelRequestVO requestVO) {
        Vendor vendor = vendorService.getById(requestVO.getId());
        ValidateUtils.isNullException(vendor,"该供应商不存在");

        // 通过调用供应商变更表，获取最新版变更记录
        VendorChangeRequestVO result = vendorChangeService.getVendorUpdateDetail(requestVO.getId());
        VendorChange vendorChange = result.getVendorChange();
        super.update(new LambdaUpdateWrapper<VendorChange>()
                .set(VendorChange::getVendorClass,requestVO.getVendorClass())
                .set(VendorChange::getVendorLevel,requestVO.getVendorLevel())
                .eq(VendorChange::getId,vendorChange.getId()));

        // 添加批注信息
        vendorChange.setOperateComment(requestVO.getOperateComment());

        // 添加操作记录
        Long logId = vendorOperateLogService.addVendorOperateLog(requestVO.getId(), VendorOperateLogCodeEnum.UPDATE_VENDOR_LEVEL);
        // 添加附件
        attachmentService.addAttachment(requestVO.getAttachmentList(), AttachmentTypeEnum.UPDATE_VENDOR_LEVEL,logId);

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
        paramMap.put("businessTitle", "供应商修改等级");
        paramMap.put("businessContent", String.format("您有供应商：%s修改等级在审核节点的审批！", vendor.getEnterpriseName()));
        paramMap.put("detailUrl", "/vendor/vendor-detail/"+ Base64.encodeStr(("\""+vendorChange.getVendorId().toString()+"\"").getBytes(),true,true));
        String org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
        String customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATE_LEVEL.getIdentifying().replace("{org}",org);
        paramMap.put("customProcessKey", customProcessKey);
        paramMap.put("operateComment", vendorChange.getOperateComment());
        processService.startProcessInstance(
                ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATE_LEVEL.getIdentifying(),paramMap);
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
            vendor.setProcessType(VendorProcessTypeEnum.VENDOR_UPDATE_LEVEL.getState());
            vendorService.updateById(vendor);
        }
    }

    /**
     * 审批通过后将等级信息更新
     * @param vendorChange
     */
    private void handleApprove(VendorChange vendorChange) {
        // 更新企业信息
        vendorService.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getVendorClass,vendorChange.getVendorClass())
                .set(Vendor::getVendorLevel,vendorChange.getVendorLevel())
                .set(Vendor::getState,VendorStateEnum.APPROVE.getState())
                .set(Vendor::getWfProcessId,vendorChange.getWfProcessId())
                .eq(Vendor::getId,vendorChange.getVendorId()));
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
}
