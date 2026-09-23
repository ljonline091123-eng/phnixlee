package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.vendor.domain.VendorChange;
import com.zhaocai.business.vendor.domain.VendorContactChange;

import java.util.List;

/**
 * 供应商联系人变更Service接口
 *
 * @author lsn
 * @date 2024-08-08
 */
public interface IVendorContactChangeService extends IService<VendorContactChange> {

    /**
     * 创建联系人变更新版本
     */
    List<VendorContactChange> createContactChange(Long vendorId, Integer version);

    /**
     * 审批通过-更新供应商联系人
     * @param vendorChange
     */
    void handleApprove(VendorChange vendorChange);
}
