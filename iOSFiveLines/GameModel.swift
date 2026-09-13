import Foundation
import SwiftUI
import AVFoundation

@MainActor
final class GameModel: ObservableObject {
    static let size = 9
    static let cellCount = size * size
    static let normalColors = 8
    static let white = 9
    static let bomb = 10

    @Published private(set) var board = Array(repeating: 0, count: cellCount)
    @Published private(set) var preview = Array(repeating: 1, count: 3)
    @Published private(set) var score = 0
    @Published private(set) var gameOver = false
    @Published var selectedIndex: Int?
    @Published private(set) var movingIndex: Int?
    @Published private(set) var movingType: Int = 0
    @Published private(set) var spawnedIndices = Set<Int>()
    @Published private(set) var removedIndices = Set<Int>()
    @Published private(set) var spawnScale: CGFloat = 1
    @Published private(set) var removeScale: CGFloat = 1
    @Published private(set) var highScores: [ScoreEntry] = []
    @Published var showMenu = false
    @Published var showSettings = false
    @Published var showHighScores = false
    @Published var showGameOver = false

    @Published var difficulty: Double = 1
    @Published var whiteProbability: Double = 1
    @Published var bombProbability: Double = 1
    @Published var movementSpeed = 0
    @Published var musicEnabled = true
    @Published var soundEnabled = true
    @Published var musicVolume: Double = 0.35
    @Published var soundVolume: Double = 0.75

    private var rng = SystemRandomNumberGenerator()
    private var moving = false
    private var resolving = false
    private var spawnedThisTurn = false
    private var clearedThisTurn = false
    private let audio = AudioEngine()
    private var moveTask: Task<Void, Never>?
    private var animationTask: Task<Void, Never>?
    private let settingsKey = "FiveLines.iOS.settings"
    private let scoresKey = "FiveLines.iOS.scores"

    init() {
        loadSettings()
        loadHighScores()
        reset()
    }

    func reset() {
        clearedThisTurn = false
        moveTask?.cancel()
        animationTask?.cancel()
        board = Array(repeating: 0, count: Self.cellCount)
        score = 0
        selectedIndex = nil
        movingIndex = nil
        movingType = 0
        moving = false
        resolving = false
        spawnedThisTurn = false
        clearedThisTurn = false
        clearedThisTurn = false
        clearedThisTurn = false
        clearedThisTurn = false
        clearedThisTurn = false
        clearedThisTurn = false
        clearedThisTurn = false
        gameOver = false
        showGameOver = false
        spawnScale = 1
        removeScale = 1
        spawnedIndices = []
        removedIndices = []
        preview = (0..<3).map { _ in randomPiece() }
        spawnPieces()
    }

    func applySettings() {
        difficulty = min(2, max(1, difficulty))
        whiteProbability = min(2, max(0, whiteProbability))
        bombProbability = min(2, max(0, bombProbability))
        movementSpeed = min(3, max(0, movementSpeed))
        let values: [String: Any] = [
            "difficulty": difficulty,
            "whiteProbability": whiteProbability,
            "bombProbability": bombProbability,
            "movementSpeed": movementSpeed,
            "musicEnabled": musicEnabled,
            "soundEnabled": soundEnabled,
            "musicVolume": musicVolume,
            "soundVolume": soundVolume
        ]
        UserDefaults.standard.set(values, forKey: settingsKey)
        audio.configure(soundEnabled: soundEnabled, soundVolume: soundVolume,
                        musicEnabled: musicEnabled, musicVolume: musicVolume)
    }

    func tap(index: Int) {
        guard !gameOver, !moving, !resolving, board.indices.contains(index) else { return }
        if board[index] != 0 {
            selectedIndex = index
            audio.playClick()
            return
        }
        guard let selected = selectedIndex,
              let path = shortestPath(from: selected, to: index) else { return }
        selectedIndex = nil
        startMove(path: path)
    }

    func saveScore(name: String) {
        let clean = name.trimmingCharacters(in: .whitespacesAndNewlines)
        highScores.append(ScoreEntry(name: clean.isEmpty ? "玩家" : String(clean.prefix(12)), score: score))
        highScores.sort { $0.score > $1.score }
        if highScores.count > 10 { highScores.removeLast(highScores.count - 10) }
        if let data = try? JSONEncoder().encode(highScores) {
            UserDefaults.standard.set(data, forKey: scoresKey)
        }
        showGameOver = false
    }

    func speedLabel(_ value: Int? = nil) -> String {
        switch value ?? movementSpeed {
        case 1: return "正常"
        case 2: return "快"
        case 3: return "光速"
        default: return "慢"
        }
    }

    private func loadSettings() {
        let values = UserDefaults.standard.dictionary(forKey: settingsKey) ?? [:]
        difficulty = values["difficulty"] as? Double ?? 1
        whiteProbability = values["whiteProbability"] as? Double ?? 1
        bombProbability = values["bombProbability"] as? Double ?? 1
        movementSpeed = values["movementSpeed"] as? Int ?? 0
        musicEnabled = values["musicEnabled"] as? Bool ?? true
        soundEnabled = values["soundEnabled"] as? Bool ?? true
        musicVolume = values["musicVolume"] as? Double ?? 0.35
        soundVolume = values["soundVolume"] as? Double ?? 0.75
        audio.configure(soundEnabled: soundEnabled, soundVolume: soundVolume,
                        musicEnabled: musicEnabled, musicVolume: musicVolume)
    }

    private func loadHighScores() {
        guard let data = UserDefaults.standard.data(forKey: scoresKey),
              let entries = try? JSONDecoder().decode([ScoreEntry].self, from: data) else { return }
        highScores = entries.sorted { $0.score > $1.score }.prefix(10).map { $0 }
    }

    private func randomPiece() -> Int {
        let base = 1.0 / Double(Self.normalColors + 2)
        let bombChance = base * self.bombProbability
        let whiteChance = base * self.whiteProbability
        let normalProbability = max(0, (1 - bombChance - whiteChance) / Double(Self.normalColors))
        let roll = Double.random(in: 0..<1, using: &rng)
        if roll < bombChance { return Self.bomb }
        if roll < bombChance + whiteChance { return Self.white }
        let color = Int((roll - bombChance - whiteChance) / max(0.000001, normalProbability)) + 1
        return min(Self.normalColors, max(1, color))
    }

    private func spawnCount() -> Int {
        guard score >= 50 else { return 3 }
        let factor = (Double(score) / 100) * (difficulty - 1)
        return 3 + Int(floor(3 * factor))
    }

    private func spawnPieces() {
        let empty = board.indices.filter { board[$0] == 0 }.shuffled()
        let count = min(spawnCount(), empty.count)
        spawnedIndices = Set(empty.prefix(count))
        for (offset, index) in empty.prefix(count).enumerated() {
            board[index] = offset < preview.count ? preview[offset] : randomPiece()
        }
        preview = (0..<3).map { _ in randomPiece() }
        guard count > 0 else { return }
        spawnScale = 0.05
        audio.playSpawn()
        withAnimation(.easeInOut(duration: 0.32)) { spawnScale = 1 }
        animationTask?.cancel()
        animationTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 340_000_000)
            guard !Task.isCancelled else { return }
            self?.spawnedIndices = []
        }
    }

    private func startMove(path: [Int]) {
        guard path.count > 1 else { return }
        moving = true
        movingType = board[path[0]]
        board[path[0]] = 0
        movingIndex = path[0]
        audio.playMove()
        let duration = movementStepDuration()
        moveTask?.cancel()
        moveTask = Task { [weak self] in
            guard let self else { return }
            for index in path.dropFirst() {
                try? await Task.sleep(nanoseconds: UInt64(duration * 1_000_000))
                guard !Task.isCancelled else { return }
                withAnimation(.linear(duration: Double(duration) / 1000)) {
                    self.movingIndex = index
                }
            }
            guard !Task.isCancelled else { return }
            self.board[path[path.count - 1]] = self.movingType
            self.movingIndex = nil
            self.moving = false
            self.spawnedThisTurn = false
            self.resolve()
        }
    }

    private func resolve() {
        // A line clear ends spawning for this turn; resolve() may still process cascades.
        guard !resolving, !gameOver else { return }
        if prepareRemoval() {
            animateRemoval()
        } else if board.allSatisfy({ $0 != 0 }) {
            gameOver = true
            showGameOver = true
        } else if clearedThisTurn || spawnedThisTurn {
            return
        } else {
        spawnedThisTurn = true
            spawnPieces()
            animationTask?.cancel()
            animationTask = Task { [weak self] in
                try? await Task.sleep(nanoseconds: 350_000_000)
                guard !Task.isCancelled else { return }
                self?.resolveAfterSpawn()
            }
        }
    }

    private func resolveAfterSpawn() {
        guard !gameOver else { return }
        if prepareRemoval() { animateRemoval() }
        else if board.allSatisfy({ $0 != 0 }) {
            gameOver = true
            showGameOver = true
        }
    }

    private func animateRemoval() {
        clearedThisTurn = true
        resolving = true
        let indices = removalPlan()
        removedIndices = Set(indices)
        removeScale = 1
        withAnimation(.easeIn(duration: 0.5)) { removeScale = 0.02 }
        if board.contains(Self.bomb) { audio.playExplosion() } else { audio.playClear() }
        animationTask?.cancel()
        animationTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 520_000_000)
            guard !Task.isCancelled, let self else { return }
            for index in indices { self.board[index] = 0 }
            self.score += self.lineScore(self.plannedLine.count) + self.plannedBlast.count
            self.removedIndices = []
            self.removeScale = 1
            self.resolving = false
            self.resolve()
        }
    }

    private var plannedLine = Set<Int>()
    private var plannedBlast = Set<Int>()

    private func prepareRemoval() -> Bool {
        _ = removalPlan()
        return !plannedLine.isEmpty || !plannedBlast.isEmpty
    }

    private func removalPlan() -> [Int] {
        plannedLine = []
        plannedBlast = []
        var bombColors = Set<Int>()
        let directions = [(1, 0), (0, 1), (1, 1), (1, -1)]
        for row in 0..<Self.size {
            for col in 0..<Self.size {
                let type = board[row * Self.size + col]
                guard (1...Self.normalColors).contains(type) else { continue }
                for (dr, dc) in directions {
                    let line = collectLine(row: row, col: col, color: type, dr: dr, dc: dc)
                    guard line.count >= 5 else { continue }
                    plannedLine.formUnion(line)
                    if line.contains(where: { board[$0] == Self.bomb }) { bombColors.insert(type) }
                }
            }
        }
        for index in board.indices where board[index] == Self.bomb {
            let row = index / Self.size, col = index % Self.size
            for color in 1...Self.normalColors {
                for (dr, dc) in directions {
                    let line = collectLine(row: row, col: col, color: color, dr: dr, dc: dc)
                    if line.count >= 5 && line.contains(where: { board[$0] == color }) {
                        plannedLine.formUnion(line)
                        bombColors.insert(color)
                    }
                }
            }
        }
        for color in bombColors {
            for index in board.indices where board[index] == color && !plannedLine.contains(index) {
                plannedBlast.insert(index)
            }
        }
        return Array(plannedLine.union(plannedBlast))
    }

    private func collectLine(row: Int, col: Int, color: Int, dr: Int, dc: Int) -> [Int] {
        guard matches(row: row, col: col, color: color) else { return [] }
        var result: [Int] = []
        var r = row, c = col
        while inside(r, c), matches(row: r, col: c, color: color) {
            result.append(r * Self.size + c); r -= dr; c -= dc
        }
        r = row + dr; c = col + dc
        while inside(r, c), matches(row: r, col: c, color: color) {
            result.append(r * Self.size + c); r += dr; c += dc
        }
        return Array(Set(result)).sorted()
    }

    private func matches(row: Int, col: Int, color: Int) -> Bool {
        let type = board[row * Self.size + col]
        return type == color || type == Self.white || type == Self.bomb
    }

    private func lineScore(_ count: Int) -> Int {
        count >= 5 ? 5 + (count - 5) * 2 : 0
    }

    private func shortestPath(from: Int, to: Int) -> [Int]? {
        guard board[from] != 0, board[to] == 0 else { return nil }
        var queue = [from], head = 0
        var previous = Array(repeating: -1, count: Self.cellCount)
        var visited = Array(repeating: false, count: Self.cellCount)
        visited[from] = true
        let steps = [-1, 1, -Self.size, Self.size]
        while head < queue.count {
            let current = queue[head]; head += 1
            if current == to { break }
            for step in steps {
                let next = current + step
                guard next >= 0, next < Self.cellCount,
                      (step == -1 ? current % Self.size > 0 : step == 1 ? current % Self.size < Self.size - 1 : true),
                      !visited[next], board[next] == 0 || next == to else { continue }
                visited[next] = true; previous[next] = current; queue.append(next)
            }
        }
        guard visited[to] else { return nil }
        var path: [Int] = []; var current = to
        while current != -1 { path.append(current); if current == from { break }; current = previous[current] }
        return path.last == from ? Array(path.reversed()) : nil
    }

    private func inside(_ row: Int, _ col: Int) -> Bool { row >= 0 && row < Self.size && col >= 0 && col < Self.size }
    private func movementStepDuration() -> Int { [220, 135, 80, 35][movementSpeed] }
}

struct ScoreEntry: Codable, Identifiable {
    var id = UUID()
    var name: String
    var score: Int
}

private final class AudioEngine {
    private var soundEnabled = true
    private var soundVolume = 0.75
    private var musicEnabled = true
    private var musicVolume = 0.35
    private var musicIndex = 0
    private var musicTimer: Timer?
    private var activePlayers: [AVAudioPlayer] = []

    func configure(soundEnabled: Bool, soundVolume: Double, musicEnabled: Bool, musicVolume: Double) {
        self.soundEnabled = soundEnabled; self.soundVolume = soundVolume
        self.musicEnabled = musicEnabled; self.musicVolume = musicVolume
        if musicEnabled { startMusic() } else { stopMusic() }
    }

    func playClick() { playTone(660, 0.055, 0.4) }
    func playMove() { playTone(440, 0.13, 0.28) }
    func playSpawn() { playTone(520, 0.10, 0.22) }
    func playClear() { playTone(740, 0.18, 0.38) }
    func playExplosion() { playTone(72, 0.39, 0.82); playNoise(0.26, 0.58) }

    private func startMusic() {
        guard musicTimer == nil else { return }
        playMusicNote()
        musicTimer = Timer.scheduledTimer(withTimeInterval: 0.43, repeats: true) { [weak self] _ in
            self?.playMusicNote()
        }
    }

    private func stopMusic() {
        musicTimer?.invalidate()
        musicTimer = nil
    }

    private func playMusicNote() {
        guard musicEnabled else { return }
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
        playSamples(wave: { i, rate in sin(2 * .pi * frequency * Double(i) / rate) }, duration: duration, volume: volume, music: music)
    }
    private func playNoise(_ duration: Double, _ volume: Double) { playSamples(wave: { _, _ in Double.random(in: -1...1) }, duration: duration, volume: volume) }

    private func playSamples(wave: (Int, Double) -> Double, duration: Double, volume: Double, music: Bool = false) {
        guard (music ? musicEnabled : soundEnabled), volume > 0.001 else { return }
        let rate = 22_050.0, count = max(1, Int(rate * duration))
        var data = Data(); data.append(contentsOf: wavHeader(sampleRate: Int(rate), sampleCount: count))
        for i in 0..<count {
            let progress = Double(i) / Double(count)
            let envelope = min(1, Double(i) / (rate * 0.012)) * min(1, (1 - progress) / 0.16)
            var sample = Int16(max(-1, min(1, wave(i, rate) * envelope * volume)) * Double(Int16.max))
            data.append(Data(bytes: &sample, count: 2))
        }
        guard let player = try? AVAudioPlayer(data: data) else { return }
        player.volume = 1; player.prepareToPlay(); player.play()
        activePlayers.append(player)
        activePlayers = activePlayers.filter { $0.isPlaying }
    }

    private func wavHeader(sampleRate: Int, sampleCount: Int) -> Data {
        let bytes = sampleCount * 2, total = bytes + 36
        var d = Data("RIFF".utf8); appendLE(&d, total); d.append(contentsOf: Data("WAVEfmt ".utf8)); appendLE(&d, 16); appendLE(&d, Int16(1)); appendLE(&d, Int16(1)); appendLE(&d, sampleRate); appendLE(&d, sampleRate * 2); appendLE(&d, Int16(2)); appendLE(&d, Int16(16)); d.append(contentsOf: Data("data".utf8)); appendLE(&d, bytes); return d
    }
    private func appendLE<T: FixedWidthInteger>(_ data: inout Data, _ value: T) { var v = value.littleEndian; data.append(Data(bytes: &v, count: MemoryLayout<T>.size)) }
}
