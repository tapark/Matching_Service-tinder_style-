package com.example.tinder_clone

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import org.w3c.dom.Text

class SignUpActivity: AppCompatActivity() {

    private val emailEditText: EditText by lazy {
        findViewById<EditText>(R.id.emailEditText)
    }
    private val passwordEditText: EditText by lazy {
        findViewById<EditText>(R.id.passwordEditText)
    }
    private val password2EditText: EditText by lazy {
        findViewById<EditText>(R.id.password2EditText)
    }
    private val nameEditText: EditText by lazy {
        findViewById<EditText>(R.id.nameEditText)
    }
    private val signUpButton: AppCompatButton by lazy {
        findViewById<AppCompatButton>(R.id.signUpButton)
    }
    private val profileImageView: ImageView by lazy {
        findViewById<ImageView>(R.id.profileImageView)
    }
    private val nameTextView: TextView by lazy {
        findViewById<TextView>(R.id.nameTextView)
    }
    private val profileAddTextView: TextView by lazy {
        findViewById<TextView>(R.id.profileAddTextView)
    }
    private val progressBar: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.progressBar)
    }

    private var localImageUrl: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var currentUserDB: DatabaseReference

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = Firebase.auth
        storage = Firebase.storage

        initProfileImage()

        initSignUpButton()

        initTextChangeListener()

        nameEditText.addTextChangedListener {
            nameTextView.text = "${nameEditText.text}\n"
        }

    }

    private fun initTextChangeListener() {
        emailEditText.addTextChangedListener {
            editTextEmptyCheck()
        }
        passwordEditText.addTextChangedListener {
            editTextEmptyCheck()
        }
        password2EditText.addTextChangedListener {
            editTextEmptyCheck()
        }
        nameEditText.addTextChangedListener {
            editTextEmptyCheck()
        }
    }

    private fun editTextEmptyCheck() {
        val enable = emailEditText.text.isNotEmpty()
                && passwordEditText.text.isNotEmpty()
                && password2EditText.text.isNotEmpty()
                && nameEditText.text.isNotEmpty()
        signUpButton.isEnabled = enable
    }

    private fun initSignUpButton() {
        signUpButton.setOnClickListener {
            Log.d("Teddy", "회원가입버튼이 눌렸습니다.")
            if (password2EditText.text.toString() != passwordEditText.text.toString()) {
                Toast.makeText(this, "비밀번호 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (passwordEditText.text.toString().length < 8) {
                Toast.makeText(this, "비밀번호는 8자 이상이여야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        signUpSuccessHandler()
                    } else {
                        Toast.makeText(this, "이미 가입된 이메일입니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            Log.d("Teddy", "계정생성 성공?")
        }
    }

    private fun signUpSuccessHandler() {
        progressBar.isVisible = true
        val userId = auth.currentUser?.uid.orEmpty()
        val name = nameEditText.text.toString()

        if (localImageUrl != null) {
            val uri = localImageUrl ?: return
            uploadImageToStorage(uri,
                successHandler = {
                initDatabase(userId, name, it)
            },
                errorHandler = {
                Toast.makeText(this, "이미지 업로드 실패.", Toast.LENGTH_SHORT).show()
            })
        } else {
            initDatabase(userId, name, "")
        }

    }

    private fun uploadImageToStorage(uri: Uri, successHandler: (String) -> Unit, errorHandler: () -> Unit) {
        val userId = auth.currentUser?.uid.orEmpty()
        val fileName = "${userId}_${System.currentTimeMillis()}.png"

        storage.reference.child("profile").child(fileName)
            .putFile(uri)
            .addOnCompleteListener { it ->
                if (it.isSuccessful) {
                    storage.reference.child("profile").child(fileName)
                        .downloadUrl.addOnSuccessListener { uri ->
                            successHandler(uri.toString())
                        }.addOnFailureListener {
                            errorHandler()
                        }
                } else {
                    errorHandler()
                }
            }
    }

    private fun initDatabase(userId: String, name: String, imageUrl: String) {
        currentUserDB = Firebase.database.reference.child("Users").child(userId)
        val user = mutableMapOf<String, Any>("userId" to userId, "name" to name, "url" to imageUrl)
        currentUserDB.updateChildren(user)

        Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        progressBar.isVisible = false
        startActivity(intent)
    }

    private fun initProfileImage() {
        profileAddTextView.setOnClickListener {
            // 파일 업로드 권한
            when {
                ContextCompat.checkSelfPermission(this, STORAGE_PERMISSION)
                        == PackageManager.PERMISSION_GRANTED -> { /*권한이 허용된 경우*/
                    startContentProvider()
                }
                shouldShowRequestPermissionRationale(STORAGE_PERMISSION) -> { /*권한을 거절한 경우*/
                    showPermissionContextPopup()
                }
                else -> {  /*처음 권한을 요청*/
                    requestPermissions(arrayOf(STORAGE_PERMISSION), REQUEST_CODE)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startContentProvider()
                } else {
                    Toast.makeText(this, "권한을 거부하셨습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun startContentProvider() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, INTENT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            INTENT_REQUEST_CODE -> {
                val uri = data?.data
                if (uri != null) {
                    profileImageView.setImageURI(uri)
                    profileAddTextView.text = ""
                    localImageUrl = uri
                } else {
                    Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun showPermissionContextPopup() {
        AlertDialog.Builder(this).setTitle("권한이 필요합니다.")
            .setMessage("프로필 사진 등록")
            .setPositiveButton("동의") { _, _ ->
                requestPermissions(arrayOf(STORAGE_PERMISSION), REQUEST_CODE)
            }
            .create().show()
    }

    companion object {
        const val STORAGE_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        const val REQUEST_CODE = 1000
        const val INTENT_REQUEST_CODE = 2000
    }
}