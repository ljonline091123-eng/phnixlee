package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.business.vendor.domain.VendorChange;
import com.zhaocai.business.vendor.vo.req.VendorChangeRequestVO;
import com.zhaocai.business.vendor.vo.res.VendorManagementDetailVO;

/**
 * 供应商变更Service接口
 *
 * @author lsn
 * @date 2024-08-05
 */
public interface IVendorChangeService extends IService<VendorChange>, IProcessBusinessBaseService {

    /**
     * 供应商修改详情
     * @param vendorId 供应商id
     * @return
     */
    VendorChangeRequestVO getVendorUpdateDetail(Long vendorId);

    /**
     * 保存供应商信息
     *
     * @param requestVO
     */
    void saveVendorChange(VendorChangeRequestVO requestVO);

    /**
     * 提交供应商信息
     *
     * @param requestVO
     */
    void submitVendorChance(VendorChangeRequestVO requestVO);

    /**
     * 获取最新的版本号
     * @param id 供应商id
     * @return
     */
    Integer getLastVersion(Long id);

    /**
     * 获取供应商变更详情
     * @param id 供应商id
     * @return
     */
    VendorManagementDetailVO getVendorManagementDetail(Long id);
}
