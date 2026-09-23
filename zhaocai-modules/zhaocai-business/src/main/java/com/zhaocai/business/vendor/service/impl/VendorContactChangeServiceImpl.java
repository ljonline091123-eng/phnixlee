package com.zhaocai.business.vendor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.ProcessStateEnum;
import com.zhaocai.business.common.enums.VendorContactStateEnum;
import com.zhaocai.business.common.enums.VendorStateEnum;
import com.zhaocai.business.vendor.domain.*;
import com.zhaocai.business.vendor.mapper.VendorContactChangeMapper;
import com.zhaocai.business.vendor.service.IVendorContactChangeService;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 供应商联系人变更Service业务层处理
 *
 * @author lsn
 * @date 2024-08-08
 */
@Slf4j
@Service
public class VendorContactChangeServiceImpl extends ServiceImpl<VendorContactChangeMapper, VendorContactChange> implements IVendorContactChangeService {

    @Autowired
    private IVendorContactService vendorContactService;

    /**
     * 创建联系人变更新版本
     * @param vendorId
     * @param version
     * @return
     */
    @Override
    public List<VendorContactChange> createContactChange(Long vendorId, Integer version) {
        List<VendorContact> vendorContactList = vendorContactService.list(new LambdaQueryWrapper<VendorContact>().eq(VendorContact::getVendorId, vendorId));
        List<VendorContactChange> contactChangeList = new ArrayList<>();
        for(VendorContact contact : vendorContactList) {
            VendorContactChange contactChange = BeanCopierUtil.copyBean(contact, VendorContactChange.class);
            contactChange.setContactId(contact.getId());
            contactChange.setId(null);
            contactChange.setVersion(version);
            if(version.equals(0)){
                contactChange.setChangeStatus(VendorStateEnum.APPROVE.getState());
            }else{
                contactChange.setChangeStatus(Integer.valueOf(ProcessStateEnum.FREEDOM.getValue()));
            }
            contactChangeList.add(contactChange);
        }
        super.saveBatch(contactChangeList);
        return contactChangeList;
    }

    /**
     * 审批通过后更新企业联系人信息
     * @param vendorChange
     */
    @Override
    public void handleApprove(VendorChange vendorChange) {
        List<VendorContactChange> contactChangeList = super.list(new LambdaQueryWrapper<VendorContactChange>()
                .eq(VendorContactChange::getVendorId, vendorChange.getVendorId())
                .eq(VendorContactChange::getVersion, vendorChange.getVersion()));
        for (VendorContactChange contactChange : contactChangeList) {
            VendorContact contact = new VendorContact();
            BeanUtils.copyProperties(contactChange, contact);
            contact.setId(contactChange.getContactId());
            // 账号状态不覆盖
            VendorContact vendorContact = vendorContactService.getById(contactChange.getContactId());
            if(null != vendorContact && null != vendorContact.getState()){
                contact.setState(vendorContact.getState());
            }
            vendorContactService.updateById(contact);
        }
    }
}
