package com.zhaocai.business.vendor.service.impl;

import cn.hutool.core.codec.Base64;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.*;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.domain.DwCdBank;
import com.zhaocai.business.pub.domain.TAccountInfo;
import com.zhaocai.business.pub.service.IAccountService;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.IBankService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.vendor.config.DataMiddlePlatformConfig;
import com.zhaocai.business.vendor.domain.*;
import com.zhaocai.business.vendor.mapper.VendorChangeMapper;
import com.zhaocai.business.vendor.service.*;
import com.zhaocai.business.vendor.util.DataCenterUtil;
import com.zhaocai.business.vendor.vo.req.VendorBlackRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorChangeRequestVO;
import com.zhaocai.business.vendor.vo.res.*;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.domain.BaseEntity;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private IBankService bankService;

    @Autowired
    private UnderlingSystemService underlingSystemService;
    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private DataCenterUtil dataCenterUtil;

    @Autowired
    private DataMiddlePlatformConfig dataMiddlePlatformConfig;

    @Autowired
    private IAccountService accountService;

    /**
     * 获取供应商修改详情
     * @param vendorId
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public VendorChangeRequestVO getVendorUpdateDetailNew(Long vendorId) {
        VendorChangeRequestVO vendorChangeRequestVO;
        // 供应商变更信息
        VendorChange vendorChange;

        // 根据供应商id在供应商变更表中查找最后一次变更信息
        List<VendorChange> vendorChangeList = super.list(new LambdaQueryWrapper<VendorChange>()
                .eq(VendorChange::getVendorId, vendorId)
                .orderByDesc(VendorChange::getVersion));
        // 不存在变更版本时，创建一份VO版本
        if (CollectionUtils.isEmpty(vendorChangeList)) {
            vendorChange = this.createVendorChange(vendorId);
            // 创建最新版本副本
            vendorChangeRequestVO = this.createCopy(vendorChange);
        } else {
            // 存在变更版本
            vendorChange = vendorChangeList.get(0);
            // 获取最新版本副本
            vendorChangeRequestVO = this.createCopy(vendorChange);
        }
        // 处理返回的内容
        return this.handleReturnInfo(vendorChangeRequestVO);
    }

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
            if(vendor.getState()==VendorStateEnum.REJECT.getState()||vendor.getState()==VendorStateEnum.SAVE.getState()
                    ||vendor.getState()==VendorStateEnum.IN_APPROVAL.getState()){
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

        if(vendorChangeRequestVO.getVendorChange() != null && StringUtil.isNotEmpty(vendorChangeRequestVO.getVendorChange().getAccountBranch())){
            DwCdBank bank = bankService.selectBankById(vendorChangeRequestVO.getVendorChange().getAccountBranch());
            if(bank != null){
                vendorChangeRequestVO.getVendorChange().setBankName(bank.getName());
            }else {
                vendorChangeRequestVO.getVendorChange().setBankName(vendorChangeRequestVO.getVendorChange().getAccountBranch());
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
        Date integrityistDate = null;
        for(VendorCertificationChange certification : certificationChangeList){
            if(CertificationTypeEnum.BUSINESS_LICENSE.equalsType(certification.getBusinessCode()))  {
                List<VendorCertificationChange> list = requestVO.getBusinessLicenseList();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(certification);
                requestVO.setBusinessLicenseList(list);
                //requestVO.setBusinessLicense(certification);
            }  else if (CertificationTypeEnum.INTEGRITY.equalsType(certification.getBusinessCode())) {
                List<VendorCertificationChange> list = requestVO.getIntegrityList();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(certification);
                requestVO.setIntegrityList(list);
                if(integrityistDate == null){
                    integrityistDate = certification.getEffectiveEndDate();
                    requestVO.setIntegrity(certification);
                }else if(certification.getEffectiveEndDate() != null
                        && certification.getEffectiveEndDate().compareTo(integrityistDate) < 0){
                    integrityistDate = certification.getEffectiveEndDate();
                    requestVO.setIntegrity(certification);
                }
                //requestVO.setIntegrity(certification);
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
            paramMap.put("businessTitle", "修改信息审批");
            paramMap.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.VENDOR_CHANGE_APPROVE.getDesc(), vendor.getEnterpriseName()));
            paramMap.put("detailUrl", "/vendor/vendor-detail/"+ Base64.encodeStr(("\""+vendor.getId().toString()+"\"").getBytes(),true,true));
            UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATEINFO.name()).
                    businessId(vendor.getId().toString())
                    .toDoType(ToDoTypeEnum.EXAMINE.name()).build();
            paramMap.put("userObj", JSON.toJSONString(userObj));
            String org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
            //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
            String customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATEINFO.getIdentifying().replace("{org}",org);
            /* 获取三级单位 */
            String orgThree = underlingSystemService.getL3OrgByOrgId(vendor.getFirstCooperationCompanyCode());
            /* 获取所有流程 */
            List<ListCataLogDTO> listCataLogDTOS = underlingSystemService.listCatalog();
            if (listCataLogDTOS != null) {
                /* 判断二级单位流程是否存在 */
                ListCataLogDTO cataLogDTOTwo = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(org)).findFirst().orElse(null);
                if (cataLogDTOTwo != null) {
                    /* 赋值使用二级单位 */
                    customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATEINFO.getIdentifying().replace("{org}",org);
                }
                if (orgThree != null) {
                    /* 判断三级单位流程是否存在 */
                    String finalOrgThree = orgThree;
                    ListCataLogDTO cataLogDTOThree = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(finalOrgThree)).findFirst().orElse(null);
                    if (cataLogDTOThree != null) {
                        /* 赋值使用三级单位 */
                        customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATEINFO.getIdentifying().replace("{org}",orgThree);
                    }
                }
            }
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
            /* 递归拼接部门名称 */
            vendorState.setFirstCooperationCompanyName(remoteSystemService.getDeptNameLoop(vendor.getFirstCooperationCompanyCode(),"null", SecurityConstants.INNER));

            VendorBlackRequestVO vendorBlack = this.getBlackDetail(vendorChange);
            if(vendorVO != null && StringUtil.isNotEmpty(vendorVO.getAccountBranch())){
                DwCdBank bank = bankService.selectBankById(vendorVO.getAccountBranch());
                if(bank != null){
                    vendorVO.setOpeningBranch(bank.getName());
                }else {
                    vendorVO.setOpeningBranch(vendorVO.getAccountBranch());
                }
            }
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
            if(vendorChangeList.get(0).getVersion().equals(Integer.valueOf(0))){
                return id;
            }
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
        //推送供应商信息
        pushVendor(vendorChange.getVendorId(),Vendor.LOG_TYPE_MODIFY);
        //vendorService.pushMarketVendor(vendorChange.getVendorId());
    }

    private void pushVendor(Long id, String type) {
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
        Vendor bean = vendorService.getById(id);
        if(bean != null){
            Map<String, Object> map = new HashMap<>();
            map.put("internal_id", bean.getId() + "");
            SysDept   newdept= remoteSystemService.getByThridDeptId(bean.getFirstCooperationCompanyCode(),"inner");
            map.put("dept_id",  newdept.getZtDeptId());
            //map.put("cust_mercht_id",  "");
            map.put("cust_mercht_full_name",  bean.getEnterpriseName());
            map.put("cust_mercht_cdtfy",  "供应商");
            map.put("cust_mercht_cdtfy_cd",  "G");
            map.put("is_ext_cust_mercht_cate", sysDictDataService.getRemark("is_external",bean.getIsExternal()+"","label"));
            map.put("is_ext_cust_mercht_cate_cd",  sysDictDataService.getRemark("is_external",bean.getIsExternal()+"",null));
            map.put("cust_mercht_attr",  "法人单位");
            map.put("cust_mercht_attr_cd",  "1");
            //map.put("cust_mercht_modif_pre_name",  );//客商变更前名称(曾用名)
            map.put("corp_princ_legal_rep",  bean.getLegalRepresentative());
            map.put("unified_soci_crdt_cd",  bean.getSocialCreditCode());
            map.put("rgst_cap", bean.getRegisteredCapital()==null? new BigDecimal(0):bean.getRegisteredCapital().multiply(new BigDecimal(10000)) );
            String range = bean.getBusinessScope();
            if(range !=null &&range.length()>0){
                range = range.replaceAll("\\n|\\r\\n", "");
            }
            map.put("oper_range", range);
            //map.put("fdg_tm",  "");//成立时间
            map.put("czp_zone_rgst_name",  "中国");
            map.put("czp_zone_rgst_cd",  "156");
            map.put("admin_region_prov_city_county_rgst_nm",  bean.getEnterpriseCityName());
            map.put("admin_region_prov_city_county_rgst_cd",  bean.getEnterpriseCityCode());
            map.put("dtl_addr",  bean.getEnterpriseAddress());
            map.put("cust_mercht_status",  "正常");
            map.put("cust_mercht_status_cd",  "1");//客商状态
            map.put("cust_mercht_char",   sysDictDataService.getRemark("enterprise_nature", bean.getEnterpriseNature()+"","label"));
            map.put("cust_mercht_char_cd", sysDictDataService.getRemark("enterprise_nature", bean.getEnterpriseNature()+"",null));//企业性质
            map.put("addvl_pay_tax_type",  sysDictDataService.getRemark("taxpayer_type", bean.getTaxpayerType()+"","label"));//????
            map.put("addvl_pay_tax_type_cd",  sysDictDataService.getRemark("taxpayer_type", bean.getTaxpayerType()+"",null));//增值税纳税人类型
            map.put("setup_dt",  bean.getCreateTime());
            if(StringUtil.isNotEmpty(bean.getFirstCooperationCompanyCode())){
                SysDept dept = remoteSystemService.getByThridDeptId(bean.getFirstCooperationCompanyCode(),SecurityConstants.INNER);
                if(dept != null){
                    map.put("setup_corp_org_name",  dept.getDeptName());//创建单位
                    map.put("setup_corp_org_id",  dept.getInterialId());//创建单位ID
                }
            }
            map.put("setup_corp_org_code",  bean.getFirstCooperationCompanyCode());//创建单位编码
            map.put("cust_mercht_cont_tel",  bean.getContactPhone());
            if(StringUtil.isNotEmpty(bean.getEnterpriseType())){
                String[] split = bean.getEnterpriseType().split(",");
                VendorClassify classify = vendorClassifyService.getById(Long.valueOf(split[0]));
                if(classify != null){
                    map.put("provi_type",  classify.getMiddleName());
                    map.put("provi_type_cd",  classify.getMiddleCode());//供应商主业类型
                }
            }
            //map.put("setup_person",  "胡杰");//创建人
            //map.put("setup_person_id",  "201700209");
            JSONObject jsonObject = new JSONObject(map);
            if(bean != null && StringUtil.isEmpty(bean.getEnterpriseCode())){
                //如果没有中台code就要走中台新增方法
                JSONObject object = dataCenterUtil.postCommonInfo(jsonObject, dataMiddlePlatformConfig.getVendorAdd(), Vendor.LOG_TYPE_ADD, SecurityUtils.getUsername(), null);
                if (object != null && object.containsKey("code") && object.getInteger("code") == 200) {
                    //成功的
                    JSONObject data = object.getJSONObject("data");
                    if(data != null && data.containsKey("added")) {
                        JSONArray added = data.getJSONArray("added");
                        if (added != null && added.size() >0) {
                            JSONObject obj = added.getJSONObject(0);
                            String custMerchtId = obj.getString("cust_mercht_id");
                            vendorService.update(new LambdaUpdateWrapper<Vendor>()
                                    .set(Vendor::getEnterpriseCode, custMerchtId)
                                    .set(Vendor::getMiddleVendorCode, custMerchtId)
                                    .eq(Vendor::getId, id));
                            List<TAccountInfo> list = accountService.list(new LambdaUpdateWrapper<TAccountInfo>()
                                    .eq(TAccountInfo::getUpId, bean.getId()));
                            if (!list.isEmpty()) {
                                list.stream().forEach(p -> {
                                    accountService.pushAcct(p,bean, custMerchtId, Vendor.LOG_TYPE_ADD);
                                });
                                list.stream().forEach(p -> {
                                    accountService.pushAcct(p,bean, custMerchtId, Vendor.LOG_TYPE_MODIFY);
                                });
                            }
                        }
                    }
                }
            }else{
                dataCenterUtil.postCommonInfo(jsonObject,dataMiddlePlatformConfig.getVendorUpdate(),type, SecurityUtils.getUsername(),null);
                /*JSONObject object =dataCenterUtil.postCommonInfo(jsonObject,dataMiddlePlatformConfig.getVendorUpdate(),type, SecurityUtils.getUsername(),null);
                if (object != null && object.containsKey("code") && object.getInteger("code") == 200) {
                    //成功的
                    JSONObject data = object.getJSONObject("data");
                    if(data != null && data.containsKey("updated")) {
                        JSONArray added = data.getJSONArray("updated");
                        if (added != null && added.size() >0) {
                            JSONObject obj = added.getJSONObject(0);
                            String custMerchtId = obj.getString("cust_mercht_id");
                            System.out.println("custMerchtId:"+custMerchtId);
                        }
                    }
                }*/
            }

        }
        });
        executor.shutdown();
    }

    /**
     * 审批通过
     * @param variables
     */
    @Override
    public void processAuditPass(Map<String, Object> variables) {
        System.out.println("-------------vendorChange----processAuditPass----start");
        System.out.println("businessId-----");
        String businessId = variables.get("businessId").toString();
        System.out.println(businessId);
        super.update(new LambdaUpdateWrapper<VendorChange>()
                .set(VendorChange::getChangeStatus,VendorStateEnum.APPROVE.getState())
                .eq(VendorChange::getId, businessId));
        // 审批通过
        VendorChange vendorChange = super.getById(businessId);
        this.handleApprove(vendorChange);
        System.out.println("-------------vendorChange----processAuditPass----end");
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
     * 审批测回
     * @param variables
     */
    @Override
    public void processAuditRevoke(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        // 修改变更状态为保存
        super.update(new LambdaUpdateWrapper<VendorChange>()
                .set(VendorChange::getChangeStatus,VendorStateEnum.SAVE.getState())
                .eq(VendorChange::getId, businessId));
        // 审批驳回将供应商状态改回审批通过
        VendorChange vendorChange = super.getById(businessId);
        vendorService.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getState,VendorStateEnum.APPROVE.getState())
                .eq(Vendor::getId, vendorChange.getVendorId()));
    }

    @Override
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        VendorChange vendorChange = getById(requestDTO.getBusinessId());
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
        VendorChange vendorChange = getById(requestDTO.getBusinessId());
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
        VendorChange vendorChange = getById((Serializable) variables.get("businessId"));
        String org = underlingSystemService.getL2OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        variables.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
        variables.put("companyId", org);/* 公司 二级单位 */
        variables.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        variables.put("parentProjectCode", org);/* 父项目编码(项目部) */
        return processService.auditProcessInstance(ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATEINFO.getIdentifying(),variables);
    }

    @Override
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        VendorChange vendorChange = getById(requestDTO.getBusinessId());
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
