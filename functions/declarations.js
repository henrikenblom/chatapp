"use strict";
Object.defineProperty(exports, "__esModule", {value: true});
class UserProfile {
    constructor(firebaseUser) {
        this.displayName = firebaseUser.displayName;
        this.email = firebaseUser.email;
        this.photoURL = firebaseUser.photoURL;
        this.uid = firebaseUser.uid;
    }
}
exports.UserProfile = UserProfile;
class ChatMessage {
    constructor(postedBy, submittedAt, type, path) {
        this.type = "text";
        this.postedBy = postedBy;
        this.submittedAt = submittedAt;
        this.type = type;
        this.path = path;
    }
}

exports.ChatMessage = ChatMessage;
