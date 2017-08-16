import * as functions from "firebase-functions"
import * as admin from "firebase-admin"
import {ChatMessage, UserProfile} from "./declarations";

admin.initializeApp(functions.config().firebase);
const YEAR_3000 = 3250368000000;

exports.setupNewUser = functions.auth.user().onCreate(event => {

    const reference = admin.database().ref("/userprofiles/" + event.data.uid + "/");
    const userProfile = new UserProfile(event.data);
    userProfile.createdAt = admin.database.ServerValue.TIMESTAMP;

    return reference.set(userProfile);

});

exports.setupNewChat = functions.database.ref("/chats/{chatId}").onCreate(event => {

    getAllProfiles().then(data => {

        const userProfiles = data.userProfiles;

        getChatmembers(event.params.chatId).then(data => {

            const members = data.members;

            for (let memberKey in members) {

                let chatName = [];
                let memberPhotos = [];

                for (let profileKey in userProfiles) {
                    if (profileKey in members
                        && profileKey != memberKey) {
                        chatName.push((userProfiles[profileKey] as UserProfile).displayName);
                        memberPhotos.push((userProfiles[profileKey] as UserProfile).photoURL);
                    }
                }

                admin.database()
                    .ref("user_chats")
                    .child(memberKey)
                    .child(event.params.chatId)
                    .child("chat_name")
                    .set(chatName.join(", "));

                for (let photo of memberPhotos) {
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

    return event.data.ref.child("createdAt").set(admin.database.ServerValue.TIMESTAMP);

});

exports.updateChatMessageTimestamps = functions.database.ref("/chats/{chatId}/messages/{messageId}").onCreate(event => {

    getChatmembers(event.params.chatId).then(data => {

        const timestamp = new Date().getTime();
        const members = data.members;

        for (let memberKey in members) {

            let orderBy = YEAR_3000 - timestamp;

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

exports.sendMessageNotifications = functions.database.ref("/chats/{chatId}/messages/{messageId}").onCreate(event => {

    const message = event.data.val() as ChatMessage;

    getAllProfiles().then(data => {

        const userProfiles = data.userProfiles;

        getChatmembers(event.params.chatId).then(data => {

            const members = data.members;

            for (let memberKey in members) {

                if (message.postedBy != memberKey) {

                    const tokens = userProfiles[memberKey].notificationTokens;

                    getChatname(event.params.chatId, memberKey).then(chatName => {

                        for (let tokenId in tokens) {

                            const payload = {
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

                            admin.messaging().sendToDevice(tokens[tokenId], payload);

                        }

                    });

                }

            }

        });

    });

});

exports.cleanUpChat = functions.database.ref("/chats/{chatId}/members/{memberKey}").onDelete(event => {

    admin.database()
        .ref("user_chats")
        .child(event.params.memberKey)
        .child(event.params.chatId)
        .remove();

});

const getAllProfiles = () => {
    return admin.database()
        .ref("userprofiles")
        .orderByKey()
        .once("value")
        .then(snap => {
            return {userProfiles: snap.val()};
        });
};

const getChatmembers = chatId => {
    return admin.database()
        .ref("chats")
        .child(chatId)
        .child("members")
        .orderByKey()
        .once("value")
        .then(snap => {
            return {members: snap.val()};
        });
};

const getChatname = (chatId, uid) => {
    return admin.database()
        .ref("user_chats")
        .child(uid)
        .child(chatId)
        .child("chat_name")
        .orderByKey()
        .once("value")
        .then(snap => {
            return snap.val();
        });
};