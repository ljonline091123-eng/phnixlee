package com.zhaocai.business.pub.service;

import com.zhaocai.business.pub.vo.res.OrganizationVO;

import java.util.List;


/**
 * 组织机构服务接口
 *
 * @author chenming
 * @date 2024-07-17
 */
public interface IOrganizationService {

    /**
     * 获取树形结构的组织机构
     * @return
     */
    List<OrganizationVO> getOriganizationTreeList();

    /**
     * 获取树形结构的组织机构,不包含项目部
     * @return
     */
    List<OrganizationVO> getOriganizationTreeUnXList();

    /**
     * 获取公司
     * @return
     */
    List<OrganizationVO> listOrganization4Company();

    /**
     * 获取集团和二级单位公司
     * @return
     */
    List<OrganizationVO> listOrganizationGroupOrUnit();

    /**
     * 范本选择获取公司
     * @return
     */
    List<OrganizationVO> listOrganizationCalligraphy();
}
