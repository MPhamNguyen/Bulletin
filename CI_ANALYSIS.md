# CI efficiency analysis

## What CI does today

`.github/workflows/ci.yml` has one required **Build and Test** job with a 30-minute timeout. It decides whether a change needs the full build, a narrower run, or no Gradle run for docs-only changes.

For a change under any bounded context, the workflow currently adds `:shared:compileAndroidMain` and one `:shared:testAndroidHostTest` task, then passes filters for all five domains—home, marketplace, listings, messages, and profile—plus app and core tests. Gradle runs that as one task in one job. The contexts are separate source packages, but they are **not separate Gradle modules or test tasks**: they all live in `:shared`.

A change under any one domain therefore runs tests for every domain. The workflow also does not identify *which* domain changed; it only sets `domain_changed=true`.

## What “parallel domains” means here

There are two kinds of parallelism to distinguish:

- **Separate GitHub Actions jobs:** Each domain gets its own job or matrix entry. GitHub can run those jobs concurrently, subject to runner and concurrency limits.
- **Gradle test-worker parallelism:** One Gradle invocation runs test classes in parallel workers. This can shorten a single test task, but it does not create independently reported domain jobs, and it still runs the combined filtered suite.

Because the domains are packages inside one module, Gradle does not automatically treat them as parallel modules. The clearest way to run a domain’s filtered tests independently is a separate Gradle invocation for that domain, typically from a matrix job.

## A practical design

For **affected-domain CI**, first map changed paths to a list of affected domains instead of one Boolean. Then create matrix entries for those domains. Each entry can run `:shared:compileAndroidMain` and `:shared:testAndroidHostTest` with one domain’s `--tests` filter. Add a separate check for the app/core tests that the current workflow also runs for domain changes.

This gives you:

1. **Narrower coverage:** a home-only change runs home tests rather than all five domain suites.
2. **Concurrent execution:** multiple changed domains can run in separate Actions jobs.
3. **Clearer failures:** the failed matrix entry identifies the domain suite that failed.
4. **A deliberate fallback:** build-wide changes, unrecognized paths, scheduled runs, and merge-queue runs can retain the full-check path.

The workflow already has an `affected` mode and distinguishes domain changes from other paths, so this can fit its current shape. The main change is replacing the single `domain_changed` flag and combined filters with a changed-domain list and matrix.

## Trade-offs and caveats

- **Each matrix job has setup and compilation work.** Splitting five suites can reduce elapsed time when jobs run concurrently, but it increases total runner minutes and duplicates Gradle configuration and some compilation work. `gradle/actions/setup-gradle` may help through its cache, but the overhead remains.
- **Parallelism is not guaranteed to equal the number of domains.** GitHub’s available concurrency and repository limits determine how many jobs actually run at once. A matrix provides independent jobs; it does not guarantee five simultaneous runners.
- **Keep shared tests covered.** The existing affected-domain invocation runs `com.jdrms.bulletin.app.*` and `com.jdrms.bulletin.core.*` alongside domain tests. A domain-only matrix must preserve that coverage in a separate job or shared test step.
- **Do not weaken full CI accidentally.** The current full path runs `./gradlew check`, which is broader than the affected-domain filters. A matrix for affected changes does not, by itself, parallelize that full path. Splitting the full check needs a separate design for keeping non-domain tests, compilation, lint, and other `check` dependencies covered exactly once.
- **Verify wildcard scope.** The workflow already uses `--tests com.jdrms.bulletin.domain.<domain>.*` for package filtering. Before relying on that pattern for each matrix entry, confirm it selects the nested test packages intended for that domain.

## Recommendation

Start by splitting **affected-domain runs** into a matrix over the *changed* domains, while retaining app/core tests and the existing full-check fallback. This addresses both current inefficiencies: unrelated domain tests run after a single-domain change, and selected tests run in one job. Then compare elapsed time and total runner use against the current workflow before deciding whether the full `check` path should also be reorganized.

This is analysis only; no workflow or Gradle configuration changes are included.
