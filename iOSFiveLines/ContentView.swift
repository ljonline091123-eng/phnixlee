import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var game: GameModel

    var body: some View {
        GeometryReader { proxy in
            let boardWidth = min(proxy.size.width - 24, proxy.size.height - 230)
            VStack(spacing: 0) {
                header
                board(size: max(280, boardWidth))
                    .frame(width: max(280, boardWidth), height: max(280, boardWidth))
                    .padding(.top, 14)
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(red: 0.945, green: 0.918, blue: 0.875))
        }
        .sheet(isPresented: $game.showMenu) { MenuView() }
        .sheet(isPresented: $game.showSettings) { SettingsView() }
        .sheet(isPresented: $game.showHighScores) { HighScoresView() }
        .sheet(isPresented: $game.showGameOver) { GameOverView() }
    }

    private var header: some View {
        VStack(spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("五子消除").font(.system(size: 27, weight: .bold)).foregroundColor(.white)
                    Text("白色万能 · 炸药可清除同色棋子").font(.caption).foregroundColor(.white.opacity(0.76))
                }
                Spacer()
                Button("菜单") { game.showMenu = true }
                    .font(.headline).foregroundColor(.white)
                    .padding(.horizontal, 16).padding(.vertical, 11)
                    .background(Color.red.opacity(0.86), in: RoundedRectangle(cornerRadius: 13))
            }
            HStack(spacing: 12) {
                scoreBox
                previewBox
            }
        }
        .padding(.horizontal, 18).padding(.top, 12).padding(.bottom, 10)
        .background(Color(red: 0.063, green: 0.094, blue: 0.125))
    }

    private var scoreBox: some View {
        HStack(spacing: 11) {
            Text("得分").font(.subheadline.weight(.semibold)).foregroundColor(Color.gray.opacity(0.9))
            Text(String(format: "%05d", min(99999, game.score))).font(.system(size: 26, design: .monospaced).weight(.bold)).foregroundColor(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 13).frame(height: 54)
        .background(Color(red: 0.15, green: 0.22, blue: 0.28), in: RoundedRectangle(cornerRadius: 11))
    }

    private var previewBox: some View {
        HStack(spacing: 8) {
            Text("下一轮").font(.subheadline.weight(.semibold)).foregroundColor(Color.gray.opacity(0.9))
            ForEach(Array(game.preview.enumerated()), id: \.offset) { item in PieceView(type: item.element, size: 27) }
        }
        .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 10).frame(height: 54)
        .background(Color(red: 0.15, green: 0.22, blue: 0.28), in: RoundedRectangle(cornerRadius: 11))
    }

    private func board(size: CGFloat) -> some View {
        let columns = Array(repeating: GridItem(.flexible(), spacing: 0), count: GameModel.size)
        return LazyVGrid(columns: columns, spacing: 0) {
            ForEach(0..<GameModel.cellCount, id: \.self) { index in
                ZStack {
                    Rectangle().fill((index / 9 + index % 9).isMultiple(of: 2) ? Color(red: 0.87, green: 0.79, blue: 0.67) : Color(red: 0.80, green: 0.69, blue: 0.55))
                    Rectangle().stroke(Color(red: 0.42, green: 0.30, blue: 0.20), lineWidth: 1)
                    if game.board[index] != 0, game.movingIndex != index {
                        PieceView(type: game.board[index], size: size / 9 * 0.68)
                            .scaleEffect(game.spawnedIndices.contains(index) ? game.spawnScale : game.removedIndices.contains(index) ? game.removeScale : 1)
                    }
                    if game.movingIndex == index { PieceView(type: game.movingType, size: size / 9 * 0.68) }
                    if game.selectedIndex == index {
                        Rectangle().stroke(Color.red, lineWidth: 3).padding(3)
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture { game.tap(index: index) }
                .aspectRatio(1, contentMode: .fit)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(red: 0.42, green: 0.30, blue: 0.20), lineWidth: 7))
        .shadow(color: .black.opacity(0.22), radius: 8, y: 5)
    }
}

struct PieceView: View {
    let type: Int
    let size: CGFloat
    var body: some View {
        if type == GameModel.bomb {
            Text("💣").font(.system(size: size * 0.78)).frame(width: size, height: size)
        } else {
            Circle().fill(LinearGradient(colors: [color.opacity(0.92), color, color.opacity(0.62)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .overlay(Circle().stroke(type == GameModel.white ? Color.gray : color.opacity(0.4), lineWidth: 1.5))
                .overlay(Circle().fill(.white.opacity(0.75)).frame(width: size * 0.16, height: size * 0.16).offset(x: -size * 0.2, y: -size * 0.22))
                .frame(width: size, height: size)
                .shadow(color: .black.opacity(0.23), radius: 2, x: 1, y: 2)
        }
    }
    private var color: Color {
        switch type {
        case 1: return Color(red: 0.88, green: 0.02, blue: 0.02)
        case 2: return Color(red: 1, green: 0.79, blue: 0.02)
        case 3: return Color(red: 0.02, green: 0.64, blue: 0.40)
        case 4: return Color(red: 0.45, green: 0.03, blue: 0.72)
        case 5: return Color(red: 0.98, green: 0.02, blue: 0.55)
        case 6: return Color(red: 0.03, green: 0.67, blue: 0.82)
        case 7: return Color(white: 0.08)
        case 8: return Color(red: 0.02, green: 0.28, blue: 0.85)
        case GameModel.white: return .white
        default: return .gray
        }
    }
}

struct MenuView: View {
    @EnvironmentObject private var game: GameModel
    var body: some View {
        NavigationStack {
            List {
                Button("新游戏") { game.reset(); game.showMenu = false }
                Button("高分榜") { game.showMenu = false; game.showHighScores = true }
                Button("设置") { game.showMenu = false; game.showSettings = true }
            }
            .navigationTitle("菜单")
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("关闭") { game.showMenu = false } } }
        }
    }
}

struct HighScoresView: View {
    @EnvironmentObject private var game: GameModel
    var body: some View {
        NavigationStack {
            List(Array(game.highScores.enumerated()), id: \.element.id) { offset, entry in
                HStack { Text("\(offset + 1). \(entry.name)"); Spacer(); Text("\(entry.score)").monospacedDigit() }
            }
            .navigationTitle("高分榜（前10名）")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("关闭") { game.showHighScores = false } } }
        }
    }
}

struct GameOverView: View {
    @EnvironmentObject private var game: GameModel
    @State private var name = ""
    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Text("游戏结束").font(.largeTitle.bold())
                Text("最终得分：\(game.score)").font(.title3)
                TextField("输入昵称", text: $name).textFieldStyle(.roundedBorder).padding(.horizontal)
                Button("保存并开始新游戏") { game.saveScore(name: name); game.reset() }.buttonStyle(.borderedProminent)
            }
            .padding()
            .navigationTitle("本局结果")
        }
        .interactiveDismissDisabled()
    }
}

struct SettingsView: View {
    @EnvironmentObject private var game: GameModel
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            Form {
                Section("概率与难度") {
                    Slider(value: $game.difficulty, in: 1...2, step: 0.1) { Text("难度系数") }
                    Text("难度系数：\(game.difficulty, specifier: "%.1f")")
                    Slider(value: $game.whiteProbability, in: 0...2, step: 0.1) { Text("白棋概率系数") }
                    Text("白棋概率系数：\(game.whiteProbability, specifier: "%.1f")")
                    Slider(value: $game.bombProbability, in: 0...2, step: 0.1) { Text("炸药概率系数") }
                    Text("炸药概率系数：\(game.bombProbability, specifier: "%.1f")")
                }
                Section("移动速度") {
                    Picker("速度", selection: $game.movementSpeed) { ForEach(0..<4) { Text(game.speedLabel($0)).tag($0) } }.pickerStyle(.segmented)
                }
                Section("声音") {
                    Toggle("音乐", isOn: $game.musicEnabled)
                    Slider(value: $game.musicVolume, in: 0...1) { Text("音乐音量") }
                    Toggle("音效", isOn: $game.soundEnabled)
                    Slider(value: $game.soundVolume, in: 0...1) { Text("音效音量") }
                }
            }
            .navigationTitle("设置")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("保存") { game.applySettings(); dismiss() } } }
        }
    }
}
