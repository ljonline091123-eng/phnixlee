package com.zhaocai.flowable.mapper;

public interface VendorMapper {

    /**
     * 修改供应商状态
     * @param vendorId
     */
    void updateVendorState(Long vendorId);

    /**
     * 修改供应商联系人状态
     * @param vendorContactId
     */
    void updateVendorContactState(Long vendorContactId);
}
