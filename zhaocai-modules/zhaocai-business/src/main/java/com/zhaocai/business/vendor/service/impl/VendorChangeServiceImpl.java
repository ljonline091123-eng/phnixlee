package com.zhaocai.business.vendor.service.impl;

import cn.hutool.core.codec.Base64;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.vendor.domain.*;
import com.zhaocai.business.vendor.mapper.VendorChangeMapper;
import com.zhaocai.business.vendor.service.*;
import com.zhaocai.business.vendor.vo.req.VendorBlackRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorChangeRequestVO;
import com.zhaocai.business.vendor.vo.res.*;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import com.zhaocai.common.core.web.domain.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 供应商变更Service业务层处理
 *
 * @author lsn
 * @date 2024-08-05
 */
@Slf4j
@Service
public class VendorChangeServiceImpl extends ServiceImpl<VendorChangeMapper,VendorChange> implements IVendorChangeService {

    @Autowired
    private IVendorService vendorService;

    @Autowired
    private IBPMProcessService processService;

    @Autowired
    private IVendorCertificationChangeService certificationChangeService;

    @Autowired
    private IVendorContactChangeService contactChangeService;

    @Autowired
    private IVendorContactService contactService;

    @Autowired
    private IVendorClassifyService vendorClassifyService;

    @Autowired
    private IVendorOperateLogService vendorOperateLogService;


    @Autowired
    private IAttachmentService attachmentService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

    /**
     * 获取供应商修改详情
     * @param vendorId
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public VendorChangeRequestVO getVendorUpdateDetail(Long vendorId) {
        VendorChangeRequestVO vendorChangeRequestVO = new VendorChangeRequestVO();
        // 供应商变更信息
        VendorChange vendorChange = new VendorChange();
        // 供应商资质变更信息
        List<VendorCertificationChange> certificationList;
        // 供应商联系人变更信息
        List<VendorContactChange> contactChangeList = new ArrayList<>();
        VendorContactChange  contactChange  = new VendorContactChange();
        // 根据供应商id在供应商变更表中查找最后一次变更信息
        List<VendorChange> vendorChangeList = super.list(new LambdaQueryWrapper<VendorChange>()
                .eq(VendorChange::getVendorId, vendorId)
                .orderByDesc(VendorChange::getVersion));
        Vendor vendor = vendorService.getById(vendorId);
        // 不存在变更版本时，创建一份VO版本
        if (CollectionUtils.isEmpty(vendorChangeList)) {
            //如果并未审批通过，不生成新版本
            if(vendor.getState()==VendorStateEnum.REJECT.getState()||vendor.getState()==VendorStateEnum.IN_APPROVAL.getState()){
                BeanUtils.copyProperties(vendor, vendorChange);
//                List<VendorContact>  contactList= vendorContactService.list(new LambdaQueryWrapper<VendorContact>()
//                        .eq(VendorContact::getVendorId, vendorId));
//                if(!CollectionUtils.isEmpty(contactList)){
//                    BeanUtils.copyProperties(contactList.get(0), contactChange);
//                    contactChangeList.add(contactChange);
//                }
//                certificationList  = certificationChangeService.getCertificationChange(vendorId,0);
                vendorChangeRequestVO.setVendorChange(vendorChange);
                return vendorChangeRequestVO;
            }else{
                vendorChange = this.createVendorChange(vendorId);
            certificationList = certificationChangeService.createCertificationChange(vendorId,0);
            contactChangeList = contactChangeService.createContactChange(vendorId, 0);
            // 创建最新版本副本
            vendorChangeRequestVO = this.createCopy(vendorChange);
            }

        } else {
            // 存在变更版本，判断最新版本的变更是保存状态还是审批通过状态
            vendorChange = vendorChangeList.get(0);
            certificationList = certificationChangeService.list(new LambdaQueryWrapper<VendorCertificationChange>()
                    .eq(VendorCertificationChange::getVendorId, vendorId)
                    .eq(VendorCertificationChange::getVersion, vendorChange.getVersion())
                    .eq(BaseEntity::getDelFlag, 0));
            contactChangeList = contactChangeService.list(new LambdaQueryWrapper<VendorContactChange>()
                    .eq(VendorContactChange::getVendorId, vendorId)
                    .eq(VendorContactChange::getVersion, vendorChange.getVersion()));
            vendorChangeRequestVO.setVendorChange(vendorChange);
            vendorChangeRequestVO.setCertificationChangeList(certificationList);
            vendorChangeRequestVO.setContactChangeList(contactChangeList);
            // 如果最新版本审批通过，则返回最新版本副本
            if (null != vendorChange.getChangeStatus() && vendorChange.getChangeStatus().equals(VendorStateEnum.APPROVE.getState())) {
                // 获取最新版本副本
                vendorChangeRequestVO = this.createCopy(vendorChange);
            }
        }
        // 处理返回的内容
        return this.handleReturnInfo(vendorChangeRequestVO);
    }

    /**
     * 处理返回的内容
     */
    private VendorChangeRequestVO handleReturnInfo(VendorChangeRequestVO requestVO) {
        // 企业资质分类
        List<VendorCertificationChange> certificationChangeList = requestVO.getCertificationChangeList();
        for(VendorCertificationChange certification : certificationChangeList){
            if(CertificationTypeEnum.BUSINESS_LICENSE.equalsType(certification.getBusinessCode()))  {
                requestVO.setBusinessLicense(certification);
            }  else if (CertificationTypeEnum.INTEGRITY.equalsType(certification.getBusinessCode())) {
                requestVO.setIntegrity(certification);
            }else if (CertificationTypeEnum.LEGAL_AUTHORIZATION.equalsType(certification.getBusinessCode())) {
                List<VendorCertificationChange> list = requestVO.getLegalAuthorizationList();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(certification);
                requestVO.setLegalAuthorizationList(list);
            }else if (CertificationTypeEnum.RELEVANT_CERTIFICATION.equalsType(certification.getBusinessCode())) {
                List<VendorCertificationChange> list = requestVO.getRelevantCertificationList();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(certification);
                requestVO.setRelevantCertificationList(list);
            }
        }
        // 企业联系人分类
        List<VendorContactChange> contactChangeList = requestVO.getContactChangeList();
        if(!CollectionUtils.isEmpty(contactChangeList)){
            VendorContactChange mainContact = contactChangeList.stream()
                    .filter(x -> x.getIsMainContact().equals(1))
                    .map(mc -> BeanCopierUtil.copyBean(mc, VendorContactChange.class))
                    .findFirst()
                    .orElseThrow(()->new ParamValidateException("该供应商没有设置主要联系人"));
            List<VendorContactChange> contactVOList = contactChangeList.stream()
                    .filter(x -> x.getIsMainContact().equals(0))
                    .map(mc -> BeanCopierUtil.copyBean(mc, VendorContactChange.class))
                    .collect(Collectors.toList());
            requestVO.setMainContactChange(mainContact);
            requestVO.setContactChangeList(contactVOList);
        }
        return requestVO;
    }

    /**
     * 保存供应商信息
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void saveVendorChange(VendorChangeRequestVO requestVO) {
        VendorChange vendorChange = requestVO.getVendorChange();
        Vendor vendor = vendorService.getById(vendorChange.getVendorId());
        if (vendor.getState().equals(VendorStateEnum.IN_APPROVAL.getState())) {
            throw new ParamValidateException("该供应商处于审批中，请勿修改信息");
        }
        // 供应商信息校验
        this.checkVendorInfo(vendorChange);
        // 修改供应商变更信息
        super.updateById(vendorChange);
        // 修改供应商资质变更信息(已实时修改)

        // 修改供应商联系人
        VendorContactChange mainContact = requestVO.getMainContactChange();
        if(null != mainContact){
            VendorContact checkContact = contactService.getOne(new LambdaQueryWrapper<VendorContact>()
                    .eq(VendorContact::getContactPhone,mainContact.getContactPhone())
                    .ne(VendorContact::getId, mainContact.getContactId()));
            ValidateUtils.isNotNullException(checkContact,"该联系人手机号码已存在，请勿重新添加");
            contactChangeService.update(new LambdaUpdateWrapper<VendorContactChange>()
                    .set(VendorContactChange::getContactName, mainContact.getContactName())
                    .set(VendorContactChange::getContactIdCard, mainContact.getContactIdCard())
                    .set(VendorContactChange::getContactPhone, mainContact.getContactPhone())
                    .set(VendorContactChange::getContactEmail, mainContact.getContactEmail())
                    .set(VendorContactChange::getIsLegal, mainContact.getIsLegal())
                    .eq(VendorContactChange::getId, mainContact.getId()));
        }
        List<VendorContactChange> contactChangeList = requestVO.getContactChangeList();
        if(!CollectionUtils.isEmpty(contactChangeList)){
            for (VendorContactChange vendorContactChange : contactChangeList) {
                VendorContact checkContact = contactService.getOne(new LambdaQueryWrapper<VendorContact>()
                        .eq(VendorContact::getContactPhone,vendorContactChange.getContactPhone())
                        .ne(VendorContact::getId, vendorContactChange.getContactId()));
                ValidateUtils.isNotNullException(checkContact,"该联系人手机号码已存在，请勿重新添加");
                contactChangeService.update(new LambdaUpdateWrapper<VendorContactChange>()
                        .set(VendorContactChange::getContactName, vendorContactChange.getContactName())
                        .set(VendorContactChange::getContactIdCard, vendorContactChange.getContactIdCard())
                        .set(VendorContactChange::getContactPhone, vendorContactChange.getContactPhone())
                        .set(VendorContactChange::getContactEmail, vendorContactChange.getContactEmail())
                        .set(VendorContactChange::getIsLegal, vendorContactChange.getIsLegal())
                        .eq(VendorContactChange::getId, vendorContactChange.getId()));
            }
        }
    }

    /**
     * 提交供应商信息
     * @param requestVO
     */
    @Override
    public void submitVendorChance(VendorChangeRequestVO requestVO) {
        // 保存供应商信息
        this.saveVendorChange(requestVO);

        VendorChange vendorChange = requestVO.getVendorChange();
        Vendor vendor = vendorService.getById(vendorChange.getVendorId());
        // 提交前判断是否需要提交流程
        boolean flag = this.checkIsSubmit(requestVO);
        if(flag){
            //接入底层逻辑平台流程
            Map<String,Object> paramMap = new HashMap<>();
            paramMap.put("businessId", vendorChange.getId());
            paramMap.put("businessTitle", "供应商-修改信息审批");
            paramMap.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.VENDOR_CHANGE_APPROVE.getDesc(), vendor.getEnterpriseName()));
            paramMap.put("detailUrl", "/vendor/vendor-detail/"+ Base64.encodeStr(("\""+vendor.getId().toString()+"\"").getBytes(),true,true));
            String org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
            //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
            String customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATEINFO.getIdentifying().replace("{org}",org);
            paramMap.put("customProcessKey", customProcessKey);
            paramMap.put("operateComment", vendorChange.getOperateComment());
            processService.startProcessInstance(
                    ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATEINFO.getIdentifying(),paramMap);
        }else{
            // 不需要提交，直接修改
            this.handleApprove(requestVO.getVendorChange());
        }
    }

    /**
     * 提交前判断是否需要提交流程
     * @param requestVO
     * @return
     */
    private boolean checkIsSubmit(VendorChangeRequestVO requestVO) {
        Boolean flag = false;
        VendorChange vendorChange = requestVO.getVendorChange();
        List<VendorChange> changeList = super.list(new LambdaQueryWrapper<VendorChange>()
                .eq(VendorChange::getVendorId, vendorChange.getVendorId())
                .orderByDesc(VendorChange::getVersion));
        if(!CollectionUtils.isEmpty(changeList) && changeList.size() > 1){
            VendorChange checkVendor = changeList.get(1);
            VendorChange updateVendor = changeList.get(0);
            VendorContactChange checkContact= contactChangeService.getOne(new LambdaQueryWrapper<VendorContactChange>()
                    .eq(VendorContactChange::getVendorId, checkVendor.getVendorId())
                    .eq(VendorContactChange::getVersion,checkVendor.getVersion())
                    .eq(VendorContactChange::getIsMainContact,1));
            VendorContactChange updateContact= contactChangeService.getOne(new LambdaQueryWrapper<VendorContactChange>()
                    .eq(VendorContactChange::getVendorId, checkVendor.getVendorId())
                    .eq(VendorContactChange::getVersion,updateVendor.getVersion())
                    .eq(VendorContactChange::getIsMainContact,1));
            if (!checkVendor.getEnterpriseName().equals(updateVendor.getEnterpriseName())
                    || !checkVendor.getSocialCreditCode().equals(updateVendor.getSocialCreditCode())
                    || !checkVendor.getLegalRepresentative().equals(updateVendor.getLegalRepresentative())
                    || !checkContact.getContactPhone().equals(updateContact.getContactPhone())) {
                flag = true;
            } else {
                // 判断资质是否需要提交流程
                flag = certificationChangeService.checkCertificationSubmit(updateVendor);
            }
        }
        return flag;
    }

    /**
     * 获取最新版本号
     * @param id
     * @return
     */
    @Override
    public Integer getLastVersion(Long id) {
        List<VendorChange> changeList = super.list(new LambdaQueryWrapper<VendorChange>()
                .eq(VendorChange::getVendorId, id)
                .orderByDesc(VendorChange::getVersion));
        return changeList.get(0).getVersion();
    }

    /**
     * 获取变更详情
     * @param id 供应商id
     * @return
     */
    @Override
    public VendorManagementDetailVO getVendorManagementDetail(Long id) {
        Vendor vendor = vendorService.getById(id);
        Integer version = this.getLastVersion(id);
        List<VendorChange> vendorChangeList = super.list(new LambdaQueryWrapper<VendorChange>()
                .eq(VendorChange::getVendorId, id)
                .eq(VendorChange::getVersion, version));
        if(!CollectionUtils.isEmpty(vendorChangeList)){
            VendorChange vendorChange = vendorChangeList.get(0);
            VendorVO vendorVO = BeanCopierUtil.copyBean(vendorChange,VendorVO.class);
            vendorVO.setProcessType(vendor.getProcessType());
            vendorVO.setEnterpriseTypeText(vendorClassifyService.getVendorClassifyName(vendorVO.getEnterpriseType()));
            vendorVO.setState(vendor.getState());
            VendorContactChange contactChange = contactChangeService.getOne(new LambdaQueryWrapper<VendorContactChange>()
                    .eq(VendorContactChange::getVendorId, id)
                    .eq(VendorContactChange::getIsMainContact,1)
                    .eq(VendorContactChange::getVersion, version));
            VendorMainContactVO mainContactVO = BeanCopierUtil.copyBean(contactChange,VendorMainContactVO.class);

            VendorCertificationListVO certificationList = certificationChangeService.listCertification(id, version);

            VendorStateVO vendorState = BeanCopierUtil.copyBean(vendorChange,VendorStateVO.class);

            VendorBlackRequestVO vendorBlack = this.getBlackDetail(vendorChange);
            return VendorManagementDetailVO.builder()
                    .vendor(vendorVO)
                    .mainContact(mainContactVO)
                    .certificationList(certificationList)
                    .vendorState(vendorState)
                    .vendorBlack(vendorBlack)
                    .build();
        } else {
            return null;
        }
    }

    @Override
    public Long getLastChangeId(Long id) {
        // 根据供应商id在供应商变更表中查找最后一次变更信息
        List<VendorChange> vendorChangeList = super.list(new LambdaQueryWrapper<VendorChange>()
                .eq(VendorChange::getVendorId, id)
                .eq(VendorChange::getChangeStatus, VendorStateEnum.APPROVE.getState())
                .orderByDesc(VendorChange::getVersion));
        // 不存在变更版本时，创建一份VO版本
        if (!CollectionUtils.isEmpty(vendorChangeList)) {
            return vendorChangeList.get(0).getId();
        }
        return null;
    }

    /**
     * 获取黑名单那详情
     * @param vendorChange
     * @return
     */
    private VendorBlackRequestVO getBlackDetail(VendorChange vendorChange) {
        VendorBlackRequestVO vendorBlack = BeanCopierUtil.copyBean(vendorChange,VendorBlackRequestVO.class);
        vendorBlack.setId(vendorChange.getVendorId());
        List<Attachment> attachment = new ArrayList<>();
        List<VendorOperateLog> logVO;
        if (vendorChange.getIsBlack() == 1) {
            logVO = vendorOperateLogService.list(new LambdaQueryWrapper<VendorOperateLog>()
                    .eq(VendorOperateLog::getVendorId, vendorChange.getVendorId())
                    .eq(VendorOperateLog::getBusinessCode, VendorOperateLogCodeEnum.ADD_TO_BLACK)
                    .orderByDesc(BaseEntity::getCreateTime));
            if (!ObjectUtils.isEmpty(logVO)) {
                attachment = attachmentService.list(new LambdaQueryWrapper<Attachment>()
                        .eq(Attachment::getBusinessId, logVO.get(0).getId())
                        .eq(Attachment::getBusinessType, VendorOperateLogCodeEnum.ADD_TO_BLACK));
                vendorBlack.setAttachmentList(BeanCopierUtil.copyList(attachment, AttachmentRequestVO.class));
            }
        } else {
            logVO = vendorOperateLogService.list(new LambdaQueryWrapper<VendorOperateLog>()
                    .eq(VendorOperateLog::getVendorId, vendorChange.getVendorId())
                    .eq(VendorOperateLog::getBusinessCode, VendorOperateLogCodeEnum.REMOVE_FROM_BLACK)
                    .orderByDesc(BaseEntity::getCreateTime));
            if (!ObjectUtils.isEmpty(logVO)) {
                attachment = attachmentService.list(new LambdaQueryWrapper<Attachment>()
                        .eq(Attachment::getBusinessId, logVO.get(0).getId()));
                vendorBlack.setAttachmentList(BeanCopierUtil.copyList(attachment, AttachmentRequestVO.class));
            }
        }
        return vendorBlack;
    }

    /**
     * 供应商信息校验
     * @param vendorChange
     */
    private void checkVendorInfo(VendorChange vendorChange) {
        Vendor checkVendor = vendorService.getOne(new LambdaQueryWrapper<Vendor>()
                .eq(Vendor::getSocialCreditCode,vendorChange.getSocialCreditCode()));
        if (checkVendor != null && !checkVendor.getId().equals(vendorChange.getVendorId())) {
            throw new ParamValidateException("该统一社会信用代码已存在");
        }
        checkVendor = vendorService.getOne(new LambdaQueryWrapper<Vendor>()
                .eq(Vendor::getEnterpriseName,vendorChange.getEnterpriseName()));
        if (checkVendor != null && !checkVendor.getId().equals(vendorChange.getVendorId())) {
            throw new ParamValidateException("该企业名称已存在");
        }
    }

    /**
     * 创建最新版本副本
     */
    private VendorChangeRequestVO createCopy(VendorChange vendorChange) {
        // 供应商信息
        vendorChange.setId(null);
        vendorChange.setVersion(vendorChange.getVersion()+1);
        vendorChange.setChangeStatus(0);
        super.save(vendorChange);
        // 供应商资质
        List<VendorCertificationChange> certificationList = certificationChangeService.createCertificationChange(vendorChange.getVendorId(), vendorChange.getVersion());
        // 供应商联系人
        List<VendorContactChange> contactChangeList = contactChangeService.createContactChange(vendorChange.getVendorId(), vendorChange.getVersion());
        VendorChangeRequestVO vo  = new VendorChangeRequestVO();
        vo.setVendorChange(vendorChange);
        vo.setCertificationChangeList(certificationList);
        vo.setContactChangeList(contactChangeList);
        return vo;
    }

    /**
     * 不存在变更版本时，创建一份V0版本供应商信息
     * @param vendorId
     * @return
     */
    private VendorChange createVendorChange(Long vendorId) {
        Vendor vendor = vendorService.getById(vendorId);
        ValidateUtils.isNullException(vendor,"该供应商信息不存在");
        VendorChange vendorChange = new VendorChange();
        BeanUtils.copyProperties(vendor, vendorChange);
        vendorChange.setVendorId(vendorId);
        vendorChange.setId(null);
        vendorChange.setVersion(0);
        vendorChange.setChangeStatus(VendorStateEnum.APPROVE.getState());
        super.save(vendorChange);
        return vendorChange;
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
            vendor.setProcessType(VendorProcessTypeEnum.VENDOR_UPDATEINFO.getState());
            vendorService.updateById(vendor);
        }
    }

    /**
     * 处理审批通过后信息更新
     * @param vendorChange
     */
    private void handleApprove(VendorChange vendorChange) {
        Vendor checkVendor = vendorService.getById(vendorChange.getVendorId());
        // 更新企业信息
        Vendor vendor = new Vendor();
        BeanUtils.copyProperties(vendorChange, vendor);
        vendor.setId(vendorChange.getVendorId());
        vendor.setState(VendorStateEnum.APPROVE.getState());
        vendor.setProcessType(checkVendor.getProcessType());
        vendor.setCreateTime(checkVendor.getCreateTime());
        vendor.setVendorClass(checkVendor.getVendorClass());
        vendor.setVendorLevel(checkVendor.getVendorLevel());
        vendor.setIsBlack(checkVendor.getIsBlack());
        vendor.setBlackBeginDate(checkVendor.getBlackBeginDate());
        vendor.setBlackEndDate(checkVendor.getBlackEndDate());
        vendorService.updateById(vendor);
        // 更新供应商资质信息
        certificationChangeService.handleApprove(vendorChange);
        // 更新供应商联系人
        contactChangeService.handleApprove(vendorChange);
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
