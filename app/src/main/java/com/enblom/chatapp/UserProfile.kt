package com.enblom.chatapp

import java.util.*

class UserProfile {

    var displayName: String? = null
    var email: String? = null
    var photoURL: String? = null
    var uid: String? = null
    var createdAt: Long? = null
    var lastSeenAt: Long? = null
    var notificationTokens: ArrayList<String?> = ArrayList()
    var connections: HashMap<String, Long> = HashMap()

}