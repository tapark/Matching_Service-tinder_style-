# 매칭서비스 tinder style

### Firebase Authentication - Email Login
초기설정(공통)
~~~ kotlin
// build.gradle 에 추가
implementation 'com.google.firebase:firebase-auth-ktx'
implementation 'com.facebook.android:facebook-login:8.2.0'
~~~

~~~kotlin
// AndroidManifest.xml 에 추가
<meta-data
	android:name="com.facebook.sdk.ApplicationId"
	android:value="@string/facebook_app_id"/>
<activity
	android:name="com.facebook.FacebookActivity"
	android:configChanges= "keyboard|keyboardHidden|screenLayout|screenSize|orientation"
	android:label="@string/app_name" />
<activity
	android:name="com.facebook.CustomTabActivity"
	android:exported="true">
	<intent-filter>
		<action android:name="android.intent.action.VIEW" />
		<category android:name="android.intent.category.DEFAULT" />
		<category android:name="android.intent.category.BROWSABLE" />
		<data android:scheme="@string/fb_login_protocol_scheme" />
	</intent-filter>
</activity>
~~~
이메일 로그인
~~~kotlin
// FirebaseAuth 생성
private var auth: FirebaseAuth = Firebase.auth /*코틀린*/
private var auth: FirebaseAuth = FirebaseAuth.getInstance() /*자바*/
~~~
~~~kotlin
val email = emailEditText.text.toString()
val password = passwordEditText.text.toString()
// email, password 로 계정 생성
auth.createUserWithEmailAndPassword(email, password)
	.addOnCompleteListener(this) { task ->
		if (task.isSuccessful) {
			// 계정생성 완료
		} else {
			// 계정생성 실패 -> 이미가입된 or 아이디 비밀번호 형식
		}
	}
// email, password 로 로그인
auth.signInWithEmailAndPassword(email, password)
	.addOnCompleteListener(this) {
		if (it.isSuccessful) {
			// 로그인 성공 -> 데이터를 저장하고 다음 Activity로
			handleSuccessLogin()
		} else {
			// 로그인 실패 -> 실패 메세지
		}
~~~

### Firebase Authentication - Facebook Login
페이스북 로그인
~~~kotlin
// 페이스북 로그인 버튼 생성
<com.facebook.login.widget.LoginButton
	android:id="@+id/facebookLoginButton"
	android:layout_width="0dp"
	android:layout_height="wrap_content"
	app:layout_constraintTop_toBottomOf="@id/loginButton"
	app:layout_constraintStart_toStartOf="parent"
	app:layout_constraintEnd_toEndOf="parent"
	app:layout_constraintBottom_toBottomOf="parent"/>
~~~

~~~kotlin
// 페이스북 로그인 버튼
private val facebookLoginButton: LoginButton by lazy {
    findViewById<LoginButton>(R.id.facebookLoginButton)
// 권한 설정 (가져올 정보)
facebookLoginButton.setPermissions("email", "public_profile")
facebookLoginButton.registerCallback(callbackManager, object: FacebookCallback<LoginResult> {
	override fun onSuccess(result: LoginResult) {// 로그인 성공
		// facebook 토큰을 Firebase credential 로 가져옴
		val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
		// Firebase Auth에서 credential로 로그인
		auth.signInWithCredential(credential)
			.addOnCompleteListener(this@LoginActivity) { task ->
				if (task.isSuccessful) {
					// 로그인 성공 -> 데이터를 저장하고 다음 Activity로
					handleSuccessLogin()
				} else {
					// 로그인 실패 -> 실패 메세지
				}
			}

	}
	override fun onCancel() { /*로그인 과정에서 취소*/ }

	override fun onError(error: FacebookException?) {
		//로그인 실패
	}

})
~~~
~~~kotlin
// 현재 로그인된 유저정보를 가져오기(없다면 null)
private val currentUser = auth.currentUser?.uid.orEmpty()
~~~

### Firebase Realtime Database
~~~kotlin
// build.gradle 에 추가
implementation 'com.google.firebase:firebase-database-ktx'
~~~

~~~kotlin
// Realtime Database 생성
userDB = Firebase.database.reference.child("Users")
// reference : Database 최상위
// 하위 Key 값에 .child("key")로 접근(or 없으면 생성)
~~~
데이터를 읽는 3가지 방법
1. addValueEventListener()
   데이터 경로 전체를 읽고 변경사항에 대해 수신대기
2. addListenerForSingleValueEvent()
   한번만 호출되고 수신대기 할 필요 없는 데이터에 사용
3. addChildEventListener()
   경로의 특정 하위(child)의 변경사항에 대해 수신대기
~~~kotlin
// 호출되는 snapshot을 통해 데이터를 읽어올 수 있다.
// 나를 만난적이 없는 유저를 선택하여 ListAdapter로 submit
val userDB = Firebase.database.reference.child("Users")
private fun getUnselectedUsers() {
	userDB.addChildEventListener(object : ChildEventListener {
		override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
			if (snapshot.child("userId").value != getCurrentUserID()
				&& snapshot.child("likeBy").child("like").hasChild(getCurrentUserID()).not()
				&& snapshot.child("likeBy").child("disLike").hasChild(getCurrentUserID()).not()) {
				// 유저 아이디를 저장
				val userId = snapshot.child("userId").value.toString()
				var name = "undefined"
				if (snapshot.child("name").value != null) {
					// 유저 이름을 저장
					name = snapshot.child("name").value.toString()
				}
				val cardItem = CardItem(userId, name)
				cardItemList.add(cardItem)
				// data class List에 추가하여 adapter에 submit 후 적용
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
~~~

### Open Source Library 적용 (tinder style card slide animation)
출처 : https://github.com/yuyakaido/CardStackView
~~~kotlin
//setup
val cardStackView = findViewById<CardStackView>(R.id.card_stack_view)
cardStackView.layoutManager = CardStackLayoutManager()
cardStackView.adapter = CardStackAdapter() // recyclerView Adapter
~~~
~~~kotlin
// swipe 방향에 따른 action을 정의
override fun onCardSwiped(direction: Direction?) {
	when (direction) {
		Direction.Right -> like()
		Direction.Left -> disLike()
	}
}
~~~

### RecyclerView, Adapter
~~~kotlin
// Activity에 RecyclerView 추가
private fun initMatchedUserRecyclerView() {
	val recyclerView = findViewById<RecyclerView>(R.id.matchedUserRecyclerView)

	recyclerView.layoutManager = LinearLayoutManager(this)
	recyclerView.adapter = MatchedUserAdapter() // adapter class 생성
}
~~~

~~~kotlin
// MatchedUserAdapter.kt 생성
class MatchedUserAdapter: ListAdapter<CardItem, MatchedUserAdapter.ViewHolder>(diffUtil) {

    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {

		// layout의 하위 View들에 List의 어떤 값들이 들어가는지?
        fun bind(cardItem: CardItem) {
            view.findViewById<TextView>(R.id.yourName).text = cardItem.name
        }
    }

	// 각 List에 보여질 layout(view)을 호출하여 ViewHolder(view)로 반환
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_match, parent, false)
        return ViewHolder(view)
    }
	// 리스트의 현재 포지션의 값을 View에 표시
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

	// 변경사항에 대한 업데이트를 효율적으로
    companion object  {
        val diffUtil = object: DiffUtil.ItemCallback<CardItem>() {
            override fun areItemsTheSame(oldItem: CardItem, newItem: CardItem): Boolean {
                return oldItem.userId == newItem.userId
            }

            override fun areContentsTheSame(oldItem: CardItem, newItem: CardItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
~~~