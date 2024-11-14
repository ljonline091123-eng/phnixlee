<template>
  <div id="app">
    <router-view />
    <theme-picker />
  </div>
</template>

<script>
import ThemePicker from "@/components/ThemePicker";
import { removeToken, removeControlToken } from "@/utils/auth";
export default {
  name: "App",
  components: { ThemePicker },
  metaInfo() {
    return {
      title:
        this.$store.state.settings.dynamicTitle &&
        this.$store.state.settings.title,
      titleTemplate: (title) => {
        return title
          ? `${title} - ${process.env.VUE_APP_TITLE}`
          : process.env.VUE_APP_TITLE;
      },
    };
  },
  mounted() {
    let project = null;
    let currentOrg = "";
    let scopeType = "";
    console.log(' %c 🚀 ~ file:App --method:mounted --line:30 --variable:window.parent.scopeType===>', 'font-size:16px;background-color: #42b983;color:#fff;', window.parent.scopeType)
    if (
      process.env.NODE_ENV === "staging" ||
      process.env.NODE_ENV === "production"
    ) {
      project = window.$wujie?.props?.prj || {};
      currentOrg = window.localStorage.getItem("currentOrgValue") || "";
      scopeType = window.parent.scopeType || "1";
    } else {
      // project = window.$wujie?.props?.prj || {}
      project = window.$wujie?.props.prj || {
        code: "SG20012024000026-1",
        id: "1834769165014523904",
        name: "金甲小学项目",
      };
      currentOrg =
        window.localStorage.getItem("currentOrgValue") || "2013000000";
      scopeType = window.parent.scopeType || "1";
    }
    if (scopeType) {
      this.$store.dispatch('app/changeScopeType', scopeType)
    }
    console.log("this.$store.state.app.scopeType",this.$store.state.app.scopeType)
    if (project.id) {
      this.$store.commit("SET_PROJECT", project);
      console.log(project, "通过window.$wujie?.props.prj获取的项目");
    }
    if (currentOrg) {
      this.$store.commit("SET_ORG", currentOrg);
    }
    //监听路由变化
    window.$wujie?.bus.$on("wj_routeChange", (data) => {
      console.log(data, "路由~~");
      this.$router.push({
        path: data.path,
      });
    });
    //获取主控项目
    window.$wujie?.bus.$on("currentPrjValueChange", (data) => {
      console.log(data, "通过currentPrjValueChange获取的项目");
      this.$store.commit("SET_PROJECT", data);
    });
    //获取主控组织
    window.$wujie?.bus.$on("currentOrgValueChange", (data) => {
      console.log(data, "通过currentOrgValueChange获取的组织");
      this.$store.commit("SET_ORG", data.id);
    });
    //监控主控退出
    window.$wujie?.bus.$on("LogOut", () => {
      this.$store.commit("SET_TOKEN", "");
      this.$store.commit("SET_ROLES", []);
      this.$store.commit("SET_PERMISSIONS", []);
      removeToken();
      removeControlToken();
    });
  },
};
</script>
<style scoped>
#app .theme-picker {
  display: none;
}
</style>
