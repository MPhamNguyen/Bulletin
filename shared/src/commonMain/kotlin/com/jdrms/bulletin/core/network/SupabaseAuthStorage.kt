package com.jdrms.bulletin.core.network

import io.github.jan.supabase.auth.CodeVerifierCache

/** Platform-provided persistence for PKCE verifiers; session migration is a separate rollout. */
interface SupabaseAuthStorage : CodeVerifierCache
