package com.lml.control

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.SamplerConfig

object LocalAgentRuntime {
    interface JavaCallback {
        fun onReply(text: String)
        fun onFailure(message: String)
    }

    @JvmStatic
    fun replyAsync(context: Context, query: String, allowedApps: Int, scenarios: Int, callback: JavaCallback) {
        Thread {
            val modelFile = ModelDownloadManager.getModelFile(context)
            if (!ModelDownloadManager.isInstalled(context)) {
                callback.onReply("Le moteur local n’est pas encore téléchargé. Ouvrez Modèle local pour installer Qwen3 0.6B, ou utilisez l’aide locale disponible.")
                return@Thread
            }
            try {
                Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
                val config = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = context.cacheDir.absolutePath
                )
                Engine(config).use { engine ->
                    engine.initialize()
                    val policy = """
                        Tu es LML, un assistant local et supervisé. Réponds en français, de façon brève.
                        Tu ne peux pas exécuter d’actions. Tu peux seulement proposer un plan pour une application autorisée.
                        Les demandes d’envoi, paiement, suppression, publication, compte ou mots de passe doivent être marquées comme critiques et renvoyées vers une double validation humaine.
                        Applications autorisées : $allowedApps. Scénarios locaux : $scenarios.
                    """.trimIndent()
                    val conversation = engine.createConversation(
                        ConversationConfig(
                            systemInstruction = Contents.of(policy),
                            samplerConfig = SamplerConfig(topK = 20, topP = 0.8, temperature = 0.2)
                        )
                    )
                    conversation.use {
                        val answer = it.sendMessage(query).toString()
                        callback.onReply(answer.ifBlank { "Je n’ai pas pu produire une réponse locale." })
                    }
                }
            } catch (error: Throwable) {
                callback.onFailure(error.message ?: "Le moteur local n’a pas pu répondre.")
            }
        }, "lml-local-agent").start()
    }
}
