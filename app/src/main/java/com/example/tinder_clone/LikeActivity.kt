package com.example.tinder_clone

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.solver.widgets.analyzer.Direct
import androidx.core.view.marginStart
import androidx.core.view.setPadding
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class LikeActivity: AppCompatActivity(), CardStackListener {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    private lateinit var userDB: DatabaseReference

    private val cardStackView: CardStackView by lazy {
        findViewById<CardStackView>(R.id.cardStackView)
    }

    private val adapter = CardItemAdapter()
    private val cardItemList = mutableListOf<CardItem>()

    private val manager by lazy {
        CardStackLayoutManager(this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        userDB = Firebase.database.reference.child("Users")

        val  currentUserDB = userDB.child(getCurrentUserID())
        // DB에서 값을 가져오기
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child("name").value == null) {
                    showNameInputPopup()
                    return
                }
                // todo 유저 정보 갱신
                getUnselectedUsers()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

        initCardStackView()
        initSignOutButton()
        initMatchedListButton()
    }

    private fun initSignOutButton() {
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener {
            auth.signOut()
            LoginManager.getInstance().logOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun initMatchedListButton() {
        val matchedListButton = findViewById<Button>(R.id.matchedListButton)
        matchedListButton.setOnClickListener {
            val intent = Intent(this, MatchedUserActivity::class.java)
            startActivity(intent)
        }

    }

    private fun like() {
        val card = cardItemList[manager.topPosition - 1]
        cardItemList.removeAt(manager.topPosition - 1)

        userDB.child(card.userId).child("likeBy").child("like")
            .child(getCurrentUserID()).setValue(true)

        // todo 매칭이 된 시점?
        saveMatchIfOtherUserLikeMe(card.userId)

        Toast.makeText(this, "좋아요 ✅", Toast.LENGTH_SHORT).show()
    }

    private fun disLike() {
        val card = cardItemList[manager.topPosition - 1]
        cardItemList.removeAt(manager.topPosition - 1)

        userDB.child(card.userId).child("likeBy").child("disLike")
            .child(getCurrentUserID()).setValue(true)

        Toast.makeText(this, "싫어요 ❌", Toast.LENGTH_SHORT).show()
    }

    private fun saveMatchIfOtherUserLikeMe(otherUserId: String) {
        val isOtherUserExistMyLikeDB = userDB.child(getCurrentUserID()).child("likeBy").child("like").child(otherUserId)

        isOtherUserExistMyLikeDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == true) {
                    userDB.child(getCurrentUserID()).child("likeBy")
                        .child("match").child(otherUserId).setValue(true)

                    userDB.child(otherUserId).child("likeBy")
                        .child("match").child(getCurrentUserID()).setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) { }
        })
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) { }

    override fun onCardSwiped(direction: Direction?) {
        when (direction) {
            Direction.Right -> like()
            Direction.Left -> disLike()
        }
    }

    override fun onCardRewound() { }

    override fun onCardCanceled() { }

    override fun onCardAppeared(view: View?, position: Int) { }

    override fun onCardDisappeared(view: View?, position: Int) { }

    private fun initCardStackView() {
        cardStackView.layoutManager = manager
        cardStackView.adapter = adapter
    }

    private fun getUnselectedUsers() {
        userDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.child("userId").value != getCurrentUserID()
                    && snapshot.child("likeBy").child("like").hasChild(getCurrentUserID()).not()
                    && snapshot.child("likeBy").child("disLike").hasChild(getCurrentUserID()).not()) {

                    val userId = snapshot.child("userId").value.toString()
                    var name = "undefined"
                    if (snapshot.child("name").value != null) {
                        name = snapshot.child("name").value.toString()
                    }
                    val cardItem = CardItem(userId, name)
                    cardItemList.add(cardItem)

                    adapter.submitList(cardItemList)
                    adapter.notifyDataSetChanged()
                }

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                cardItemList.find { it.userId == snapshot.key }?.let {
                    it.name = snapshot.child("name").value.toString()
                }
                adapter.submitList(cardItemList)
                adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}

        })
    }

    private fun getCurrentUserID(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
        return auth.currentUser?.uid.orEmpty()
    }

    private fun showNameInputPopup() {
        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("닉네임을 입력해주세요")
            .setMessage("다른 사용자들에게 표시됩니다.")
            .setView(editText)
            .setPositiveButton("저장") {_, _ ->
                if (editText.text.isEmpty()) {
                    showNameInputPopup()
                    Toast.makeText(this, "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show()
                } else {
                    saveUserName(editText.text.toString())
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun saveUserName(name: String) {
        val userId = getCurrentUserID()
        val currentUserDB = userDB.child(userId)
        val user = mutableMapOf<String, Any>()
        user["userId"] = userId
        user["name"] = name
        currentUserDB.updateChildren(user)

        getUnselectedUsers()
    }
}

//출저 : https://github.com/yuyakaido/CardStackView