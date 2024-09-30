<template>
  <div class="app-container">
    <el-form :model="formData" ref="form" :rules="rules" label-position="right" label-width="110px" size="medium"
      @submit.native.prevent>
      <div class="page-title">
        <span>基本信息</span>
        <div class="page-title-right">
          <el-button type="primary" plain size="mini" :disabled="isSubmit" @click="$tab.closePage()">取消</el-button>
          <el-button type="primary" size="mini" @click="submitForm('form')" :disabled="isSubmit" :loading="isSubmit">{{
            isSubmit ? "提交中..." : "确定" }}</el-button>
        </div>
      </div>
      <div class="form-body">
        <el-row :gutter="40">
          <el-col :span="8" class="grid-cell">
            <el-form-item label="模板名称" prop="name" class="required label-right-align">
              <el-input type="text" clearable v-model="formData.name" />
            </el-form-item>
          </el-col>
          <el-col :span="8" class="grid-cell">
            <el-form-item label="评分类型" prop="selectedTypes">
              <el-checkbox-group v-model="formData.selectedTypes">
                <el-checkbox v-for="dict in dict.type.mark_item_type" :label="dict.value" :key="dict.value">{{ dict.label }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="40">
          <el-col :span="8" class="grid-cell" prop="createUser">
            <el-form-item label="维护人" prop="projectCode" class="required label-right-align">
              <el-input v-model="formData.createUser" disabled></el-input>
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
      </div>
      <div class="page-title">
        <span>设置评分模板内容</span>
      </div>
      <div v-for="(table, index) in formData.biddingMarkCategoryVOList" :key="index" class="table-section">
        <div class="form-body">
          <el-row :gutter="40">
            <el-col :span="10" class="grid-cell">
              <el-form-item label="评分项类型" class="required label-right-align">
                <template slot-scope>
                    <el-checkbox v-for="dict in dict.type.mark_item_type" :checked="table.itemType === dict.value" :label="dict.value" :key="dict.value" disabled>{{ dict.label }}</el-checkbox>
                </template>
              </el-form-item>
            </el-col>
            <el-col :span="10" class="grid-cell">
              <el-form-item label="总分" >
                <template slot-scope>
                  <el-input v-model="table.totalScore" disabled></el-input>
                </template>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="20" class="grid-cell">
              <el-form-item label="评分项" class="required label-right-align">
                <el-table :data="table.biddingMarkItemVOList" default-expand-all>
                  <el-table-column width="80">
                    <template #header>
                      <el-button @click="addRow(index)" icon="el-icon-plus" circle size="mini" type="success"></el-button>
                    </template>
                    <template slot-scope="scope">
                      <div style="display: flex;">
                        <el-button @click="addSubRow(index, scope.$index)" icon="el-icon-plus" circle size="mini" type="success"></el-button>
                        <el-button type="danger" @click="removeRow(index, scope.$index)" icon="el-icon-minus"
                          circle size="mini"></el-button>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column type="expand">
                    <template slot-scope="scope">
                      <div class="subItems">
                        <el-table :data="scope.row.subBiddingMarkItemDetailVOList">
                          <el-table-column label="" width="80">
                            <template slot-scope="subScope">
                              <el-button type="danger" @click="
                                removeSubRow(
                                  index,
                                  scope.$index,
                                  subScope.$index
                                )
                                " icon="el-icon-minus" circle size="mini"></el-button>
                            </template>
                          </el-table-column>
                          <el-table-column  label="子评分项名称">
                            <template slot-scope="subScope">
                              <el-input v-model="subScope.row.name"></el-input>
                            </template>
                          </el-table-column>
                          <!-- <el-table-column  label="最低分">
                            <template slot-scope="subScope">
                              <el-form-item :prop="'formData.biddingMarkCategoryVOList.' +
                                index +
                                '.biddingMarkItemVOList.' +
                                scope.$index +
                                '.subItems.' +
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
                          <el-table-column  label="分值" align="center">
                            <template slot-scope="subScope">
                              <el-form-item :prop="'formData.biddingMarkCategoryVOList.' +
                                index +
                                '.biddingMarkItemVOList.' +
                                scope.$index +
                                '.subItems.' +
                                subScope.$index +
                                '.highRange'
                                " >
                                <el-input v-model.number="subScope.row.highRange"
                                  @blur="validateScores(index, scope.$index)" :class="{
                                    'is-invalid': subScope.row.highRange === 0,
                                  }"></el-input>
                              </el-form-item>
                            </template>
                          </el-table-column>
                        </el-table>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="'biddingMarkCategoryVOList.' +
                        index +
                        '.biddingMarkItemVOList.' +
                        scope.$index +
                        '.name'
                        " label="评分项名称">
                    <template slot-scope="scope">
                      <el-input v-model="scope.row.name"></el-input>
                    </template>
                  </el-table-column>
                  <!-- <el-table-column prop="lowRange" label="最低分">
                    <template slot-scope="scope">
                      <el-form-item :prop="'biddingMarkCategoryVOList.' +
                        index +
                        '.biddingMarkItemVOList.' +
                        scope.$index +
                        '.lowRange'
                        " :rules="numberRules">
                        <el-input v-model.number="scope.row.lowRange" @blur="validateS"
                          :class="{ 'is-invalid': scope.row.lowRange < 1 }"></el-input>
                      </el-form-item>
                    </template>
                  </el-table-column> -->
                  <el-table-column prop="highRange" align="center" label="分值">
                    <template slot-scope="scope">
                      <el-form-item :prop="'biddingMarkCategoryVOList.' +
                        index +
                        '.biddingMarkItemVOList.' +
                        scope.$index +
                        '.highRange'
                        " :rules="numberRules">
                        <el-input v-model.number="scope.row.highRange" @blur="validateS"
                          :class="{ 'is-invalid': scope.row.highRange === 0 }"></el-input>
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
</template>

<script>
import { Base64 } from "js-base64";
import { addUpdateRating,getRating } from "@/api/template/rating";
export default {
  name: "add-plan",
  dicts: ['mark_item_type'],
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
        useUnit: "",
        biddingMarkCategoryVOList:[],
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
      editTrue: true,
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
    };
  },
  created() {
    this.formData.createUser = this.$store.state.user.name;
    // this.getList();
    // this.getListProcurementOfficer()
  },
  methods: {
    //提交
    submitForm(formName) {
      // console.log(this.planList,'ppp');
      this.isSubmit = true;

      this.$refs[formName].validate(async (valid, done) => {
        if (valid) {
          // const { indexs } = this;

          let isValid = true;
          let totalMaxScore = 0;
          let totalMinScore = 0;

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
              if (item.lowRange === null || item.lowRange < 1) {
                this.$message.error("最低分不能为空且必须大于等于1");
                isValid = false;
                this.isSubmit = false;
            loading.close();
            done();
            return;
              }
              if (item.highRange === null || item.highRange === 0) {
                this.$message.error("最高分不能为空且不能为0");
                isValid = false;
                this.isSubmit = false;
            loading.close();
            done();
            return;
              }

              totalMaxScore += item.highRange;
              totalMinScore += item.lowRange;

              // item.subItems.forEach((subItem) => {
              //   if (subItem.name.trim() === "") {
              //     this.$message.error("子评分项名称不能为空");
              //     isValid = false;
              //   }
              //   if (subItem.lowRange === null || subItem.lowRange < 1) {
              //     this.$message.error("子评分项最低分不能为空且必须大于等于1");
              //     isValid = false;
              //   }
              //   if (subItem.highRange === null || subItem.highRange === 0) {
              //     this.$message.error("子评分项最高分不能为空且不能为0");
              //     isValid = false;
              //   }
              // });
            });
          });

          if (!isValid) {
            this.$message.error("请完善表单信息");
            this.isSubmit = false;
            loading.close();
            done();
            return;
          }

          if (totalMaxScore > 100) {
            this.$message.error("所有表格的最高分加起来不能超过100分");
            this.isSubmit = false;
            loading.close();
            done();
            return;
          }
          if (totalMinScore > 100) {
            this.$message.error("所有表格的最低分加起来不能超过100分");
            this.isSubmit = false;
            loading.close();
            done();
            return;
          }

          // 提交逻辑
          console.log("提交表单数据:", this.formData);
          // if(!indexs.length){
          //   this.isSubmit = false;
          //   this.$message({
          //     message: '拆分合约不能为空',
          //     type: 'error'
          //   });
          //   return false;
          // }
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
            this.$tab.closePage().then(() => {
              // 执行结束的逻辑
              this.$router.push(`/template/rating`);
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

    submit() {
      console.log("sub");
    },
    updateTables(selectedTypes) {
      // 对比之前的参数
      const newTypes = selectedTypes.filter(type => !this.formData.biddingMarkCategoryVOList.some(obj => obj.itemType === type));
      console.log('新增的类型：', newTypes);
      const missingTypes = this.formData.biddingMarkCategoryVOList.filter(obj => !selectedTypes.includes(obj.itemType)).map(obj => obj.itemType);
      console.log('缺失的类型：', missingTypes);
      if(newTypes && newTypes.length > 0){
        let newT = newTypes.map((type) => ({
          itemType: type,
          totalScore: 0,
          biddingMarkItemVOList: [
            {
              name: "",
              lowRange: null,
              highRange: null,
              subItems: [],
            },
          ],
        }));
        this.formData.biddingMarkCategoryVOList = this.formData.biddingMarkCategoryVOList.concat(newT);
      }

      if(missingTypes  && missingTypes.length > 0){
        this.formData.biddingMarkCategoryVOList = this.formData.biddingMarkCategoryVOList.filter(obj => !missingTypes.includes(obj.itemType));
      }
    },
    addRow(tableIndex) {
      this.formData.biddingMarkCategoryVOList[tableIndex].biddingMarkItemVOList.push({
        name: "",
        lowRange: null,
        highRange: null,
        subItems: [],
      });
    },
    addSubRow(tableIndex, rowIndex) {
      this.formData.biddingMarkCategoryVOList[tableIndex].biddingMarkItemVOList[rowIndex].subItems.push({
        name: "",
        lowRange: null,
        highRange: null,
      });
      this.$forceUpdate(); // 强制刷新视图
    },
    removeRow(tableIndex, rowIndex) {
      this.formData.biddingMarkCategoryVOList[tableIndex].biddingMarkItemVOList.splice(rowIndex, 1);
      this.validateS();
    },
    removeSubRow(tableIndex, rowIndex, subRowIndex) {
      this.formData.biddingMarkCategoryVOList[tableIndex].biddingMarkItemVOList[rowIndex].subItems.splice(subRowIndex, 1);
    },
    validateS() {
      let totalMaxScore = 0;
      let totalMinScore = 0;

      this.formData.biddingMarkCategoryVOList.forEach((table) => {
        let num = 0;
        table.biddingMarkItemVOList.forEach((item) => {
          totalMaxScore += item.highRange;
          totalMinScore += item.lowRange;
          num += item.highRange;
        });
        table.totalScore = num;
      });

      if (totalMaxScore > 100) {
        this.$message.error("所有父表格的最高分加起来不能超过100分");
        return;
      }
      if (totalMinScore > 100) {
        this.$message.error("所有父表格的最低分加起来不能超过100分");
        return;
      }

    },
    validateScores(tableIndex, rowIndex) {
      const item = this.formData.biddingMarkCategoryVOList[tableIndex].biddingMarkItemVOList[rowIndex];
      let subMinScoreSum = 0;
      let subMaxScoreSum = 0;

      item.subItems.forEach((subItem) => {
        subMinScoreSum += subItem.lowRange || 0;
        subMaxScoreSum += subItem.highRange || 0;
      });

      console.log("item.minScore", item.lowRange);
      console.log("item.subMinScoreSum", subMinScoreSum);
      console.log("item.minScore", item.highRange);
      console.log("item.subMinScoreSum", subMaxScoreSum);

      if (item.lowRange != subMinScoreSum || item.highRange != subMaxScoreSum) {
        this.$message.error(
          "子表格的最低分总和和最高分总和必须等于当前父表格所在行的最低分和最高分"
        );
      }
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
              if (item.lowRange === null || item.lowRange < 1) {
                this.$message.error("最低分不能为空且必须大于等于1");
                isValid = false;
              }
              if (item.highRange === null || item.highRange === 0) {
                this.$message.error("最高分不能为空且不能为0");
                isValid = false;
              }

              totalMaxScore += item.highRange;
              totalMinScore += item.lowRange;

              item.subItems.forEach((subItem) => {
                if (subItem.name.trim() === "") {
                  this.$message.error("子评分项名称不能为空");
                  isValid = false;
                }
                if (subItem.lowRange === null || subItem.lowRange < 1) {
                  this.$message.error("子评分项最低分不能为空且必须大于等于1");
                  isValid = false;
                }
                if (subItem.highRange === null || subItem.highRange === 0) {
                  this.$message.error("子评分项最高分不能为空且不能为0");
                  isValid = false;
                }
              });
            });
          });

          if (!isValid) {
            this.$message.error("请完善表单信息");
            return;
          }

          if (totalMaxScore > 100) {
            this.$message.error("所有父表格的最高分加起来不能超过100分");
            return;
          }
          if (totalMinScore > 100) {
            this.$message.error("所有父表格的最低分加起来不能超过100分");
            return;
          }

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
    "formData.selectedTypes": function (newVal) {
      if(this.editTrue){
        return;
      }
      this.updateTables(newVal);
    },
    '$route.params.params': {
      async handler(val) {
        if (val) {
          this.editTrue = true;
          const id = JSON.parse(Base64.decode(val))
          const res = await getRating({id})
          res.data.selectedTypes = res.data.markCategoryDatailVOList.map(obj=>String(obj.itemType));
          // res.data.useUnit = this.units[0].value;
          res.data.biddingMarkCategoryVOList = res.data.markCategoryDatailVOList.map((obj,index)=>{
            res.data.markCategoryDatailVOList[index].itemType =String(obj.itemType);
            res.data.markCategoryDatailVOList[index].biddingMarkItemVOList = obj.markItemDetailVOList;
            return obj;
          })
          this.formData = res.data;
          setTimeout(()=>{
            this.editTrue = false;
          },300)
          
          // markCategoryDatailVOList
          // this.formData.selectedTypes = this.formData.markCategoryDatailVOList.map(obj=>obj.itemType);
        }
      },
      immediate: true
    },
  },

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
</style>
