package com.zhaocai.business.pub.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.manager.http.common.config.RestTemplateUtils;
import com.zhaocai.business.manager.http.dto.req.UnderlyingPlatformBaseDTO;
import com.zhaocai.business.manager.http.dto.res.UnderlingResultData;
import com.zhaocai.business.manager.http.service.PlatCountryService;
import com.zhaocai.business.manager.template.config.UnderlingPlatformConfig;
import com.zhaocai.business.pub.domain.DwCdBank;
import com.zhaocai.business.pub.mapper.BankMapper;
import com.zhaocai.business.pub.service.IBankService;
import com.zhaocai.common.core.web.bean.thrid.ThridResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 支行档案Service业务层处理
 *
 * @author cs
 * @date 2024-11-20
 */
@Service
public class BankServiceImpl extends ServiceImpl<BankMapper, DwCdBank> implements IBankService
        {
            private static final Logger log = LoggerFactory.getLogger(PlatCountryService.class);


    @Autowired
    private BankMapper bankMapper;

    @Autowired
    private UnderlingPlatformConfig underlingPlatformConfig;


            /**
             * 同步支行档案
             *
             * @return 支行档案
             */
            @Override
            public List<DwCdBank> getBankList() {
                List<DwCdBank> banks = new ArrayList<>();
                UriComponents uriComponents = UriComponentsBuilder.fromUriString(underlingPlatformConfig.getBaseUrl() + "/rest/dwService/selectBank")
                        .queryParam("authCode", underlingPlatformConfig.getAuthCode())
                        .build();
                try {
                    UnderlyingPlatformBaseDTO requestDTO = new UnderlyingPlatformBaseDTO();
                    HttpHeaders headers = new HttpHeaders();
                    // 授权码
                    headers.add("Authorization", requestDTO.getAuthorization());
                    UnderlingResultData< ArrayList<LinkedHashMap>> response = RestTemplateUtils.getForObject2Header(uriComponents.toString(), UnderlingResultData.class, headers);
                    //如果成功返回
                    if (response.getCode() == ThridResultCode.SUCCESS.getCode()){
                       ArrayList<LinkedHashMap> dataList = response.getData();
                        for (LinkedHashMap dataMap : dataList){
                            DwCdBank bank =  JSON.parseObject(JSON.toJSONString(dataMap), new TypeReference<DwCdBank>() {});
                            banks.add(bank);
                        }
                    }
                } catch (Exception ex) {
                    log.error("同步银行账户接口失败:{}", ex.getMessage());
                }
                return banks;
            }

     /**
     * 查询支行档案
     *
     * @param id 支行档案主键
     * @return 支行档案
     */
    @Override
    public DwCdBank selectBankById(Long id)
    {
        return bankMapper.selectBankById(id);
    }

    /**
     * 查询支行档案列表
     *
     * @param dwCdBank 支行档案
     * @return 支行档案
     */
    @Override
    public List<DwCdBank> selectBankList(DwCdBank dwCdBank)
    {
        return bankMapper.selectBankList(dwCdBank);
    }

    /**
     * 新增支行档案
     *
     * @param dwCdBank 支行档案
     * @return 结果
     */
    @Override
    public int insertBank(DwCdBank dwCdBank)
    {
        return bankMapper.insertBank(dwCdBank);
    }

    /**
     * 修改支行档案
     *
     * @param dwCdBank 支行档案
     * @return 结果
     */
    @Override
    public int updateBank(DwCdBank dwCdBank)
    {
        return bankMapper.updateBank(dwCdBank);
    }

    /**
     * 批量删除支行档案
     *
     * @param ids 需要删除的支行档案主键
     * @return 结果
     */
    @Override
    public int deleteBankByIds(Long[] ids)
    {
        return bankMapper.deleteBankByIds(ids);
    }

    /**
     * 删除支行档案信息
     *
     * @param id 支行档案主键
     * @return 结果
     */
    @Override
    public int deleteBankById(Long id)
    {
        return bankMapper.deleteBankById(id);
    }

    @Override
    public Boolean deleteSyncBank() {
        return baseMapper.deleteSyncBank();
    }
}
