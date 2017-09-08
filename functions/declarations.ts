import UserRecord = admin.auth.UserRecord;

export class UserProfile {

    displayName: string;
    email: string;
    photoURL: string;
    uid: string;
    createdAt: number;
    notificationTokens: object;

    constructor(firebaseUser: UserRecord) {
        this.displayName = firebaseUser.displayName;
        this.email = firebaseUser.email;
        this.photoURL = firebaseUser.photoURL;
        this.uid = firebaseUser.uid;
    }

}

export class ChatMessage {

    postedBy: string;
    submittedAt: number;
    text: string;

}

export class MediaChatMessage {

    postedBy: string;
    submittedAt: number;
    type: string;
    path: string;


    constructor(postedBy: string, submittedAt: number, type: string, path: string) {
        this.postedBy = postedBy;
        this.submittedAt = submittedAt;
        this.type = type;
        this.path = path;
    }
}