package com.chayzay.catequesisapp.data

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

/** Respeta las claves class_<id>, bold_font_view y class_finished del proyecto antiguo. */
class ClassProgressStore(private val context: Context, private val accountId: Int? = null) {
    private val suffix = accountId?.takeIf { it > 0 }?.let { "_account_$it" }.orEmpty()
    private val internal = File(context.filesDir, "class_course_user$suffix.json")
    private val courseApprovedFile = File(context.filesDir, "course_approved$suffix.json")
    private val testsFile = File(context.filesDir, "test_class$suffix.json")

    /** Equivale a borrar los tres JSON de progreso en la app antigua. */
    fun clearLocalProgress() {
        listOf(internal, courseApprovedFile, testsFile).forEach { file ->
            if (file.exists() && !file.delete()) throw IllegalStateException("No se pudo reiniciar el progreso local")
        }
    }

    private fun tests(): JSONArray {
        if (accountId == null && !testsFile.exists()) {
            val external = context.getExternalFilesDir(null)
            val legacy = listOfNotNull(
                external?.resolve("Android/data/${context.packageName}/db/json/test_class.json"),
                external?.parentFile?.resolve("db/json/test_class.json")
            ).firstOrNull { it.isFile }
            if (legacy != null) try { testsFile.writeText(legacy.readText(Charsets.UTF_8), Charsets.UTF_8) }
                catch (_: Exception) { }
        }
        return try { if (testsFile.isFile) JSONArray(testsFile.readText(Charsets.UTF_8)) else JSONArray() }
        catch (_: Exception) { JSONArray() }
    }

    private fun courseApprovals(): JSONArray {
        if (accountId == null && !courseApprovedFile.exists()) {
            val external = context.getExternalFilesDir(null)
            val legacy = listOfNotNull(
                external?.resolve("Android/data/${context.packageName}/db/json/course_approved.json"),
                external?.parentFile?.resolve("db/json/course_approved.json")
            ).firstOrNull { it.isFile }
            if (legacy != null) try {
                courseApprovedFile.writeText(legacy.readText(Charsets.UTF_8), Charsets.UTF_8)
            } catch (_: Exception) { }
        }
        return try {
            if (courseApprovedFile.isFile) JSONArray(courseApprovedFile.readText(Charsets.UTF_8)) else JSONArray()
        } catch (_: Exception) { JSONArray() }
    }

    fun isCourseApproved(courseId: Int): Boolean {
        val records = courseApprovals()
        return (0 until records.length()).any { index ->
            val item = records.optJSONObject(index)
            item != null && item.optInt("id_course", -1) == courseId && item.optInt("approved", 0) == 1
        }
    }

    /** No concede aprobación sin una lista real de clases y todas las piezas terminadas. */
    fun approveCourseIfComplete(courseId: Int, classIds: List<Int>): Boolean {
        if (classIds.isEmpty() || classIds.distinct().any { !flags(it).completed }) return false
        if (isCourseApproved(courseId)) return true
        val records = courseApprovals()
        records.put(JSONObject().put("approved", "1").put("id_course", courseId.toString()))
        courseApprovedFile.writeText(records.toString(), Charsets.UTF_8)
        return true
    }

    private fun read(): JSONObject {
        if (accountId == null && !internal.exists()) {
            // Archivos.getExternalBase de la app vieja cambió su base en Android 10.
            val external = context.getExternalFilesDir(null)
            val legacy = listOfNotNull(
                external?.resolve("Android/data/${context.packageName}/db/json/class_course_user.json"),
                external?.parentFile?.resolve("db/json/class_course_user.json")
            ).firstOrNull { it.isFile }
            if (legacy?.isFile == true) {
                try { internal.writeText(legacy.readText(Charsets.UTF_8), Charsets.UTF_8) }
                catch (_: Exception) { }
            }
        }
        return try { if (internal.isFile) JSONObject(internal.readText(Charsets.UTF_8)) else JSONObject() }
        catch (_: Exception) { JSONObject() }
    }

    fun flags(id: Int): Flags {
        val entry = read().optJSONObject("class_$id")
        val testRecords = tests()
        val testPassed = (0 until testRecords.length()).any { index ->
            val test = testRecords.optJSONObject(index)
            test != null && test.optInt("id_class_course", -1) == id &&
                test.optInt("approved", 0) == 1 && test.optInt("score", 0) == 10
        }
        val completed = entry?.optBoolean("class_finished", false) == true || testPassed
        return Flags(entry?.optBoolean("bold_font_view", false) == true || completed, completed)
    }

    fun setVisited(id: Int, visited: Boolean) {
        val all = read()
        val key = "class_$id"
        val entry = all.optJSONObject(key) ?: JSONObject().put("class_finished", false)
        entry.put("bold_font_view", visited)
        all.put(key, entry)
        internal.writeText(all.toString(), Charsets.UTF_8)
    }

    fun markPassed(id: Int) {
        val all = read()
        val key = "class_$id"
        val entry = all.optJSONObject(key) ?: JSONObject()
        entry.put("bold_font_view", true)
        entry.put("class_finished", true)
        all.put(key, entry)
        internal.writeText(all.toString(), Charsets.UTF_8)

        val tests = tests()
        var found = false
        for (position in 0 until tests.length()) {
            val item = tests.optJSONObject(position) ?: continue
            if (item.optInt("id_class_course", -1) == id) {
                item.put("approved", 1).put("score", 10)
                found = true
                break
            }
        }
        if (!found) tests.put(JSONObject().put("approved", 1).put("score", 10).put("id_class_course", id))
        testsFile.writeText(tests.toString(), Charsets.UTF_8)
    }

    /** Sólo se comparten aprobaciones; nunca se reemplaza una aprobación existente por cero. */
    fun approvedTests(): Set<Int> {
        val rows = tests()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            row.optInt("id_class_course").takeIf { it > 0 && row.optInt("approved") == 1 && row.optInt("score") == 10 }
        }.toSet()
    }

    fun approvedCourses(): Set<Int> {
        val rows = courseApprovals()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            row.optInt("id_course").takeIf { it > 0 && row.optInt("approved") == 1 }
        }.toSet()
    }

    fun mergeApprovals(testIds: Set<Int>, courseIds: Set<Int>) {
        val pendingTests = testIds - approvedTests()
        pendingTests.forEach { markPassed(it) }
        val pendingCourses = courseIds - approvedCourses()
        if (pendingCourses.isNotEmpty()) {
            val rows = courseApprovals()
            pendingCourses.forEach { rows.put(JSONObject().put("approved", 1).put("id_course", it)) }
            courseApprovedFile.writeText(rows.toString(), Charsets.UTF_8)
        }
    }

    data class Flags(val visited: Boolean, val completed: Boolean)
}
