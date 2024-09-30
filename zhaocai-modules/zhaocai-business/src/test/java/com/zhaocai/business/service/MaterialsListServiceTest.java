package com.zhaocai.business.service;

import com.zhaocai.business.base.SpringBaseTest;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.procurement.service.IMaterialsListService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class MaterialsListServiceTest extends SpringBaseTest {

    @Autowired
    private IMaterialsListService materialsListService;

    @Test
    public void getSubjectMatterTest() {
        List<String> subjectMatterList = new ArrayList<>();

        subjectMatterList.add("A1=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A1",null));
        subjectMatterList.add("A12=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A12",null));
        subjectMatterList.add("A101=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A101",null));
        subjectMatterList.add("A1013=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A1013",null));
        subjectMatterList.add("A10101=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A10101",null));
        subjectMatterList.add("A101013=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A101013",null));
        subjectMatterList.add("A1010134=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A1010134",null));
        subjectMatterList.add("A10101345=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A10101345",null));

        subjectMatterList.add("A2=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A2",null));
        subjectMatterList.add("A22=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A22",null));
        subjectMatterList.add("A201=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A201",null));
        subjectMatterList.add("A2012=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A2012",null));
        subjectMatterList.add("A20101=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A20101",null));
        subjectMatterList.add("A201012=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A201012",null));
        subjectMatterList.add("A2010123=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A2010123",null));
        subjectMatterList.add("A20101234=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A20101234",null));

        subjectMatterList.add("A3=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A3",null));
        subjectMatterList.add("A31=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A31",null));
        subjectMatterList.add("A301=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A301",null));
        subjectMatterList.add("A3011=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A3011",null));
        subjectMatterList.add("A30101=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A30101",null));
        subjectMatterList.add("A301011=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A301011",null));
        subjectMatterList.add("A3010112=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A3010112",null));
        subjectMatterList.add("A30101123=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.PURCHASE_MATERIALS.getType(), "A30101123",null));

//        subjectMatterList.add("A4=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(), "A4",null));
//        subjectMatterList.add("A41=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(), "A41",null));
//        subjectMatterList.add("A411=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(), "A411",null));
        subjectMatterList.add("A4001=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(), "A4001",null));
        subjectMatterList.add("A40011=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(), "A40011",null));
        subjectMatterList.add("A400111=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(), "A400111",null));
        subjectMatterList.add("A4010001=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(), "A4010001",null));
        subjectMatterList.add("A40100011=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(), "A40100011",null));
        subjectMatterList.add("A401000111=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(), "A401000111",null));
        subjectMatterList.add("A4010001112=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.RENTAL_MACHINERY.getType(), "A4010001112",null));

        subjectMatterList.add("A1=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A1",null));
        subjectMatterList.add("A12=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A12",null));
        subjectMatterList.add("A101=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A101",null));
        subjectMatterList.add("A1013=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A1013",null));
        subjectMatterList.add("A10101=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A10101",null));
        subjectMatterList.add("A101013=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A101013",null));
        subjectMatterList.add("A1010134=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A1010134",null));
        subjectMatterList.add("A10101345=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A10101345",null));

        subjectMatterList.add("A2=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A2",null));
        subjectMatterList.add("A22=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A22",null));
        subjectMatterList.add("A201=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A201",null));
        subjectMatterList.add("A2012=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A2012",null));
        subjectMatterList.add("A20101=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A20101",null));
        subjectMatterList.add("A201012=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A201012",null));
        subjectMatterList.add("A2010123=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A2010123",null));
        subjectMatterList.add("A20101234=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A20101234",null));

        subjectMatterList.add("A3=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A3",null));
        subjectMatterList.add("A31=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A31",null));
        subjectMatterList.add("A301=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A301",null));
        subjectMatterList.add("A3011=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A3011",null));
        subjectMatterList.add("A30101=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A30101",null));
        subjectMatterList.add("A301011=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A301011",null));
        subjectMatterList.add("A3010112=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A3010112",null));
        subjectMatterList.add("A30101123=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A30101123",null));

//        subjectMatterList.add("A4=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A4",null));
//        subjectMatterList.add("A41=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A41",null));
//        subjectMatterList.add("A411=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A411",null));
        subjectMatterList.add("A4001=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A4090",null));
        subjectMatterList.add("A40011=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A40011",null));
        subjectMatterList.add("A400111=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A400111",null));
        subjectMatterList.add("A4010001=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A4010001",null));
        subjectMatterList.add("A40100011=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A40100011",null));
        subjectMatterList.add("A401000111=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A401000111",null));
        subjectMatterList.add("A4010001112=" + materialsListService.getSubjectMatter(ProcurementPlanTypeEnum.OTHER_TYPE.getType(), "A4010001112",null));

        subjectMatterList.forEach(System.out::println);
    }
}
