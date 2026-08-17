package cl.controlacceso

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cl.controlacceso.databinding.ActivityWelcomeBinding
import cl.controlacceso.util.UiHelpers

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UiHelpers.liftContentAboveNavBar(binding.root)

        binding.versionText.text = getString(R.string.welcome_version, BuildConfig.VERSION_NAME)

        binding.continueButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}
