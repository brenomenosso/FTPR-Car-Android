package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    private var verificationId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        verifyLoggedUser()
        setUpView()
    }

    private fun verifyLoggedUser() {
        if (auth.currentUser != null) {
            Log.d("USER", "Usuário está logado, redirecionando para a página de lista")
            startActivity(MainActivity.newIntent(this))
        }
    }

    private fun setUpView() {
        supportActionBar?.title = getString(R.string.login)

        binding.sendSms.setOnClickListener {
            sendSmsOnClick()
        }

        binding.verify.setOnClickListener{
            Log.d("SMS", "Clicou no botão")
            verifyOnClick()
        }

    }

    private fun verifyOnClick() {
        Log.d("SMS", "Verificando código")
        val verificationCode = binding.etSmsCode.text.toString()

        if (verificationCode.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.please_enter_phone_number),
                Toast.LENGTH_SHORT
            ).show()
            binding.etSmsCode.requestFocus()
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)
        auth.signInWithCredential(credential)
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    Log.d("SMS", "Suscesso na autenticação")
                    startActivity(MainActivity.newIntent(this))
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.coul_not_authenticate),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun sendSmsOnClick() {
        val phoneNumber = binding.etPhoneNumber.text.toString()

        if (phoneNumber.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.please_enter_phone_number),
                Toast.LENGTH_SHORT
            ).show()
            binding.etPhoneNumber.requestFocus()
            return
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object: PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    TODO("Not yet implemented")
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.could_not_send_code),
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@LoginActivity.verificationId = verificationId
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.verification_code_sent),
                        Toast.LENGTH_LONG
                    ).show()
                    binding.tlSmsCode.visibility = View.VISIBLE
                    binding.verify.visibility = View.VISIBLE

                }

            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }


}