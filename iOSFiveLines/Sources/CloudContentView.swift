import SwiftUI
import Darwin

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
                        TileView(tile: tile, compact: true, rayRacerMode: game.rayRacerMode, kuromiTheme: game.kuromiTheme, heartMode: game.heartMode)
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
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 3), count: 9), spacing: 3) {
                    ForEach(0..<81, id: \.self) { index in
                        let trailIndex = game.racerTrail.firstIndex(of: index)
                        BoardCell(
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
    let rayRacerMode: Bool
    let kuromiTheme: Bool
    let heartMode: Bool
    let heartBurst: Bool

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 4)
                .fill(Color(red: 0.57, green: 0.39, blue: 0.22))
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.black.opacity(0.22), lineWidth: 1))
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
            RoundedRectangle(cornerRadius: 5)
                .stroke(selected ? Color.white : Color.clear, lineWidth: 3)
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
                standardBall
                Image(systemName: "burst.fill")
                    .font(.system(size: compact ? 11 : 16, weight: .black))
                    .foregroundStyle(.white)
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
                Circle()
                    .stroke(tile == .white ? Color.gray.opacity(0.45) : Color.white.opacity(0.6), lineWidth: compact ? 1 : 2)
            )
            .shadow(color: .black.opacity(0.25), radius: compact ? 2 : 4, y: 2)
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
                    Button("完成") {
                        game.saveSettingsAndCheckAdminEaster()
                        dismiss()
                    }
                }
            }
        }
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
