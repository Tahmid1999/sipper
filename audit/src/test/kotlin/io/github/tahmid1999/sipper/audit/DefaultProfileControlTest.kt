package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class DefaultProfileControlTest {

    @Test
    fun api34DefaultFailsItsOwnRequiredKeys() = control(34)

    @Test
    fun api36DefaultFailsItsOwnRequiredKeys() = control(36)

    private fun resource(path: String): String? =
        javaClass.getResourceAsStream(path)?.use { it.readBytes().decodeToString() }

    private fun control(api: Int) {
        val xml = resource("/aosp-defaults/$api/power_profile.xml")
            ?: fail("missing fixture /aosp-defaults/$api/power_profile.xml")
        val model = ProfileModel(parseProfile(xml), api)
        val required = requiredKeys(api)
        val absent = required
            .filter { verdictOf(model.provenance(it)) == Verdict.ZERO_BY_ABSENCE }
            .toSortedSet()

        val committed = resource("/aosp-defaults/$api/expected-absent.txt")
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.startsWith("#") }
            ?.toSet()
            ?: fail(
                "no committed expected-absent.txt for api $api. Write " +
                    "audit/src/test/resources/aosp-defaults/$api/expected-absent.txt " +
                    "containing exactly these ${absent.size} lines:\n" + absent.joinToString("\n"),
            )

        // 1 - the committed list is not stale.
        assertEquals(
            emptySet(), committed - required,
            "expected-absent.txt for api $api lists keys that are not required at that level",
        )

        // 2 - none of them is declared in the release's own default file.
        assertEquals(
            emptySet(), committed.filter { it in model.declared }.toSet(),
            "keys declared in the api $api default but listed as expected-absent",
        )

        // 3 - none of them resolves through a back-fill chain either.
        assertEquals(
            emptySet(), committed.filter { model.provenance(it) is Provenance.BackFilled }.toSet(),
            "keys back-filled at api $api but listed as expected-absent",
        )

        // 4 - each one audits to exactly ZERO_BY_ABSENCE, and the file as a whole still fails.
        committed.forEach {
            assertEquals(Verdict.ZERO_BY_ABSENCE, verdictOf(model.provenance(it)), it)
        }
        assertTrue(
            absent.isNotEmpty(),
            "positive control passed, which means the claim is gone: the api $api default " +
                "power_profile.xml now satisfies requiredKeys($api). Either the file changed " +
                "upstream or requiredKeys($api) is wrong. Do not delete this gate to make the " +
                "build green.",
        )
        assertEquals(
            committed, absent.toSet(),
            "the set of absent required keys drifted from the committed list for api $api",
        )
    }
}
