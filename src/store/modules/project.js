const project = {
  state: {
    project:{}
  },

  mutations: {
    SET_PROJECT: (state, project) => {
      state.project = project
    }
  },

  actions: {
    setProject({commit}) {
      commit('SET_PROJECT')
    }
  }
}

export default project