//
//  FirestoreManager.swift
//  Radio Sihina
//
//  Created by Rusiru Wijethilake on 2022-06-13.
//

import Foundation
import Firebase
import FirebaseStorage

class FirestoreManager: ObservableObject {
    @Published var streamName: String = ""
    @Published var streamPresenter: String = ""
    @Published var streamUrl: String = ""
    @Published var streamImageUrl: String = ""
    @Published var isStreamOnline: Bool = false
    
    func fetchStreamData() {
        let db = Firestore.firestore()
        let storage = Storage.storage()

        let docRef = db.collection("live").document("radiosihina")

        docRef.addSnapshotListener { documentSnapshot, error in
            guard let document = documentSnapshot else {
              print("Error fetching document: \(error!)")
              return
            }

            if let document = documentSnapshot, document.exists {
                let data = document.data()
                if let data = data {
                    let imageData = data["imgurl"] as? String ?? ""
                    storage.reference(forURL: imageData).downloadURL { url, error in
                        if let error = error {
                          // Handle any errors
                        } else {
                            self.streamName = data["nowplaying"] as? String ?? ""
                            self.streamPresenter = data["presenter"] as? String ?? ""
                            self.streamUrl = data["streamurl"] as? String ?? ""
                            self.streamImageUrl = url?.absoluteString as? String ?? ""
                            self.isStreamOnline = data["status"] as? Bool ?? false
                        }
                      }
                }
            }
          }
    }
    
    init() {
        fetchStreamData()
    }
    
}
