-- Lunabloom Ready Board — standalone tables only; safe for a dedicated Supabase project.
-- Intended for deployment via the Supabase SQL editor or a migration.
-- The browser only uses the PUBLIC publishable/anon key; never a service-role key.

create or replace function public.lunabloom_service_day()
returns date language sql stable set search_path = '' as $$
  select ((now() at time zone 'Asia/Jakarta') - interval '5 hours')::date;
$$;

create table if not exists public.lunabloom_selections (
  service_day date not null,
  user_id uuid not null references auth.users(id) on delete cascade,
  slot smallint not null check (slot between 0 and 47),
  created_at timestamptz not null default now(),
  primary key(service_day, user_id, slot)
);
create index if not exists lunabloom_selections_day_slot_idx on public.lunabloom_selections(service_day,slot);

create table if not exists public.lunabloom_slot_totals (
  service_day date not null,
  slot smallint not null check (slot between 0 and 47),
  members integer not null default 0 check (members between 0 and 35),
  primary key(service_day,slot)
);

alter table public.lunabloom_selections enable row level security;
alter table public.lunabloom_slot_totals enable row level security;
-- No direct read/write policies on selections: identities stay private.
revoke all on public.lunabloom_selections from anon,authenticated;
revoke all on public.lunabloom_slot_totals from anon,authenticated;
grant select on public.lunabloom_slot_totals to anon,authenticated;

drop policy if exists lunabloom_read_totals on public.lunabloom_slot_totals;
create policy lunabloom_read_totals on public.lunabloom_slot_totals
for select to anon,authenticated using (service_day = public.lunabloom_service_day());

create or replace function public.lunabloom_update_totals()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if TG_OP='DELETE' then
    update public.lunabloom_slot_totals
       set members = greatest(0,members-1)
     where service_day=OLD.service_day and slot=OLD.slot;
    return old;
  elsif TG_OP='INSERT' then
    insert into public.lunabloom_slot_totals(service_day,slot,members)
    values (NEW.service_day,NEW.slot,1)
    on conflict(service_day,slot) do update
    set members=public.lunabloom_slot_totals.members+1;
    return new;
  end if;
  return null;
end;
$$;
drop trigger if exists lunabloom_selection_totals_trigger on public.lunabloom_selections;
create trigger lunabloom_selection_totals_trigger
  after insert or delete on public.lunabloom_selections
  for each row execute function public.lunabloom_update_totals();

create or replace function public.lunabloom_board()
returns table(slot smallint, members integer)
language sql stable security definer set search_path = '' as $$
  select t.slot,t.members from public.lunabloom_slot_totals t
  where t.service_day=public.lunabloom_service_day() and t.members>0
  order by t.slot;
$$;

create or replace function public.lunabloom_my_slots()
returns smallint[] language sql stable security definer set search_path = '' as $$
  select coalesce(array_agg(s.slot order by s.slot),array[]::smallint[])
  from public.lunabloom_selections s
  where s.service_day=public.lunabloom_service_day() and s.user_id=auth.uid();
$$;

create or replace function public.lunabloom_set_slots(p_slots integer[])
returns smallint[] language plpgsql security definer set search_path = '' as $$
declare
  user_uuid uuid := auth.uid();
  today date := public.lunabloom_service_day();
  normalized smallint[];
  distinct_count integer;
  active_count integer;
begin
  if user_uuid is null then raise exception 'auth_required' using errcode='P0001'; end if;
  if p_slots is null or cardinality(p_slots)>5 then
    raise exception 'max_five' using errcode='P0001';
  end if;
  if exists(select 1 from unnest(p_slots) x where x is null or x < 0 or x > 47) then
    raise exception 'invalid_slot' using errcode='P0001';
  end if;
  select coalesce(array_agg(x order by x),array[]::smallint[]),count(*)
    into normalized,distinct_count
    from (select distinct unnest(p_slots)::smallint as x) vals;
  if distinct_count <> cardinality(p_slots) then
    raise exception 'duplicate_slot' using errcode='P0001';
  end if;

  -- Serializes all writes, including simultaneous first-time registrations.
  perform pg_advisory_xact_lock(hashtext('lunabloom-'||today::text));
  if cardinality(normalized)>0 and not exists (
    select 1 from public.lunabloom_selections
    where service_day=today and user_id=user_uuid
  ) then
    select count(distinct user_id) into active_count
    from public.lunabloom_selections where service_day=today;
    if active_count>=35 then raise exception 'board_full' using errcode='P0001'; end if;
  end if;

  delete from public.lunabloom_selections s
   where s.service_day=today and s.user_id=user_uuid
   and not (s.slot=any(normalized));
  insert into public.lunabloom_selections(service_day,user_id,slot)
   select today,user_uuid,x from unnest(normalized) x
   on conflict do nothing;

  -- Seven-day retention, logical daily reset at 05.00 (Asia/Jakarta).
  delete from public.lunabloom_selections where service_day < today-7;
  delete from public.lunabloom_slot_totals where service_day < today-7;
  return normalized;
end;
$$;

revoke all on function public.lunabloom_board() from public;
revoke all on function public.lunabloom_my_slots() from public;
revoke all on function public.lunabloom_set_slots(integer[]) from public;
grant execute on function public.lunabloom_board() to anon,authenticated;
grant execute on function public.lunabloom_my_slots() to authenticated;
grant execute on function public.lunabloom_set_slots(integer[]) to authenticated;
grant execute on function public.lunabloom_service_day() to anon,authenticated;

-- Public aggregate only; NEVER add lunabloom_selections to realtime publication.
do $$ begin
  if not exists (
    select 1 from pg_publication_tables
    where pubname='supabase_realtime' and schemaname='public' and tablename='lunabloom_slot_totals'
  ) then
    alter publication supabase_realtime add table public.lunabloom_slot_totals;
  end if;
end $$;
