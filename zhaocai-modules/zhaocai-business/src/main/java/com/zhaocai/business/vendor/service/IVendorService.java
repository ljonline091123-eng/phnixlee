package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.vo.req.*;
import com.zhaocai.business.vendor.vo.res.*;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 供应商Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IVendorService  extends IService<Vendor> , IProcessBusinessBaseService, IPBMOverrideService {

    /**
     * 供应商注册
     * @param requestVO
     */
    void register(VendorRegisterRequestVO requestVO);

    /**
     * 供应商修改详情
     * @param vendorId 供应商id
     * @return
     */
    VendorRegisterRequestVO getVendorUpdateDetail(Long vendorId);


    /**
     * 易料供应商推送
     * @param requestVO
     */
    void vendorRegister(VendorRegisterRequestVO requestVO);


    /**
     * 获取供应商详情
     * @param id
     * @return
     */
    VendorDetailVO getVendorDetail(Long id);

    /**
     * 保存供应商信息
     * @param requestVO
     */
    void saveVendor(VendorSaveRequestVO requestVO);

    /**
     * 根据登录用户获取其对应供应商
     * @param userId
     * @return
     */
    Vendor getByLoginUser(Long userId);

    /**
     * 供应商列表查询
     * @param queryVO
     * @return
     */
    PageResult<VendorManagementListVO> listVendor(VendorManagementListQueryVO queryVO);

    /**
     * 供应商列表数据查询
     * @param queryVO
     * @return
     */
    List<VendorManagementListDataVO> getListVendor(VendorManagementListQueryDataVO queryVO);

    /**
     * 获取供应商管理端
     * @param id
     * @return
     */
    VendorManagementDetailVO getVendorManagementDetail(Long id);

    /**
     * 修改供应商等级
     * @param requestVO
     */
    void updateVendorLevel(VendorLevelRequestVO requestVO);

    /**
     * 修改供应商黑名单状态
     * @param requestVO
     */
    void updateBlackState(VendorBlackRequestVO requestVO);

    /**
     * 获取基本信息
     * @return
     */
    VendorIndexInfoVO getBaseInfo();

    /**
     * 获取供应商履约评价
     * @param id
     * @return
     */
    List<VendorPerformanceListVO> listVendorPerformance(Long id);

    /**
     * 获取供应商列表
     * @param queryVO
     * @return
     */
    PageResult<VendorVO> listVendorJkptht(VendorManagementListQueryVO queryVO);

    /**
     * 校验企业名称
     * @param enterpriseName
     */
    String checkEnterpriseName(String enterpriseName);

    /**
     * 供应商注册电子签章
     * @return
     */
    String vendorSignAuth();

    /**
     * 获取供应商认证状态
     * @return
     */
    VendorSignAuthInfo getVendorSignAuthInfo();

    String checkRegister(Long vendorId);

    void  revokeVendor(Long id);

    void registerLinkman(VendorOneRequestVO requestVO);

    Vendor getByLoginUserTwo(Long userId);

    VendorOneRequestVO getLoginUserDetail(Long loginUserId);

    /**
     * 供应商注册保存
     * @param requestVO
     */
    void registerSave(VendorOneRequestVO requestVO);

    /**
     * 校验企业名称和id
     * @param enterpriseName
     * @param vendorId
     */
    String checkEnterpriseNameAndId(String enterpriseName, Long vendorId);

    void pushVendor(Long vendorId, String logTypeModify, Integer isBlack);

    void initializeCode();
}
