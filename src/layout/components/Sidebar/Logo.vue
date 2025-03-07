<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}" :style="{ backgroundColor: sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground }">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logoOff" class="sidebar-logo-off" />
        <!-- <h1 v-else class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' ? variables.logoTitleColor : variables.logoLightTitleColor }">{{ title }} </h1> -->
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
        <!-- <h1 class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' ? variables.logoTitleColor : variables.logoLightTitleColor }">{{ title }} </h1> -->
      </router-link>
    </transition>

  </div>
</template>

<script>
import logoImg from '@/assets/logo/logo_title.png'
import logoOffImg from '@/assets/logo/logo_title_off.png'

// import logoImg from '@/assets/logo/logo.png'

import variables from '@/assets/styles/variables.scss'

export default {
  name: 'SidebarLogo',
  props: {
    collapse: {
      type: Boolean,
      required: true
    }
  },
  computed: {
    variables() {
      return variables;
    },
    sideTheme() {
      return this.$store.state.settings.sideTheme
    }
  },
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      logo: logoImg,
      logoOff:logoOffImg,
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 70px;
  line-height: 70px;
  // background: #0644ef;
  text-align: center;
  overflow: hidden;
  border-bottom: 1px solid #4B65D3;
  & .sidebar-logo-link {
    height: 100%;
    width: 100%;

    & .sidebar-logo {
      width: 180px;
      height: 28px;
      vertical-align: middle;
      margin-right: 8px;
      margin-top: 6px;
    }
 & .sidebar-logo-off {
      width: 28px;
      height: 28px;
      vertical-align: middle;
      // margin-right: 20px;
      margin-top: 12px;
    }
    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: #fff;
      font-weight: 600;
      line-height: 50px;
      font-size: 14px;
      font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
      vertical-align: middle;
    }
  }

  &.collapse {
    .sidebar-logo {
      margin-right: 0px;
    }
  }
}
</style>
