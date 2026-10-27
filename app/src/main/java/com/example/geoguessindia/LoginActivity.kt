package com.example.geoguessindia

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.geoguessindia.firebase.AuthHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progress = findViewById(R.id.progress)


        btnLogin.setOnClickListener { attemptLogin() }


        findViewById<TextView>(R.id.tvGoSignup).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        findViewById<TextView>(R.id.tvForgot).setOnClickListener { resetPassword() }
    }

    private fun attemptLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            return
        }
        if (password.length < 6) {
            etPassword.error = "At least 6 characters"
            return
        }

        setLoading(true)
        AuthHelper.login(email, password) { success, error ->
            setLoading(false)
            if (success) {
                goHome()
            } else {

                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun resetPassword() {
        val email = etEmail.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter your email above first"
            return
        }
        setLoading(true)
        AuthHelper.sendPasswordReset(email) { success, error ->
            setLoading(false)
            if (success) {

                AlertDialog.Builder(this)
                    .setTitle("Check your email")
                    .setMessage("If an account exists for $email, a password reset link is on its way. Check your spam folder too.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }
}