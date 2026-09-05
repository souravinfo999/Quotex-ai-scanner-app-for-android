package com.example.data.model

data class MistralChatRequest(
    val model: String,
    val messages: List<MistralMessage>,
    val temperature: Double = 0.2,
    val max_tokens: Int? = 1024,
    val response_format: ResponseFormat? = ResponseFormat(type = "json_object")
)

data class ResponseFormat(
    val type: String = "json_object"
)

data class MistralMessage(
    val role: String,
    val content: Any // Can be String or List<ContentPart>
)

data class TextContentPart(
    val type: String = "text",
    val text: String
)

data class ImageUrlContentPart(
    val type: String = "image_url",
    val image_url: ImageUrl
)

data class ImageUrl(
    val url: String // e.g. "data:image/jpeg;base64,..."
)

data class MistralChatResponse(
    val id: String?,
    val model: String?,
    val choices: List<MistralChoice>?
)

data class MistralChoice(
    val index: Int?,
    val message: MistralResponseMessage?,
    val finish_reason: String?
)

data class MistralResponseMessage(
    val role: String?,
    val content: String?
)
