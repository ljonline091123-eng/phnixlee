package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.manager.http.dto.req.BpmInitializeRequestDTO;
import com.zhaocai.business.manager.http.dto.req.UsersRoleListRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.UsersRoleContractPlanListResponseDTO;
import com.zhaocai.business.manager.http.dto.res.UsersRoleListResponseDTO;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.business.procurement.domain.ProcurementPlan;
import com.zhaocai.business.procurement.vo.req.*;
import com.zhaocai.business.procurement.vo.res.*;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;

import java.util.List;
import java.util.Map;

/**
 * 采购计划Service接口
 *
 * @author chenming
 * @date 2024-05-24
 */
public interface IProcurementPlanService  extends IService<ProcurementPlan>, IProcessBusinessBaseService, IPBMOverrideService {

    /**
     * 列表查询
     * @param queryVO
     * @return
     */
    PageResult<ProcurementPlanListVO> listPage(ProcurementPlanListQueryVO queryVO);

    /**
     * 项目合约规划列表查询
     * @param queryVO
     * @return
     */
    PageResult<ContractPlanningListVO> listContractPlanningPage(ContractPlanningListQueryVO queryVO);

    /**
     * 项目合约规划列表查询
     * @param queryVO
     * @return
     */
    ContractPlanningVO listContractPlanning(ContractPlanningListQueryVO queryVO);

    /**
     * 获取项目合约规划物料清单
     * @param queryVO
     * @return
     */
    ContractPlanMaterialListVO listContractMaterials(ContractPlanMaterialListQueryVO queryVO);

    /**
     * 保存采购计划
     * @param requestVO
     */
    MaterialProcurementPushRequestVO saveProcurementPlan(ProcurementPlanRequestVO requestVO);

    /**
     * 提交采购计划
     * @param planId
     */
    void submitProcurementPlan(Long planId);

    /**
     * 获取采购计划详情
     * @param id
     * @return
     */
    ProcurementPlanDetailVO getProcurementPlanDetail(Long id);

    /**
     * 获取采购计划的合约拆分记录
     * @param id
     * @return
     */
    List<ContractSplitListVO> listContractSplit(Long id);

    /**
     * 根据合约拆分id获取绑定的采购计划
     * @param splitId
     * @return
     */
    ProcurementPlan getBySplitId(Long splitId);

    /**
     * 获取采购计划和拆分合约
     * @param queryVO
     * @return
     */
    PageResult<ProcurementPlanContractSplitVO> listPlanContractSplit(ProcurementPlanContractSplitQueryVO queryVO);

    /**
     * 作废采购计划
     * @param planId
     */
    void cancellationProcurementPlan(Long planId);

    /**
     * 设置拆分标识
     * @param flag
     */
    void setContractPlanSplitFlag(String flag);

    /**
     * getContractPlanSplitFlag
     * @return
     */
    String getContractPlanSplitFlag();

    /**
     * 推送采购计划
     * @param planPushVO push参数
     */
    void pushProcurementPlan(ProcurementPlanPushVO planPushVO);

    /**
     * 获取第三方角色用户信息接口并关联采购方案
     * @return
     */
    UsersRoleContractPlanListResponseDTO getUsersRoleContractPlanList(ContractPlanningQueryVO requestDTO);

    /**
     * 推送易料采购清单
     * @param requestVO
     * @return
     */
    ProcurementPlanDetailVO pushMaterialProcurementList(MaterialProcurementPushRequestVO requestVO);

    /**
     * 撤销推送的易料采购清单
     * @param requestVO
     * @return
     */
    ProcurementPlanDetailVO revokePushMaterialProcurementList(MaterialProcurementPushRequestVO requestVO);

    String getgetYjtUrl(String type, String code) throws Exception;

    ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO);

    void submitProcurementPlan(Long id, String detailUrl, String operateComment);


    void processStart(Map<String, Object> variables);

    void revokeProcurementPlan(Long id);
}
