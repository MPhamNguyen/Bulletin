create policy "Users can remove their own blocks"
    on public.user_blocks for delete
    using (auth.uid() = blocker_id);
