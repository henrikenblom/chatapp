"use strict";
Object.defineProperty(exports, "__esModule", {value: true});
var functions = require("firebase-functions");
var admin = require("firebase-admin");
var declarations_1 = require("./declarations");
admin.initializeApp(functions.config().firebase);
var YEAR_3000 = 3250368000000;
exports.setupNewUser = functions.auth.user().onCreate(function (event) {
    var reference = admin.database().ref("/userprofiles/" + event.data.uid + "/");
    var userProfile = new declarations_1.UserProfile(event.data);
    userProfile.createdAt = admin.database.ServerValue.TIMESTAMP;
    return reference.set(userProfile);
});
exports.setupNewChat = functions.database.ref("/chats/{chatId}").onCreate(function (event) {
    var timestamp = admin.database.ServerValue.TIMESTAMP;
    getAllProfiles().then(function (data) {
        var userProfiles = data.userProfiles;
        getChatmembers(event.params.chatId).then(function (data) {
            var members = data.members;
            var isGroupChat = Object.keys(members).length > 2;
            for (var memberKey in members) {
                var chatName = [];
                var memberPhotos = [];
                for (var profileKey in userProfiles) {
                    if (profileKey in members
                        && profileKey != memberKey) {
                        chatName.push(userProfiles[profileKey].displayName);
                        memberPhotos.push(userProfiles[profileKey].photoURL);
                    }
                }
                admin.database()
                    .ref("user_chats")
                    .child(memberKey)
                    .child(event.params.chatId)
                    .child("chat_name")
                    .set(chatName.join(", "));
                admin.database()
                    .ref("user_chats")
                    .child(memberKey)
                    .child(event.params.chatId)
                    .child("isGroupChat")
                    .set(isGroupChat);
                admin.database()
                    .ref("user_chats")
                    .child(memberKey)
                    .child(event.params.chatId)
                    .child("lastMessageAt")
                    .set(timestamp);
                for (var _i = 0, memberPhotos_1 = memberPhotos; _i < memberPhotos_1.length; _i++) {
                    var photo = memberPhotos_1[_i];
                    admin.database()
                        .ref("user_chats")
                        .child(memberKey)
                        .child(event.params.chatId)
                        .child("photos")
                        .push(photo);
                }
            }
        });
    });
    return event.data.ref.child("createdAt").set(timestamp);
});
exports.updateChatMessageTimestamps = functions.database.ref("/chats/{chatId}/messages/{messageId}").onCreate(function (event) {
    getChatmembers(event.params.chatId).then(function (data) {
        var timestamp = new Date().getTime();
        var members = data.members;
        var groupChat = Object.keys(members).length > 2;
        for (var memberKey in members) {
            var orderBy = YEAR_3000 - timestamp;
            admin.database()
                .ref("user_chats")
                .child(memberKey)
                .child(event.params.chatId)
                .child("groupChat")
                .set(groupChat);
            admin.database()
                .ref("user_chats")
                .child(memberKey)
                .child(event.params.chatId)
                .child("lastMessageAt")
                .set(timestamp);
            admin.database()
                .ref("user_chats")
                .child(memberKey)
                .child(event.params.chatId)
                .child("orderBy")
                .set(orderBy);
        }
    });
    return event.data.ref.child("submittedAt").set(admin.database.ServerValue.TIMESTAMP);
});
exports.sendMessageNotifications = functions.database.ref("/chats/{chatId}/messages/{messageId}").onCreate(function (event) {
    var message = event.data.val();
    getAllProfiles().then(function (data) {
        var userProfiles = data.userProfiles;
        getChatmembers(event.params.chatId).then(function (data) {
            var members = data.members;
            var _loop_1 = function (memberKey) {
                if (message.postedBy != memberKey) {
                    var tokens_1 = userProfiles[memberKey].notificationTokens;
                    getChatname(event.params.chatId, memberKey).then(function (chatName) {
                        for (var tokenId in tokens_1) {
                            var payload = {
                                notification: {
                                    title: userProfiles[message.postedBy].displayName,
                                    body: message.text,
                                    sound: "chatapp_notice",
                                    click_action: "ACTIVITY_CHAT"
                                },
                                data: {
                                    chatKey: event.params.chatId,
                                    chatName: chatName
                                }
                            };
                            admin.messaging().sendToDevice(tokens_1[tokenId], payload);
                        }
                    });
                }
            };
            for (var memberKey in members) {
                _loop_1(memberKey);
            }
        });
    });
});
exports.cleanUpChat = functions.database.ref("/chats/{chatId}/members/{memberKey}").onDelete(function (event) {
    return admin.database()
        .ref("user_chats")
        .child(event.params.memberKey)
        .child(event.params.chatId)
        .remove();
});
var getAllProfiles = function () {
    return admin.database()
        .ref("userprofiles")
        .orderByKey()
        .once("value")
        .then(function (snap) {
            return {userProfiles: snap.val()};
        });
};
var getChatmembers = function (chatId) {
    return admin.database()
        .ref("chats")
        .child(chatId)
        .child("members")
        .orderByKey()
        .once("value")
        .then(function (snap) {
            return {members: snap.val()};
        });
};
var getChatname = function (chatId, uid) {
    return admin.database()
        .ref("user_chats")
        .child(uid)
        .child(chatId)
        .child("chat_name")
        .orderByKey()
        .once("value")
        .then(function (snap) {
            return snap.val();
        });
};
