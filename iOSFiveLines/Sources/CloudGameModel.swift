import SwiftUI
import Foundation
import AudioToolbox

final class CloudGameModel: ObservableObject {
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
    @Published var difficulty = 1.0
    @Published var whiteProbability = 1.0
    @Published var bombProbability = 1.0
    @Published var musicEnabled = true
    @Published var effectsEnabled = true
    @Published var musicVolume = 0.65
    @Published var effectsVolume = 0.8
    @Published var moveSpeed: MoveSpeed = .slow

    private var busy = false
    private var clearedThisTurn = false

    init() {
        loadScores()
        startNewGame()
    }

    func startNewGame() {
        board = Array(repeating: nil, count: 81)
        nextTiles = randomPreview()
        selectedIndex = nil
        removing = []
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
            resolveAfterMove()
            return
        }
        let next = route[step]
        withAnimation(.easeInOut(duration: moveSpeed.stepDuration)) {
            board[current] = nil
            board[next] = tile
        }
        playEffect(1104)
        DispatchQueue.main.asyncAfter(deadline: .now() + moveSpeed.stepDuration) { [weak self] in
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
        playEffect(1105)

        withAnimation(.easeIn(duration: 0.5)) {
            for index in allCells {
                board[index] = nil
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            guard let self else { return }
            self.removing = []
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
        playEffect(1105)
        withAnimation(.easeIn(duration: 0.5)) {
            for index in allCells {
                board[index] = nil
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            guard let self else { return }
            self.removing = []
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
        spawnPieces(tiles: Array(tiles.prefix(count)), animated: true)
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

    private func spawnPieces(count: Int, animated: Bool) {
        spawnPieces(tiles: (0..<count).map { _ in randomTile() }, animated: animated)
    }

    private func spawnPieces(tiles: [Tile], animated: Bool) {
        let empty = board.indices.filter { board[$0] == nil }.shuffled()
        let placements = Array(empty.prefix(tiles.count))
        guard !placements.isEmpty else {
            busy = false
            checkGameOver()
            return
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

    private func playEffect(_ id: SystemSoundID) {
        guard effectsEnabled else { return }
        AudioServicesPlaySystemSound(id)
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
