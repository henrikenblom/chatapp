package com.enblom.chatapp

import com.firebase.ui.database.FirebaseArray
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Query

class FilterableFirebaseArray<T>(query: Query, parser: SnapshotParser<T>) : FirebaseArray<T>(query, parser) {

    val excludedKeys = HashSet<String?>()

    override fun onChildAdded(snapshot: DataSnapshot, previousChildKey: String?) {
        if (!excludedKeys.contains(snapshot.key))
            super.onChildAdded(snapshot, if (excludedKeys.contains(previousChildKey)) null else previousChildKey)
    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildKey: String?) {
        if (!excludedKeys.contains(snapshot.key))
            super.onChildChanged(snapshot, previousChildKey)
    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildKey: String?) {
        if (!excludedKeys.contains(snapshot.key))
            super.onChildMoved(snapshot, previousChildKey)
    }

    override fun onChildRemoved(snapshot: DataSnapshot) {
        if (!excludedKeys.contains(snapshot.key))
            super.onChildRemoved(snapshot)
    }

}