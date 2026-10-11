package cn.xiaobai.acgfakamobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

private const val APP_VERSION_NAME = "0.2.0"
private const val APP_VERSION_CODE = 2

private val sections = listOf(
    "服务器配置", "插件管理", "订单管理", "商品与卡密", "用户管理", "网站设置", "服务器日志"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AdminApp(openUrl = { url ->
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }) } }
    }
}

private data class Bootstrap(
    val latestVersionName: String,
    val latestVersionCode: Int,
    val minSupportedVersionCode: Int,
    val forceUpdate: Boolean,
    val updateUrl: String,
    val announcementId: String,
    val announcementTitle: String,
    val announcementBody: String,
    val announcementForce: Boolean,
    val announcementEnabled: Boolean
)

private object MobileApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    suspend fun checkBootstrap(baseUrl: String): Bootstrap = withContext(Dispatchers.IO) {
        val normalized = normalizeBaseUrl(baseUrl)
        val request = Request.Builder()
            .url("$normalized/mobile-api/v1/bootstrap")
            .get()
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("服务器返回 HTTP ${response.code}")
            val body = response.body?.string() ?: error("服务器没有返回内容")
            val json = JSONObject(body)
            if (json.optBoolean("ok", false).not()) error(json.optString("message", "服务器拒绝请求"))
            val app = json.optJSONObject("app") ?: error("缺少 app 配置")
            val announcement = json.optJSONObject("announcement") ?: JSONObject()
            Bootstrap(
                latestVersionName = app.optString("latest_version_name", APP_VERSION_NAME),
                latestVersionCode = app.optInt("latest_version_code", APP_VERSION_CODE),
                minSupportedVersionCode = app.optInt("min_supported_version_code", 1),
                forceUpdate = app.optBoolean("force_update", false),
                updateUrl = app.optString("update_url", ""),
                announcementId = announcement.optString("id", ""),
                announcementTitle = announcement.optString("title", ""),
                announcementBody = announcement.optString("body", ""),
                announcementForce = announcement.optBoolean("force", false),
                announcementEnabled = announcement.optBoolean("enabled", false)
            )
        }
    }

    private fun normalizeBaseUrl(input: String): String {
        val value = input.trim().trimEnd('/')
        val uri = URI(value)
        require(uri.scheme == "https") { "只允许 HTTPS 服务器地址" }
        require(!uri.host.isNullOrBlank()) { "服务器地址格式不正确" }
        require(uri.userInfo == null) { "地址中不能包含用户名或密码" }
        return value
    }
}

@Composable
private fun AdminApp(openUrl: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("服务器配置") }
    var status by remember { mutableStateOf("请填写服务器 HTTPS 地址，然后检查版本、公告和连接策略。") }
    var busy by remember { mutableStateOf(false) }
    var bootstrap by remember { mutableStateOf<Bootstrap?>(null) }
    var updateDialog by remember { mutableStateOf(false) }
    var announcementDialog by remember { mutableStateOf(false) }
    var updateBlocked by remember { mutableStateOf(false) }
    var announcementSeen by remember { mutableStateOf(false) }

    fun checkServer() {
        scope.launch {
            busy = true
            status = "正在检查服务器版本策略…"
            try {
                val result = MobileApi.checkBootstrap(serverUrl)
                bootstrap = result
                val mustUpdate = APP_VERSION_CODE < result.minSupportedVersionCode ||
                    (result.forceUpdate && APP_VERSION_CODE < result.latestVersionCode)
                updateBlocked = mustUpdate
                if (mustUpdate) {
                    updateDialog = true
                    announcementDialog = false
                    status = "当前客户端版本过低，必须更新后才能继续。"
                } else {
                    status = "版本检查完成：本地 $APP_VERSION_NAME ($APP_VERSION_CODE)，服务器最新 ${result.latestVersionName} (${result.latestVersionCode})。"
                    if (result.announcementEnabled && result.announcementId.isNotBlank()) {
                        announcementDialog = true
                    }
                }
            } catch (e: Exception) {
                status = "检查失败：${e.message ?: "未知错误"}。请确认服务器已部署移动端接口。"
            } finally {
                busy = false
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("异次元管理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("ACG-Faka · 原生 Android 客户端 · v$APP_VERSION_NAME",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        bottomBar = {
            NavigationBar {
                listOf("概览", "插件", "订单", "设置").forEachIndexed { i, item ->
                    val target = when (item) {
                        "概览" -> "服务器配置"
                        "插件" -> "插件管理"
                        "订单" -> "订单管理"
                        else -> "网站设置"
                    }
                    NavigationBarItem(
                        selected = selected == target,
                        onClick = { selected = target },
                        icon = {
                            Icon(when (i) {
                                0 -> Icons.Default.Dashboard
                                1 -> Icons.Default.Extension
                                2 -> Icons.Default.ReceiptLong
                                else -> Icons.Default.Settings
                            }, contentDescription = item)
                        },
                        label = { Text(item) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("管理模块", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            sections.forEach { name ->
                ElevatedCard(onClick = { selected = name }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconFor(name), contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (name == "服务器配置") "服务器地址、版本检查、公告策略"
                                else "原生页面入口已预留，业务数据需接入受保护的服务端 API",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "打开")
                    }
                }
            }
            HorizontalDivider()
            Text(selected, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (selected == "服务器配置") {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("服务器根地址") },
                    placeholder = { Text("https://shop.example.com") },
                    singleLine = true
                )
                Button(enabled = !busy && serverUrl.isNotBlank(), onClick = { checkServer() }) {
                    Text(if (busy) "检查中…" else "检查版本与公告")
                }
                Text("客户端版本：$APP_VERSION_NAME（版本号 $APP_VERSION_CODE）",
                    style = MaterialTheme.typography.bodySmall)
                bootstrap?.let {
                    Text("服务器最新版本：${it.latestVersionName}（${it.latestVersionCode}）")
                    Text("最低允许版本号：${it.minSupportedVersionCode}")
                    Text("强制更新：${if (it.forceUpdate) "开启" else "关闭"}")
                    Text("公告：${if (it.announcementEnabled) "已启用" else "未启用"}")
                }
            } else {
                Text(
                    "这里是本地原生界面，不加载 ACG-Faka 网页。模块的数据操作将在核对实际后台权限和业务接口后接入。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "不会直接暴露数据库、任意文件读写或 Shell 执行接口；卡密、用户资料和日志需要按权限脱敏。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ElevatedCard {
                Column(Modifier.padding(14.dp)) {
                    Text("连接状态", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(status)
                }
            }
        }
    }

    if (updateDialog) {
        val policy = bootstrap
        AlertDialog(
            onDismissRequest = { if (!updateBlocked) updateDialog = false },
            title = { Text("需要更新客户端") },
            text = {
                Text("当前版本 $APP_VERSION_NAME（$APP_VERSION_CODE）不满足服务器要求。最新版本：${policy?.latestVersionName ?: "未知"}。请更新后继续使用。")
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = policy?.updateUrl.orEmpty()
                    if (url.isNotBlank()) openUrl(url)
                }) { Text("前往更新") }
            },
            dismissButton = {
                if (!updateBlocked) TextButton(onClick = { updateDialog = false }) { Text("稍后") }
            }
        )
    }

    if (announcementDialog && bootstrap != null) {
        val item = bootstrap!!
        AlertDialog(
            onDismissRequest = { if (!item.announcementForce) announcementDialog = false },
            title = { Text(item.announcementTitle.ifBlank { "系统公告" }) },
            text = { Text(item.announcementBody) },
            confirmButton = {
                TextButton(onClick = {
                    announcementSeen = true
                    announcementDialog = false
                }) { Text(if (item.announcementForce) "我已知晓" else "知道了") }
            },
            dismissButton = {
                if (!item.announcementForce) {
                    TextButton(onClick = { announcementDialog = false }) { Text("关闭") }
                }
            }
        )
    }
}

private fun iconFor(name: String) = when (name) {
    "服务器配置" -> Icons.Default.Dns
    "插件管理" -> Icons.Default.Extension
    "订单管理" -> Icons.Default.ReceiptLong
    "商品与卡密" -> Icons.Default.Inventory2
    "用户管理" -> Icons.Default.People
    "网站设置" -> Icons.Default.Settings
    else -> Icons.Default.Article
}
