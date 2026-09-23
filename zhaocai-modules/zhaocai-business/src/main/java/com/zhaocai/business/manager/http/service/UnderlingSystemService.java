package com.zhaocai.business.manager.http.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhaocai.business.common.exception.BusinessException;
//import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.*;
import com.zhaocai.business.pub.vo.req.DeviceQueryVO;
import com.zhaocai.business.pub.vo.req.MaterialsQueryVO;
import com.zhaocai.business.pub.vo.res.*;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnderlingSystemService {

    /**
     * 获取底层逻辑的系统字典
     * @param type
     * @return
     */
    public List<DictListVO> listDict(String type) {
        if (StringUtils.isBlank(type)) {
            throw new BusinessException("请传入系统字典类型");
        }

        BaseDictListRequestDTO requestDTO = new BaseDictListRequestDTO(type);
//        List<BaseDictListDTO> dictList = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.DICT_LIST_MAP,BaseDictListDTO.class,requestDTO);
        List<BaseDictListDTO> dictList = new ArrayList<>();

        List<DictListVO> resultList = Collections.emptyList();
        if (CollectionUtil.isNotEmpty(dictList)) {
            resultList = dictList.stream()
                    .filter(f -> f.getEnable() && !"0".equals(f.getParentId()))
                    .map( x -> new DictListVO(x.getDictName(),x.getDictValue(),x.getSort(),x.getRemark()))
                    .collect(Collectors.toList());
        }

        // 排序
        resultList.sort(Comparator.comparing(DictListVO::getSort));

        return resultList;
    }

    /**
     * 获取底层逻辑的系统字典映射
     * @param type
     * @return
     */
    public Map<String,String> listDictMap(String type) {
        List<DictListVO> dictList = listDict(type);
        if (CollectionUtil.isNotEmpty(dictList)) {
            return dictList.stream()
                    .collect(Collectors.toMap(DictListVO::getDictValue, DictListVO::getDictLabel));
        }

        return Collections.emptyMap();
    }

    /**
     * 获取设备分类
     * @return
     */
    public List<DeviceClassVO> listDeviceClass() {
        DeviceClassRequestDTO requestDTO = new DeviceClassRequestDTO();

//        List<DeviceClassListResponseDTO> responseList = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.DEVICE_CLASS_LIST,DeviceClassListResponseDTO.class,requestDTO);
        List<DeviceClassListResponseDTO> responseList = new ArrayList<>();
        return convertToTree4DeviceClass(responseList);
    }

    /**
     * 获取设备列表
     * @param queryVO
     * @return
     */
    public List<DeviceFeatureVO> deviceFeatureList(DeviceQueryVO queryVO) {
        if (StringUtils.isBlank(queryVO.getQueryId())) {
            return Collections.emptyList();
        }

        DeviceFeatureRequestDTO requestDTO = new DeviceFeatureRequestDTO(queryVO.getQueryId());

//        List<DeviceFeatureResponseDTO> resultList = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.DEVICE_FEATURE_LIST,
//                DeviceFeatureResponseDTO.class,requestDTO);
        List<DeviceFeatureResponseDTO> resultList = new ArrayList<>();

        return BeanCopierUtil.copyList(resultList,DeviceFeatureVO.class);
    }

    /**
     * 获取设备特征值列表
     * @param queryVO
     * @return
     */
    public List<DeviceFeatureValueVO> deviceFeatureValueList(DeviceQueryVO queryVO) {
        if (StringUtils.isBlank(queryVO.getQueryId())) {
            return Collections.emptyList();
        }

        DeviceFeatureValueRequestDTO requestDTO = new DeviceFeatureValueRequestDTO(queryVO.getQueryId());

//        List<DeviceFeatureValueResponseDTO> resultList = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.DEVICE_FEATURE_VALUE_LIST,
//                DeviceFeatureValueResponseDTO.class,requestDTO);

        List<DeviceFeatureValueResponseDTO> resultList = new ArrayList<>();
        return BeanCopierUtil.copyList(resultList,DeviceFeatureValueVO.class);
    }

    /**
     * 获取物料分类
     * @return
     */
    public List<MaterialsClassVO> listMaterialsClass() {
        MaterialsClassRequestDTO requestDTO = new MaterialsClassRequestDTO();
//        List<MaterialsClassResponseDTO> responseList = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.MATERIALS_CLASS_LIST,
//                MaterialsClassResponseDTO.class,requestDTO);
        List<MaterialsClassResponseDTO> responseList = new ArrayList<>();

        return convertToTree4MaterialsClass(responseList);
    }

    /**
     * 材料特征项列表
     * @param queryVO
     * @return
     */
    public List<MaterialsFeatureVO> listMaterialsFeature(MaterialsQueryVO queryVO) {
        if (StringUtils.isBlank(queryVO.getQueryId())) {
            return Collections.emptyList();
        }

        MaterialsFeatureRequestDTO requestDTO = new MaterialsFeatureRequestDTO(queryVO.getQueryId());

//        List<MaterialsFeatureResponseDTO> resultList = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.MATERIALS_FEATURE_LIST,
//                MaterialsFeatureResponseDTO.class,requestDTO);

        List<MaterialsFeatureResponseDTO> resultList = new ArrayList<>();

        return BeanCopierUtil.copyList(resultList,MaterialsFeatureVO.class);
    }

    /**
     * 获取材料特征值
     * @param queryVO
     * @return
     */
    public List<MaterialsFeatureValueVO> listMaterialsFeatureValue(MaterialsQueryVO queryVO) {
        if (StringUtils.isBlank(queryVO.getQueryId())) {
            return Collections.emptyList();
        }

        MaterialsFeatureValueRequestDTO requestDTO = new MaterialsFeatureValueRequestDTO(queryVO.getQueryId());

//        List<MaterialsFeatureValueResponseDTO> resultList = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.MATERIALS_FEATURE_VALUE_LIST,
//                MaterialsFeatureValueResponseDTO.class,requestDTO);
        List<MaterialsFeatureValueResponseDTO> resultList = new ArrayList<>();

        return BeanCopierUtil.copyList(resultList,MaterialsFeatureValueVO.class);
    }

    public String getL2OrgByOrgId(String orgId){
        String l2Org = "2001000000000";
        GetL2OrgByOrgIdRequestDTO reqDTO = new GetL2OrgByOrgIdRequestDTO();
        reqDTO.setOrgId(orgId);
//        String responseStr =  UnderlingRestTemplateService.getForObject
//                (UnderlingPlatformUrlEnum.GET_L2_ORG_BY_ORGID,String.class,reqDTO);
        String responseStr =  null;
        if (null != responseStr){
            l2Org = responseStr;
        }
        return l2Org;
    }

    public String getL3OrgByOrgId(String orgId){
        GetL3OrgByOrgIdRequestDTO reqDTO = new GetL3OrgByOrgIdRequestDTO();
        reqDTO.setOrgId(orgId);
//        JsonNode dataNode = UnderlingRestTemplateService.getForObject(UnderlingPlatformUrlEnum.GET_L3_ORG_BY_ORGID,reqDTO);
        JsonNode dataNode = null;
        if (dataNode == null || dataNode.isNull()) {
            log.info("[获取到的三级单位]-[getL3OrgByOrgId] param:{}, response: null", orgId);
            return null;
        } else if (dataNode.isTextual()) {
            String dataString = dataNode.asText();
            log.info("[获取到的三级单位]-[getL3OrgByOrgId] param:{},response:{}",orgId,dataString);
            return dataString;
        } else if (dataNode.isArray()) {
            List<String> dataList = new ArrayList<>();
            for (JsonNode node : dataNode) {
                dataList.add(node.asText());
            }
            log.info("[获取到的三级单位]-[getL3OrgByOrgId] param:{},responseList:{}",orgId,dataList);
            return null;
        } else {
            log.warn("[获取到的三级单位]-[getL3OrgByOrgId] param:{}, response: unexpected data type", orgId);
            return null;
        }
    }

    /**
     * 获取 招采 流程分组数据 获取流程分组数据
     * Time:2024/10/29 下午6:13
     * */
    public List<ListCataLogDTO> listCatalog(){
//        return UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.LIST_CATA_LOG,ListCataLogDTO.class,new ListCataLogRequestDTO());
        return new ArrayList<>();
    }
    /**
     * 获取 dm071 数据
     * @return
     */
    public List<DwMmAssetInfResponseDTO> listDwMmAssetInf() {
//        return UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.DW_MM_ASSET_INF,DwMmAssetInfResponseDTO.class,new DwMmInfRequestDTO());
        return new ArrayList<>();
    }

    /**
     * 获取 dm073 数据
     * @return
     */
    public List<DwMmServiceInfResponseDTO> listDwMmServiceInf() {
//        return UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.DW_MM_SERVICE_INF,DwMmServiceInfResponseDTO.class,new DwMmInfRequestDTO());
        return new ArrayList<>();
    }

    /**
     *
     * @param materialsClassList
     * @return
     */
    private List<MaterialsClassVO> convertToTree4MaterialsClass(List<MaterialsClassResponseDTO> materialsClassList) {
        Map<String,MaterialsClassVO> materialsClassMap = new HashMap<>();

        for (MaterialsClassResponseDTO materialsClass : materialsClassList) {
            MaterialsClassVO materialsClassVO = new MaterialsClassVO(materialsClass.getId(),materialsClass.getMtrClassCode(),
                                    materialsClass.getMtrClassName(),materialsClass.getMeasureUnit());
            materialsClassVO.setChildren(new ArrayList<>());
            materialsClassMap.put(materialsClass.getId(),materialsClassVO);
        }

        // 构建树形结构
        List<MaterialsClassVO> rootList = new ArrayList<>();
        for (MaterialsClassResponseDTO materialsClass : materialsClassList) {
            MaterialsClassVO treeVo = materialsClassMap.get(materialsClass.getId());
            if (StringUtils.isBlank(materialsClass.getParentId()) || "0".equals(materialsClass.getParentId())) {
                // 根节点，直接添加
                rootList.add(treeVo);
            } else {
                // 非根节点，找到父节点并添加到其子节点列表中
                MaterialsClassVO materialsClassVO = materialsClassMap.get(materialsClass.getParentId());
                materialsClassVO.getChildren().add(treeVo);
            }
        }

        return rootList;
    }

    /**
     * 构建树形结构
     * @param deviceClassList
     * @return
     */
    private List<DeviceClassVO> convertToTree4DeviceClass(List<DeviceClassListResponseDTO> deviceClassList) {
        Map<String, DeviceClassVO> deviceClassMap = new HashMap<>();

        for (DeviceClassListResponseDTO deviceClass : deviceClassList) {
            DeviceClassVO deviceClassVO = new DeviceClassVO(deviceClass.getId(), deviceClass.getDeviceClassCode(), deviceClass.getDeviceClassName());
            deviceClassVO.setChildren(new ArrayList<>());
            deviceClassMap.put(deviceClass.getId(), deviceClassVO);

        }

        // 构建树形结构
        List<DeviceClassVO> rootList = new ArrayList<>();
        for (DeviceClassListResponseDTO deviceClass : deviceClassList) {
            DeviceClassVO treeVo = deviceClassMap.get(deviceClass.getId());
            if (StringUtils.isBlank(deviceClass.getParentId()) || "0".equals(deviceClass.getParentId())) {
                // 根节点，直接添加
                rootList.add(treeVo);
            } else {
                // 非根节点，找到父节点并添加到其子节点列表中
                DeviceClassVO deviceClassParentVO = deviceClassMap.get(deviceClass.getParentId());
                deviceClassParentVO.getChildren().add(treeVo);
            }
        }

        return rootList;
    }
}
