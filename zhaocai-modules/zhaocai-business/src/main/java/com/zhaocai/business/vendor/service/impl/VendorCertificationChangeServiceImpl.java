package com.zhaocai.business.vendor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.CertificationTypeEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.vendor.domain.VendorCertification;
import com.zhaocai.business.vendor.domain.VendorCertificationChange;
import com.zhaocai.business.vendor.domain.VendorChange;
import com.zhaocai.business.vendor.mapper.VendorCertificationChangeMapper;
import com.zhaocai.business.vendor.service.IVendorCertificationChangeService;
import com.zhaocai.business.vendor.service.IVendorCertificationService;
import com.zhaocai.business.vendor.vo.res.VendorCertificationListVO;
import com.zhaocai.business.vendor.vo.res.VendorCertificationVO;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 供应商资质变更Service业务层处理
 *
 * @author lsn
 * @date 2024-08-06
 */
@Service
public class VendorCertificationChangeServiceImpl extends ServiceImpl<VendorCertificationChangeMapper, VendorCertificationChange> implements IVendorCertificationChangeService {

    @Autowired
    private IVendorCertificationService vendorCertificationService;

    /**
     * 创建资质变更新版本
     *
     * @param vendorId
     * @return
     */
    @Override
    public List<VendorCertificationChange> createCertificationChange(Long vendorId, Integer version) {
        // 资质类型
        List<String> typeList = Arrays.asList(CertificationTypeEnum.BUSINESS_LICENSE.getType(),
                CertificationTypeEnum.INTEGRITY.getType(),
                CertificationTypeEnum.LEGAL_AUTHORIZATION.getType(),
                CertificationTypeEnum.RELEVANT_CERTIFICATION.getType());
        // 获取原始表中供应商资质
        List<VendorCertification> attachments = vendorCertificationService.list(new LambdaQueryWrapper<VendorCertification>()
                .eq(VendorCertification::getVendorId, vendorId)
                .eq(VendorCertification::getDelFlag, 0)
                .eq(VendorCertification::getBusinessId, vendorId)
                .in(VendorCertification::getBusinessCode, typeList));
        // 创建供应商资质变更
        List<VendorCertificationChange> certificationChangeList = new ArrayList<>();
        for (VendorCertification certification : attachments) {
            VendorCertificationChange certificationChange = BeanCopierUtil.copyBean(certification, VendorCertificationChange.class);
            certificationChange.setCertificationId(certification.getId());
            certificationChange.setId(null);
            certificationChange.setVersion(version);
            certificationChange.setChangeStatus(0);
            certificationChangeList.add(certificationChange);
        }
        super.saveBatch(certificationChangeList);
        return certificationChangeList;
    }

    /**
     * 审批通过后更新企业资质
     *
     * @param vendorChange
     */
    @Override
    public void handleApprove(VendorChange vendorChange) {
        // 将原供应商资质表中的数据（四类）设置为删除状态
        List<String> typeList = Arrays.asList(CertificationTypeEnum.BUSINESS_LICENSE.getType(),
                CertificationTypeEnum.INTEGRITY.getType(),
                CertificationTypeEnum.LEGAL_AUTHORIZATION.getType(),
                CertificationTypeEnum.RELEVANT_CERTIFICATION.getType());
        // 获取本次资质变更的信息
        List<VendorCertificationChange> changeList = super.list(new LambdaQueryWrapper<VendorCertificationChange>()
                .eq(VendorCertificationChange::getVendorId, vendorChange.getVendorId())
                .eq(VendorCertificationChange::getVersion, vendorChange.getVersion())
                .eq(VendorCertificationChange::getDelFlag, 0));
        // 将非更新或新增的企业资质删除
        List<Long> certificationIds = changeList.stream().filter(i -> null != i.getCertificationId()).map(VendorCertificationChange::getCertificationId).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(certificationIds)){
            vendorCertificationService.update(new LambdaUpdateWrapper<VendorCertification>()
                    .set(VendorCertification::getDelFlag, 2)
                    .eq(VendorCertification::getVendorId, vendorChange.getVendorId())
                    .eq(VendorCertification::getBusinessId, vendorChange.getVendorId())
                    .in(VendorCertification::getBusinessCode, typeList)
                    .notIn(VendorCertification::getId, certificationIds));
        } else {
            vendorCertificationService.update(new LambdaUpdateWrapper<VendorCertification>()
                    .set(VendorCertification::getDelFlag, 2)
                    .eq(VendorCertification::getVendorId, vendorChange.getVendorId())
                    .eq(VendorCertification::getBusinessId, vendorChange.getVendorId())
                    .in(VendorCertification::getBusinessCode, typeList));
        }
        for (VendorCertificationChange certificationChange : changeList) {
            VendorCertification certification = BeanCopierUtil.copyBean(certificationChange, VendorCertification.class);
            certification.setId(certificationChange.getCertificationId());
            if (CertificationTypeEnum.BUSINESS_LICENSE.equalsType(certificationChange.getBusinessCode())) {
                this.updateCertification(certification, vendorChange.getVendorId(), CertificationTypeEnum.BUSINESS_LICENSE);
            } else if (CertificationTypeEnum.INTEGRITY.equalsType(certificationChange.getBusinessCode())) {
                this.updateCertification(certification, vendorChange.getVendorId(), CertificationTypeEnum.INTEGRITY);
            } else if (CertificationTypeEnum.LEGAL_AUTHORIZATION.equalsType(certificationChange.getBusinessCode())) {
                this.updateCertification(certification, vendorChange.getVendorId(), CertificationTypeEnum.LEGAL_AUTHORIZATION);
            } else if (CertificationTypeEnum.RELEVANT_CERTIFICATION.equalsType(certificationChange.getBusinessCode())) {
                this.updateCertification(certification, vendorChange.getVendorId(), CertificationTypeEnum.RELEVANT_CERTIFICATION);
            }
        }

    }

    /**
     * 删除变更表的企业资质
     *
     * @param vendorId
     * @param id
     */
    @Override
    public void deleteCertification(Long vendorId, Long id) {
        VendorCertificationChange attachment = super.getById(id);
        ValidateUtils.isNullException(attachment, "该企业资质不存在");

        if (!vendorId.equals(attachment.getVendorId())) {
            throw new ParamValidateException("你无权删除不是您的企业资质");
        }

        super.removeById(id);
    }

    /**
     * 新增变更表的企业资质
     *
     * @param requestVO         资质信息
     * @param id                供应商id
     * @param version           版本
     * @param certificationType 资质分类
     */
    @Override
    public void addCertification(VendorCertificationChange requestVO, Long id, Integer version, CertificationTypeEnum certificationType) {
        if (NumberUtil.isNullOrZero(requestVO.getId())) {
            if (CertificationTypeEnum.BUSINESS_LICENSE.equalsType(certificationType.getType())
                    || CertificationTypeEnum.INTEGRITY.equalsType(certificationType.getType())) {
                // 判断是否唯一
                long count = super.count(new LambdaQueryWrapper<VendorCertificationChange>()
                        .eq(VendorCertificationChange::getBusinessCode, certificationType.getType())
                        .eq(VendorCertificationChange::getVendorId, id)
                        .eq(VendorCertificationChange::getVersion, version)
                );
                if (count >= 1) {
                    throw new BusinessException(certificationType.getDesc() + "已经存在一个了，请勿重新添加");
                }
            }
            requestVO.setVendorId(id);
            requestVO.setBusinessId(id);
            requestVO.setVersion(version);
            requestVO.setBusinessCode(certificationType.getType());
            requestVO.setDelFlag("0");
            requestVO.setChangeStatus(0);

            super.save(requestVO);
        } else {
            requestVO.setDelFlag("0");
            super.updateById(requestVO);
        }
    }

    /**
     * 获取企业资质信息
     *
     * @param id      供应商id
     * @param version 版本
     * @return
     */
    @Override
    public VendorCertificationListVO listCertification(Long id, Integer version) {
        List<String> typeList = Arrays.asList(CertificationTypeEnum.BUSINESS_LICENSE.getType(),
                CertificationTypeEnum.INTEGRITY.getType(),
                CertificationTypeEnum.LEGAL_AUTHORIZATION.getType(),
                CertificationTypeEnum.RELEVANT_CERTIFICATION.getType());
        List<VendorCertificationChange> attachments = super.list(new LambdaQueryWrapper<VendorCertificationChange>()
                .eq(VendorCertificationChange::getVendorId, id)
                .eq(VendorCertificationChange::getBusinessId, id)
                .eq(VendorCertificationChange::getDelFlag, 0)
                .in(VendorCertificationChange::getBusinessCode, typeList)
                .eq(VendorCertificationChange::getVersion, version));
        // 将企业资质根据资质类型分类
        VendorCertificationListVO listVO = new VendorCertificationListVO();
        for (VendorCertificationChange certification : attachments) {
            VendorCertificationVO vendorCertificationVo = BeanCopierUtil.copyBean(certification, VendorCertificationVO.class);

            if (CertificationTypeEnum.BUSINESS_LICENSE.equalsType(certification.getBusinessCode())) {
                listVO.setBusinessLicense(vendorCertificationVo);
            } else if (CertificationTypeEnum.INTEGRITY.equalsType(certification.getBusinessCode())) {
                listVO.setIntegrity(vendorCertificationVo);
            } else if (CertificationTypeEnum.LEGAL_AUTHORIZATION.equalsType(certification.getBusinessCode())) {
                List<VendorCertificationVO> list = listVO.getLegalAuthorizationList();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(vendorCertificationVo);
                listVO.setLegalAuthorizationList(list);
            } else if (CertificationTypeEnum.RELEVANT_CERTIFICATION.equalsType(certification.getBusinessCode())) {
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

    /**
     * 判断资质是否需要提交流程
     * @param updateVendor
     * @return
     */
    @Override
    public Boolean checkCertificationSubmit(VendorChange updateVendor) {
        Boolean flag = false;
        List<VendorCertificationChange> certificationChangeList = super.list(new LambdaQueryWrapper<VendorCertificationChange>()
                .eq(VendorCertificationChange::getVendorId, updateVendor.getVendorId())
                .eq(VendorCertificationChange::getVersion, updateVendor.getVersion()));
        List<VendorCertificationChange> checkNull = certificationChangeList.stream().filter(i -> null == i.getCertificationId()).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(checkNull)){
            flag = true;
        } else {
            for (VendorCertificationChange change : certificationChangeList){
                VendorCertification certification = vendorCertificationService.getById(change.getCertificationId());
                if ((null != certification.getAttachmentFileName() && null != change.getAttachmentFileName() && !certification.getAttachmentFileName().equals(change.getAttachmentFileName()))
                        || (null != certification.getAttachmentFileUrl() && null != change.getAttachmentFileUrl() && !change.getAttachmentFileUrl().equals(certification.getAttachmentFileUrl()))
                        || (null != certification.getEffectiveBeginDate() && null != change.getEffectiveBeginDate() && !change.getEffectiveBeginDate().equals(certification.getEffectiveBeginDate()))
                        || (null != certification.getEffectiveEndDate() && null != change.getEffectiveEndDate() && !change.getEffectiveEndDate().equals(certification.getEffectiveEndDate()))) {
                    flag = true;
                }
            }
        }
        return flag;
    }

    /**
     * 审批通过后更新企业资质信息
     *
     * @param certification     企业资质信息
     * @param vendorId          版本
     * @param certificationType 资质类型
     */
    private void updateCertification(VendorCertification certification, Long vendorId, CertificationTypeEnum certificationType) {
        if (NumberUtil.isNullOrZero(certification.getId())) {
            if (CertificationTypeEnum.BUSINESS_LICENSE.equalsType(certificationType.getType()) || CertificationTypeEnum.INTEGRITY.equalsType(certificationType.getType())) {
                // 判断是否唯一
                long count = vendorCertificationService.count(new LambdaQueryWrapper<VendorCertification>()
                        .eq(VendorCertification::getBusinessCode, certificationType.getType())
                        .eq(VendorCertification::getVendorId, vendorId));
                if (count >= 1) {
                    throw new BusinessException(certificationType.getDesc() + "已经存在一个了，请勿重新添加");
                }
            }
            certification.setVendorId(vendorId);
            certification.setBusinessId(vendorId);
            certification.setBusinessCode(certificationType.getType());
            certification.setDelFlag("0");
            vendorCertificationService.save(certification);
        } else {
//            vendorCertificationService.update(new LambdaUpdateWrapper<VendorCertification>()
//                    .set(VendorCertification::getDelFlag, 0)
//                    .set(VendorCertification::getEffectiveBeginDate, certification.getEffectiveBeginDate())
//                    .set(VendorCertification::getEffectiveEndDate, certification.getEffectiveEndDate())
//                    .eq(VendorCertification::getId, certification.getId()));
            certification.setDelFlag("0");
            vendorCertificationService.updateById(certification);
        }
    }
}
