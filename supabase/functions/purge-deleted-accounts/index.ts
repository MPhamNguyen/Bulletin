import { createHandler } from "./worker.ts";

Deno.serve(createHandler({
  url: Deno.env.get("SUPABASE_URL") ?? "",
  serviceKey: Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
  cronSecret: Deno.env.get("ACCOUNT_DELETION_CRON_SECRET") ?? "",
}));
