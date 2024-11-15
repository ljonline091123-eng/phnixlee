<template>
  <div>
    <el-form label-width="100px" class="form-body" label-suffix=":">
      <el-row :gutter="40">
        <el-col :span="8">
          <el-form-item label="供应商名称" class="custom-form-item">
            <span>{{ vendorInfo.vnedorName }}</span>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="投标总价(元)" class="custom-form-item">
            <span>{{ vendorInfo.notTaxPricePattern }}</span>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="联系人" class="custom-form-item">
            <span>{{ vendorInfo.contact }}</span>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="联系电话" class="custom-form-item">
            <span>{{ vendorInfo.phone }}</span>
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="投标附件" class="custom-form-item">
            <div v-if="vendorInfo.attachments">
              <div v-for="(item,index) in vendorInfo.attachments" :key="index">
                <a :href="item.fileUrl" target="_blank" class="link-type">{{ item.fileName }}</a>
              </div>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <div class="page-title">
      <span>清单报价</span>
    </div>
    <!-- <el-table  :data="backBidList" stripe  highlight-current-row size="small" border>
    <el-table-column label="序号" type="index" width="50" align="center" />
    <el-table-column label="清单编码" align="center" key="materialsCode" prop="materialsCode" show-overflow-tooltip/>
    <el-table-column label="清单名称" key="materialsName" prop="materialsName" show-overflow-tooltip/>
    <el-table-column label="规格型号" align="center" key="specification" prop="specification" show-overflow-tooltip/>
    <el-table-column label="计量单位" align="center" key="unitMeasurement" prop="unitMeasurement" />
    <el-table-column label="数量" align="center" key="count" prop="count" />
    <el-table-column label="含税单价(元)" align="right" key="taxUnitPricePattern" prop="taxUnitPricePattern" />
    <el-table-column label="不含税单价(元)" align="right" key="notTaxUnitPricePattern" prop="notTaxUnitPricePattern" />
    <el-table-column label="含税总价(元)" align="right" key="taxPricePattern" prop="taxPricePattern" />
    <el-table-column label="不含税总价(元)" align="right" key="notTaxPricePattern" prop="notTaxPricePattern" />
    <el-table-column label="税率(%)" align="center" key="taxRate" prop="taxRate" />
    <el-table-column label="发票类型" align="center" key="billTypeText" prop="billTypeText" show-overflow-tooltip/>
  </el-table> -->

    <el-table
      size="small"
      :data="backBidList"
      row-key="rowId"
      highlight-current-row
      border
      :tree-props="{
        hasChildren: 'hasChildren',
        children: 'compVOList',
      }"
      default-expand-all
    >
      <el-table-column
        label="序号"
        type="index"
        width="50"
        align="center"
        fixed="left"
      />
      <el-table-column
        prop="contractPlanningName"
        label="合约规划名称"
        show-overflow-tooltip
        width="130"
        align="left"
      ></el-table-column>
      <el-table-column
        prop="splitContractName"
        label="拆分合约规划名称"
        width="130"
        align="center"
      ></el-table-column>
      <el-table-column
        prop="contractScope"
        label="拟签约合同拆包范围"
        width="150"
        align="center"
      >
      </el-table-column>
      <el-table-column prop="materialsLists" label="清单" align="center">
        <template #default="{ row }">
          <span v-if="!row.compVOList">
            <el-table
              size="small"
              :data="row.materialsLists || []"
              :style="{ width: 'calc(100% - 1px)' }"
            >
              <el-table-column
                label="序号"
                type="index"
                width="50"
                align="center"
                fixed="left"
              />
              <el-table-column
                label="清单名称"
                width="200"
                align="left"
                prop="materialsName"
                fixed="left"
                show-overflow-tooltip
              />
              <el-table-column
                label="价格类型"
                width="200"
                align="left"
                prop="priceType"
                fixed="left"
                :formatter="formatterPriceType"
                show-overflow-tooltip
              />
              <el-table-column
                label="交易标的物"
                width="150"
                align="left"
                prop="subjectMatterName"
              />
              <el-table-column
                label="清单编码"
                width="200"
                align="left"
                prop="materialsCode"
              />
              <el-table-column
                label="规格型号"
                align="center"
                width="150"
                prop="specification"
              />
              <el-table-column
                label="计量单位"
                align="center"
                prop="unitMeasurement"
              />
              <el-table-column label="清单数量" align="center" prop="count" />
              <el-table-column
                label="税率（%）"
                align="center"
                prop="taxRate"
                width="100"
              >
              </el-table-column>

              <el-table-column
                label="浮动价"
                width="100"
                align="right"
                prop="floatingPriceText"
                v-if="
                  (vendorInfo.subjectMatterType === 1 ||
                    vendorInfo.subjectMatterType === 2) &&
                  vendorInfo.priceType === 2
                "
              >
                <template #default="{ row }">
                  <span>{{
                    row.floatingPriceText ? row.floatingPriceText : "/"
                  }}</span>
                </template>
              </el-table-column>
              <el-table-column
                label="卸费"
                width="100"
                align="right"
                prop="unloadingFeeText"
                v-if="
                  (vendorInfo.subjectMatterType === 1 ||
                    vendorInfo.subjectMatterType === 2) &&
                  vendorInfo.priceType === 2
                "
              >
              </el-table-column>
              <el-table-column
                label="基价"
                width="100"
                align="right"
                prop="basePriceText"
                v-if="
                  (vendorInfo.subjectMatterType === 1 ||
                    vendorInfo.subjectMatterType === 2) &&
                  vendorInfo.priceType === 2
                "
              >
              </el-table-column>
              <el-table-column
                label="含税单价(元)"
                width="100"
                align="right"
                prop="taxUnitPrice"
              >
                <template #default="{ row }">
                  <span
                    style="
                      display: inline-block;
                      text-align: center;
                      width: 100%;
                    "
                    v-if="
                      (vendorInfo.subjectMatterType === 1 ||
                        vendorInfo.subjectMatterType === 2) &&
                      vendorInfo.priceType === 2
                    "
                    >/</span
                  >
                  <span v-else>{{ row.taxUnitPriceText }}</span>
                </template>
              </el-table-column>
              <el-table-column
                label="不含税单价(元)"
                width="150"
                align="right"
                prop="notTaxUnitPrice"
              >
                <template #default="{ row }">
                  <span
                    style="
                      display: inline-block;
                      text-align: center;
                      width: 100%;
                    "
                    v-if="
                      (vendorInfo.subjectMatterType === 1 ||
                        vendorInfo.subjectMatterType === 2) &&
                      vendorInfo.priceType === 2
                    "
                    >/</span
                  >
                  <span v-else>{{ row.notTaxUnitPriceText }}</span>
                </template>
              </el-table-column>
              <el-table-column
                label="含税总价(元)"
                width="150"
                align="right"
                prop="taxPrice"
              >
                <template #default="{ row }">
                  <span>{{ row.taxPriceText ? row.taxPriceText : "/" }}</span>
                </template>
              </el-table-column>
              <el-table-column
                label="不含税总价(元)"
                width="150"
                align="right"
                prop="notTaxPrice"
              >
                <template #default="{ row }">
                  <span>{{
                    row.notTaxPriceText ? row.notTaxPriceText : "/"
                  }}</span>
                </template>
              </el-table-column>
              <el-table-column
                label="发票类型"
                align="center"
                prop="billType"
                width="170"
              >
                <template #default="{ row }">
                  <span>{{ getInvoiceType(row.billType) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 招标文件预览 -->
    <!-- <el-dialog title="招标文件预览" :visible.sync="templateDialogVisible" width="80%" modal-append-to-body append-to-body>
    <FileModule :attachmentId="scheme.biddingTemplate && scheme.biddingTemplate.attachmentId" height="500px"/>
  </el-dialog> -->
  </div>
</template>
<script>
import { getBidInfo } from "@/api/procurement/manage";
import FileModule from "@/components/FileModule/index.vue";
import {PRICETYPELIST} from "@/utils/constants";
export default {
  data() {
    return {
      backBidLoading: false,
      backBidList: [],
      vendorInfo: {},
      // templateDialogVisible:false
      invoice_type: [
        { label: "增值税专用发票", value: "1" },
        { label: "增值税普通发票", value: "2" },
        { label: "普通发票", value: "3" },
      ],
    };
  },
  components: {
    FileModule,
  },
  props: {
    id: {
      type: String,
      default: "",
    },
    noticeDetail: {
      type: Object,
      default: () => {},
    },
    scheme: {
      type: Object,
      default: () => {},
    },
  },
  methods: {
    formatterPriceType(_row,_column,cellValue) {
      const findObj= PRICETYPELIST.find((item) => item.value === cellValue);
      return findObj? findObj.label: "未知价格类型";
    },
    // 封装枚举方法
    getInvoiceType(billType) {
      const invoice = this.invoice_type.find((item) => item.value === billType);
      return invoice ? invoice.label : "未知发票类型"; // 如果找不到匹配的类型，返回一个默认值
    },
    generateUniqueId() {
      return "_" + Math.random().toString(36).substr(2, 9);
    },
    assignRowIds(data) {
      data.forEach((item) => {
        // 为当前项分配唯一的 rowId
        item.rowId = this.generateUniqueId();
        // 如果存在 compVOList，递归处理
        if (item.compVOList) {
          item.compVOList.forEach((comp) => {
            // 为 comp 分配唯一的 rowId
            comp.rowId = this.generateUniqueId();
            // 如果存在 materialsLists，递归处理
            if (comp.materialsLists) {
              comp.materialsLists.forEach((material) => {
                // 为 material 分配唯一的 rowId
                material.rowId = this.generateUniqueId();
              });
            }
          });
        }
      });
    },
    async getBidList() {
      console.log(this.noticeDetail, "noticeDetail-");
      console.log(this.scheme, "scheme-scheme");
      this.backBidLoading = true;
      try {
        const res = await getBidInfo(this.id);
        const { data } = res;
        this.assignRowIds(data.materialsList);
        this.backBidList = data.materialsList.map((materialList) => {
          return {
            ...materialList,
            compVOList: materialList.compVOList.map((compVO) => {
              return {
                ...compVO,
                materialsLists: compVO.materialsLists.map((material) => {
                  return {
                    ...material,
                    billType: material.billType
                      ? material.billType.toString()
                      : "1",
                  };
                }),
              };
            }),
          };
        });
        this.vendorInfo = {
          vnedorName: data.vendorName,
          notTaxPricePattern: data.notTaxPricePattern,
          contact: data.contact,
          phone: data.phone,
          attachments: data.attachments,
          subjectMatterType: data.subjectMatterType,
          priceType: data.priceType,
        };
        console.log(this.vendorInfo, "详情");
      } catch (err) {
        console.log(err);
      }
      this.backBidLoading = false;
    },
  },
  watch: {
    id: {
      handler() {
        this.getBidList();
      },
      immediate: true,
    },
  },
};
</script>
<style scoped lang="scss">
.page-title {
  width: 100%;
  border-bottom: solid 1px #ccc;
  padding: 10px;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;

  &::before {
    content: "";
    height: 20px;
    width: 5px;
    background-color: rgba(41, 65, 137, 1);
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
  }
}
.form-body {
  font-size: 13px;
  &::v-deep .el-form-item {
    margin-bottom: 0;
  }
  &::v-deep label {
    font-weight: normal;
  }
}
</style>
