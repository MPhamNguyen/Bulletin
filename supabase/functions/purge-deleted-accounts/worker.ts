// No dependencies; tests inject HTTP and time. Never log response bodies or file paths.
type Job = { user_id: string; token: string };
type File = { bucket_id: string; object_name: string };
export type Configuration = {
  url: string;
  serviceKey: string;
  cronSecret: string;
};

class BackendFailure extends Error {
  constructor(readonly stage: string, readonly status: number) {
    super(`${stage}: HTTP ${status}`);
  }
}

export function createHandler(
  config: Configuration,
  request: typeof fetch = fetch,
  now: () => number = Date.now,
) {
  async function api(path: string, body: unknown, method = "POST") {
    const response = await request(`${config.url}${path}`, {
      method,
      headers: {
        apikey: config.serviceKey,
        Authorization: `Bearer ${config.serviceKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(15_000),
    });
    if (!response.ok) {
      throw new BackendFailure(path.split("/")[1], response.status);
    }
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  }
  const rpc = (name: string, body = {}) =>
    api(`/rest/v1/rpc/account_purge_${name}`, body);
  const step = (job: Job, action: string) =>
    rpc("step", {
      p_user: job.user_id,
      p_token: job.token,
      p_action: action,
    });

  return async (req: Request): Promise<Response> => {
    if (!config.cronSecret || !config.serviceKey || !config.url) {
      return Response.json({ error: "Worker configuration is incomplete" }, {
        status: 503,
      });
    }
    const supplied = req.headers.get("x-deletion-secret") ?? "";
    // Compare fixed-length digests, never privileged project keys from the request.
    const digest = async (s: string) =>
      new Uint8Array(
        await crypto.subtle.digest("SHA-256", new TextEncoder().encode(s)),
      );
    const [a, b] = await Promise.all([
      digest(supplied),
      digest(config.cronSecret),
    ]);
    if (a.reduce((diff, byte, i) => diff | (byte ^ b[i]), 0) !== 0) {
      return Response.json({ error: "Unauthorized" }, { status: 401 });
    }
    if (req.method !== "POST") return new Response(null, { status: 405 });
    let body;
    try {
      body = await req.json();
      if (
        body === null || typeof body !== "object" || Array.isArray(body) ||
        (body.dry_run !== undefined && typeof body.dry_run !== "boolean")
      ) throw Error();
    } catch {
      return Response.json({
        error: "Expected JSON with optional boolean dry_run",
      }, { status: 400 });
    }
    let job: Job | null = null;
    try {
      // Missing flag defaults to preview. A preview never claims or freezes accounts.
      if (body.dry_run !== false) {
        return Response.json({ dry_run: true, ...await rpc("preview") });
      }
      job = await rpc("claim");
      if (!job) return Response.json({ completed: 0 });
      const deadline = now() + 70_000;
      while (now() < deadline) {
        const files: File[] = await step(job, "files");
        if (!files.length) {
          try {
            await api(`/auth/v1/admin/users/${job.user_id}`, {
              should_soft_delete: false,
            }, "DELETE");
          } catch (error) {
            // A previous invocation may have deleted Auth and died before marking complete.
            if (
              !(error instanceof BackendFailure && error.stage === "auth" &&
                error.status === 404)
            ) throw error;
          }
          await step(job, "complete");
          return Response.json({ completed: 1 });
        }
        const bucket = files[0].bucket_id;
        await api(`/storage/v1/object/${encodeURIComponent(bucket)}`, {
          prefixes: files.filter((f) => f.bucket_id === bucket).map((f) =>
            f.object_name
          ),
        }, "DELETE");
      }
      await step(job, "retry");
      return Response.json({ completed: 0, retry_pending: true }, {
        status: 202,
      });
    } catch (error) {
      if (job) {
        try {
          await step(job, "retry");
        } catch { /* Expired lease recovers automatically. */ }
      }
      const detail = error instanceof BackendFailure
        ? { stage: error.stage, backend_status: error.status }
        : { stage: "transport_or_worker" };
      console.error(
        JSON.stringify({ event: "account_purge_failed", ...detail }),
      );
      return Response.json({
        error: "Cleanup failed; retry scheduled",
        ...detail,
      }, { status: 500 });
    }
  };
}
