package com.zhaocai.business.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.BiddingMarkTemplate;
import com.zhaocai.business.bidding.service.IBiddingMarkTemplateService;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.procurement.domain.ProcurementSchemeBidding;
import com.zhaocai.business.procurement.mapper.ProcurementSchemeBiddingMapper;
import com.zhaocai.business.procurement.service.IProcurementSchemeBiddingService;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeBiddingVO;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeTemplateVO;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.domain.Template;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ITemplateService;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 采购方案-招标信息Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class ProcurementSchemeBiddingServiceImpl extends ServiceImpl<ProcurementSchemeBiddingMapper,ProcurementSchemeBidding> implements IProcurementSchemeBiddingService {

    @Autowired
    private IBiddingMarkTemplateService biddingMarkTemplateService;

    @Autowired
    private ITemplateService templateService;

    @Autowired
    private IAttachmentService attachmentService;

    @Override
    public void saveProcurementSchemeBidding(ProcurementSchemeBidding procurementSchemeBidding, Long schemeId) {
        procurementSchemeBidding.setSchemeId(schemeId);

        super.save(procurementSchemeBidding);

        // 更新招标文件附件
        attachmentService.updateBusiness(procurementSchemeBidding.getBiddingAttachmentId(), AttachmentTypeEnum.SCHEME_BIDDING,procurementSchemeBidding.getSchemeId());
    }

    @Override
    public void updateProcurementSchemeBidding(ProcurementSchemeBidding procurementSchemeBidding, Long schemeId) {
        if (NumberUtil.isNullOrZero(procurementSchemeBidding.getId())) {
            throw new ParamValidateException("采购方案编辑时，需要传招标文件 id");
        }
        procurementSchemeBidding.setSchemeId(schemeId);
        super.updateById(procurementSchemeBidding);

        attachmentService.updateBusiness(procurementSchemeBidding.getBiddingAttachmentId(), AttachmentTypeEnum.SCHEME_BIDDING,procurementSchemeBidding.getSchemeId());
    }

    @Override
    public ProcurementSchemeBiddingVO getBySchemeId(Long schemeId) {
        ProcurementSchemeBidding schemeBidding = baseMapper.selectOne(new LambdaQueryWrapper<ProcurementSchemeBidding>()
                .eq(ProcurementSchemeBidding::getSchemeId,schemeId));
        ValidateUtils.isNullException(schemeBidding,"该采购方案对应的招标信息不存在");

        ProcurementSchemeBiddingVO schemeBiddingVO = BeanCopierUtil.copyBean(schemeBidding,ProcurementSchemeBiddingVO.class);

        // 评分模板
        BiddingMarkTemplate markTemplate = biddingMarkTemplateService.getById(schemeBidding.getEvaluationTemplateId());
        if(markTemplate!=null){
            schemeBiddingVO.setEvaluationTemplate(new ProcurementSchemeTemplateVO(markTemplate.getId(),markTemplate.getName()));
        }else{
            schemeBiddingVO.setEvaluationTemplate(null);
        }

        // 招标文件模板
        Template template = templateService.getById(schemeBidding.getBiddingTemplateId());
        if(template!=null){
            ProcurementSchemeTemplateVO schemeTemplate = new ProcurementSchemeTemplateVO(template.getId(),template.getTemplateName());
            Attachment biddingAttachment = attachmentService.getById(schemeBidding.getBiddingAttachmentId());
            if(biddingAttachment!=null){
                schemeTemplate.setAttachmentId(biddingAttachment.getId());
                schemeTemplate.setFileName(biddingAttachment.getFileName());
                schemeTemplate.setFileUrl(biddingAttachment.getFileUrl());
            }
            schemeBiddingVO.setBiddingTemplate(schemeTemplate);
        }else{
            schemeBiddingVO.setBiddingTemplate(null);
        }

        // 合同模板
        template = templateService.getById(schemeBidding.getContractTemplateId());
        if(template!=null) {
            ProcurementSchemeTemplateVO schemeTemplate = new ProcurementSchemeTemplateVO(template.getId(),template.getTemplateName());
            AttachmentVO agreementAttachment = templateService.getTemplateAttachmentInfo(schemeBidding.getContractTemplateId());
            if(agreementAttachment!=null){
                schemeTemplate.setFileName(agreementAttachment.getFileName());
                schemeTemplate.setFileUrl(agreementAttachment.getFileUrl());
            }
            schemeBiddingVO.setContractTemplate(schemeTemplate);
        }else{
            schemeBiddingVO.setContractTemplate(null);
        }

        return schemeBiddingVO;
    }

    @Override
    public ProcurementSchemeBiddingVO getBiddingTemplateBySchemeId(Long schemeId) {
        ProcurementSchemeBidding schemeBidding = baseMapper.selectOne(new LambdaQueryWrapper<ProcurementSchemeBidding>()
                .eq(ProcurementSchemeBidding::getSchemeId,schemeId));
        ValidateUtils.isNullException(schemeBidding,"该采购方案对应的招标信息不存在");

        ProcurementSchemeBiddingVO schemeBiddingVO = BeanCopierUtil.copyBean(schemeBidding,ProcurementSchemeBiddingVO.class);

        // 评分模板
        BiddingMarkTemplate markTemplate = biddingMarkTemplateService.getById(schemeBidding.getEvaluationTemplateId());
        if (ObjectUtils.isNotEmpty(markTemplate)){
            schemeBiddingVO.setEvaluationTemplate(new ProcurementSchemeTemplateVO(markTemplate.getId(),markTemplate.getName()));
        }

        // 招标文件模板
        Attachment biddingAttachment = attachmentService.getById(schemeBidding.getBiddingAttachmentId());
        if (ObjectUtils.isNotEmpty(biddingAttachment)){
            schemeBiddingVO.setBiddingTemplate((new ProcurementSchemeTemplateVO(biddingAttachment.getId(),biddingAttachment.getFileUrl(),biddingAttachment.getFileName())));
        }

        return schemeBiddingVO;
    }

    @Override
    public ProcurementSchemeBidding getDomainBySchemeId(Long schemeId) {
        return super.getOne(new LambdaQueryWrapper<ProcurementSchemeBidding>()
                .eq(ProcurementSchemeBidding::getSchemeId,schemeId));
    }
}
