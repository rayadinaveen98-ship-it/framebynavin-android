#!/usr/bin/env bash
set -euo pipefail
grep -Fq 'val trashedAtMillis: Long = 0L' app/src/main/java/com/framebynavin/app/data/IdeaVault.kt
grep -Fq '.put("trashedAtMillis", idea.trashedAtMillis)' app/src/main/java/com/framebynavin/app/data/IdeaVaultStore.kt
grep -Fq 'trashedAtMillis = item.optLong(' app/src/main/java/com/framebynavin/app/data/IdeaVaultStore.kt
grep -Fq 'fun restoreIdea(id: String)' app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt
grep -Fq 'val desired = ideas.toList() + deletedIdeas.toList()' app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt
grep -Fq '!idea.isTrashed' app/src/main/java/com/framebynavin/app/data/IdeaReminderPolicy.kt
grep -Fq 'Recently Deleted' app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt
grep -Fq 'V162DeletedIdeaCard' app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt
grep -Fq 'onRestore = vm::restoreIdea' app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt
grep -Fq 'fun similarTitles' app/src/main/java/com/framebynavin/app/data/IdeaVaultGuidance.kt
echo 'V162 preservation checks passed'
