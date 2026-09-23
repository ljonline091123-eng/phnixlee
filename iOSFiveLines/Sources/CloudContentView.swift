import SwiftUI
import Foundation

struct CloudContentView: View {
    @EnvironmentObject private var game: CloudGameModel
    @State private var menuPresented = false
    @State private var scoresPresented = false
    @State private var settingsPresented = false
    @State private var historyPresented = false
    @State private var achievementsPresented = false
    @State private var collectionPresented = false

    var body: some View {
        VStack(spacing: 0) {
            header
                .frame(height: 156)
            GeometryReader { proxy in
                let size = max(0, min(proxy.size.width - 32, proxy.size.height - 32) - 14)
                board(size: size)
                    .position(x: proxy.size.width / 2, y: (size + 14) / 2 + 16)
            }
        }
        .background(Color(red: 0.945, green: 0.918, blue: 0.875).ignoresSafeArea(edges: [.horizontal, .bottom]))
        .background(Color(red: 0.063, green: 0.094, blue: 0.125).ignoresSafeArea())
        .confirmationDialog("菜单", isPresented: $menuPresented, titleVisibility: .visible) {
            Button("新游戏") { game.startNewGame() }
            Button("撤销一步") { game.undoLastMove() }
                .disabled(!game.undoAvailable)
            Button("新手引导") { game.beginTutorial() }
            Button("高分榜") { scoresPresented = true }
            Button("历史战绩") { historyPresented = true }
            Button("成就") { achievementsPresented = true }
            Button("收藏与挑战") { collectionPresented = true }
            Button("设置") {
                game.beginSettingsSession()
                settingsPresented = true
            }
            Button("取消", role: .cancel) {}
        }
        .sheet(isPresented: $scoresPresented) {
            HighScoresView()
                .environmentObject(game)
        }
        .sheet(isPresented: $settingsPresented) {
            SettingsView()
                .environmentObject(game)
        }
        .sheet(isPresented: $historyPresented) {
            HistoryView()
                .environmentObject(game)
        }
        .sheet(isPresented: $achievementsPresented) {
            AchievementsView()
                .environmentObject(game)
        }
        .sheet(isPresented: $collectionPresented) {
            CollectionAndChallengesView()
                .environmentObject(game)
        }
        .sheet(isPresented: $game.isGameOver) {
            GameOverView()
                .environmentObject(game)
                .presentationDetents([.medium])
                .interactiveDismissDisabled()
        }
        .overlay {
            if let prompt = game.easterPrompt {
                EasterPromptOverlay(
                    prompt: prompt,
                    onSecretTap: { game.tapEasterSecret($0) },
                    onOK: { game.closeEasterPrompt() }
                )
            }
            if let step = game.tutorialStep, step >= 2 {
                HeaderTutorialCoach(step: step) {
                    if step == 2 {
                        game.advanceTutorial()
                    } else {
                        game.finishTutorial()
                        menuPresented = true
                    }
                }
            }
            if let message = game.achievementToast {
                VStack {
                    Text(message)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color(red: 0.063, green: 0.094, blue: 0.125), in: Capsule())
                        .padding(.top, 8)
                    Spacer()
                }
                .allowsHitTesting(false)
            }
        }
    }

    private var header: some View {
        VStack(spacing: 8) {
            HStack(alignment: .top, spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(game.dailyChallenge ? "每日挑战" : "五子消除")
                        .font(.system(size: 30, weight: .bold))
                        .foregroundStyle(.white)
                    Text(game.dailyChallenge
                        ? "今日最佳 \(String(format: "%05d", game.dailyBestScore)) · 每 12 步提升生成压力"
                        : "白色万能球 · 炸药+至少四颗同色球清除全盘")
                        .font(.system(size: 12))
                        .foregroundStyle(.white.opacity(0.82))
                        .lineLimit(1)
                        .minimumScaleFactor(0.72)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Button {
                    game.playClick()
                    menuPresented = true
                } label: {
                    Text("菜单")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 94, height: 50)
                        .background(Color(red: 0.937, green: 0.325, blue: 0.314), in: RoundedRectangle(cornerRadius: 15))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("菜单")
            }

            HStack(spacing: 14) {
                scoreCard
                previewCard
            }
            .frame(height: 66)
        }
        .padding(.horizontal, 20)
        .padding(.top, 10)
        .padding(.bottom, 12)
        .background(Color(red: 0.063, green: 0.094, blue: 0.125))
    }

    private var scoreCard: some View {
        HStack(spacing: 8) {
            Text("得分")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color(red: 0.79, green: 0.84, blue: 0.87))
            Text(String(format: "%05d", min(99_999, game.score)))
                .font(.system(size: 24, weight: .bold, design: .monospaced))
                .foregroundStyle(.white)
                .monospacedDigit()
                .minimumScaleFactor(0.72)
        }
        .padding(.horizontal, 10)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(red: 0.149, green: 0.216, blue: 0.275))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(red: 0.376, green: 0.49, blue: 0.545), lineWidth: 1))
        )
    }

    private var previewCard: some View {
        HStack(spacing: 5) {
            Text("下一轮")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color(red: 0.79, green: 0.84, blue: 0.87))
                .fixedSize()
            HStack(spacing: 3) {
                ForEach(Array(game.nextTiles.enumerated()), id: \.offset) { _, tile in
                    TileView(
                        tile: tile,
                        compact: true,
                        rayRacerMode: game.rayRacerMode,
                        kuromiTheme: game.kuromiTheme,
                        heartMode: game.heartMode
                    )
                    .frame(width: 30, height: 30)
                }
            }
        }
        .padding(.horizontal, 8)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(red: 0.149, green: 0.216, blue: 0.275))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(red: 0.376, green: 0.49, blue: 0.545), lineWidth: 1))
        )
    }

    private func board(size: CGFloat) -> some View {
        ZStack {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: 9), spacing: 0) {
                ForEach(0..<81, id: \.self) { index in
                    let trailIndex = game.racerTrail.firstIndex(of: index)
                    BoardCell(
                        index: index,
                        tile: game.board[index],
                        selected: game.selectedIndex == index,
                        removing: game.removing.contains(index),
                        trailTile: game.racerTrailTile,
                        trailIndex: trailIndex,
                        rayRacerMode: game.rayRacerMode,
                        kuromiTheme: game.kuromiTheme,
                        heartMode: game.heartMode,
                        heartBurst: game.heartMode && game.explodingBombs.contains(index)
                    )
                    .aspectRatio(1, contentMode: .fit)
                    .contentShape(Rectangle())
                    .onTapGesture { game.tap(index) }
                }
            }
            if let loading = game.easterLoading {
                LoadingDotsView(loading: loading)
                    .frame(width: size * 0.92, height: size * 0.42)
                    .allowsHitTesting(false)
            }
            if let step = game.tutorialStep, step < 2,
               let index = step == 0 ? game.tutorialSource : game.tutorialTarget {
                BoardTutorialCoach(step: step, index: index, boardSize: size)
                    .allowsHitTesting(false)
            }
        }
        .frame(width: size, height: size)
        .padding(7)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color(red: 0.447, green: 0.329, blue: 0.243))
        )
    }
}

private struct CollectionAndChallengesView: View {
    @EnvironmentObject private var game: CloudGameModel
    @State private var profileNameInput = ""
    var body: some View {
        NavigationStack {
            List {
                Section("本地档案") {
                    LabeledContent("显示名", value: game.profileName)
                    LabeledContent("玩家 ID", value: game.profileID)
                    LabeledContent("联网状态", value: "尚未开放联网")
                    HStack {
                        TextField("修改本地显示名", text: $profileNameInput)
                            .textInputAutocapitalization(.never)
                        Button("保存") { game.updateProfileName(profileNameInput) }
                            .disabled(profileNameInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                    LabeledContent("当前等级", value: game.currentMedal.name)
                    LabeledContent("成长分", value: "\(game.growthPoints)")
                    if let next = game.nextMedal {
                        LabeledContent("下一阶", value: "\(next.name) · 还需 \(max(0, next.threshold - game.growthPoints)) 分")
                    } else {
                        LabeledContent("下一阶", value: "已达到最高等级")
                    }
                    Text("本地玩家档案 · 尚未连接服务端")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Section("每日残局图鉴 · 首期 30 关") {
                    Text("以下布局与目标为未验证草稿，暂不可游玩。完成解法验证前，不启用通关判定与首通奖励。")
                        .foregroundStyle(Color.secondary)
                    ForEach(0..<30, id: \.self) { index in
                        let puzzle = CloudGameModel.dailyPuzzle(forDay: index + 1)
                        VStack(alignment: .leading, spacing: 3) {
                            Text("\(index + 1). \(puzzle.title)")
                            Text("\(puzzle.summary) · 目标：\(puzzle.target)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                Section("奖牌与卡册 · \(game.unlockedCardIDs.count)/54") {
                    ForEach(CloudGameModel.medals) { medal in
                        HStack {
                            Image(systemName: game.growthPoints >= medal.threshold ? "medal.fill" : "medal")
                                .foregroundStyle(game.growthPoints >= medal.threshold ? Color.orange : Color.secondary)
                            Text(medal.name)
                            Spacer()
                            Text("\(medal.threshold) 分")
                                .foregroundStyle(.secondary)
                        }
                    }
                    ForEach(CloudGameModel.abilityCards) { card in
                        VStack(alignment: .leading, spacing: 3) {
                            HStack {
                                Image(systemName: game.unlockedCardIDs.contains(card.id) ? "checkmark.circle.fill" : "lock.fill")
                                    .foregroundStyle(game.unlockedCardIDs.contains(card.id) ? Color.green : Color.secondary)
                            Text(card.name)
                                Spacer()
                                Text(card.group)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Text(game.unlockedCardIDs.contains(card.id) ? "已解锁 · 收藏记录（装备效果尚未开放）" : card.condition)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            if let unlockedAt = game.cardUnlockDates[card.id] {
                                Text("获得时间：\(unlockedAt.formatted(date: .abbreviated, time: .omitted))")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("收藏与挑战")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

private struct BoardTutorialCoach: View {
    let step: Int
    let index: Int
    let boardSize: CGFloat

    var body: some View {
        let cell = boardSize / 9
        let column = CGFloat(index % 9)
        let row = CGFloat(index / 9)
        let target = CGRect(x: column * cell + 2, y: row * cell + 2, width: cell - 4, height: cell - 4)
        ZStack(alignment: .topLeading) {
            Canvas { context, size in
                var mask = Path()
                mask.addRect(CGRect(origin: .zero, size: size))
                mask.addRoundedRect(in: target.insetBy(dx: -3, dy: -3), cornerSize: CGSize(width: 8, height: 8))
                context.fill(mask, with: .color(.black.opacity(0.62)), style: FillStyle(eoFill: true))
                var border = Path()
                border.addRoundedRect(in: target.insetBy(dx: -3, dy: -3), cornerSize: CGSize(width: 8, height: 8))
                context.stroke(border, with: .color(.yellow), lineWidth: 3)
            }
            Text(step == 0 ? "第 1 步：点击高亮棋子" : "第 2 步：点击高亮空格完成移动")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(Color(red: 0.063, green: 0.094, blue: 0.125), in: RoundedRectangle(cornerRadius: 8))
                .frame(maxWidth: boardSize - 24)
                .position(x: boardSize / 2, y: row < 4 ? boardSize - 30 : 30)
        }
        .frame(width: boardSize, height: boardSize)
    }
}

private struct HeaderTutorialCoach: View {
    let step: Int
    let action: () -> Void

    var body: some View {
        GeometryReader { proxy in
            let target = step == 2
                ? CGRect(x: proxy.size.width * 0.50, y: 76, width: proxy.size.width * 0.45 - 20, height: 68)
                : CGRect(x: proxy.size.width - 118, y: 8, width: 100, height: 56)
            ZStack(alignment: .top) {
                Canvas { context, size in
                    var mask = Path()
                    mask.addRect(CGRect(origin: .zero, size: size))
                    mask.addRoundedRect(in: target, cornerSize: CGSize(width: 14, height: 14))
                    context.fill(mask, with: .color(.black.opacity(0.62)), style: FillStyle(eoFill: true))
                    var border = Path()
                    border.addRoundedRect(in: target, cornerSize: CGSize(width: 14, height: 14))
                    context.stroke(border, with: .color(.yellow), lineWidth: 3)
                }
                VStack(spacing: 10) {
                    Text(step == 2
                        ? "第 3 步：这里显示下一轮棋子"
                        : "第 4 步：从菜单进入每日挑战、撤销和成就")
                        .font(.headline)
                        .multilineTextAlignment(.center)
                    Button(step == 2 ? "下一步" : "打开菜单", action: action)
                        .buttonStyle(.borderedProminent)
                }
                .foregroundStyle(.white)
                .padding(14)
                .frame(maxWidth: min(340, proxy.size.width - 32))
                .background(Color(red: 0.063, green: 0.094, blue: 0.125), in: RoundedRectangle(cornerRadius: 10))
                .padding(.top, 174)
            }
        }
        .ignoresSafeArea(edges: .bottom)
    }
}

private struct BoardCell: View {
    let index: Int
    let tile: CloudGameModel.Tile?
    let selected: Bool
    let removing: Bool
    let trailTile: CloudGameModel.Tile?
    let trailIndex: Int?
    let rayRacerMode: Bool
    let kuromiTheme: Bool
    let heartMode: Bool
    let heartBurst: Bool

    var body: some View {
        ZStack {
            Rectangle()
                .fill(index / 9 % 2 == index % 9 % 2
                    ? Color(red: 0.875, green: 0.788, blue: 0.667)
                    : Color(red: 0.812, green: 0.702, blue: 0.557))
                .overlay(Rectangle().stroke(Color(red: 0.463, green: 0.345, blue: 0.247), lineWidth: 0.8))
            if let trailTile, let trailIndex {
                TileTrailView(tile: trailTile, index: trailIndex, heartMode: heartMode)
            }
            if let tile {
                TileView(tile: tile, compact: false, rayRacerMode: rayRacerMode, kuromiTheme: kuromiTheme, heartMode: heartMode)
                    .scaleEffect(removing ? 0.08 : 1)
                    .opacity(removing ? 0 : 1)
                    .transition(.scale)
            }
            if heartBurst {
                HeartBurstView()
            }
        }
        .overlay(
            Rectangle()
                .inset(by: 3)
                .stroke(selected ? Color(red: 0.863, green: 0.149, blue: 0.149) : Color.clear, lineWidth: 3)
        )
        .animation(.easeInOut(duration: 0.5), value: removing)
    }
}

private struct TileView: View {
    let tile: CloudGameModel.Tile
    let compact: Bool
    let rayRacerMode: Bool
    let kuromiTheme: Bool
    let heartMode: Bool

    var body: some View {
        ZStack {
            if tile == .bomb {
                DynamiteTileView(compact: compact)
            } else if heartMode {
                HeartShape()
                    .fill(tile.color)
                    .overlay(
                        HeartShape()
                            .stroke(tile == .white ? Color.gray.opacity(0.55) : Color.white.opacity(0.6), lineWidth: compact ? 1 : 2)
                    )
                    .shadow(color: .black.opacity(0.25), radius: compact ? 2 : 4, y: 2)
                    .overlay(alignment: .topLeading) {
                        Circle()
                            .fill(.white.opacity(0.76))
                            .frame(width: compact ? 5 : 8, height: compact ? 5 : 8)
                            .padding(compact ? 7 : 10)
                    }
            } else if kuromiTheme {
                KuromiTileView(tile: tile, compact: compact)
            } else if rayRacerMode {
                CarLogoTileView(tile: tile, compact: compact)
            } else {
                standardBall
            }
        }
        .padding(compact ? 2 : 3)
    }

    private var standardBall: some View {
        Circle()
            .fill(tile.color)
            .overlay(
                Circle().fill(
                    RadialGradient(
                        colors: [.white.opacity(0.42), .clear, .black.opacity(0.48)],
                        center: UnitPoint(x: 0.34, y: 0.30),
                        startRadius: 1,
                        endRadius: compact ? 24 : 42
                    )
                )
            )
            .overlay(
                Circle().stroke(
                    tile == .white ? Color(red: 0.482, green: 0.529, blue: 0.58) : Color.black.opacity(0.48),
                    lineWidth: compact ? 1 : 1.7
                )
            )
            .shadow(color: .black.opacity(0.25), radius: compact ? 2 : 4, y: 2)
            .overlay(alignment: .topLeading) {
                Circle()
                    .fill(.white.opacity(0.72))
                    .frame(width: compact ? 4 : 7, height: compact ? 4 : 7)
                    .padding(compact ? 7 : 10)
            }
    }
}

private struct DynamiteTileView: View {
    let compact: Bool

    var body: some View {
        GeometryReader { proxy in
            let size = min(proxy.size.width, proxy.size.height)
            let bodyWidth = size * 0.34
            let bodyHeight = size * 0.74
            ZStack {
                RoundedRectangle(cornerRadius: size * 0.07)
                    .fill(.black.opacity(0.28))
                    .frame(width: bodyWidth, height: bodyHeight)
                    .offset(x: size * 0.05, y: size * 0.06)
                RoundedRectangle(cornerRadius: size * 0.07)
                    .fill(Color(red: 0.718, green: 0.11, blue: 0.11))
                    .frame(width: bodyWidth, height: bodyHeight)
                    .overlay(alignment: .leading) {
                        RoundedRectangle(cornerRadius: size * 0.025)
                            .fill(Color(red: 0.937, green: 0.325, blue: 0.314))
                            .frame(width: size * 0.06)
                            .padding(.vertical, size * 0.06)
                            .padding(.leading, size * 0.04)
                    }
                    .overlay {
                        VStack {
                            Rectangle().fill(Color(red: 0.427, green: 0.082, blue: 0.082)).frame(height: size * 0.055)
                            Spacer()
                            Rectangle().fill(Color(red: 0.427, green: 0.082, blue: 0.082)).frame(height: size * 0.055)
                        }
                        .padding(.vertical, size * 0.15)
                    }
                Rectangle()
                    .fill(Color(red: 0.306, green: 0.204, blue: 0.18))
                    .frame(width: size * 0.11, height: size * 0.12)
                    .offset(y: -bodyHeight * 0.56)
                Path { path in
                    path.move(to: CGPoint(x: size * 0.51, y: size * 0.12))
                    path.addCurve(
                        to: CGPoint(x: size * 0.74, y: size * 0.08),
                        control1: CGPoint(x: size * 0.61, y: -size * 0.01),
                        control2: CGPoint(x: size * 0.69, y: size * 0.14)
                    )
                }
                .stroke(Color(red: 0.306, green: 0.204, blue: 0.18), style: StrokeStyle(lineWidth: max(1.5, size * 0.045), lineCap: .round))
                Circle()
                    .fill(Color(red: 1.0, green: 0.757, blue: 0.027))
                    .frame(width: size * 0.13, height: size * 0.13)
                    .overlay(Circle().fill(Color(red: 1.0, green: 0.961, blue: 0.616)).frame(width: size * 0.05, height: size * 0.05))
                    .position(x: size * 0.75, y: size * 0.07)
            }
            .frame(width: size, height: size)
            .rotationEffect(.degrees(-16))
            .position(x: proxy.size.width / 2, y: proxy.size.height / 2)
        }
        .padding(compact ? 1 : 2)
        .allowsHitTesting(false)
    }
}
private struct TileTrailView: View {
    let tile: CloudGameModel.Tile
    let index: Int
    let heartMode: Bool

    var body: some View {
        Group {
            if heartMode, tile != .bomb {
                HeartShape()
                    .fill(tile.color.opacity(0.72))
                    .overlay(HeartShape().stroke(.white.opacity(0.22), lineWidth: 0.8))
            } else {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [tile.color.opacity(0.80), tile.color.opacity(0.16), .clear],
                            center: .center,
                            startRadius: 1,
                            endRadius: 28
                        )
                    )
            }
        }
        .padding(CGFloat(5 + index * 2))
        .opacity(max(0.16, 0.58 - Double(index) * 0.07))
        .blur(radius: CGFloat(index) * 0.45)
    }
}

private struct HeartShape: Shape {
    func path(in rect: CGRect) -> Path {
        let width = rect.width
        let height = rect.height
        let x = rect.midX
        let y = rect.midY
        var path = Path()
        path.move(to: CGPoint(x: x, y: y + height * 0.33))
        path.addCurve(
            to: CGPoint(x: x - width * 0.13, y: y - height * 0.31),
            control1: CGPoint(x: x - width * 0.52, y: y - height * 0.02),
            control2: CGPoint(x: x - width * 0.39, y: y - height * 0.44)
        )
        path.addCurve(
            to: CGPoint(x: x, y: y - height * 0.13),
            control1: CGPoint(x: x - width * 0.04, y: y - height * 0.27),
            control2: CGPoint(x: x, y: y - height * 0.20)
        )
        path.addCurve(
            to: CGPoint(x: x + width * 0.13, y: y - height * 0.31),
            control1: CGPoint(x: x, y: y - height * 0.20),
            control2: CGPoint(x: x + width * 0.04, y: y - height * 0.27)
        )
        path.addCurve(
            to: CGPoint(x: x, y: y + height * 0.33),
            control1: CGPoint(x: x + width * 0.39, y: y - height * 0.44),
            control2: CGPoint(x: x + width * 0.52, y: y - height * 0.02)
        )
        path.closeSubpath()
        return path
    }
}

private struct HeartBurstView: View {
    private let colors: [Color] = [.pink, .red, .orange, .yellow, .purple, .cyan, .green, .white]

    var body: some View {
        GeometryReader { proxy in
            TimelineView(.animation) { timeline in
                let progress = CGFloat((timeline.date.timeIntervalSinceReferenceDate * 2).truncatingRemainder(dividingBy: 1))
                ZStack {
                    ForEach(colors.indices, id: \.self) { index in
                        let angle = -CGFloat.pi / 2 + CGFloat(index) * .pi * 2 / CGFloat(colors.count)
                        let distance = min(proxy.size.width, proxy.size.height) * (0.12 + 0.48 * progress)
                        HeartShape()
                            .fill(colors[index].opacity(Double(1 - progress)))
                            .frame(width: proxy.size.width * 0.22, height: proxy.size.height * 0.22)
                            .position(
                                x: proxy.size.width / 2 + cos(angle) * distance,
                                y: proxy.size.height / 2 + sin(angle) * distance
                            )
                    }
                }
            }
        }
        .allowsHitTesting(false)
    }
}
private enum CarBrand {
    case bmw, mercedes, audi, ferrari, lamborghini, porsche, dodge, cadillac, nissan

    var assetName: String {
        switch self {
        case .bmw: return "CarBmw"
        case .mercedes: return "CarMercedes"
        case .audi: return "CarAudi"
        case .ferrari: return "CarFerrari"
        case .lamborghini: return "CarLamborghini"
        case .porsche: return "CarPorsche"
        case .dodge: return "CarDodge"
        case .cadillac: return "CarCadillac"
        case .nissan: return "CarNissan"
        }
    }
}

private extension CloudGameModel.Tile {
    var kuromiOuterColor: Color {
        self == .white ? Color(red: 0.95, green: 0.89, blue: 0.77) : color
    }

    var carOuterColor: Color {
        self == .white ? Color(red: 0.95, green: 0.89, blue: 0.77) : color
    }

    var carBrand: CarBrand {
        switch self {
        case .blue: return .bmw
        case .black: return .mercedes
        case .white: return .audi
        case .red: return .ferrari
        case .yellow: return .lamborghini
        case .green: return .porsche
        case .purple, .orange: return .dodge
        case .pink: return .cadillac
        case .cyan: return .nissan
        case .bomb: return .nissan
        }
    }
}

private struct KuromiTileView: View {
    let tile: CloudGameModel.Tile
    let compact: Bool

    var body: some View {
        GeometryReader { proxy in
            let size = min(proxy.size.width, proxy.size.height)
            let center = CGPoint(x: proxy.size.width / 2, y: proxy.size.height * 0.54)
            let radius = size * 0.34
            let outer = tile.kuromiOuterColor
            let creamFace = Color(red: 1.0, green: 0.985, blue: 0.955)
            let ink = Color(red: 0.055, green: 0.055, blue: 0.075)
            ZStack {
                Circle()
                    .fill(.black.opacity(0.26))
                    .frame(width: radius * 2.04, height: radius * 2.04)
                    .position(x: center.x + radius * 0.10, y: center.y + radius * 0.14)

                KuromiEarShape(side: -1)
                    .fill(
                        RadialGradient(
                            colors: [Color.white.opacity(0.40), outer, outer.opacity(0.58)],
                            center: .topLeading,
                            startRadius: 1,
                            endRadius: size * 0.70
                        )
                    )
                    .overlay(KuromiEarShape(side: -1).stroke(outer.opacity(0.55), lineWidth: max(0.8, size * 0.015)))
                KuromiEarShape(side: 1)
                    .fill(
                        RadialGradient(
                            colors: [Color.white.opacity(0.34), outer, outer.opacity(0.58)],
                            center: .topLeading,
                            startRadius: 1,
                            endRadius: size * 0.70
                        )
                    )
                    .overlay(KuromiEarShape(side: 1).stroke(outer.opacity(0.55), lineWidth: max(0.8, size * 0.015)))

                Circle()
                    .fill(
                        RadialGradient(
                            colors: [Color.white.opacity(0.46), outer, outer.opacity(0.62)],
                            center: .topLeading,
                            startRadius: 1,
                            endRadius: radius * 2.1
                        )
                    )
                    .frame(width: radius * 2, height: radius * 2)
                    .overlay(Circle().stroke(tile == .white ? Color(red: 0.76, green: 0.64, blue: 0.43) : outer.opacity(0.70), lineWidth: compact ? 1.0 : 1.6))
                    .position(center)

                KuromiFaceMaskShape()
                    .fill(creamFace)
                    .frame(width: radius * 1.72, height: radius * 1.18)
                    .position(x: center.x, y: center.y + radius * 0.19)

                Group {
                    Ellipse()
                        .fill(ink)
                        .frame(width: radius * 0.30, height: radius * 0.50)
                        .position(x: center.x - radius * 0.33, y: center.y + radius * 0.20)
                    Ellipse()
                        .fill(ink)
                        .frame(width: radius * 0.30, height: radius * 0.50)
                        .position(x: center.x + radius * 0.33, y: center.y + radius * 0.20)
                    lashPath(center: center, radius: radius, side: -1)
                        .stroke(ink, style: StrokeStyle(lineWidth: max(1, radius * 0.065), lineCap: .round))
                    lashPath(center: center, radius: radius, side: 1)
                        .stroke(ink, style: StrokeStyle(lineWidth: max(1, radius * 0.065), lineCap: .round))
                    Ellipse()
                        .fill(Color(red: 1.0, green: 0.62, blue: 0.72))
                        .frame(width: radius * 0.33, height: radius * 0.20)
                        .position(x: center.x - radius * 0.57, y: center.y + radius * 0.55)
                    Ellipse()
                        .fill(Color(red: 1.0, green: 0.62, blue: 0.72))
                        .frame(width: radius * 0.33, height: radius * 0.20)
                        .position(x: center.x + radius * 0.57, y: center.y + radius * 0.55)
                    Ellipse()
                        .fill(Color(red: 1.0, green: 0.42, blue: 0.62))
                        .frame(width: radius * 0.18, height: radius * 0.11)
                        .position(x: center.x, y: center.y + radius * 0.43)
                }

                KuromiSkullView()
                    .frame(width: radius * 0.55, height: radius * 0.44)
                    .position(x: center.x, y: center.y - radius * 0.45)

                Ellipse()
                    .fill(.white.opacity(0.70))
                    .frame(width: radius * 0.55, height: radius * 0.16)
                    .rotationEffect(.degrees(-28))
                    .position(x: center.x - radius * 0.44, y: center.y - radius * 0.60)
                Circle()
                    .fill(.white.opacity(0.50))
                    .frame(width: radius * 0.18, height: radius * 0.18)
                    .position(x: center.x - radius * 0.08, y: center.y - radius * 0.67)
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .allowsHitTesting(false)
    }

    private func lashPath(center: CGPoint, radius: CGFloat, side: CGFloat) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: center.x + side * radius * 0.42, y: center.y + radius * 0.08))
        path.addLine(to: CGPoint(x: center.x + side * radius * 0.60, y: center.y - radius * 0.06))
        path.move(to: CGPoint(x: center.x + side * radius * 0.42, y: center.y + radius * 0.20))
        path.addLine(to: CGPoint(x: center.x + side * radius * 0.62, y: center.y + radius * 0.12))
        return path
    }
}

private struct KuromiEarShape: Shape {
    let side: CGFloat

    func path(in rect: CGRect) -> Path {
        let size = min(rect.width, rect.height)
        let cx = rect.midX
        let cy = rect.minY + size * 0.54
        let r = size * 0.34
        var path = Path()
        path.move(to: CGPoint(x: cx + side * r * 0.35, y: cy - r * 0.72))
        path.addCurve(
            to: CGPoint(x: cx + side * r * 0.94, y: cy - r * 1.43),
            control1: CGPoint(x: cx + side * r * 0.49, y: cy - r * 1.12),
            control2: CGPoint(x: cx + side * r * 0.69, y: cy - r * 1.41)
        )
        path.addCurve(
            to: CGPoint(x: cx + side * r * 1.22, y: cy - r * 0.75),
            control1: CGPoint(x: cx + side * r * 1.13, y: cy - r * 1.45),
            control2: CGPoint(x: cx + side * r * 1.16, y: cy - r * 1.09)
        )
        path.addQuadCurve(to: CGPoint(x: cx + side * r * 0.81, y: cy - r * 0.59),
                          control: CGPoint(x: cx + side * r * 1.22, y: cy - r * 0.54))
        path.closeSubpath()
        path.addEllipse(in: CGRect(x: cx + side * r * 1.02 - r * 0.20, y: cy - r * 1.44 - r * 0.20, width: r * 0.40, height: r * 0.40))
        return path
    }
}

private struct KuromiFaceMaskShape: Shape {
    func path(in rect: CGRect) -> Path {
        let cx = rect.midX
        let cy = rect.midY - rect.height * 0.02
        let rx = rect.width * 0.50
        let ry = rect.height * 0.50
        var path = Path()
        path.move(to: CGPoint(x: cx - rx * 0.86, y: cy - ry * 0.18))
        path.addCurve(
            to: CGPoint(x: cx - rx * 0.05, y: cy - ry * 0.34),
            control1: CGPoint(x: cx - rx * 0.83, y: cy - ry * 0.84),
            control2: CGPoint(x: cx - rx * 0.34, y: cy - ry * 0.78)
        )
        path.addQuadCurve(to: CGPoint(x: cx + rx * 0.05, y: cy - ry * 0.34), control: CGPoint(x: cx, y: cy - ry * 0.22))
        path.addCurve(
            to: CGPoint(x: cx + rx * 0.86, y: cy - ry * 0.18),
            control1: CGPoint(x: cx + rx * 0.34, y: cy - ry * 0.78),
            control2: CGPoint(x: cx + rx * 0.83, y: cy - ry * 0.84)
        )
        path.addCurve(
            to: CGPoint(x: cx, y: cy + ry * 0.86),
            control1: CGPoint(x: cx + rx * 0.96, y: cy + ry * 0.54),
            control2: CGPoint(x: cx + rx * 0.46, y: cy + ry * 0.88)
        )
        path.addCurve(
            to: CGPoint(x: cx - rx * 0.86, y: cy - ry * 0.18),
            control1: CGPoint(x: cx - rx * 0.46, y: cy + ry * 0.88),
            control2: CGPoint(x: cx - rx * 0.96, y: cy + ry * 0.54)
        )
        path.closeSubpath()
        return path
    }
}

private struct KuromiSkullView: View {
    var body: some View {
        GeometryReader { proxy in
            let size = min(proxy.size.width, proxy.size.height)
            ZStack {
                Ellipse()
                    .fill(Color(red: 1.0, green: 0.60, blue: 0.72))
                    .frame(width: size * 0.92, height: size * 0.74)
                    .position(x: proxy.size.width / 2, y: proxy.size.height * 0.42)
                HStack(spacing: size * 0.08) {
                    RoundedRectangle(cornerRadius: size * 0.04).fill(Color(red: 1.0, green: 0.60, blue: 0.72))
                    RoundedRectangle(cornerRadius: size * 0.04).fill(Color(red: 1.0, green: 0.60, blue: 0.72))
                    RoundedRectangle(cornerRadius: size * 0.04).fill(Color(red: 1.0, green: 0.60, blue: 0.72))
                }
                .frame(width: size * 0.58, height: size * 0.25)
                .position(x: proxy.size.width / 2, y: proxy.size.height * 0.78)
                HStack(spacing: size * 0.18) {
                    Circle().fill(Color(red: 0.055, green: 0.055, blue: 0.075))
                    Circle().fill(Color(red: 0.055, green: 0.055, blue: 0.075))
                }
                .frame(width: size * 0.48, height: size * 0.16)
                .position(x: proxy.size.width / 2, y: proxy.size.height * 0.39)
            }
        }
    }
}

private struct CarLogoTileView: View {
    let tile: CloudGameModel.Tile
    let compact: Bool

    var body: some View {
        GeometryReader { proxy in
            let size = min(proxy.size.width, proxy.size.height)
            let outer = tile.carOuterColor
            ZStack {
                Circle()
                    .fill(.black.opacity(0.26))
                    .frame(width: size * 0.88, height: size * 0.88)
                    .position(x: proxy.size.width / 2 + size * 0.04, y: proxy.size.height / 2 + size * 0.05)
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [Color.white.opacity(0.55), outer, outer.opacity(0.58)],
                            center: .topLeading,
                            startRadius: 1,
                            endRadius: size * 0.70
                        )
                    )
                    .frame(width: size * 0.92, height: size * 0.92)
                    .overlay(Circle().stroke(tile == .white ? Color(red: 0.76, green: 0.64, blue: 0.43) : outer.opacity(0.72), lineWidth: compact ? 1.0 : 1.8))
                    .position(x: proxy.size.width / 2, y: proxy.size.height / 2)
                Circle()
                    .fill(Color(red: 0.12, green: 0.13, blue: 0.16))
                    .frame(width: size * 0.75, height: size * 0.75)
                    .overlay(Circle().stroke(Color(red: 0.86, green: 0.88, blue: 0.92), lineWidth: max(1, size * 0.028)))
                    .position(x: proxy.size.width / 2, y: proxy.size.height / 2)
                Image(tile.carBrand.assetName)
                    .resizable()
                    .interpolation(.high)
                    .aspectRatio(contentMode: .fit)
                    .frame(width: size * 0.76, height: size * 0.76)
                    .position(x: proxy.size.width / 2, y: proxy.size.height / 2)
                Ellipse()
                    .fill(.white.opacity(0.72))
                    .frame(width: size * 0.27, height: size * 0.09)
                    .rotationEffect(.degrees(-28))
                    .position(x: proxy.size.width * 0.32, y: proxy.size.height * 0.22)
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .allowsHitTesting(false)
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
            let titleLayout = Self.layout(for: loading.title)
            let subtitleLayout = Self.layout(for: loading.subtitle)
            let titleStep = min(
                proxy.size.width / CGFloat(max(1, titleLayout.columns + 2)),
                proxy.size.height / CGFloat(max(8, titleLayout.rows + 4))
            )
            let subtitleStep = min(
                proxy.size.width / CGFloat(max(1, subtitleLayout.columns + 4)),
                proxy.size.height / CGFloat(max(14, subtitleLayout.rows + 9))
            )
            let totalHeight = titleStep * CGFloat(titleLayout.rows) + subtitleStep * 1.55 + subtitleStep * CGFloat(subtitleLayout.rows)
            let titleTop = max(0, (proxy.size.height - totalHeight) / 2.0)
            let subtitleTop = titleTop + titleStep * CGFloat(titleLayout.rows) + subtitleStep * 1.55
            let color = Self.colors[loading.colorPhase % Self.colors.count]
            ZStack {
                dotLine(
                    layout: titleLayout,
                    step: titleStep,
                    dotSize: max(5.0, titleStep * 0.60),
                    top: titleTop,
                    canvasWidth: proxy.size.width,
                    color: color
                )
                dotLine(
                    layout: subtitleLayout,
                    step: subtitleStep,
                    dotSize: max(3.0, subtitleStep * 0.58),
                    top: subtitleTop,
                    canvasWidth: proxy.size.width,
                    color: color
                )
            }
            .opacity(loading.visible ? 1.0 : 0.26)
        }
    }

    private func dotLine(
        layout: (dots: [Dot], columns: Int, rows: Int),
        step: CGFloat,
        dotSize: CGFloat,
        top: CGFloat,
        canvasWidth: CGFloat,
        color: Color
    ) -> some View {
        let startX = (canvasWidth - CGFloat(max(0, layout.columns - 1)) * step) / 2.0
        return ZStack {
            ForEach(layout.dots) { dot in
                Circle()
                    .fill(color)
                    .frame(width: dotSize, height: dotSize)
                    .shadow(color: color.opacity(0.55), radius: 4)
                    .overlay(Circle().stroke(Color.white.opacity(0.35), lineWidth: 0.6))
                    .position(
                        x: startX + CGFloat(dot.x) * step,
                        y: top + CGFloat(dot.y) * step
                    )
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
        .orange,
        .black
    ]

    private static func layout(for message: String) -> (dots: [Dot], columns: Int, rows: Int) {
        var dots: [Dot] = []
        var cursor = 0
        var id = 0
        var rows = 1
        for character in message {
            let glyph = glyph(for: character)
            rows = max(rows, glyph.count)
            for row in 0..<glyph.count {
                let values = Array(glyph[row])
                for column in 0..<values.count where values[column] == "#" {
                    dots.append(Dot(id: id, x: cursor + column, y: row))
                    id += 1
                }
            }
            cursor += (glyph.first?.count ?? 0) + 1
        }
        return (dots, max(1, cursor - 1), rows)
    }

    private static func glyph(for character: Character) -> [String] {
        switch character.lowercased() {
        case "1": return [" # ", "## ", " # ", " # ", "###"]
        case "3": return ["## ", "  #", " # ", "  #", "## "]
        case "4": return ["# #", "# #", "###", "  #", "  #"]
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
        case "头": return ["   #   ", "#  #  #", " # # # ", "  ###  ", "   #   ", "  # #  ", "##   ##"]
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
                    Text(String(format: "%05d", min(99_999, entry.score)))
                        .fontWeight(.bold)
                        .monospacedDigit()
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
            .navigationTitle("高分榜（前10名）")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("完成") { dismiss() }
                }
            }
        }
    }
}

private struct HistoryView: View {
    @EnvironmentObject private var game: CloudGameModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(game.history) { record in
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(record.dailyChallenge ? "每日挑战" : "普通模式")
                            .font(.headline)
                        Spacer()
                        Text(String(format: "%05d", min(99_999, record.score)))
                            .font(.headline)
                            .monospacedDigit()
                    }
                    Text("\(record.date) · 移动 \(record.moves) 次 · 消除 \(record.removed) 个")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 3)
            }
            .overlay {
                if game.history.isEmpty {
                    VStack(spacing: 8) {
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.title2)
                        Text("暂无历史战绩")
                    }
                    .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("历史战绩")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("完成") { dismiss() }
                }
            }
        }
    }
}

private struct AchievementsView: View {
    @EnvironmentObject private var game: CloudGameModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(CloudGameModel.achievements) { achievement in
                let unlocked = game.unlockedAchievementIDs.contains(achievement.id)
                HStack(spacing: 12) {
                    Image(systemName: unlocked ? "checkmark.seal.fill" : "lock.fill")
                        .foregroundStyle(unlocked ? Color.green : Color.secondary)
                        .frame(width: 28)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(achievement.title)
                            .font(.headline)
                        Text(achievement.detail)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .opacity(unlocked ? 1 : 0.62)
                .padding(.vertical, 3)
            }
            .navigationTitle("成就 \(game.unlockedAchievementIDs.count)/\(CloudGameModel.achievements.count)")
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
                    Text("每完成 12 次有效移动，每回合会多生成 1 个棋子，最多额外生成 3 个。")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Text(adminModeNote)
                        .font(.footnote)
                        .foregroundStyle(game.adminMode ? .green : .secondary)
                    settingSlider("难度系数", value: $game.difficulty, range: 1...2, step: 0.1)
                        .disabled(!game.adminMode)
                    settingSlider("白棋概率系数", value: $game.whiteProbability, range: 0...2, step: 0.1)
                        .disabled(!game.adminMode)
                    settingSlider("炸药概率系数", value: $game.bombProbability, range: 0...2, step: 0.1)
                        .disabled(!game.adminMode)
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
                    Toggle("音乐", isOn: Binding(
                        get: { game.musicEnabled },
                        set: { value in
                            game.musicEnabled = value
                            game.noteMusicToggleForAdminUnlock()
                        }
                    ))
                    Picker("\u{97F3}\u{4E50}\u{66F2}\u{76EE}", selection: Binding(
                        get: {
                            CloudGameModel.MusicTrack.selectable.contains(game.musicTrack) ? game.musicTrack : .music1
                        },
                        set: { game.musicTrack = $0 }
                    )) {
                        ForEach(CloudGameModel.MusicTrack.selectable) { track in
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
                ToolbarItem(placement: .topBarLeading) {
                    Button("保存") {
                        game.saveSettingsAndCheckAdminEaster()
                        dismiss()
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
        }
        .onAppear { game.beginSettingsSession() }
    }

    private var adminModeNote: String {
        if game.adminMode {
            return "管理员模式已开启，可以修改概率参数。保存一次后会自动关闭管理员模式。"
        }
        return String(
            format: "普通模式锁定：当前难度 %.1f、白棋 %.1f、炸药 %.1f。连续切换音乐 7 次可进入管理员模式。",
            game.difficulty,
            game.whiteProbability,
            game.bombProbability
        )
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
            Text(game.dailyChallenge ? "每日挑战" : "普通模式")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(game.dailyChallenge ? Color.orange : Color.secondary)
            Text("本局得分：\(game.score)")
                .font(.headline)
            VStack(spacing: 5) {
                Text("移动 \(game.moves) 次 · 消除 \(game.removedCount) 个")
                Text("完成 \(game.linesCleared) 次连线 · 单次最多 \(game.bestClearCount) 个")
                Text("最高连锁 \(game.bestChain) 次 · 引爆炸药 \(game.bombsTriggered) 个")
            }
            .font(.subheadline)
            .foregroundStyle(.secondary)
            if game.qualifiesForHighScore {
                TextField("昵称", text: $nickname)
                    .textFieldStyle(.roundedBorder)
                    .padding(.horizontal)
                Button("保存成绩并开始新游戏") {
                    game.submitScore(nickname)
                    dismiss()
                }
                .buttonStyle(.borderedProminent)
            } else {
                Text("本局未进入前 10 名")
                    .foregroundStyle(.secondary)
                Button("开始新游戏") {
                    game.dismissGameOverAndRestart()
                    dismiss()
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding(24)
    }
}
