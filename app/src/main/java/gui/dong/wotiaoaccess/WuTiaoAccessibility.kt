package gui.dong.wotiaoaccess

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Message
import android.support.v4.view.ViewPager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.blankj.ALog

/**
 * @author 梁桂栋
 * @date 2018/11/12  14:52.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: gui.dong.wotiaoaccess
 * @version 1.0
 */
class WuTiaoAccessibility : AccessibilityService() {

    companion object {
        var voteTime = 5
        var autoMode = false
        var inVotePager = false
        var nextHasClick = false
        var currentContentCode = 0
        const val codeFinder = 2
        const val codeNext = 3
        const val nextDelayCheck = 4
    }


    private var recheckVote = false
    private var needVoteClick = false
    private var inCheckShouldVote = false
    private var canVoteClick = false
    private var voteCount = 0
    private var visitCount = 0
    private var handler: MyHandler? = null
    private var accessTimeGap = 0L
    private var normalNextTime = 3000L
    private var voteNextTime = 11000L
    private var timeNodeInfo: AccessibilityNodeInfo? = null
    private var titleNodeInfo: AccessibilityNodeInfo? = null

    override fun onInterrupt() {
    }

    override fun onCreate() {
        super.onCreate()
        handler = MyHandler()
        handler?.callBack = object : HandlerCallback {
            override fun onHandlerCall(code: Int, arg: Int) {
                if (!inCheckShouldVote && arg == visitCount) {
                    checkShouldVote()
                }
            }
        }
        ALog.init(this)
            .setLogSwitch(BuildConfig.DEBUG)
            .setConsoleSwitch(BuildConfig.DEBUG)
            .setGlobalTag("WuTiao")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.callBack = null
        handler = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        voteFinder(event)
    }

    private fun voteFinder(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }
        ALog.d(
            AccessibilityEvent.eventTypeToString(event.eventType),
            event.className,
            event.source?.text
        )
        //投票过早引发的Toast提醒
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && event.text.toString().contains("请审阅内容后再投票")) {
            if (autoMode && recheckVote) {
                voteClick(5000)
            }
            recheckVote = true
            return
        }
        //已投票在点击投票引发的Toast提醒
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && event.text.toString().contains("已投票")) {
            nextClick(1000)
            return
        }
        //投票数未达100，当条内容审核时间到达的Toast提醒（当日审核数达100后不在提醒）
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && event.text.toString().contains("审核内容")) {
            canVoteClick = true
            if (autoMode) {
                voteNextTime = 1000
                checkShouldVote()
                voteNextTime = 11000
            }
            return
        }
        //投票成功自动换一篇
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && event.text.toString().contains("投票成功")) {
            val nextNodes =
                rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.kingnet.fiveline:id/" + "mLayoutNext")
            if (nextNodes.isEmpty()) {
                return
            }
            recheckVote = false
            nextNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        if (event.source == null) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && nextHasClick) {
            checkShouldVote()
            return
        }

        //停留在主页（首页）是会有ViewPager的轮播，此时引发的事件不处理
        if (event.className.endsWith(ViewPager::class.java.name) ||
            event.className.endsWith("MainActivity")
        ) {
            inVotePager = false
            nextHasClick = false
            return
        }
        //点击进入发现者投票页面时引发的事件不处理
        if (event.text.toString().endsWith("发现内容") ||
            event.className.endsWith("FinderBusinessActivity")
        ) {
            inVotePager = true
            return
        }
        if (!inVotePager) {
            return
        }

        //点击了【标记】
        //视频播放进度条更新事件不处理
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED && event.className.contains("ProgressBar")) {
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && event.text.toString().endsWith("标记")) {
            return
        }
        //页面滑动事件不处理
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ) {
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val sourceText = event.source.text
            if ("标记" == sourceText || "关注" == sourceText || "发现者投票" == sourceText) {
                return
            }
            var timeOk: Boolean
            if (sourceText == null) {
                timeOk = checkTimeNodeInfo(event.source)
                if (timeNodeInfo == null) {
                    timeOk = checkTimeNodeInfo(event.source.parent)
                }
            } else {
                val parent = event.source.parent
                timeOk = checkTimeNodeInfo(event.source)
                if (parent != null) {
                    timeOk = checkTimeNodeInfo(parent)
                }
            }
            if (!inCheckShouldVote && timeOk) {
                checkShouldVote()
            }
            return
        }
        //点击了【换一篇】
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && event.text.toString().contains("换一篇")) {
            visitCount++
            nextHasClick = true
            inCheckShouldVote = false
            canVoteClick = false
            needVoteClick = false
            timeNodeInfo = null
            titleNodeInfo = null
            //ALog.e("换一篇")
            //为确保点击换一篇后引发的TYPE_WINDOW_CONTENT_CHANGED小于 minClickChangeCount
            //而不能自动点击下一篇的问题
            handler?.sendMessageDelayed(
                handler?.obtainMessage(nextDelayCheck, visitCount, 0)
                , normalNextTime - 1000
            )
            recheckVote = false
            return
        }
    }

    private fun voteClick(delay: Long) {
        val voteNode = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.kingnet.fiveline:id/" + "voteBtn")
        if (!voteNode.isEmpty()) {
            handler?.sendMessageDelayed(
                handler?.obtainMessage(codeFinder, currentContentCode, 0, voteNode[0])
                , delay
            )
        }
        recheckVote = true
    }

    private fun nextClick(delay: Long) {
        val nextNodes =
            rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.kingnet.fiveline:id/" + "mLayoutNext")
        if (!nextNodes.isEmpty()) {
            handler?.sendMessageDelayed(
                handler?.obtainMessage(codeNext, currentContentCode, 0, nextNodes[0])
                , delay
            )
        }
        recheckVote = false
    }

    private fun checkTimeNodeInfo(nodeInfo: AccessibilityNodeInfo?): Boolean {
        if (nodeInfo == null) {
            return false
        }
        //内容标题节点
        var titleNodes =
            nodeInfo.findAccessibilityNodeInfosByViewId("com.kingnet.fiveline:id/" + "tvVideoDes")
        if (titleNodes.isEmpty())
            titleNodes =
                    nodeInfo.findAccessibilityNodeInfosByViewId("com.kingnet.fiveline:id/" + "mTextConsultTitle")
        if (!titleNodes.isEmpty()) {
            titleNodeInfo = titleNodes[0]
        }

        //内容时间显示的节点
        var node = nodeInfo.findAccessibilityNodeInfosByViewId("com.kingnet.fiveline:id/" + "tvDate")
        if (node.isEmpty()) {
            node = nodeInfo.findAccessibilityNodeInfosByViewId("com.kingnet.fiveline:id/" + "mTextConsultTime")
        }
        if (!node.isEmpty()) {
            timeNodeInfo = node[0]
            ALog.e(timeNodeInfo?.text, titleNodeInfo?.text)
            return true
        }
        return false
    }

    private fun checkShouldVote() {
        inCheckShouldVote = true
        val source = rootInActiveWindow
        if (source != null) {
            //如果有时间显示，解析时间，并根据时间进行投票判断
            if (timeNodeInfo != null) {
                finder(timeNodeInfo!!.text, titleNodeInfo?.text)
            } else {
                //无时间显示，自动【换一篇】
                nextClick(accessTimeGap)
            }
        }
    }

    private fun finder(timeStr: CharSequence, title: CharSequence?) {
        if (timeStr.contains("刚刚")) {
            needVoteClick = true
        }
        if (timeStr.contains("分钟")) {
            val sb = StringBuilder()
            for (i in 0 until timeStr.length) {
                if (timeStr[i] in '0'..'9') {
                    sb.append(timeStr[i])
                }
            }
            val time = sb.toString().toInt()
            if (time <= voteTime) {
                needVoteClick = true
            }
        }
        ALog.e(voteTime, timeStr, title)
        if (needVoteClick) {
            if (autoMode) {
                accessTimeGap = voteNextTime
                if (canVoteClick) {
                    accessTimeGap = 500
                }
                voteClick(accessTimeGap)
            }
            voteCount++
            Shooter.shoot(applicationContext, "五条发现者投票", "符合投票【${title}】($voteCount)次")
        } else {
            nextClick(normalNextTime)
        }
    }


    class MyHandler : Handler() {
        var callBack: HandlerCallback? = null
        override fun handleMessage(msg: Message?) {
            if (msg?.what == nextDelayCheck) {
                callBack?.onHandlerCall(nextDelayCheck, msg.arg1)
            } else if (msg?.obj != null) {
                val node = msg.obj as AccessibilityNodeInfo
                if (msg.arg1 == currentContentCode)
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    interface HandlerCallback {
        fun onHandlerCall(code: Int, arg: Int)
    }
}