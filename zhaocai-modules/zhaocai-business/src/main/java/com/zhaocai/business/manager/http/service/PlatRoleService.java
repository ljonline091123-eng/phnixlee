package com.zhaocai.business.manager.http.service;

//import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.req.RoleListRequestDTO;
import com.zhaocai.business.manager.http.dto.req.UsersRoleListRequestDTO;
import com.zhaocai.business.manager.http.dto.res.RoleListResponseDTO;
import com.zhaocai.business.manager.http.dto.res.UsersRoleListResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ssy
 * @date 2024/9/11 17:37
 */
@Slf4j
@Service
public class PlatRoleService {

    /**
     * 获取第三方角色接口
     * @param requestDTO
     * @return
     */
    public List<RoleListResponseDTO> getRoleList(RoleListRequestDTO requestDTO) {
//        List<RoleListResponseDTO> list = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.BASIC_DATA_ROLE,
//                RoleListResponseDTO.class, requestDTO);
        List<RoleListResponseDTO> list = new ArrayList<>();
        return list;
    }

    /**
     * 获取第三方角色用户信息接口
     * @param requestDTO
     * @return
     */
    public List<UsersRoleListResponseDTO> getUsersRoleList(UsersRoleListRequestDTO requestDTO) {
//        List<UsersRoleListResponseDTO> list = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.BASIC_DATA_USER_ROLE,
//                UsersRoleListResponseDTO.class, requestDTO);
        List<UsersRoleListResponseDTO> list = new ArrayList<>();
        list = list.stream().sorted(Comparator.comparingInt(UsersRoleListResponseDTO::getRoleSort)).collect(Collectors.toList());
        return list;
    }

}
