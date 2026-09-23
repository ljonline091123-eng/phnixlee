package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.pub.domain.Country;

import java.util.List;

/**
 * 国家和地区档案Service接口
 *
 * @author WH
 * @date 2024-07-14
 */
public interface ICountryService  extends IService<Country>
{
    /**
     * 查询国家和地区档案
     *
     * @param id 国家和地区档案主键
     * @return 国家和地区档案
     */
    public Country selectCountryById(Long id);

    /**
     * 查询国家和地区档案列表
     *
     * @param country 国家和地区档案
     * @return 国家和地区档案集合
     */
    public List<Country> selectCountryList(Country country);

    /**
     * 新增国家和地区档案
     *
     * @param country 国家和地区档案
     * @return 结果
     */
    public int insertCountry(Country country);

    /**
     * 修改国家和地区档案
     *
     * @param country 国家和地区档案
     * @return 结果
     */
    public int updateCountry(Country country);

    /**
     * 批量删除国家和地区档案
     *
     * @param ids 需要删除的国家和地区档案主键集合
     * @return 结果
     */
    public int deleteCountryByIds(Long[] ids);

    /**
     * 删除国家和地区档案信息
     *
     * @param id 国家和地区档案主键
     * @return 结果
     */
    public int deleteCountryById(Long id);

    Boolean deleteSyncCountry();
}
