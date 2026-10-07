package vn.ca.vietvoiceoffline

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig

/**
 * LiteRT-LM bridge for the Official heavy pass. It prefers multimodal GPU execution,
 * then falls back through CPU/text modes so the fast translator remains usable.
 */
class HeavyTranslator(private val context: Context, private val modelPath: String) : AutoCloseable {
    @Volatile private var engine: Engine? = null
    @Volatile private var visionReady = false

    private fun create(backend: Backend, vision: Boolean): Engine {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            visionBackend = if (vision) backend else null,
            maxNumImages = if (vision) 1 else null,
            cacheDir = context.cacheDir.absolutePath
        )
        return Engine(config).also { it.initialize() }
    }

    @Synchronized
    fun initialize() {
        if (engine != null) return
        val errors = ArrayList<String>()
        val attempts = listOf(
            Pair<Backend, Boolean>(Backend.GPU(), true),
            Pair<Backend, Boolean>(Backend.CPU(), true),
            Pair<Backend, Boolean>(Backend.GPU(), false),
            Pair<Backend, Boolean>(Backend.CPU(), false)
        )
        for ((backend, withVision) in attempts) {
            try {
                engine = create(backend, withVision)
                visionReady = withVision
                return
            } catch (t: Throwable) {
                errors.add("${if (backend is Backend.GPU) "GPU" else "CPU"}${if (withVision) "+ảnh" else ""}: ${t.message}")
                try { engine?.close() } catch (_: Throwable) {}
                engine = null
            }
        }
        throw IllegalStateException("Không nạp được model LiteRT-LM. ${errors.joinToString("; ")}")
    }

    private fun config(): ConversationConfig = ConversationConfig(
        systemInstruction = Contents.of("Bạn là bộ dịch phim chạy cục bộ. Hình ảnh chỉ là ngữ cảnh hỗ trợ; không tự bịa thêm lời thoại hay tình tiết."),
        samplerConfig = SamplerConfig(topK = 20, topP = 0.9, temperature = 0.2),
        maxOutputToken = 128,
        thinkingConfig = ThinkingConfig(enableThinking = false)
    )

    @Synchronized
    fun translate(language: String, source: String, fastVi: String, scene: String, history: String, sceneImage: ByteArray?): String {
        val e = engine ?: throw IllegalStateException("Heavy AI chưa sẵn sàng")
        val prompt = HeavyPrompt.build(language, source, fastVi, scene, history)
        var raw: String
        if (visionReady && sceneImage != null && sceneImage.isNotEmpty()) {
            try {
                raw = e.createConversation(config()).use {
                    it.sendMessage(Contents.of(Content.ImageBytes(sceneImage), Content.Text(prompt))).toString()
                }
            } catch (_: Throwable) {
                // A model may load but reject image input. Translation must still continue.
                raw = e.createConversation(config()).use { it.sendMessage(prompt).toString() }
            }
        } else {
            raw = e.createConversation(config()).use { it.sendMessage(prompt).toString() }
        }
        return HeavyPrompt.cleanResponse(raw)
    }

    @Synchronized
    override fun close() { try { engine?.close() } finally { engine = null; visionReady = false } }
}
