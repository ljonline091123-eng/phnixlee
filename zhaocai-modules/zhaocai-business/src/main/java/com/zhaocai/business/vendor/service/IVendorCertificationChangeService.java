package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.common.enums.CertificationTypeEnum;
import com.zhaocai.business.vendor.domain.VendorCertificationChange;
import com.zhaocai.business.vendor.domain.VendorChange;
import com.zhaocai.business.vendor.vo.res.VendorCertificationListVO;

import java.util.List;

/**
 * 供应商资质变更Service接口
 *
 * @author lsn
 * @date 2024-08-06
 */
public interface IVendorCertificationChangeService extends IService<VendorCertificationChange> {

    /**
     * 创建资质变更新版本
     * @param vendorId
     * @return
     */
    List<VendorCertificationChange> createCertificationChange(Long vendorId, Integer version);

    /**
     * 创建资质变更新版本
     * @param vendorId
     * @return
     */
    List<VendorCertificationChange> getCertificationChange(Long vendorId, Integer version);



    /**
     * 处理审批通过后资质信息更新
     * @param vendorChange
     */
    void handleApprove(VendorChange vendorChange);

    /**
     * 删除企业资质变更信息
     * @param id
     * @param id
     */
    void deleteCertification(Long id, Long id1);

    /**
     * 新增企业资质信息
     * @param requestVO
     * @param id
     * @param version
     * @param legalAuthorization
     */
    void addCertification(VendorCertificationChange requestVO, Long id, Integer version, CertificationTypeEnum legalAuthorization);

    /**
     * 获取企业资质变更详情
     * @param id
     * @param version
     * @return
     */
    VendorCertificationListVO listCertification(Long id, Integer version);

    /**
     * 判断资质是否需要提交流程
     * @param updateVendor
     * @return
     */
    Boolean checkCertificationSubmit(VendorChange updateVendor);
}
