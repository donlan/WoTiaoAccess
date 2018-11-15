package gui.dong.wotiaoaccess

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var accessFlagTv: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        accessFlagTv = findViewById(R.id.access_flag)
        accessFlagTv.text =
                if (AccessibilityUtils.isAccessibilitySettingsOn(WuTiaoAccessibility::class.java.simpleName, this)) {
                    "辅助服务已开启"
                } else {
                    "去开启辅助服务"
                }
        accessFlagTv.setOnClickListener {
            AccessibilityUtils.openAccessibility(
                WuTiaoAccessibility::class.java.simpleName,
                this
            )
        }

        Permission.instance().check({ },this,Collections.singletonList(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        val voteTimeSeekBar = findViewById<SeekBar>(R.id.time_vote_seek)
        val voteTimeTv  = findViewById<TextView>(R.id.vote_time_tv)
        val videoTimeSeekbar = findViewById<SeekBar>(R.id.video_time_vote_seek)
        val videoTimeTv = findViewById<TextView>(R.id.video_vote_time_tv)
        voteTimeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                getSharedPreferences("wutiao", Context.MODE_PRIVATE)
                    .edit().putInt("vote_time", progress)
                    .commit()
                WuTiaoAccessibility.voteTime = progress
                voteTimeTv.text = progress.toString()
            }
        })
        videoTimeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                getSharedPreferences("wutiao", Context.MODE_PRIVATE)
                    .edit().putInt("video_vote_time", progress)
                    .commit()
                WuTiaoAccessibility.videoVoteTime = progress
                videoTimeTv.text = progress.toString()
            }
        })
        findViewById<ToggleButton>(R.id.vote_auto_mode).setOnCheckedChangeListener { _, isChecked ->
            WuTiaoAccessibility.autoMode = isChecked
        }

        WuTiaoAccessibility.voteTime = getSharedPreferences("wotiao",Context.MODE_PRIVATE).getInt("vote_time",5)
        voteTimeSeekBar.progress = WuTiaoAccessibility.voteTime
        voteTimeTv.text = WuTiaoAccessibility.voteTime.toString()

        WuTiaoAccessibility.videoVoteTime = getSharedPreferences("wotiao",Context.MODE_PRIVATE).getInt("video_vote_time",5)
        videoTimeSeekbar.progress = WuTiaoAccessibility.videoVoteTime
        videoTimeTv.text = WuTiaoAccessibility.videoVoteTime.toString()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Permission.instance().handleRequestResult(this,requestCode, permissions, grantResults)
    }
    override fun onDestroy() {
        super.onDestroy()
        Permission.instance().onDestroy()
    }
}
