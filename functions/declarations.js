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
}
exports.ChatMessage = ChatMessage;

class MediaChatMessage {
    constructor(postedBy, submittedAt, type, path) {
        this.postedBy = postedBy;
        this.submittedAt = submittedAt;
        this.type = type;
        this.path = path;
    }
}

exports.MediaChatMessage = MediaChatMessage;
