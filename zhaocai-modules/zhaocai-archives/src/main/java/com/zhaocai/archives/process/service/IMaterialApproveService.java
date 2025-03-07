package com.zhaocai.archives.process.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.common.vo.req.BpmInitializeRequestDTO;
import com.zhaocai.archives.common.vo.res.BpmInitializeResponseDTO;
import com.zhaocai.archives.process.domain.MaterialApprove;
import com.zhaocai.common.core.web.bean.ResultData;

import java.util.List;
import java.util.Map;

/**
 * 物料审批辅Service接口
 *
 * @author lzq
 * @date 2025-02-12
 */
public interface IMaterialApproveService extends IService<MaterialApprove> {
    /**
     * 查询物料审批辅
     *
     * @param id 物料审批辅主键
     * @return 物料审批辅
     */
    public MaterialApprove selectMaterialApproveById(Long id);

    /**
     * 查询物料审批辅列表
     *
     * @param materialApprove 物料审批辅
     * @return 物料审批辅集合
     */
    public List<MaterialApprove> selectMaterialApproveList(MaterialApprove materialApprove);

    /**
     * 新增物料审批辅
     *
     * @param materialApprove 物料审批辅
     * @return 结果
     */
    public int insertMaterialApprove(MaterialApprove materialApprove);

    /**
     * 修改物料审批辅
     *
     * @param materialApprove 物料审批辅
     * @return 结果
     */
    public int updateMaterialApprove(MaterialApprove materialApprove);

    /**
     * 批量删除物料审批辅
     *
     * @param ids 需要删除的物料审批辅主键集合
     * @return 结果
     */
    public int deleteMaterialApproveByIds(Long[] ids);

    /**
     * 删除物料审批辅信息
     *
     * @param id 物料审批辅主键
     * @return 结果
     */
    public int deleteMaterialApproveById(Long id);


    boolean submit(MaterialApprove materialApprove);

    void processStart(Map<String, Object> variables);

    String audit(String processKey, JSONObject body);

    void processAuditPass(Map<String, Object> variables);

    void processAuditFreedom(Map<String, Object> variables);

    void processAuditReject(Map<String, Object> variables);

    BpmInitializeResponseDTO initialize(BpmInitializeRequestDTO requestDTO);

    ResultData<String> revokeProcess(Long id);

    void processAuditRevoke(Map<String, Object> variables);
}
