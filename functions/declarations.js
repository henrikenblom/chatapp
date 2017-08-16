"use strict";
Object.defineProperty(exports, "__esModule", {value: true});
var UserProfile = (function () {
    function UserProfile(firebaseUser) {
        this.displayName = firebaseUser.displayName;
        this.email = firebaseUser.email;
        this.photoURL = firebaseUser.photoURL;
        this.uid = firebaseUser.uid;
    }

    return UserProfile;
}());
exports.UserProfile = UserProfile;
var ChatMessage = (function () {
    function ChatMessage() {
    }

    return ChatMessage;
}());
exports.ChatMessage = ChatMessage;
