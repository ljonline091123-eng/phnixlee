package com.zhaocai.business.vendor.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeListVO;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.vendor.domain.TbVendorEvaluate;
import com.zhaocai.business.vendor.mapper.TbVendorEvaluateMapper;
import com.zhaocai.business.vendor.service.ITbVendorEvaluateService;
import com.zhaocai.business.vendor.vo.req.TbVendorEvaluateQueryVo;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.DateUtils;
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

}
