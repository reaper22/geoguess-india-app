package com.example.geoguessindia

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.geoguessindia.firebase.AuthHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SignupActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnSignup: MaterialButton
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirm = findViewById(R.id.etConfirm)
        btnSignup = findViewById(R.id.btnSignup)
        progress = findViewById(R.id.progress)

        btnSignup.setOnClickListener { attemptSignup() }
        findViewById<TextView>(R.id.tvGoLogin).setOnClickListener { finish() }
    }


    private fun passwordProblem(p: String): String? = when {
        p.length < 8 -> "At least 8 characters"
        p.none { it.isUpperCase() } -> "Add an uppercase letter"
        p.none { it.isLowerCase() } -> "Add a lowercase letter"
        p.none { it.isDigit() } -> "Add a number"
        else -> null
    }

    private fun attemptSignup() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirm = etConfirm.text.toString()

        if (name.length < 2) { etName.error = "Enter a player name"; return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"; return
        }
        val problem = passwordProblem(password)
        if (problem != null) { etPassword.error = problem; return }
        if (password != confirm) { etConfirm.error = "Passwords do not match"; return }

        setLoading(true)
        AuthHelper.signUp(name, email, password) { success, error ->
            setLoading(false)
            if (success) {
                Toast.makeText(this, "Account created. Welcome, $name!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnSignup.isEnabled = !loading
    }
}