package com.zhaocai.business.pub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.pub.domain.AreaDivision;
import com.zhaocai.business.pub.mapper.AreaDivisionMapper;
import com.zhaocai.business.pub.service.IAreaDivisionService;
import com.zhaocai.business.pub.vo.res.AreaDivisionTreeVO;
import com.zhaocai.business.pub.vo.res.AreaDivisionVO;
import com.zhaocai.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 行政区划Service业务层处理
 *
 * @author WH
 * @date 2024-07-12
 */
@Service
public class AreaDivisionServiceImpl extends ServiceImpl<AreaDivisionMapper, AreaDivision> implements IAreaDivisionService {

    @Override
    public Boolean deleteSyncAreaDivision() {
        return baseMapper.deleteSyncAreaDivision();
    }

    @Override
    public List<AreaDivisionVO> listAreaDivisionByParentCode(String parentCode) {
        return baseMapper.selectAreaDivisionListByParentCode(parentCode);
    }

    @Override
    public AreaDivision getByCode(String areaCode) {
        return super.getOne(new LambdaQueryWrapper<AreaDivision>()
                .eq(AreaDivision::getAreaCode,areaCode));
    }

    @Override
    public List<AreaDivisionTreeVO> listAreaDivisionTree() {
        List<AreaDivision> list = super.list();
        return convertToTree(list);
    }

    @Override
    public Map<String, String> getAreaDivisionMap(List<String> codeList) {
        List<AreaDivision> divisionList = super.list(new LambdaQueryWrapper<AreaDivision>()
                .in(AreaDivision::getAreaCode,codeList));

        return divisionList.stream().collect(Collectors.toMap(AreaDivision::getAreaCode, AreaDivision::getAreaName));
    }

    /**
     * 构建树形结构
     * @param list
     * @return
     */
    private List<AreaDivisionTreeVO> convertToTree(List<AreaDivision> list) {
        Map<String, AreaDivisionTreeVO> treeMap = new HashMap<>();

        for (AreaDivision vo : list) {
            AreaDivisionTreeVO treeVO = new AreaDivisionTreeVO();
            treeVO.setDivisionCode(vo.getAreaCode());
            treeVO.setDivisionName(vo.getAreaName());
            treeVO.setChildren(new ArrayList<>());
            treeMap.put(vo.getAreaCode(), treeVO);
        }

        // 构建树形结构
        List<AreaDivisionTreeVO> rootList = new ArrayList<>();
        for (AreaDivision vo : list) {
            AreaDivisionTreeVO treeVO = treeMap.get(vo.getAreaCode());
            if (StringUtils.isBlank(vo.getParentCode())) {
                // 跟节点
                rootList.add(treeVO);
            } else {
                // 非根节点，找到父节点并添加到其子节点列表中
                AreaDivisionTreeVO parentTreeVO = treeMap.get(vo.getParentCode());
                if (parentTreeVO != null) {
                    parentTreeVO.getChildren().add(treeVO);
                }
            }
        }
        return rootList;
    }
}
