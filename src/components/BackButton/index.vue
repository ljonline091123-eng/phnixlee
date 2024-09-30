<template>
  <div class="back-button">
      <div>
        <span class="back" @click="back"><i class="el-icon-back"></i> 返回</span>
        {{ title }}
      </div>
     <slot></slot>
  </div>
</template>
<script>
export default {
  name: 'BackButton',
  props:{
    path:{
      type:String,
      default:''
    },
    title:{
      type:String,
      default:''
    }
  },
  methods:{
    back(){
      this.$router.push(this.path)
    }
  }
}
</script>
<style lang="scss" scoped>
.back-button{
    position: relative;
    height: 50px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 15px;
    font-weight: 700;
    padding: 0 15px;
    border-bottom: 1px solid #eee;
    color: #606266;
    .back{
      border-right: 1px solid #999;
      padding-right: 15px;
      margin-right: 15px;
      font-size: 14px;
      cursor: pointer;
    }
}
</style>