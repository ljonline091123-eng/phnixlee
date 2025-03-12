package com.zhaocai.business.procurement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.req.MinProjectListRequestDTO;
import com.zhaocai.business.manager.http.dto.res.MinProjectDetailResponseDTO;
import com.zhaocai.business.manager.http.service.ContractPlanService;
import com.zhaocai.business.procurement.domain.MinProject;
import com.zhaocai.business.procurement.mapper.MinProjectMapper;
import com.zhaocai.business.procurement.service.IContractPlanningService;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.procurement.vo.res.MinProjectListVO;
import com.zhaocai.business.procurement.vo.res.MinProjectVO;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 最小核算项目信息Service业务层处理
 *
 * @author WH
 * @date 2024-07-16
 */
@Service
public class MinProjectServiceImpl extends ServiceImpl<MinProjectMapper, MinProject> implements IMinProjectService {

    @Autowired
    private ContractPlanService contractPlanService;

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private IContractPlanningService contractPlanningService;

    @Resource
    private MinProjectMapper minProjectMapper;

    @Override
    public MinProject saveMinProject(MinProjectDetailResponseDTO projectDetail) {
        synchronized (projectDetail.getMinAccountCode()) {
            MinProject saveMinProject = BeanCopierUtil.copyBean(projectDetail, MinProject.class);

            if (null != projectDetail.getId()) {
                saveMinProject.setMinProjectId(projectDetail.getId());
            }
            if (StringUtils.isNoneBlank(projectDetail.getPrjAddr())) {
                List<String> list = JSONArray.parseArray(projectDetail.getPrjAddr(), String.class);
                // TODO 省市区的需要再次确认
                if (CollectionUtil.isNotEmpty(list) && list.size() > 2) {
                    // 有 3 位，为省、市、区
                    saveMinProject.setPrjAddrProvince(list.get(0));
                    saveMinProject.setPrjAddrCity(list.get(1));
                    saveMinProject.setPrjAddrRegion(list.get(2));
                } else if (CollectionUtil.isNotEmpty(list) && list.size() > 1) {
                    // 有 2 位，为市、区
                    saveMinProject.setPrjAddrProvince(list.get(0));
                    saveMinProject.setPrjAddrCity(list.get(0));
                    saveMinProject.setPrjAddrRegion(list.get(1));
                }
            }

            MinProject minProject = super.getOne(new LambdaQueryWrapper<MinProject>()
                    .eq(MinProject::getMinAccountCode, projectDetail.getMinAccountCode()));
            if (minProject != null) {
                saveMinProject.setId(minProject.getId());
            } else {
                saveMinProject.setId(null);
            }

            super.saveOrUpdate(saveMinProject);
            return saveMinProject;
        }
    }

    @Override
    public MinProjectVO getMinProjectByMinAccountCode(String projectCode) {
        if (StringUtils.isBlank(projectCode)) {
            throw new ParamValidateException("最小核算项目编码不能为空");
        }
        QueryWrapper<MinProject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("min_account_code",projectCode);
        queryWrapper.eq("del_flag","0");
        MinProject minProject = minProjectMapper.selectOne(queryWrapper);

        MinProjectVO minProjectVO = BeanCopierUtil.copyBean(minProject, MinProjectVO.class);
        if (StringUtils.isBlank(minProjectVO.getManagementOrgId())) {
            throw new ParamValidateException("该项目所归属管理组织为空，请确认");
        }

        // 获取我们系统里面对应的机构 id
        SysDept sysDept = remoteSystemService.getByThridDeptId(minProjectVO.getManagementOrgId(), SecurityConstants.INNER);
        ValidateUtils.isNullException(sysDept, "该项目所归属管理组织对应的组织机构不存在，请确认");
        minProjectVO.setDeptId(sysDept.getDeptId());
        return minProjectVO;
    }


    @Override
    public List<MinProjectVO> getMinProjectList(String managementOrgId) {
        QueryWrapper<MinProject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("management_org_id", managementOrgId);
        queryWrapper.orderByAsc("min_account_code");
        List<MinProject> minProjectList = super.list(queryWrapper);
        List<MinProjectVO> minProjectVOS = new ArrayList<>();
        for (MinProject minProject : minProjectList) {
            MinProjectVO minProjectVO = BeanCopierUtil.copyBean(minProject, MinProjectVO.class);
            minProjectVOS.add(minProjectVO);
        }
        return minProjectVOS;
    }

    @Override
    public PageResult<MinProjectListVO> getProjectListByQuery(MinProjectListRequestDTO requestDTO) {
        SysDept sysDept = remoteSystemService.getInfo(requestDTO.getDeptId(),SecurityConstants.INNER);

        requestDTO.setManagementOrgId(sysDept.getThridDeptId());

        // 执行分页查询
        IPage<MinProject> minProjectPage = baseMapper.selectListPage(requestDTO.toMybatisPage(), requestDTO);

        // 将查询结果转换为 VO 对象
        List<MinProjectListVO> minProjectVOS = minProjectPage.getRecords().stream()
                .map(minProject -> BeanCopierUtil.copyBean(minProject, MinProjectListVO.class))
                .collect(Collectors.toList());

        // 返回分页结果
        return new PageResult<>(minProjectVOS, (int) minProjectPage.getTotal());
    }

}
