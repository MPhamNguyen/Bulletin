# Security Policy

Bulletin is a student campus marketplace built as a CECS 491A/B capstone (senior design) project by Team JDRMS. It handles accounts tied to university email addresses, direct messages between students, profile data, and moderation reports, so we take security and privacy reports seriously.

> **Project status:** Bulletin is an academic capstone project in development. It has not had a formal production security audit and is not intended to hold real production data until the gaps in [Known Limitations](#known-limitations) are closed. **Maintenance may stop or slow after the course ends**, so response times below apply only while the team is actively working on the project.

## Supported Versions

Bulletin does not publish versioned releases. Security fixes are made on the default branch only, and only while the project is actively maintained. If the repository is archived or the team stops maintaining it, no further fixes should be expected.

| Version / Branch | Supported |
| --- | --- |
| `main` (latest commit) | ✅ |
| Feature branches, forks, older commits | ❌ |

## Reporting a Vulnerability

**Please do not open a public issue, pull request, or discussion for a security problem.**

Report privately using one of these:

1. **GitHub private vulnerability reporting (preferred):** go to the repository's **Security** tab and choose **Report a vulnerability**.
2. **Direct contact:** if that option is unavailable, contact a maintainer listed in [`CODEOWNERS`](CODEOWNERS) through their GitHub profile and ask for a private channel. Do not include details in a public message.

Please include as much of the following as you can:

- A description of the issue and its potential impact
- The affected component (Android app, admin portal, Supabase configuration, storage buckets, CI)
- Steps to reproduce, or a proof of concept
- Any relevant request/response samples, with real tokens and personal data removed
- Your suggested fix, if you have one

### What to expect

This is a student-run capstone project, so these are good-faith targets rather than guarantees, and they may be slower during exams, breaks, or after the course ends:

| Step | Target |
| --- | --- |
| Acknowledge your report | within 5 business days |
| Initial assessment and severity | within 10 business days |
| Fix or mitigation for confirmed high-severity issues | as soon as practical, with updates as we go |

We will keep you informed, credit you in the fix if you wish, and coordinate disclosure timing with you. There is no bug bounty program.

## Scope

**In scope**

- The Android app and shared Kotlin code (`androidApp/`, `shared/`)
- The Flask admin portal (`admin/`)
- Supabase usage as configured by this repository: Row Level Security, storage policies, RPCs, Auth settings, and email templates (`supabase/`)
- GitHub Actions workflows and repository configuration (`.github/`)

**Examples of issues we want to hear about**

- Reading or modifying another user's messages, profile, bookmarks, listings, or reviews
- Bypassing the `.edu` signup requirement or the account suspension check
- Access to the admin portal, or admin actions, without `profiles.is_admin`
- Exposure of the Supabase service-role key or other credentials
- RLS, storage, or Realtime policy gaps; unsafe security-definer functions
- Admin portal issues such as CSRF, session or cookie weaknesses, injection, or XSS
- Secrets, session tokens, message contents, or precise locations appearing in logs or the repository
- Vulnerable or malicious dependencies and workflow supply-chain risks

**Out of scope**

- Social engineering, phishing, or physical attacks against team members or users
- Denial-of-service or volumetric/load testing
- Automated scanner output with no demonstrated impact
- Findings in third-party services (Supabase, Vercel, GitHub, Microsoft Entra) that are not caused by our configuration. Please report those to the vendor.
- Issues that require a rooted device or a compromised user account/device
- Public image URLs being fetchable by anyone with the link (a documented behavior; see below)

## Responsible Testing

Please act in good faith:

- Test only against your own accounts and data. **Do not access, modify, or delete other users' data.** Stop and report as soon as you confirm an issue.
- Do not run tests that degrade service for others.
- Do not use social engineering or target other students.
- Do not publicly disclose an issue until we have had a reasonable chance to fix it.

We will not pursue action against researchers who follow this policy.

## Known Limitations

Our README documents hardening work that is still open. You are welcome to report related findings, but these are already tracked:

- Review-insert and conversation-creation authorization need tightening
- Broad table grants and profile column exposure need narrowing
- Some database functions have mutable search paths
- Leaked-password protection is not yet enabled
- Microsoft sign-in and live actor-based access tests have not been verified end to end
- The Supabase schema and RLS policies live in the hosted project; migration files are not yet checked in
- The `listing_imgs` and `pfp` storage buckets are public, so anyone with an image URL can retrieve the image
- Removing a profile photo clears the avatar URL but leaves the uploaded file in Storage until replaced

## Handling Secrets and Sensitive Data

If you are contributing, follow the rules in [`AGENTS.md`](AGENTS.md):

- The Android app uses **only** the Supabase anon/public key. Never commit the service-role key, `.env` files, credentials, or real user data. `.env.example` holds placeholders only.
- The service-role key and `ADMIN_SECRET_KEY` belong only in the admin portal's server-side environment.
- Never log passwords, session tokens, message contents, or precise locations.
- Keep tokens in the platform's secure storage abstraction.

**If you find a leaked secret in this repository or its history**, report it privately as described above. We will rotate the credential first and then clean up.

## Security Updates

Fixes are delivered as commits to `main`. Where appropriate, we will publish a GitHub Security Advisory describing the issue and credit the reporter.

Thank you for helping keep Bulletin and its student community safe.
