package com.zhaocai.business.pub.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementSignStamper;
import com.zhaocai.business.agreement.service.IAgreementSignStamperService;
import com.zhaocai.business.agreement.vo.res.AgreementSignStamperVO;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.domain.Template;
import com.zhaocai.business.pub.mapper.TemplateMapper;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ITemplateService;
import com.zhaocai.business.pub.vo.req.TemplateListQueryVO;
import com.zhaocai.business.pub.vo.req.TemplateSaveRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.pub.vo.res.TemplateListVO;
import com.zhaocai.business.pub.vo.res.TemplateVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.text.Convert;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模板管理Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper,Template> implements ITemplateService {

    @Autowired
    private IAttachmentService attachmentService;

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private IAgreementSignStamperService agreementSignStamperService;


    @Override
    public PageResult<TemplateListVO> listPage(TemplateListQueryVO queryVO) {
        IPage<TemplateListVO> pages = baseMapper.selectList(queryVO.toMybatisPage(),queryVO);
        return new PageResult<>(pages);
    }

    @Override
    public PageResult<TemplateListVO> fanListPage(TemplateListQueryVO queryVO) {
        IPage<TemplateListVO> pages = fanList(queryVO,"1");
        return new PageResult<>(pages);
    }

    /**
     * @param queryVO
     * @param group 区分范本列表是否是二级单位
     * @return
     */
    @Nullable
    private IPage<TemplateListVO> fanList(TemplateListQueryVO queryVO,String group) {
        /* 获取当前登录人对应的第三方(主控)的部门id */
        String currUserTowLevelThridDeptId =  remoteSystemService.getTwoLevelDeptByDeptId
                (SecurityUtils.getSysUser().getDeptId(),SecurityConstants.INNER).getThridDeptId();
        /* 获取所有二级组织及集团 thrid_org_level IS NOT NULL */
        List<SysDept> sysDeptList = remoteSystemService.getTwoLevelDepts(SecurityConstants.INNER);
//        if (!currUserTowLevelThridDeptId.equals(UserConstants.GROUP_DEPT_ID)) {
//            /** 通用模板 = 部门层级是一级单位 部门同步方法{@link com.zhaocai.system.manager.controller.SyncPlatformDataController#syncDept} */
//            if (group.equals("2")) {
//                sysDeptList = sysDeptList.stream().filter(item -> item.getThridOrgLevel() == NumberConstant.ONE ||
//                        item.getThridDeptId().equals(currUserTowLevelThridDeptId)).collect(Collectors.toList());
//            }else {
//                sysDeptList = sysDeptList.stream().filter(item ->
//                        item.getThridDeptId().equals(currUserTowLevelThridDeptId)).collect(Collectors.toList());
//            }
//        }
        if (CollectionUtil.isNotEmpty(sysDeptList)) {
            List<String> deptIdList = sysDeptList.stream().map(dept -> dept.getDeptId()+"").collect(Collectors.toList());
            String id=String.join(",",deptIdList);
            String[] deptIds = Convert.toStrArray(id);
            queryVO.setUsingUnitNo(deptIds);
            IPage<TemplateListVO> pages = baseMapper.selectList(queryVO.toMybatisPage(),queryVO);
            procurementPlanTypeEnumNmae(pages);
            return pages;
        }
        return null;
    }

    /**
     * 返回类型名称
     * @param pages
     */
    private void procurementPlanTypeEnumNmae(IPage<TemplateListVO> pages) {
        for (TemplateListVO mode : pages.getRecords()) {
            for (ProcurementPlanTypeEnum value : ProcurementPlanTypeEnum.values()) {
                if (StringUtils.isNotEmpty(mode.getContractType())) {
                    if (mode.getContractType().equals(String.valueOf(value.getType()))) {
                        mode.setContractName(value.getDesc());
                    }
                }
            }
        }
    }

    /**
     * 采购方案选择招标文件模板切换
     * @param queryVO
     * @return
     */
    @Override
    public PageResult<TemplateListVO> switchListPage(TemplateListQueryVO queryVO) {
        IPage<TemplateListVO> pages =null;
        //类型判断
        if(StringUtils.isBlank(queryVO.getSwitchTemplateType())){
            throw new BusinessException("切换类型为空");
        }
        //类型为:1、通用模板 2、复用模板
        if(queryVO.getSwitchTemplateType().equals("1")){
            //deptId==1000000000作为 集团 判断依据 查他本身和二级单位
            pages=fanList(queryVO,"2");
           /* pages = getTemplateListVOIPage(pages,queryVO);*/
        }else {
            pages = baseMapper.multiplexList(queryVO.toMybatisPage(),queryVO);
        }


        return new PageResult<>(pages);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void saveTemplate(TemplateSaveRequestVO requestVO) {
        if (NumberUtil.isNullOrZero(requestVO.getId())) {
            addTemplate(requestVO);
        } else {
            updateTemplate(requestVO);
        }

        // 处理合同模板
//        if (requestVO.getTemplateType() == 1) {
//            // 合同模板，必须要有合同签章信息
//            if (CollectionUtil.isEmpty(requestVO.getAgreementSignStamperList())) {
//                throw new BusinessException("合同签章签署位置信息不能为空");
//            }
//            agreementSignStamperService.saveAgreementSignStamper(requestVO.getId(),requestVO.getAgreementSignStamperList());
//        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        super.removeById(id);

        agreementSignStamperService.deleteByTemplateId(id);
    }

    @Override
    public TemplateVO detail(Long id) {
        Template template = this.getById(id);
        ValidateUtils.isNullException(template,"该模板不存在");

        TemplateVO templateVO = BeanCopierUtil.copyBean(template,TemplateVO.class);

        // 获取附件
        Attachment attachment = attachmentService.getById(template.getAttachmentId());
        //合同类型
        for (ProcurementPlanTypeEnum value : ProcurementPlanTypeEnum.values()) {
            if (StringUtils.isNotEmpty(templateVO.getContractType())) {
                if (templateVO.getContractType().equals(String.valueOf(value.getType()))) {
                    templateVO.setContractName(value.getDesc());
                }
            }
        }

        if (template.getTemplateType() == 1) {
            List<AgreementSignStamper> signStampers = agreementSignStamperService.listByTemplateId(id);
            templateVO.setAgreementSignStamperList(BeanCopierUtil.copyList(signStampers, AgreementSignStamperVO.class));
        }

        templateVO.setFileName(attachment.getFileName());
        templateVO.setFileUrl(attachment.getFileUrl());
        templateVO.setAttachmentId(attachment.getId());
        return templateVO;
    }

    @Override
    public AttachmentVO getTemplateAttachmentInfo(Long templateId) {
        Template template = super.getById(templateId);
        ValidateUtils.isNullException(template,"该模板信息不存在");
        if (NumberUtil.isNullOrZero(template.getAttachmentId())) {
            throw new BusinessException("该模板对应的附件不存在");
        }
        Attachment attachment = attachmentService.getById(template.getAttachmentId());
        ValidateUtils.isNullException(attachment,"该模板对应的附件不存在");

        return new AttachmentVO(attachment.getId(),attachment.getFileUrl(),attachment.getFileName());
    }

    /**
     * 修改模板文件
     * @param requestVO
     */
    private void updateTemplate(TemplateSaveRequestVO requestVO) {
        Template checkTemplate = this.getById(requestVO.getId());
        ValidateUtils.isNullException(checkTemplate,"该模板不存在");

        Template template = BeanCopierUtil.copyBean(requestVO,Template.class);
        this.updateById(template);

        updateAttachment(template.getId(),requestVO.getTemplateType(),requestVO.getAttachmentId());
    }

    /**
     * 新增模板文件
     * @param requestVO
     */
    private void addTemplate(TemplateSaveRequestVO requestVO) {
        Template template = BeanCopierUtil.copyBean(requestVO,Template.class);
        this.save(template);
        requestVO.setId(template.getId());

        updateAttachment(template.getId(),requestVO.getTemplateType(),requestVO.getAttachmentId());
    }

    /**
     * 更新附件信息
     * @param templateId
     * @param templateType
     * @param attachmentId
     */
    private void updateAttachment(long templateId,int templateType,long attachmentId) {
        Attachment attachment = attachmentService.getById(attachmentId);
        ValidateUtils.isNullException(attachment,"上传的附件不存在,请确认");
        AttachmentTypeEnum typeEnum = templateType == 1 ? AttachmentTypeEnum.TEMPLATE_AGREEMENT : AttachmentTypeEnum.TEMPLATE_BIDDING;
        attachmentService.updateBusiness(attachment.getId(),typeEnum,templateId);
    }
}
