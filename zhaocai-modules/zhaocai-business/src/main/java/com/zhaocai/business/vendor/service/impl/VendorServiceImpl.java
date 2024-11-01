package com.zhaocai.business.vendor.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.req.UserObj;
import com.zhaocai.business.manager.http.dto.res.ListCataLogDTO;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ISystemUserService;
import com.zhaocai.business.vendor.domain.*;
import com.zhaocai.business.vendor.mapper.VendorMapper;
import com.zhaocai.business.vendor.service.*;
import com.zhaocai.business.vendor.vo.req.*;
import com.zhaocai.business.vendor.vo.res.*;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.CompanyAuthCommandRequest;
import com.zhaocai.common.signature.dto.command.CompanyAuthCommandRequestBuilder;
import com.zhaocai.common.signature.service.SignatureCommandFactory;
import com.zhaocai.common.signature.service.command.CompanyAuthCommand;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteSystemService;
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

import java.util.*;
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



    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void vendorRegister(VendorRegisterRequestVO requestVO) {
        checkVendorInfo(requestVO.getVendor());

        // 保存基本信息
        Vendor vendor = requestVO.getVendor();
        vendor.setState(VendorStateEnum.APPROVE.getState());
        vendor.setSignState(SignStateEnum.TO_SIGN.getState());
        vendor.setVendorClass(1);
        vendor.setVendorLevel(1);
        super.save(vendor);

        // 保存供应商资质
        vendorCertificationService.addCertification(requestVO.getBusinessLicense(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
        vendorCertificationService.addCertification(requestVO.getIntegrity(), CertificationTypeEnum.INTEGRITY,vendor.getId());
        Long legalAuthorizationId = vendorCertificationService.addCertification(requestVO.getLegalAuthorization(),CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
        if (CollUtil.isNotEmpty(requestVO.getRelevantCertificationList())){
            vendorCertificationService.addCertification(requestVO.getRelevantCertificationList(), CertificationTypeEnum.RELEVANT_CERTIFICATION,vendor.getId());
        }

        // 主要联系人
        VendorContact contact = requestVO.getVendorContact();

        // 新增供应商账号
        Long longUserId = vendorContactService.addLoginUser(contact.getContactPhone(),contact.getContactName());
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
        checkVendorInfo(requestVO.getVendor());

        // 保存基本信息
        Vendor vendor = requestVO.getVendor();
        vendor.setState(VendorStateEnum.IN_APPROVAL.getState());
        vendor.setSignState(SignStateEnum.TO_SIGN.getState());
        vendor.setVendorClass(1);
        vendor.setVendorLevel(1);
        super.save(vendor);

        // 保存供应商资质
        vendorCertificationService.addCertification(requestVO.getBusinessLicense(), CertificationTypeEnum.BUSINESS_LICENSE,vendor.getId());
        vendorCertificationService.addCertification(requestVO.getIntegrity(), CertificationTypeEnum.INTEGRITY,vendor.getId());
        Long legalAuthorizationId = vendorCertificationService.addCertification(requestVO.getLegalAuthorization(),CertificationTypeEnum.LEGAL_AUTHORIZATION,vendor.getId());
        if (CollUtil.isNotEmpty(requestVO.getRelevantCertificationList())){
            vendorCertificationService.addCertification(requestVO.getRelevantCertificationList(), CertificationTypeEnum.RELEVANT_CERTIFICATION,vendor.getId());
        }

        // 主要联系人
        VendorContact contact = requestVO.getVendorContact();

        // 新增供应商账号
        Long longUserId = vendorContactService.addLoginUser(contact.getContactPhone(),contact.getContactName());
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

        //接入底层逻辑平台流程
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("businessId", vendor.getId());
        paramMap.put("businessTitle", "供应商注册审批");

        /* 获取二级单位 */
        String org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        /** 获取三级单位
         *
         *
         * */
        String orgThree = "获取三级单位"+vendor.getFirstCooperationCompanyCode();

        //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
        String customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.getIdentifying().replace("{org}",org);

        /* 获取所有流程 */
        List<ListCataLogDTO> listCataLogDTOS = underlingSystemService.listCatalog();
        if (listCataLogDTOS != null) {
            /* 判断二级单位流程是否存在 */
            ListCataLogDTO cataLogDTOTwo = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(org)).findFirst().orElse(null);
            if (cataLogDTOTwo != null) {
                /* 赋值使用二级单位 */
                customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.getIdentifying().replace("{org}",org);
            }

            /* 判断三级单位流程是否存在 */
            ListCataLogDTO cataLogDTOThree = listCataLogDTOS.stream().filter(cateLog -> cateLog.getCatalogKey().equals(orgThree)).findFirst().orElse(null);
            if (cataLogDTOThree != null) {
                /* 赋值使用二级单位 */
                customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.getIdentifying().replace("{org}",orgThree);
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

        /* 流程角色配置规则传参 */
        paramMap.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
        paramMap.put("companyId", org);/* 公司 二级单位 */
        paramMap.put("responsibilityDeptId", org);/* 责任单位 三级单位 */
        paramMap.put("parentProjectCode", null);/* 父项目编码(项目部) */

        processService.startProcessInstance(ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.getIdentifying(),paramMap);

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
        return Optional.of(vendor).orElseThrow(() -> new ParamValidateException("用户对应的供应商信息不存在"));
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
        for (VendorManagementListVO vo : iPage.getRecords()) {
            if (vo.getIsBlack() == 1) {
                vo.setVendorLibraryText("黑名单");
            } else if (vo.getState() == 1) {
                vo.setVendorLibraryText("待审供应商");
            } else if (vo.getVendorClass() == 1) {
                vo.setVendorLibraryText("合格供应商");
            } else if (vo.getVendorClass() == 2) {
                vo.setVendorLibraryText("战略供应商");
            }

            // 处理企业分类
            if (StringUtils.isNotBlank(vo.getEnterpriseType())) {
                vo.setEnterpriseTypeText(vendorClassifyService.getVendorClassifyName(vo.getEnterpriseType()));
            }
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

            // 查询最新变更id
            Long changeId = vendorChangeService.getLastChangeId(id);
            if(null != changeId){
                vendorVO.setChangeId(changeId);
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

        Vendor vendor = super.getById(vendorContact.getVendorId());
        ValidateUtils.isNullException(vendor,"获取所属供应商信息失败");

        VendorIndexInfoVO vendorIndexInfoVO = new VendorIndexInfoVO();
        vendorIndexInfoVO.setEnterpriseName(vendor.getEnterpriseName());
        vendorIndexInfoVO.setContactName(vendorContact.getContactName());
        vendorIndexInfoVO.setApproveState(vendor.getState());
        vendorIndexInfoVO.setApproveMsg(vendor.getOperateComment());/* 审批信息 */
        vendorIndexInfoVO.setIsManager(vendorContact.getIsManager());
        vendorIndexInfoVO.setIsBlack(vendor.getIsBlack());
        vendorIndexInfoVO.setContactPhone(vendorContact.getContactPhone());

        // 判断是否可用
        boolean isAvailable = true;
        String message = "账号可用";

        if (VendorStateEnum.IN_APPROVAL.equalsState(vendor.getState())) {
            message = "供应商还处于审批中，请稍后重试";
            isAvailable = false;
        } else if (VendorStateEnum.REJECT.equalsState(vendor.getState())){
            message = "审批被拒绝，请联系管理员";
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
        }

        vendorIndexInfoVO.setIsAvailable(isAvailable);
        vendorIndexInfoVO.setMessage(message);

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
                .set(Vendor::getState,VendorStateEnum.APPROVE.getState())
                .eq(Vendor::getId, businessId));
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
}
