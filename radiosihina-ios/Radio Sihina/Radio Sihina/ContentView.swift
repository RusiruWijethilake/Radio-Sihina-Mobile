//
//  ContentView.swift
//  Radio Sihina
//
//  Created by Rusiru Wijethilake on 2022-06-12.
//

import SwiftUI
import SwiftAudioPlayer
import Firebase

struct ContentView: View {
    @EnvironmentObject var firestoreManager: FirestoreManager
    
    var body: some View {
        TabView {
            RadioView()
                .environmentObject(FirestoreManager())
                .tabItem {
                    Image(systemName: "play")
                    Text("Radio")
                }
            Text("Under Development")
                .tabItem {
                    Image(systemName: "books.vertical")
                    Text("Library")
                }
            AboutView()
                .tabItem {
                    Image(systemName: "info")
                    Text("About")
               }
        }
    }
}

struct RadioView: View {
    @EnvironmentObject var firestoreManager: FirestoreManager
    
    @State var txtPlayBtn: String = "Play"
    @State var icoPlayBtn: String = "play"
    
    var body: some View {
        VStack {
            AsyncImage(url: URL(string: "\(firestoreManager.streamImageUrl)")) { image in
                image.resizable()
            } placeholder: {
                ProgressView()
            }
            .frame(width: 250, height: 250)
            .cornerRadius(30.0)
            Spacer().frame(height: 40.0)
            Text("Now Playing")
                .font(.system(size: 18, weight: .light))
                .padding(.bottom, 2)
            Text("\(firestoreManager.streamName)")
                .font(.system(size: 24, weight: .semibold))
                .padding(.bottom, 20)
            Text("Presented By")
                .font(.system(size: 16, weight: .light))
                .padding(.bottom, 2)
            Text("\(firestoreManager.streamPresenter)")
                .font(.system(size: 20, weight: .regular))
                .padding(.bottom, 30)
            HStack {
                Button(action: {playRadio(status: firestoreManager.isStreamOnline, radiourl: "\(firestoreManager.streamUrl)")}){
                    Label("Play", systemImage: "play")
                }
                .buttonStyle(.bordered)
                Spacer().frame(width: 20.0)
                Button(action: shareRadio){
                    Label("Share", systemImage: "link")
                }
                .buttonStyle(.borderless)
            }
        }
    }
    
    func playRadio(status: Bool, radiourl: String){
        if(SAPlayer.shared.playerNode?.isPlaying != nil){
            SAPlayer.shared.pause()
            SAPlayer.shared.clear()
        }else{
            if(status == true){
                let url = URL(string: radiourl)!
                SAPlayer.shared.startRemoteAudio(withRemoteUrl: url)
                let info = SALockScreenInfo(title: "", artist: "Foo", artwork: UIImage(), releaseDate: 123456789)
                SAPlayer.shared.mediaInfo = info
                SAPlayer.shared.play()
            }else{
            }
        }
    }
    
    func shareRadio(){
        
    }
    
}

struct AboutView: View {
    var body: some View {
        VStack {
            Image("logoRadioSihina").resizable().aspectRatio(contentMode: .fit).frame(width: 300.0)
                .padding(.bottom, 5)
            Text("Radio Sihina IOS App")
                .multilineTextAlignment(.center)
                .font(.system(size: 24, weight: .medium))
                .padding(.bottom, 10)
            Text("Radio Sihina is a online radio + podcast initiation by Microlion Technologies.")
                .multilineTextAlignment(.center)
                .padding(.bottom, 20)
            Text("Radio Sihina android app is the official player for Apple IOS mobile devices. The app provide easy access to Radio Sihina services such as radio player, audio library, presenter panel and in app notifications.")
                .multilineTextAlignment(.center)
                .font(Font.body.weight(.light))
                .padding(.bottom, 20)
            Text("Follow us on our social media")
                .padding(.bottom, 20)
            HStack {
                Button(action: goToFacebook, label: {Image("iconFacebook").resizable().frame(width: 32.0, height: 32.0)}).padding(.horizontal, 2)
                Button(action: goToTwitter, label: {Image("iconTwitter").resizable().frame(width: 32.0, height: 32.0)}).padding(.horizontal, 2)
                Button(action: sendAEmail, label: {Image("iconEmail").resizable().frame(width: 32.0, height: 32.0)}).padding(.horizontal, 2)
            }
            .buttonStyle(.borderless)
        }
        .padding(20)
    }
}

struct ContentView_Previews: PreviewProvider {
   static var previews: some View {
        ContentView()
           .environmentObject(FirestoreManager())
    }
}

func goToFacebook(){
    let url = URL(string: "fb://profile/radiosihina")!
    let application = UIApplication.shared
    if application.canOpenURL(url) {
        application.open(url)
    } else {
        application.open(URL(string: "https://de-de.facebook.com/radiosihina")!)
    }
}

func goToTwitter(){
    let url = URL(string: "twitter://user?screen_name=radiosihina")!
    let application = UIApplication.shared
    if application.canOpenURL(url) {
        application.open(url)
    } else {
        application.open(URL(string: "https://twitter.com/radiosihinalive")!)
    }
}

func sendAEmail(){
    let mailtoString = "mailto:radiosihinalive@gmail.com".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)
    let mailtoUrl = URL(string: mailtoString!)!
    if UIApplication.shared.canOpenURL(mailtoUrl) {
            UIApplication.shared.open(mailtoUrl, options: [:])
    }
}
