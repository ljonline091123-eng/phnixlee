const org = {
  state: {
    org:""
  },

  mutations: {
    SET_ORG: (state, org) => {
      state.org = org
    }
  },

  actions: {
    setOrg({commit}) {
      commit('SET_ORG')
    }
  }
}

export default org