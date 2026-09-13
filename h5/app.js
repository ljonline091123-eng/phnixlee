(() => {
  const SIZE = 9;
  const CELL_COUNT = SIZE * SIZE;
  const LINE_DIRECTIONS = [[0, 1], [1, 0], [1, 1], [1, -1]];
  const ORDINARY_COLORS = ["red", "gold", "green", "blue", "violet", "cyan", "pink", "lime", "black"];
  const COLORS = {
    red: ["#ea403a", "#8d0d12"],
    gold: ["#f4c430", "#9b6500"],
    green: ["#36a852", "#0e5426"],
    blue: ["#2264dc", "#0c2f8a"],
    violet: ["#7e45d9", "#3e1f84"],
    cyan: ["#12a7b3", "#07515d"],
    pink: ["#f05a9d", "#9e1652"],
    lime: ["#96c93d", "#476d0d"],
    black: ["#3c4446", "#050708"],
    white: ["#ffffff", "#d9dedb"],
    bomb: ["#6d7478", "#111619"]
  };
  const SPEEDS = {
    slow: { label: "慢", ms: 210 },
    normal: { label: "正常", ms: 125 },
    fast: { label: "快", ms: 70 },
    lightning: { label: "光速", ms: 28 }
  };
  const STORAGE = {
    state: "five-lines-h5-state",
    scores: "five-lines-h5-scores",
    settings: "five-lines-h5-settings"
  };

  const dom = {
    board: document.getElementById("board"),
    score: document.getElementById("score"),
    preview: document.getElementById("preview"),
    statusText: document.getElementById("statusText"),
    toast: document.getElementById("toast"),
    menuButton: document.getElementById("menuButton"),
    menuModal: document.getElementById("menuModal"),
    settingsModal: document.getElementById("settingsModal"),
    scoresModal: document.getElementById("scoresModal"),
    gameOverModal: document.getElementById("gameOverModal"),
    newGameButton: document.getElementById("newGameButton"),
    highScoreButton: document.getElementById("highScoreButton"),
    settingsButton: document.getElementById("settingsButton"),
    scoresList: document.getElementById("scoresList"),
    finalScore: document.getElementById("finalScore"),
    nickname: document.getElementById("nickname"),
    saveScoreButton: document.getElementById("saveScoreButton"),
    soundButton: document.getElementById("soundButton"),
    soundLabel: document.getElementById("soundLabel"),
    difficulty: document.getElementById("difficulty"),
    difficultyValue: document.getElementById("difficultyValue"),
    difficultyHint: document.getElementById("difficultyHint"),
    whiteProbability: document.getElementById("whiteProbability"),
    whiteProbabilityValue: document.getElementById("whiteProbabilityValue"),
    bombProbability: document.getElementById("bombProbability"),
    bombProbabilityValue: document.getElementById("bombProbabilityValue"),
    musicToggle: document.getElementById("musicToggle"),
    effectsToggle: document.getElementById("effectsToggle"),
    musicVolume: document.getElementById("musicVolume"),
    effectsVolume: document.getElementById("effectsVolume"),
    speedPicker: document.getElementById("speedPicker")
  };

  const defaultSettings = {
    difficulty: 1,
    whiteProbability: 1,
    bombProbability: 1,
    music: true,
    effects: true,
    musicVolume: 0.65,
    effectsVolume: 0.8,
    speed: "slow"
  };

  let settings = { ...defaultSettings, ...readJson(STORAGE.settings, {}) };
  let state = {
    board: Array(CELL_COUNT).fill(null),
    next: [],
    selected: null,
    score: 0,
    busy: false,
    gameOver: false,
    clearedThisTurn: false,
    lastDifficultyMilestone: 0
  };
  let audioContext = null;
  let musicTimer = null;

  function init() {
    buildBoard();
    buildSpeedPicker();
    bindEvents();
    applySettingsToControls();

    const saved = readJson(STORAGE.state, null);
    if (saved && Array.isArray(saved.board) && saved.board.length === CELL_COUNT) {
      state = {
        ...state,
        ...saved,
        busy: false,
        selected: null,
        clearedThisTurn: false
      };
      if (!Array.isArray(state.next) || state.next.length !== 3) state.next = makePreview();
    } else {
      newGame();
      return;
    }

    render();
    startMusic();
    if (state.gameOver) showGameOver();
  }

  function buildBoard() {
    dom.board.innerHTML = "";
    for (let i = 0; i < CELL_COUNT; i += 1) {
      const cell = document.createElement("button");
      cell.className = "cell";
      cell.type = "button";
      cell.dataset.index = String(i);
      cell.setAttribute("role", "gridcell");
      cell.setAttribute("aria-label", `第${Math.floor(i / SIZE) + 1}行第${i % SIZE + 1}列`);
      dom.board.appendChild(cell);
    }
  }

  function buildSpeedPicker() {
    dom.speedPicker.innerHTML = "";
    Object.entries(SPEEDS).forEach(([key, speed]) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "segment";
      button.dataset.speed = key;
      button.textContent = speed.label;
      button.addEventListener("click", () => {
        settings.speed = key;
        saveSettings();
        renderSpeedPicker();
      });
      dom.speedPicker.appendChild(button);
    });
  }

  function bindEvents() {
    dom.board.addEventListener("click", event => {
      const cell = event.target.closest(".cell");
      if (!cell) return;
      handleCellTap(Number(cell.dataset.index));
    });

    dom.menuButton.addEventListener("click", () => openModal(dom.menuModal));
    dom.newGameButton.addEventListener("click", () => {
      closeModal(dom.menuModal);
      newGame();
    });
    dom.highScoreButton.addEventListener("click", () => {
      renderScores();
      closeModal(dom.menuModal);
      openModal(dom.scoresModal);
    });
    dom.settingsButton.addEventListener("click", () => {
      closeModal(dom.menuModal);
      openModal(dom.settingsModal);
    });
    dom.saveScoreButton.addEventListener("click", saveScoreAndRestart);
    dom.soundButton.addEventListener("click", () => {
      settings.effects = !settings.effects;
      settings.music = settings.effects;
      saveSettings();
      applySettingsToControls();
      render();
      if (settings.music) startMusic(); else stopMusic();
    });

    document.querySelectorAll("[data-close]").forEach(button => {
      button.addEventListener("click", () => closeModal(document.getElementById(button.dataset.close)));
    });
    document.querySelectorAll(".modal-backdrop").forEach(backdrop => {
      backdrop.addEventListener("click", event => {
        if (event.target === backdrop && backdrop !== dom.gameOverModal) closeModal(backdrop);
      });
    });

    bindRange(dom.difficulty, dom.difficultyValue, "difficulty");
    bindRange(dom.whiteProbability, dom.whiteProbabilityValue, "whiteProbability");
    bindRange(dom.bombProbability, dom.bombProbabilityValue, "bombProbability");
    bindRange(dom.musicVolume, null, "musicVolume");
    bindRange(dom.effectsVolume, null, "effectsVolume");
    dom.musicToggle.addEventListener("change", () => {
      settings.music = dom.musicToggle.checked;
      saveSettings();
      if (settings.music) startMusic(); else stopMusic();
    });
    dom.effectsToggle.addEventListener("change", () => {
      settings.effects = dom.effectsToggle.checked;
      saveSettings();
      render();
    });

    window.addEventListener("beforeunload", saveState);
    document.addEventListener("visibilitychange", () => {
      saveState();
      if (document.hidden) stopMusic();
      else if (settings.music) startMusic();
    });
    document.addEventListener("pointerdown", unlockAudio, { once: true });
  }

  function bindRange(input, output, key) {
    input.addEventListener("input", () => {
      settings[key] = Number(input.value);
      if (output) output.value = Number(input.value).toFixed(1);
      // Changing difficulty should make the current score threshold testable
      // on the next ordinary (non-clearing) turn instead of silently keeping a
      // threshold consumed while the game was still at the old coefficient.
      if (key === "difficulty") {
      const reachedMilestone = Math.floor(state.score / 50);
      state.lastDifficultyMilestone = Math.max(0, reachedMilestone - 1);
      saveState();
      updateDifficultyHint();
      }
      saveSettings();
    });
  }

  function applySettingsToControls() {
    dom.difficulty.value = settings.difficulty;
    dom.difficultyValue.value = Number(settings.difficulty).toFixed(1);
    updateDifficultyHint();
    dom.whiteProbability.value = settings.whiteProbability;
    dom.whiteProbabilityValue.value = Number(settings.whiteProbability).toFixed(1);
    dom.bombProbability.value = settings.bombProbability;
    dom.bombProbabilityValue.value = Number(settings.bombProbability).toFixed(1);
    dom.musicToggle.checked = settings.music;
    dom.effectsToggle.checked = settings.effects;
    dom.musicVolume.value = settings.musicVolume;
    dom.effectsVolume.value = settings.effectsVolume;
    renderSpeedPicker();
  }

  function renderSpeedPicker() {
    dom.speedPicker.querySelectorAll(".segment").forEach(button => {
      button.classList.toggle("active", button.dataset.speed === settings.speed);
    });
  }

  function newGame() {
    state = {
      board: Array(CELL_COUNT).fill(null),
      next: makePreview(),
      selected: null,
      score: 0,
      busy: false,
      gameOver: false,
      clearedThisTurn: false,
      lastDifficultyMilestone: 0
    };
    spawnPieces(makeRandomTiles(3), false);
    render();
    saveState();
    startMusic();
  }

  async function handleCellTap(index) {
    if (state.busy || state.gameOver) return;
    const tile = state.board[index];

    if (state.selected === null) {
      if (tile) {
        state.selected = index;
        playEffect("select");
        updateStatus("选择目标空位");
        render();
      }
      return;
    }

    if (tile) {
      state.selected = index;
      playEffect("select");
      render();
      return;
    }

    const route = shortestPath(state.selected, index);
    if (!route) {
      state.selected = null;
      updateStatus("没有可通行路径");
      showToast("没有可通行路径");
      render();
      return;
    }

    const from = state.selected;
    state.selected = null;
    state.clearedThisTurn = false;
    await movePiece(from, route);
    await resolveTurn();
  }

  async function movePiece(from, route) {
    state.busy = true;
    let current = from;
    let tile = state.board[current];
    for (const next of route) {
      state.board[current] = null;
      state.board[next] = tile;
      render(next);
      playEffect("move");
      await wait(SPEEDS[settings.speed]?.ms ?? SPEEDS.normal.ms);
      current = next;
    }
  }

  async function resolveTurn() {
    let anyCleared = false;
    while (true) {
      const match = findMatches();
      if (match.lineCells.size === 0) break;
      anyCleared = true;
      state.clearedThisTurn = true;
      await clearMatch(match);
    }

    if (!anyCleared) {
      await spawnForTurn();
      await resolveGeneratedMatches();
    } else {
      state.busy = false;
      updateStatus("消除成功，本轮不新增棋子");
      checkGameOver();
    }
    saveState();
    render();
  }

  async function resolveGeneratedMatches() {
    while (true) {
      const match = findMatches();
      if (match.lineCells.size === 0) break;
      await clearMatch(match);
    }
    state.busy = false;
    checkGameOver();
  }

  async function clearMatch(match) {
    const blastCells = bombBlastCells(match);
    const all = union(match.lineCells, blastCells);
    const lineCount = match.lineCells.size;
    const blastOnlyCount = difference(blastCells, match.lineCells).size;

    state.score += scoreForLine(lineCount) + blastOnlyCount;
    render();
    all.forEach(index => {
      const piece = dom.board.children[index]?.querySelector(".piece");
      if (piece) piece.classList.add("removing");
    });
    playEffect(match.bombColors.size > 0 ? "bomb" : "clear");
    await wait(510);
    all.forEach(index => { state.board[index] = null; });
    render();
    await wait(80);
  }

  async function spawnForTurn() {
    const count = spawnCountForScore();
    let tiles = state.next.slice(0, Math.min(3, count));
    while (tiles.length < count) tiles.push(randomTile());
    spawnPieces(tiles, true);
    state.next = makePreview();
    render();
    playEffect("spawn");
    if (count > 3) {
      showToast(`难度触发：本轮生成 ${count} 颗棋子`);
    }
    await wait(330);
  }

  function spawnPieces(tiles, animated) {
    const empty = state.board.map((value, index) => value ? null : index).filter(value => value !== null);
    shuffle(empty);
    empty.slice(0, tiles.length).forEach((index, tileIndex) => {
      state.board[index] = tiles[tileIndex];
    });
    if (!animated) render();
  }

  function spawnCountForScore() {
    const base = 3;
    const coefficient = Number(settings.difficulty);
    const milestone = Math.floor(state.score / 50);
    if (milestone < 1 || coefficient <= 1) return base;
    if (milestone <= state.lastDifficultyMilestone) return base;

    // One difficulty burst is consumed per 50-point threshold. The score at
    // the moment of the burst is used exactly as specified by the game rule.
    state.lastDifficultyMilestone = milestone;
    const extra = difficultyExtraPieces(state.score, coefficient, base);
    return base + Math.max(0, extra);
  }

  function difficultyExtraPieces(score, coefficient, basePieces = 3) {
    if (score < 50 || coefficient <= 1) return 0;
    return Math.max(0, Math.floor(basePieces * ((score / 100) * (coefficient - 1))));
  }

  function difficultySpawnCount(score, coefficient) {
    return 3 + difficultyExtraPieces(score, coefficient, 3);
  }

  function updateDifficultyHint() {
    if (!dom.difficultyHint) return;
    const previewCount = difficultySpawnCount(state.score, Number(settings.difficulty));
    if (state.score < 50 || Number(settings.difficulty) <= 1) {
      dom.difficultyHint.textContent = "当前分数下每轮生成 3 颗";
      return;
    }
    dom.difficultyHint.textContent = `当前分数下触发时生成 ${previewCount} 颗（含基础 3 颗）`;
  }

  function findMatches() {
    const lineCells = new Set();
    const bombColors = new Set();

    for (let start = 0; start < CELL_COUNT; start += 1) {
      const startRow = Math.floor(start / SIZE);
      const startCol = start % SIZE;
      for (const [dr, dc] of LINE_DIRECTIONS) {
        const beforeRow = startRow - dr;
        const beforeCol = startCol - dc;
        if (inside(beforeRow, beforeCol) && state.board[indexOf(beforeRow, beforeCol)]) continue;

        const run = [];
        for (let step = 0; step < SIZE; step += 1) {
          const row = startRow + dr * step;
          const col = startCol + dc * step;
          if (!inside(row, col)) break;
          const index = indexOf(row, col);
          if (!state.board[index]) break;
          run.push(index);
        }
        if (run.length < 5) continue;
        evaluateRun(run, lineCells, bombColors);
      }
    }

    return { lineCells, bombColors };
  }

  function evaluateRun(run, lineCells, bombColors) {
    for (let start = 0; start <= run.length - 5; start += 1) {
      for (let end = start + 5; end <= run.length; end += 1) {
        const segment = run.slice(start, end);
        const tiles = segment.map(index => state.board[index]);
        const normals = tiles.filter(tile => tile && !isWildcard(tile));
        if (normals.length === 0) continue;
        const color = normals[0];
        if (!normals.every(tile => tile === color)) continue;
        segment.forEach(index => lineCells.add(index));
        if (tiles.includes("bomb")) bombColors.add(color);
      }
    }
  }

  function bombBlastCells(match) {
    const cells = new Set();
    match.bombColors.forEach(color => {
      state.board.forEach((tile, index) => {
        if (tile === color) cells.add(index);
      });
    });
    return cells;
  }

  function shortestPath(start, goal) {
    const queue = [start];
    const visited = new Set([start]);
    const previous = new Map();
    while (queue.length) {
      const current = queue.shift();
      if (current === goal) {
        const route = [];
        let cursor = goal;
        while (cursor !== start) {
          route.push(cursor);
          cursor = previous.get(cursor);
        }
        return route.reverse();
      }
      for (const next of neighbors(current)) {
        if (visited.has(next)) continue;
        if (next !== goal && state.board[next]) continue;
        visited.add(next);
        previous.set(next, current);
        queue.push(next);
      }
    }
    return null;
  }

  function neighbors(index) {
    const row = Math.floor(index / SIZE);
    const col = index % SIZE;
    const result = [];
    [[row - 1, col], [row + 1, col], [row, col - 1], [row, col + 1]].forEach(([r, c]) => {
      if (inside(r, c)) result.push(indexOf(r, c));
    });
    return result;
  }

  function render(movingIndex = null) {
    dom.score.textContent = String(state.score).padStart(5, "0").slice(-5);
    updateDifficultyHint();
    dom.preview.innerHTML = state.next.map(tile => `<span class="preview-dot ${tile}" style="${pieceStyle(tile)}"></span>`).join("");
    dom.soundLabel.textContent = settings.effects ? "音效" : "静音";
    Array.from(dom.board.children).forEach((cell, index) => {
      const tile = state.board[index];
      cell.classList.toggle("selected", state.selected === index);
      cell.innerHTML = tile ? `<span class="piece ${tile}${movingIndex === index ? " moving" : ""}" style="${pieceStyle(tile)}">${tile === "bomb" ? "✹" : ""}</span>` : "";
      cell.disabled = state.busy;
    });
  }

  function pieceStyle(tile) {
    const colors = COLORS[tile] || COLORS.red;
    if (tile === "bomb") return "";
    return `background:radial-gradient(circle at 32% 27%, ${colors[0]}, ${colors[0]} 34%, ${colors[1]} 100%)`;
  }

  function updateStatus(text) {
    dom.statusText.textContent = text;
  }

  function showToast(text) {
    dom.toast.textContent = text;
    dom.toast.classList.add("show");
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => dom.toast.classList.remove("show"), 1400);
  }

  function checkGameOver() {
    if (state.board.some(tile => !tile)) return false;
    state.gameOver = true;
    showGameOver();
    return true;
  }

  function showGameOver() {
    dom.finalScore.textContent = String(state.score);
    dom.nickname.value = "";
    openModal(dom.gameOverModal);
  }

  function saveScoreAndRestart() {
    const scores = readJson(STORAGE.scores, []);
    const name = (dom.nickname.value || "玩家").trim().slice(0, 12) || "玩家";
    scores.push({ name, score: state.score, time: Date.now() });
    scores.sort((a, b) => b.score - a.score);
    localStorage.setItem(STORAGE.scores, JSON.stringify(scores.slice(0, 10)));
    closeModal(dom.gameOverModal);
    newGame();
  }

  function renderScores() {
    const scores = readJson(STORAGE.scores, []);
    if (!scores.length) {
      dom.scoresList.innerHTML = `<div class="empty-scores">暂无记录</div>`;
      return;
    }
    dom.scoresList.innerHTML = scores.slice(0, 10).map((entry, index) => `
      <div class="score-entry"><span class="rank">${index + 1}</span><span>${escapeHtml(entry.name)}</span><span class="entry-score">${entry.score}</span></div>
    `).join("");
  }

  function openModal(modal) {
    modal.hidden = false;
  }

  function closeModal(modal) {
    modal.hidden = true;
  }

  function makePreview() {
    return makeRandomTiles(3);
  }

  function makeRandomTiles(count) {
    return Array.from({ length: count }, randomTile);
  }

  function randomTile() {
    const base = 1 / 11;
    const whiteChance = Math.min(1, Math.max(0, base * settings.whiteProbability));
    const bombChance = Math.min(1, Math.max(0, base * settings.bombProbability));
    const ordinaryTotal = Math.max(0, 1 - whiteChance - bombChance);
    const roll = Math.random();
    if (roll < whiteChance) return "white";
    if (roll < whiteChance + bombChance) return "bomb";
    const normalized = (roll - whiteChance - bombChance) / Math.max(ordinaryTotal, Number.EPSILON);
    const index = Math.min(ORDINARY_COLORS.length - 1, Math.floor(normalized * ORDINARY_COLORS.length));
    return ORDINARY_COLORS[index];
  }

  function scoreForLine(count) {
    if (count < 5) return 0;
    return 5 + (count - 5) * 2;
  }

  function isWildcard(tile) {
    return tile === "white" || tile === "bomb";
  }

  function inside(row, col) {
    return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
  }

  function indexOf(row, col) {
    return row * SIZE + col;
  }

  function union(a, b) {
    const result = new Set(a);
    b.forEach(value => result.add(value));
    return result;
  }

  function difference(a, b) {
    const result = new Set();
    a.forEach(value => {
      if (!b.has(value)) result.add(value);
    });
    return result;
  }

  function shuffle(list) {
    for (let i = list.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1));
      [list[i], list[j]] = [list[j], list[i]];
    }
  }

  function wait(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  function saveSettings() {
    localStorage.setItem(STORAGE.settings, JSON.stringify(settings));
  }

  function saveState() {
    localStorage.setItem(STORAGE.state, JSON.stringify({
      board: state.board,
      next: state.next,
      score: state.score,
      gameOver: state.gameOver,
      lastDifficultyMilestone: state.lastDifficultyMilestone
    }));
  }

  function readJson(key, fallback) {
    try {
      const value = localStorage.getItem(key);
      return value ? JSON.parse(value) : fallback;
    } catch {
      return fallback;
    }
  }

  function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, char => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      "\"": "&quot;",
      "'": "&#039;"
    }[char]));
  }

  function unlockAudio() {
    getAudioContext();
    startMusic();
  }

  function getAudioContext() {
    if (!audioContext) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) audioContext = new AudioCtx();
    }
    if (audioContext?.state === "suspended") audioContext.resume();
    return audioContext;
  }

  function playEffect(kind) {
    if (!settings.effects) return;
    const ctx = getAudioContext();
    if (!ctx) return;
    const volume = Number(settings.effectsVolume) || 0;
    if (kind === "bomb") {
      explosionSound(ctx, volume);
      return;
    }
    const map = {
      select: [620, 0.035, "sine", 0.08],
      move: [390, 0.045, "triangle", 0.055],
      spawn: [520, 0.07, "sine", 0.075],
      clear: [740, 0.12, "triangle", 0.09]
    };
    const spec = map[kind] || map.select;
    tone(ctx, spec[0], spec[1], spec[2], volume * spec[3]);
  }

  function tone(ctx, frequency, duration, type, gainValue) {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.value = frequency;
    gain.gain.setValueAtTime(gainValue, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + duration);
    osc.connect(gain).connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + duration);
  }

  function noiseBurst(ctx, gainValue) {
    const buffer = ctx.createBuffer(1, ctx.sampleRate * 0.3, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < data.length; i += 1) {
      const decay = Math.pow(1 - i / data.length, 1.8);
      data[i] = (Math.random() * 2 - 1) * decay;
    }
    const noise = ctx.createBufferSource();
    const gain = ctx.createGain();
    noise.buffer = buffer;
    gain.gain.setValueAtTime(gainValue, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.3);
    noise.connect(gain).connect(ctx.destination);
    noise.start();
  }

  function explosionSound(ctx, volume) {
    const now = ctx.currentTime;
    const loudness = Math.max(0, Math.min(1, volume));

    // A short fuse crackle gives the blast a clear beginning.
    const fuseBuffer = ctx.createBuffer(1, Math.floor(ctx.sampleRate * 0.16), ctx.sampleRate);
    const fuseData = fuseBuffer.getChannelData(0);
    for (let i = 0; i < fuseData.length; i += 1) {
      const envelope = Math.pow(1 - i / fuseData.length, 0.7);
      fuseData[i] = (Math.random() * 2 - 1) * envelope;
    }
    const fuse = ctx.createBufferSource();
    const fuseFilter = ctx.createBiquadFilter();
    const fuseGain = ctx.createGain();
    fuse.buffer = fuseBuffer;
    fuseFilter.type = "highpass";
    fuseFilter.frequency.setValueAtTime(1800, now);
    fuseGain.gain.setValueAtTime(loudness * 0.12, now);
    fuseGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.16);
    fuse.connect(fuseFilter).connect(fuseGain).connect(ctx.destination);
    fuse.start(now);

    // Layered low boom with a quick downward pitch sweep.
    const boom = ctx.createOscillator();
    const boomGain = ctx.createGain();
    boom.type = "sine";
    boom.frequency.setValueAtTime(145, now + 0.08);
    boom.frequency.exponentialRampToValueAtTime(42, now + 0.5);
    boomGain.gain.setValueAtTime(loudness * 0.32, now + 0.08);
    boomGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.5);
    boom.connect(boomGain).connect(ctx.destination);
    boom.start(now + 0.08);
    boom.stop(now + 0.52);

    const crack = ctx.createBufferSource();
    const crackFilter = ctx.createBiquadFilter();
    const crackGain = ctx.createGain();
    const crackBuffer = ctx.createBuffer(1, Math.floor(ctx.sampleRate * 0.38), ctx.sampleRate);
    const crackData = crackBuffer.getChannelData(0);
    for (let i = 0; i < crackData.length; i += 1) {
      const envelope = Math.pow(1 - i / crackData.length, 2.2);
      crackData[i] = (Math.random() * 2 - 1) * envelope;
    }
    crack.buffer = crackBuffer;
    crackFilter.type = "bandpass";
    crackFilter.frequency.setValueAtTime(1150, now + 0.08);
    crackFilter.Q.value = 0.65;
    crackGain.gain.setValueAtTime(loudness * 0.22, now + 0.08);
    crackGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.46);
    crack.connect(crackFilter).connect(crackGain).connect(ctx.destination);
    crack.start(now + 0.08);

    noiseBurst(ctx, loudness * 0.16);
  }

  function startMusic() {
    if (!settings.music || musicTimer) return;
    const ctx = getAudioContext();
    if (!ctx) return;
    const melody = [392, 440, 523, 587, 659, 587, 523, 440, 392, 330, 392, 440];
    const bass = [196, 220, 262, 220, 175, 196];
    const chords = [
      [262, 330, 392],
      [220, 262, 330],
      [196, 247, 294],
      [233, 294, 349]
    ];
    let step = 0;
    musicTimer = setInterval(() => {
      if (document.hidden || !settings.music) return;
      const volume = Number(settings.musicVolume) || 0;
      const melodyNote = melody[step % melody.length];
      tone(ctx, melodyNote, 0.34, "sine", volume * 0.022);
      if (step % 2 === 0) {
        tone(ctx, bass[(step / 2) % bass.length], 0.48, "triangle", volume * 0.012);
      }
      if (step % 3 === 0) {
        const chord = chords[(step / 3) % chords.length];
        chord.forEach(note => tone(ctx, note, 0.9, "sine", volume * 0.0045));
      }
      step += 1;
    }, 470);
  }

  function stopMusic() {
    if (!musicTimer) return;
    clearInterval(musicTimer);
    musicTimer = null;
  }

  if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => navigator.serviceWorker.register("sw.js").catch(() => {}));
  }

  init();
})();
