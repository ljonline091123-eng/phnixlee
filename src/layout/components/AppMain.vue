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
  /* 50= navbar  50  */ 
  // min-height: calc(100vh - 50px);
  height: 100%;
  width: 100%;
  position: relative;
  overflow: scroll;
  background-color: #F2F2F8;
}

.fixed-header + .app-main {
  padding-top: 50px;
}

.hasTagsView {
  .app-main {
    /* 84 = navbar + tags-view = 50 + 34 */
    min-height: calc(100vh - 84px);
  }

  .fixed-header + .app-main {
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
