package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.business.vendor.vo.req.*;
import com.zhaocai.business.vendor.vo.res.VendorContactListVO;
import com.zhaocai.business.vendor.vo.res.VendorMainContactVO;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 供应商联系人Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IVendorContactService  extends IService<VendorContact> , IProcessBusinessBaseService {

    /**
     * 保存供应商主要联系人
     * @param contact
     */
    Long saveMainVendorContact(VendorContact contact);

    /**
     * 获取供应商联系人
     * @param vendorId
     * @return
     */
    List<VendorContact> listContactByVendorId(Long vendorId);

    /**
     * 获取供应商联系人
     *
     * @param vendorId
     * @return
     */
    List<VendorContactListVO> listVendorContact(Long vendorId);

    /**
     * 获取供应商主要联系人
     * @param vendorId
     * @return
     */
    VendorMainContactVO getMainContact(Long vendorId);

    /**
     * 新增供应商联系人
     * @param requestVO
     * @param vendorId
     */
    void addVendorContact(VendorContactAddRequestVO requestVO,Long vendorId);

    /**
     * 修改供应商状态
     * @param id
     * @param state
     */
    void updateContactState(Long id, Integer state);

    /**
     * 获取供应商联系人管理列表
     * @param queryVO
     * @return
     */
    PageResult<VendorContactListVO> listContactManagementList(VendorContactListQueryVO queryVO);

    /**
     * 设置联系人登录信息
     * @param loginUserId
     * @param id
     */
    void setContractLoginUser(Long loginUserId, Long id);

    /**
     * 根据登录用户id获取供应商联系人
     * @param userId
     * @return
     */
    VendorContact getVendorContactByLoginUser(Long userId);

    /**
     * 获取联系人授权书
     *
     * @param id
     * @return
     */
    AttachmentVO getAuthorization(Long id);

    /**
     * 修改联系人授权书
     * @param requestVO
     */
    void updateAuthorizationFile(UpdateAuthorizationFileRequestVO requestVO);

    /**
     * 修改联系人授权书有效时间
     * @param requestVO
     */
    void updateAuthorizationDate(UpdateAuthorizationDateRequestVO requestVO);

    /**
     * 增加登录账号
     * @param contactPhone
     * @param contactName
     * @return
     */
    Long addLoginUser(String contactPhone, String contactName);

    /**
     * 设置供应商管理员
     * @param requestVO
     */
    void updateContactManager(UpdateContactManagerRequestVO requestVO);

    /**
     * 修改供应商联系人
     * @param requestVO
     * @param id
     */
    void updateVendorContact(VendorContactSaveRequestVo requestVO, Long id);

    /**
     * 获取供应商联系人详情
     * @param id
     * @return
     */
    VendorContactSaveRequestVo getVendorContactDetail(Long id);

    /**
     * 企业联系人授权
     * @return
     */
    String vendorContactSignAuth();

    /**
     * 获取供应商管理员
     * @param vendorId
     * @return
     */
    VendorContact getVendorManager(Long vendorId);

    VendorContact getInfo(Long id);
}
