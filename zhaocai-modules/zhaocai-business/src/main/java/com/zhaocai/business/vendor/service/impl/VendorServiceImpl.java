package com.zhaocai.business.vendor.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.KeyUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.deepoove.poi.data.Numberings;
import com.zhaocai.business.common.conver.ProcurementPlanTypeConver;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.NotFoundException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
//import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.business.manager.http.dto.res.ListCataLogDTO;
import com.zhaocai.business.manager.http.service.UnderlingRestTemplateService;
import com.zhaocai.business.manager.http.dto.res.*;
import com.zhaocai.business.manager.http.service.UnderlingRestTemplateService;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.procurement.vo.res.ContractPlanningListVO;
import com.zhaocai.business.pub.domain.DwCdBank;
import com.zhaocai.business.pub.domain.TAccountInfo;
import com.zhaocai.business.pub.service.*;
import com.zhaocai.business.pub.service.impl.SysDictDataServiceImpl;
import com.zhaocai.business.pub.vo.req.TAccountInfoVo;
import com.zhaocai.business.vendor.config.DataMiddlePlatformConfig;
import com.zhaocai.business.vendor.domain.*;
import com.zhaocai.business.vendor.mapper.VendorMapper;
import com.zhaocai.business.vendor.service.*;
import com.zhaocai.business.vendor.util.DataCenterUtil;
import com.zhaocai.business.vendor.vo.req.*;
import com.zhaocai.business.vendor.vo.res.*;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import com.zhaocai.common.core.utils.uuid.IdUtils;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.common.signature.dto.command.CompanyAuthCommandRequest;
import com.zhaocai.common.signature.dto.command.CompanyAuthCommandRequestBuilder;
import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.service.SignatureCommandFactory;
import com.zhaocai.common.signature.service.command.CompanyAuthCommand;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysDictData;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteSystemService;
import com.zhaocai.system.api.system.RemoteUserService;
import lombok.extern.slf4j.Slf4j;
import net.qiyuesuo.v3sdk.model.company.response.CompanyauthH5pageResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 供应商Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Slf4j
@Service
public class VendorServiceImpl extends ServiceImpl<VendorMapper,Vendor> implements IVendorService {

    @Autowired
    private IVendorContactService vendorContactService;

    @Autowired
    private IVendorCertificationService vendorCertificationService;

    @Autowired
    private IAttachmentService attachmentService;

    @Autowired
    private IVendorOperateLogService vendorOperateLogService;

    @Autowired
    private IVendorPerformanceEvaluationService vendorPerformanceEvaluationService;

    @Autowired
    private IVendorClassifyService vendorClassifyService;

    @Autowired
    private IBPMProcessService processService;

    @Lazy
    @Autowired
    private IVendorChangeService vendorChangeService;


    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Autowired
    private ISystemUserService systemUserService;

    @Autowired
    private IAccountService accountService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private IBankService bankService;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private DataCenterUtil dataCenterUtil;

    @Autowired
    private DataMiddlePlatformConfig dataMiddlePlatformConfig;

    @Autowired
    private IVendorCertificationChangeService certificationChangeService;

    @Autowired
    private IVendorContactChangeService contactChangeService;

    @Lazy
    @Autowired
    private ITbVendorEvaluateService tbVendorEvaluateService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public VendorRegisterRequestVO getVendorUpdateDetail(Long vendorId) {
        VendorRegisterRequestVO vendorRequestVO = new VendorRegisterRequestVO();
        // 供应商联系人变更信息
        List<VendorContact> contactList;

        // 供应商联系人变更信息
        List<VendorContact> contactListMain;
        // 根据供应商id在供应商
        Vendor vendor = super.getById(vendorId);
        if(vendor != null && StringUtil.isNotEmpty(vendor.getAccountBranch())){
            DwCdBank bank = bankService.selectBankById(vendor.getAccountBranch());
            if(bank != null){
                vendor.setBankName(bank.getName());
            }else {
                vendor.setBankName(vendor.getAccountBranch());
            }
        }
        contactList = vendorContactService.list(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getVendorId, vendorId)
                .eq(VendorContact::getIsMainContact, 1));
        vendorRequestVO  =  vendorCertificationService.listCertification(vendorRequestVO,vendorId,contactList.get(0).getId());
        vendorRequestVO.setVendor(vendor);
        if(contactList!=null&&contactList.size()>0){
            vendorRequestVO.setVendorContact(contactList.get(0));
        }

        return vendorRequestVO;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void vendorRegister(VendorRegisterRequestVO requestVO) {
        checkVendorInfo(requestVO.getVendor());

        // 保存基本信息
        Vendor vendor = requestVO.getVendor();
        vendor.setState(VendorStateEnum.APPROVE.getState());
        vendor.setSignState(SignStateEnum.TO_SIGN.getState());
        vendor.setVendorClass(1);
        vendor.setVendorLevel(3);
        super.save(vendor);

        // 保存供应商资质
        vendorCertificationService.addCertification(requestVO.getBusinessLicense(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
        vendorCertificationService.addCertification(requestVO.getIntegrity(), CertificationTypeEnum.INTEGRITY,vendor.getId());
        Long legalAuthorizationId = vendorCertificationService.addCertification(requestVO.getLegalAuthorization(),CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
        /*if (CollUtil.isNotEmpty(requestVO.getRelevantCertificationList())){*/
            vendorCertificationService.addCertification(requestVO.getRelevantCertificationList(), CertificationTypeEnum.RELEVANT_CERTIFICATION,vendor.getId());
        /*}*/

        // 主要联系人
        VendorContact contact = requestVO.getVendorContact();

        // 新增供应商账号
        Long longUserId = vendorContactService.addLoginUser(contact.getContactPhone(),contact.getContactName(),null);
        contact.setLoginUserId(longUserId);
        contact.setCertificationId(legalAuthorizationId);
        contact.setVendorId(vendor.getId());
        // 注册时为默认为管理员
        contact.setIsManager(1);
        long contactId = vendorContactService.saveMainVendorContact(contact);

        // 更新法人授权的 businessId
        vendorCertificationService.update(new LambdaUpdateWrapper<VendorCertification>()
                .set(VendorCertification::getBusinessId,contactId)
                .eq(VendorCertification::getId,legalAuthorizationId));
    }



    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void register(VendorRegisterRequestVO requestVO) {
        Boolean flag =true;
        Long legalAuthorizationId = null;
        Vendor vendor = new Vendor();
        //如果重新提交则走if里面的方法
        if(requestVO.getVendor()!=null&&requestVO.getVendor().getId()!=null){
            flag = false;
            Long id =requestVO.getVendor().getId();
            vendor = super.getById(requestVO.getVendor().getId());
            if(vendor!=null){
                Vendor vendor1 = BeanCopierUtil.copyBean(vendor, Vendor.class);
                // 保存基本信息
                vendor = requestVO.getVendor();
                vendor.setState(VendorStateEnum.IN_APPROVAL.getState());
                vendor.setSignState(SignStateEnum.TO_SIGN.getState());
                vendor.setVendorClass(1);
                vendor.setVendorLevel(3);
                super.saveOrUpdate(vendor);
                //保存银行账户信息
                //先删除
                List<TAccountInfo> list = accountService.list(new LambdaUpdateWrapper<TAccountInfo>()
                        .eq(TAccountInfo::getUpId, vendor.getId()));
                if(!list.isEmpty()){
                    list.stream().forEach(p->{
                        accountService.deleteAccountById(p.getId());
                    });
                }
                //再新增
                DwCdBank bank = bankService.selectBankById(vendor.getAccountBranch());
                TAccountInfo acount = new TAccountInfo();
                acount.setBankAccount(vendor.getBankAccount());
                acount.setBipId(vendor.getAccountBranch());
                acount.setCurrency(vendor.getCurrencyCode());
                if(bank!=null){
                    acount.setInterbankNumber(bank.getCode());
                    acount.setAffiliatedBank(bank.getParentName());
                    acount.setOpeningBranch(bank.getName());
                }
                acount.setUpId(vendor.getId());
                //供应商
                acount.setAcountType(AccountEnum.GYS_TYPE.getType());
                //默认账户
                acount.setStatus(1);
                acount.setId(IdUtil.getSnowflakeNextId());
                accountService.save(acount);
                /*TAccountInfo acount = new TAccountInfo();
                acount.setBankAccount(vendor.getBankAccount());
                acount.setBipId(vendor.getAccountBranch());
                acount.setCurrency(vendor.getCurrencyCode());
                acount.setUpId(vendor.getId());
                acount.setAcountType(AccountEnum.GYS_TYPE.getType());
                accountService.save(acount);*/
                // 保存供应商资质
                if(requestVO.getBusinessLicense() != null){
                    vendorCertificationService.addCertification(requestVO.getBusinessLicense(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
                }
                if(requestVO.getIntegrity() != null){
                    vendorCertificationService.addCertification(requestVO.getIntegrity(), CertificationTypeEnum.INTEGRITY,vendor.getId());
                }
                if(requestVO.getLegalAuthorization() != null){
                    legalAuthorizationId = vendorCertificationService.addCertification(requestVO.getLegalAuthorization(),CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
                }
                /*if (CollUtil.isNotEmpty(requestVO.getRelevantCertificationList())){*/
                    vendorCertificationService.addCertification(requestVO.getRelevantCertificationList(), CertificationTypeEnum.RELEVANT_CERTIFICATION,vendor.getId());
                /*}*/
                if (CollUtil.isNotEmpty(requestVO.getBusinessLicenseList())){
                    vendorCertificationService.addCertification(requestVO.getBusinessLicenseList(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
                }
                if (CollUtil.isNotEmpty(requestVO.getIntegrityList())){
                    vendorCertificationService.addCertification(requestVO.getIntegrityList(), CertificationTypeEnum.INTEGRITY,vendor.getId());
                }
                if (CollUtil.isNotEmpty(requestVO.getLegalAuthorizationList())){
                    legalAuthorizationId = vendorCertificationService.addCertificationList(requestVO.getLegalAuthorizationList(), CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
                }
                /*vendorCertificationService.addCertification(requestVO.getBusinessLicense(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
                vendorCertificationService.addCertification(requestVO.getIntegrity(), CertificationTypeEnum.INTEGRITY,vendor.getId());
                legalAuthorizationId = vendorCertificationService.addCertification(requestVO.getLegalAuthorization(),CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
                if (CollUtil.isNotEmpty(requestVO.getRelevantCertificationList())){
                    vendorCertificationService.addCertification(requestVO.getRelevantCertificationList(), CertificationTypeEnum.RELEVANT_CERTIFICATION,vendor.getId());
                }*/
                // 主要联系人
               VendorContact contact = requestVO.getVendorContact();
                // 新增供应商账号
                //  Long longUserId = vendorContactService.getULoginUser(contact.getContactPhone(),contact.getContactName());
                //  contact.setLoginUserId(longUserId);
                contact.setCertificationId(legalAuthorizationId);
                contact.setVendorId(vendor.getId());
                // 注册时为默认为管理员
                contact.setIsManager(1);
                vendorContactService.saveOrUpdate(contact);
                // 更新法人授权的 businessId
                vendorCertificationService.update(new LambdaUpdateWrapper<VendorCertification>()
                        .set(VendorCertification::getBusinessId,contact.getId())
                        .eq(VendorCertification::getId,legalAuthorizationId));
                if(vendor1 != null && StringUtil.isNotEmpty(vendor1.getWfProcessId())
                        && VendorProcessTypeEnum.VENDOR_REGISTER.getState().equals(vendor1.getProcessType())
                        && !vendor1.getFirstCooperationCompanyCode().equals(vendor.getFirstCooperationCompanyCode())){
                    Long uuid = IdUtil.getSnowflakeNextId();
                    System.out.println("uuid:"+uuid);
                    //重新赋值供应商id
                    baseMapper.updateByMyId(id,uuid);
                    //更新联系人的关联id
                    vendorContactService.updateByVendorId(id,uuid);
                    //更新资质的关联id
                    vendorCertificationService.updateByVendorId(id,uuid);
                    //更新银行账号的关联id
                    accountService.updateByVendorId(id,uuid);
                    vendor.setId(uuid);
                }
            }else{
                flag = true;
            }
        }else{
            flag =true;
        }
        if(flag){
            checkVendorInfo(requestVO.getVendor());
//            checkVendorContact(requestVO.getVendor(), requestVO.getVendorContact());
            // 保存基本信息
            vendor = requestVO.getVendor();
            vendor.setState(VendorStateEnum.IN_APPROVAL.getState());
            vendor.setSignState(SignStateEnum.TO_SIGN.getState());
            vendor.setVendorClass(1);
            vendor.setVendorLevel(3);
            super.save(vendor);
            //保存银行账户信息
            DwCdBank bank = bankService.selectBankById(vendor.getAccountBranch());
            TAccountInfo acount = new TAccountInfo();
            acount.setBankAccount(vendor.getBankAccount());
            acount.setBipId(vendor.getAccountBranch());
            acount.setCurrency(vendor.getCurrencyCode());
            if(bank!=null){
               acount.setInterbankNumber(bank.getCode());
               acount.setAffiliatedBank(bank.getParentName());
               acount.setOpeningBranch(bank.getName());
            }
            acount.setUpId(vendor.getId());
            //供应商
            acount.setAcountType(AccountEnum.GYS_TYPE.getType());
            //默认账户
            acount.setStatus(1);
            acount.setId(IdUtil.getSnowflakeNextId());
            accountService.save(acount);
            // 保存供应商资质
            if(requestVO.getBusinessLicense() != null){
                vendorCertificationService.addCertification(requestVO.getBusinessLicense(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
            }
            if(requestVO.getIntegrity() != null){
                vendorCertificationService.addCertification(requestVO.getIntegrity(), CertificationTypeEnum.INTEGRITY,vendor.getId());
            }
            if(requestVO.getLegalAuthorization() != null){
                legalAuthorizationId = vendorCertificationService.addCertification(requestVO.getLegalAuthorization(),CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
            }
            /*if (CollUtil.isNotEmpty(requestVO.getRelevantCertificationList())){*/
                vendorCertificationService.addCertification(requestVO.getRelevantCertificationList(), CertificationTypeEnum.RELEVANT_CERTIFICATION,vendor.getId());
            /*}*/
            if (CollUtil.isNotEmpty(requestVO.getBusinessLicenseList())){
                vendorCertificationService.addCertification(requestVO.getBusinessLicenseList(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
            }
            if (CollUtil.isNotEmpty(requestVO.getIntegrityList())){
                vendorCertificationService.addCertification(requestVO.getIntegrityList(), CertificationTypeEnum.INTEGRITY,vendor.getId());
            }
            if (CollUtil.isNotEmpty(requestVO.getLegalAuthorizationList())){
                legalAuthorizationId = vendorCertificationService.addCertificationList(requestVO.getLegalAuthorizationList(), CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
            }
            /*vendorCertificationService.addCertification(requestVO.getBusinessLicense(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
            vendorCertificationService.addCertification(requestVO.getIntegrity(), CertificationTypeEnum.INTEGRITY,vendor.getId());
            legalAuthorizationId = vendorCertificationService.addCertification(requestVO.getLegalAuthorization(),CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
            if (CollUtil.isNotEmpty(requestVO.getRelevantCertificationList())){
                vendorCertificationService.addCertification(requestVO.getRelevantCertificationList(), CertificationTypeEnum.RELEVANT_CERTIFICATION,vendor.getId());
            }*/
            // 主要联系人
            VendorContact contact = requestVO.getVendorContact();
            // 新增供应商账号
            Long longUserId = vendorContactService.addLoginUser(contact.getContactPhone(),contact.getContactName(),null);
            contact.setLoginUserId(longUserId);
            contact.setCertificationId(legalAuthorizationId);
            contact.setVendorId(vendor.getId());
            // 注册时为默认为管理员
            contact.setIsManager(0);
            //新增法人账号(存在法人则法人为管理员，法人和主要联系人一样，则生成主要联系人信息)
            /*if(!contact.getContactPhone().equals(vendor.getLegalPhone())){
                Long longinId =vendorContactService.addLoginUser(vendor.getLegalPhone(),vendor.getLegalRepresentative(),null);
                VendorContact contact1 = new VendorContact();
                contact1.setVendorId(vendor.getId());
                contact1.setContactName(vendor.getLegalRepresentative());
                contact1.setContactPhone(vendor.getLegalPhone());
                contact1.setLoginUserId(longinId);
                contact1.setContactIdCard(vendor.getLegalIdCard());
                contact1.setIsManager(1);
                vendorContactService.saveVendorContact(contact1);
            }else{
                contact.setIsManager(1);
            }*/
            long contactId=vendorContactService.saveMainVendorContact(contact);
            // 更新法人授权的 businessId
            vendorCertificationService.update(new LambdaUpdateWrapper<VendorCertification>()
                    .set(VendorCertification::getBusinessId,contactId)
                    .eq(VendorCertification::getId,legalAuthorizationId));
        }

        //接入底层逻辑平台流程
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("businessId", vendor.getId());
        paramMap.put("businessTitle", "供应商注册审批");
        /* 获取二级单位 */
        String org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
        String customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.getIdentifying().replace("{org}",org);
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        /* 获取所有流程 */
        List<ListCataLogDTO> listCataLogDTOS = underlingSystemService.listCatalog();
        if (listCataLogDTOS != null) {
            /* 判断二级单位流程是否存在 */
            ListCataLogDTO cataLogDTOTwo = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(org)).findFirst().orElse(null);
            if (cataLogDTOTwo != null) {
                /* 赋值使用二级单位 */
                customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.getIdentifying().replace("{org}",org);
            }
            if (orgThree != null) {
                /* 判断三级单位流程是否存在 */
                String finalOrgThree = orgThree;
                ListCataLogDTO cataLogDTOThree = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(finalOrgThree)).findFirst().orElse(null);
                if (cataLogDTOThree != null) {
                    /* 赋值使用三级单位 */
                    customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.getIdentifying().replace("{org}",orgThree);
                }
            }
        }

        paramMap.put("customProcessKey", customProcessKey);
        paramMap.put("businessContent",
                String.format(ApproveFlowPromptTemplateEnum.VENDOR_REGISTER_APPROVE.getDesc(), vendor.getEnterpriseName()));
        paramMap.put("detailUrl", "/vendor/vendor-detail/"+ Base64.encodeStr(("\""+vendor.getId().toString()+"\"").getBytes(),true,true));
        UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.name()).
                businessId(vendor.getId().toString())
                .toDoType(ToDoTypeEnum.EXAMINE.name()).build();
        paramMap.put("userObj", JSON.toJSONString(userObj));

        /* 获取三级单位 */
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        paramMap.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
        paramMap.put("companyId", org);/* 公司 二级单位 */
        paramMap.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        paramMap.put("parentProjectCode", org);/* 父项目编码(项目部) */
        processService.startProcessInstance(ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.getIdentifying(),paramMap);
    }

    /**
     * 校验新增用户账号是否已存在
     * @param vendor
     * @param vendorContact
     */
    private void checkVendorContact(Vendor vendor, VendorContact vendorContact) {
        SysUser user = remoteUserService.getUserInfoByUsername(vendor.getLegalPhone(), SecurityConstants.INNER);
        if (user != null) {
            throw new ParamValidateException("该法人联系方式已存在");
        }
        user = remoteUserService.getUserInfoByUsername(vendorContact.getContactPhone(), SecurityConstants.INNER);
        if (user != null) {
            throw new ParamValidateException("该联系人电话已存在");
        }
    }




    @Override
    public VendorDetailVO getVendorDetail(Long id) {
        Vendor vendor = super.getById(id);
        ValidateUtils.isNullException(vendor,"该供应商信息不存在");
        VendorVO vendorVO = BeanCopierUtil.copyBean(vendor,VendorVO.class);

        // 处理企业分类
        vendorVO.setEnterpriseTypeText(vendorClassifyService.getVendorClassifyName(vendorVO.getEnterpriseType()));

        List<VendorContact> contactList = vendorContactService.listContactByVendorId(id);
        VendorMainContactVO mainContact = contactList.stream()
                .filter(x -> x.getIsMainContact().equals(1))
                .map(mc -> BeanCopierUtil.copyBean(mc, VendorMainContactVO.class))
                .findFirst()
                .orElseThrow(()->new ParamValidateException("该供应商没有设置主要联系人"));

        List<VendorContactVO> contactVOList = contactList.stream()
                .filter(x -> x.getIsMainContact().equals(0))
                .map(mc -> BeanCopierUtil.copyBean(mc, VendorContactVO.class))
                .collect(Collectors.toList());
        if(vendorVO != null && StringUtil.isNotEmpty(vendorVO.getAccountBranch())){
            DwCdBank bank = bankService.selectBankById(vendorVO.getAccountBranch());
            if(bank != null){
                vendorVO.setOpeningBranch(bank.getName());
            }else {
                vendorVO.setOpeningBranch(vendorVO.getAccountBranch());
            }
        }
        return VendorDetailVO.builder()
                .vendor(vendorVO)
                .mainContact(mainContact)
                .contactList(contactVOList)
                .build();
    }

    @Override
    public void saveVendor(VendorSaveRequestVO requestVO) {
        Vendor checkVendor = super.getById(requestVO.getVendor().getId());
        Vendor updateVendor = requestVO.getVendor();

        checkVendorInfo(updateVendor);

        VendorMainContactVO mainContact = vendorContactService.getMainContact(updateVendor.getId());
        VendorContact updateContact = requestVO.getVendorContact();

        boolean approveFlag = false;
        if (!checkVendor.getEnterpriseName().equals(updateVendor.getEnterpriseName())
            || !checkVendor.getSocialCreditCode().equals(updateVendor.getSocialCreditCode())
                || !mainContact.getContactPhone().equals(updateContact.getContactPhone())) {
            log.warn("供应商[{}]主要信息已发生变更，需要进行审核",updateVendor.getId());
            approveFlag = true;
        }

        // 保存供应商信息
        this.updateById(updateVendor);

        // 保存供应商主要联系人
        vendorContactService.saveMainVendorContact(requestVO.getVendorContact());
        if (approveFlag) {
            //todo 发起审核

        }
    }

    // todo 这里需要使用缓存
    @Override
    public Vendor getByLoginUser(Long userId) {
        VendorContact vendorContact = vendorContactService.getVendorContactByLoginUser(userId);
        Vendor vendor = super.getById(vendorContact.getVendorId());
        return Optional.ofNullable(vendor).orElseThrow(() -> new ParamValidateException("用户对应的供应商信息不存在"));
    }

    @Override
    public Vendor getByLoginUserTwo(Long userId) {
        VendorContact vendorContact = vendorContactService.getVendorContactByLoginUser(userId);
        Vendor vendor = super.getById(vendorContact.getVendorId());
        return Optional.ofNullable(vendor).orElse(new Vendor());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public VendorOneRequestVO getLoginUserDetail(Long loginUserId) {
        VendorOneRequestVO vo = new VendorOneRequestVO();
        VendorContact vendorContact = vendorContactService.getVendorContactByLoginUser(loginUserId);
        ValidateUtils.validateStatusNotEquals(VendorContactStateEnum.VALID::equalsState,vendorContact.getState(),"您当前已被禁用!!!");
        vo.setVendorContact(vendorContact);
        Vendor vendor = super.getById(vendorContact.getVendorId());
        if(vendor != null && StringUtil.isNotEmpty(vendor.getAccountBranch())){
            DwCdBank bank = bankService.selectBankById(vendor.getAccountBranch());
            if(bank != null){
                vendor.setBankName(bank.getName());
            }else {
                vendor.setBankName(vendor.getAccountBranch());
            }
        }
        vo.setVendor(vendor);
        if(vendorContact.getVendorId() != null && vendor != null){
            vendorCertificationService.listCertification(vo,vendorContact.getVendorId(), vendorContact.getId());
        }
        return vo;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void registerSave(VendorOneRequestVO requestVO) {
        Boolean flag =true;
        Long legalAuthorizationId = null;
        Vendor vendor = new Vendor();
        //如果重新提交则走if里面的方法
        if(requestVO.getVendor()!=null&&requestVO.getVendor().getId()!=null){
            flag = false;
            Long id =requestVO.getVendor().getId();
            vendor = super.getById(requestVO.getVendor().getId());
            if(vendor!=null){
                // 保存基本信息
                vendor = requestVO.getVendor();
                vendor.setState(VendorStateEnum.SAVE.getState());
                vendor.setSignState(SignStateEnum.TO_SIGN.getState());
                vendor.setVendorClass(1);
                vendor.setVendorLevel(3);
                super.saveOrUpdate(vendor);
                //保存银行账户信息
                // 保存供应商资质
                if(requestVO.getBusinessLicense() != null){
                    vendorCertificationService.addCertification(requestVO.getBusinessLicense(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
                }
                if(requestVO.getIntegrity() != null){
                    vendorCertificationService.addCertification(requestVO.getIntegrity(), CertificationTypeEnum.INTEGRITY,vendor.getId());
                }
                if(requestVO.getLegalAuthorization() != null){
                    legalAuthorizationId = vendorCertificationService.addCertification(requestVO.getLegalAuthorization(),CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
                }
                /*if (CollUtil.isNotEmpty(requestVO.getRelevantCertificationList())){*/
                    vendorCertificationService.addCertification(requestVO.getRelevantCertificationList(), CertificationTypeEnum.RELEVANT_CERTIFICATION,vendor.getId());
                /*}*/
                if (CollUtil.isNotEmpty(requestVO.getBusinessLicenseList())){
                    vendorCertificationService.addCertification(requestVO.getBusinessLicenseList(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
                }
                if (CollUtil.isNotEmpty(requestVO.getIntegrityList())){
                    vendorCertificationService.addCertification(requestVO.getIntegrityList(), CertificationTypeEnum.INTEGRITY,vendor.getId());
                }
                if (CollUtil.isNotEmpty(requestVO.getLegalAuthorizationList())){
                    legalAuthorizationId = vendorCertificationService.addCertificationList(requestVO.getLegalAuthorizationList(), CertificationTypeEnum.LEGAL_AUTHORIZATION, vendor.getId());
                }
                // 主要联系人
                VendorContact contact = requestVO.getVendorContact();
                // 新增供应商账号
                contact.setCertificationId(legalAuthorizationId);
                contact.setVendorId(vendor.getId());
                // 注册时为默认为管理员
                contact.setIsManager(1);
                vendorContactService.saveOrUpdate(contact);
                // 更新法人授权的 businessId
                vendorCertificationService.update(new LambdaUpdateWrapper<VendorCertification>()
                        .set(VendorCertification::getBusinessId,contact.getId())
                        .eq(VendorCertification::getId,legalAuthorizationId));
            }else{
                flag = true;
            }
        }else{
            flag =true;
        }
        if(flag){
            vendor = requestVO.getVendor();
            Vendor checkVendor = super.getOne(new LambdaQueryWrapper<Vendor>()
                    .eq(Vendor::getSocialCreditCode,vendor.getSocialCreditCode()));
            if (checkVendor != null && !checkVendor.getId().equals(vendor.getId())) {
                throw new ParamValidateException("该统一社会信用代码已存在");
            }

            checkVendor = super.getOne(new LambdaQueryWrapper<Vendor>()
                    .eq(Vendor::getEnterpriseName,vendor.getEnterpriseName()));
            if (checkVendor != null && !checkVendor.getId().equals(vendor.getId())) {
                throw new ParamValidateException("该企业名称已存在");
            }
//            checkVendorContact(requestVO.getVendor(), requestVO.getVendorContact());
            // 保存基本信息

            vendor.setState(VendorStateEnum.SAVE.getState());
            vendor.setSignState(SignStateEnum.TO_SIGN.getState());
            vendor.setVendorClass(1);
            vendor.setVendorLevel(3);
            super.save(vendor);
            // 保存供应商资质
            if(requestVO.getBusinessLicense() != null){
                vendorCertificationService.addCertification(requestVO.getBusinessLicense(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
            }
            if(requestVO.getIntegrity() != null){
                vendorCertificationService.addCertification(requestVO.getIntegrity(), CertificationTypeEnum.INTEGRITY,vendor.getId());
            }
            if(requestVO.getLegalAuthorization() != null){
                legalAuthorizationId = vendorCertificationService.addCertification(requestVO.getLegalAuthorization(),CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
            }
            /*if (CollUtil.isNotEmpty(requestVO.getRelevantCertificationList())){*/
                vendorCertificationService.addCertification(requestVO.getRelevantCertificationList(), CertificationTypeEnum.RELEVANT_CERTIFICATION,vendor.getId());
            /*}*/
            if (CollUtil.isNotEmpty(requestVO.getBusinessLicenseList())){
                vendorCertificationService.addCertification(requestVO.getBusinessLicenseList(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
            }
            if (CollUtil.isNotEmpty(requestVO.getIntegrityList())){
                legalAuthorizationId = vendorCertificationService.addCertificationList(requestVO.getIntegrityList(), CertificationTypeEnum.INTEGRITY,vendor.getId());
            }
            if (CollUtil.isNotEmpty(requestVO.getLegalAuthorizationList())){
                legalAuthorizationId = vendorCertificationService.addCertificationList(requestVO.getLegalAuthorizationList(), CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
            }
            // 主要联系人
            VendorContact contact = requestVO.getVendorContact();
            // 修改用户别名
            //Long longUserId = vendorContactService.addLoginUser(contact.getContactPhone(),contact.getContactName(),null);
            contact.setCertificationId(legalAuthorizationId);
            contact.setVendorId(vendor.getId());
            // 注册时为默认为管理员
            contact.setIsManager(0);
            long contactId=vendorContactService.saveMainVendorContact(contact);
            // 更新法人授权的 businessId
            vendorCertificationService.update(new LambdaUpdateWrapper<VendorCertification>()
                    .set(VendorCertification::getBusinessId,contactId)
                    .eq(VendorCertification::getId,legalAuthorizationId));
        }
    }

    @Override
    public PageResult<VendorManagementListVO> listVendor(VendorManagementListQueryVO queryVO) {
        if (StringUtils.isNotEmpty(queryVO.getEnterpriseType())){
            List<Long> classifyIds = new ArrayList<>();
            //查询企业分类是否存在子集
            List<VendorClassify> classifys = vendorClassifyService.getVendorClassifySubList(Long.valueOf(queryVO.getEnterpriseType()));
            if (!CollectionUtils.isEmpty(classifys)){
                classifyIds = classifys.stream().map(VendorClassify::getId).collect(Collectors.toList());
            } else {
                classifyIds.add(Long.valueOf(queryVO.getEnterpriseType()));
            }
            queryVO.setClassifyIds(classifyIds);
        }

        IPage<VendorManagementListVO> iPage = baseMapper.selectVendorListPage(queryVO.toMybatisPage(),queryVO);
        // 异步处理结果集转换
        if (CollectionUtil.isNotEmpty(iPage.getRecords())) {
            List<CompletableFuture<VendorManagementListVO>> futureList = iPage.getRecords().stream()
                    .map(dto -> CompletableFuture.supplyAsync(() -> {
                        VendorManagementListVO vo = BeanCopierUtil.copyBean(dto,VendorManagementListVO.class);
                        if (vo.getIsBlack() == 1) {
                            vo.setVendorLibraryText("黑名单供应商");
                        } else if (vo.getState() == 1 && vo.getIsBlack() == 0
                                && VendorProcessTypeEnum.VENDOR_REGISTER.getState().equals(vo.getProcessType())) {
                            vo.setVendorLibraryText("注册待审供应商");
                        } else if (vo.getVendorClass() == 1 && vo.getIsBlack() == 0
                                && (VendorStateEnum.APPROVE.getState().equals(vo.getState()) ||
                                (VendorStateEnum.IN_APPROVAL.getState().equals(vo.getState()) &&
                                        !VendorProcessTypeEnum.VENDOR_REGISTER.getState().equals(vo.getProcessType())))) {
                            vo.setVendorLibraryText("合格供应商");
                        } else if (vo.getVendorClass() == 2 && vo.getIsBlack() == 0
                                && (VendorStateEnum.APPROVE.getState().equals(vo.getState()) ||
                                (VendorStateEnum.IN_APPROVAL.getState().equals(vo.getState()) &&
                                        !VendorProcessTypeEnum.VENDOR_REGISTER.getState().equals(vo.getProcessType())))) {
                            vo.setVendorLibraryText("战略供应商");
                        }
                        if(VendorStateEnum.APPROVE.getState().equals(vo.getState())){
                            vo.setVendorState("正常");
                        }else if(VendorStateEnum.IN_APPROVAL.getState().equals(vo.getState())
                                && VendorProcessTypeEnum.VENDOR_UPDATEINFO.getState().equals(vo.getProcessType())){
                            vo.setVendorState("信息修改待审");
                        }else if(VendorStateEnum.IN_APPROVAL.getState().equals(vo.getState())
                                && VendorProcessTypeEnum.VENDOR_UPDATE_LEVEL.getState().equals(vo.getProcessType())){
                            vo.setVendorState("等级修改待审");
                        }else if(VendorStateEnum.IN_APPROVAL.getState().equals(vo.getState())
                                && VendorProcessTypeEnum.VENDOR_MOVE_INOROUT_BLACK.getState().equals(vo.getProcessType())
                                && Integer.valueOf(0).equals(vo.getIsBlack()) ){
                            vo.setVendorState("移入黑名单待审");
                        }else if(VendorStateEnum.IN_APPROVAL.getState().equals(vo.getState())
                                && VendorProcessTypeEnum.VENDOR_MOVE_INOROUT_BLACK.getState().equals(vo.getProcessType())
                                && Integer.valueOf(1).equals(vo.getIsBlack()) ){
                            vo.setVendorState("移出黑名单待审");
                        }else if(VendorStateEnum.IN_APPROVAL.getState().equals(vo.getState())
                                && VendorProcessTypeEnum.VENDOR_REGISTER.getState().equals(vo.getProcessType())){
                            vo.setVendorState("注册供应商待审");
                        }
                        if (vo.getState() == 0) {
                            vo.setVendorStateTwo("保存");
                        } else if (vo.getState() == 2 ) {
                            vo.setVendorStateTwo("驳回");
                        }
                        // 处理企业分类
                        if (StringUtils.isNotBlank(vo.getEnterpriseType())) {
                            vo.setEnterpriseTypeText(vendorClassifyService.getVendorClassifyName(vo.getEnterpriseType()));
                        }
                        vo.setFirstCooperationCompanyName(remoteSystemService.getDeptNameLoop(vo.getFirstCooperationCompanyCode(),"null",SecurityConstants.INNER));
                        if (StringUtil.isEmpty(vo.getEnableStatus())) {
                            vo.setEnableStatus("0");
                        }
                        return vo;
                    })).collect(Collectors.toList());

            List<VendorManagementListVO> resultList = futureList.stream()
                    .map(future -> {
                        try {
                            return future.join();
                        } catch (Exception e) {
                            // 记录日志，或者返回一个默认值
                            log.error("Future execution failed", e);
                            return null; // 根据需求处理失败的 future
                        }
                    })
                    .collect(Collectors.toList());
            iPage.setRecords(resultList);
        }
        return new PageResult<>(iPage);
    }

    @Override
    public List<VendorManagementListDataVO> getListVendor(VendorManagementListQueryDataVO queryVO) {
        if (StringUtils.isNotEmpty(queryVO.getEnterpriseType())){
            List<Long> classifyIds = new ArrayList<>();
            //查询企业分类是否存在子集
            List<VendorClassify> classifys = vendorClassifyService.list(new LambdaQueryWrapper<VendorClassify>()
                    .eq(VendorClassify::getParentId, Long.valueOf(queryVO.getEnterpriseType())));
            if (!CollectionUtils.isEmpty(classifys)){
                classifyIds = classifys.stream().map(VendorClassify::getId).collect(Collectors.toList());
            } else {
                classifyIds.add(Long.valueOf(queryVO.getEnterpriseType()));
            }
            queryVO.setClassifyIds(classifyIds);
        }

        return baseMapper.selectVendorManagementList(queryVO);
    }

    @Override
    public VendorManagementDetailVO getVendorManagementDetail(Long id) {
        Vendor vendor  = super.getById(id);
        ValidateUtils.isNullException(vendor,"该供应商不存在");
        // 当处于审批中且审批类型不为注册时，查询变更表内容
        if(!vendor.getState().equals(VendorStateEnum.IN_APPROVAL.getState()) || null == vendor.getProcessType() || vendor.getProcessType().equals(VendorProcessTypeEnum.VENDOR_REGISTER.getState())){
            VendorVO vendorVO = BeanCopierUtil.copyBean(vendor,VendorVO.class);
            vendorVO.setEnterpriseTypeText(vendorClassifyService.getVendorClassifyName(vendorVO.getEnterpriseType()));

            VendorMainContactVO mainContactVO = vendorContactService.getMainContact(id);

            VendorCertificationListVO certificationList = vendorCertificationService.listCertification(id,mainContactVO.getId());

            VendorStateVO vendorState = BeanCopierUtil.copyBean(vendor,VendorStateVO.class);
            /* 递归拼接部门名称 */
            //vendorState.setFirstCooperationCompanyName(remoteSystemService.getDeptNameLoop(vendor.getFirstCooperationCompanyCode(),vendorState.getFirstCooperationCompanyName(),SecurityConstants.INNER));
            vendorState.setFirstCooperationCompanyName(remoteSystemService.getDeptNameLoop(vendor.getFirstCooperationCompanyCode(),"null",SecurityConstants.INNER));
            // 查询最新变更id
            Long changeId = vendorChangeService.getLastChangeId(id);
            if(null != changeId){
                vendorVO.setChangeId(changeId);
            }
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
                    .build();
        } else {
            return vendorChangeService.getVendorManagementDetail(id);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void updateVendorLevel(VendorLevelRequestVO requestVO) {
        Vendor vendor = super.getById(requestVO.getId());
        ValidateUtils.isNullException(vendor,"该供应商不存在");

        super.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getVendorClass,requestVO.getVendorClass())
                .set(Vendor::getVendorLevel,requestVO.getVendorLevel())
                .eq(Vendor::getId,requestVO.getId()));

        // 添加操作记录
        Long logId = vendorOperateLogService.addVendorOperateLog(requestVO.getId(), VendorOperateLogCodeEnum.UPDATE_VENDOR_LEVEL);

        attachmentService.addAttachment(requestVO.getAttachmentList(), AttachmentTypeEnum.UPDATE_VENDOR_LEVEL,logId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void updateBlackState(VendorBlackRequestVO requestVO) {
        Vendor vendor = super.getById(requestVO.getId());
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
        // todo 加校验

        super.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getIsBlack,requestVO.getBlackState())
                .set(Vendor::getBlackBeginDate,null)
                .set(Vendor::getBlackEndDate,null)
                .eq(Vendor::getId,requestVO.getId()));

        // 添加操作记录
        Long logId = vendorOperateLogService.addVendorOperateLog(requestVO.getId(), VendorOperateLogCodeEnum.REMOVE_FROM_BLACK);

        // 添加附件
        attachmentService.addAttachment(requestVO.getAttachmentList(),AttachmentTypeEnum.REMOVE_VENDOR_BLACK,logId);

        // todo 加入审批流
    }

    /**
     * 移入黑名单
     * @param requestVO
     */
    private void addVendorToBlack(VendorBlackRequestVO requestVO) {
        // todo 加校验
        // 黑名单
        super.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getIsBlack,requestVO.getBlackState())
                // 移入黑名单，设置限制时间
                .set(Vendor::getBlackBeginDate,requestVO.getBlackBeginDate())
                .set(Vendor::getBlackEndDate,requestVO.getBlackEndDate())
                .eq(Vendor::getId,requestVO.getId()));

        // 添加操作记录
        Long logId = vendorOperateLogService.addVendorOperateLog(requestVO.getId(), VendorOperateLogCodeEnum.ADD_TO_BLACK);

        // 添加附件
        attachmentService.addAttachment(requestVO.getAttachmentList(),AttachmentTypeEnum.ADD_VENDOR_BLACK,logId);

        // todo 加入审批流
    }

    @Override
    public VendorIndexInfoVO getBaseInfo() {
        long userId = SecurityUtils.getUserId();
        VendorContact vendorContact = vendorContactService.getVendorContactByLoginUser(userId);
        ValidateUtils.validateStatusNotEquals(VendorContactStateEnum.VALID::equalsState,vendorContact.getState(),"您当前已被禁用!!!");
        if(vendorContact.getVendorId() == null){
            throw new NotFoundException("请先完善供应商信息！");
        }
        Vendor vendor = super.getById(vendorContact.getVendorId());
        ValidateUtils.isNullException(vendor,"获取所属供应商信息失败");

        VendorIndexInfoVO vendorIndexInfoVO = new VendorIndexInfoVO();
        vendorIndexInfoVO.setEnterpriseName(vendor.getEnterpriseName());
        vendorIndexInfoVO.setContactName(vendorContact.getContactName());
        vendorIndexInfoVO.setApproveState(vendor.getState());
        vendorIndexInfoVO.setApproveMsg(vendor.getOperateComment()==null?"":vendor.getOperateComment());/* 审批信息 */
        vendorIndexInfoVO.setIsManager(vendorContact.getIsManager());
        vendorIndexInfoVO.setIsBlack(vendor.getIsBlack());
        vendorIndexInfoVO.setContactPhone(vendorContact.getContactPhone());

        // 判断是否可用
        boolean isAvailable = true;
        String message = "账号可用";

        if (VendorStateEnum.IN_APPROVAL.equalsState(vendor.getState())) {
            message = "供应商还处于审批中!";
            isAvailable = false;
        } else if (VendorStateEnum.REJECT.equalsState(vendor.getState())){
            message = "审批被拒绝，请联系管理员";
            isAvailable = false;
        } else if (VendorStateEnum.SAVE.equalsState(vendor.getState())){
            message = "注册审批已撤回";
            isAvailable = false;
        } else {
            if (vendor.getIsBlack() == 1) {
                message = "您已被拉入黑名单，请联系管理员";
                isAvailable = false;
            }
            if (!VendorContactStateEnum.VALID.equalsState(vendorContact.getState())) {
                message = "登录账号不可用，请联系管理员";
                isAvailable = false;
            }

            Long count = tbVendorEvaluateService.getIsAppeal(vendor.getId());
            if (count > 0) {
                message = "供应商存在低于60分评价,被列为不合格供应商!";
                isAvailable = false;
            }
        }

        vendorIndexInfoVO.setIsAvailable(isAvailable);
        vendorIndexInfoVO.setMessage(message);
        /* 加上detail参数给前端判断资质信息是否过期 */
        /*VendorChangeRequestVO vendorDetailVO = vendorChangeService.getVendorUpdateDetail(vendor.getId());
        vendorIndexInfoVO.setDetail(vendorDetailVO);*/
        VendorCertificationListVO vendorCertificationListVO = vendorCertificationService.listCertification(vendor.getId(), vendorContact.getId());
        vendorIndexInfoVO.setCertification(vendorCertificationListVO);
        return vendorIndexInfoVO;
    }

    @Override
    public List<VendorPerformanceListVO> listVendorPerformance(Long id) {
        Vendor vendor = super.getById(id);
        ValidateUtils.isNullException(vendor,"该供应商不存在");

        List<VendorPerformanceEvaluation> performanceEvaluations =  vendorPerformanceEvaluationService.getAndSyncVendorPerformanceList(id);
        if (CollectionUtil.isNotEmpty(performanceEvaluations)) {
            return performanceEvaluations.stream()
                    .map(VendorPerformanceListVO::new)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public PageResult<VendorVO> listVendorJkptht(VendorManagementListQueryVO queryVO) {
        IPage<VendorVO> page = baseMapper.selectVendorJkpthtListPage(queryVO.toMybatisPage(),queryVO);
        return new PageResult<>(page);
    }

    @Override
    public String checkEnterpriseName(String enterpriseName) {
        Vendor vendor = super.getOne(new LambdaQueryWrapper<Vendor>()
                .eq(Vendor::getEnterpriseName,enterpriseName));
        if (vendor != null) {
            VendorMainContactVO mainContact = vendorContactService.getMainContact(vendor.getId());
            return "企业名称已存在，请联系 " + mainContact.getContactName() + "-" + mainContact.getContactPhone();
        }
        return "成功";
    }

    @Override
    public String vendorSignAuth() {
        SysUser loginUser = systemUserService.getLoginUser();

        Vendor vendor = this.getByLoginUser(loginUser.getUserId());
        ValidateUtils.isNullException(vendor,"您所属的供应商不存在");
        ValidateUtils.validateStatusNotEquals(VendorStateEnum.APPROVE::equalsState,vendor.getState(),"您当前状态不允许执行签章操作");

        VendorContact vendorContact = vendorContactService.getVendorContactByLoginUser(loginUser.getUserId());
        if (!SignStateEnum.isCompleteSign(vendorContact.getSignState())) {
            throw new BusinessException("您还没有完成个人认证授权，请先完成个人认证授权！");
        }

        CompanyAuthCommandRequest commandRequest = new CompanyAuthCommandRequestBuilder()
                    .builder("tb_vendor",vendor.getId(),vendorContact.getContactName(),vendorContact.getContactPhone(),
                                vendor.getEnterpriseName(),vendor.getSocialCreditCode())
                    .legalPersonName(vendor.getLegalRepresentative())
                    .legalPersonPhone(vendor.getLegalPhone())
                    .companyId(vendor.getId())
                    .build();
        SignatureResponse response = SignatureCommandFactory.getInstance().executeSignCommand(new CompanyAuthCommand(commandRequest),null);
        if (response.isSuccess()) {
            CompanyauthH5pageResponse pageResponse = (CompanyauthH5pageResponse) response.getResponseResult();
            return pageResponse.getAuthurl();
        }
        throw new BusinessException(response.getMessage());
    }

    @Override
    public String checkRegister(Long vendorId) {
        String flag ="true";
        Vendor vendor = super.getById(vendorId);
        if(vendor.getState()!=null&&(vendor.getState()==VendorStateEnum.REJECT.getState()||vendor.getState()==(VendorStateEnum.IN_APPROVAL.getState()))){
            flag ="false";
        }
        return flag;
    }

    @Override
    public VendorSignAuthInfo getVendorSignAuthInfo() {
        VendorContact vendorContact = vendorContactService.getVendorContactByLoginUser(SecurityUtils.getUserId());
        if (vendorContact.getIsManager() == 1) {
            if (SignStateEnum.TO_SIGN.equalsState(vendorContact.getSignState())) {
                return new VendorSignAuthInfo(0,"请完成个人签章认证授权");
            }

            // 是管理员，需要校验是否完成电子签章
            if (!SignStateEnum.SIGN_SUCCESS.equalsState(vendorContact.getSignState())) {
                // 需要进行个人认证
                return new VendorSignAuthInfo(1,vendorContact.getSignAuthFailReason());
            }

            // 完成个人签章，校验企业是否验证
            Vendor vendor = this.getById(vendorContact.getVendorId());
            if (SignStateEnum.TO_SIGN.equalsState(vendor.getSignState())) {
                return new VendorSignAuthInfo(2,"请完成企业签章认证授权");
            }

            if (!SignStateEnum.SIGN_SUCCESS.equalsState(vendor.getSignState())) {
                return new VendorSignAuthInfo(3,vendor.getSignAuthFailReason());
            }

            return new VendorSignAuthInfo(4,null);
        }

        return new VendorSignAuthInfo();
    }


    /**
     * 供应商信息校验
     * @param vendor
     */
    private void checkVendorInfo(Vendor vendor) {
        if(NumberUtil.isNotNullAndZero(SecurityUtils.getUserId())) {
            throw new BusinessException("无权注册供应商信息");
        }

        Vendor checkVendor = super.getOne(new LambdaQueryWrapper<Vendor>()
                .eq(Vendor::getSocialCreditCode,vendor.getSocialCreditCode()));
        if (checkVendor != null && !checkVendor.getId().equals(vendor.getId())) {
            throw new ParamValidateException("该统一社会信用代码已存在");
        }

        checkVendor = super.getOne(new LambdaQueryWrapper<Vendor>()
                .eq(Vendor::getEnterpriseName,vendor.getEnterpriseName()));
        if (checkVendor != null && !checkVendor.getId().equals(vendor.getId())) {
            throw new ParamValidateException("该企业名称已存在");
        }
    }

    @Override
    public void processStart(Map<String, Object> variables) {
        String processId = variables.get("processId").toString();
        String businessId = variables.get("businessId").toString();
        Object flagObj = variables.get("completedFlag");
        Integer vendorState =  VendorStateEnum.IN_APPROVAL.getState();
        if (!ObjectUtils.isEmpty(flagObj) && ProcessStateEnum.COMPLETED.getDesc().equals(flagObj.toString())) {
            vendorState = VendorStateEnum.APPROVE.getState();
        }
        super.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getWfProcessId,processId)
                .set(Vendor::getState,vendorState)
                .set(Vendor::getProcessType,VendorProcessTypeEnum.VENDOR_REGISTER.getState())
                .eq(Vendor::getId,businessId));

    }

    /**
     * 审批通过
     * @param variables
     */
    @Override
    public void processAuditPass(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getState, VendorStateEnum.APPROVE.getState())
                .set(Vendor::getRegisterApprovalTime, new Date())
                .eq(Vendor::getId, businessId));
        //推送供应商信息
        pushVendor(Long.parseLong(businessId),"add",null);

    }

    @Override
    public void pushMarketVendor(Long id){
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            VendorManagementDetailVO bean = this.getVendorManagementDetail(id);
            if(bean != null && bean.getVendor() != null){
                MarketSupplierRequestDTO requestDTO = new MarketSupplierRequestDTO();
                requestDTO.setName(bean.getVendor().getEnterpriseName());
                requestDTO.setUnifiedSocialCreditCode(bean.getVendor().getSocialCreditCode());
                requestDTO.setProvinceName(bean.getVendor().getEnterpriseProvinceName());
                requestDTO.setCityName(bean.getVendor().getEnterpriseCityName());
                //requestDTO.setdistrictName
                requestDTO.setAddress(bean.getVendor().getEnterpriseAddress());
                requestDTO.setBusinessScope(bean.getVendor().getBusinessScope());
                if(bean.getMainContact() != null){
                    requestDTO.setContactName(bean.getMainContact().getContactName());
                    requestDTO.setContactPhone(bean.getMainContact().getContactPhone());
                }
                if(bean.getCertificationList().getBusinessLicense() != null){
                    requestDTO.setBusinessLicense(bean.getCertificationList().getBusinessLicense().getAttachmentFileUrl());
                }
//                UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.MARKET_SUPPLIER_PUSH,
//                        MarketSupplierRequestDTO.class, requestDTO);
            }
        });
        executor.shutdown();
    }

    @Override
    public void pushVendor(Long id,String type, Integer isBlack){
            Vendor bean = this.getById(id);
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
                if(isBlack != null && isBlack == 1){
                    map.put("cust_mercht_status",  "黑名单");
                    map.put("cust_mercht_status_cd",  "5");//客商状态
                }else{
                    map.put("cust_mercht_status",  "正常");
                    map.put("cust_mercht_status_cd",  "1");//客商状态
                }
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
                                super.update(new LambdaUpdateWrapper<Vendor>()
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
                    }else{
                        JSONObject objectOne = dataCenterUtil.postCommonInfo(jsonObject,dataMiddlePlatformConfig.getVendorUpdate(),Vendor.LOG_TYPE_MODIFY, SecurityUtils.getUsername(),null);
                        if (objectOne != null && objectOne.containsKey("code") && objectOne.getInteger("code") == 200) {
                            //成功的
                            JSONObject data = objectOne.getJSONObject("data");
                            if(data != null && data.containsKey("updated")) {
                                JSONArray added = data.getJSONArray("updated");
                                if (added != null && added.size() >0) {
                                    JSONObject obj = added.getJSONObject(0);
                                    String custMerchtId = obj.getString("cust_mercht_id");
                                    super.update(new LambdaUpdateWrapper<Vendor>()
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
                    }
                }else{
                    JSONObject objectOne =dataCenterUtil.postCommonInfo(jsonObject,dataMiddlePlatformConfig.getVendorUpdate(),Vendor.LOG_TYPE_MODIFY, SecurityUtils.getUsername(),null);
                    if (objectOne != null && objectOne.containsKey("code") && objectOne.getInteger("code") == 200) {
                        //成功的
                        JSONObject data = objectOne.getJSONObject("data");
                        if(data != null && data.containsKey("updated")) {
                            JSONArray added = data.getJSONArray("updated");
                            if (added != null && added.size() >0) {
                                JSONObject obj = added.getJSONObject(0);
                                String custMerchtId = obj.getString("cust_mercht_id");
                                super.update(new LambdaUpdateWrapper<Vendor>()
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
                }
            }
    }

    /**
     * 审批驳回
     * @param variables
     */
    @Override
    public void processAuditReject(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getState,VendorStateEnum.REJECT.getState())
                .set(Vendor::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                .eq(Vendor::getId, businessId));
    }


    @Override
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        Vendor vendor = getById(requestDTO.getBusinessId());
        VendorChange vendorChange = vendorChangeService.getOne(new LambdaQueryWrapper<VendorChange>()
                .eq(VendorChange::getId,requestDTO.getBusinessId()));
        String org,orgThree;
        if(vendorChange!=null){
            requestDTO.setBusinessId(vendorChange.getId()+"");
            requestDTO.setProcessId(vendorChange.getWfProcessId());
            /* 根据组织获取对应的二级单位 */
            org = underlingSystemService.getL2OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
            /* 获取三级单位 */
            orgThree = underlingSystemService.getL3OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        }else{
            requestDTO.setBusinessId(vendor.getId()+"");
            requestDTO.setProcessId(vendor.getWfProcessId());
            /* 根据组织获取对应的二级单位 */
            org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
            /* 获取三级单位 */
            orgThree = underlingSystemService.getL3OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        }
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
        Vendor vendor = getById(requestDTO.getBusinessId());
        VendorChange vendorChange = vendorChangeService.getOne(new LambdaQueryWrapper<VendorChange>()
                .eq(VendorChange::getId,requestDTO.getBusinessId()));
        String org,orgThree;
        if(vendorChange!=null){
            requestDTO.setBusinessId(vendorChange.getId()+"");
            requestDTO.setProcessId(vendorChange.getWfProcessId());
            /* 根据组织获取对应的二级单位 */
            org = underlingSystemService.getL2OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
            /* 获取三级单位 */
            orgThree = underlingSystemService.getL3OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        }else{
            requestDTO.setBusinessId(vendor.getId()+"");
            requestDTO.setProcessId(vendor.getWfProcessId());
            /* 根据组织获取对应的二级单位 */
            org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
            /* 获取三级单位 */
            orgThree = underlingSystemService.getL3OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        }
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
        Vendor vendor = getById((Serializable) variables.get("businessId"));
        VendorChange vendorChange = vendorChangeService.getOne(new LambdaQueryWrapper<VendorChange>()
                .eq(VendorChange::getId,variables.get("businessId")));
        String org,orgThree;
        if(vendorChange!=null){
            variables.put("businessId",vendorChange.getId()+"");
            variables.put("processId",vendorChange.getWfProcessId());
            /* 根据组织获取对应的二级单位 */
            org = underlingSystemService.getL2OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
            /* 获取三级单位 */
            orgThree = underlingSystemService.getL3OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        }else{
            variables.put("businessId",vendor.getId()+"");
            variables.put("processId",vendor.getWfProcessId());
            /* 根据组织获取对应的二级单位 */
            org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
            /* 获取三级单位 */
            orgThree = underlingSystemService.getL3OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        }
        if(orgThree==null)orgThree = org;
        /* 流程角色配置规则传参 */
        variables.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
        variables.put("companyId", org);/* 公司 二级单位 */
        variables.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        variables.put("parentProjectCode", org);/* 父项目编码(项目部) */

        return processService.auditProcessInstance(processKey,variables);
    }

    @Override
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        Vendor vendor = getById(requestDTO.getBusinessId());
        VendorChange vendorChange = vendorChangeService.getOne(new LambdaQueryWrapper<VendorChange>()
                .eq(VendorChange::getId,requestDTO.getBusinessId()));
        String org,orgThree;
        if(vendorChange!=null){
            requestDTO.setBusinessId(vendorChange.getId()+"");
            requestDTO.setProcessId(vendorChange.getWfProcessId());
            /* 根据组织获取对应的二级单位 */
            org = underlingSystemService.getL2OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
            /* 获取三级单位 */
            orgThree = underlingSystemService.getL3OrgByOrgId(vendorChange.getFirstCooperationCompanyCode());
        }else{
            requestDTO.setBusinessId(vendor.getId()+"");
            requestDTO.setProcessId(vendor.getWfProcessId());
            /* 根据组织获取对应的二级单位 */
            org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
            /* 获取三级单位 */
            orgThree = underlingSystemService.getL3OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        }
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


    /**
     * 审批驳回到提交人状态
     * @param variables
     */
    @Override
    public void processAuditFreedom(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getState,VendorStateEnum.REJECT.getState())
                .set(Vendor::getOperateComment,variables.get("operateComment")==null?"":variables.get("operateComment").toString())
                .eq(Vendor::getId, businessId));
    }

    @Override
    public void revokeVendor(Long id) {
        VendorDetailVO vendorDetail = this.getVendorDetail(id);
        VendorChangeRequestVO vendorUpdateDetail = vendorChangeService.getVendorUpdateDetail(id);
        if(VendorProcessTypeEnum.VENDOR_REGISTER.getState().equals(vendorDetail.getVendor().getProcessType())){
            ValidateUtils.validateStatusNotEquals(VendorStateEnum.IN_APPROVAL::equalsState,vendorDetail.getVendor().getState(),"该状态下的供应商不允许撤回");
            // 撤回流程
            Map<String,Object> paramMap = new HashMap<>();
            paramMap.put("businessId", vendorDetail.getVendor().getId());
            paramMap.put("processId", vendorDetail.getVendor().getWfProcessId());
            processService.revokeVendorProcess(ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.getIdentifying(),paramMap);
        }else{
            //VendorChangeRequestVO vendorUpdateDetail = vendorChangeService.getVendorUpdateDetail(id);
            //VendorManagementDetailVO vendorManagementDetail = vendorChangeService.getVendorManagementDetail(id);
            ValidateUtils.isNullException(vendorUpdateDetail,"该供应商不存在");
            ValidateUtils.isNullException(vendorUpdateDetail.getVendorChange(),"该供应商不存在修改");
            ValidateUtils.validateStatusNotEquals(VendorStateEnum.IN_APPROVAL::equalsState,vendorUpdateDetail.getVendorChange().getChangeStatus(),"该状态下的供应商不允许撤回");
            // 撤回流程
            Map<String,Object> paramMap = new HashMap<>();
            paramMap.put("businessId", vendorUpdateDetail.getVendorChange().getId());
            paramMap.put("processId", vendorUpdateDetail.getVendorChange().getWfProcessId());
            processService.revokeVendorProcess(ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATEINFO.getIdentifying(),paramMap);
        }

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void registerLinkman(VendorOneRequestVO requestVO) {
        Boolean flag =true;
        if(flag){
            //查询是否有联系人和对应的登录用户,有联系人数据并且绑定了userid不让注册，没有联系人绑定userid但有用户账号则添加联系人
            // 主要联系人
            VendorContact contact = requestVO.getVendorContact();
            List<String> stateList = new ArrayList<>();
            stateList.add("0");
            stateList.add("1");
            List<VendorContact> contactList = vendorContactService.list(new LambdaUpdateWrapper<VendorContact>().eq(VendorContact::getContactPhone, contact.getContactPhone())
                    .isNotNull(VendorContact::getLoginUserId)
                    .eq(VendorContact::getDelFlag, "0")
                    .in(VendorContact::getState, stateList));
            if(contactList != null && contactList.size()>0){
                throw new NotFoundException("已存在手机号为"+contact.getContactPhone()+"的用户");
            }else{
                contact.setIsManager(0);
            }
            // 新增供应商账号
            Long longUserId = vendorContactService.addLoginUser(contact.getContactPhone(),contact.getContactName(),contact.getNewPassword());
            contact.setLoginUserId(longUserId);
            contact.setIsMainContact(1);
            contact.setState(VendorContactStateEnum.VALID.getState());
            contact.setIsLegal(1);
            vendorContactService.saveOrUpdate(contact);
        }


    }

    /**
     * 审批测回
     * @param variables
     */
    @Override
    public void processAuditRevoke(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        System.out.println(businessId+"供应商注册撤回");
        super.update(new LambdaUpdateWrapper<Vendor>()
                .set(Vendor::getState,VendorStateEnum.SAVE.getState())
                .eq(Vendor::getId, businessId));
    }

    @Override
    public String checkEnterpriseNameAndId(String enterpriseName, Long vendorId) {
        Vendor vendor = super.getOne(new LambdaQueryWrapper<Vendor>()
                .eq(Vendor::getEnterpriseName,enterpriseName));
        if (vendor != null && !vendor.getId().equals(vendorId)) {
            VendorMainContactVO mainContact = vendorContactService.getMainContact(vendor.getId());
            return "企业名称已存在，请联系 " + mainContact.getContactName() + "-" + mainContact.getContactPhone();
        }
        return "成功";
    }

    @Override
    public void initializeCode() {
        List<Vendor> list = super.list(new LambdaQueryWrapper<Vendor>()
                .eq(Vendor::getState, VendorStateEnum.APPROVE.getState())
                .eq(Vendor::getMiddleVendorCode,"1")
                .eq(Vendor::getDelFlag,"0"));
        if(CollectionUtil.isNotEmpty(list)){
            list.stream().forEach(p->{
                this.pushVendor(p.getId(),Vendor.LOG_TYPE_ADD,p.getIsBlack());
            });
            //this.pushVendor(list.get(0).getId(),Vendor.LOG_TYPE_ADD,list.get(0).getIsBlack());
        }

    }

    @Override
    public boolean removeBlacklist() {
        boolean flat=true;
        try {
            List<Vendor> list = super.list(new LambdaQueryWrapper<Vendor>()
                    .eq(Vendor::getState, VendorStateEnum.APPROVE.getState())
                    .eq(Vendor::getIsBlack,Integer.valueOf("1"))
                    .le(Vendor::getBlackEndDate,new Date()));
            if(CollectionUtil.isNotEmpty(list)){
                list.stream().forEach(p->{
                    super.update(new LambdaUpdateWrapper<Vendor>()
                            .set(Vendor::getBlackBeginDate,null)
                            .set(Vendor::getBlackEndDate,null)
                            .set(Vendor::getIsBlack,Integer.valueOf("0"))
                            .eq(Vendor::getId, p.getId()));
                    this.pushVendor(p.getId(),Vendor.LOG_TYPE_MODIFY,Integer.valueOf("0"));
                    List<VendorChange> vendorChangeList = vendorChangeService.list(new LambdaQueryWrapper<VendorChange>()
                            .eq(VendorChange::getVendorId, p.getId())
                            .orderByDesc(VendorChange::getVersion));
                    if (!CollectionUtils.isEmpty(vendorChangeList)) {
                        VendorChange vendorChange = vendorChangeList.get(0);
                        vendorChange.setId(null);
                        vendorChange.setVersion(vendorChange.getVersion()+1);
                        vendorChange.setIsBlack(Integer.valueOf("0"));
                        vendorChange.setBlackBeginDate(null);
                        vendorChange.setBlackEndDate(null);
                        vendorChangeService.save(vendorChange);
                        // 供应商资质
                        List<VendorCertificationChange> certificationList = certificationChangeService.createCertificationChange(vendorChange.getVendorId(), vendorChange.getVersion());
                        // 供应商联系人
                        List<VendorContactChange> contactChangeList = contactChangeService.createContactChange(vendorChange.getVendorId(), vendorChange.getVersion());
                    }
                });
            }
        } catch (Exception ex) {
            flat=false;
            log.error("移除黑名单失败:{}", ex.getMessage());
            ex.printStackTrace();
        }
        return flat;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void updateEnableStatus(VendorBlackRequestVO requestVO) {
        Vendor vendor = getById(requestVO.getId());
        ValidateUtils.isNullException(vendor,"该供应商不存在");
        if (requestVO.getEnableStatus() == 1) {
            // 停用
            super.update(new LambdaUpdateWrapper<Vendor>()
                    .set(Vendor::getEnableStatus, 1)
                    .eq(Vendor::getId,requestVO.getId()));
        } else {
            // 启用
            super.update(new LambdaUpdateWrapper<Vendor>()
                    .set(Vendor::getEnableStatus, 0)
                    .eq(Vendor::getId,requestVO.getId()));
        }
    }
}
