package com.drodriguez.maestro.api.response

data class MaestroResponseBody(var message: String, var data: Any) {
    constructor(message: String) : this(message, {})
}
