package com.framebynavin.app.cloud

import org.junit.Assert.*
import org.junit.Test

class PersonalArchiveCloudV2RulesTest {
    private val owner = "5e33c70b-e34f-46a5-98d6-996761b1ab91"
    private val other = "efdb2743-8841-4a56-87b0-3d380b334d16"
    private val archive = "v2-f928d94c-e7a9-4c02-9f58-232e22c11f4d.zip"

    @Test fun onlyExactlyOwnerPrefixedAndImmutableUuidZipIsAllowed() {
        PersonalArchiveCloudV2Rules.validateObject(owner + "/" + archive, owner)
        assertEquals("backlot-personal-archive-v2", PersonalArchiveCloudV2Rules.BUCKET)
    }

    @Test fun anotherAccountsObjectCannotBeReadOrClaimed() {
        assertThrows(IllegalArgumentException::class.java) {
            PersonalArchiveCloudV2Rules.validateObject(other + "/" + archive, owner)
        }
    }

    @Test fun pathTraversalAndLegacyCreatorFilesAreRejected() {
        for (path in listOf(
            owner + "/../" + archive, owner + "/" + archive + "/extra",
            owner + "/voice.m4a", owner + "/v2-not-uuid.zip",
            owner + "/v2-" + "f".repeat(900) + ".zip",
        )) assertThrows(IllegalArgumentException::class.java) {
            PersonalArchiveCloudV2Rules.validateObject(path, owner)
        }
    }

    @Test fun invalidAccountAndHashCannotEnterCloudCas() {
        assertThrows(IllegalArgumentException::class.java) {
            PersonalArchiveCloudV2Rules.validateAccount("../other")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PersonalArchiveCloudV2Rules.validateSha("0".repeat(63))
        }
        PersonalArchiveCloudV2Rules.validateSha("f".repeat(64))
    }
}
