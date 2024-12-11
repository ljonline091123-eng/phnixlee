const project = {
  state: {
    project: {}
  },

  mutations: {
    SET_PROJECT: (state, project) => {
      state.project = project; // 更新 project
    }
  },

  actions: {
    setProject({ commit }, data) {
      console.log('%c👽👽 data ', `font-size: 20px;background-color: #f00;`, data);
      commit('SET_PROJECT', data); // 传递 data 给 mutation
    }
  }
}

export default project;
