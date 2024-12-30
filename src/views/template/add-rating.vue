<template>
  <div class="app-container">
    <BackButton path="/template/template" title="新增评分模板">
      <div class="page-title-right">
        <el-button
          type="primary"
          plain
          size="mini"
          :disabled="isSubmit"
          @click="$tab.closePage()"
          >取消</el-button
        >
        <el-button
          type="primary"
          size="mini"
          @click="submitForm('form')"
          :disabled="isSubmit"
          :loading="isSubmit"
          >{{ isSubmit ? "提交中..." : "确定" }}</el-button
        >
      </div>
    </BackButton>
    <div class="context">
      <el-form
        :model="formData"
        ref="form"
        :rules="rules"
        label-position="right"
        label-width="110px"
        size="medium"
        @submit.native.prevent
      >
        <PageTitle title="基本信息" />
        <div class="form-body">
          <el-row :gutter="30">
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="模板名称"
                prop="name"
                class="required label-right-align"
              >
                <el-input type="text" clearable v-model="formData.name" />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label="评分类型" prop="selectedTypes">
                <el-checkbox-group v-model="formData.selectedTypes">
                  <el-checkbox
                    v-for="dict in dict.type.mark_item_type"
                    :label="dict.value"
                    :key="dict.value"
                    >{{ dict.label }}</el-checkbox
                  >
                </el-checkbox-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="使用单位" prop="useUnit">
                <treeselect
                  v-model="formData.useUnit"
                  :options="treeData"
                  clearValueText="清除"
                  noOptionsText="暂无数据"
                  placeholder="请选择使用单位"
                  @select="treeSelect"
                />
              </el-form-item>
            </el-col>

            <!-- <el-col :span="8" class="grid-cell">
            <el-form-item label="使用单位" prop="useUnit">
              <el-select v-model="formData.useUnit" placeholder="请选择使用单位">
                <el-option v-for="unit in units" :key="unit.value" :label="unit.label" :value="unit.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col> -->
          </el-row>
          <el-row>
            <el-col :span="8" class="grid-cell" prop="createUser">
              <el-form-item
                label="维护人"
                prop="projectCode"
                class="required label-right-align"
              >
                <el-input
                  style="width: 95%"
                  v-model="formData.createUser"
                  disabled
                ></el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
        <PageTitle title="设置评分模板内容" />
        <div
          v-for="(table, index) in formData.biddingMarkCategoryVOList"
          :key="index"
          class="table-section"
        >
          <div class="form-body">
            <el-row :gutter="40">
              <el-col :span="10" class="grid-cell">
                <el-form-item
                  label="评分项类型"
                  class="required label-right-align"
                >
                  <template slot-scope>
                    <el-checkbox
                      v-for="dict in dict.type.mark_item_type"
                      :checked="table.itemType === dict.value"
                      :label="dict.value"
                      :key="dict.value"
                      disabled
                      >{{ dict.label }}</el-checkbox
                    >
                  </template>
                </el-form-item>
              </el-col>
              <el-col :span="10" class="grid-cell">
                <el-form-item label="总分">
                  <template slot-scope>
                    <el-input v-model="table.totalScore" disabled></el-input>
                  </template>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="40">
              <el-col :span="20" class="grid-cell">
                <el-form-item label="评分项" class="required label-right-align">
                  <el-table
                    :data="table.biddingMarkItemVOList"
                    default-expand-all
                    border
                    stripe
                  >
                    <el-table-column label="序号" type="index" width="50">
                    </el-table-column>
                    <el-table-column width="80">
                      <template #header>
                        <el-button
                          type="success"
                          @click="addRow(index)"
                          icon="el-icon-plus"
                          circle
                          size="mini"
                        ></el-button>
                      </template>
                      <template slot-scope="scope">
                        <div style="display: flex">
                          <el-button
                            type="success"
                            @click="addSubRow(index, scope.$index)"
                            icon="el-icon-plus"
                            circle
                            size="mini"
                          ></el-button>
                          <el-button
                            type="danger"
                            @click="removeRow(index, scope.$index)"
                            icon="el-icon-minus"
                            size="mini"
                            circle
                          ></el-button>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column type="expand">
                      <template slot-scope="scope">
                        <div class="subItems">
                          <el-table :data="scope.row.subBiddingMarkItemVOList">
                            <el-table-column
                              label="序号"
                              type="index"
                              width="50"
                            >
                            </el-table-column>
                            <el-table-column label="" width="80">
                              <template slot-scope="subScope">
                                <el-button
                                  type="danger"
                                  size="mini"
                                  @click="
                                    removeSubRow(
                                      index,
                                      scope.$index,
                                      subScope.$index
                                    )
                                  "
                                  icon="el-icon-minus"
                                  circle
                                ></el-button>
                              </template>
                            </el-table-column>
                            <el-table-column label="子评分项名称">
                              <template slot-scope="subScope">
                                <el-form-item
                                  :prop="
                                    'biddingMarkCategoryVOList.' +
                                    index +
                                    '.biddingMarkItemVOList.' +
                                    scope.$index +
                                    '.subBiddingMarkItemVOList.' +
                                    subScope.$index +
                                    '.name'
                                  "
                                  :rules="nameRules"
                                >
                                  <el-input
                                    v-model="subScope.row.name"
                                  ></el-input>
                                </el-form-item>
                              </template>
                            </el-table-column>
                            <!-- <el-table-column  label="最低分">
                            <template slot-scope="subScope">
                              <el-form-item :prop="'formData.biddingMarkCategoryVOList.' +
                                index +
                                '.biddingMarkItemVOList.' +
                                scope.$index +
                                '.subBiddingMarkItemVOList.' +
                                subScope.$index +
                                '.lowRange'
                                " >
                                <el-input v-model.number="subScope.row.lowRange"
                                  @blur="validateScores(index, scope.$index)" :class="{
                                    'is-invalid': subScope.row.lowRange < 1,
                                  }"></el-input>
                              </el-form-item>
                            </template>
                          </el-table-column> -->
                            <el-table-column label="分值" align="center">
                              <template slot-scope="subScope">
                                <el-form-item
                                  :prop="
                                    'biddingMarkCategoryVOList.' +
                                    index +
                                    '.biddingMarkItemVOList.' +
                                    scope.$index +
                                    '.subBiddingMarkItemVOList.' +
                                    subScope.$index +
                                    '.highRange'
                                  "
                                  :rules="numberRules"
                                >
                                  <el-input
                                    v-model.number="subScope.row.highRange"
                                    @blur="validateScores(index, scope.$index)"
                                    :class="{
                                      'is-invalid':
                                        subScope.row.highRange === 0,
                                    }"
                                  ></el-input>
                                </el-form-item>
                              </template>
                            </el-table-column>
                            <el-table-column label="评分描述">
                              <template slot-scope="subScope">
                                <el-form-item
                                  :prop="
                                    'biddingMarkCategoryVOList.' +
                                    index +
                                    '.biddingMarkItemVOList.' +
                                    scope.$index +
                                    '.subBiddingMarkItemVOList.' +
                                    subScope.$index +
                                    '.contant'
                                  "
                                  :rules="contantRules"
                                >
                                  <el-input
                                    v-model="subScope.row.contant"
                                  ></el-input>
                                </el-form-item>
                              </template>
                            </el-table-column>
                          </el-table>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column label="评分项名称">
                      <template slot-scope="scope">
                        <el-form-item
                          :prop="
                            'biddingMarkCategoryVOList.' +
                            index +
                            '.biddingMarkItemVOList.' +
                            scope.$index +
                            '.name'
                          "
                          :rules="nameRules"
                        >
                          <el-input v-model="scope.row.name"></el-input>
                        </el-form-item>
                      </template>
                    </el-table-column>
                    <!-- <el-table-column prop="lowRange" label="最低分">
                    <template slot-scope="scope">
                      <el-form-item v-if="!scope.row.subBiddingMarkItemVOList.length" :prop="'biddingMarkCategoryVOList.' +
                        index +
                        '.biddingMarkItemVOList.' +
                        scope.$index +
                        '.lowRange'
                        " :rules="numberRules">
                        <el-input v-model.number="scope.row.lowRange" @blur="validateS"
                          :class="{ 'is-invalid': scope.row.lowRange < 1 }"></el-input>
                      </el-form-item>
                      <span v-else>{{ scope.row.lowRange }}</span>
                    </template>
                  </el-table-column> -->
                    <el-table-column
                      prop="highRange"
                      label="分值"
                      align="center"
                    >
                      <template slot-scope="scope">
                        <el-form-item
                          v-if="!scope.row.subBiddingMarkItemVOList.length"
                          :prop="
                            'biddingMarkCategoryVOList.' +
                            index +
                            '.biddingMarkItemVOList.' +
                            scope.$index +
                            '.highRange'
                          "
                          :rules="numberRules"
                        >
                          <el-input
                            v-model.number="scope.row.highRange"
                            @blur="validateS"
                            :class="{ 'is-invalid': scope.row.highRange === 0 }"
                          ></el-input>
                        </el-form-item>
                        <span v-else>{{ scope.row.highRange }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="评分描述">
                      <template slot-scope="scope">
                        <el-form-item
                          :prop="
                            'biddingMarkCategoryVOList.' +
                            index +
                            '.biddingMarkItemVOList.' +
                            scope.$index +
                            '.contant'
                          "
                          :rules="contantRules"
                        >
                          <el-input v-model="scope.row.contant"></el-input>
                        </el-form-item>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import { Base64 } from "js-base64";
import { addUpdateRating } from "@/api/template/rating";
import BackButton from "@/components/BackButton/index.vue";
import PageTitle from "@/components/PageTitle/index.vue";
import { listOrganizationCall } from "@/api/template/file";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
export default {
  name: "add-rating",
  dicts: ["mark_item_type"],
  data() {
    let checkNum = (rule, value, callback) => {
      if (!/^[1-9]\d*$/.test(value)) {
        callback(new Error("请输入正整数"));
      } else {
        callback();
      }
    };
    return {
      formData: {
        name: "",
        selectedTypes: [],
        createUser: "",
        useUnit: null,
        biddingMarkCategoryVOList: [],
      },
      scoreTypes: ["技术评分", "商务评分"],
      units: [
        { value: "unit1", label: "单位一" },
        { value: "unit2", label: "单位二" },
        { value: "unit3", label: "单位三" },
      ],
      tables: [],
      // planList: [],
      // inventoryList: [],
      numberRules: [
        { required: true, message: "请输入分数", trigger: "blur" },
        {
          type: "number",
          message: "分数必须为数字",
          trigger: ["blur", "change"],
        },
      ],
      nameRules: [{ required: true, message: "请输入名称", trigger: "blur" }],
      contantRules:[{ required: true, message: "请输入评分描述", trigger: "blur" }],
      rules: {
        name: [{ required: true, message: "请输入模板名称", trigger: "blur" }],
        createUser: [
          {
            required: true,
            message: "请获取维护人",
            trigger: ["blur", "change"],
          },
        ],
        selectedTypes: [
          { required: true, message: "请选择评分类型", trigger: "change" },
        ],
        useUnit: [
          { required: true, message: "请选择使用单位", trigger: "change" },
        ],
      },
      // 遮罩层
      loading: false,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 查询参数
      // queryParams: {
      //   pageNum: 1,
      //   pageSize: 10,
      //   procurementPlanCode: undefined,
      //   procurementPlanName: undefined,
      //   projectName: undefined,
      //   operator: undefined,
      //   procurementPlanType: 'all'
      // },
      // inventoryVisible: false,
      // splitVisible: false, //是否显示拆分合同
      // splitForm: {
      //   num: ''
      // }, //拆分合同数表单
      // splitRules: {
      //   num: [
      //     { required: true, message: '请输入拆分的份数', trigger: 'blur' },
      //     { validator: checkNum, trigger: 'blur' }
      //   ],
      // },
      // currentContract: {},
      isSubmit: false,
      // indexs:[],
      // operatorList:[]
      treeData: [],
      unitId: "",
    };
  },
  components: {
    BackButton,
    PageTitle,
    Treeselect,
  },
  created() {
    (this.formData = {
      name: "",
      selectedTypes: [],
      createUser: "",
      useUnit: null,
      biddingMarkCategoryVOList: [],
    }),
      (this.isSubmit = false),
      (this.formData.createUser = this.$store.state.user.nickname);
    this.listOrganization4Company();
    // this.getList();
    // this.getListProcurementOfficer()
  },
  methods: {
    //提交
    submitForm(formName) {
      // console.log(this.planList,'ppp');
      this.isSubmit = true;
      console.log("保存--this.formData",this.formData);
      console.log("保存--this.unitId",this.unitId);
      if(!this.unitId) {
        this.$set(this.formData, "useUnit", undefined);
      }
      this.$refs[formName].validate(async (valid, done) => {
        if (valid) {
          // const { indexs } = this;

          let isValid = true;
          let totalMaxScore = 0;
          let totalMinScore = 0;
          this.formData = { ...this.formData, useUnit: this.unitId };
          this.formData.biddingMarkCategoryVOList.forEach((table) => {
            table.biddingMarkItemVOList.forEach((item) => {
              if (item.name.trim() === "") {
                this.$message.error("评分项名称不能为空");
                isValid = false;
                this.isSubmit = false;
                loading.close();
                done();
                return;
              }
              // if (!item.subBiddingMarkItemVOList.length && item.lowRange === null || !item.subBiddingMarkItemVOList.length && item.lowRange < 1) {
              //   this.$message.error("最低分不能为空且必须大于等于1");
              //   isValid = false;
              //   this.isSubmit = false;
              //   loading.close();
              //   done();
              //   return;
              // }
              if (
                (!item.subBiddingMarkItemVOList.length &&
                  item.highRange === null) ||
                (!item.subBiddingMarkItemVOList.length && item.highRange === 0)
              ) {
                this.$message.error("最高分不能为空且不能为0");
                isValid = false;
                this.isSubmit = false;
                loading.close();
                done();
                return;
              }

              if (!item.subBiddingMarkItemVOList.length) {
                totalMaxScore += item.highRange;
                totalMinScore += item.lowRange;
              } else {
                item.subBiddingMarkItemVOList.forEach((subItem) => {
                  totalMaxScore += subItem.highRange;
                  totalMinScore += subItem.lowRange;
                });
              }

              item.subBiddingMarkItemVOList.length &&
                item.subBiddingMarkItemVOList.forEach((subItem) => {
                  if (subItem.name.trim() === "") {
                    this.$message.error("子评分项名称不能为空");
                    isValid = false;
                    return;
                  }
                  // if (subItem.lowRange === null || subItem.lowRange < 1) {
                  //   this.$message.error("子评分项最低分不能为空且必须大于等于1");
                  //   isValid = false;
                  //   return
                  // }
                  if (subItem.highRange === null || subItem.highRange === 0) {
                    this.$message.error("子评分项最高分不能为空且不能为0");
                    isValid = false;
                    return;
                  }
                });
            });
          });

          if (!isValid) {
            this.isSubmit = false;
            loading.close();
            done();
            return;
          }

          if (totalMaxScore > 100) {
            this.$message.error("总分不能超过100分");
            this.isSubmit = false;
            loading.close();
            done();
            return;
          }
          // if (totalMinScore > 100) {
          //   this.$message.error("所有表格的最低分加起来不能超过100分");
          //   this.isSubmit = false;
          //   loading.close();
          //   done();
          //   return;
          // }

          // 提交逻辑
          console.log("提交表单数据:", this.formData);
          const loading = this.$loading({
            lock: true,
            text: "数据提交中...",
            background: "rgba(0, 0, 0, 0.7)",
          });

          try {
            const res = await addUpdateRating(this.formData);
            loading.close();
            this.$message({
              message: "保存成功",
              type: "success",
            });
            console.log(res, "r~~~~~~~~~~~~~~~~~");
            this.$tab
              .closePage()
              .then(() => {
                // 执行结束的逻辑
                this.$router.push({
                  path: "/template/template",
                  query: { paramName: "rating" },
                });
              })
              .then(() => {
                console.log("跳转时传递的参数:", this.$route.query.paramName); // 确保这里已经有值
              });
          } catch (err) {
            console.log(err);
            this.isSubmit = false;
            loading.close();
            done();
          }
        } else {
          this.isSubmit = false;
          return false;
        }
      });
    },

    async listOrganization4Company() {
      const res = await listOrganizationCall();
      this.treeData = this.normalizeOptions(res.data);
    },
    normalizeOptions(options) {
      const normalizedOptions = [];
      if (options) {
        for (const option of options) {
          // 创建一个规范化选项对象，将id和label属性映射到该对象中
          const normalizedOption = {
            id: option.organizationId,
            label: option.organizationName,
            disabled: option.organizationType === "2",
            organizationId: option.organizationId,
          };
          // 检查当前选项是否有子选项
          if (option.children && option.children.length > 0) {
            // 如果有子选项，递归调用normalizeOptions方法对子选项进行规范化
            // 并将规范化后的子选项数组赋值给当前选项的children属性
            normalizedOption.children = this.normalizeOptions(option.children);
          }
          // 将规范化后的选项对象添加到normalizedOptions数组中
          normalizedOptions.push(normalizedOption);
        }
      }
      return normalizedOptions;
    },
    treeSelect(value) {
      if (value.disabled) {
        this.$modal.msgError("该单位不能选择");
        // this.formData.useUnit = "";
        this.$set(this.formData,"useUnit",undefined);
        this.unitId = undefined;
      } else {
        this.unitId = value.organizationId;
        this.$refs.fileFormRef.clearValidate("useUnit");
      }
    },
    submit() {
      console.log("sub");
    },
    updateTables(selectedTypes) {
      // 对比之前的参数
      const newTypes = selectedTypes.filter(
        (type) =>
          !this.formData.biddingMarkCategoryVOList.some(
            (obj) => obj.itemType === type
          )
      );
      console.log("新增的类型：", newTypes);
      const missingTypes = this.formData.biddingMarkCategoryVOList
        .filter((obj) => !selectedTypes.includes(obj.itemType))
        .map((obj) => obj.itemType);
      console.log("缺失的类型：", missingTypes);
      if (newTypes && newTypes.length > 0) {
        let newT = newTypes.map((type) => ({
          itemType: type,
          totalScore: 0,
          biddingMarkItemVOList: [
            {
              name: "",
              lowRange: null,
              highRange: null,
              contant: null,
              subBiddingMarkItemVOList: [],
            },
          ],
        }));
        this.formData.biddingMarkCategoryVOList =
          this.formData.biddingMarkCategoryVOList.concat(newT);
      }

      if (missingTypes && missingTypes.length > 0) {
        this.formData.biddingMarkCategoryVOList =
          this.formData.biddingMarkCategoryVOList.filter(
            (obj) => !missingTypes.includes(obj.itemType)
          );
      }
    },
    addRow(tableIndex) {
      this.formData.biddingMarkCategoryVOList[
        tableIndex
      ].biddingMarkItemVOList.push({
        name: "",
        lowRange: null,
        highRange: null,
        contant:null,
        subBiddingMarkItemVOList: [],
      });
    },
    addSubRow(tableIndex, rowIndex) {
      this.formData.biddingMarkCategoryVOList[tableIndex].biddingMarkItemVOList[
        rowIndex
      ].subBiddingMarkItemVOList.push({
        name: "",
        lowRange: null,
        highRange: null,
        contant: null,
      });
      this.$forceUpdate(); // 强制刷新视图
    },
    removeRow(tableIndex, rowIndex) {
      this.formData.biddingMarkCategoryVOList[
        tableIndex
      ].biddingMarkItemVOList.splice(rowIndex, 1);
      this.validateS();
    },
    removeSubRow(tableIndex, rowIndex, subRowIndex) {
      this.formData.biddingMarkCategoryVOList[tableIndex].biddingMarkItemVOList[
        rowIndex
      ].subBiddingMarkItemVOList.splice(subRowIndex, 1);
    },
    validateS() {
      let totalMaxScore = 0;
      let totalMinScore = 0;

      this.formData.biddingMarkCategoryVOList.forEach((table) => {
        let num = 0;
        table.biddingMarkItemVOList.forEach((item) => {
          if (
            item.subBiddingMarkItemDetailVOList &&
            item.subBiddingMarkItemDetailVOList.length
          ) {
            item.subBiddingMarkItemDetailVOList.forEach((subItem) => {
              totalMaxScore += subItem.highRange;
              totalMinScore += subItem.lowRange;
              num += subItem.highRange;
            });
          } else {
            totalMaxScore += item.highRange;
            totalMinScore += item.lowRange;
            num += item.highRange;
          }
        });
        table.totalScore = num;
      });

      if (totalMaxScore > 100) {
        this.$message.error("总分不能超过100分");
        return;
      }
      // if (totalMinScore > 100) {
      //   this.$message.error("所有父表格的最低分加起来不能超过100分");
      //   return;
      // }
    },
    validateScores(tableIndex, rowIndex) {
      const parentItem = this.formData.biddingMarkCategoryVOList[tableIndex];
      const item =
        this.formData.biddingMarkCategoryVOList[tableIndex]
          .biddingMarkItemVOList[rowIndex];
      let subMinScoreSum = 0;
      let subMaxScoreSum = 0;

      item.subBiddingMarkItemVOList.forEach((subItem) => {
        subMinScoreSum += subItem.lowRange || 0;
        subMaxScoreSum += subItem.highRange || 0;
      });

      let totalScore = 0;
      parentItem.biddingMarkItemVOList.forEach((item) => {
        if (
          item.subBiddingMarkItemVOList &&
          item.subBiddingMarkItemVOList.length
        ) {
          item.subBiddingMarkItemVOList.forEach((subItem) => {
            totalScore += subItem.highRange || 0;
          });
        } else {
          totalScore += item.highRange || 0;
        }
      });

      item.lowRange = subMinScoreSum;
      item.highRange = subMaxScoreSum;
      parentItem.totalScore = totalScore;

      console.log("item.minScore", item.lowRange);
      console.log("item.subMinScoreSum", subMinScoreSum);
      console.log("item.maxScore", item.highRange);
      console.log("item.subMaxScoreSum", subMaxScoreSum);

      // if (item.lowRange != subMinScoreSum || item.highRange != subMaxScoreSum) {
      //   this.$message.error(
      //     "子表格的最低分总和和最高分总和必须等于当前父表格所在行的最低分和最高分"
      //   );
      // }
    },
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          let isValid = true;
          let totalMaxScore = 0;
          let totalMinScore = 0;

          this.formData.biddingMarkCategoryVOList.forEach((table) => {
            table.biddingMarkItemVOList.forEach((item) => {
              if (item.name.trim() === "") {
                this.$message.error("评分项名称不能为空");
                isValid = false;
              }
              // if (item.lowRange === null || item.lowRange < 1) {
              //   this.$message.error("最低分不能为空且必须大于等于1");
              //   isValid = false;
              // }
              // if (item.highRange === null || item.highRange === 0) {
              //   this.$message.error("最高分不能为空且不能为0");
              //   isValid = false;
              // }

              totalMaxScore += item.highRange;
              totalMinScore += item.lowRange;

              item.subBiddingMarkItemVOList.forEach((subItem) => {
                if (subItem.name.trim() === "") {
                  this.$message.error("子评分项名称不能为空");
                  isValid = false;
                }
                // if (subItem.lowRange === null || subItem.lowRange < 1) {
                //   this.$message.error("子评分项最低分不能为空且必须大于等于1");
                //   isValid = false;
                // }
                // if (subItem.highRange === null || subItem.highRange === 0) {
                //   this.$message.error("子评分项最高分不能为空且不能为0");
                //   isValid = false;
                // }
              });
            });
          });

          if (!isValid) {
            this.$message.error("请完善表单信息");
            return;
          }

          if (totalMaxScore > 100) {
            this.$message.error("总分不能超过100分");
            return;
          }
          // if (totalMinScore > 100) {
          //   this.$message.error("所有父表格的最低分加起来不能超过100分");
          //   return;
          // }

          // 提交逻辑
          console.log("提交表单数据:", this.formData);
        } else {
          this.$message.error("请完成表单校验后再提交");
          return false;
        }
      });
    },
  },
  watch: {
    '$route': {
      immediate: true,
      handler(newVal) {
        (this.formData = {
          name: "",
          selectedTypes: [],
          createUser: "",
          useUnit: null,
          biddingMarkCategoryVOList: [],
        }),
          (this.isSubmit = false),
          (this.formData.createUser = this.$store.state.user.nickname);
        this.listOrganization4Company();
      }
    },
    "formData.selectedTypes": function (newVal) {
      this.updateTables(newVal);
    },
  },
  // watch: {
  //   '$route.params.params': {
  //     async handler(val) {
  //       if (val) {
  //         const param = JSON.parse(Base64.decode(val))
  //         console.log(param,'param--param--param');
  //         this.currentContract = param;
  //         this.planList.push(param)
  //         const { contractCode } = this.currentContract
  //         const res = await getContractMaterials(contractCode)
  //         this.inventoryList = res.data;
  //         this.formData.projectName = param.contractName;
  //         this.formData.projectCode = param.contractCode;
  //       }
  //     },
  //     immediate: true
  //   },
  // }
};
</script>
<style lang="scss" scoped>
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
  padding: 20px;
}

.tables-tit {
  margin-right: 8px;
}

.table-section {
  margin: 0 20px 20px;
}

// .table-items {
//   // margin: 0 20px;
// }

.subItems {
  margin-left: 50px;
}

.is-invalid {
  border-color: red;
}
::v-deep .el-table .el-table__cell {
  padding: 5px 0 !important;
}
</style>
