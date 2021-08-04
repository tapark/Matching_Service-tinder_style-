package com.example.tinder_clone

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity: AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var callbackManager: CallbackManager

    private val emailEditText: EditText by lazy {
        findViewById<EditText>(R.id.emailEditText)
    }
    private val passwordEditText: EditText by lazy {
        findViewById<EditText>(R.id.passwordEditText)
    }
    private val loginButton: AppCompatButton by lazy {
        findViewById<AppCompatButton>(R.id.loginButton)
    }
    private val signUpButton: AppCompatButton by lazy {
        findViewById<AppCompatButton>(R.id.signUpButton)
    }

    private val facebookLoginButton: LoginButton by lazy {
        findViewById<LoginButton>(R.id.facebookLoginButton)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        callbackManager = CallbackManager.Factory.create()

        FacebookSdk.sdkInitialize(getApplicationContext())
        AppEventsLogger.activateApp(this)

        initLoginButton()
        initSignUpButton()
        initEmailAndPasswordEditText()
        initFacebookLoginButton()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun initLoginButton() {
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) {
                    if (it.isSuccessful) {
                        handleSuccessLogin()
                    } else {
                        Toast.makeText(this, "로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun initSignUpButton() {
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initEmailAndPasswordEditText() {
        emailEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
        }

        passwordEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
        }
    }

    private fun initFacebookLoginButton() {
        facebookLoginButton.setPermissions("email", "public_profile")
        facebookLoginButton.registerCallback(callbackManager, object: FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {// 로그인 성공
                // facebook 토큰을 Firebase credential 로 가져옴
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                // credential 로 로그인
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this@LoginActivity) { task ->
                        if (task.isSuccessful) {
                            handleSuccessLogin()
                        } else {
                            Toast.makeText(this@LoginActivity, "페이스북 로그인이 실패하였습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

            }

            override fun onCancel() { /*로그인 과정에서 취소*/ }

            override fun onError(error: FacebookException?) {
                Toast.makeText(this@LoginActivity, "페이스북 로그인이 실패하였습니다.", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun handleSuccessLogin() {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = auth.currentUser?.uid.orEmpty()
        // reference = 최상위 child(하위분류 or 분류생성)
        val currentUserDB = Firebase.database.reference.child("Users").child(userId)
        val user = mutableMapOf<String, Any>()
        user["userId"] = userId
        currentUserDB.updateChildren(user)

        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        ActivityCompat.finishAffinity(this)
    }

}

