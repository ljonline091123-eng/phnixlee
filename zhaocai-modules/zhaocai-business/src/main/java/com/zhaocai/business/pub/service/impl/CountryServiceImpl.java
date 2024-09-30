package com.zhaocai.business.pub.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.pub.domain.Country;
import com.zhaocai.business.pub.mapper.CountryMapper;
import com.zhaocai.business.pub.service.ICountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 国家和地区档案Service业务层处理
 *
 * @author WH
 * @date 2024-07-14
 */
@Service
public class CountryServiceImpl extends ServiceImpl<CountryMapper,Country> implements ICountryService
        {
    @Autowired
    private CountryMapper countryMapper;

    /**
     * 查询国家和地区档案
     *
     * @param id 国家和地区档案主键
     * @return 国家和地区档案
     */
    @Override
    public Country selectCountryById(Long id)
    {
        return countryMapper.selectCountryById(id);
    }

    /**
     * 查询国家和地区档案列表
     *
     * @param country 国家和地区档案
     * @return 国家和地区档案
     */
    @Override
    public List<Country> selectCountryList(Country country)
    {
        return countryMapper.selectCountryList(country);
    }

    /**
     * 新增国家和地区档案
     *
     * @param country 国家和地区档案
     * @return 结果
     */
    @Override
    public int insertCountry(Country country)
    {
        return countryMapper.insertCountry(country);
    }

    /**
     * 修改国家和地区档案
     *
     * @param country 国家和地区档案
     * @return 结果
     */
    @Override
    public int updateCountry(Country country)
    {
        return countryMapper.updateCountry(country);
    }

    /**
     * 批量删除国家和地区档案
     *
     * @param ids 需要删除的国家和地区档案主键
     * @return 结果
     */
    @Override
    public int deleteCountryByIds(Long[] ids)
    {
        return countryMapper.deleteCountryByIds(ids);
    }

    /**
     * 删除国家和地区档案信息
     *
     * @param id 国家和地区档案主键
     * @return 结果
     */
    @Override
    public int deleteCountryById(Long id)
    {
        return countryMapper.deleteCountryById(id);
    }

    @Override
    public Boolean deleteSyncCountry() {
        return baseMapper.deleteSyncCountry();
    }
}
