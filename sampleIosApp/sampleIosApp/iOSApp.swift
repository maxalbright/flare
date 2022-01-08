import SwiftUI
import test
import FirebaseCore

@main
struct iOSApp: App {
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
    init() {
        FirebaseApp.configure()
        //DispatchQueue.global().async {
//            FirebaseApp.confpigure()
//            Auth.auth().createUser(withEmail: "hey@sup.com", password: "mypassword") { authResult, error in
//                print(authResult != nil ? authResult : "nil")
//                print(error != nil ? error : "nil")
//              // ...
//            }
            
        print("Start")
            DeviceTest().runDeviceTest(context: nil)
            //SpecificTest().runDeviceTest()
        print("End")
        //}
    }
}
