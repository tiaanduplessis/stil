package app.stil.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView

open class FixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "Fixture opened"; textSize = 28f })
    }

    override fun onResume() {
        super.onResume()
        sendBroadcast(Intent("app.stil.launcher.test.FIXTURE_OPENED").setPackage("app.stil.launcher")
            .putExtra("activity", javaClass.name))
    }
}

class SecondFixtureActivity : FixtureActivity()
class ThirdFixtureActivity : FixtureActivity()
class FourthFixtureActivity : FixtureActivity()
class FifthFixtureActivity : FixtureActivity()
class SixthFixtureActivity : FixtureActivity()
