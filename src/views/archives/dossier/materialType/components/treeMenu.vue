<template>
    <div class="left">
      <div class="custom-tree-node" style="height: 30px;align-items: center;margin:3px 0 0 10px;">
        <span style="font-weight: 500;color: #303133;">{{title}}</span>
      
        <!-- <i
        class="small-operation-btn el-icon-plus"
        @click.stop="handleAdd(data, node)"
           /> -->
        </div>
        <div v-if="queryType" style="   width: 100%;height: 33px;margin: -10px 10px 16px 0; background-color: #ffffff;">
          <el-radio-group
          style="margin:10px 0 0 10px;"
          v-model="queryRadioType"
          size="small"
          @change="handleRadioChange"
        >
            <el-radio-button
            :label="dict.value"
            :name="dict.value" 
            v-for="dict in radioList"
            :key="dict.value"
            >{{ dict.label }}</el-radio-button>
        </el-radio-group>
        </div>
        <el-input
            v-model="label"
            placeholder="请输入名称"
            clearable
            size="small"
            prefix-icon="el-icon-search"
          />
        <div class="circle-buttons" style="margin: 10px 0 0 10px">
          <button
            v-for="n in buttonCount"
            :key="n"
            class="circle-btn"
            :class="levelExpand==n?'circle-btn-selected':'circle-btn'"
            @click="expandNodes(n)"
          >
            {{ n }}
          </button>
        </div>
        <el-tree
          v-loading="loading"
          :data="deptOptions"
          class="address-tree"
          :props="defaultProps"
          :filter-node-method="filterNode"
          :current-node-key="currentNodeKey"
          :default-expanded-keys="defaultExpandedKeys"
          ref="tree"
          node-key="id"
          highlight-current
          @node-click="handleNodeClick"
        >
        <span slot-scope="{ node, data }" class="custom-tree-node">
          <span class="tooltip" >
            <el-tooltip :content="data.label" placement="top" effect="dark">
            <span :class="node.level>2?'add-f-s':'add-f-s-14'">{{ data.label }}</span>
            </el-tooltip>
          </span>
          <!-- v-if="node.isCurrent == true" -->
          <div  v-if="isHoveredNode(node) && isEditable" class="hover-operations operation-view">
            <el-tooltip v-if="isNodeAdd(node)" content="增加子级节点" placement="top" effect="dark">
              <i
                class="small-operation-btn el-icon-plus"
                @click.stop="handleAdd(data, node)"
              />
            </el-tooltip>
            <el-tooltip :content="data.state!='1' && data.state!='3'?'编辑当前节点':'查看当前节点'" placement="top" effect="dark" >
              <i
                v-if="isMaster(node,data)"
                class="small-operation-btn el-icon-edit "
                @click.stop="handleEdit(data, node)"/>

                <i  v-else class="small-operation-btn el-icon-view " @click.stop="handleCheck(data, node)" />
            </el-tooltip>
            
            <el-tooltip content="删除当前节点" placement="top" effect="dark"   v-if="isMaster(node,data)">
              <i
                class="small-operation-btn el-icon-delete"
                @click.stop="handleDelete(data, node)"
              />
            </el-tooltip>
          </div>
          <div v-else  class="hover-operations operation-view">
            <el-tooltip v-if="isEditable"  content="查看当前节点" placement="top" effect="dark" >
              <i
                class="small-operation-btn el-icon-view "
                @click.stop="handleCheck(data, node)"
              />
            </el-tooltip>
          </div>
          <div  v-show="isMain" class="hover-operations operation-view"  >
            <el-tooltip v-if="data.isMain!=='Y' && data.mainId==='0'" content="新增至主库" placement="top" effect="dark">
              <i
                class="small-operation-btn el-icon-circle-plus-outline"
                @click.stop="mainAdd(data, node)"
              />
            </el-tooltip>
            <el-tooltip v-if="data.isMain!=='Y' && data.mainId==='0'" content="关联至主库" placement="top" effect="dark">
              <i
                class="small-operation-btn el-icon-document-copy "
                @click.stop="mainEdit(data, node)"
              />
            </el-tooltip>
            <el-tooltip v-if="data.isMain!=='Y' && data.mainId!=='0'" content="取消关联" placement="top" effect="dark">
              <i
                class="small-operation-btn el-icon-circle-close"
                @click.stop="mainDelete(data, node)"
              />
            </el-tooltip>
          </div>
        </span>
        </el-tree>
        
      </div>
  </template>
  <script>
  import {mapGetters} from "vuex";
  export default {
    name: "treeMenu",
    data() {
      return {
        queryRadioType: "untreated",
        radioList: [
        {
          value: 'untreated',
          label: "未处理",
        },
        {
          value: 'processed',
          label: "已处理",
        },
        {
          value: 'all',
          label: "全部",
        },
      ],
        queryParams: {},
        buttonCount: 1,
        // levelExpand:1, //默认展示的级别
        label:undefined,
        modelOptions: [],
        parentNode:{},
        // currentNodeKey: '3897276558322302978', // 默认选中节点1
        value: '2',
        // 部门名称
        deptName: undefined,
        defaultProps: {
        children: "children",
        label: "label",
      },
      };
    },
    computed:{
    ...mapGetters(['project','org']),
   },
  props: {
 
    // 表格数据
    deptOptions: {
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
    //是否可编辑editable
    isEditable: {
      type: Boolean,
      default: false,
    },
    //是否可操作主库
    isMain: {
      type: Boolean,
      default: false,
    },
    radioType: {
      type: String,
      default: "0",
    },
    title: {
      type: String,
      default: "材料档案",
    },
    currentNodeKey: {
      type: String,
      default: "3897276558322302978",
    },
    defaultExpandedKeys: {
      type: Array,
      default: () => [],
    },
    levelExpand: {
      type: Number,
      default: 1,
    },
     //是否显示查询类型
     queryType: {
      type: Boolean,
      default: false,
    },
},
watch:{
     // 根据名称筛选部门树
     label(val) {
      this.$refs.tree.filter(val);
    },
      org: {
        handler(newVal) {
          if(newVal) {
              this.queryParams.organCode = newVal;
            this.$emit("query", this.queryParams);
          }
        }
      },
      deptOptions: {
        handler(arr) {
          this.initButtons()
        }
      },
      radioType: {
        handler(arr) {
          this.queryParams.organCode = this.org;
          this.onRefresh();
        }
      }
  },
    created() {
    },
    mounted() {
      
		if (this.org) {
      this.queryParams.organCode = this.org;
			this.onRefresh();
		}
	},
    methods: {
      handleRadioChange(value) {
        let radio =this.radioList[value]
        console.log(JSON.stringify(value))
        this.queryParams.queryType=value
        this.$emit("query", this.queryParams);
    },
    //材料第五级不能显示新增按钮。设备第三级不显示新增按钮
    isNodeAdd(node) {
      if(node.level<3 && this.radioType=='1'){
        return true;
      }else if(node.level<5 && this.radioType=='0'){
        return true;
      }
      
    },
    //判断是主库材料第三级不显示删除、编辑按钮。主库设备第二级不显示删除、编辑按钮
    isMaster(node,data){
      var isLevel=null
      if(!data.state && node.level==3 && this.radioType=='0'){
        isLevel=false 
      }else if(!data.state && node.level==2 && this.radioType=='1'){
        isLevel=false 
      }else if(data.state!='1' && data.state!='3'){
        isLevel=true
      }
      return isLevel;
    },
      isHoveredNode(node) {
        var isLevel=null
      if(this.radioType=='2' || this.radioType=='3'){
        //劳务和分包类型不能增删改
        isLevel=false
      }else{
        if(this.radioType=='1'){
          if(node.level>1){
              isLevel=true
            }else{
              isLevel=false
            }
        }else{
          if(node.level>2){
           isLevel=true
          }else{
            isLevel=false
          }
        }
        }
      return isLevel;
      },

       // 添加新增按钮
    handleAdd(data) {
      this.$emit('addItem', data)
    },

    // 点击删除按钮
    handleDelete(data, node) {
      this.$emit('deleteItem', data)
    },

    // 点击编辑按钮
    handleEdit(data, node) {
      // 获取选中的父节点
      this.selectItem = data
      data.parentLabel=this.parentNode.label
      this.$emit('editItem', JSON.parse(JSON.stringify(data)))
    },
    
    handleCheck(data){
      this.$emit('checkItem', JSON.parse(JSON.stringify(data)))
    },

    // 点击按钮新增至主库
    mainAdd(data, node) {
          this.$emit('mainAdd', data)
        },
     // 点击按钮关联主库
     mainEdit(data, node) {
      this.$emit('mainEdit', data)
    },
     // 点击按钮删除关联
     mainDelete(data, node) {
      this.$emit('mainDelete', data)
    },

    // 添加新记录，树形列表回显
    // treeAddItem(data) {
    //   this.$refs.tree.append(data, data.pid)
    // },
    treeAddItem(item,data) {
      console.log(data.children)
        // const newChild = item ;
        // const newChild = { id: item.id, label: 'testtest', children: [] };
        // if (!data.children) {
        //   this.$set(data, 'children', []);
        // }
        // data.children.push(newChild);
      },
    refreshTree(id) {
      console.log("----------------------------")
      // 通过替换treeData来刷新树
      this.$emit("query", this.queryParams);
      this.defaultExpandedKeys.push(id)
    },
  
    // 删除节点
    treeDeleteItem(val) {
      this.$refs.tree.remove(val)
    },

    // 修改记录，树形列表回显
    treeEditItem(val) {
      Object.assign(this.selectItem, val)
      this.selectItem = {}
    },
    onRefresh() {
        this.$emit("query", this.queryParams);
		},
    // 初始化按钮
    initButtons() {
      const maxLevel = this.calculateMaxLevel(this.deptOptions);
      this.buttonCount = maxLevel + 1;
    },
         // 递归计算最大层级
    calculateMaxLevel(nodes, level = 0) {
      let maxLevel = level;
      nodes.forEach((node) => {
        if (node.children && node.children.length > 0) {
          maxLevel = Math.max(
            maxLevel,
            this.calculateMaxLevel(node.children, level + 1)
          );
        }
      });
      return maxLevel;
    },
       // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
          // 节点单击事件
    handleNodeClick(data,node) {
      this.$emit('treeClick', data,node)
    },
    
    expandNodes(level) {
      this.levelExpand=level
      if (level === 1) {
          // 关闭所有默认节点
          const tree = this.$refs.tree;
          const allNodes = Object.values(tree.store.nodesMap);
          allNodes.forEach((node) => {
              // 关闭所有父节点
              if (node.level === 1) {
                tree.store.getNode(node.key).expanded = false;
              }
          });
        } else if (level === 2) {
          const tree = this.$refs.tree;
          const allNodes = Object.values(tree.store.nodesMap);
            allNodes.forEach((node) => {
              if (node.level==1) {
                  tree.store.getNode(node.key).expanded = true; // 展开父节点
                } else{
                  tree.store.getNode(node.key).expanded = false;
                }
          });
        }else if (level === 3) {
           const tree = this.$refs.tree;
          const allNodes = Object.values(tree.store.nodesMap);
            allNodes.forEach((node) => {
              if (node.level==1) {
                  tree.store.getNode(node.key).expanded = true; // 展开父节点
                } else if(node.level==2){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else{
                  tree.store.getNode(node.key).expanded = false;
                }
          });
        }else if (level === 4) {
            const tree = this.$refs.tree;
            const allNodes = Object.values(tree.store.nodesMap);
            allNodes.forEach((node) => {
              if (node.level==1) {
                  tree.store.getNode(node.key).expanded = true; // 展开父节点
                } else if(node.level==2){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else if(node.level==3){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else{
                  tree.store.getNode(node.key).expanded = false;
                }
          });
        }
        else if (level === 5) {
            const tree = this.$refs.tree;
            const allNodes = Object.values(tree.store.nodesMap);
            allNodes.forEach((node) => {
              if (node.level==1) {
                  tree.store.getNode(node.key).expanded = true; // 展开父节点
                } else if(node.level==2){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else if(node.level==3){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else if(node.level==4){
                  tree.store.getNode(node.key).expanded = true; // 展开二级节点
                }else{
                  tree.store.getNode(node.key).expanded = false;
                }
          });
        }
    },
    }
  };
  </script>
  <style lang="scss" scoped>
    .left {
      /* width: 260px; */
      margin:0px 10px 0 10px;
      background-color: #ffffff;
    }
   
    .tooltip {
      width: 100%;
    margin-right: 5px;
    font-size: 13px;
    border-radius: 4px;
    box-sizing: border-box;
    white-space: nowrap;
    padding: 4px;
  }
  .custom-tree-node:hover .add-f-s-14 {
    display: inline-block; /* 或者 block，根据需要 */
    max-width: 140px; /* 确保不超过父容器的宽度 */
    white-space: nowrap; /* 防止文本换行 */
    overflow: hidden; /* 隐藏溢出的内容 */
    text-overflow: ellipsis; /* 显示省略号来表示溢出内容 */
    }
  .custom-tree-node:hover .add-f-s {
  display: inline-block; /* 或者 block，根据需要 */
  max-width: 80px; /* 确保不超过父容器的宽度 */
  white-space: nowrap; /* 防止文本换行 */
  overflow: hidden; /* 隐藏溢出的内容 */
  text-overflow: ellipsis; /* 显示省略号来表示溢出内容 */
  }
 .custom-tree-node {
  display: flex;
  justify-content: space-between;
 }
::v-deep .vue-treeselect {
  .vue-treeselect__control {
    height: 32px;
  }
}
.address-tree {
  margin: 0;
  height: calc(100vh - 215px);
  overflow-y: scroll;

  &::-webkit-scrollbar {
    display: none;
  }

  ::v-deep .icon-shouyetianchong:before {
    content: "\E692";
    color: #004ea2;
  }

  ::v-deep .icon-24gf-folderOpen:before {
    content: "\eac5";
    color: #004ea2;
  }

  ::v-deep .el-tree-node {
    .el-tree-node__content {
      height: auto;
      padding: 2px 0;
      margin: 2px 0;

      font-size: 13px;
      color: #606266;
      &:hover .hover-operations { 
      display: inline-block;
    }

      .el-tree-node__label {
        white-space: pre-wrap;
        line-height: 20px;
      }
    }
  }
}

  .operation-view, .hover-operations { 
    position: absolute;
    right: 8px;
    display: none;
    padding: 5px 0px;
    margin-left: 50px;
    /* margin-left: 50px; */
    color: #777777;
  }
  .small-operation-btn {
    margin: 0px 3px;
    color: #3D98F9;
  }


::v-deep  .el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content  {
  color: #2b4acb !important;
  
  .add-f-s-14 {
    display: inline-block; /* 或者 block，根据需要 */
    max-width: 140px; /* 确保不超过父容器的宽度 */
    white-space: nowrap; /* 防止文本换行 */
    overflow: hidden; /* 隐藏溢出的内容 */
    text-overflow: ellipsis; /* 显示省略号来表示溢出内容 */
    }
  .hover-operations { 
      display: inline-block;
    }
}
.circle-btn {
  background-color: #e8e8ef;
  color: #999;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  text-align: center;
  line-height: 20px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
  margin: 5px;
}
.circle-btn-selected {
  background-color: #2b4acb;
  color: #fff;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  text-align: center;
  line-height: 20px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
  margin: 5px;
}
/* 鼠标移上去时的效果 */
.circle-btn:hover {
  background-color: #2b4acb;
  color: #ffffff;
}
::v-deep .left_box{
    background: #ffffff;

}
  </style>
  