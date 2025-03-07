package com.zhaocai.archives.process.service.impl;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.enums.AgreementStateEnum;
import com.zhaocai.archives.common.enums.ApproveFlowPromptTemplateEnum;
import com.zhaocai.archives.common.enums.ProcessKeyEnum;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.common.service.UnderlingSystemService;
import com.zhaocai.archives.common.vo.req.BpmInitializeRequestDTO;
import com.zhaocai.archives.common.vo.req.PropertyListRequestDTO;
import com.zhaocai.archives.common.vo.res.BpmInitializeResponseDTO;
import com.zhaocai.archives.dossier.domain.*;
import com.zhaocai.archives.dossier.service.*;
import com.zhaocai.archives.process.domain.MaterialApprove;
import com.zhaocai.archives.process.domain.MaterialItemVo;
import com.zhaocai.archives.process.mapper.MaterialApproveMapper;
import com.zhaocai.archives.process.service.IMaterialApproveService;
import com.zhaocai.archives.pub.service.ISystemUserService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.business.RemoteBusinessProcessService;
import com.zhaocai.system.api.domain.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * 物料审批辅Service业务层处理
 *
 * @author lzq
 * @date 2025-02-12
 */
@Service
public class MaterialApproveServiceImpl extends ServiceImpl<MaterialApproveMapper, MaterialApprove> implements IMaterialApproveService {
    @Autowired
    private MaterialApproveMapper materialApproveMapper;

    @Resource
    private IMaterialTypeService materialTypeService;


    @Resource
    private ILabourTypeService labourTypeService;

    @Resource
    private ISubcontractingTypeService subcontractingTypeService;

    @Resource
    private IDeviceTypeService deviceTypeService;

    @Resource
    private RemoteBusinessProcessService remoteBusinessProcessService;

    @Autowired
    private ISystemUserService systemUserService;

    @Resource
    private UnderlingSystemService underlingSystemService;

    @Resource
    private IMaterialItemService materialItemService;

    @Resource
    private IDeviceItemService deviceItemService;


    @Resource
    private ILabourItemService labourItemService;

    @Resource
    private ISubcontractingItemService subcontractingItemService;

    @Resource
    private IMaterialEigenvalueService iMaterialEigenvalueService;

    @Resource
    private ILabourEigenvalueService labourEigenvalueService;

    @Resource
    private ISubcontractingEigenvalueService subcontractingEigenvalueService;

    @Resource
    private IDeviceEigenvalueService deviceEigenvalueService;

    @Resource
    private IMaterialDetailsService iMaterialDetailsService;

    @Resource
    private IDeviceDetailsService deviceDetailsService;

    @Resource
    private ILabourDetailsService labourDetailsService;

    @Resource
    private ISubcontractingDetailsService subcontractingDetailsService;

    private static final Comparator<MaterialItem> MATERIAL_ITEM_COMPARATOR = Comparator.comparing(MaterialItem::getItemCode);

    private static final Comparator<DeviceItem> DEVICE_ITEM_COMPARATOR = Comparator.comparing(DeviceItem::getItemCode);

    private static final Comparator<LabourItem> LABOUR_ITEM_COMPARATOR = Comparator.comparing(LabourItem::getItemCode);

    private static final Comparator<SubcontractingItem> SUBCONTRACTING_ITEM_COMPARATOR = Comparator.comparing(SubcontractingItem::getItemCode);

    /**
     * 查询物料审批辅
     *
     * @param id 物料审批辅主键
     * @return 物料审批辅
     */
    @Override
    public MaterialApprove selectMaterialApproveById(Long id) {
        MaterialApprove materialApprove = materialApproveMapper.selectMaterialApproveById(id);
        if (MaterialApprove.CL_TYPE.equals(materialApprove.getType())) {
            MaterialItem materialItem = new MaterialItem();
            materialItem.setDelFlag("0");
            materialItem.setWfBatch(materialApprove.getId() + "");
            materialItem.setTypeId(materialApprove.getJoinId());
            List<MaterialItem> materialItems = materialItemService.selectMaterialItemList(materialItem);
            Map<Long, Long> map = new HashMap<>();
            materialItems.forEach(item -> {
                map.put(item.getId(), item.getId());
            });
            MaterialEigenvalue materialEigenvalue = new MaterialEigenvalue();
            materialEigenvalue.setDelFlag("0");
            materialEigenvalue.setWfBatch(materialApprove.getId() + "");
            List<MaterialEigenvalue> eigenvalues = iMaterialEigenvalueService.selectMaterialEigenvalueList(materialEigenvalue);
            if (eigenvalues != null && !eigenvalues.isEmpty()) {
                List<Long> ids = new ArrayList<>();
                eigenvalues.forEach(item -> {
                    ids.add(item.getItemId());
                });
                List<MaterialItem> materialItems1 = materialItemService.listByIds(ids);
                materialItems1.forEach(item -> {
                    if (map.get(item.getId()) == null) {
                        materialItems.add(item);
                    }
                });
            }
            Collections.sort(materialItems, MATERIAL_ITEM_COMPARATOR);
            List<MaterialItemVo> materialItemVos = JSON.parseArray(JSON.toJSONString(materialItems), MaterialItemVo.class);
            materialApprove.setMaterialItemVos(materialItemVos);
        } else if (MaterialApprove.SB_TYPE.equals(materialApprove.getType())) {
            DeviceItem materialItem = new DeviceItem();
            materialItem.setDelFlag("0");
            materialItem.setWfBatch(materialApprove.getId() + "");
            materialItem.setTypeId(materialApprove.getJoinId());
            List<DeviceItem> materialItems = deviceItemService.selectDeviceItemList(materialItem);
            Map<Long, Long> map = new HashMap<>();
            materialItems.forEach(item -> {
                map.put(item.getId(), item.getId());
            });
            DeviceEigenvalue materialEigenvalue = new DeviceEigenvalue();
            materialEigenvalue.setDelFlag("0");
            materialEigenvalue.setWfBatch(materialApprove.getId() + "");
            List<DeviceEigenvalue> eigenvalues = deviceEigenvalueService.selectDeviceEigenvalueList(materialEigenvalue);
            if (eigenvalues != null && !eigenvalues.isEmpty()) {
                List<Long> ids = new ArrayList<>();
                eigenvalues.forEach(item -> {
                    ids.add(item.getItemId());
                });
                List<DeviceItem> materialItems1 = deviceItemService.listByIds(ids);
                materialItems1.forEach(item -> {
                    if (map.get(item.getId()) == null) {
                        materialItems.add(item);
                    }
                });
            }
            Collections.sort(materialItems, DEVICE_ITEM_COMPARATOR);
            List<MaterialItemVo> materialItemVos = JSON.parseArray(JSON.toJSONString(materialItems), MaterialItemVo.class);
            materialApprove.setMaterialItemVos(materialItemVos);
        } else if (MaterialApprove.LW_TYPE.equals(materialApprove.getType())) {
            LabourItem materialItem = new LabourItem();
            materialItem.setDelFlag("0");
            materialItem.setWfBatch(materialApprove.getId() + "");
            materialItem.setTypeId(materialApprove.getJoinId());
            List<LabourItem> materialItems = labourItemService.selectLabourItemList(materialItem);
            Map<Long, Long> map = new HashMap<>();
            materialItems.forEach(item -> {
                map.put(item.getId(), item.getId());
            });
            LabourEigenvalue materialEigenvalue = new LabourEigenvalue();
            materialEigenvalue.setDelFlag("0");
            materialEigenvalue.setWfBatch(materialApprove.getId() + "");
            List<LabourEigenvalue> eigenvalues = labourEigenvalueService.selectLabourEigenvalueList(materialEigenvalue);
            if (eigenvalues != null && !eigenvalues.isEmpty()) {
                List<Long> ids = new ArrayList<>();
                eigenvalues.forEach(item -> {
                    ids.add(item.getItemId());
                });
                List<LabourItem> materialItems1 = labourItemService.listByIds(ids);
                materialItems1.forEach(item -> {
                    if (map.get(item.getId()) == null) {
                        materialItems.add(item);
                    }
                });
            }
            Collections.sort(materialItems, LABOUR_ITEM_COMPARATOR);
            List<MaterialItemVo> materialItemVos = JSON.parseArray(JSON.toJSONString(materialItems), MaterialItemVo.class);
            materialApprove.setMaterialItemVos(materialItemVos);
        } else if (MaterialApprove.ZYFB_TYPE.equals(materialApprove.getType())) {
            SubcontractingItem materialItem = new SubcontractingItem();
            materialItem.setDelFlag("0");
            materialItem.setWfBatch(materialApprove.getId() + "");
            materialItem.setTypeId(materialApprove.getJoinId());
            List<SubcontractingItem> materialItems = subcontractingItemService.selectSubcontractingItemList(materialItem);
            Map<Long, Long> map = new HashMap<>();
            materialItems.forEach(item -> {
                map.put(item.getId(), item.getId());
            });
            SubcontractingEigenvalue materialEigenvalue = new SubcontractingEigenvalue();
            materialEigenvalue.setDelFlag("0");
            materialEigenvalue.setWfBatch(materialApprove.getId() + "");
            List<SubcontractingEigenvalue> eigenvalues = subcontractingEigenvalueService.selectSubcontractingEigenvalueList(materialEigenvalue);
            if (eigenvalues != null && !eigenvalues.isEmpty()) {
                List<Long> ids = new ArrayList<>();
                eigenvalues.forEach(item -> {
                    ids.add(item.getItemId());
                });
                List<SubcontractingItem> materialItems1 = subcontractingItemService.listByIds(ids);
                materialItems1.forEach(item -> {
                    if (map.get(item.getId()) == null) {
                        materialItems.add(item);
                    }
                });
            }
            Collections.sort(materialItems, SUBCONTRACTING_ITEM_COMPARATOR);
            List<MaterialItemVo> materialItemVos = JSON.parseArray(JSON.toJSONString(materialItems), MaterialItemVo.class);
            materialApprove.setMaterialItemVos(materialItemVos);
        }
        return materialApprove;
    }

    /**
     * 查询物料审批辅列表
     *
     * @param materialApprove 物料审批辅
     * @return 物料审批辅
     */
    @Override
    public List<MaterialApprove> selectMaterialApproveList(MaterialApprove materialApprove) {
        return materialApproveMapper.selectMaterialApproveList(materialApprove);
    }

    /**
     * 新增物料审批辅
     *
     * @param materialApprove 物料审批辅
     * @return 结果
     */
    @Override
    public int insertMaterialApprove(MaterialApprove materialApprove) {
        materialApprove.setCreateTime(DateUtils.getNowDate());
        return materialApproveMapper.insertMaterialApprove(materialApprove);
    }

    /**
     * 修改物料审批辅
     *
     * @param materialApprove 物料审批辅
     * @return 结果
     */
    @Override
    public int updateMaterialApprove(MaterialApprove materialApprove) {
        materialApprove.setUpdateTime(DateUtils.getNowDate());
        return materialApproveMapper.updateMaterialApprove(materialApprove);
    }

    /**
     * 批量删除物料审批辅
     *
     * @param ids 需要删除的物料审批辅主键
     * @return 结果
     */
    @Override
    public int deleteMaterialApproveByIds(Long[] ids) {
        return materialApproveMapper.deleteMaterialApproveByIds(ids);
    }

    /**
     * 删除物料审批辅信息
     *
     * @param id 物料审批辅主键
     * @return 结果
     */
    @Override
    public int deleteMaterialApproveById(Long id) {
        return materialApproveMapper.deleteMaterialApproveById(id);
    }


    /**
     * 提交材料流程
     *
     * @param materialApprove
     * @return
     */
    @Override
    public boolean submit(MaterialApprove materialApprove) {
        if (materialApprove == null) {
            throw new RuntimeException("参数为空");
        }
        if (materialApprove.getJoinId() == null) {
            throw new RuntimeException("业务id不能为空");
        }
        if (StringUtils.isEmpty(materialApprove.getType())) {
            throw new RuntimeException("业务类型不能为空");
        }
        String type = materialApprove.getType();
        materialApprove.setId(KeyUtils.generateId());
        materialApprove.setCreateId(SecurityUtils.getUserId());
        materialApprove.setCreateBy(SecurityUtils.getUsername());
        materialApprove.setCreateTime(DateUtils.getNowDate());
        materialApprove.setState(0L);
        if (MaterialApprove.CL_TYPE.equals(type)) {
            MaterialType materialType = materialTypeService.selectMaterialTypeById(materialApprove.getJoinId());
            if (materialType.getState() != 0L && materialType.getState() != 5L && materialType.getState() != 3L) {
                throw new RuntimeException("该材料分类无法再次提交");
            }
            int i = materialTypeService.getMaterialJoin(materialType.getId());
            if (i <= 0) {
                throw new RuntimeException("该材料分类下没有需要提交的相关数据，请检查后重试");
            }
            if (materialType.getState() == 5L) {
                String wfBatch = materialType.getWfBatch();
                materialApprove = baseMapper.selectMaterialApproveById(Long.parseLong(wfBatch));
            } else {
                baseMapper.insertMaterialApprove(materialApprove);
            }
            Map<String, Object> params = JSON.parseObject(JSON.toJSONString(materialType), Map.class);
            params.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_MATERIAL.getIdentifying());
//        params.put("businessId", materialType.getWfProcessId());
            params.put("businessId", materialApprove.getId());
            params.put("businessTitle", "材料档案审核");
            params.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.DOSSIER_MATERIAL_PUSH.getDesc(), materialType.getMaterialName()));
            SysUser sysUser = systemUserService.getUserById(materialType.getCreateId());
            /* 根据组织获取对应的二级单位 */
            String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
            //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
            String customProcessKey = ProcessKeyEnum.ZHAOCAI_ASSISTANT_MATERIAL.getIdentifying().replace("{org}", materialType.getOrganCode());
            params.put("customProcessKey", customProcessKey);
            params.put("detailUrl", "/archives-approve/" + Base64.encodeStr(("\"" + materialApprove.getId().toString() + "\"").getBytes(), true, true));
//        UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.name()).businessId(expert.getId().toString()).toDoType(ToDoTypeEnum.EXAMINE.name()).build();
//        params.put("userObj", JSON.toJSONString(userObj));

            /* 获取三级单位 */
//        if(orgThree==null)orgThree = org;
            /* 流程角色配置规则传参 */
            params.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            params.put("companyId", org);/* 公司 二级单位 */
//        params.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
            params.put("parentProjectCode", org);/* 父项目编码(项目部) */
            params.put("Authorization", SecurityUtils.getMasterControlToken());
            ResultData<String> stringResultData = remoteBusinessProcessService.startProcess(JSONObject.parseObject(JSON.toJSONString(params)));
            if ("200".equals(stringResultData.getCode() + "")) {
                return true;
            } else {
                throw new RuntimeException(stringResultData.getMsg());
            }
        } else if (MaterialApprove.SB_TYPE.equals(type)) {
            DeviceType materialType = deviceTypeService.selectDeviceTypeById(materialApprove.getJoinId());
            if (materialType.getState() != 0L && materialType.getState() != 5L && materialType.getState() != 3L) {
                throw new RuntimeException("该设备分类无法再次提交");
            }
            int i = deviceTypeService.getMaterialJoin(materialType.getId());
            if (i <= 0) {
                throw new RuntimeException("该设备分类下没有需要提交的相关数据，请检查后重试");
            }
            if (materialType.getState() == 5L) {
                String wfBatch = materialType.getWfBatch();
                materialApprove = baseMapper.selectMaterialApproveById(Long.parseLong(wfBatch));
            } else {
                baseMapper.insertMaterialApprove(materialApprove);
            }
            Map<String, Object> params = JSON.parseObject(JSON.toJSONString(materialType), Map.class);
            params.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_DEVICE.getIdentifying());
//        params.put("businessId", materialType.getWfProcessId());
            params.put("businessId", materialApprove.getId());
            params.put("businessTitle", "设备档案审核");
            params.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.DOSSIER_DEVICE_PUSH.getDesc(), materialType.getDeviceName()));
            SysUser sysUser = systemUserService.getUserById(materialType.getCreateId());
            /* 根据组织获取对应的二级单位 */
            String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
            //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
            String customProcessKey = ProcessKeyEnum.ZHAOCAI_ASSISTANT_DEVICE.getIdentifying().replace("{org}", materialType.getOrganCode());
            params.put("customProcessKey", customProcessKey);
            params.put("detailUrl", "/archives-approve/" + Base64.encodeStr(("\"" + materialApprove.getId().toString() + "\"").getBytes(), true, true));
//        UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.name()).businessId(expert.getId().toString()).toDoType(ToDoTypeEnum.EXAMINE.name()).build();
//        params.put("userObj", JSON.toJSONString(userObj));

            /* 获取三级单位 */
//        if(orgThree==null)orgThree = org;
            /* 流程角色配置规则传参 */
            params.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            params.put("companyId", org);/* 公司 二级单位 */
//        params.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
            params.put("parentProjectCode", org);/* 父项目编码(项目部) */
            params.put("Authorization", SecurityUtils.getMasterControlToken());
            ResultData<String> stringResultData = remoteBusinessProcessService.startProcess(JSONObject.parseObject(JSON.toJSONString(params)));
            if ("200".equals(stringResultData.getCode() + "")) {
                return true;
            } else {
                throw new RuntimeException(stringResultData.getMsg());
            }
        } else if (MaterialApprove.LW_TYPE.equals(type)) {
            LabourType materialType = labourTypeService.selectLabourTypeById(materialApprove.getJoinId());
            if (materialType.getState() != 0L && materialType.getState() != 5L && materialType.getState() != 3L) {
                throw new RuntimeException("该劳务分类无法再次提交");
            }
            int i = labourTypeService.getMaterialJoin(materialType.getId());
            if (i <= 0) {
                throw new RuntimeException("该劳务分类下没有需要提交的相关数据，请检查后重试");
            }
            if (materialType.getState() == 5L) {
                String wfBatch = materialType.getWfBatch();
                materialApprove = baseMapper.selectMaterialApproveById(Long.parseLong(wfBatch));
            } else {
                baseMapper.insertMaterialApprove(materialApprove);
            }
            Map<String, Object> params = JSON.parseObject(JSON.toJSONString(materialType), Map.class);
            params.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_LABOUR.getIdentifying());
//        params.put("businessId", materialType.getWfProcessId());
            params.put("businessId", materialApprove.getId());
            params.put("businessTitle", "劳务档案审核");
            params.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.DOSSIER_LABOUR_PUSH.getDesc(), materialType.getLabourName()));
            SysUser sysUser = systemUserService.getUserById(materialType.getCreateId());
            /* 根据组织获取对应的二级单位 */
            String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
            //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
            String customProcessKey = ProcessKeyEnum.ZHAOCAI_ASSISTANT_LABOUR.getIdentifying().replace("{org}", materialType.getOrganCode());
            params.put("customProcessKey", customProcessKey);
            params.put("detailUrl", "/archives-approve/" + Base64.encodeStr(("\"" + materialApprove.getId().toString() + "\"").getBytes(), true, true));
//        UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.name()).businessId(expert.getId().toString()).toDoType(ToDoTypeEnum.EXAMINE.name()).build();
//        params.put("userObj", JSON.toJSONString(userObj));

            /* 获取三级单位 */
//        if(orgThree==null)orgThree = org;
            /* 流程角色配置规则传参 */
            params.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            params.put("companyId", org);/* 公司 二级单位 */
//        params.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
            params.put("parentProjectCode", org);/* 父项目编码(项目部) */
            params.put("Authorization", SecurityUtils.getMasterControlToken());
            ResultData<String> stringResultData = remoteBusinessProcessService.startProcess(JSONObject.parseObject(JSON.toJSONString(params)));
            if ("200".equals(stringResultData.getCode() + "")) {
                return true;
            } else {
                throw new RuntimeException(stringResultData.getMsg());
            }
        } else if (MaterialApprove.ZYFB_TYPE.equals(type)) {
            SubcontractingType materialType = subcontractingTypeService.selectSubcontractingTypeById(materialApprove.getJoinId());
            if (materialType.getState() != 0L && materialType.getState() != 5L && materialType.getState() != 3L) {
                throw new RuntimeException("该专业分包分类无法再次提交");
            }
            int i = subcontractingTypeService.getMaterialJoin(materialType.getId());
            if (i <= 0) {
                throw new RuntimeException("该专业分包分类下没有需要提交的相关数据，请检查后重试");
            }
            if (materialType.getState() == 5L) {
                String wfBatch = materialType.getWfBatch();
                materialApprove = baseMapper.selectMaterialApproveById(Long.parseLong(wfBatch));
            } else {
                baseMapper.insertMaterialApprove(materialApprove);
            }
            Map<String, Object> params = JSON.parseObject(JSON.toJSONString(materialType), Map.class);
            params.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_SUBCONTRACTING.getIdentifying());
//        params.put("businessId", materialType.getWfProcessId());
            params.put("businessId", materialApprove.getId());
            params.put("businessTitle", "专业分包档案审核");
            params.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.DOSSIER_SUB_PUSH.getDesc(), materialType.getSubcontractingName()));
            SysUser sysUser = systemUserService.getUserById(materialType.getCreateId());
            /* 根据组织获取对应的二级单位 */
            String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
            //供应商注册时候选择审批单位，只能由选择的单位维护的供应商审核人员进行审核，如果供应商信息修改也是需要原审核单位进行审核
            String customProcessKey = ProcessKeyEnum.ZHAOCAI_ASSISTANT_SUBCONTRACTING.getIdentifying().replace("{org}", materialType.getOrganCode());
            params.put("customProcessKey", customProcessKey);
            params.put("detailUrl", "/archives-approve/" + Base64.encodeStr(("\"" + materialApprove.getId().toString() + "\"").getBytes(), true, true));
//        UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.name()).businessId(expert.getId().toString()).toDoType(ToDoTypeEnum.EXAMINE.name()).build();
//        params.put("userObj", JSON.toJSONString(userObj));

            /* 获取三级单位 */
//        if(orgThree==null)orgThree = org;
            /* 流程角色配置规则传参 */
            params.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            params.put("companyId", org);/* 公司 二级单位 */
//        params.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
            params.put("parentProjectCode", org);/* 父项目编码(项目部) */
            params.put("Authorization", SecurityUtils.getMasterControlToken());
            ResultData<String> stringResultData = remoteBusinessProcessService.startProcess(JSONObject.parseObject(JSON.toJSONString(params)));
            if ("200".equals(stringResultData.getCode() + "")) {
                return true;
            } else {
                throw new RuntimeException(stringResultData.getMsg());
            }
        }
        return false;
    }

    /**
     * 流程提交回調
     *
     * @param variables
     */
    @Override
    public void processStart(Map<String, Object> variables) {
        String processId = variables.get("processId").toString();
        String businessId = variables.get("businessId").toString();
        Object flagObj = variables.get("completedFlag");
        Integer agreementState = AgreementStateEnum.IN_APPROVAL.getState();
        if (!ObjectUtils.isEmpty(flagObj) && AgreementStateEnum.APPROVE.getDesc().equals(flagObj.toString())) {
            agreementState = AgreementStateEnum.APPROVE.getState();
        }
        MaterialApprove materialApprove = baseMapper.selectMaterialApproveById(Long.parseLong(businessId));
        super.update(new LambdaUpdateWrapper<MaterialApprove>()
                .set(MaterialApprove::getWfProcessId, processId)
                .set(MaterialApprove::getState, agreementState)
                .eq(MaterialApprove::getId, businessId));

        String batch = materialApprove.getId() + "";
        if (MaterialApprove.SB_TYPE.equals(materialApprove.getType())) {
            DeviceType materialType = deviceTypeService.selectDeviceTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setWfProcessId(processId);
            materialType.setState(Long.valueOf(agreementState));
            materialType.setWfBatch(batch);
            deviceTypeService.updateDeviceTypeNoChange(materialType);


            //循环调用问题处理添加
            // spring:
            //  main:
            //    allow-circular-references:true
            QueryWrapper<DeviceItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.in("state", Arrays.asList(0L, 5L));
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<DeviceItem> materialItems = deviceItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setWfProcessId(processId);
                materialItem.setWfBatch(batch);
                materialItem.setState(1L);
            });
            deviceItemService.updateBatchById(materialItems);

            QueryWrapper<DeviceEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.in("state", Arrays.asList(0L, 5L));
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<DeviceEigenvalue> eigenvalues = deviceEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setWfProcessId(processId);
                materialEigenvalue.setWfBatch(batch);
                materialEigenvalue.setState(1L);
            });
            deviceEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<DeviceDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.in("state", Arrays.asList(0L, 5L));
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<DeviceDetails> materialDetails = deviceDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setWfProcessId(processId);
                materialDetails1.setWfBatch(batch);
                materialDetails1.setState(1L);
            });
            deviceDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.CL_TYPE.equals(materialApprove.getType())) {
            MaterialType materialType = materialTypeService.selectMaterialTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setWfProcessId(processId);
            materialType.setState(Long.valueOf(agreementState));
            materialType.setWfBatch(batch);
            materialTypeService.updateMaterialTypeNoChange(materialType);


            //循环调用问题处理添加
            // spring:
            //  main:
            //    allow-circular-references:true
            QueryWrapper<MaterialItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.in("state", Arrays.asList(0L, 5L));
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<MaterialItem> materialItems = materialItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setWfProcessId(processId);
                materialItem.setWfBatch(batch);
                materialItem.setState(1L);
            });
            materialItemService.updateBatchById(materialItems);

            QueryWrapper<MaterialEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.in("state", Arrays.asList(0L, 5L));
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<MaterialEigenvalue> eigenvalues = iMaterialEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setWfProcessId(processId);
                materialEigenvalue.setWfBatch(batch);
                materialEigenvalue.setState(1L);
            });
            iMaterialEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<MaterialDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.in("state", Arrays.asList(0L, 5L));
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<MaterialDetails> materialDetails = iMaterialDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setWfProcessId(processId);
                materialDetails1.setWfBatch(batch);
                materialDetails1.setState(1L);
            });
            iMaterialDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.LW_TYPE.equals(materialApprove.getType())) {
            LabourType materialType = labourTypeService.selectLabourTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setWfProcessId(processId);
            materialType.setState(Long.valueOf(agreementState));
            materialType.setWfBatch(batch);
            labourTypeService.updateLabourTypeNoChange(materialType);


            //循环调用问题处理添加
            // spring:
            //  main:
            //    allow-circular-references:true
            QueryWrapper<LabourItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.in("state", Arrays.asList(0L, 5L));
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<LabourItem> materialItems = labourItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setWfProcessId(processId);
                materialItem.setWfBatch(batch);
                materialItem.setState(1L);
            });
            labourItemService.updateBatchById(materialItems);

            QueryWrapper<LabourEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.in("state", Arrays.asList(0L, 5L));
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<LabourEigenvalue> eigenvalues = labourEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setWfProcessId(processId);
                materialEigenvalue.setWfBatch(batch);
                materialEigenvalue.setState(1L);
            });
            labourEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<LabourDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.in("state", Arrays.asList(0L, 5L));
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<LabourDetails> materialDetails = labourDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setWfProcessId(processId);
                materialDetails1.setWfBatch(batch);
                materialDetails1.setState(1L);
            });
            labourDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.ZYFB_TYPE.equals(materialApprove.getType())) {
            SubcontractingType materialType = subcontractingTypeService.selectSubcontractingTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setWfProcessId(processId);
            materialType.setState(Long.valueOf(agreementState));
            materialType.setWfBatch(batch);
            subcontractingTypeService.updateSubcontractingTypeNoChange(materialType);


            //循环调用问题处理添加
            // spring:
            //  main:
            //    allow-circular-references:true
            QueryWrapper<SubcontractingItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.in("state", Arrays.asList(0L, 5L));
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<SubcontractingItem> materialItems = subcontractingItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setWfProcessId(processId);
                materialItem.setWfBatch(batch);
                materialItem.setState(1L);
            });
            subcontractingItemService.updateBatchById(materialItems);

            QueryWrapper<SubcontractingEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.in("state", Arrays.asList(0L, 5L));
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<SubcontractingEigenvalue> eigenvalues = subcontractingEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setWfProcessId(processId);
                materialEigenvalue.setWfBatch(batch);
                materialEigenvalue.setState(1L);
            });
            subcontractingEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<SubcontractingDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.in("state", Arrays.asList(0L, 5L));
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<SubcontractingDetails> materialDetails = subcontractingDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setWfProcessId(processId);
                materialDetails1.setWfBatch(batch);
                materialDetails1.setState(1L);
            });
            subcontractingDetailsService.updateBatchById(materialDetails);
        }
    }

    @Override
    public String audit(String processKey, JSONObject variables) {
        SysUser sysUser;
        MaterialApprove materialApprove = this.getOne(new LambdaQueryWrapper<MaterialApprove>()
                .eq(MaterialApprove::getId, variables.get("businessId")));
        if (materialApprove == null) {
            throw new BusinessException("参数异常");
        }
        if (MaterialApprove.CL_TYPE.equals(materialApprove.getType())) {
            MaterialType expertChange = materialTypeService.selectMaterialTypeById(materialApprove.getJoinId());
            if (expertChange == null) {
                throw new BusinessException("参数异常");
            }
            sysUser = systemUserService.getUserById(expertChange.getCreateId());
            /* 根据组织获取对应的二级单位 */
            String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
            /* 获取三级单位 */
            String orgThree = underlingSystemService.getL3OrgByOrgId(sysUser.getThridOrgId());
            if (orgThree == null) orgThree = org;
            /* 流程角色配置规则传参 */
            variables.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            variables.put("companyId", org);/* 公司 二级单位 */
            variables.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
            variables.put("parentProjectCode", org);/* 父项目编码(项目部) */
            variables.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_MATERIAL.getIdentifying());
            variables.put("Authorization", SecurityUtils.getMasterControlToken());
            ResultData<String> stringResultData = remoteBusinessProcessService.auditProcess(variables);
            if ("200".equals(stringResultData.getCode() + "")) {
                return stringResultData.getData();
            } else {
                throw new RuntimeException(stringResultData.getMsg());
            }
        } else if (MaterialApprove.SB_TYPE.equals(materialApprove.getType())) {
            DeviceType expertChange = deviceTypeService.selectDeviceTypeById(materialApprove.getJoinId());
            if (expertChange == null) {
                throw new BusinessException("参数异常");
            }
            sysUser = systemUserService.getUserById(expertChange.getCreateId());
            /* 根据组织获取对应的二级单位 */
            String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
            /* 获取三级单位 */
            String orgThree = underlingSystemService.getL3OrgByOrgId(sysUser.getThridOrgId());
            if (orgThree == null) orgThree = org;
            /* 流程角色配置规则传参 */
            variables.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            variables.put("companyId", org);/* 公司 二级单位 */
            variables.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
            variables.put("parentProjectCode", org);/* 父项目编码(项目部) */
            variables.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_DEVICE.getIdentifying());
            variables.put("Authorization", SecurityUtils.getMasterControlToken());
            ResultData<String> stringResultData = remoteBusinessProcessService.auditProcess(variables);
            if ("200".equals(stringResultData.getCode() + "")) {
                return stringResultData.getData();
            } else {
                throw new RuntimeException(stringResultData.getMsg());
            }
        } else if (MaterialApprove.LW_TYPE.equals(materialApprove.getType())) {
            LabourType expertChange = labourTypeService.selectLabourTypeById(materialApprove.getJoinId());
            if (expertChange == null) {
                throw new BusinessException("参数异常");
            }
            sysUser = systemUserService.getUserById(expertChange.getCreateId());
            /* 根据组织获取对应的二级单位 */
            String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
            /* 获取三级单位 */
            String orgThree = underlingSystemService.getL3OrgByOrgId(sysUser.getThridOrgId());
            if (orgThree == null) orgThree = org;
            /* 流程角色配置规则传参 */
            variables.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            variables.put("companyId", org);/* 公司 二级单位 */
            variables.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
            variables.put("parentProjectCode", org);/* 父项目编码(项目部) */
            variables.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_LABOUR.getIdentifying());
            variables.put("Authorization", SecurityUtils.getMasterControlToken());
            ResultData<String> stringResultData = remoteBusinessProcessService.auditProcess(variables);
            if ("200".equals(stringResultData.getCode() + "")) {
                return stringResultData.getData();
            } else {
                throw new RuntimeException(stringResultData.getMsg());
            }
        } else if (MaterialApprove.ZYFB_TYPE.equals(materialApprove.getType())) {
            SubcontractingType expertChange = subcontractingTypeService.selectSubcontractingTypeById(materialApprove.getJoinId());
            if (expertChange == null) {
                throw new BusinessException("参数异常");
            }
            sysUser = systemUserService.getUserById(expertChange.getCreateId());
            /* 根据组织获取对应的二级单位 */
            String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
            /* 获取三级单位 */
            String orgThree = underlingSystemService.getL3OrgByOrgId(sysUser.getThridOrgId());
            if (orgThree == null) orgThree = org;
            /* 流程角色配置规则传参 */
            variables.put("groupId", UserConstants.GROUP_DEPT_ID);/* 集团 */
            variables.put("companyId", org);/* 公司 二级单位 */
            variables.put("responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
            variables.put("parentProjectCode", org);/* 父项目编码(项目部) */
            variables.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_SUBCONTRACTING.getIdentifying());
            variables.put("Authorization", SecurityUtils.getMasterControlToken());
            ResultData<String> stringResultData = remoteBusinessProcessService.auditProcess(variables);
            if ("200".equals(stringResultData.getCode() + "")) {
                return stringResultData.getData();
            } else {
                throw new RuntimeException(stringResultData.getMsg());
            }
        }
        return null;
    }

    /**
     * 审核通过
     *
     * @param variables
     */
    @Override
    public void processAuditPass(Map<String, Object> variables) {
        Integer state = AgreementStateEnum.APPROVE.getState();
        MaterialApprove materialApprove = this.getOne(new LambdaQueryWrapper<MaterialApprove>()
                .eq(MaterialApprove::getId, variables.get("businessId")));

        super.update(new LambdaUpdateWrapper<MaterialApprove>()
                .set(MaterialApprove::getState, state)
                .eq(MaterialApprove::getId, variables.get("businessId")));

        if (MaterialApprove.CL_TYPE.equals(materialApprove.getType())) {
            MaterialType materialType = materialTypeService.selectMaterialTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(state));
            materialTypeService.updateMaterialTypeNoChange(materialType);

            QueryWrapper<MaterialItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<MaterialItem> materialItems = materialItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(Long.valueOf(state));
            });
            materialItemService.updateBatchById(materialItems);

            QueryWrapper<MaterialEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<MaterialEigenvalue> eigenvalues = iMaterialEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(Long.valueOf(state));
            });
            iMaterialEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<MaterialDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<MaterialDetails> materialDetails = iMaterialDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(Long.valueOf(state));
            });
            iMaterialDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.SB_TYPE.equals(materialApprove.getType())) {
            DeviceType materialType = deviceTypeService.selectDeviceTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(state));
            deviceTypeService.updateDeviceTypeNoChange(materialType);

            QueryWrapper<DeviceItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<DeviceItem> materialItems = deviceItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(Long.valueOf(state));
            });
            deviceItemService.updateBatchById(materialItems);

            QueryWrapper<DeviceEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<DeviceEigenvalue> eigenvalues = deviceEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(Long.valueOf(state));
            });
            deviceEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<DeviceDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<DeviceDetails> materialDetails = deviceDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(Long.valueOf(state));
            });
            deviceDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.LW_TYPE.equals(materialApprove.getType())) {
            LabourType materialType = labourTypeService.selectLabourTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(state));
            labourTypeService.updateLabourTypeNoChange(materialType);

            QueryWrapper<LabourItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<LabourItem> materialItems = labourItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(Long.valueOf(state));
            });
            labourItemService.updateBatchById(materialItems);

            QueryWrapper<LabourEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<LabourEigenvalue> eigenvalues = labourEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(Long.valueOf(state));
            });
            labourEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<LabourDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<LabourDetails> materialDetails = labourDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(Long.valueOf(state));
            });
            labourDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.ZYFB_TYPE.equals(materialApprove.getType())) {
            SubcontractingType materialType = subcontractingTypeService.selectSubcontractingTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(state));
            subcontractingTypeService.updateSubcontractingTypeNoChange(materialType);

            QueryWrapper<SubcontractingItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<SubcontractingItem> materialItems = subcontractingItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(Long.valueOf(state));
            });
            subcontractingItemService.updateBatchById(materialItems);

            QueryWrapper<SubcontractingEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<SubcontractingEigenvalue> eigenvalues = subcontractingEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(Long.valueOf(state));
            });
            subcontractingEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<SubcontractingDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<SubcontractingDetails> materialDetails = subcontractingDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(Long.valueOf(state));
            });
            subcontractingDetailsService.updateBatchById(materialDetails);
        }
    }

    /**
     * 驳回到发起人，可设置状态为 保存/自由态
     *
     * @param variables
     */
    @Override
    public void processAuditFreedom(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        MaterialApprove materialApprove = this.getOne(new LambdaQueryWrapper<MaterialApprove>()
                .eq(MaterialApprove::getId, variables.get("businessId")));
        super.update(new LambdaUpdateWrapper<MaterialApprove>()
                .set(MaterialApprove::getState, AgreementStateEnum.DRAFT.getState())
                .eq(MaterialApprove::getId, businessId));

        if (MaterialApprove.CL_TYPE.equals(materialApprove.getType())) {
            MaterialType materialType = materialTypeService.selectMaterialTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(AgreementStateEnum.DRAFT.getState()));
            materialTypeService.updateMaterialTypeNoChange(materialType);

            QueryWrapper<MaterialItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<MaterialItem> materialItems = materialItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(0L);
            });
            materialItemService.updateBatchById(materialItems);

            QueryWrapper<MaterialEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<MaterialEigenvalue> eigenvalues = iMaterialEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(0L);
            });
            iMaterialEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<MaterialDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<MaterialDetails> materialDetails = iMaterialDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(0L);
            });
            iMaterialDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.SB_TYPE.equals(materialApprove.getType())) {
            DeviceType materialType = deviceTypeService.selectDeviceTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(AgreementStateEnum.DRAFT.getState()));
            deviceTypeService.updateDeviceTypeNoChange(materialType);

            QueryWrapper<DeviceItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<DeviceItem> materialItems = deviceItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(0L);
            });
            deviceItemService.updateBatchById(materialItems);

            QueryWrapper<DeviceEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<DeviceEigenvalue> eigenvalues = deviceEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(0L);
            });
            deviceEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<DeviceDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<DeviceDetails> materialDetails = deviceDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(0L);
            });
            deviceDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.LW_TYPE.equals(materialApprove.getType())) {
            LabourType materialType = labourTypeService.selectLabourTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(AgreementStateEnum.DRAFT.getState()));
            labourTypeService.updateLabourTypeNoChange(materialType);

            QueryWrapper<LabourItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<LabourItem> materialItems = labourItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(0L);
            });
            labourItemService.updateBatchById(materialItems);

            QueryWrapper<LabourEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<LabourEigenvalue> eigenvalues = labourEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(0L);
            });
            labourEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<LabourDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<LabourDetails> materialDetails = labourDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(0L);
            });
            labourDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.ZYFB_TYPE.equals(materialApprove.getType())) {
            SubcontractingType materialType = subcontractingTypeService.selectSubcontractingTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(AgreementStateEnum.DRAFT.getState()));
            subcontractingTypeService.updateSubcontractingTypeNoChange(materialType);

            QueryWrapper<SubcontractingItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<SubcontractingItem> materialItems = subcontractingItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(0L);
            });
            subcontractingItemService.updateBatchById(materialItems);

            QueryWrapper<SubcontractingEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<SubcontractingEigenvalue> eigenvalues = subcontractingEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(0L);
            });
            subcontractingEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<SubcontractingDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<SubcontractingDetails> materialDetails = subcontractingDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(0L);
            });
            subcontractingDetailsService.updateBatchById(materialDetails);
        }
    }

    /**
     * 驳回到中途节点，可以设置状态为 审批中
     *
     * @param variables
     */
    @Override
    public void processAuditReject(Map<String, Object> variables) {
//        String businessId = variables.get("businessId").toString();
////        super.update(new LambdaUpdateWrapper<MaterialType>()
////                .set(MaterialType::getState, AgreementStateEnum.REVOKED.getState())
////                .eq(MaterialType::getId, businessId));
    }

    @Override
    public BpmInitializeResponseDTO initialize(BpmInitializeRequestDTO requestDTO) {
        MaterialApprove materialApprove = this.getOne(new LambdaQueryWrapper<MaterialApprove>()
                .eq(MaterialApprove::getId, requestDTO.getBusinessId()));
        if (materialApprove == null) {
            throw new BusinessException("参数异常");
        }
        SysUser sysUser = new SysUser();
        if (MaterialApprove.CL_TYPE.equals(materialApprove.getType())) {
            MaterialType expertChange = materialTypeService.selectMaterialTypeById(materialApprove.getJoinId());
            if (expertChange == null) {
                throw new BusinessException("参数异常");
            }
            sysUser = systemUserService.getUserById(expertChange.getCreateId());
        } else if (MaterialApprove.SB_TYPE.equals(materialApprove.getType())) {
            DeviceType expertChange = deviceTypeService.selectDeviceTypeById(materialApprove.getJoinId());
            if (expertChange == null) {
                throw new BusinessException("参数异常");
            }
            sysUser = systemUserService.getUserById(expertChange.getCreateId());
        } else if (MaterialApprove.LW_TYPE.equals(materialApprove.getType())) {
            LabourType expertChange = labourTypeService.selectLabourTypeById(materialApprove.getJoinId());
            if (expertChange == null) {
                throw new BusinessException("参数异常");
            }
            sysUser = systemUserService.getUserById(expertChange.getCreateId());
        } else if (MaterialApprove.ZYFB_TYPE.equals(materialApprove.getType())) {
            SubcontractingType expertChange = subcontractingTypeService.selectSubcontractingTypeById(materialApprove.getJoinId());
            if (expertChange == null) {
                throw new BusinessException("参数异常");
            }
            sysUser = systemUserService.getUserById(expertChange.getCreateId());
        }
        /* 根据组织获取对应的二级单位 */
        String org = underlingSystemService.getL2OrgByOrgId(sysUser.getThridOrgId());
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(sysUser.getThridOrgId());
        if (orgThree == null) orgThree = org;
        /* 流程角色配置规则传参 */
        List<PropertyListRequestDTO<Object>> propertyList = new ArrayList<>();
        PropertyListRequestDTO.addPropertyToList(propertyList, "groupId", UserConstants.GROUP_DEPT_ID);/* 1000000000 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "companyId", org);/* 公司 二级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "responsibilityDeptId", orgThree);/* 责任单位 三级单位 */
        PropertyListRequestDTO.addPropertyToList(propertyList, "parentProjectCode", org);/* 父项目编码(项目部) */
        requestDTO.setPropertyList(propertyList);
        ResultData<String> initialize = remoteBusinessProcessService.initialize(JSONObject.parseObject(JSON.toJSONString(requestDTO)));
        String data = initialize.getData();
        BpmInitializeResponseDTO bpmInitializeResponseDTO = JSONObject.parseObject(data, BpmInitializeResponseDTO.class);
        return bpmInitializeResponseDTO;
    }

    @Override
    public ResultData<String> revokeProcess(Long id) {
        MaterialApprove materialApprove = this.getOne(new LambdaQueryWrapper<MaterialApprove>()
                .eq(MaterialApprove::getId, id));
        if (materialApprove == null) {
            throw new BusinessException("参数异常");
        }
        if (MaterialApprove.CL_TYPE.equals(materialApprove.getType())) {
            MaterialType materialType = materialTypeService.getById(materialApprove.getJoinId());
            if (materialType == null) {
                throw new RuntimeException("材料类型不存在");
            }
            if (materialType.getState() != 1L) {
                throw new RuntimeException("该状态下的材料类型不允许撤回");
            }
            // 撤回流程
            Map<String, Object> variables = new HashMap<>();
            variables.put("businessId", materialApprove.getId());
            variables.put("processId", materialApprove.getWfProcessId());
            variables.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_MATERIAL.getIdentifying());
            variables.put("Authorization", SecurityUtils.getMasterControlToken());
            return remoteBusinessProcessService.revokedProcess(JSONObject.parseObject(JSON.toJSONString(variables)));
        } else if (MaterialApprove.SB_TYPE.equals(materialApprove.getType())) {
            DeviceType materialType = deviceTypeService.getById(materialApprove.getJoinId());
            if (materialType == null) {
                throw new RuntimeException("设备类型不存在");
            }
            if (materialType.getState() != 1L) {
                throw new RuntimeException("该状态下的设备类型不允许撤回");
            }
            // 撤回流程
            Map<String, Object> variables = new HashMap<>();
            variables.put("businessId", materialApprove.getId());
            variables.put("processId", materialApprove.getWfProcessId());
            variables.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_DEVICE.getIdentifying());
            variables.put("Authorization", SecurityUtils.getMasterControlToken());
            return remoteBusinessProcessService.revokedProcess(JSONObject.parseObject(JSON.toJSONString(variables)));
        } else if (MaterialApprove.LW_TYPE.equals(materialApprove.getType())) {
            LabourType materialType = labourTypeService.getById(materialApprove.getJoinId());
            if (materialType == null) {
                throw new RuntimeException("劳务类型不存在");
            }
            if (materialType.getState() != 1L) {
                throw new RuntimeException("该状态下的劳务类型不允许撤回");
            }
            // 撤回流程
            Map<String, Object> variables = new HashMap<>();
            variables.put("businessId", materialApprove.getId());
            variables.put("processId", materialApprove.getWfProcessId());
            variables.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_LABOUR.getIdentifying());
            variables.put("Authorization", SecurityUtils.getMasterControlToken());
            return remoteBusinessProcessService.revokedProcess(JSONObject.parseObject(JSON.toJSONString(variables)));
        } else if (MaterialApprove.ZYFB_TYPE.equals(materialApprove.getType())) {
            SubcontractingType materialType = subcontractingTypeService.getById(materialApprove.getJoinId());
            if (materialType == null) {
                throw new RuntimeException("专业分包类型不存在");
            }
            if (materialType.getState() != 1L) {
                throw new RuntimeException("该状态下的专业分包类型不允许撤回");
            }
            // 撤回流程
            Map<String, Object> variables = new HashMap<>();
            variables.put("businessId", materialApprove.getId());
            variables.put("processId", materialApprove.getWfProcessId());
            variables.put("processKey", ProcessKeyEnum.ZHAOCAI_ASSISTANT_SUBCONTRACTING.getIdentifying());
            variables.put("Authorization", SecurityUtils.getMasterControlToken());
            return remoteBusinessProcessService.revokedProcess(JSONObject.parseObject(JSON.toJSONString(variables)));
        }
        return null;
    }

    /**
     * 审批驳回到发起人
     *
     * @param variables
     */
    @Override
    public void processAuditRevoke(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        MaterialApprove materialApprove = this.getOne(new LambdaQueryWrapper<MaterialApprove>()
                .eq(MaterialApprove::getId, businessId));
        super.update(new LambdaUpdateWrapper<MaterialApprove>()
                .set(MaterialApprove::getState, AgreementStateEnum.REVOKED.getState())
                .eq(MaterialApprove::getId, businessId));
        if (materialApprove == null) {
            throw new BusinessException("参数异常");
        }
        if (MaterialApprove.CL_TYPE.equals(materialApprove.getType())) {
            MaterialType materialType = materialTypeService.selectMaterialTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(AgreementStateEnum.REVOKED.getState()));
            materialTypeService.updateMaterialTypeNoChange(materialType);

            QueryWrapper<MaterialItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<MaterialItem> materialItems = materialItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(5L);
            });
            materialItemService.updateBatchById(materialItems);

            QueryWrapper<MaterialEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<MaterialEigenvalue> eigenvalues = iMaterialEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(5L);
            });
            iMaterialEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<MaterialDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<MaterialDetails> materialDetails = iMaterialDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(5L);
            });
            iMaterialDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.SB_TYPE.equals(materialApprove.getType())) {
            DeviceType materialType = deviceTypeService.selectDeviceTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(AgreementStateEnum.REVOKED.getState()));
            deviceTypeService.updateDeviceTypeNoChange(materialType);

            QueryWrapper<DeviceItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<DeviceItem> materialItems = deviceItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(5L);
            });
            deviceItemService.updateBatchById(materialItems);

            QueryWrapper<DeviceEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<DeviceEigenvalue> eigenvalues = deviceEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(5L);
            });
            deviceEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<DeviceDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<DeviceDetails> materialDetails = deviceDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(5L);
            });
            deviceDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.LW_TYPE.equals(materialApprove.getType())) {
            LabourType materialType = labourTypeService.selectLabourTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(AgreementStateEnum.REVOKED.getState()));
            labourTypeService.updateLabourTypeNoChange(materialType);

            QueryWrapper<LabourItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<LabourItem> materialItems = labourItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(5L);
            });
            labourItemService.updateBatchById(materialItems);

            QueryWrapper<LabourEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<LabourEigenvalue> eigenvalues = labourEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(5L);
            });
            labourEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<LabourDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<LabourDetails> materialDetails = labourDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(5L);
            });
            labourDetailsService.updateBatchById(materialDetails);
        } else if (MaterialApprove.ZYFB_TYPE.equals(materialApprove.getType())) {
            SubcontractingType materialType = subcontractingTypeService.selectSubcontractingTypeByIdNoChange(materialApprove.getJoinId());
            materialType.setState(Long.valueOf(AgreementStateEnum.REVOKED.getState()));
            subcontractingTypeService.updateSubcontractingTypeNoChange(materialType);

            QueryWrapper<SubcontractingItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type_id", materialType.getId());
            queryWrapper.eq("state", 1L);
            queryWrapper.eq("del_flag", "0");
            queryWrapper.eq("is_main", "N");
            List<SubcontractingItem> materialItems = subcontractingItemService.list(queryWrapper);
            materialItems.forEach(materialItem -> {
                materialItem.setState(5L);
            });
            subcontractingItemService.updateBatchById(materialItems);

            QueryWrapper<SubcontractingEigenvalue> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("type_id", materialType.getId());
            queryWrapper1.eq("state", 1L);
            queryWrapper1.eq("del_flag", "0");
            queryWrapper1.eq("is_main", "N");
            List<SubcontractingEigenvalue> eigenvalues = subcontractingEigenvalueService.list(queryWrapper1);
            eigenvalues.forEach(materialEigenvalue -> {
                materialEigenvalue.setState(5L);
            });
            subcontractingEigenvalueService.updateBatchById(eigenvalues);

            QueryWrapper<SubcontractingDetails> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.eq("type_id", materialType.getId());
            queryWrapper2.eq("state", 1L);
            queryWrapper2.eq("del_flag", "0");
            queryWrapper2.eq("is_main", "N");
            List<SubcontractingDetails> materialDetails = subcontractingDetailsService.list(queryWrapper2);
            materialDetails.forEach(materialDetails1 -> {
                materialDetails1.setState(5L);
            });
            subcontractingDetailsService.updateBatchById(materialDetails);
        }

    }


}
