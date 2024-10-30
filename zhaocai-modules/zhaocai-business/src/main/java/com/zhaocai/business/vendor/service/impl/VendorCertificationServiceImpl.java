package com.zhaocai.business.vendor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.CertificationTypeEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.vendor.domain.VendorCertification;
import com.zhaocai.business.vendor.mapper.VendorCertificationMapper;
import com.zhaocai.business.vendor.service.IVendorCertificationService;
import com.zhaocai.business.vendor.vo.req.VendorCertificationRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorRegisterRequestVO;
import com.zhaocai.business.vendor.vo.res.VendorCertificationListVO;
import com.zhaocai.business.vendor.vo.res.VendorCertificationVO;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 供应商附件Service业务层处理
 *
 * @author WH
 * @date 2024-05-29
 */
@Service
public class VendorCertificationServiceImpl extends ServiceImpl<VendorCertificationMapper, VendorCertification> implements IVendorCertificationService {

    @Override
    public Long addCertification(VendorCertificationRequestVO requestVO, CertificationTypeEnum certificationType, Long vendorId) {
                super.remove(new LambdaQueryWrapper<VendorCertification>()
                        .eq(VendorCertification::getVendorId,vendorId)
                        .eq(VendorCertification::getDelFlag,0)
                        .eq(VendorCertification::getBusinessCode,certificationType));
        if (requestVO != null && StringUtils.isNotBlank(requestVO.getAttachmentFileUrl()) &&
                StringUtils.isNotBlank(requestVO.getAttachmentFileName())) {
            VendorCertification certification = new VendorCertification();
            certification.setVendorId(vendorId);
            certification.setBusinessCode(certificationType.getType());
            certification.setAttachmentFileUrl(requestVO.getAttachmentFileUrl());
            certification.setAttachmentFileName(requestVO.getAttachmentFileName());
            certification.setEffectiveBeginDate(requestVO.getEffectiveBeginDate());
            certification.setEffectiveEndDate(requestVO.getEffectiveEndDate());
            if (!CertificationTypeEnum.LEGAL_AUTHORIZATION.equalsType(certificationType.getType())) {
                // 非法人授权书设置为供应商 id
                certification.setBusinessId(vendorId);
            }
            super.save(certification);
            return certification.getId();
        }
        return null;
    }

    @Override
    public void addCertification(List<VendorCertificationRequestVO> requestList, CertificationTypeEnum certificationType, Long vendorId) {
        super.remove(new LambdaQueryWrapper<VendorCertification>()
                .eq(VendorCertification::getVendorId,vendorId)
                .eq(VendorCertification::getDelFlag,0)
                .eq(VendorCertification::getBusinessCode,certificationType));
        List<VendorCertification> list = requestList.stream()
                .map(x ->{
                    VendorCertification certification = new VendorCertification();
                    certification.setVendorId(vendorId);
                    certification.setBusinessCode(certificationType.getType());
                    certification.setBusinessId(vendorId);
                    certification.setAttachmentFileUrl(x.getAttachmentFileUrl());
                    certification.setAttachmentFileName(x.getAttachmentFileName());
                    certification.setEffectiveBeginDate(x.getEffectiveBeginDate());
                    certification.setEffectiveEndDate(x.getEffectiveEndDate());
                    return  certification;
                }).collect(Collectors.toList());
        super.saveBatch(list);
    }

    @Override
    public VendorCertificationListVO listCertification(Long vendorId,Long mainContactId) {
        List<String> typeList = Arrays.asList(CertificationTypeEnum.BUSINESS_LICENSE.getType(),
                                                CertificationTypeEnum.INTEGRITY.getType(),
                                                CertificationTypeEnum.LEGAL_AUTHORIZATION.getType(),
                                                CertificationTypeEnum.RELEVANT_CERTIFICATION.getType());
        // 获取该企业的授权书
        List<VendorCertification> attachments = super.list(new LambdaQueryWrapper<VendorCertification>()
                .eq(VendorCertification::getVendorId,vendorId)
                .eq(VendorCertification::getDelFlag,0)
                .in(VendorCertification::getBusinessCode,typeList));

        VendorCertificationListVO  listVO = new VendorCertificationListVO();
        for(VendorCertification certification : attachments){
            VendorCertificationVO vendorCertificationVo = BeanCopierUtil.copyBean(certification,VendorCertificationVO.class);

            if(CertificationTypeEnum.BUSINESS_LICENSE.equalsType(certification.getBusinessCode()))  {
                listVO.setBusinessLicense(vendorCertificationVo);
            }  else if (CertificationTypeEnum.INTEGRITY.equalsType(certification.getBusinessCode())) {
                listVO.setIntegrity(vendorCertificationVo);
            }else if (CertificationTypeEnum.LEGAL_AUTHORIZATION.equalsType(certification.getBusinessCode())
                    && mainContactId.equals(certification.getBusinessId())) {
                // 只显示主要联系人的授权书
                List<VendorCertificationVO> list = listVO.getLegalAuthorizationList();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(vendorCertificationVo);
                listVO.setLegalAuthorizationList(list);
            }else if (CertificationTypeEnum.RELEVANT_CERTIFICATION.equalsType(certification.getBusinessCode())) {
                List<VendorCertificationVO> list = listVO.getRelevantCertificationList();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(vendorCertificationVo);
                listVO.setRelevantCertificationList(list);
            }
        }
        return listVO;
    }

    @Override
    public VendorRegisterRequestVO listCertification(VendorRegisterRequestVO vendorRequestVO, Long vendorId, Long mainContactId) {
        List<String> typeList = Arrays.asList(CertificationTypeEnum.BUSINESS_LICENSE.getType(),
                CertificationTypeEnum.INTEGRITY.getType(),
                CertificationTypeEnum.LEGAL_AUTHORIZATION.getType(),
                CertificationTypeEnum.RELEVANT_CERTIFICATION.getType());
        // 获取该企业的授权书
        List<VendorCertification> attachments = super.list(new LambdaQueryWrapper<VendorCertification>()
                .eq(VendorCertification::getVendorId,vendorId)
                .eq(VendorCertification::getDelFlag,0)
                .in(VendorCertification::getBusinessCode,typeList));
        for(VendorCertification certification : attachments){
            VendorCertificationRequestVO vendorCertificationVo = BeanCopierUtil.copyBean(certification,VendorCertificationRequestVO.class);
            if(CertificationTypeEnum.BUSINESS_LICENSE.equalsType(certification.getBusinessCode()))  {
                vendorRequestVO.setBusinessLicense(vendorCertificationVo);
            }  else if (CertificationTypeEnum.INTEGRITY.equalsType(certification.getBusinessCode())) {
                vendorRequestVO.setIntegrity(vendorCertificationVo);
            }else if (CertificationTypeEnum.LEGAL_AUTHORIZATION.equalsType(certification.getBusinessCode())
                    && mainContactId.equals(certification.getBusinessId())) {
                // 只显示主要联系人的授权书
                vendorRequestVO.setLegalAuthorization(vendorCertificationVo);
                //资质信息
            }else if (CertificationTypeEnum.RELEVANT_CERTIFICATION.equalsType(certification.getBusinessCode())) {
                List<VendorCertificationRequestVO> list = vendorRequestVO.getRelevantCertificationList();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(vendorCertificationVo);
                vendorRequestVO.setRelevantCertificationList(list);
            }
        }
        return vendorRequestVO;
    }

    @Override
    public void deleteCertification(Long vendorId, Long id) {
        VendorCertification attachment = super.getById(id);
        ValidateUtils.isNullException(attachment,"该企业资质不存在");

        if (!vendorId.equals(attachment.getVendorId())) {
            throw new ParamValidateException("你无权删除不是您的企业资质");
        }

        super.removeById(id);
    }

    @Override
    public void addCertification(VendorCertificationRequestVO requestVO, Long vendorId,CertificationTypeEnum certificationType) {
        if (NumberUtil.isNullOrZero(requestVO.getId())) {
            if (CertificationTypeEnum.BUSINESS_LICENSE.equalsType(certificationType.getType())
                    || CertificationTypeEnum.INTEGRITY.equalsType(certificationType.getType())) {
                // 判断是否唯一
                long count = super.count(new LambdaQueryWrapper<VendorCertification>()
                        .eq(VendorCertification::getBusinessCode,certificationType.getType())
                        .eq(VendorCertification::getVendorId,vendorId));
                if (count >= 1) {
                    throw new BusinessException(certificationType.getDesc() + "已经存在一个了，请勿重新添加");
                }
            }
            VendorCertification vendorCertification  = BeanCopierUtil.copyBean(requestVO,VendorCertification.class);
            vendorCertification.setVendorId(vendorId);
            vendorCertification.setBusinessId(vendorId);
            vendorCertification.setBusinessCode(certificationType.getType());

            super.save(vendorCertification);
        } else {
            VendorCertification vendorCertification  = BeanCopierUtil.copyBean(requestVO,VendorCertification.class);
            super.updateById(vendorCertification);
        }
    }

    @Override
    public Long addCertification(Long vendorId, AttachmentRequestVO attachment, CertificationTypeEnum businessType, Long businessId) {
        VendorCertification certification = new VendorCertification();
        certification.setVendorId(vendorId);
        certification.setAttachmentFileName(attachment.getFileName());
        certification.setAttachmentFileUrl(attachment.getFileUrl());
        certification.setBusinessCode(businessType.getType());
        certification.setBusinessId(businessId);

        this.save(certification);
        return certification.getId();
    }

    @Override
    public List<VendorCertification> listVendorCertification(Long vendorId, CertificationTypeEnum businessType, Long businessId) {
        return super.list(new LambdaQueryWrapper<VendorCertification>()
                .eq(VendorCertification::getVendorId,vendorId)
                .eq(VendorCertification::getBusinessCode,businessType.getType())
                .eq(VendorCertification::getBusinessId,businessId));
    }
}
