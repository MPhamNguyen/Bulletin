import { createHandler } from "./worker.ts";

const config = {
  url: "https://example.test",
  serviceKey: "server-only",
  cronSecret: "cron-only",
};
function assert(value: unknown, message = "Assertion failed"): asserts value {
  if (!value) throw Error(message);
}
function req(body: unknown = {}, secret = config.cronSecret) {
  return new Request(config.url, {
    method: "POST",
    headers: { "x-deletion-secret": secret },
    body: JSON.stringify(body),
  });
}
function mock(
  responses: {
    path: string;
    value?: unknown;
    status?: number;
    body?: unknown;
  }[],
) {
  const calls: string[] = [];
  const fetcher: typeof fetch = (input, init) => {
    const path = new URL(input.toString()).pathname;
    calls.push(path);
    const next = responses.shift();
    assert(next && path.endsWith(next.path), `Unexpected call: ${path}`);
    if (next.body) {
      assert(
        JSON.stringify(JSON.parse((init as RequestInit)?.body as string)) ===
          JSON.stringify(next.body),
      );
    }
    return Promise.resolve(
      Response.json(next.value ?? {}, { status: next.status ?? 200 }),
    );
  };
  return { calls, fetcher, remaining: () => responses.length };
}
const job = { user_id: "test-user", token: "lease" };

Deno.test("rejects unauthorized and malformed requests without backend calls", async () => {
  const m = mock([]);
  const handler = createHandler(config, m.fetcher);
  assert((await handler(req({}, "wrong"))).status === 401);
  assert((await handler(req({ dry_run: "false" }))).status === 400);
  assert(m.calls.length === 0);
});
Deno.test("defaults to a preview that cannot claim or delete", async () => {
  const m = mock([{
    path: "account_purge_preview",
    value: { eligible: 2, held: 1 },
  }]);
  const response = await createHandler(config, m.fetcher)(req());
  assert((await response.json()).dry_run === true);
  assert(m.calls.length === 1);
});
Deno.test("removes Storage, rechecks metadata, then hard deletes Auth", async () => {
  const m = mock([
    { path: "account_purge_claim", value: job },
    {
      path: "account_purge_step",
      value: [{ bucket_id: "pfp", object_name: "user/avatar" }],
    },
    { path: "/storage/v1/object/pfp", body: { prefixes: ["user/avatar"] } },
    { path: "account_purge_step", value: [] },
    {
      path: "/auth/v1/admin/users/test-user",
      body: { should_soft_delete: false },
    },
    { path: "account_purge_step" },
  ]);
  const response = await createHandler(config, m.fetcher)(
    req({ dry_run: false }),
  );
  assert((await response.json()).completed === 1);
  assert(m.remaining() === 0);
});
Deno.test("Storage failure schedules retry and never deletes Auth", async () => {
  const m = mock([
    { path: "account_purge_claim", value: job },
    {
      path: "account_purge_step",
      value: [{ bucket_id: "pfp", object_name: "avatar" }],
    },
    { path: "/storage/v1/object/pfp", status: 500 },
    {
      path: "account_purge_step",
      body: { p_user: job.user_id, p_token: job.token, p_action: "retry" },
    },
  ]);
  assert(
    (await createHandler(config, m.fetcher)(req({ dry_run: false }))).status ===
      500,
  );
  assert(m.remaining() === 0);
});
Deno.test("recovers when Auth was already deleted before a worker crash", async () => {
  const m = mock([
    { path: "account_purge_claim", value: job },
    { path: "account_purge_step", value: [] },
    { path: "/auth/v1/admin/users/test-user", status: 404 },
    {
      path: "account_purge_step",
      body: { p_user: job.user_id, p_token: job.token, p_action: "complete" },
    },
  ]);
  assert(
    (await createHandler(config, m.fetcher)(req({ dry_run: false })).then((r) =>
      r.json()
    )).completed === 1,
  );
});

Deno.test("Auth constraint failure schedules retry without marking complete", async () => {
  const m = mock([
    { path: "account_purge_claim", value: job },
    { path: "account_purge_step", value: [] },
    { path: "/auth/v1/admin/users/test-user", status: 500 },
    {
      path: "account_purge_step",
      body: { p_user: job.user_id, p_token: job.token, p_action: "retry" },
    },
  ]);
  assert(
    (await createHandler(config, m.fetcher)(req({ dry_run: false }))).status ===
      500,
  );
  assert(m.remaining() === 0);
});

Deno.test("exhausted time budget yields the job without calling Auth", async () => {
  const m = mock([
    { path: "account_purge_claim", value: job },
    {
      path: "account_purge_step",
      body: { p_user: job.user_id, p_token: job.token, p_action: "retry" },
    },
  ]);
  let time = 0;
  const response = await createHandler(
    config,
    m.fetcher,
    () => (time += 71_000),
  )(req({ dry_run: false }));
  assert(response.status === 202);
  assert(m.remaining() === 0);
});
