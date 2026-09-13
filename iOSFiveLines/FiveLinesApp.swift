import SwiftUI

@main
struct FiveLinesApp: App {
    @StateObject private var game = GameModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(game)
                .preferredColorScheme(.light)
        }
    }
}
