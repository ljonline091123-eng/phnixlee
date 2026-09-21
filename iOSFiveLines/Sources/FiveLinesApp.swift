import SwiftUI

@main
struct FiveLinesApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var game = CloudGameModel()

    var body: some Scene {
        WindowGroup {
            CloudContentView()
                .environmentObject(game)
                .onChange(of: scenePhase) { phase in
                    game.setAppActive(phase == .active)
                }
        }
    }
}
