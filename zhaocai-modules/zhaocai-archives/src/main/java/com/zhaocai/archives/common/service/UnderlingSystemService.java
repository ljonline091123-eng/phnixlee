package com.zhaocai.archives.common.service;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhaocai.archives.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.archives.common.vo.req.BaseDictListRequestDTO;
import com.zhaocai.archives.common.vo.req.GetL2OrgByOrgIdRequestDTO;
import com.zhaocai.archives.common.vo.req.GetL3OrgByOrgIdRequestDTO;
import com.zhaocai.archives.common.vo.res.BaseDictListDTO;
import com.zhaocai.archives.common.vo.res.DictListVO;
import com.zhaocai.common.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnderlingSystemService {

    /**
     * 获取底层逻辑的系统字典
     *
     * @param type
     * @return
     */
    public List<DictListVO> listDict(String type) {
        if (StringUtils.isBlank(type)) {
            throw new RuntimeException("请传入系统字典类型");
        }

        BaseDictListRequestDTO requestDTO = new BaseDictListRequestDTO(type);
        List<BaseDictListDTO> dictList = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.DICT_LIST_MAP, BaseDictListDTO.class, requestDTO);

        List<DictListVO> resultList = Collections.emptyList();
        if (CollectionUtil.isNotEmpty(dictList)) {
            resultList = dictList.stream()
                    .filter(f -> f.getEnable() && !"0".equals(f.getParentId()))
                    .map(x -> new DictListVO(x.getDictName(), x.getDictValue(), x.getSort()))
                    .collect(Collectors.toList());
        }

        // 排序
        resultList.sort(Comparator.comparing(DictListVO::getSort));

        return resultList;
    }

    /**
     * 获取底层逻辑的系统字典映射
     *
     * @param type
     * @return
     */
    public Map<String, String> listDictMap(String type) {
        List<DictListVO> dictList = listDict(type);
        if (CollectionUtil.isNotEmpty(dictList)) {
            return dictList.stream()
                    .collect(Collectors.toMap(DictListVO::getDictValue, DictListVO::getDictLabel));
        }

        return Collections.emptyMap();
    }


    public String getL2OrgByOrgId(String orgId) {
        String l2Org = "2001000000000";
        GetL2OrgByOrgIdRequestDTO reqDTO = new GetL2OrgByOrgIdRequestDTO();
        reqDTO.setOrgId(orgId);
        String responseStr = UnderlingRestTemplateService.getForObject
                (UnderlingPlatformUrlEnum.GET_L2_ORG_BY_ORGID, String.class, reqDTO);
        if (null != responseStr) {
            l2Org = responseStr;
        }
        return l2Org;
    }

    public String getL3OrgByOrgId(String orgId) {
        GetL3OrgByOrgIdRequestDTO reqDTO = new GetL3OrgByOrgIdRequestDTO();
        reqDTO.setOrgId(orgId);
        JsonNode dataNode = UnderlingRestTemplateService.getForObject(UnderlingPlatformUrlEnum.GET_L3_ORG_BY_ORGID, reqDTO);
        if (dataNode == null || dataNode.isNull()) {
            log.info("[获取到的三级单位]-[getL3OrgByOrgId] param:{}, response: null", orgId);
            return null;
        } else if (dataNode.isTextual()) {
            String dataString = dataNode.asText();
            log.info("[获取到的三级单位]-[getL3OrgByOrgId] param:{},response:{}", orgId, dataString);
            return dataString;
        } else if (dataNode.isArray()) {
            List<String> dataList = new ArrayList<>();
            for (JsonNode node : dataNode) {
                dataList.add(node.asText());
            }
            log.info("[获取到的三级单位]-[getL3OrgByOrgId] param:{},responseList:{}", orgId, dataList);
            return null;
        } else {
            log.warn("[获取到的三级单位]-[getL3OrgByOrgId] param:{}, response: unexpected data type", orgId);
            return null;
        }
    }

}
