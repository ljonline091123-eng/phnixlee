import SwiftUI

@main
struct FiveLinesApp: App {
    @StateObject private var game = CloudGameModel()

    var body: some Scene {
        WindowGroup {
            CloudContentView()
                .environmentObject(game)
        }
    }
}
