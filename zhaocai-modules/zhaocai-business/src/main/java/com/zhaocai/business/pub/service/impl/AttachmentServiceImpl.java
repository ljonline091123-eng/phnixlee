package com.zhaocai.business.pub.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.mapper.AttachmentMapper;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ISysFileService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.vo.res.DownloadAgreementVO;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 附件Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment> implements IAttachmentService {

    @Autowired
    private ISysFileService sysFileService;

    @Override
    public void addAttachment(List<AttachmentRequestVO> attachmentList, AttachmentTypeEnum businessType, Long businessId) {
        if (CollectionUtil.isNotEmpty(attachmentList)) {
            //这里先删除附件
            List<Attachment> attachments = super.list(new LambdaQueryWrapper<Attachment>()
                    .eq(Attachment::getBusinessType, businessType.getType())
                    .eq(Attachment::getBusinessId, businessId)
                    .eq(Attachment::getDelFlag, 0));
            if (CollUtil.isNotEmpty(attachments)) {
                List<Long> ids = attachments.stream().map(Attachment::getId).collect(Collectors.toList());
                super.removeBatchByIds(ids);
            }
            //保存多个附件
            List<Attachment> list = new ArrayList<>();
            attachmentList.forEach(x -> {
                Attachment attachment = new Attachment();
                attachment.setBusinessType(businessType.getType());
                attachment.setBusinessId(businessId);
                attachment.setFileUrl(x.getFileUrl());
                attachment.setFileName(x.getFileName());
                list.add(attachment);
            });
            super.saveOrUpdateBatch(list);
        }
    }

    @Override
    public List<AttachmentVO> listAttachment(AttachmentTypeEnum businessType, Long businessId) {
        List<Attachment> attachments = super.list(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getBusinessType, businessType.getType())
                .eq(Attachment::getBusinessId, businessId)
                .eq(Attachment::getDelFlag, 0));

        return BeanCopierUtil.copyList(attachments, AttachmentVO.class);
    }

    @Override
    public AttachmentVO getAttachmentById(Long attachmentId) {
        Attachment attachment = super.getOne(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getId, attachmentId));
        return BeanCopierUtil.copyBean(attachment, AttachmentVO.class);
    }

    @Override
    public Long addAttachment(AttachmentRequestVO requestVO, AttachmentTypeEnum businessType, Long businessId) {
        Attachment attachment = new Attachment();
        attachment.setBusinessType(businessType.getType());
        attachment.setBusinessId(businessId);
        attachment.setFileUrl(requestVO.getFileUrl());
        attachment.setFileName(requestVO.getFileName());

        super.save(attachment);
        return attachment.getId();
    }

    @Override
    public void deleteByBusinessId(AttachmentTypeEnum businessType, Long businessId) {
        super.update(new LambdaUpdateWrapper<Attachment>()
                .set(Attachment::getDelFlag, 2)
                .eq(Attachment::getBusinessId, businessId)
                .eq(Attachment::getBusinessType, businessType.getType()));
    }

    @Override
    public Long saveAttachment(AttachmentRequestVO requestVO) {
        Attachment attachment = new Attachment();
        attachment.setFileUrl(requestVO.getFileUrl());
        attachment.setFileName(requestVO.getFileName());

        super.save(attachment);

        return attachment.getId();
    }

    @Override
    public void updateBusiness(Long id, Long businessId,String fileUrl,String fileName) {
        if (NumberUtil.isNullOrZero(id)  || NumberUtil.isNullOrZero(businessId)||fileUrl==null||fileName==null) {
            throw new ParamValidateException("保存附件时关键信息为空");
        }
        super.update(new LambdaUpdateWrapper<Attachment>()
                .set(Attachment::getBusinessId, businessId)
                .eq(Attachment::getId, id));
    }

    @Override
    public void updateBusiness(Long id, AttachmentTypeEnum businessType, Long businessId) {
        if (NumberUtil.isNullOrZero(id) || businessType == null || NumberUtil.isNullOrZero(businessId)) {
            throw new ParamValidateException("保存附件时关键信息为空");
        }

        super.update(new LambdaUpdateWrapper<Attachment>()
                .set(Attachment::getBusinessType, businessType.getType())
                .set(Attachment::getBusinessId, businessId)
                .eq(Attachment::getId, id));
    }

    @Override
    public DownloadAgreementVO getAttachmentInputStream(long attachmentId, String agreementName) {
        Attachment attachment = super.getById(attachmentId);
        ValidateUtils.isNullException(attachment, "该合同附件不存在，请联系管理员");
        if (StringUtils.isBlank(attachment.getFileUrl())) {
            throw new ParamValidateException("附件 url 地址不存在，请联系管理员");
        }

        InputStream inputStream = sysFileService.getFileByFileUrl(attachment.getFileUrl());

        DownloadAgreementVO agreementVO = new DownloadAgreementVO();
        agreementVO.setFileStream(inputStream);
        agreementVO.setFileName(agreementName + FileNameUtil.getPrefix(attachment.getFileName()));
        return agreementVO;
    }
}
