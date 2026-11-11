-- GARMENTO wipe: deletes ALL data from every table.
--
-- DESTRUCTIVE AND PERMANENT. No undo. Delete order handled by CASCADE.
-- Run in Supabase SQL Editor once. Safe to run again (tables just stay empty).

truncate table
  orders,
  transactions,
  subscriptions,
  customers,
  subscription_plans,
  admins,
  order_audit,
  pending_sync
cascade;