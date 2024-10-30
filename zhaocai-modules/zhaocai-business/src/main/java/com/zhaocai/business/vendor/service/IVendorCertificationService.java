package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.common.enums.CertificationTypeEnum;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.vendor.domain.VendorCertification;
import com.zhaocai.business.vendor.vo.req.VendorCertificationRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorRegisterRequestVO;
import com.zhaocai.business.vendor.vo.res.VendorCertificationListVO;

import java.util.List;

/**
 * 供应商附件Service接口
 *
 * @author WH
 * @date 2024-05-29
 */
public interface IVendorCertificationService extends IService<VendorCertification> {

    /**
     * 增加供应商资质
     * @param requestVO
     * @param certificationType
     * @param vendorId
     */
    Long addCertification(VendorCertificationRequestVO requestVO, CertificationTypeEnum certificationType, Long vendorId);

    /**
     *
     * @param requestList
     * @param certificationType
     * @param vendorId
     */
    void addCertification(List<VendorCertificationRequestVO> requestList, CertificationTypeEnum certificationType, Long vendorId);

    /**
     * 获取供应商认证
     * @param vendorId
     * @param mainContactId
     * @return
     */
    VendorCertificationListVO listCertification(Long vendorId,Long mainContactId);


    /**
     * 获取供应商认证
     * @param vendorId
     * @param mainContactId
     * @return
     */
    VendorRegisterRequestVO listCertification(VendorRegisterRequestVO  vendorRequestVO,Long vendorId, Long mainContactId);

    /**
     * 删除供应商认证
     * @param vendorId
     * @param id
     */
    void deleteCertification(Long vendorId, Long id);

    /**
     * 新增企业资质
     * @param requestVO
     * @param vendorId
     * @param certificationType
     */
    void addCertification(VendorCertificationRequestVO requestVO, Long vendorId,CertificationTypeEnum certificationType);

    /**
     * 新增企业资质
     * @param vendorId
     * @param attachment
     * @param businessType
     * @param businessId
     */
    Long addCertification(Long vendorId,AttachmentRequestVO attachment, CertificationTypeEnum businessType,Long businessId);

    /**
     * 获取企业资质
     * @param vendorId
     * @param businessType
     * @param businessId
     * @return
     */
    List<VendorCertification> listVendorCertification(Long vendorId, CertificationTypeEnum businessType, Long businessId);
}
