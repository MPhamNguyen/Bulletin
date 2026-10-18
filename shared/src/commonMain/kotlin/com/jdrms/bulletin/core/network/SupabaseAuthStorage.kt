package com.jdrms.bulletin.core.network

import io.github.jan.supabase.auth.CodeVerifierCache
import io.github.jan.supabase.auth.SessionManager

/** Platform-provided persistence for Supabase sessions and PKCE verifiers. */
interface SupabaseAuthStorage : SessionManager, CodeVerifierCache
