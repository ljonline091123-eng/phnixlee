package com.zhaocai.business.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.enums.PriceTypeEnum;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.business.manager.http.dto.res.ContractPlanMaterialListDTO;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.procurement.domain.MaterialsList;
import com.zhaocai.business.procurement.domain.ProcurementPlan;
import com.zhaocai.business.procurement.dto.MaterialsListDTO;
import com.zhaocai.business.procurement.dto.SubjectMatterDTO;
import com.zhaocai.business.procurement.mapper.MaterialsListMapper;
import com.zhaocai.business.procurement.service.IMaterialsListService;
import com.zhaocai.business.procurement.service.IProcurementPlanService;
import com.zhaocai.business.procurement.vo.req.ContractSplitMaterialsQueryVO;
import com.zhaocai.business.procurement.vo.res.CompContractSplitMaterialsVO;
import com.zhaocai.business.procurement.vo.res.CompMaterialsContentVO;
import com.zhaocai.business.procurement.vo.res.ContractSplitMaterialsVO;
import com.zhaocai.business.procurement.vo.res.MaterialsVO;
import com.zhaocai.business.pub.domain.DwMmAssetInf;
import com.zhaocai.business.pub.domain.DwMmServiceInf;
import com.zhaocai.business.pub.service.IDwMmAssetInfService;
import com.zhaocai.business.pub.service.IDwMmServiceInfService;
import com.zhaocai.common.core.constant.Constants;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购物料清单Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class MaterialsListServiceImpl extends ServiceImpl<MaterialsListMapper, MaterialsList> implements IMaterialsListService {

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Autowired
    private IDwMmAssetInfService dwMmAssetInfService;

    @Autowired
    private IDwMmServiceInfService dwMmServiceInfService;

    @Autowired
    @Lazy
    private IProcurementPlanService procurementPlanService;

    /**
     * 交易标的物为钢筋
     */
    @Value(value = "${subject-matter.rebar}")
    private String subjectMatterRebarCode;

    /**
     * 交易标的物为砼
     */
    @Value(value = "${subject-matter.concrete}")
    private String subjectMatterConcreteCode;

    @Override
    public List<MaterialsList> saveMaterialsList(List<MaterialsList> materialsLists, Long contractSplitId, Long planId, ProcurementPlan procurementPlan,Integer[] floatCount,Integer[] fixedCount) {
//        Integer[] floatCount = {0};
//        Integer[] fixedCount = {0};
        materialsLists.forEach(materials -> {
            materials.setPlanId(planId);
            materials.setContractSplitId(contractSplitId);
            materials.setUsedCount(BigDecimal.ZERO);

            /* 是否是 “购买材料” */
            if(procurementPlan.getProcurementPlanType().equals(NumberConstant.ONE)){
                /* 使用清单内每一条设置的 价格类型 */
                if (PriceTypeEnum.FLOAT_PRICE.equalsType(materials.getPriceType())) {
                    // 浮动价 >>> 含税单价 = 基价 + 浮动价 + 卸费
                    BigDecimal unitPriceInclTax = AmountCalUtil.addAmount(materials.getBasePrice(),materials.getBasePrice(),materials.getFloatingPrice(),materials.getUnloadingFee());
                    /* 单价(含税) */
                    materials.setUnitPriceInclTax(unitPriceInclTax);
                    floatCount[0]++;
                }else{
                    fixedCount[0]++;
                }
            }else{
                /* 使用 采购计划 设置的 价格类型 */
                if (PriceTypeEnum.FLOAT_PRICE.equalsType(procurementPlan.getPriceType())) {
                    // 浮动价 >>> 含税单价 = 基价 + 浮动价 + 卸费
                    BigDecimal unitPriceInclTax = AmountCalUtil.addAmount(materials.getBasePrice(),materials.getBasePrice(),materials.getFloatingPrice(),materials.getUnloadingFee());
                    /* 单价(含税) */
                    materials.setUnitPriceInclTax(unitPriceInclTax);
                }
            }
//            /* 使用 采购计划 设置的 价格类型 */
//            if (PriceTypeEnum.FLOAT_PRICE.equalsType(procurementPlan.getPriceType())) {
//                // 浮动价 >>> 含税单价 = 基价 + 浮动价 + 卸费
//                BigDecimal unitPriceInclTax = AmountCalUtil.addAmount(materials.getBasePrice(),materials.getBasePrice(),materials.getFloatingPrice(),materials.getUnloadingFee());
//                /* 单价(含税) */
//                materials.setUnitPriceInclTax(unitPriceInclTax);
//            }

            // 不含税单价 = 含税单价 / (1 + 税率%)
            materials.setUnitPriceExclTax(AmountCalUtil.calUnitPriceExclTax(materials.getUnitPriceInclTax(),materials.getTaxRate()));

            // 含税金额 = 含税单价 * 数量
            materials.setAmountInclTax(AmountCalUtil.calTotalAmountInclTax(materials.getCount(),materials.getUnitPriceInclTax()));

            // 不含税金额 = 含税金额 / (1 * 税率%)
            materials.setAmountExclTax(AmountCalUtil.calTotalAmountExclTax(materials.getAmountInclTax(),materials.getTaxRate()));

            // 税额 = 含税金额 - 不含税金额
            materials.setTaxAmount(AmountCalUtil.calTaxAmount(materials.getAmountInclTax(),materials.getAmountExclTax()));

            baseMapper.insert(materials);
        });
        /* 是否是 “购买材料” */
        if(procurementPlan.getProcurementPlanType().equals(NumberConstant.ONE)){
            if(floatCount[0]>0 && fixedCount[0]>0){
                /* 固定、浮动价 */
                procurementPlan.setPriceType(PriceTypeEnum.FIXED_FLOAT_PRICE.getType());
            }else if(floatCount[0]>0){
                /* 浮动价 */
                procurementPlan.setPriceType(PriceTypeEnum.FLOAT_PRICE.getType());
            }else{
                /* 固定价 */
                procurementPlan.setPriceType(PriceTypeEnum.FIXED_PRICE.getType());
            }
            /* 更新采购计划 */
            procurementPlanService.update(new LambdaUpdateWrapper<ProcurementPlan>()
                    .set(ProcurementPlan::getPriceType,procurementPlan.getPriceType())
                    .eq(ProcurementPlan::getId,procurementPlan.getId()));
        }
        return materialsLists;
    }

    @Override
    public void deleteByPlanId(Long planId) {
        baseMapper.deleteByPlanId(planId);
    }

    @Override
    public List<ContractSplitMaterialsVO> listMaterialsByPlanId(Long planId,Boolean isFilter) {
        List<MaterialsListDTO> materialsList = baseMapper.selectMaterialsListByPlanId(planId);

        if (isFilter) {
            // 过滤掉数量为 0 的清单 和 已推送到易料的清单
            materialsList = materialsList.stream()
                    .filter(x -> NumberUtil.compare(x.getCount(),BigDecimal.ZERO) > 0 && x.getPushFlag().equals("N"))
                    .collect(Collectors.toList());
        }

        Map<String, List<MaterialsList>> map = materialsList.stream()
                 .collect(Collectors.groupingBy( x -> x.getSplitId() + "-_#_-" + x.getSplitContractName() + "-_#_-" +x.getContractScope() ,
                         Collectors.mapping(materials -> BeanCopierUtil.copyBean(materials,MaterialsList.class),
                                 Collectors.toList())));

        Map<String,String> rentModeMap = underlingSystemService.listDictMap(DictBizEnum.UNDERLING_RENT_MODE.getName());

        List<ContractSplitMaterialsVO> resultList = new ArrayList<>(map.size());
        ContractSplitMaterialsVO  splitMaterialsVO;
        for (Map.Entry<String,List<MaterialsList>> entry : map.entrySet()) {
            splitMaterialsVO = new ContractSplitMaterialsVO();
            String[] keys = entry.getKey().split("-_#_-");
            splitMaterialsVO.setSplitId(Long.valueOf(keys[0]));
            splitMaterialsVO.setSplitContractName(keys[1]);
            splitMaterialsVO.setContractScope(keys[2]);

            List<MaterialsVO> materialsLists = BeanCopierUtil.copyList(entry.getValue(), MaterialsVO.class);
            for (MaterialsVO materials : materialsLists) {
                materials.setRentModeText(rentModeMap.get(materials.getRentMode()));
            }

            /* 排序一下 根据 物料编码 */
            materialsLists.stream().sorted(Comparator.comparing(MaterialsVO::getMaterialsCode).reversed()).collect(Collectors.toList());

            splitMaterialsVO.setMaterialsLists(materialsLists);

            resultList.add(splitMaterialsVO);
        }

        // 排序
        resultList.sort(Comparator.comparing(ContractSplitMaterialsVO::getSplitId));

        return resultList;
    }

    @Override
    public List<MaterialsList> listMaterialsListByPlanIds(List<Long> planIds) {
        return super.list(new LambdaQueryWrapper<MaterialsList>()
                .in(MaterialsList::getPlanId,planIds));
    }

    @Override
    public List<MaterialsList> listMaterialsListByContractSplitIds(List<Long> contractSplitIds) {
        List<MaterialsList> materialsLists = super.list(new LambdaQueryWrapper<MaterialsList>()
                .in(MaterialsList::getContractSplitId,contractSplitIds));
        return materialsLists.stream()
                .filter(x -> NumberUtil.compare(x.getCount(),BigDecimal.ZERO) > 0 && x.getPushFlag().equals("N"))
                .collect(Collectors.toList());
    }

    @Override
    public List<ContractSplitMaterialsVO> listContractSplitMaterials(ContractSplitMaterialsQueryVO queryVO) {
        List<ContractSplitMaterialsVO> materialsList = this.listMaterialsByPlanId(queryVO.getPlanId(),true);

        return materialsList.stream()
                .filter(x -> queryVO.getContractSpiltIdList().contains(x.getSplitId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CompContractSplitMaterialsVO> listContractSplitMaterials4Bidding(ContractSplitMaterialsQueryVO queryVO) {
        List<CompContractSplitMaterialsVO> materialsList = this.listMaterialsByPlanId4Bidding(queryVO.getPlanId());

        return materialsList.stream()
                .filter(x -> queryVO.getContractSpiltIdList().contains(x.getSplitId()))
                .collect(Collectors.toList());
    }

    @Override
    public SubjectMatterDTO getSubjectMatter(Integer procurementPlanType, String materialsCode,String contractPlanCode) {
        if (ProcurementPlanTypeEnum.isMaterials(procurementPlanType)) {
            /*
             * 材料类
             * 材料类交易标的物从第 3 级开始找为交易标的物的资产类数据，如果没有找到，则依次往下降，即第 3 级找不到，就找第 2 级、第 2 级找不到就找第一级
             */
            int level = getSubjectMatterLevel(procurementPlanType,materialsCode);
            return getAssetSubjectMatter(materialsCode,level);
        } else if (ProcurementPlanTypeEnum.isService(procurementPlanType)) {
            // 劳务分包、专业分包直接根据合约规划编码从服务类表中找
            DwMmServiceInf dwMmServiceInf = dwMmServiceInfService.getByClassCode(contractPlanCode);
            if (dwMmServiceInf != null) {
                return new SubjectMatterDTO(dwMmServiceInf.getServiceClassCode(),dwMmServiceInf.getServiceClassName());
            }
            throw new BusinessException("该合约规划编码[" + contractPlanCode + "]无对应的交易标的物，请确认");
        } else if (ProcurementPlanTypeEnum.RENTAL_MACHINERY.equalsType(procurementPlanType)) {
            /*
             * 租赁机械设备
             * 和材料类一样，也是从第 3 级开始查询
             * 先根据清单编码去找到对应的记录，获取映射资产分类编码（map_asset_class_code），然后再用这个映射资产编码去找标的物，如果不存在，则降级找
             */
            int level = getSubjectMatterLevel(procurementPlanType,materialsCode);
            return getMachinerySubjectMatter(materialsCode,level);
        } else {
            // 其他
            if (materialsCode.startsWith("A1") || materialsCode.startsWith("A2") || materialsCode.startsWith("A3")) {
                /*
                 * A1：原材料  A2：周转材料  A3：其他货物
                 */
                int level = getSubjectMatterLevel(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(),materialsCode);
                return getAssetSubjectMatter(materialsCode,level);
            } else if (materialsCode.startsWith("A4")){
                // A4：机械设备类
                int level = getSubjectMatterLevel(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(),materialsCode);
                return getMachinerySubjectMatter(materialsCode,level);
            } else {
                return new SubjectMatterDTO(Constants.SUBJECT_MATTER_BLANK,"");
            }
        }
    }

    /**
     * 获取交易标的物的级别
     * @param procurementPlanType
     * @param materialsCode
     * @return
     */
    private int getSubjectMatterLevel(Integer procurementPlanType, String materialsCode) {
        int level = 0;
        if (ProcurementPlanTypeEnum.isMaterials(procurementPlanType)) {
            // 材料类
            if (materialsCode.length() >= 6) {
                level = 3;
            } else if (materialsCode.length() >= 4) {
                level = 2;
            } else if (materialsCode.length() >= 2) {
                level = 1;
            } else {
                throw new BusinessException(String.format("清单编码[%s]格式有误",materialsCode));
            }
        } else if (ProcurementPlanTypeEnum.RENTAL_MACHINERY.equalsType(procurementPlanType)) {
            // 机械设备类
            if (materialsCode.length() >= 8) {
                level = 3;
            } else if (materialsCode.length() >= 5) {
                level = 2;
            } else if (materialsCode.length() >= 2) {
                level = 1;
            } else {
                throw new BusinessException(String.format("清单编码[%s]格式有误",materialsCode));
            }
        }

        return level;
    }

    @Override
    public Integer getSubjectMatterType(String subjectMatterCode) {
        String[] subjectMatterCodes = subjectMatterCode.split(",");

        Set<Integer> subjectMatterValueSet = new HashSet<>();
        for (String code : subjectMatterCodes) {
            if (subjectMatterRebarCode.contains(code)) {
                subjectMatterValueSet.add(1);
            } else if (subjectMatterConcreteCode.contains(code)) {
                subjectMatterValueSet.add(2);
            } else {
                subjectMatterValueSet.add(0);
            }
        }
        if (subjectMatterValueSet.size() > 1) {
            String message = subjectMatterValueSet.stream().map(val -> {
                if (val == 1) {
                    return "钢筋";
                } else if (val == 2) {
                    return "砼";
                } else {
                    return "其他";
                }
            }).collect(Collectors.joining(","));

            throw new ParamValidateException("该合约规划的清单中存在多种交易标的物类型，有:" + message);
        }
        return subjectMatterValueSet.stream().findFirst().get();
    }

    @Override
    public void updateMaterialsListUsedCount(List<MaterialsList> materialsLists) {
        List<MaterialsList> updateList = materialsLists.stream()
                .map(val -> {
                    MaterialsList materialsList = new MaterialsList();
                    materialsList.setId(val.getId());
                    materialsList.setUsedCount(val.getUsedCount());
                    return materialsList;
                }).collect(Collectors.toList());

        this.updateBatchById(updateList);
    }

    /**
     * 获取机械设备的交易标的物
     * @param materialsCode
     * @return
     */
    private SubjectMatterDTO getMachinerySubjectMatter(String materialsCode,int level) {
        if (level == 3) {
            materialsCode = materialsCode.substring(0,8);
        } else if(level == 2){
            materialsCode = materialsCode.substring(0,5);
        } else {
            materialsCode = materialsCode.substring(0,2);
        }

        DwMmAssetInf dwMmAssetInf = dwMmAssetInfService.getOne(new LambdaQueryWrapper<DwMmAssetInf>()
                .eq(DwMmAssetInf::getAssetClassCode,materialsCode));

        if (dwMmAssetInf != null && StringUtils.isNotBlank(dwMmAssetInf.getMapAssetClassCode())) {
            dwMmAssetInf = dwMmAssetInfService.getOne(new LambdaQueryWrapper<DwMmAssetInf>()
                    .eq(DwMmAssetInf::getAssetClassCode,dwMmAssetInf.getMapAssetClassCode())
                    .eq(DwMmAssetInf::getIsSubjectMatterCd,"Y"));
            if (dwMmAssetInf != null) {
                return new SubjectMatterDTO(dwMmAssetInf.getAssetClassCode(),dwMmAssetInf.getAssetClassName());
            }
        }

        // 没有找到，且为第 1 级，则无交易标的物，直接报错
        if (level == 1) {
            throw new BusinessException("清单编码编码[" + materialsCode + "]没有交易标的物，请确认");
        }

        // 降级查询
        return getMachinerySubjectMatter(materialsCode,level - 1);
    }

    /**
     * 获取资产的交易标的物
     * @param materialsCode
     * @return
     */
    private SubjectMatterDTO getAssetSubjectMatter(String materialsCode,int level) {
        /*
         * 第 3 级为 6 位
         * 第 2 级为 4 位
         * 第 1 级为 2 位
         */
        if (level == 3) {
            materialsCode = materialsCode.substring(0,6);
        } else if (level == 2) {
            materialsCode = materialsCode.substring(0,4);
        } else {
            materialsCode = materialsCode.substring(0,2);
        }

        DwMmAssetInf dwMmAssetInf = dwMmAssetInfService.getOne(new LambdaQueryWrapper<DwMmAssetInf>()
                .eq(DwMmAssetInf::getAssetClassCode,materialsCode)
                .eq(DwMmAssetInf::getIsSubjectMatterCd,"Y"));

        if (dwMmAssetInf != null) {
            // 存在，直接返回
            return new SubjectMatterDTO(dwMmAssetInf.getAssetClassCode(),dwMmAssetInf.getAssetClassName());
        }

        // 没有找到，且为第 1 级，则无交易标的物，直接报错
        if (level == 1) {
            throw new BusinessException("清单编码编码[" + materialsCode + "]没有交易标的物，请确认");
        }

        // 降级查询
        return getAssetSubjectMatter(materialsCode,level - 1);
    }


    private List<CompContractSplitMaterialsVO> listMaterialsByPlanId4Bidding(Long planId) {
        List<MaterialsListDTO> materialsList = baseMapper.selectMaterialsListByPlanId(planId);

        materialsList = materialsList.stream()
                .filter(x -> NumberUtil.compare(x.getCount(),BigDecimal.ZERO) > 0 && x.getPushFlag().equals("N"))
                .collect(Collectors.toList());

        Map<String, List<MaterialsList>> map = materialsList.stream()
                .collect(Collectors.groupingBy( x -> x.getSplitId() + "-_#_-" + x.getSplitContractName() + "-_#_-" +x.getContractScope() ,
                        Collectors.mapping(materials -> BeanCopierUtil.copyBean(materials,MaterialsList.class),
                                Collectors.toList())));

        List<CompContractSplitMaterialsVO> resultList = new ArrayList<>(map.size());
        CompContractSplitMaterialsVO  splitMaterialsVO;
        for (Map.Entry<String,List<MaterialsList>> entry : map.entrySet()) {
            splitMaterialsVO = new CompContractSplitMaterialsVO();
            String[] keys = entry.getKey().split("-_#_-");
            splitMaterialsVO.setSplitId(Long.valueOf(keys[0]));
            splitMaterialsVO.setSplitContractName(keys[1]);
            splitMaterialsVO.setContractScope(keys[2]);

            List<CompMaterialsContentVO> materialsLists = BeanCopierUtil.copyList(entry.getValue(), CompMaterialsContentVO.class);

            /* 排序一下 根据 物料编码 */
            materialsLists.stream().sorted(Comparator.comparing(CompMaterialsContentVO::getMaterialsCode).reversed()).collect(Collectors.toList());

            splitMaterialsVO.setMaterialsLists(materialsLists);

            resultList.add(splitMaterialsVO);
        }
        return resultList;
    }
}
