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
            .overlay {
                if let prompt = game.easterPrompt {
                    EasterPromptOverlay(
                        prompt: prompt,
                        onSecretTap: { game.tapEasterSecret($0) },
                        onOK: { game.closeEasterPrompt() }
                    )
                }
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
                        TileView(tile: tile, compact: true, kuromiTheme: game.kuromiTheme)
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
            ZStack {
                if game.kuromiTheme {
                    KuromiHeadView()
                        .frame(width: size * 0.42, height: size * 0.42)
                        .opacity(0.20)
                        .allowsHitTesting(false)
                }
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 3), count: 9), spacing: 3) {
                    ForEach(0..<81, id: \.self) { index in
                        let trailIndex = game.racerTrail.firstIndex(of: index)
                        BoardCell(
                            tile: game.board[index],
                            selected: game.selectedIndex == index,
                            removing: game.removing.contains(index),
                            trailTile: game.racerTrailTile,
                            trailIndex: trailIndex,
                            kuromiTheme: game.kuromiTheme
                        )
                        .aspectRatio(1, contentMode: .fit)
                        .contentShape(Rectangle())
                        .onTapGesture { game.tap(index) }
                    }
                }
                if let loading = game.easterLoading {
                    LoadingDotsView(loading: loading)
                        .frame(width: size * 0.86, height: size * 0.28)
                        .allowsHitTesting(false)
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
    let trailTile: CloudGameModel.Tile?
    let trailIndex: Int?
    let kuromiTheme: Bool

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 4)
                .fill(Color(red: 0.57, green: 0.39, blue: 0.22).opacity(kuromiTheme ? 0.78 : 1.0))
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.black.opacity(0.22), lineWidth: 1))
            if let trailTile, let trailIndex {
                TileTrailView(tile: trailTile, index: trailIndex)
            }
            if let tile {
                TileView(tile: tile, compact: false, kuromiTheme: kuromiTheme)
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
    let kuromiTheme: Bool

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
            if kuromiTheme, tile != .bomb {
                KuromiHeadView()
                    .padding(compact ? 3 : 5)
            }
        }
        .padding(compact ? 2 : 3)
    }
}

private struct TileTrailView: View {
    let tile: CloudGameModel.Tile
    let index: Int

    var body: some View {
        Circle()
            .fill(
                RadialGradient(
                    colors: [tile.color.opacity(0.80), tile.color.opacity(0.16), .clear],
                    center: .center,
                    startRadius: 1,
                    endRadius: 28
                )
            )
            .padding(CGFloat(5 + index * 2))
            .opacity(max(0.16, 0.58 - Double(index) * 0.07))
            .blur(radius: CGFloat(index) * 0.45)
    }
}

private struct KuromiHeadView: View {
    var body: some View {
        GeometryReader { proxy in
            let size = min(proxy.size.width, proxy.size.height)
            let center = CGPoint(x: proxy.size.width / 2, y: proxy.size.height / 2 + size * 0.03)
            ZStack {
                Path { path in
                    path.move(to: CGPoint(x: center.x - size * 0.20, y: center.y - size * 0.25))
                    path.addCurve(
                        to: CGPoint(x: center.x - size * 0.03, y: center.y - size * 0.31),
                        control1: CGPoint(x: center.x - size * 0.48, y: center.y - size * 0.78),
                        control2: CGPoint(x: center.x - size * 0.31, y: center.y - size * 0.88)
                    )
                    path.addLine(to: CGPoint(x: center.x - size * 0.20, y: center.y - size * 0.25))

                    path.move(to: CGPoint(x: center.x + size * 0.20, y: center.y - size * 0.25))
                    path.addCurve(
                        to: CGPoint(x: center.x + size * 0.03, y: center.y - size * 0.31),
                        control1: CGPoint(x: center.x + size * 0.48, y: center.y - size * 0.78),
                        control2: CGPoint(x: center.x + size * 0.31, y: center.y - size * 0.88)
                    )
                    path.addLine(to: CGPoint(x: center.x + size * 0.20, y: center.y - size * 0.25))
                }
                .fill(Color(red: 0.07, green: 0.07, blue: 0.09))

                Circle()
                    .fill(Color(red: 0.07, green: 0.07, blue: 0.09))
                    .frame(width: size * 0.68, height: size * 0.68)
                    .position(center)

                Ellipse()
                    .fill(Color(red: 0.97, green: 0.94, blue: 0.90))
                    .frame(width: size * 0.47, height: size * 0.36)
                    .position(x: center.x, y: center.y + size * 0.06)

                HStack(spacing: size * 0.13) {
                    Circle().fill(Color(red: 0.05, green: 0.05, blue: 0.06))
                    Circle().fill(Color(red: 0.05, green: 0.05, blue: 0.06))
                }
                .frame(width: size * 0.26, height: size * 0.045)
                .position(x: center.x, y: center.y + size * 0.02)

                Circle()
                    .fill(Color(red: 1.0, green: 0.31, blue: 0.69))
                    .frame(width: size * 0.13, height: size * 0.13)
                    .position(x: center.x, y: center.y - size * 0.22)

                Circle()
                    .fill(Color(red: 0.05, green: 0.05, blue: 0.06))
                    .frame(width: size * 0.025, height: size * 0.025)
                    .position(x: center.x - size * 0.025, y: center.y - size * 0.225)
                Circle()
                    .fill(Color(red: 0.05, green: 0.05, blue: 0.06))
                    .frame(width: size * 0.025, height: size * 0.025)
                    .position(x: center.x + size * 0.025, y: center.y - size * 0.225)
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }
}

private struct LoadingDotsView: View {
    let loading: CloudGameModel.EasterLoading

    private struct Dot: Identifiable {
        let id: Int
        let x: Int
        let y: Int
    }

    var body: some View {
        GeometryReader { proxy in
            let layout = Self.layout(for: loading.message)
            let step = min(
                proxy.size.width / CGFloat(max(1, layout.columns)),
                proxy.size.height / 5.0
            )
            let dotSize = max(3.0, step * 0.58)
            let startX = (proxy.size.width - CGFloat(layout.columns - 1) * step) / 2.0
            let startY = (proxy.size.height - 4.0 * step) / 2.0
            ForEach(layout.dots) { dot in
                Circle()
                    .fill(Self.colors[(dot.id + loading.colorPhase) % Self.colors.count])
                    .frame(width: dotSize, height: dotSize)
                    .shadow(color: Self.colors[(dot.id + loading.colorPhase) % Self.colors.count].opacity(0.45), radius: 4)
                    .position(
                        x: startX + CGFloat(dot.x) * step,
                        y: startY + CGFloat(dot.y) * step
                    )
                    .opacity(loading.visible ? 1.0 : 0.24)
            }
        }
    }

    private static let colors: [Color] = [
        .red,
        .yellow,
        .green,
        .blue,
        .purple,
        .cyan,
        .pink,
        .orange
    ]

    private static func layout(for message: String) -> (dots: [Dot], columns: Int) {
        var dots: [Dot] = []
        var cursor = 0
        var id = 0
        for character in message {
            let glyph = glyph(for: character)
            for row in 0..<glyph.count {
                let values = Array(glyph[row])
                for column in 0..<values.count where values[column] == "#" {
                    dots.append(Dot(id: id, x: cursor + column, y: row))
                    id += 1
                }
            }
            cursor += (glyph.first?.count ?? 0) + 1
        }
        return (dots, max(1, cursor - 1))
    }

    private static func glyph(for character: Character) -> [String] {
        switch character.lowercased() {
        case "a": return [" # ", "# #", "###", "# #", "# #"]
        case "d": return ["## ", "# #", "# #", "# #", "## "]
        case "g": return [" ##", "#  ", "# #", "# #", " ##"]
        case "i": return ["###", " # ", " # ", " # ", "###"]
        case "l": return ["#  ", "#  ", "#  ", "#  ", "###"]
        case "m": return ["# #", "###", "###", "# #", "# #"]
        case "n": return ["## ", "# #", "# #", "# #", "# #"]
        case "o": return [" # ", "# #", "# #", "# #", " # "]
        case "r": return ["## ", "# #", "## ", "# #", "# #"]
        case "y": return ["# #", "# #", " # ", " # ", " # "]
        case ".": return [" ", " ", " ", " ", "#"]
        default: return ["  ", "  ", "  ", "  ", "  "]
        }
    }
}

private struct EasterPromptOverlay: View {
    let prompt: CloudGameModel.EasterPrompt
    let onSecretTap: (Character) -> Void
    let onOK: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.34)
                .ignoresSafeArea()
            VStack(spacing: 18) {
                Text(prompt.title)
                    .font(.headline)
                    .foregroundStyle(prompt.accent)
                HStack(spacing: 0) {
                    ForEach(Array("Are you ready?".enumerated()), id: \.offset) { _, character in
                        if character == prompt.secret {
                            Button {
                                onSecretTap(character)
                            } label: {
                                Text(String(character))
                                    .font(.system(size: 30, weight: .bold, design: .rounded))
                                    .foregroundStyle(prompt.accent)
                                    .frame(minWidth: 15)
                            }
                            .buttonStyle(.plain)
                        } else {
                            Text(String(character))
                                .font(.system(size: 30, weight: .bold, design: .rounded))
                                .frame(minWidth: character == " " ? 10 : 15)
                        }
                    }
                }
                Button("ok", action: onOK)
                    .buttonStyle(.borderedProminent)
            }
            .padding(24)
            .frame(maxWidth: 320)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18))
            .shadow(color: .black.opacity(0.22), radius: 20, y: 10)
        }
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
                    Picker("\u{97F3}\u{4E50}\u{66F2}\u{76EE}", selection: $game.musicTrack) {
                        ForEach(CloudGameModel.MusicTrack.allCases) { track in
                            Text(track.title).tag(track)
                        }
                    }
                    .pickerStyle(.segmented)
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
