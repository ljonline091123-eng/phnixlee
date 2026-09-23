package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.vendor.domain.VendorClassify;
import com.zhaocai.business.vendor.vo.res.VendorClassifyTreeVO;

import java.util.List;

/**
 * @author ssy
 * @date 2024/7/13 16:00
 */
public interface IVendorClassifyService extends IService<VendorClassify> {

    List<VendorClassifyTreeVO> getVendorClassifyTree();

    /**
     * 获取供应商分类
     * @param ids
     * @return
     */
    String getVendorClassifyName(String ids);

    /**
     * 获取供应商分类子数据集合
     * */
    List<VendorClassify> getVendorClassifySubList(Long id);
}
