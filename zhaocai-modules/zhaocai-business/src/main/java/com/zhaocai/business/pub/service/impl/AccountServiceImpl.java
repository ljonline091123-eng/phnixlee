package com.zhaocai.business.pub.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSONObject;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.pub.domain.TAccountInfo;
import com.zhaocai.business.pub.mapper.AccountMapper;
import com.zhaocai.business.pub.service.IAccountService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.pub.vo.req.TAccountInfoVo;
import com.zhaocai.business.vendor.config.DataMiddlePlatformConfig;
import com.zhaocai.business.vendor.domain.TInterfaceLog;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.ITInterfaceLogService;
import com.zhaocai.business.vendor.util.DataCenterUtil;
import com.zhaocai.business.vendor.config.DataMiddlePlatformConfig;
import com.zhaocai.business.vendor.domain.TInterfaceLog;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.ITInterfaceLogService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.util.DataCenterUtil;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 银行账户Service业务层处理
 *
 * @author cs
 * @date 2024-11-20
 */
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, TAccountInfo> implements IAccountService
        {
            @Autowired
            private AccountMapper accountMapper;

            @Autowired
            private ISysDictDataService sysDictDataService;

            @Autowired
            private DataCenterUtil dataCenterUtil;

            @Autowired
            private DataMiddlePlatformConfig dataMiddlePlatformConfig;

            @Autowired
            private ITInterfaceLogService tInterfaceLogService;

            @Lazy
            @Autowired
            private IVendorService vendorService;

            /**
     * 查询银行账户档案
     *
     * @param id 银行账户档案主键
     * @return 银行账户档案
     */
    @Override
    public TAccountInfo selectAccountById(Long id)
    {
        return accountMapper.selectAccountById(id);
    }

    /**
     * 查询银行账户档案列表
     *
     * @param tAccountInfo 银行账户档案
     * @return 银行账户档案
     */
    @Override
    public PageResult<TAccountInfoVo> selectAccountList(TAccountInfoVo tAccountInfo)
    {

        IPage<TAccountInfoVo> list = accountMapper.selectAccountList(tAccountInfo.toMybatisPage(),tAccountInfo);
        return new  PageResult<>(list);
    }

    /**
     * 新增银行账户档案
     *
     * @param tAccountInfo 银行账户档案
     * @return 结果
     */
    @Override
    public int insertAccount(TAccountInfo tAccountInfo)
    {
        return accountMapper.insertAccount(tAccountInfo);
    }

    /**
     * 修改银行账户档案
     *
     * @param tAccountInfo 银行账户档案
     * @return 结果
     */
    @Override
    public int updateAccount(TAccountInfo tAccountInfo)
    {
        return accountMapper.updateAccount(tAccountInfo);
    }

    /**
     * 批量删除银行账户档案
     *
     * @param ids 需要删除的银行账户档案主键
     * @return 结果
     */
    @Override
    public int deleteAccountByIds(Long[] ids)
    {
        return accountMapper.deleteAccountByIds(ids);
    }


            /**
             * 推送银行账户档案信息
             *
             * @param accountInfo 银行账户档案
             * @return 结果
             */
            @Override
            public void push(TAccountInfo accountInfo,String type) {
                if(accountInfo.getUpId()!=null){
                    Vendor vendor = vendorService.getById(accountInfo.getUpId());
                    if(vendor != null){
                        this.pushAcct(accountInfo ,vendor ,vendor.getMiddleVendorCode() ,type);
                    }
                }
            }

            @Override
            public int updateByVendorId(Long id, Long uuid) {
                return baseMapper.updateByVendorId(id,uuid);
            }

            @Override
            public void pushAcct(TAccountInfo bean, Vendor vendor, String custMerchtId, String type) {
                Map<String, Object> mapAcct = new HashMap<>();
                mapAcct.put("internal_id", bean.getId() + "");
                mapAcct.put("internal_ref_id", bean.getUpId()+"");
                mapAcct.put("dept_id",  vendor.getFirstCooperationCompanyCode());
                mapAcct.put("cust_mercht_code",  custMerchtId);//客商编码
                mapAcct.put("cust_mercht_full_name",  vendor.getEnterpriseName());//所属客商全称
                mapAcct.put("cust_mercht_id",  vendor.getId()+"");//所属客商ID
                mapAcct.put("bank_acct_nm",  vendor.getEnterpriseName());//户名
                mapAcct.put("bank_acct",  bean.getBankAccount());//银行账号
                mapAcct.put("unified_soci_crdt_cd",  vendor.getSocialCreditCode());//客商-统一社会信用编码
                mapAcct.put("open_branch_bank_nme",  bean.getOpeningBranch());//开户支行名称
                mapAcct.put("linenumber",  bean.getInterbankNumber());//联行号
                mapAcct.put("belg_bank_nme",  bean.getAffiliatedBank());//所属银行名称
                mapAcct.put("curr_cd_cd",  sysDictDataService.getRemark("currency",bean.getCurrency()+"",null));//币种cd
                mapAcct.put("curr_cd",  sysDictDataService.getRemark("currency",bean.getCurrency()+"","label"));//币种名称
                mapAcct.put("acct_type",  "对公账号");//账户类型
                mapAcct.put("acct_type_cd",  "1");//账户类型cd
                mapAcct.put("is_default_acct_cd",  bean.getStatus() == 1 ? "Y": "N");//默认银行账号cd
                mapAcct.put("is_default_acct",  bean.getStatus() == 1 ? "是": "否");//默认银行账号
                JSONObject jsonObject = new JSONObject(mapAcct);
                if(Vendor.LOG_TYPE_MODIFY.equals(type)){
                    //修改要看是否有新增数据没有就要走新增方法
                    TInterfaceLog log = new TInterfaceLog();
                    log.setBusinessId(bean.getId() + "");
                    log.setFlag("true");
                    List<TInterfaceLog> logList = tInterfaceLogService.selectTInterfaceLogList(log);
                    if (CollectionUtil.isNotEmpty(logList)) {
                        //修改
                        dataCenterUtil.postCommonInfo(jsonObject,dataMiddlePlatformConfig.getVendorAcctUpdate(),Vendor.LOG_TYPE_MODIFY, SecurityUtils.getUsername(),null);
                    } else {
                        //新增
                        dataCenterUtil.postCommonInfo(jsonObject,dataMiddlePlatformConfig.getVendorAcctAdd(),Vendor.LOG_TYPE_ADD, SecurityUtils.getUsername(),null);
                    }

                }else if(Vendor.LOG_TYPE_REMOVE.equals(type)){
                    dataCenterUtil.postCommonInfo(jsonObject,dataMiddlePlatformConfig.getVendorAcctRemove(),type, SecurityUtils.getUsername(),null);
                }else{
                    dataCenterUtil.postCommonInfo(jsonObject,dataMiddlePlatformConfig.getVendorAcctAdd(),Vendor.LOG_TYPE_ADD, SecurityUtils.getUsername(),null);
                }
            }

            /**
     * 删除银行账户档案信息
     *
     * @param id 银行账户档案主键
     * @return 结果
     */
    @Override
    public int deleteAccountById(Long id)
    {
        return accountMapper.deleteAccountById(id);
    }

    @Override
    public Boolean deleteSyncAccount() {
        return baseMapper.deleteSyncAccount();
    }

        }
