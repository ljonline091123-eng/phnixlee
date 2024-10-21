package com.zhaocai.business.vendor.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.NotFoundException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.req.UserObj;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorCertification;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.business.vendor.mapper.VendorContactMapper;
import com.zhaocai.business.vendor.service.IVendorCertificationService;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorOperateLogService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.*;
import com.zhaocai.business.vendor.vo.res.VendorContactInfoVO;
import com.zhaocai.business.vendor.vo.res.VendorContactListVO;
import com.zhaocai.business.vendor.vo.res.VendorMainContactVO;
import com.zhaocai.business.vendor.vo.res.VendorVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.domain.R;
import com.zhaocai.common.core.enums.UserTypeEnum;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.domain.BaseEntity;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.command.PersonAuthCommandRequest;
import com.zhaocai.common.signature.dto.command.PersonAuthCommandRequestBuilder;
import com.zhaocai.common.signature.service.SignatureCommandFactory;
import com.zhaocai.common.signature.service.command.PersonAuthCommand;
import com.zhaocai.system.api.domain.BusinessUser;
import com.zhaocai.system.api.system.RemoteUserService;
import lombok.extern.slf4j.Slf4j;
import net.qiyuesuo.v3sdk.model.auth.response.UserauthAuthurl2Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 供应商联系人Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Slf4j
@Service
public class VendorContactServiceImpl extends ServiceImpl<VendorContactMapper,VendorContact> implements IVendorContactService {

    @Autowired
    private IVendorOperateLogService vendorOperateLogService;

    @Autowired
    private IVendorCertificationService vendorCertificationService;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private IBPMProcessService processService;

    @Lazy
    @Autowired
    private IVendorService vendorService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Override
    public Long saveMainVendorContact(VendorContact contact) {
        //为主要联系人
        contact.setIsMainContact(1);
        contact.setState(VendorContactStateEnum.VALID.getState());

        // 校验联系人
        checkVendorContact(contact);

        // 只有一个主要联系人
        VendorContact mainContact = this.getOne(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getVendorId,contact.getVendorId())
                .eq(VendorContact::getIsMainContact,1));
        if (mainContact != null) {
            contact.setId(mainContact.getId());
        }

        super.saveOrUpdate(contact);
        return contact.getId();
    }

    @Override
    public List<VendorContact> listContactByVendorId(Long vendorId) {
        return super.list(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getVendorId,vendorId));
    }

    @Override
    public List<VendorContactListVO> listVendorContact(Long vendorId) {
        VendorContactListQueryVO queryVO = new VendorContactListQueryVO();
        queryVO.setVendorId(vendorId);

       return baseMapper.selectVendorContactListPage(queryVO.toMybatisPage(),queryVO).getRecords();
    }

    @Override
    public VendorMainContactVO getMainContact(Long vendorId) {
        VendorContact vendorContact = this.getOne(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getVendorId,vendorId)
                .eq(VendorContact::getIsMainContact,1));
        return BeanCopierUtil.copyBean(vendorContact,VendorMainContactVO.class);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void addVendorContact(VendorContactAddRequestVO requestVO,Long vendorId) {
        VendorContact checkContact = super.getOne(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getContactPhone,requestVO.getContactPhone()));
        ValidateUtils.isNotNullException(checkContact,"该联系人手机号码已存在，请勿重新添加");

        VendorContact contact = BeanCopierUtil.copyBean(requestVO,VendorContact.class);
        contact.setState(VendorContactStateEnum.IN_APPROVAL.getState());
        contact.setVendorId(vendorId);
        // 不是主要联系人
        contact.setIsMainContact(0);
        // 校验
        checkVendorContact(contact);

        // 保存联系人
        super.save(contact);

        // 保存授权书
        Long certificationId = vendorCertificationService.addCertification(vendorId,requestVO.getAttachment(), CertificationTypeEnum.LEGAL_AUTHORIZATION,contact.getId());

        // 增加授权 id
        super.update(new LambdaUpdateWrapper<VendorContact>()
                .set(VendorContact::getCertificationId,certificationId)
                .eq(VendorContact::getId,contact.getId()));

        // 提交新增联系人流程
        this.submitProcess(contact.getId(), requestVO.getOperateComment());
    }

    @Override
    public void updateContactState(Long id, Integer state) {
        VendorContact vendorContact = super.getById(id);
        ValidateUtils.isNullException(vendorContact,"该联系人不存在");

        super.update(new LambdaUpdateWrapper<VendorContact>()
                .set(VendorContact::getState,state)
                .eq(VendorContact::getId,id));

        if (state == 1) {
            vendorOperateLogService.addVendorOperateLog(vendorContact.getVendorId(), VendorOperateLogCodeEnum.ENABLE_VENDOR_CONTACT);
        } else {
            vendorOperateLogService.addVendorOperateLog(vendorContact.getVendorId(), VendorOperateLogCodeEnum.DISABLE_VENDOR_CONTACT);
        }
    }

    @Override
    public PageResult<VendorContactListVO> listContactManagementList(VendorContactListQueryVO queryVO) {
        IPage<VendorContactListVO> iPage = baseMapper.selectVendorContactListPage(queryVO.toMybatisPage(),queryVO);
        return new PageResult<>(iPage);
    }

    @Override
    public void setContractLoginUser(Long loginUserId, Long id) {
        VendorContact checkContact = super.getOne(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getVendorId,loginUserId));
        ValidateUtils.isNotNullException(checkContact,"生成的登录用户已存在，请确认");

        super.update(new LambdaUpdateWrapper<VendorContact>()
                .set(VendorContact::getLoginUserId,loginUserId)
                .eq(VendorContact::getId,id));
    }

    @Override
    public VendorContact getVendorContactByLoginUser(Long userId) {
        List<VendorContact> list = super.list(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getLoginUserId,userId));
        if (CollectionUtil.isEmpty(list)) {
            log.error("loginUserId[{}]对应的企业联系人不存在..",userId);
            throw new NotFoundException("获取当前用户失败");
        }
        if (list.size() > 1) {
            throw new BusinessException("该登录用户对应了多个联系人，请确认");
        }

        return list.get(0);
    }

    @Override
    public AttachmentVO getAuthorization(Long id) {
        VendorContact vendorContact = super.getById(id);
        ValidateUtils.isNullException(vendorContact,"该联系人不存在，请确认");

        VendorCertification vendorCertification = vendorCertificationService.getById(vendorContact.getCertificationId());

        return Optional.ofNullable(vendorCertification)
                .map(x -> new AttachmentVO(x.getAttachmentFileName(),x.getAttachmentFileUrl()))
                .orElse(null);
    }

    @Override
    public void updateAuthorizationFile(UpdateAuthorizationFileRequestVO requestVO) {
        VendorContact vendorContact = super.getById(requestVO.getId());
        ValidateUtils.isNullException(vendorContact,"该联系人不存在，请确认");
        if (NumberUtil.isNullOrZero(vendorContact.getCertificationId())) {
            // 不存在授权书，则新增
            AttachmentRequestVO attachmentRequest = new AttachmentRequestVO(requestVO.getFileName(), requestVO.getFileUrl());
            Long certificationId = vendorCertificationService.addCertification(vendorContact.getVendorId(),attachmentRequest,CertificationTypeEnum.LEGAL_AUTHORIZATION,vendorContact.getId());
            super.update(new LambdaUpdateWrapper<VendorContact>()
                    .set(VendorContact::getCertificationId,certificationId)
                    .eq(VendorContact::getId,vendorContact.getId()));
        } else {
            VendorCertification vendorCertification = vendorCertificationService.getById(vendorContact.getCertificationId());
            vendorCertificationService.update(new LambdaUpdateWrapper<VendorCertification>()
                    .set(VendorCertification::getAttachmentFileName,requestVO.getFileName())
                    .set(VendorCertification::getAttachmentFileUrl,requestVO.getFileUrl())
                    .eq(VendorCertification::getId,vendorCertification.getId()));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void updateAuthorizationDate(UpdateAuthorizationDateRequestVO requestVO) {
        VendorContact vendorContact = super.getById(requestVO.getId());
        ValidateUtils.isNullException(vendorContact,"该联系人不存在，请确认");
        if (NumberUtil.isNullOrZero(vendorContact.getCertificationId())) {
            throw new ParamValidateException("该联系人的法人授权书不存在，请先上传");
        }

        VendorCertification vendorCertification = vendorCertificationService.getById(vendorContact.getCertificationId());
        vendorCertificationService.update(new LambdaUpdateWrapper<VendorCertification>()
                .set(VendorCertification::getEffectiveBeginDate,requestVO.getBeginDate())
                .set(VendorCertification::getEffectiveEndDate,requestVO.getEndDate())
                .eq(VendorCertification::getId,vendorCertification.getId()));
    }

    @Override
    public Long addLoginUser(String contactPhone, String contactName) {
        BusinessUser businessUser = new BusinessUser();
        businessUser.setUserName(contactPhone);
        businessUser.setNickName(contactName);
        businessUser.setUserType(UserTypeEnum.VENDOR);
        R<Long> r = remoteUserService.addBusinessUser(businessUser, SecurityConstants.INNER);

        if (R.SUCCESS != r.getCode()) {
            throw new BusinessException(r.getMsg());
        }

        return r.getData();
    }

    @Override
    public void updateContactManager(UpdateContactManagerRequestVO requestVO) {
        VendorContact vendorContact = super.getById(requestVO.getContactId());
        ValidateUtils.isNullException(vendorContact,"该联系人不存在，请确认");

        if (requestVO.getIsManager() == 1) {
            VendorContact checkContact = super.getOne(new LambdaQueryWrapper<VendorContact>()
                    .eq(VendorContact::getVendorId,vendorContact.getVendorId())
                    .eq(VendorContact::getIsManager,1));
            if (checkContact != null) {
                throw new BusinessException("供应商已经存在了一个管理员，请勿再添加");
            }
        }

        super.update(new LambdaUpdateWrapper<VendorContact>()
                .set(requestVO.getIsManager() == 1,VendorContact::getIsManager,1)
                .set(requestVO.getIsManager() != 1,VendorContact::getIsManager,0)
                .eq(VendorContact::getId,requestVO.getContactId()));
    }

    /**
     * 修改供应商联系人
     * @param requestVO
     * @param vendorId
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void updateVendorContact(VendorContactSaveRequestVo requestVO, Long vendorId) {
        VendorContact checkContact = super.getOne(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getContactPhone,requestVO.getContactPhone())
                .ne(BaseEntity::getId,requestVO.getId()));
        ValidateUtils.isNotNullException(checkContact,"该联系人手机号码已存在，请勿重新添加");

        VendorContact contact = BeanCopierUtil.copyBean(requestVO,VendorContact.class);
        contact.setState(VendorContactStateEnum.IN_APPROVAL.getState());
        contact.setVendorId(vendorId);
        // 不是主要联系人
        contact.setIsMainContact(0);
        // 校验
        checkVendorContact(contact);
        // 保存联系人
        super.updateById(contact);
        // 修改授权书
        UpdateAuthorizationFileRequestVO updateAuthorizationFile = BeanCopierUtil.copyBean(requestVO.getAttachment(),UpdateAuthorizationFileRequestVO.class);
        updateAuthorizationFile.setId(contact.getId());
        this.updateAuthorizationFile(updateAuthorizationFile);

        // 提交新增联系人流程
        this.submitProcess(contact.getId(), requestVO.getOperateComment());
    }

    /**
     * 提交新增联系人流程
     * @param id 联系人id
     */
    private void submitProcess(Long id,String operateComment) {
        VendorContact contact = super.getById(id);
        Vendor vendor = vendorService.getById(contact.getVendorId());
        //接入底层逻辑平台流程
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("businessId", id);
        paramMap.put("businessTitle", "供应商-新增联系人审批");
        paramMap.put("businessContent", String.format("您有供应商：%s新增联系人在审核节点的审批！", vendor.getEnterpriseName()));
        paramMap.put("detailUrl", "/vendor/vendor-account?id="+Base64.encode(id+""));
        String org = underlingSystemService.getL2OrgByOrgId(vendor.getFirstCooperationCompanyCode());
        //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
        String customProcessKey = ProcessKeyEnum.ZHAOCAI_VENDOR_ADDCONTACT.getIdentifying().replace("{org}",org);
        UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_VENDOR_ADDCONTACT.name()).
                businessId(vendor.getId().toString())
                .toDoType(ToDoTypeEnum.EXAMINE.name()).build();
        paramMap.put("userObj", JSON.toJSONString(userObj));
        paramMap.put("customProcessKey", customProcessKey);
        paramMap.put("operateComment", operateComment);
        processService.startProcessInstance(
                ProcessKeyEnum.ZHAOCAI_VENDOR_ADDCONTACT.getIdentifying(),paramMap);
    }

    /**
     * 获取供应商联系人详情
     * @param id
     * @return
     */
    @Override
    public VendorContactSaveRequestVo getVendorContactDetail(Long id) {
        VendorContact vendorContact = this.getOne(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getId,id));
        VendorContactSaveRequestVo contact = BeanCopierUtil.copyBean(vendorContact, VendorContactSaveRequestVo.class);
        AttachmentVO certification = this.getAuthorization(id);
        contact.setAttachment(certification);
        return contact;
    }

    @Override
    public String vendorContactSignAuth() {
        VendorContact vendorContact = getVendorContactByLoginUser(SecurityUtils.getUserId());
        ValidateUtils.isNullException(vendorContact,"联系人不存在，请确认后操作");
        if (!SignStateEnum.isCompleteSign(vendorContact.getState())) {
            throw new BusinessException("您以完成个人签证授权，无需再次操作");
        }

        PersonAuthCommandRequest personAuthCommandRequest = new PersonAuthCommandRequestBuilder("tb_vendor_contact",vendorContact.getId(),
                vendorContact.getContactName(),vendorContact.getContactPhone())
                .userId(vendorContact.getId().toString())
                .build();

        SignatureResponse response = SignatureCommandFactory.getInstance().executeSignCommand(new PersonAuthCommand(personAuthCommandRequest),null);

        if (response.isSuccess()) {
            UserauthAuthurl2Response authUrl2Response = (UserauthAuthurl2Response) response.getResponseResult();
            return authUrl2Response.getResult();
        } else if ("2001008".equals(response.getCode())){
            log.warn("企业个人认证 - {},用户已在签章平台完成签约",vendorContact.getLoginUserId());
            this.update(new LambdaUpdateWrapper<VendorContact>()
                    .set(VendorContact::getSignState,SignStateEnum.SIGN_SUCCESS.getState())
                    .eq(VendorContact::getId,vendorContact.getId()));
            throw new BusinessException("您已在签章平台完成认证授权，请勿重新操作");
        }
        throw new BusinessException(response.getMessage());
    }

    @Override
    public VendorContact getVendorManager(Long vendorId) {
        return getOne(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getVendorId,vendorId)
                .eq(VendorContact::getIsManager,1));
    }

    @Override
    public VendorContactInfoVO getInfo(Long id) {
        VendorContact vendorContact = getOne(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getId,id));
        if(vendorContact!=null){
            VendorContactInfoVO vo = BeanCopierUtil.copyBean(vendorContact, VendorContactInfoVO.class);
            Vendor vendor = vendorService.getById(vendorContact.getVendorId());
            VendorVO vendorVo = BeanCopierUtil.copyBean(vendor, VendorVO.class);
            if(vendor!=null){
                vendorVo.setCreateTime(vendor.getCreateTime());
                vo.setVendorVO(vendorVo);
            }
            return vo;
        }
        throw new BusinessException("该id经查询无供应商联系人数据");
    }

    /**
     * 校验联系人
     * @param contact
     */
    private void checkVendorContact(VendorContact contact) {
        VendorContact checkContact = super.getOne(new LambdaQueryWrapper<VendorContact>()
                .eq(VendorContact::getContactPhone,contact.getContactPhone()));

        if (checkContact != null && !checkContact.getId().equals(contact.getId())) {
            throw new ParamValidateException("该联系人的联系电话已存在，请勿重新输入");
        }

        long totalCount = super.count(new LambdaQueryWrapper<VendorContact>()
                        .eq(VendorContact::getVendorId,contact.getVendorId()));
        if (totalCount > 5 && null == contact.getId()) {
            throw new ParamValidateException("联系人已满5人，不能再添加了");
        }

        if (contact.getIsManager() == 1) {
            // 如果设置成了管理员，校验
            checkContact = super.getOne(new LambdaQueryWrapper<VendorContact>()
                    .eq(VendorContact::getVendorId,contact.getVendorId())
                    .eq(VendorContact::getIsManager,1));
            if (checkContact != null && !checkContact.getId().equals(contact.getId())) {
                throw new BusinessException("供应商已经存在了一个管理员，请勿再添加");
            }
        }
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
        Integer state = VendorContactStateEnum.IN_APPROVAL.getState();
        if (!ObjectUtils.isEmpty(flagObj) && ProcessStateEnum.COMPLETED.getDesc().equals(flagObj.toString())) {
            state = VendorContactStateEnum.VALID.getState();
            // 审批通过
            VendorContact contact = super.getById(businessId);
            this.handleApprove(contact);
        }
        super.update(new LambdaUpdateWrapper<VendorContact>()
                .set(VendorContact::getWfProcessId,processId)
                .set(VendorContact::getState,state)
                .eq(VendorContact::getId,businessId));
    }

    /**
     * 处理审批通过后信息更新
     * @param contact
     */
    private void handleApprove(VendorContact contact) {
        // 增加新增账号
        Long loginUserId=  this.addLoginUser(contact.getContactPhone(),contact.getContactName());
        contact.setLoginUserId(loginUserId);
        super.updateById(contact);
    }

    @Override
    public void processAuditPass(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<VendorContact>()
                .set(VendorContact::getState,VendorContactStateEnum.VALID.getState())
                .eq(VendorContact::getId, businessId));
        // 审批通过
        VendorContact contact = super.getById(businessId);
        this.handleApprove(contact);
    }

    @Override
    public void processAuditReject(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<VendorContact>()
                .set(VendorContact::getState,VendorContactStateEnum.APPROVAL_REJECTION.getState())
                .eq(VendorContact::getId, businessId));
    }
}
