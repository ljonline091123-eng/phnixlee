package com.zhaocai.business.vendor.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeListVO;
import com.zhaocai.business.pub.domain.Message;
import com.zhaocai.business.pub.service.IMessageService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.utils.KeyUtils;
import com.zhaocai.business.vendor.domain.TbVendorEvaluate;
import com.zhaocai.business.vendor.mapper.TbVendorEvaluateMapper;
import com.zhaocai.business.vendor.service.ITbVendorEvaluateService;
import com.zhaocai.business.vendor.vo.req.TbVendorEvaluateQueryVo;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 供应商评价Service业务层处理
 *
 * @author xw
 * @date 2025-10-11
 */
@Service
public class TbVendorEvaluateServiceImpl extends ServiceImpl<TbVendorEvaluateMapper, TbVendorEvaluate> implements ITbVendorEvaluateService {

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private IMessageService messageService;

    @Override
    public PageResult<TbVendorEvaluate> listPage(TbVendorEvaluateQueryVo queryVO) {
        Map<String, String> evaluateTimeMap = sysDictDataService.listDictMap(DictBizEnum.evaluate_time.getName());
        IPage<TbVendorEvaluate> iPage = baseMapper.listPage(queryVO.toMybatisPage(), queryVO);
        List<TbVendorEvaluate> records = iPage.getRecords();
        records.stream().forEach(x->{
            if("2".equals(x.getEvaluateType())){
                String year = DateUtils.parseDateToStr(DateUtils.YYYY, x.getEvaluateTime());
                x.setEvaluateTimeTxtName(year+"年"+evaluateTimeMap.get(x.getEvaluateTimeTxt()));
            }else if("3".equals(x.getEvaluateType())){
                x.setEvaluateTimeTxtName(DateUtils.parseDateToStr(DateUtils.YYYY, x.getEvaluateTime()));
            }else{
                x.setEvaluateTimeTxtName(DateUtils.parseDateToStr(DateUtils.YYYY_MM, x.getEvaluateTime()));
            }
        });
        iPage.setRecords(records);
        return new PageResult<>(iPage);
    }

    @Override
    public List<TbVendorEvaluate> getList(TbVendorEvaluateQueryVo queryVO) {
        Map<String, String> sysYesNoMap = sysDictDataService.listDictMap(DictBizEnum.sys_yes_no.getName());
        Map<String, String> evaluateStatusMap = sysDictDataService.listDictMap(DictBizEnum.evaluate_status.getName());
        Map<String, String> evaluateTypeMap = sysDictDataService.listDictMap(DictBizEnum.evaluate_type.getName());
        Map<String, String> evaluateTimeMap = sysDictDataService.listDictMap(DictBizEnum.evaluate_time.getName());
        List<TbVendorEvaluate> list = baseMapper.getList(queryVO);
        list.stream().forEach(x->{
            x.setIsQualifiedName(sysYesNoMap.get(x.getIsQualified()));
            x.setEvaluateStatusName(evaluateStatusMap.get(x.getEvaluateStatus()));
            x.setEvaluateTypeName(evaluateTypeMap.get(x.getEvaluateType()));
            if("2".equals(x.getEvaluateType())){
                String year = DateUtils.parseDateToStr(DateUtils.YYYY, x.getEvaluateTime());
                x.setEvaluateTimeTxtName(year+"年"+evaluateTimeMap.get(x.getEvaluateTimeTxt()));
            }else if("3".equals(x.getEvaluateType())){
                x.setEvaluateTimeTxtName(DateUtils.parseDateToStr(DateUtils.YYYY, x.getEvaluateTime()));
            }else{
                x.setEvaluateTimeTxtName(DateUtils.parseDateToStr(DateUtils.YYYY_MM, x.getEvaluateTime()));
            }
        });


        return list;
    }

    @Override
    public Long getIsAppeal(Long id) {
        return baseMapper.getIsAppeal(id);
    }

    @Override
    public boolean sendMessage(TbVendorEvaluate tbVendorEvaluate) {
        Map<String, String> evaluateTimeMap = sysDictDataService.listDictMap(DictBizEnum.evaluate_time.getName());
        Message message = new Message();
        message.setId(KeyUtils.generateId());
        message.setMessageType(6L);
        message.setMessageTitle(tbVendorEvaluate.getVendorName()+"供应商评价申诉");
        if("2".equals(tbVendorEvaluate.getEvaluateType())){
            String year = DateUtils.parseDateToStr(DateUtils.YYYY, tbVendorEvaluate.getEvaluateTime());
            tbVendorEvaluate.setEvaluateTimeTxtName(year+"年"+evaluateTimeMap.get(tbVendorEvaluate.getEvaluateTimeTxt()));
        }else if("3".equals(tbVendorEvaluate.getEvaluateType())){
            tbVendorEvaluate.setEvaluateTimeTxtName(DateUtils.parseDateToStr(DateUtils.YYYY, tbVendorEvaluate.getEvaluateTime()));
        }else{
            tbVendorEvaluate.setEvaluateTimeTxtName(DateUtils.parseDateToStr(DateUtils.YYYY_MM, tbVendorEvaluate.getEvaluateTime()));
        }
        message.setMessageContent(tbVendorEvaluate.getVendorName()+"对"+tbVendorEvaluate.getEvaluateTimeTxtName()
                +"评价申诉。申述说明："+tbVendorEvaluate.getAppealDescribe());
        message.setReadFlag("0");
        message.setBusinessId(tbVendorEvaluate.getId()+"");
        message.setMsgMan(tbVendorEvaluate.getCreateId());
        message.setMsgManName(tbVendorEvaluate.getCreateBy());
        message.setCreateTime(DateUtils.getNowDate());
        message.setCreateId(SecurityUtils.getUserId());
        message.setCreateBy(SecurityUtils.getUsername());
        message.setDelFlag("0");
        return messageService.saveOrUpdate(message);
    }

}
