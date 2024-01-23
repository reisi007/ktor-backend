package pictures.reisinger.test


import assertk.Assert
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.fail
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.*
import kotlin.reflect.KProperty1


@TestDsl
suspend inline fun <reified T> HttpClient.postJson(url: String, data: T): HttpReturn = post(url) {
    accept(ContentType.Application.Json)
    contentType(ContentType.Application.Json)
    setBody(data)
}.toHttpReturn<T>()

@TestDsl
suspend inline fun <reified T> HttpClient.putJson(url: String, data: T): HttpReturn = put(url) {
    accept(ContentType.Application.Json)
    contentType(ContentType.Application.Json)
    setBody(data)
}.toHttpReturn<T>()

@TestDsl
suspend inline fun <reified T> HttpClient.deleteJson(url: String, data: T): HttpReturn = delete(url) {
    accept(ContentType.Application.Json)
    contentType(ContentType.Application.Json)
    setBody(data)
}.toHttpReturn<T>()

@TestDsl
suspend inline fun <reified T> HttpClient.getJson(url: String): HttpReturn = get(url) {
    accept(ContentType.Application.Json)
}.toHttpReturn<T>()

@TestDsl
suspend inline fun <reified T> HttpResponse.toHttpReturn(): HttpReturn = when (status.value) {
    204, 205, 304 -> NoContent(status, headers)
    in 200 until 300 -> SuccessContent(status, headers, body<T>())
    else -> ErrorContent(body(), headers)
}


fun HttpReturn.isNoContent(block: Assert<NoContent>.() -> Unit = {}) = assertThis { block(isInstanceOf<NoContent>()) }

inline fun <reified T : Any> Assert<Any>.isInstanceOf(): Assert<T> {
    return transform(name) { actual ->
        actual as? T ?: expected("Instaceof check not successful", T::class, actual)
    }
}

fun HttpReturn.isErrorContent(block: Assert<ErrorContent>.() -> Unit) =
    assertThis { block(isInstanceOf<ErrorContent>()) }

fun <T> HttpReturn.isSuccessContent(block: Assert<SuccessContent<T>>.() -> Unit) =
    assertThis { block(isInstanceOf<SuccessContent<T>>()) }

fun <T> Assert<SuccessContent<T>>.getBody() = prop(SuccessContent<T>::data)

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
sealed interface HttpReturn

@TestDsl
data class NoContent(val statusCode: HttpStatusCode, val headers: Headers) : HttpReturn

@TestDsl
data class ErrorContent(val apiError: Error, val headers: Headers) : HttpReturn


@TestDsl
data class SuccessContent<T>(val statusCode: HttpStatusCode, val headers: Headers, val data: T) : HttpReturn
