package pictures.reisinger.test


import assertk.Assert
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.fail
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.*
import kotlin.reflect.KProperty1

@TestDsl
suspend inline fun <reified T> HttpClient.postJson(url: String, data: T): HttpReturn<Unit> = post(url) {
    accept(ContentType.Application.Json)
    contentType(ContentType.Application.Json)
    setBody(data)
}.toHttpReturn<Unit>()

@TestDsl
suspend inline fun <reified T> HttpClient.putJson(url: String, data: T): HttpReturn<Unit> = put(url) {
    accept(ContentType.Application.Json)
    contentType(ContentType.Application.Json)
    setBody(data)
}.toHttpReturn<Unit>()

@TestDsl
suspend inline fun <reified T> HttpClient.deleteJson(url: String, data: T): HttpReturn<Unit> = delete(url) {
    accept(ContentType.Application.Json)
    contentType(ContentType.Application.Json)
    setBody(data)
}.toHttpReturn<Unit>()

@TestDsl
suspend inline fun <reified T> HttpClient.getJson(url: String): HttpReturn<T> = get(url) {
    accept(ContentType.Application.Json)
}.toHttpReturn<T>()

@TestDsl
suspend inline fun <reified T> HttpResponse.toHttpReturn(): HttpReturn<T> = when (status.value) {
    204, 205, 304 -> NoContent(status, headers)
    in 200 until 300 -> SuccessContent(status, headers, body<T>())
    else -> ErrorContent(status, bodyAsText(), headers)
}

fun HttpReturn<Any>.isNoContent(block: Assert<NoContent<Any>>.() -> Unit = {}) =
    assertThis { block(isInstanceOf<NoContent<Any>>()) }

inline fun <reified T : Any> Assert<Any>.isInstanceOf(): Assert<T> {
    return transform(name) { actual ->
        actual as? T ?: expected("Instanceof check not successful got", T::class, actual)
    }
}

fun HttpReturn<Any>.isErrorContent(block: Assert<ErrorContent<Unit>>.() -> Unit) =
    assertThis { block(isInstanceOf<ErrorContent<Unit>>()) }

fun <T> HttpReturn<T>.isSuccessContent(block: Assert<SuccessContent<T>>.() -> Unit = {}) =
    assertThis { block(isInstanceOf<SuccessContent<T>>()) }

fun <T> Assert<SuccessContent<T>>.getBody() = prop(SuccessContent<T>::data)

fun <T> Assert<ErrorContent<T>>.hasStatus(statusCode: HttpStatusCode) {
    transform { it.statusCode }.isEqualTo(statusCode)
}

fun <T> T.assertThis(block: Assert<T>.() -> Unit) = assertAll {
    assertThat(this).let(block)
}

fun <T> Assert<Pair<T, T>>.areEqual() = given { (expected, actual) ->
    if (actual == expected) return
    fail(expected, actual)
}

fun <T : Comparable<T>> Assert<Pair<T, T>>.areEqualComparingTo() = given { (expected, actual) ->
    if (actual.compareTo(expected) == 0) return
    fail(expected, actual)
}

fun <I, O> Assert<Pair<I, I>>.propPair(callable: KProperty1<I, O>): Assert<Pair<O, O>> =
    prop(callable.name) { (first, second) ->
        callable.get(first) to callable.get(second)
    }


@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class TestDsl

@TestDsl
sealed interface HttpReturn<T>

@TestDsl
data class NoContent<T>(val statusCode: HttpStatusCode, val headers: Headers) : HttpReturn<T>

@TestDsl
data class ErrorContent<T>(val statusCode: HttpStatusCode, val apiError: String, val headers: Headers) : HttpReturn<T>

@TestDsl
data class SuccessContent<T>(val statusCode: HttpStatusCode, val headers: Headers, val data: T) : HttpReturn<T>
