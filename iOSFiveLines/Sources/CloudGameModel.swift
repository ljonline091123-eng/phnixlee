import SwiftUI
import Foundation
import Darwin
import AudioToolbox
import AVFoundation

final class CloudGameModel: ObservableObject {
    static let defaultDifficulty = 1.1
    static let defaultWhiteProbability = 0.8
    static let defaultBombProbability = 0.4

    enum Tile: String, CaseIterable, Identifiable, Codable, Hashable {
        case red, yellow, green, blue, purple, cyan, pink, orange, black, white, bomb

        var id: String { rawValue }
        var isWildcard: Bool { self == .white || self == .bomb }
        var isBomb: Bool { self == .bomb }

        var color: Color {
            switch self {
            case .red: return Color(red: 0.88, green: 0.12, blue: 0.15)
            case .yellow: return Color(red: 0.98, green: 0.75, blue: 0.08)
            case .green: return Color(red: 0.10, green: 0.62, blue: 0.28)
            case .blue: return Color(red: 0.08, green: 0.32, blue: 0.83)
            case .purple: return Color(red: 0.47, green: 0.18, blue: 0.72)
            case .cyan: return Color(red: 0.02, green: 0.68, blue: 0.72)
            case .pink: return Color(red: 0.92, green: 0.22, blue: 0.52)
            case .orange: return Color(red: 0.95, green: 0.40, blue: 0.05)
            case .black: return Color(red: 0.06, green: 0.07, blue: 0.09)
            case .white: return Color.white
            case .bomb: return Color(red: 0.20, green: 0.22, blue: 0.25)
            }
        }
    }

    enum MoveSpeed: String, CaseIterable, Identifiable {
        case slow, normal, fast, lightning
        var id: String { rawValue }
        var title: String {
            switch self {
            case .slow: return "慢"
            case .normal: return "正常"
            case .fast: return "快"
            case .lightning: return "光速"
            }
        }
        var stepDuration: Double {
            switch self {
            case .slow: return 0.24
            case .normal: return 0.14
            case .fast: return 0.08
            case .lightning: return 0.035
            }
        }
    }

    enum MusicTrack: Int, CaseIterable, Identifiable, Equatable {
        case music1 = 0
        case music2 = 1
        case sy = 2

        var id: Int { rawValue }

        var title: String {
            switch self {
            case .music1: return "\u{97F3}\u{4E50}1"
            case .music2: return "\u{97F3}\u{4E50}2"
            case .sy: return "sy.mp3"
            }
        }

        static var selectable: [MusicTrack] { [.music1, .music2] }
    }

    enum EasterKind: Equatable {
        case ray
        case molly
        case toutou

        var loadingTitle: String {
            switch self {
            case .ray: return "Ray"
            case .molly: return "Molly"
            case .toutou: return "头头"
            }
        }

        var loadingSubtitle: String {
            switch self {
            case .toutou: return "1314"
            default: return "loading..."
            }
        }

        var loadingMessage: String { "\(loadingTitle) \(loadingSubtitle)" }

        var secret: Character {
            switch self {
            case .ray: return "A"
            case .molly: return "y"
            case .toutou: return "A"
            }
        }

        var accent: Color {
            switch self {
            case .ray: return Color(red: 0.02, green: 0.68, blue: 0.72)
            case .molly: return Color(red: 0.92, green: 0.22, blue: 0.52)
            case .toutou: return Color(red: 1.0, green: 0.25, blue: 0.45)
            }
        }

        var secretTapCount: Int {
            switch self {
            case .toutou: return 13
            default: return 5
            }
        }

        var loadingColorCount: Int {
            switch self {
            case .toutou: return 7
            default: return 9
            }
        }
    }

    struct EasterLoading: Equatable {
        var title: String
        var subtitle: String
        var colorPhase: Int
        var visible: Bool
    }

    struct EasterPrompt: Identifiable, Equatable {
        let kind: EasterKind
        let id = UUID()

        var title: String { kind.loadingMessage }
        var secret: Character { kind.secret }
        var accent: Color { kind.accent }
    }

    struct ScoreEntry: Identifiable, Codable {
        let id: UUID
        var name: String
        var score: Int

        init(name: String, score: Int) {
            id = UUID()
            self.name = name
            self.score = score
        }
    }

    private struct MatchResult {
        var lineCells: Set<Int> = []
        var bombColors: Set<Tile> = []
    }

    @Published var board: [Tile?] = Array(repeating: nil, count: 81)
    @Published var nextTiles: [Tile] = []
    @Published var selectedIndex: Int?
    @Published var removing: Set<Int> = []
    @Published var score = 0
    @Published var highScores: [ScoreEntry] = []
    @Published var isGameOver = false
    @Published var difficulty = CloudGameModel.defaultDifficulty { didSet { saveSettings() } }
    @Published var whiteProbability = CloudGameModel.defaultWhiteProbability { didSet { saveSettings() } }
    @Published var bombProbability = CloudGameModel.defaultBombProbability { didSet { saveSettings() } }
    @Published var musicEnabled = true { didSet { applyAudioSettings() } }
    @Published var effectsEnabled = true { didSet { applyAudioSettings() } }
    @Published var musicVolume = 0.65 { didSet { applyAudioSettings() } }
    @Published var effectsVolume = 0.8 { didSet { applyAudioSettings() } }
    @Published var musicTrack: MusicTrack = .music1 { didSet { applyAudioSettings() } }
    @Published var moveSpeed: MoveSpeed = .slow { didSet { saveSettings() } }
    @Published var adminMode = false { didSet { saveSettings() } }
    @Published var rayRacerMode = false
    @Published var kuromiTheme = false
    @Published var heartMode = false
    @Published var easterLoading: EasterLoading?
    @Published var easterPrompt: EasterPrompt?
    @Published var racerTrail: [Int] = []
    @Published var racerTrailTile: Tile?
    @Published var explodingBombs: Set<Int> = []

    private var busy = false
    private var clearedThisTurn = false
    private let audio = AudioEngine()
    private let settingsKey = "FiveLines.settings"
    private var loadingTimer: Timer?
    private var easterSecretTaps = 0
    private var musicToggleUnlockCount = 0
    private var suppressSettingsSave = false

    init() {
        loadScores()
        loadSettings()
        applyAudioSettings()
        startNewGame()
    }

    func startNewGame() {
        loadingTimer?.invalidate()
        loadingTimer = nil
        board = Array(repeating: nil, count: 81)
        nextTiles = randomPreview()
        selectedIndex = nil
        removing = []
        easterLoading = nil
        easterPrompt = nil
        racerTrail = []
        racerTrailTile = nil
        explodingBombs = []
        easterSecretTaps = 0
        rayRacerMode = false
        kuromiTheme = false
        heartMode = false
        restoreConfiguredMusicAfterHiddenMode()
        score = 0
        isGameOver = false
        busy = false
        clearedThisTurn = false
        spawnPieces(count: 3, animated: false)
    }

    func tap(_ index: Int) {
        guard !busy, !isGameOver else { return }
        guard index >= 0, index < board.count else { return }

        if let selected = selectedIndex {
            if board[index] == nil {
                guard let route = shortestPath(from: selected, to: index) else {
                    selectedIndex = nil
                    return
                }
                clearedThisTurn = false
                selectedIndex = nil
                busy = true
                moveAlongPath(tile: board[selected]!, from: selected, route: route, step: 0)
            } else {
                selectedIndex = index
            }
        } else if board[index] != nil {
            selectedIndex = index
        }
    }

    func submitScore(_ rawName: String) {
        let trimmed = rawName.trimmingCharacters(in: .whitespacesAndNewlines)
        let name = trimmed.isEmpty ? "玩家" : String(trimmed.prefix(12))
        highScores.append(ScoreEntry(name: name, score: score))
        highScores.sort { $0.score > $1.score }
        highScores = Array(highScores.prefix(10))
        saveScores()
        isGameOver = false
        startNewGame()
    }

    func colorName(_ tile: Tile) -> String {
        switch tile {
        case .red: return "红"
        case .yellow: return "黄"
        case .green: return "绿"
        case .blue: return "蓝"
        case .purple: return "紫"
        case .cyan: return "青"
        case .pink: return "粉"
        case .orange: return "橙"
        case .black: return "黑"
        case .white: return "白"
        case .bomb: return "炸药"
        }
    }

    private func moveAlongPath(tile: Tile, from current: Int, route: [Int], step: Int) {
        guard step < route.count else {
            if tryStartCornerEasterEgg() {
                return
            }
            if rayRacerMode || heartMode {
                let trail = racerTrail
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) { [weak self] in
                    guard let self, self.racerTrail == trail else { return }
                    self.racerTrail = []
                    self.racerTrailTile = nil
                }
            } else {
                racerTrail = []
                racerTrailTile = nil
            }
            resolveAfterMove()
            return
        }
        let next = route[step]
        if rayRacerMode || heartMode {
            racerTrailTile = tile
            racerTrail = Array(([current] + racerTrail).prefix(7))
        }
        withAnimation(.easeInOut(duration: activeStepDuration)) {
            board[current] = nil
            board[next] = tile
        }
        playEffect(1104)
        DispatchQueue.main.asyncAfter(deadline: .now() + activeStepDuration) { [weak self] in
            self?.moveAlongPath(tile: tile, from: next, route: route, step: step + 1)
        }
    }

    private func resolveAfterMove() {
        let match = findMatches()
        guard !match.lineCells.isEmpty else {
            spawnAfterUnsuccessfulMove()
            return
        }
        clearedThisTurn = true
        busy = true
        let blastCells = bombBlastCells(for: match)
        let allCells = match.lineCells.union(blastCells)
        let lineCount = match.lineCells.count
        let blastOnlyCount = blastCells.subtracting(match.lineCells).count
        score += lineCount >= 5 ? 5 + (lineCount - 5) * 2 : 0
        score += blastOnlyCount
        removing = allCells
        explodingBombs = Set(match.lineCells.filter { board[$0]?.isBomb == true })
        playEffect(1105)

        withAnimation(.easeIn(duration: 0.5)) {
            for index in allCells {
                board[index] = nil
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            guard let self else { return }
            self.removing = []
            self.explodingBombs = []
            self.resolveChain()
        }
    }

    private func resolveChain() {
        let match = findMatches()
        guard !match.lineCells.isEmpty else {
            busy = false
            if !clearedThisTurn {
                spawnAfterUnsuccessfulMove()
            } else {
                checkGameOver()
            }
            return
        }
        clearedThisTurn = true
        let blastCells = bombBlastCells(for: match)
        let allCells = match.lineCells.union(blastCells)
        let lineCount = match.lineCells.count
        let blastOnlyCount = blastCells.subtracting(match.lineCells).count
        score += lineCount >= 5 ? 5 + (lineCount - 5) * 2 : 0
        score += blastOnlyCount
        removing = allCells
        explodingBombs = Set(match.lineCells.filter { board[$0]?.isBomb == true })
        playEffect(1105)
        withAnimation(.easeIn(duration: 0.5)) {
            for index in allCells {
                board[index] = nil
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            guard let self else { return }
            self.removing = []
            self.explodingBombs = []
            self.resolveChain()
        }
    }

    private func spawnAfterUnsuccessfulMove() {
        guard !clearedThisTurn else {
            busy = false
            checkGameOver()
            return
        }
        let count = spawnCount()
        var tiles = nextTiles
        if tiles.count < count {
            tiles.append(contentsOf: (0..<(count - tiles.count)).map { _ in randomTile() })
        }
        if spawnPieces(tiles: Array(tiles.prefix(count)), animated: true) {
            return
        }
        nextTiles = randomPreview()
        busy = false
        checkGameOver()
    }

    private func spawnCount() -> Int {
        let base = 3
        guard score >= 50, difficulty > 1 else { return base }
        let extra = Int(Double(base) * ((Double(score) / 100.0) * (difficulty - 1.0)))
        return max(base, base + extra)
    }

    @discardableResult
    private func spawnPieces(count: Int, animated: Bool) -> Bool {
        spawnPieces(tiles: (0..<count).map { _ in randomTile() }, animated: animated)
    }

    @discardableResult
    private func spawnPieces(tiles: [Tile], animated: Bool) -> Bool {
        let empty = board.indices.filter { board[$0] == nil }.shuffled()
        let placements = Array(empty.prefix(tiles.count))
        guard !placements.isEmpty else {
            busy = false
            checkGameOver()
            return false
        }
        let changes = {
            for (index, tile) in zip(placements, tiles) {
                self.board[index] = tile
            }
        }
        if animated {
            withAnimation(.spring(response: 0.32, dampingFraction: 0.68)) {
                changes()
            }
            playEffect(1103)
        } else {
            changes()
        }
        return tryStartCornerEasterEgg()
    }

    private func tryStartCornerEasterEgg() -> Bool {
        guard easterLoading == nil, easterPrompt == nil, !isGameOver else { return false }
        let corners = [0, 8, 72, 80]
        if corners.allSatisfy({ board[$0] == .cyan }) {
            startCornerEasterEgg(.ray)
            return true
        }
        if corners.allSatisfy({ board[$0] == .pink }) {
            startCornerEasterEgg(.molly)
            return true
        }
        return false
    }

    private func startCornerEasterEgg(_ kind: EasterKind) {
        loadingTimer?.invalidate()
        selectedIndex = nil
        removing = []
        board = Array(repeating: nil, count: 81)
        nextTiles = []
        rayRacerMode = false
        kuromiTheme = false
        heartMode = false
        restoreConfiguredMusicAfterHiddenMode()
        racerTrail = []
        racerTrailTile = nil
        explodingBombs = []
        busy = true
        clearedThisTurn = false
        easterSecretTaps = 0
        easterPrompt = nil
        easterLoading = EasterLoading(title: kind.loadingTitle, subtitle: kind.loadingSubtitle, colorPhase: 0, visible: true)
        audio.playClear()
        audio.playAlarm()

        let startedAt = Date()
        let flashHalfDuration = 0.18
        let flashFullDuration = flashHalfDuration * 2
        let totalDuration = Double(kind.loadingColorCount) * flashFullDuration
        var lastAlarmPhase = 0
        loadingTimer = Timer.scheduledTimer(withTimeInterval: 0.08, repeats: true) { [weak self] timer in
            guard let self else {
                timer.invalidate()
                return
            }
            let elapsed = Date().timeIntervalSince(startedAt)
            if elapsed >= totalDuration {
                timer.invalidate()
                self.loadingTimer = nil
                self.easterLoading = nil
                self.easterPrompt = EasterPrompt(kind: kind)
                return
            }
            let colorPhase = min(kind.loadingColorCount - 1, Int(elapsed / flashFullDuration))
            if colorPhase != lastAlarmPhase {
                lastAlarmPhase = colorPhase
                self.audio.playAlarm()
            }
            let halfCycle = Int(elapsed / flashHalfDuration)
            self.easterLoading = EasterLoading(
                title: kind.loadingTitle,
                subtitle: kind.loadingSubtitle,
                colorPhase: colorPhase,
                visible: halfCycle % 2 == 0
            )
        }
    }

    func closeEasterPrompt() {
        finishEaster(hidden: false)
    }

    func tapEasterSecret(_ value: Character) {
        guard let prompt = easterPrompt, prompt.secret == value else { return }
        easterSecretTaps += 1
        audio.playClick()
        if easterSecretTaps >= prompt.kind.secretTapCount {
            finishEaster(hidden: true)
        }
    }

    private func finishEaster(hidden: Bool) {
        let kind = easterPrompt?.kind
        loadingTimer?.invalidate()
        loadingTimer = nil
        easterLoading = nil
        easterPrompt = nil
        easterSecretTaps = 0

        if kind == .toutou && !hidden {
            exit(0)
        }

        if hidden {
            switch kind {
            case .ray:
                rayRacerMode = true
            case .molly:
                kuromiTheme = true
            case .toutou:
                heartMode = true
                suppressSettingsSave = true
                musicEnabled = true
                musicTrack = .sy
                suppressSettingsSave = false
                audio.configure(
                    soundEnabled: effectsEnabled,
                    soundVolume: effectsVolume,
                    musicEnabled: true,
                    musicVolume: musicVolume,
                    musicTrack: .sy
                )
            case nil:
                break
            }
            audio.playClear()
        }

        nextTiles = randomPreview()
        if spawnPieces(tiles: nextTiles, animated: true) {
            return
        }
        nextTiles = randomPreview()
        busy = false
        checkGameOver()
    }

    private func randomPreview() -> [Tile] {
        (0..<3).map { _ in randomTile() }
    }

    private func randomTile() -> Tile {
        let base = 1.0 / 11.0
        let white = base * whiteProbability
        let bomb = base * bombProbability
        let ordinary = max(0.0, 1.0 - white - bomb) / 9.0
        let roll = Double.random(in: 0..<1)
        if roll < white { return .white }
        if roll < white + bomb { return .bomb }
        let index = Int.random(in: 0..<9)
        _ = ordinary
        return Tile.allCases[index]
    }

    private func bombBlastCells(for match: MatchResult) -> Set<Int> {
        var cells: Set<Int> = []
        for color in match.bombColors {
            cells.formUnion(board.indices.filter { board[$0] == color })
        }
        return cells
    }

    private func findMatches() -> MatchResult {
        var result = MatchResult()
        let directions = [(0, 1), (1, 0), (1, 1), (1, -1)]

        for start in board.indices {
            let startRow = start / 9
            let startColumn = start % 9
            for direction in directions {
                var window: [Int] = []
                for length in 1...9 {
                    let row = startRow + direction.0 * (length - 1)
                    let column = startColumn + direction.1 * (length - 1)
                    guard row >= 0, row < 9, column >= 0, column < 9 else { break }
                    let index = row * 9 + column
                    guard board[index] != nil else { break }
                    window.append(index)
                    guard window.count >= 5 else { continue }

                    let tiles = window.compactMap { board[$0] }
                    let normal = tiles.filter { !$0.isWildcard }
                    guard let color = normal.first,
                          Set(normal).count == 1,
                          (tiles.allSatisfy { !$0.isWildcard } || normal.count >= 3) else {
                        continue
                    }
                    result.lineCells.formUnion(window)
                    if tiles.contains(where: { $0.isBomb }) {
                        result.bombColors.insert(color)
                    }
                }
            }
        }
        return result
    }

    private func shortestPath(from start: Int, to goal: Int) -> [Int]? {
        var queue = [start]
        var previous: [Int: Int] = [:]
        var visited: Set<Int> = [start]

        while !queue.isEmpty {
            let current = queue.removeFirst()
            if current == goal {
                var route: [Int] = []
                var cursor = goal
                while cursor != start {
                    route.append(cursor)
                    guard let parent = previous[cursor] else { return nil }
                    cursor = parent
                }
                return Array(route.reversed())
            }
            for neighbor in neighbors(of: current) {
                guard !visited.contains(neighbor),
                      neighbor == goal || board[neighbor] == nil else { continue }
                visited.insert(neighbor)
                previous[neighbor] = current
                queue.append(neighbor)
            }
        }
        return nil
    }

    private func neighbors(of index: Int) -> [Int] {
        let row = index / 9
        let column = index % 9
        return [(row - 1, column), (row + 1, column), (row, column - 1), (row, column + 1)]
            .filter { $0.0 >= 0 && $0.0 < 9 && $0.1 >= 0 && $0.1 < 9 }
            .map { $0.0 * 9 + $0.1 }
    }

    private func checkGameOver() {
        guard !board.contains(where: { $0 == nil }) else { return }
        busy = false
        isGameOver = true
    }

    private var activeStepDuration: Double {
        rayRacerMode ? 0.0035 : moveSpeed.stepDuration
    }

    private func playEffect(_ id: SystemSoundID) {
        switch id {
        case 1103:
            audio.playSpawn()
        case 1104:
            audio.playMove()
        case 1105:
            audio.playClear()
        default:
            audio.playClick()
        }
    }

    func noteMusicToggleForAdminUnlock() {
        musicToggleUnlockCount += 1
        if !adminMode, musicToggleUnlockCount >= 7 {
            adminMode = true
            musicToggleUnlockCount = 0
            audio.playAlarm()
        } else {
            audio.playClick()
        }
    }

    func saveSettingsAndCheckAdminEaster() {
        if !adminMode {
            difficulty = Self.defaultDifficulty
            whiteProbability = Self.defaultWhiteProbability
            bombProbability = Self.defaultBombProbability
        }
        saveSettings()
        if adminMode
            && abs(whiteProbability - 1.3) < 0.001
            && abs(bombProbability - 1.4) < 0.001 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.12) { [weak self] in
                self?.startCornerEasterEgg(.toutou)
            }
        }
    }

    private func applyAudioSettings() {
        guard !suppressSettingsSave else { return }
        audio.configure(
            soundEnabled: effectsEnabled,
            soundVolume: effectsVolume,
            musicEnabled: musicEnabled,
            musicVolume: musicVolume,
            musicTrack: musicTrack
        )
        saveSettings()
    }

    private func restoreConfiguredMusicAfterHiddenMode() {
        guard musicTrack == .sy else { return }
        let values = UserDefaults.standard.dictionary(forKey: settingsKey) ?? [:]
        musicEnabled = values["musicEnabled"] as? Bool ?? true
        if let rawTrack = values["musicTrack"] as? Int,
           let track = MusicTrack(rawValue: rawTrack),
           MusicTrack.selectable.contains(track) {
            musicTrack = track
        } else {
            musicTrack = .music1
        }
        applyAudioSettings()
    }

    private func loadSettings() {
        suppressSettingsSave = true
        defer { suppressSettingsSave = false }
        let values = UserDefaults.standard.dictionary(forKey: settingsKey) ?? [:]
        adminMode = values["adminMode"] as? Bool ?? false
        difficulty = adminMode ? values["difficulty"] as? Double ?? Self.defaultDifficulty : Self.defaultDifficulty
        whiteProbability = adminMode ? values["whiteProbability"] as? Double ?? Self.defaultWhiteProbability : Self.defaultWhiteProbability
        bombProbability = adminMode ? values["bombProbability"] as? Double ?? Self.defaultBombProbability : Self.defaultBombProbability
        musicEnabled = values["musicEnabled"] as? Bool ?? true
        effectsEnabled = values["effectsEnabled"] as? Bool ?? true
        musicVolume = values["musicVolume"] as? Double ?? 0.65
        effectsVolume = values["effectsVolume"] as? Double ?? 0.8
        if let rawTrack = values["musicTrack"] as? Int,
           let track = MusicTrack(rawValue: rawTrack),
           MusicTrack.selectable.contains(track) {
            musicTrack = track
        }
        if let rawSpeed = values["moveSpeed"] as? String,
           let speed = MoveSpeed(rawValue: rawSpeed) {
            moveSpeed = speed
        }
    }

    private func saveSettings() {
        guard !suppressSettingsSave else { return }
        let savedMusicTrack = MusicTrack.selectable.contains(musicTrack)
            ? musicTrack.rawValue
            : (UserDefaults.standard.dictionary(forKey: settingsKey)?["musicTrack"] as? Int ?? MusicTrack.music1.rawValue)
        let values: [String: Any] = [
            "difficulty": difficulty,
            "whiteProbability": whiteProbability,
            "bombProbability": bombProbability,
            "adminMode": adminMode,
            "musicEnabled": musicEnabled,
            "effectsEnabled": effectsEnabled,
            "musicVolume": musicVolume,
            "effectsVolume": effectsVolume,
            "musicTrack": savedMusicTrack,
            "moveSpeed": moveSpeed.rawValue
        ]
        UserDefaults.standard.set(values, forKey: settingsKey)
    }

    private func loadScores() {
        guard let data = UserDefaults.standard.data(forKey: "FiveLines.highScores"),
              let values = try? JSONDecoder().decode([ScoreEntry].self, from: data) else { return }
        highScores = values
    }

    private func saveScores() {
        guard let data = try? JSONEncoder().encode(highScores) else { return }
        UserDefaults.standard.set(data, forKey: "FiveLines.highScores")
    }
}

private final class AudioEngine {
    private var soundEnabled = true
    private var soundVolume = 0.8
    private var musicEnabled = true
    private var musicVolume = 0.65
    private var musicTrack: CloudGameModel.MusicTrack = .music1
    private var musicIndex = 0
    private var musicTimer: Timer?
    private var midiPlayer: AVAudioPlayer?
    private var activePlayers: [AVAudioPlayer] = []

    func configure(
        soundEnabled: Bool,
        soundVolume: Double,
        musicEnabled: Bool,
        musicVolume: Double,
        musicTrack: CloudGameModel.MusicTrack
    ) {
        let trackChanged = self.musicTrack != musicTrack
        self.soundEnabled = soundEnabled
        self.soundVolume = min(1, max(0, soundVolume))
        self.musicEnabled = musicEnabled
        self.musicVolume = min(1, max(0, musicVolume))
        self.musicTrack = musicTrack

        if musicEnabled {
            if trackChanged {
                stopMusic()
            }
            startMusic()
        } else {
            stopMusic()
        }
        midiPlayer?.volume = Float(self.musicVolume)
    }

    func playClick() { playTone(660, 0.055, soundVolume * 0.40) }
    func playMove() { playTone(440, 0.13, soundVolume * 0.28) }
    func playSpawn() { playTone(520, 0.10, soundVolume * 0.22) }
    func playClear() { playTone(740, 0.18, soundVolume * 0.38) }
    func playAlarm() {
        playTone(880, 0.095, soundVolume * 0.46)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.086) { [weak self] in
            guard let self else { return }
            self.playTone(1320, 0.11, self.soundVolume * 0.36)
        }
    }
    func playExplosion() { playTone(72, 0.39, soundVolume * 0.82); playNoise(0.26, soundVolume * 0.58) }

    private func startMusic() {
        switch musicTrack {
        case .music1:
            startSynthMusic()
        case .music2:
            startBundledMusic(named: "midi")
        case .sy:
            startBundledMusic(named: "sy")
        }
    }

    private func startSynthMusic() {
        guard musicTimer == nil else { return }
        playMusicNote()
        musicTimer = Timer.scheduledTimer(withTimeInterval: 0.43, repeats: true) { [weak self] _ in
            self?.playMusicNote()
        }
    }

    private func startBundledMusic(named resourceName: String) {
        guard musicEnabled else { return }
        do {
            if midiPlayer == nil {
                guard let url = Bundle.main.url(forResource: resourceName, withExtension: "mp3") else { return }
                midiPlayer = try AVAudioPlayer(contentsOf: url)
                midiPlayer?.numberOfLoops = -1
                midiPlayer?.prepareToPlay()
            }
            midiPlayer?.volume = Float(musicVolume)
            midiPlayer?.play()
        } catch {
            midiPlayer = nil
        }
    }

    private func stopMusic() {
        musicTimer?.invalidate()
        musicTimer = nil
        midiPlayer?.stop()
        midiPlayer = nil
    }

    private func playMusicNote() {
        guard musicEnabled, musicTrack == .music1 else { return }
        let melody = [
            261.63, 329.63, 392.00, 523.25,
            392.00, 329.63, 293.66, 329.63,
            261.63, 329.63, 392.00, 440.00,
            392.00, 329.63, 293.66, 261.63
        ]
        let harmony = [130.81, 164.81, 196.00, 220.00]
        let index = musicIndex % melody.count
        musicIndex += 1
        playTone(melody[index], 0.36, musicVolume * 0.13, music: true)
        if index % 4 == 0 {
            playTone(harmony[(index / 4) % harmony.count], 0.72, musicVolume * 0.055, music: true)
        }
    }

    private func playTone(_ frequency: Double, _ duration: Double, _ volume: Double, music: Bool = false) {
        playSamples(
            wave: { i, rate in sin(2 * .pi * frequency * Double(i) / rate) },
            duration: duration,
            volume: volume,
            music: music
        )
    }

    private func playNoise(_ duration: Double, _ volume: Double) {
        playSamples(wave: { _, _ in Double.random(in: -1...1) }, duration: duration, volume: volume)
    }

    private func playSamples(
        wave: (Int, Double) -> Double,
        duration: Double,
        volume: Double,
        music: Bool = false
    ) {
        guard (music ? musicEnabled : soundEnabled), volume > 0.001 else { return }
        let rate = 22_050.0
        let count = max(1, Int(rate * duration))
        var data = Data()
        data.append(contentsOf: wavHeader(sampleRate: Int(rate), sampleCount: count))
        for i in 0..<count {
            let progress = Double(i) / Double(count)
            let envelope = min(1, Double(i) / (rate * 0.012)) * min(1, (1 - progress) / 0.16)
            var sample = Int16(max(-1, min(1, wave(i, rate) * envelope * volume)) * Double(Int16.max))
            data.append(Data(bytes: &sample, count: 2))
        }
        guard let player = try? AVAudioPlayer(data: data) else { return }
        player.volume = 1
        player.prepareToPlay()
        player.play()
        activePlayers.append(player)
        activePlayers = activePlayers.filter { $0.isPlaying }
    }

    private func wavHeader(sampleRate: Int, sampleCount: Int) -> Data {
        let bytes = sampleCount * 2
        let total = bytes + 36
        var data = Data("RIFF".utf8)
        appendLE(&data, total)
        data.append(contentsOf: Data("WAVEfmt ".utf8))
        appendLE(&data, 16)
        appendLE(&data, Int16(1))
        appendLE(&data, Int16(1))
        appendLE(&data, sampleRate)
        appendLE(&data, sampleRate * 2)
        appendLE(&data, Int16(2))
        appendLE(&data, Int16(16))
        data.append(contentsOf: Data("data".utf8))
        appendLE(&data, bytes)
        return data
    }

    private func appendLE<T: FixedWidthInteger>(_ data: inout Data, _ value: T) {
        var value = value.littleEndian
        data.append(Data(bytes: &value, count: MemoryLayout<T>.size))
    }
}
