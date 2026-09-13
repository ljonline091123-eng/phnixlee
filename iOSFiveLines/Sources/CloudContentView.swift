import SwiftUI

struct CloudContentView: View {
    @EnvironmentObject private var game: CloudGameModel
    @State private var menuPresented = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                header
                board
            }
            .padding(12)
            .background(
                LinearGradient(
                    colors: [Color(red: 0.96, green: 0.94, blue: 0.88), Color(red: 0.82, green: 0.78, blue: 0.68)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
            )
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        menuPresented = true
                    } label: {
                        Image(systemName: "line.3.horizontal")
                    }
                    .accessibilityLabel("菜单")
                }
            }
            .sheet(isPresented: $menuPresented) {
                GameMenuView()
                    .environmentObject(game)
            }
            .sheet(isPresented: $game.isGameOver) {
                GameOverView()
                    .environmentObject(game)
                    .presentationDetents([.medium])
            }
        }
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 3) {
                Text("得分")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                Text("\(game.score)")
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .monospacedDigit()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(.white.opacity(0.82), in: RoundedRectangle(cornerRadius: 12))

            VStack(alignment: .trailing, spacing: 5) {
                Text("下一轮")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                HStack(spacing: 5) {
                    ForEach(Array(game.nextTiles.enumerated()), id: \.offset) { _, tile in
                        TileView(tile: tile, compact: true)
                    }
                }
            }
            .padding(10)
            .background(.white.opacity(0.82), in: RoundedRectangle(cornerRadius: 12))
        }
    }

    private var board: some View {
        GeometryReader { proxy in
            let size = min(proxy.size.width, proxy.size.height)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 3), count: 9), spacing: 3) {
                ForEach(0..<81, id: \.self) { index in
                    BoardCell(
                        tile: game.board[index],
                        selected: game.selectedIndex == index,
                        removing: game.removing.contains(index)
                    )
                    .aspectRatio(1, contentMode: .fit)
                    .contentShape(Rectangle())
                    .onTapGesture { game.tap(index) }
                }
            }
            .frame(width: size, height: size)
            .padding(7)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(red: 0.29, green: 0.18, blue: 0.10))
                    .shadow(color: .black.opacity(0.22), radius: 10, y: 6)
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        }
        .aspectRatio(1, contentMode: .fit)
    }
}

private struct BoardCell: View {
    let tile: CloudGameModel.Tile?
    let selected: Bool
    let removing: Bool

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 4)
                .fill(Color(red: 0.57, green: 0.39, blue: 0.22))
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.black.opacity(0.22), lineWidth: 1))
            if let tile {
                TileView(tile: tile, compact: false)
                    .scaleEffect(removing ? 0.08 : 1)
                    .opacity(removing ? 0 : 1)
                    .transition(.scale)
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: 5)
                .stroke(selected ? Color.white : Color.clear, lineWidth: 3)
        )
        .animation(.easeInOut(duration: 0.5), value: removing)
    }
}

private struct TileView: View {
    let tile: CloudGameModel.Tile
    let compact: Bool

    var body: some View {
        ZStack {
            Circle()
                .fill(tile.color)
                .overlay(
                    Circle()
                        .stroke(tile == .white ? Color.gray.opacity(0.45) : Color.white.opacity(0.6), lineWidth: compact ? 1 : 2)
                )
                .shadow(color: .black.opacity(0.25), radius: compact ? 2 : 4, y: 2)
            if tile == .bomb {
                Image(systemName: "burst.fill")
                    .font(.system(size: compact ? 11 : 16, weight: .black))
                    .foregroundStyle(.white)
            }
        }
        .padding(compact ? 2 : 3)
    }
}

private struct GameMenuView: View {
    @EnvironmentObject private var game: CloudGameModel
    @Environment(\.dismiss) private var dismiss
    @State private var scoresPresented = false
    @State private var settingsPresented = false

    var body: some View {
        NavigationStack {
            List {
                Button {
                    game.startNewGame()
                    dismiss()
                } label: {
                    Label("新游戏", systemImage: "arrow.clockwise")
                }
                Button {
                    scoresPresented = true
                } label: {
                    Label("高分榜", systemImage: "trophy")
                }
                Button {
                    settingsPresented = true
                } label: {
                    Label("设置", systemImage: "gearshape")
                }
            }
            .navigationTitle("菜单")
            .sheet(isPresented: $scoresPresented) {
                HighScoresView()
                    .environmentObject(game)
            }
            .sheet(isPresented: $settingsPresented) {
                SettingsView()
                    .environmentObject(game)
            }
        }
        .presentationDetents([.medium])
    }
}

private struct HighScoresView: View {
    @EnvironmentObject private var game: CloudGameModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(Array(game.highScores.enumerated()), id: \.offset) { index, entry in
                HStack {
                    Text("\(index + 1)")
                        .frame(width: 28, alignment: .leading)
                        .foregroundStyle(.secondary)
                    Text(entry.name)
                    Spacer()
                    Text("\(entry.score)")
                        .fontWeight(.bold)
                }
            }
            .overlay {
                if game.highScores.isEmpty {
                    VStack(spacing: 8) {
                        Image(systemName: "trophy")
                            .font(.title2)
                            .foregroundStyle(.secondary)
                        Text("暂无记录")
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("高分榜")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("完成") { dismiss() }
                }
            }
        }
    }
}

private struct SettingsView: View {
    @EnvironmentObject private var game: CloudGameModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("概率与难度") {
                    settingSlider("难度系数", value: $game.difficulty, range: 1...2, step: 0.1)
                    settingSlider("白棋概率系数", value: $game.whiteProbability, range: 0...2, step: 0.1)
                    settingSlider("炸药概率系数", value: $game.bombProbability, range: 0...2, step: 0.1)
                }
                Section("速度") {
                    Picker("移动速度", selection: $game.moveSpeed) {
                        ForEach(CloudGameModel.MoveSpeed.allCases) { speed in
                            Text(speed.title).tag(speed)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                Section("声音") {
                    Toggle("音乐", isOn: $game.musicEnabled)
                    Slider(value: $game.musicVolume, in: 0...1) {
                        Text("音乐音量")
                    }
                    Toggle("音效", isOn: $game.effectsEnabled)
                    Slider(value: $game.effectsVolume, in: 0...1) {
                        Text("音效音量")
                    }
                }
            }
            .navigationTitle("设置")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("完成") { dismiss() }
                }
            }
        }
    }

    private func settingSlider(
        _ title: String,
        value: Binding<Double>,
        range: ClosedRange<Double>,
        step: Double
    ) -> some View {
        VStack(alignment: .leading) {
            HStack {
                Text(title)
                Spacer()
                Text(value.wrappedValue, format: .number.precision(.fractionLength(1)))
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
            }
            Slider(value: value, in: range, step: step)
        }
    }
}

private struct GameOverView: View {
    @EnvironmentObject private var game: CloudGameModel
    @Environment(\.dismiss) private var dismiss
    @State private var nickname = ""

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "flag.checkered")
                .font(.system(size: 44))
                .foregroundStyle(.orange)
            Text("游戏结束")
                .font(.title.bold())
            Text("本局得分：\(game.score)")
                .font(.headline)
            TextField("昵称", text: $nickname)
                .textFieldStyle(.roundedBorder)
                .padding(.horizontal)
            Button("保存成绩并开始新游戏") {
                game.submitScore(nickname)
                dismiss()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(24)
    }
}
