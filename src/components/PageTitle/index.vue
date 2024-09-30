<template>
  <div class="page-title" :style="{marginBottom,marginTop}">
    <div style="display: flex;align-items: center">
      <span style="font-size: 13px;font-weight: bold;color: #121735">{{ title }}</span>
      <slot name="leftSuffix"></slot>
    </div>
    <slot></slot>
  </div>
</template>
<script>
export default {
  props: {
    title: {
      type: String,
      default: ''
    },
    marginBottom: {
      type: String,
      default: '0px'
    },
    marginTop: {
      type: String,
      default: '0px'
    }
  }
}
</script>
<style lang="scss" scoped>
.page-title {
  width: 100%;
  border-bottom: solid 1px #eee;
  height: 45px;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-left: 10px;

  &::before {
    content: "";
    height: 15px;
    width: 3px;
    background-color: rgba(41, 65, 137, 1);
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
  }
}
</style>
