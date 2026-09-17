<template>
  <section class="app-main">
      <keep-alive :include="cachedViews">
        <router-view v-if="!$route.meta.link" :key="key" />
      </keep-alive>
    <iframe-toggle />
  </section>
</template>

<script>
import iframeToggle from "./IframeToggle/index"

export default {
  name: 'AppMain',
  components: { iframeToggle },
  computed: {
    cachedViews() {
      // return this.$store.state.tagsView.cachedViews
      return ["add-plan","add-scheme","add-contract","edit-contract","add-rating","edit-rating"]
    },
    key() {
      return this.$route.path
    }
  }
}
</script>

<style lang="scss" scoped>
.el-popper {position:absolute !important;}
.app-main {
  /* main-container 是纵向 flex：页头占多少，剩余空间就全给内容区，
     不再按 50/84px 手工计算高度，页头实际高度怎么变都能正好占满一屏 */
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  position: relative;
  /* auto：内容不超出时不出滚动条；长页面在内容区内部滚动 */
  overflow: auto;
  background-color: #F2F2F8;
}

.fixed-header + .app-main {
  /* 页头悬浮（fixedHeader 开启）时 app-main 从页面顶部开始，用 padding 让出页头位置 */
  padding-top: 50px;
}

.hasTagsView {
  .fixed-header + .app-main {
    /* 84 = navbar 50 + tags-view 34 */
    padding-top: 84px;
  }
}
</style>

<style lang="scss">
// fix css style bug in open el-dialog
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 6px;
  }
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background-color: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background-color: #c0c0c0;
  border-radius: 3px;
}
</style>
